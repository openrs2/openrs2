package org.openrs2.archive.map

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

public object Colors {
    private val HSL_TO_RGB = IntArray(65536)
    private const val BRIGHTNESS = 0.8

    init {
        var i = 0
        for (h in 0 until 64) {
            for (s in 0 until 8) {
                for (l in 0 until 128) {
                    val hue = h.toDouble() / 64 + 0.0078125
                    val saturation = s.toDouble() / 8 + 0.0625
                    val lightness = l.toDouble() / 128
                    HSL_TO_RGB[i++] = hslToRgb(hue, saturation, lightness)
                }
            }
        }
    }

    private fun hslToRgb(h: Double, s: Double, l: Double): Int {
        var r = l
        var g = l
        var b = l

        if (s != 0.0) {
            val q = if (l * 2 < 1) {
                l * (s + 1)
            } else {
                l + s - (l * s)
            }

            val p = l * 2 - q

            var tr = h + (1.0 / 3)
            if (tr > 1) {
                tr--
            }

            var tb = h - (1.0 / 3)
            if (tb < 0) {
                tb++
            }

            r = if (tr * 6 < 1) {
                tr * (q - p) * 6 + p
            } else if (tr * 2 < 1) {
                q
            } else if (tr * 3 < 2) {
                (2.0 / 3 - tr) * (q - p) * 6 + p
            } else {
                p
            }

            g = if (h * 6 < 1) {
                h * (q - p) * 6 + p
            } else if (h * 2 < 1) {
                q
            } else if (h * 3 < 2) {
                (2.0 / 3 - h) * (q - p) * 6 + p
            } else {
                p
            }

            b = if (tb * 6 < 1) {
                tb * (q - p) * 6 + p
            } else if (tb * 2 < 1) {
                q
            } else if (tb * 3 < 2) {
                (2.0 / 3 - tb) * (q - p) * 6 + p
            } else {
                p
            }
        }

        val red = (r.pow(BRIGHTNESS) * 256).toInt()
        val green = (g.pow(BRIGHTNESS) * 256).toInt()
        val blue = (b.pow(BRIGHTNESS) * 256).toInt()

        var rgb = (red shl 16) or (green shl 8) or blue
        if (rgb == 0) {
            rgb = 1
        }

        return rgb
    }

    public fun hslToRgb(hsl: Int): Int {
        return HSL_TO_RGB[hsl]
    }

    public fun multiplyLightness(hsl: Int, factor: Int): Int {
        return when (hsl) {
            -2 -> 12345678
            -1 -> 127 - min(max(factor, 0), 127)
            else -> {
                var l = ((hsl and 0x7F) * factor) shr 7
                l = min(max(l, 2), 126)
                (hsl and 0xFF80) or l
            }
        }
    }
}
