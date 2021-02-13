package org.openrs2.cache

import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import org.openrs2.buffer.use
import org.openrs2.crypto.Rsa
import org.openrs2.crypto.Whirlpool
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Js5MasterIndexTest {
    @Test
    fun testCreateOriginal() {
        val index = Store.open(ROOT.resolve("original")).use { store ->
            Js5MasterIndex.create(store)
        }

        assertEquals(
            Js5MasterIndex(
                MasterIndexFormat.ORIGINAL,
                mutableListOf(
                    Js5MasterIndex.Entry(
                        0, 609698396, 0, 0, ByteBufUtil.decodeHexDump(
                            "0e1a2b93c80a41c7ad2a985dff707a6a8ff82e229cbc468f04191198920955a1" +
                                "4b3d7eab77a17faf99208dee5b44afb789962ad79f230b3b59106a0af892219c"
                        )
                    ),
                )
            ), index
        )
    }

    @Test
    fun testCreateVersioned() {
        val index = Store.open(ROOT.resolve("versioned")).use { store ->
            Js5MasterIndex.create(store)
        }

        assertEquals(
            Js5MasterIndex(
                MasterIndexFormat.VERSIONED,
                mutableListOf(
                    Js5MasterIndex.Entry(
                        0, 609698396, 0, 0, ByteBufUtil.decodeHexDump(
                            "0e1a2b93c80a41c7ad2a985dff707a6a8ff82e229cbc468f04191198920955a1" +
                                "4b3d7eab77a17faf99208dee5b44afb789962ad79f230b3b59106a0af892219c"
                        )
                    ),
                    Js5MasterIndex.Entry(
                        0x12345678, 78747481, 0, 0, ByteBufUtil.decodeHexDump(
                            "180ff4ad371f56d4a90d81e0b69b23836cd9b101b828f18b7e6d232c4d302539" +
                                "638eb2e9259957645aae294f09b2d669c93dbbfc0d8359f1b232ae468f678ca1"
                        )
                    ),
                    Js5MasterIndex.Entry(0, 0, 0, 0, null),
                    Js5MasterIndex.Entry(
                        0x9ABCDEF0.toInt(), -456081154, 0, 0, ByteBufUtil.decodeHexDump(
                            "972003261b7628525346e0052567662e5695147ad710f877b63b9ab53b3f6650" +
                                "ca003035fde4398b2ef73a60e4b13798aa597a30c1bf0a13c0cd412394af5f96"
                        )
                    ),
                    Js5MasterIndex.Entry(0, 0, 0, 0, null),
                    Js5MasterIndex.Entry(0, 0, 0, 0, null),
                    Js5MasterIndex.Entry(
                        0xAA55AA55.toInt(), 186613982, 0, 0, ByteBufUtil.decodeHexDump(
                            "d50a6e9abd3b5269606304dc2769cbc8618e1ae6ff705291c0dfcc374e450dd2" +
                                "5f1be5f1d5459651d22d3e87ef0a1c69be7807f661cd001be24a6609f6d57916"
                        )
                    )
                )
            ), index
        )
    }

    @Test
    fun testCreateWhirlpool() {
        val index = Store.open(ROOT.resolve("whirlpool")).use { store ->
            Js5MasterIndex.create(store)
        }

        assertEquals(
            Js5MasterIndex(
                MasterIndexFormat.WHIRLPOOL,
                mutableListOf(
                    Js5MasterIndex.Entry(
                        0, 668177970, 0, 0, ByteBufUtil.decodeHexDump(
                            "2faa83116e1d1719d5db15f128eb57f62afbf0207c47bced3f558ec17645d138" +
                                "72f4fb9b0e36a5f6f5d30e1295b3fa49556dfd0819cb5137f3b69f64155f3fb7"
                        )
                    ),
                    Js5MasterIndex.Entry(
                        0, 1925442845, 0, 0, ByteBufUtil.decodeHexDump(
                            "fcc45b0ab6d0067889e44de0004bcbb6cc538aff8f80edf1b49b583cedd73fea" +
                                "937ae6990235257fe8aa35c44d35450c13e670711337ee5116957cd98cc27985"
                        )
                    )
                )
            ), index
        )
    }

    @Test
    fun testCreateLengths() {
        val index = Store.open(ROOT.resolve("lengths")).use { store ->
            Js5MasterIndex.create(store)
        }

        assertEquals(
            Js5MasterIndex(
                MasterIndexFormat.LENGTHS,
                mutableListOf(
                    Js5MasterIndex.Entry(
                        0x12345678, -1080883457, 3, 123, ByteBufUtil.decodeHexDump(
                            "0bf30b80b7213154ada5c3797be15a8fbb6a96a80432e2093e10617bcb4e67de" +
                                "9a858211cabe844c6fa3a1fbfe3164a3e4e1918983c69597dff3fc3c53096884"
                        )
                    )
                )
            ), index
        )
    }

    @Test
    fun testReadOriginal() {
        Unpooled.wrappedBuffer(encodedOriginal).use { buf ->
            val index = Js5MasterIndex.read(buf, MasterIndexFormat.ORIGINAL)
            assertEquals(decodedOriginal, index)
        }

        assertFailsWith<IllegalArgumentException> {
            Unpooled.wrappedBuffer(byteArrayOf(0)).use { buf ->
                Js5MasterIndex.read(buf, MasterIndexFormat.ORIGINAL)
            }
        }
    }

    @Test
    fun testWriteOriginal() {
        ByteBufAllocator.DEFAULT.buffer().use { actual ->
            decodedOriginal.write(actual)

            Unpooled.wrappedBuffer(encodedOriginal).use { expected ->
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun testReadVersioned() {
        Unpooled.wrappedBuffer(encodedVersioned).use { buf ->
            val index = Js5MasterIndex.read(buf, MasterIndexFormat.VERSIONED)
            assertEquals(decodedVersioned, index)
        }

        assertFailsWith<IllegalArgumentException> {
            Unpooled.wrappedBuffer(byteArrayOf(0, 0, 0, 0)).use { buf ->
                Js5MasterIndex.read(buf, MasterIndexFormat.VERSIONED)
            }
        }
    }

    @Test
    fun testWriteVersioned() {
        ByteBufAllocator.DEFAULT.buffer().use { actual ->
            decodedVersioned.write(actual)

            Unpooled.wrappedBuffer(encodedVersioned).use { expected ->
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun testReadWhirlpool() {
        Unpooled.wrappedBuffer(encodedWhirlpool).use { buf ->
            val index = Js5MasterIndex.read(buf, MasterIndexFormat.WHIRLPOOL)
            assertEquals(decodedWhirlpool, index)
        }
    }

    @Test
    fun testReadWhirlpoolInvalidSignature() {
        Unpooled.copiedBuffer(encodedWhirlpool).use { buf ->
            val lastIndex = buf.writerIndex() - 1
            buf.setByte(lastIndex, buf.getByte(lastIndex).toInt().inv())

            assertFailsWith<IllegalArgumentException> {
                Js5MasterIndex.read(buf, MasterIndexFormat.WHIRLPOOL)
            }
        }
    }

    @Test
    fun testReadWhirlpoolInvalidSignatureLength() {
        Unpooled.wrappedBuffer(encodedWhirlpool, 0, encodedWhirlpool.size - 1).use { buf ->
            assertFailsWith<IllegalArgumentException> {
                Js5MasterIndex.read(buf, MasterIndexFormat.WHIRLPOOL)
            }
        }
    }

    @Test
    fun testWriteWhirlpool() {
        ByteBufAllocator.DEFAULT.buffer().use { actual ->
            decodedWhirlpool.write(actual)

            Unpooled.wrappedBuffer(encodedWhirlpool).use { expected ->
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun testWriteWhirlpoolNullDigest() {
        ByteBufAllocator.DEFAULT.buffer().use { actual ->
            decodedWhirlpoolNullDigest.write(actual)

            Unpooled.wrappedBuffer(encodedWhirlpoolNullDigest).use { expected ->
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun testReadSigned() {
        Unpooled.wrappedBuffer(encodedSigned).use { buf ->
            val index = Js5MasterIndex.read(buf, MasterIndexFormat.WHIRLPOOL, PUBLIC_KEY)
            assertEquals(decodedWhirlpool, index)
        }
    }

    @Test
    fun testReadSignedInvalidSignature() {
        Unpooled.copiedBuffer(encodedSigned).use { buf ->
            val lastIndex = buf.writerIndex() - 1
            buf.setByte(lastIndex, buf.getByte(lastIndex).toInt().inv())

            assertFailsWith<IllegalArgumentException> {
                Js5MasterIndex.read(buf, MasterIndexFormat.WHIRLPOOL, PUBLIC_KEY)
            }
        }
    }

    @Test
    fun testReadSignedInvalidSignatureLength() {
        Unpooled.wrappedBuffer(encodedSigned, 0, encodedSigned.size - 1).use { buf ->
            assertFailsWith<IllegalArgumentException> {
                Js5MasterIndex.read(buf, MasterIndexFormat.WHIRLPOOL, PUBLIC_KEY)
            }
        }
    }

    @Test
    fun testWriteSigned() {
        ByteBufAllocator.DEFAULT.buffer().use { actual ->
            decodedWhirlpool.write(actual, PRIVATE_KEY)

            Unpooled.wrappedBuffer(encodedSigned).use { expected ->
                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun testReadLengths() {
        Unpooled.wrappedBuffer(encodedLengths).use { buf ->
            val index = Js5MasterIndex.read(buf, MasterIndexFormat.LENGTHS)
            assertEquals(decodedLengths, index)
        }
    }

    @Test
    fun testWriteLengths() {
        ByteBufAllocator.DEFAULT.buffer().use { actual ->
            decodedLengths.write(actual)

            Unpooled.wrappedBuffer(encodedLengths).use { expected ->
                assertEquals(expected, actual)
            }
        }
    }

    private companion object {
        private val ROOT = Path.of(FlatFileStoreTest::class.java.getResource("master-index").toURI())
        private val PRIVATE_KEY = Rsa.readPrivateKey(ROOT.resolve("private.key"))
        private val PUBLIC_KEY = Rsa.readPublicKey(ROOT.resolve("public.key"))

        private val encodedOriginal = ByteBufUtil.decodeHexDump("000000010000000300000005")
        private val decodedOriginal = Js5MasterIndex(
            MasterIndexFormat.ORIGINAL,
            mutableListOf(
                Js5MasterIndex.Entry(0, 1, 0, 0, null),
                Js5MasterIndex.Entry(0, 3, 0, 0, null),
                Js5MasterIndex.Entry(0, 5, 0, 0, null)
            )
        )

        private val encodedVersioned = ByteBufUtil.decodeHexDump("000000010000000000000003000000020000000500000004")
        private val decodedVersioned = Js5MasterIndex(
            MasterIndexFormat.VERSIONED,
            mutableListOf(
                Js5MasterIndex.Entry(0, 1, 0, 0, null),
                Js5MasterIndex.Entry(2, 3, 0, 0, null),
                Js5MasterIndex.Entry(4, 5, 0, 0, null)
            )
        )

        private val encodedWhirlpool = ByteBufUtil.decodeHexDump(
            "01" +
                "89abcdef" +
                "01234567" +
                "0e1a2b93c80a41c7ad2a985dff707a6a8ff82e229cbc468f04191198920955a1" +
                "4b3d7eab77a17faf99208dee5b44afb789962ad79f230b3b59106a0af892219c" +
                "0a" +
                "ee8f66a2ce0b07de4d2b792eed26ae7a6c307b763891d085c63ea55b4c003bc0" +
                "b3ecb77cc1a8f9ccd53c405b3264e598820b4940f630ff079a9feb950f639671"
        )
        private val decodedWhirlpool = Js5MasterIndex(
            MasterIndexFormat.WHIRLPOOL,
            mutableListOf(
                Js5MasterIndex.Entry(
                    0x01234567, 0x89ABCDEF.toInt(), 0, 0, ByteBufUtil.decodeHexDump(
                        "0e1a2b93c80a41c7ad2a985dff707a6a8ff82e229cbc468f04191198920955a1" +
                            "4b3d7eab77a17faf99208dee5b44afb789962ad79f230b3b59106a0af892219c"
                    )
                )
            )
        )

        private val encodedWhirlpoolNullDigest = ByteBufUtil.decodeHexDump(
            "01" +
                "89abcdef" +
                "01234567" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0a" +
                "4a0e22540fb0a9bc06fe84bfb35f9281ba9fbd30288c3375c508ad741c4d4491" +
                "8a65765bc2dce9d67029be79bd544f96055a41d725c080bc5b85a48b5aae6e4d"
        )
        private val decodedWhirlpoolNullDigest = Js5MasterIndex(
            MasterIndexFormat.WHIRLPOOL,
            mutableListOf(
                Js5MasterIndex.Entry(0x01234567, 0x89ABCDEF.toInt(), 0, 0, null)
            )
        )

        private val encodedSigned = ByteBufUtil.decodeHexDump(
            "01" +
                "89abcdef" +
                "01234567" +
                "0e1a2b93c80a41c7ad2a985dff707a6a8ff82e229cbc468f04191198920955a1" +
                "4b3d7eab77a17faf99208dee5b44afb789962ad79f230b3b59106a0af892219c" +
                "2134b1e637d4c9f3b7bdd446ad40cedb6d824cfb48f937ae0d6e2ba3977881ea" +
                "ed02adae179ed89cea56e98772186bb569bb24a4951e441716df0d5d7199c088" +
                "28974d43c3644e74bf29ec1435e425f6cb05aca14a84163c5b46b6e6a9362f22" +
                "4f69f4a5888b3fe7aec0141da25b17c7f65069eed59f3be134fa1ade4e191b41" +
                "d561447446cd1cc4d11e6499c49e00066173908491d8d2ff282aefa86e6c6b15" +
                "dceb437d0436b6195ef60d4128e1e0184bf6929b73abd1a8aa2a047e3cb90d03" +
                "57707ce3f4f5a7af8471eda5c0c0748454a9cbb48c25ebe4e7fd94e3881b6461" +
                "d06e2bce128dc96decb537b8e9611591d445d7dfd3701d25ac05f8d091581aef"
        )

        private val decodedLengths = Js5MasterIndex(
            MasterIndexFormat.LENGTHS,
            mutableListOf(
                Js5MasterIndex.Entry(0x012345678, 0x89ABCDEF.toInt(), 3, 123, ByteArray(Whirlpool.DIGESTBYTES))
            )
        )
        private val encodedLengths = ByteBufUtil.decodeHexDump(
            "01" +
                "89abcdef" +
                "12345678" +
                "00000003" +
                "0000007b" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0a" +
                "3d9f704a2b2e1b6f4e7b3a9b558baed7ccb10787a754b6fd36acb77ba3491726" +
                "fef29e470218a98693bfc1b98a611f15e0b35a11bd181830ff4912377653a87a"
        )
    }
}
