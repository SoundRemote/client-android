package io.github.soundremote.network

import io.github.soundremote.network.Connection.Companion.createReceiveChannel
import io.github.soundremote.network.Connection.Companion.createSendChannel
import io.github.soundremote.util.ConnectionStatus
import io.github.soundremote.util.Net
import io.github.soundremote.util.Net.PROTOCOL_VERSION
import io.github.soundremote.util.Net.putUByte
import io.github.soundremote.util.Net.putUShort
import io.github.soundremote.util.Net.uShort
import io.github.soundremote.util.PacketRequestIdType
import io.github.soundremote.util.SystemMessage
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockKExtension::class)
@DisplayName("Connection")
internal class ConnectionTest {

    @RelaxedMockK
    private lateinit var sendChannel: DatagramChannel

    @RelaxedMockK
    private lateinit var receiveChannel: DatagramChannel

    @MockK
    private lateinit var audioChannel: SendChannel<ByteBuffer>

    private val losses = AtomicInteger()

    @MockK
    private lateinit var messageChannel: SendChannel<SystemMessage>

    @BeforeAll
    fun beforeTests() {
        mockkObject(Connection)
        every { createSendChannel() } returns sendChannel
        every { createReceiveChannel(bindAddress) } returns receiveChannel
        coEvery { audioChannel.send(any()) } just Runs
        coEvery { messageChannel.send(any()) } just Runs
    }

    @AfterAll
    fun afterTests() {
        unmockkObject(Connection)
    }

    @DisplayName("Has \"disconnected\" status on creation")
    @Test
    fun constructor_CreatesWithDisconnectedStatus() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)

        val connection = createConnection(this, testDispatcher)

        val actual = connection.connectionStatus.value
        actual shouldBe ConnectionStatus.DISCONNECTED
    }

    @DisplayName("Changes status to \"connecting\" on connection attempt")
    @Test
    fun connect_ChangesStatusToConnecting() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)

        val connection = createConnection(this, testDispatcher)
        connection.connect(ADDRESS, SERVER_PORT, LOCAL_PORT, COMPRESSION)

        val actual = connection.connectionStatus.value
        actual shouldBe ConnectionStatus.CONNECTING
    }

    @DisplayName("Changes status to \"disconnected\" on disconnect after connection attempt")
    @Test
    fun disconnect_ChangesStatusToDisconnected() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)

        val connection = createConnection(this, testDispatcher)
        connection.connect(ADDRESS, SERVER_PORT, LOCAL_PORT, COMPRESSION)
        connection.disconnect()

        val actual = connection.connectionStatus.value
        actual shouldBe ConnectionStatus.DISCONNECTED
    }

    @DisplayName("connect() sends datagrams to the server")
    @Test
    fun connect_SendsDatagram() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)

        val connection = createConnection(this, testDispatcher)
        connection.connect(ADDRESS, SERVER_PORT, LOCAL_PORT, COMPRESSION)
        advanceUntilIdle()

        verify(exactly = 3) { sendChannel.send(any(ByteBuffer::class), serverAddress) }
    }

    @Disabled
    @DisplayName("Changes status after receiving ACK connect datagram and disconnect datagram")
    @Test
    fun receives_AckConnectAndDisconnect() = runTest {
        var requestId: PacketRequestIdType? = null
        // Capture requestId from outgoing connect requests
        val request = slot<ByteBuffer>()
        every { sendChannel.send(capture(request), any()) } answers {
            val result = request.captured.remaining()
            requestId = getConnectRequestId(request.captured)
            result
        }
        // Imitate server response with the captured requestId.
        // When `disconnect` flag is set, respond with disconnect datagram
        val disconnect = AtomicBoolean(false)
        val response = slot<ByteBuffer>()
        every { receiveChannel.receive(capture(response)) } answers {
            if (disconnect.get()) {
                PacketHeader(Net.PacketCategory.DISCONNECT, PacketHeader.SIZE)
                    .write(response.captured)
            } else {
                requestId?.let {
                    writeConnectResponse(it, response.captured)
                    requestId = null
                }
            }
            serverAddress
        }

        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val connection = createConnection(this, testDispatcher)
        val expectedStatuses = listOf(
            ConnectionStatus.DISCONNECTED,
            ConnectionStatus.CONNECTING,
            ConnectionStatus.CONNECTED,
            ConnectionStatus.DISCONNECTED
        )
        var currentExpectedStatus = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            connection.connectionStatus.collect { actual ->
                actual shouldBe expectedStatuses[currentExpectedStatus]
                currentExpectedStatus++
                // Disconnect after getting connected
                if (actual == ConnectionStatus.CONNECTED) {
                    disconnect.set(true)
                }
            }
        }
        connection.connect(ADDRESS, SERVER_PORT, LOCAL_PORT, COMPRESSION)
        advanceUntilIdle()
    }

    // --- Utility ---

    private fun createConnection(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher,
    ): Connection {
        return Connection(audioChannel, audioChannel, losses, messageChannel, scope, dispatcher)
    }

    private fun getConnectRequestId(source: ByteBuffer): PacketRequestIdType? {
        val header = PacketHeader.read(source)
        if (header?.category != Net.PacketCategory.CONNECT.value ||
            source.remaining() < ConnectData.SIZE
        ) {
            return null
        }
        source.get()
        return source.uShort
    }

    private fun writeConnectResponse(requestId: PacketRequestIdType, dest: ByteBuffer) {
        val packetSize = AckData.SIZE + PacketHeader.SIZE
        PacketHeader(Net.PacketCategory.ACK, packetSize)
            .write(dest)
        dest.putUShort(requestId)
        dest.putUByte(PROTOCOL_VERSION)
        dest.position(packetSize)
    }

    companion object {
        @Net.Compression
        private const val COMPRESSION = Net.COMPRESSION_320
        private const val ADDRESS = "123.45.67.89"
        private const val SERVER_PORT = 30_000
        private const val LOCAL_PORT = 40_000
        private val bindAddress = InetSocketAddress(LOCAL_PORT)
        private val serverAddress = InetSocketAddress(ADDRESS, SERVER_PORT)
    }
}
