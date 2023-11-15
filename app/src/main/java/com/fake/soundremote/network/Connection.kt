package com.fake.soundremote.network

import android.os.Build
import com.fake.soundremote.util.ConnectionStatus
import com.fake.soundremote.util.Net
import com.fake.soundremote.util.PacketProtocolType
import com.fake.soundremote.util.SystemMessage
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.AlreadyBoundException
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.DatagramChannel
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

internal class Connection(
    private val uncompressedAudio: SendChannel<ByteArray>,
    private val opusAudio: SendChannel<ByteArray>,
    private val connectionMessages: SendChannel<SystemMessage>,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private var receiveJob: Job? = null
    private var keepAliveJob: Job? = null
    private var pendingRequests = mutableMapOf<Net.PacketCategory, Request>()

    private var serverAddress: InetSocketAddress? = null
    private var dataChannel: DatagramChannel? = null
    private var sendChannel: DatagramChannel? = null
    private val connectLock = Any()
    private val sendLock = Any()
    private val pendingRequestsLock = Any()

    private var serverProtocol: PacketProtocolType = 1u
    private var serverLastContact = AtomicLong(0)
    private var _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus>
        get() = _connectionStatus
    private var currentStatus
        get() = _connectionStatus.value
        set(value) {
            _connectionStatus.value = value
        }

    var processAudio = AtomicBoolean(true)

    suspend fun connect(
        address: String,
        serverPort: Int,
        localPort: Int,
        @Net.Compression compression: Int
    ) = withContext(scope.coroutineContext) {
        shutdown()
        synchronized(connectLock) {
            currentStatus = ConnectionStatus.CONNECTING
            try {
                synchronized(sendLock) {
                    serverAddress = InetSocketAddress(address, serverPort)
                    sendChannel = createSendChannel()
                }
                dataChannel = createReceiveChannel(InetSocketAddress(localPort))
            } catch (e: IllegalStateException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && e is AlreadyBoundException) {
                    sendMessage(SystemMessage.MESSAGE_ALREADY_BOUND)
                } else {
                    sendMessage(SystemMessage.MESSAGE_BIND_ERROR)
                }
                releaseChannels()
                currentStatus = ConnectionStatus.DISCONNECTED
                return@withContext false
            } catch (e: Exception) {
                sendMessage(SystemMessage.MESSAGE_BIND_ERROR)
                releaseChannels()
                currentStatus = ConnectionStatus.DISCONNECTED
                return@withContext false
            }
            receiveJob = receive()
            keepAliveJob = keepAlive()
        }
        sendConnect(compression)
    }

    /**
     * Sends the disconnect packet and closes the connection.
     */
    suspend fun disconnect() {
        send(Net.getDisconnectPacket())
        shutdown()
    }

    fun sendSetFormat(@Net.Compression compression: Int) {
        val request = Request()
        val packet = Net.getSetFormatPacket(compression, request.id)
        scope.launch { send(packet) }
        synchronized(pendingRequestsLock) {
            pendingRequests[Net.PacketCategory.SET_FORMAT] = request
        }
    }

    fun sendKeystroke(keyCode: Int, mods: Int) {
        val keystrokePacket = Net.getKeystrokePacket(keyCode.toUByte(), mods.toUByte())
        scope.launch { send(keystrokePacket) }
    }

    private suspend fun shutdown() = withContext(scope.coroutineContext) {
        synchronized(connectLock) {
            if (currentStatus == ConnectionStatus.DISCONNECTED) return@withContext
            receiveJob?.cancel()
            keepAliveJob?.cancel()
            // Close channel after cancelling receiving job to avoid trying to invoke receive
            // from closed or null channel
            releaseChannels()
            currentStatus = ConnectionStatus.DISCONNECTED
        }
    }

    private fun releaseChannels() {
        synchronized(sendLock) {
            serverAddress = null
            sendChannel?.close()
            sendChannel = null
        }
        dataChannel?.close()
        dataChannel = null
    }

    private fun receive() = scope.launch(CoroutineName("Receive")) {
        val buf = Net.createPacketBuffer(Net.RECEIVE_BUFFER_CAPACITY)
        try {
            while (isActive) {
                buf.clear()
                dataChannel?.receive(buf)
                buf.flip()
                val header: PacketHeader? = PacketHeader.read(buf)
                when (header?.category) {
                    Net.PacketCategory.DISCONNECT.value -> processDisconnect()
                    Net.PacketCategory.AUDIO_DATA_OPUS.value -> processAudioData(buf, false)
                    Net.PacketCategory.AUDIO_DATA_UNCOMPRESSED.value -> processAudioData(buf, true)
                    Net.PacketCategory.SERVER_KEEP_ALIVE.value -> updateServerLastContact()
                    Net.PacketCategory.ACK.value -> processAck(buf)
                    else -> {}
                }
            }
        } catch (_: AsynchronousCloseException) {
        }
    }

    private fun keepAlive() = scope.launch(CoroutineName("KeepAlive")) {
        serverLastContact.set(System.nanoTime())
        while (isActive) {
            delay(1000L)
            val now = System.nanoTime()
            val elapsedNanos = now - serverLastContact.get()
            val elapsedSeconds = TimeUnit.SECONDS.convert(elapsedNanos, TimeUnit.NANOSECONDS)
            if (elapsedSeconds >= Net.SERVER_TIMEOUT_SECONDS) {
                when (currentStatus) {
                    ConnectionStatus.CONNECTING -> sendMessage(SystemMessage.MESSAGE_CONNECT_FAILED)
                    ConnectionStatus.CONNECTED -> sendMessage(SystemMessage.MESSAGE_DISCONNECTED)
                    else -> Unit
                }
                shutdown()
            }
            send(Net.getKeepAlivePacket())
            maintainPendingRequests(now)
        }
    }

    private suspend fun send(data: ByteBuffer) = withContext(scope.coroutineContext) {
        synchronized(sendLock) {
            serverAddress?.let { address ->
                sendChannel?.send(data, address)
            }
        }
    }

    private fun sendMessage(message: SystemMessage) = scope.launch {
        connectionMessages.send(message)
    }

    private suspend fun sendConnect(@Net.Compression compression: Int) {
        val request = Request()
        val packet = Net.getConnectPacket(compression, request.id)
        send(packet)
        synchronized(pendingRequestsLock) {
            pendingRequests[Net.PacketCategory.CONNECT] = request
        }
    }

    private fun updateServerLastContact() {
        if (currentStatus != ConnectionStatus.CONNECTED) return
        serverLastContact.set(System.nanoTime())
    }

    private suspend fun processAudioData(buffer: ByteBuffer, uncompressed: Boolean) {
        if (currentStatus != ConnectionStatus.CONNECTED || !processAudio.get()) return
        val packetData = ByteArray(buffer.remaining())
        buffer.get(packetData)
        if (uncompressed) {
            uncompressedAudio.send(packetData)
        } else {
            opusAudio.send(packetData)
        }
        updateServerLastContact()
    }

    private fun processAck(buffer: ByteBuffer) = synchronized(pendingRequestsLock) {
        if (pendingRequests.isEmpty()) return
        val ackData = AckData.read(buffer) ?: return
        val i = pendingRequests.iterator()
        while (i.hasNext()) {
            val (category, request) = i.next()
            if (request.id == ackData.requestId) {
                when (category) {
                    Net.PacketCategory.CONNECT -> processAckConnect(ackData.customData)

                    // TODO: Process format change acknowledgement
                    Net.PacketCategory.SET_FORMAT -> {}

                    else -> {}
                }
                i.remove()
                return
            }
        }
    }

    /**
     * Process ACK response on a Connect request.
     * @param buffer [ByteBuffer] must be positioned on ACK packet custom data.
     */
    private fun processAckConnect(buffer: ByteBuffer) {
        synchronized(connectLock) {
            if (currentStatus == ConnectionStatus.CONNECTING) {
                currentStatus = ConnectionStatus.CONNECTED
            }
        }
        val ackConnectResponse = AckConnectData.read(buffer)
        if (ackConnectResponse != null) {
            serverProtocol = ackConnectResponse.protocol
        }
    }

    private suspend fun processDisconnect() {
        shutdown()
    }

    /**
     * Removes pending requests older than 1 second
     */
    private fun maintainPendingRequests(now: Long) = synchronized(pendingRequestsLock) {
        val i = pendingRequests.iterator()
        while (i.hasNext()) {
            val (_, request) = i.next()
            val elapsedNanos = now - request.sentAt
            val elapsedSeconds = TimeUnit.SECONDS.convert(elapsedNanos, TimeUnit.NANOSECONDS)
            if (elapsedSeconds > 1) {
                i.remove()
            }
        }
    }

    companion object {
        fun createSendChannel(): DatagramChannel {
            return DatagramChannel.open()
        }

        /**
         * Creates a bound [DatagramChannel]
         *
         * @param  bindAddress Address to bind to
         *
         * @throws AlreadyBoundException
         * @throws SecurityException
         * @throws IOException
         */
        fun createReceiveChannel(bindAddress: InetSocketAddress): DatagramChannel {
            val channel = DatagramChannel.open()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                channel.setOption(StandardSocketOptions.SO_REUSEADDR, true)
                channel.bind(bindAddress)
            } else {
                channel.socket()?.bind(bindAddress)
            }
            return channel
        }
    }
}

private data class Request(
    val id: UShort = Random.nextInt(0, UShort.MAX_VALUE.toInt()).toUShort(),
    val sentAt: Long = System.nanoTime()
)
