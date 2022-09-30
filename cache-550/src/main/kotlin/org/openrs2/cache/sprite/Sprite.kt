package org.openrs2.cache.sprite

import io.netty.buffer.ByteBuf
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet
import java.awt.image.BufferedImage

public class Sprite private constructor(
    public val width: Int,
    public val height: Int,
    private val palette: IntArray,
    private val frames: Array<Frame>
) {
    private class Frame(
        val xOffset: Int,
        val yOffset: Int,
        val innerWidth: Int,
        val innerHeight: Int
    ) {
        val pixels = ByteArray(innerWidth * innerHeight)
        var alpha: ByteArray? = null

        fun toImage(width: Int, height: Int, palette: IntArray): BufferedImage {
            val rgbPixels = IntArray(width * height)

            var index = 0
            for (y in 0 until innerHeight) {
                for (x in 0 until innerWidth) {
                    val paletteIndex = pixels[index].toInt() and 0xFF

                    var color = if (paletteIndex == 0) {
                        0
                    } else {
                        palette[paletteIndex - 1]
                    }

                    val alpha = alpha
                    if (alpha != null) {
                        color = color or ((alpha[index].toInt() and 0xFF) shl 24)
                    } else if (paletteIndex != 0) {
                        color = color or 0xFF000000.toInt()
                    }

                    rgbPixels[(y + yOffset) * width + x + xOffset] = color
                    index++
                }
            }

            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            image.setRGB(0, 0, width, height, rgbPixels, 0, width)
            return image
        }

        fun write(buf: ByteBuf) {
            var flags = 0

            val columnMajor = isColumnMajorBest()
            if (columnMajor) {
                flags = flags or FLAG_COLUMN_MAJOR
            }

            val alpha = alpha
            if (alpha != null) {
                flags = flags or FLAG_ALPHA
            }

            buf.writeByte(flags)

            if (columnMajor) {
                for (x in 0 until innerWidth) {
                    for (y in 0 until innerHeight) {
                        buf.writeByte(pixels[y * innerWidth + x].toInt())
                    }
                }

                if (alpha != null) {
                    for (x in 0 until innerWidth) {
                        for (y in 0 until innerHeight) {
                            buf.writeByte(alpha[y * innerWidth + x].toInt())
                        }
                    }
                }
            } else {
                buf.writeBytes(pixels)

                if (alpha != null) {
                    buf.writeBytes(alpha)
                }
            }
        }

        private fun isColumnMajorBest(): Boolean {
            var rowMajorScore = 0
            var columnMajorScore = 0

            // calculate row-major score
            var prev = 0
            for (pixel in pixels) {
                val current = pixel.toInt() and 0xFF
                rowMajorScore += current - prev
                prev = current
            }

            val alpha = alpha
            if (alpha != null) {
                for (a in alpha) {
                    val current = a.toInt() and 0xFF
                    rowMajorScore += current - prev
                    prev = current
                }
            }

            // calculate column-major score
            prev = 0
            for (x in 0 until innerWidth) {
                for (y in 0 until innerHeight) {
                    val current = pixels[y * innerWidth + x].toInt() and 0xFF
                    columnMajorScore += current - prev
                    prev = current
                }
            }

            if (alpha != null) {
                for (x in 0 until innerWidth) {
                    for (y in 0 until innerHeight) {
                        val current = alpha[y * innerWidth + x].toInt() and 0xFF
                        columnMajorScore += current - prev
                        prev = current
                    }
                }
            }

            // if equal pick row-major, as it's faster to encode/decode
            return columnMajorScore < rowMajorScore
        }

        companion object {
            fun read(buf: ByteBuf, xOffset: Int, yOffset: Int, innerWidth: Int, innerHeight: Int): Frame {
                val frame = Frame(xOffset, yOffset, innerWidth, innerHeight)

                val flags = buf.readUnsignedByte().toInt()

                val alpha = if ((flags and FLAG_ALPHA) != 0) {
                    ByteArray(frame.pixels.size)
                } else {
                    null
                }

                if ((flags and FLAG_COLUMN_MAJOR) != 0) {
                    for (x in 0 until frame.innerWidth) {
                        for (y in 0 until frame.innerHeight) {
                            frame.pixels[y * frame.innerWidth + x] = buf.readByte()
                        }
                    }

                    if (alpha != null) {
                        for (x in 0 until frame.innerWidth) {
                            for (y in 0 until frame.innerHeight) {
                                alpha[y * frame.innerWidth + x] = buf.readByte()
                            }
                        }
                    }
                } else {
                    buf.readBytes(frame.pixels)

                    if (alpha != null) {
                        buf.readBytes(alpha)
                    }
                }

                frame.alpha = alpha
                return frame
            }
        }
    }

    public fun write(buf: ByteBuf) {
        for (frame in frames) {
            frame.write(buf)
        }

        for (color in palette) {
            buf.writeMedium(color)
        }

        buf.writeShort(width)
        buf.writeShort(height)
        buf.writeByte(palette.size)

        for (frame in frames) {
            buf.writeShort(frame.xOffset)
        }

        for (frame in frames) {
            buf.writeShort(frame.yOffset)
        }

        for (frame in frames) {
            buf.writeShort(frame.innerWidth)
        }

        for (frame in frames) {
            buf.writeShort(frame.innerHeight)
        }

        buf.writeShort(frames.size)
    }

    public fun toImages(): List<BufferedImage> {
        return frames.map { frame ->
            frame.toImage(width, height, palette)
        }
    }

    public companion object {
        private const val FLAG_COLUMN_MAJOR = 0x1
        private const val FLAG_ALPHA = 0x2

        public fun read(buf: ByteBuf): Sprite {
            val framesSize = buf.getUnsignedShort(buf.writerIndex() - 2)
            var trailerLen = framesSize * 8 + 7

            buf.markReaderIndex()
            buf.readerIndex(buf.writerIndex() - trailerLen)

            val width = buf.readUnsignedShort()
            val height = buf.readUnsignedShort()
            val paletteSize = buf.readUnsignedByte().toInt()
            trailerLen += paletteSize * 3

            val xOffsets = IntArray(framesSize) { buf.readUnsignedShort() }
            val yOffsets = IntArray(framesSize) { buf.readUnsignedShort() }
            val innerWidths = IntArray(framesSize) { buf.readUnsignedShort() }
            val innerHeights = IntArray(framesSize) { buf.readUnsignedShort() }

            buf.readerIndex(buf.writerIndex() - trailerLen)

            val palette = IntArray(paletteSize) { buf.readUnsignedMedium() }

            buf.resetReaderIndex()

            val frames = Array(framesSize) { i ->
                Frame.read(buf, xOffsets[i], yOffsets[i], innerWidths[i], innerHeights[i])
            }

            buf.skipBytes(trailerLen)

            return Sprite(width, height, palette, frames)
        }

        public fun fromImage(image: BufferedImage): Sprite {
            return fromImages(listOf(image))
        }

        public fun fromImages(images: List<BufferedImage>): Sprite {
            require(images.isNotEmpty())

            val first = images.first()
            val width = first.width
            val height = first.height

            val alphaChannels = BooleanArray(images.size)
            for ((i, image) in images.withIndex()) {
                require(image.width == width && image.height == height)
                alphaChannels[i] = image.hasAlphaChannel()
            }

            val colors = IntAVLTreeSet()
            for ((i, image) in images.withIndex()) {
                val alphaChannel = alphaChannels[i]

                for (y in 0 until image.height) {
                    for (x in 0 until image.width) {
                        val rgb = image.getRGB(x, y)

                        // transparent colours only preserved if FLAG_ALPHA set
                        val alpha = (rgb shr 24) and 0xFF
                        if (alphaChannel || alpha != 0) {
                            val color = rgb and 0xFFFFFF
                            colors += color
                        }
                    }
                }
            }

            require(colors.size <= 255)

            val palette = colors.toIntArray()

            val frames = Array(images.size) { i ->
                val image = images[i]
                val alphaChannel = alphaChannels[i]

                var xOffset = 0
                var innerWidth = width

                for (x in 0 until width) {
                    if (!image.isColumnTransparent(x, alphaChannel)) {
                        break
                    }

                    xOffset++
                    innerWidth--
                }

                for (x in width - 1 downTo xOffset) {
                    if (!image.isColumnTransparent(x, alphaChannel)) {
                        break
                    }

                    innerWidth--
                }

                var yOffset = 0
                var innerHeight = height

                for (y in 0 until height) {
                    if (!image.isRowTransparent(y, alphaChannel)) {
                        break
                    }

                    yOffset++
                    innerHeight--
                }

                for (y in height - 1 downTo yOffset) {
                    if (!image.isRowTransparent(y, alphaChannel)) {
                        break
                    }

                    innerHeight--
                }

                val frame = Frame(xOffset, yOffset, innerWidth, innerHeight)
                if (alphaChannel) {
                    frame.alpha = ByteArray(innerWidth * innerHeight)
                }

                val alpha = frame.alpha
                var index = 0
                for (y in 0 until innerHeight) {
                    for (x in 0 until innerWidth) {
                        val rgb = image.getRGB(x + xOffset, y + yOffset)

                        val a = (rgb shr 24) and 0xFF
                        val color = rgb and 0xFFFFFF

                        val paletteIndex = if (alpha == null && a == 0) {
                            0
                        } else {
                            val paletteIndex = palette.binarySearch(color)
                            check(paletteIndex >= 0)
                            paletteIndex + 1
                        }

                        frame.pixels[index] = paletteIndex.toByte()

                        if (alpha != null) {
                            alpha[index] = a.toByte()
                        }

                        index++
                    }
                }

                frame
            }

            return Sprite(width, height, palette, frames)
        }

        private fun BufferedImage.hasAlphaChannel(): Boolean {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val rgb = getRGB(x, y)

                    val alpha = (rgb shr 24) and 0xFF
                    val color = rgb and 0xFFFFFF

                    // preserve transparent colours
                    if (alpha == 0 && color != 0) {
                        return true
                    } else if (alpha != 0 && alpha != 0xFF) {
                        return true
                    }
                }
            }

            return false
        }

        private fun BufferedImage.isColumnTransparent(x: Int, alphaChannel: Boolean): Boolean {
            for (y in 0 until height) {
                val rgb = getRGB(x, y)

                val alpha = (rgb shr 24) and 0xFF
                val color = rgb and 0xFFFFFF

                // transparent colours only preserved if FLAG_ALPHA set
                if (alphaChannel && color != 0) {
                    return false
                } else if (alpha != 0) {
                    return false
                }
            }

            return true
        }

        private fun BufferedImage.isRowTransparent(y: Int, alphaChannel: Boolean): Boolean {
            for (x in 0 until width) {
                val rgb = getRGB(x, y)

                val alpha = (rgb shr 24) and 0xFF
                val color = rgb and 0xFFFFFF

                // transparent colours only preserved if FLAG_ALPHA set
                if (alphaChannel && color != 0) {
                    return false
                } else if (alpha != 0) {
                    return false
                }
            }

            return true
        }
    }
}
