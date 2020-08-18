package dev.openrs2.crypto

import io.netty.buffer.ByteBufUtil
import org.junit.jupiter.api.Assertions.assertArrayEquals
import kotlin.streams.toList
import kotlin.test.Test

object WhirlpoolTest {
    private class IsoTestVector(input: String, expected: String) {
        val input = input.toByteArray(Charsets.US_ASCII)
        val expected = ByteBufUtil.decodeHexDump(expected)
    }

    private val ISO_TEST_VECTORS = listOf(
        IsoTestVector(
            "",
            "19FA61D75522A4669B44E39C1D2E1726C530232130D407F89AFEE0964997F7A7" +
                "3E83BE698B288FEBCF88E3E03C4F0757EA8964E59B63D93708B138CC42A66EB3"
        ),
        IsoTestVector(
            "a",
            "8ACA2602792AEC6F11A67206531FB7D7F0DFF59413145E6973C45001D0087B42" +
                "D11BC645413AEFF63A42391A39145A591A92200D560195E53B478584FDAE231A"
        ),
        IsoTestVector(
            "abc",
            "4E2448A4C6F486BB16B6562C73B4020BF3043E3A731BCE721AE1B303D97E6D4C" +
                "7181EEBDB6C57E277D0E34957114CBD6C797FC9D95D8B582D225292076D4EEF5"
        ),
        IsoTestVector(
            "message digest",
            "378C84A4126E2DC6E56DCC7458377AAC838D00032230F53CE1F5700C0FFB4D3B" +
                "8421557659EF55C106B4B52AC5A4AAA692ED920052838F3362E86DBD37A8903E"
        ),
        IsoTestVector(
            "abcdefghijklmnopqrstuvwxyz",
            "F1D754662636FFE92C82EBB9212A484A8D38631EAD4238F5442EE13B8054E41B" +
                "08BF2A9251C30B6A0B8AAE86177AB4A6F68F673E7207865D5D9819A3DBA4EB3B"
        ),
        IsoTestVector(
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789",
            "DC37E008CF9EE69BF11F00ED9ABA26901DD7C28CDEC066CC6AF42E40F82F3A1E" +
                "08EBA26629129D8FB7CB57211B9281A65517CC879D7B962142C65F5A7AF01467"
        ),
        IsoTestVector(
            "1234567890".repeat(8),
            "466EF18BABB0154D25B9D38A6414F5C08784372BCCB204D6549C4AFADB601429" +
                "4D5BD8DF2A6C44E538CD047B2681A51A2C60481E88C5A20B2C2A80CF3A9A083B"
        ),
        IsoTestVector(
            "abcdbcdecdefdefgefghfghighijhijk",
            "2A987EA40F917061F5D6F0A0E4644F488A7A5A52DEEE656207C562F988E95C69" +
                "16BDC8031BC5BE1B7B947639FE050B56939BAAA0ADFF9AE6745B7B181C3BE3FD"
        ),
        IsoTestVector(
            "a".repeat(1000000),
            "0C99005BEB57EFF50A7CF005560DDF5D29057FD86B20BFD62DECA0F1CCEA4AF5" +
                "1FC15490EDDC47AF32BB2B66C34FF9AD8C6008AD677F77126953B226E4ED8B01"
        )
    )

    private val NESSIE_ZERO_VECTORS = readVectors("nessie-zero-vectors.txt")
    private val NESSIE_ONE_VECTORS = readVectors("nessie-one-vectors.txt")

    private fun readVectors(file: String): List<ByteArray> {
        val input = WhirlpoolTest::class.java.getResourceAsStream("whirlpool/$file")
        return input.bufferedReader().use { reader ->
            reader.lines()
                .map(ByteBufUtil::decodeHexDump)
                .toList()
        }
    }

    @Test
    fun testIsoVectors() {
        for (vector in ISO_TEST_VECTORS) {
            val actual = Whirlpool.whirlpool(vector.input)
            assertArrayEquals(vector.expected, actual)
        }
    }

    @Test
    fun testNessieZeroVectors() {
        for ((i, vector) in NESSIE_ZERO_VECTORS.withIndex()) {
            val whirlpool = Whirlpool()
            whirlpool.NESSIEinit()
            whirlpool.NESSIEadd(ByteArray((i + 7) / 8), i.toLong())

            val digest = ByteArray(64)
            whirlpool.NESSIEfinalize(digest)
            assertArrayEquals(vector, digest)
        }
    }

    @Test
    fun testNessieOneVectors() {
        for ((i, vector) in NESSIE_ONE_VECTORS.withIndex()) {
            val bytePos = i / 8
            val bitPos = i % 8

            val input = ByteArray(64)
            input[bytePos] = (input[bytePos].toInt() or (0x80 ushr bitPos)).toByte()

            val actual = Whirlpool.whirlpool(input)
            assertArrayEquals(vector, actual)
        }
    }
}
