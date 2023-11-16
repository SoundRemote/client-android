package com.fake.soundremote.network

import com.fake.soundremote.util.Net
import com.fake.soundremote.util.Net.putUByte
import com.fake.soundremote.util.Net.putUShort
import com.fake.soundremote.util.PacketCategoryType
import com.fake.soundremote.util.PacketSignatureType
import com.fake.soundremote.util.PacketSizeType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.ByteBuffer
import java.util.stream.Stream

@DisplayName("PacketHeader")
internal class PacketHeaderTest {

    @DisplayName("read")
    @Nested
    inner class ReadHeaderTests {

        @DisplayName("Returns null given invalid datagram")
        @ParameterizedTest
        @MethodSource("com.fake.soundremote.network.PacketHeaderTest#invalidDatagramProvider")
        fun readInvalidDatagram_ReturnsNull(buffer: ByteBuffer) {
            val actual = PacketHeader.read(buffer)

            assertNull(actual)
        }

        @DisplayName("Returns correct packet header given valid datagram")
        @ParameterizedTest
        @MethodSource("com.fake.soundremote.network.PacketHeaderTest#validDatagramProvider")
        fun readValidDatagram_ReturnsCorrectHeader(buffer: ByteBuffer, expected: PacketHeader) {
            val actual = PacketHeader.read(buffer)

            assertEquals(expected, actual)
        }
    }

    @DisplayName("write")
    @Nested
    inner class WriteHeaderTests {
        @DisplayName("Writes correctly")
        @Test
        fun write_WritesCorrectly() {
            val size: PacketSizeType = 12u
            val expected = generateByteBuffer(
                capacity = size.toInt(),
                signature = Net.PROTOCOL_SIGNATURE,
                category = Net.PacketCategory.ACK.value,
                size = size,
            )

            val actual = Net.createPacketBuffer(size.toInt())
            PacketHeader(Net.PacketCategory.ACK, size.toInt()).write(actual)
            actual.rewind()

            assertTrue(expected == actual)
        }
    }

    companion object {
        @JvmStatic
        fun invalidDatagramProvider(): Stream<Arguments> {
            return Stream.of(
                // Empty buffer
                Arguments.arguments(generateEmptyByteBuffer(0)),
                // Size less than header size
                Arguments.arguments(
                    generateEmptyByteBuffer(PacketHeader.SIZE - 1)
                        .putUShort(Net.PROTOCOL_SIGNATURE)
                ),
                // Mismatched size field value and packet size
                Arguments.arguments(
                    generateByteBuffer(
                        capacity = 100,
                        signature = Net.PROTOCOL_SIGNATURE,
                        category = 20u,
                        size = 101u
                    )
                ),
                // Incorrect signature
                Arguments.arguments(
                    generateByteBuffer(
                        capacity = 50,
                        signature = (Net.PROTOCOL_SIGNATURE + 1u).toUShort(),
                        category = 20u,
                        size = 50u,
                    )
                ),
            )
        }

        @JvmStatic
        fun validDatagramProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.arguments(
                    generateByteBuffer(
                        capacity = 100,
                        signature = Net.PROTOCOL_SIGNATURE,
                        category = 20u,
                        size = 100u
                    ), PacketHeader(20u, 100)
                ),
                // Type byte value > Byte.MAX_VALUE
                Arguments.arguments(
                    generateByteBuffer(
                        capacity = 50,
                        signature = Net.PROTOCOL_SIGNATURE,
                        category = 200u,
                        size = 50u,
                    ), PacketHeader(200u, 50)
                ),
            )
        }

        private fun generateEmptyByteBuffer(capacity: Int): ByteBuffer {
            return Net.createPacketBuffer(capacity)
        }

        private fun generateByteBuffer(
            capacity: Int,
            signature: PacketSignatureType,
            category: PacketCategoryType,
            size: PacketSizeType,
        ): ByteBuffer {
            val result = Net.createPacketBuffer(capacity)
                .putUShort(signature)
                .putUByte(category)
                .putUShort(size)
            result.rewind()
            return result
        }
    }
}
