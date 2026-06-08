package io.github.soundremote.network

import android.os.Build
import io.github.soundremote.util.ConnectionStatus
import io.github.soundremote.util.KeyCode
import io.github.soundremote.util.Mods
import io.github.soundremote.util.Net
import io.github.soundremote.util.Net.calculateGap
import io.github.soundremote.util.Net.uInt
import io.github.soundremote.util.PacketProtocolType
import io.github.soundremote.util.SystemMessage
import kotlinx.coroutines.CoroutineDispatcher
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.AlreadyBoundException
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.ClosedChannelException
import java.nio.channels.DatagramChannel
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

internal class Connection(
    private val uncompressedAudio: SendChannel<ByteBuffer>,
    private val opusAudio: SendChannel<ByteBuffer>,
    private val packetsLost: AtomicInteger,
    private val connectionMessages: SendChannel<SystemMessage>,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private var connectJob: Job? = null
    private var receiveJob: Job? = null
    private var keepAliveJob: Job? = null
    private var pendingRequests = mutableMapOf<Net.PacketCategory, Request>()

    private var serverAddress: InetSocketAddress? = null
    private var dataChannel: DatagramChannel? = null
    private var sendChannel: DatagramChannel? = null

    /**
     * Sync modifications to `currentStatus`, `receiveJob`, `keepAliveJob`.
     */
    private val connectMutex = Mutex()

    /**
     * Sync modifications of `pendingRequests`.
     */
    private val pendingRequestsMutex = Mutex()

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

    private val _processAudio = AtomicBoolean(true)
    var processAudio: Boolean
        get() {
            return _processAudio.get()
        }
        set(value) {
            _processAudio.set(value)
            if (!value) {
                audioSequenceNumber = null
            }
        }

    private var audioSequenceNumber: UInt? = null

    suspend fun connect(
        address: String,
        serverPort: Int,
        localPort: Int,
        @Net.Compression compression: Int
    ) {
        shutdown()
        connectMutex.withLock {
            currentStatus = ConnectionStatus.CONNECTING
            try {
                serverAddress = InetSocketAddress(address, serverPort)
                sendChannel = createSendChannel()
                dataChannel = createReceiveChannel(InetSocketAddress(localPort))
            } catch (e: IllegalStateException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && e is AlreadyBoundException) {
                    sendMessage(SystemMessage.MESSAGE_ALREADY_BOUND)
                } else {
                    sendMessage(SystemMessage.MESSAGE_BIND_ERROR)
                }
                releaseChannels()
                currentStatus = ConnectionStatus.DISCONNECTED
                return
            } catch (_: Exception) {
                sendMessage(SystemMessage.MESSAGE_BIND_ERROR)
                releaseChannels()
                currentStatus = ConnectionStatus.DISCONNECTED
                return
            }
            receiveJob = receive()
        }
        connectJob = repeatConnect(compression)
    }

    /**
     * Sends the disconnect packet and closes the connection.
     */
    suspend fun disconnect() {
        send(Net.getDisconnectPacket())
        shutdown()
    }

    suspend fun sendSetFormat(@Net.Compression compression: Int) {
        val request = Request()
        val packet = Net.getSetFormatPacket(compression, request.id)
        send(packet)
        pendingRequestsMutex.withLock {
            pendingRequests[Net.PacketCategory.SET_FORMAT] = request
        }
    }

    suspend fun sendHotkey(keyCode: KeyCode, mods: Mods = Mods()) {
        val hotkeyPacket = Net.getHotkeyPacket(keyCode.value.toUByte(), mods.value.toUByte())
        send(hotkeyPacket)
    }

    private suspend fun shutdown() {
        connectMutex.withLock {
            if (currentStatus == ConnectionStatus.DISCONNECTED) return
            connectJob?.cancel()
            receiveJob?.cancel()
            keepAliveJob?.cancel()
            // Close channel after cancelling receiving job to avoid trying to invoke receive
            // from closed or null channel
            releaseChannels()
            currentStatus = ConnectionStatus.DISCONNECTED
            audioSequenceNumber = null
        }
    }

    private fun releaseChannels() {
        serverAddress = null
        sendChannel?.close()
        sendChannel = null
        dataChannel?.close()
        dataChannel = null
    }

    private fun receive() = scope.launch(CoroutineName("Receive") + dispatcher) {
        try {
            while (isActive) {
                val buf = Net.createPacketBuffer(Net.RECEIVE_BUFFER_CAPACITY)
                dataChannel?.receive(buf)
                buf.flip()
                val header: PacketHeader? = PacketHeader.read(buf)
                when (header?.category) {
                    Net.PacketCategory.DISCONNECT.value -> shutdown()
                    Net.PacketCategory.AUDIO_DATA_OPUS.value -> processAudioData(buf, true)
                    Net.PacketCategory.AUDIO_DATA_UNCOMPRESSED.value -> processAudioData(buf, false)
                    Net.PacketCategory.SERVER_KEEP_ALIVE.value -> updateServerLastContact()
                    Net.PacketCategory.ACK.value -> processAck(buf)
                    // For tests - yield the test dispatcher and advance time
                    else -> delay(10)
                }
            }
        } catch (_: AsynchronousCloseException) {
        }
    }

    private fun repeatConnect(compression: Int) = scope.launch(CoroutineName("Repeat connect")) {
        var attempts = 0
        while (isActive && attempts < 3) {
            sendConnect(compression)
            attempts++
            delay(1000L)
        }
        if (currentStatus != ConnectionStatus.CONNECTED) {
            sendMessage(SystemMessage.MESSAGE_CONNECT_FAILED)
            shutdown()
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
                sendMessage(SystemMessage.MESSAGE_DISCONNECTED)
                shutdown()
            }
            send(Net.getKeepAlivePacket())
            maintainPendingRequests(now)
        }
    }

    private suspend fun send(data: ByteBuffer) = withContext(dispatcher) {
        try {
            serverAddress?.let { address ->
                sendChannel?.send(data, address)
            }
        } catch (_: AsynchronousCloseException) {
            shutdown()
        } catch (_: ClosedChannelException) {
            shutdown()
        } catch (_: ClosedByInterruptException) {
            shutdown()
        }
    }

    private suspend fun sendMessage(message: SystemMessage) {
        connectionMessages.send(message)
    }

    private suspend fun sendConnect(@Net.Compression compression: Int) {
        val request = Request()
        val packet = Net.getConnectPacket(compression, request.id)
        send(packet)
        pendingRequestsMutex.withLock {
            pendingRequests[Net.PacketCategory.CONNECT] = request
        }
    }

    private fun updateServerLastContact() {
        if (currentStatus != ConnectionStatus.CONNECTED) return
        serverLastContact.set(System.nanoTime())
    }

    private suspend fun processAudioData(buffer: ByteBuffer, compressed: Boolean) {
        if (currentStatus != ConnectionStatus.CONNECTED || !processAudio) return

        val sequenceNumber = buffer.uInt
        processAudioSequenceNumber(sequenceNumber)

        if (compressed) {
            opusAudio.send(buffer)
        } else {
            uncompressedAudio.send(buffer)
        }
        updateServerLastContact()
    }

    private fun processAudioSequenceNumber(current: UInt) {
        val previous = audioSequenceNumber
        if (previous == null) {
            audioSequenceNumber = current
            return
        }
        val gap = calculateGap(previous, current)
        if (gap == 0) {
            audioSequenceNumber = current
        } else if (gap > 0) {
            Timber.i("Audio packets loss: $gap ($previous -> $current)")
            packetsLost.addAndGet(gap)
            audioSequenceNumber = current
        } else {
            Timber.i("Audio packets invalid order: $audioSequenceNumber -> $current")
        }
    }

    private suspend fun processAck(buffer: ByteBuffer): Unit = pendingRequestsMutex.withLock {
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
    private suspend fun processAckConnect(buffer: ByteBuffer) {
        connectMutex.withLock {
            if (currentStatus == ConnectionStatus.CONNECTING) {
                currentStatus = ConnectionStatus.CONNECTED
                connectJob?.cancel()
                keepAliveJob = keepAlive()
            }
        }
        val ackConnectResponse = AckConnectData.read(buffer)
        if (ackConnectResponse != null) {
            serverProtocol = ackConnectResponse.protocol
        }
    }

    /**
     * Removes pending requests older than 1 second
     */
    private suspend fun maintainPendingRequests(now: Long) = pendingRequestsMutex.withLock {
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
