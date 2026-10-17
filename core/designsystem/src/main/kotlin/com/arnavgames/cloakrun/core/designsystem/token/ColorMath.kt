package com.arnavgames.cloakrun.core.designsystem.token

import kotlin.math.abs
import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Colour science used to verify the palette: WCAG 2.x contrast and colour-vision-deficiency
 * simulation. Pure functions on packed 0xRRGGBB integers so they run in plain JVM tests and at runtime.
 */
object ColorMath {
    /** Kinds of colour-vision deficiency simulated with the Machado, Oliveira and Fernandes (2009) matrices. */
    enum class Deficiency { None, Protanopia, Deuteranopia, Tritanopia }

    fun relativeLuminance(rgb: Int): Double {
        val (r, g, b) = linearRgb(rgb)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /** WCAG contrast ratio between two opaque colours, from 1.0 to 21.0. */
    fun contrastRatio(a: Int, b: Int): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        val hi = maxOf(la, lb)
        val lo = minOf(la, lb)
        return (hi + 0.05) / (lo + 0.05)
    }

    /** Composites [foreground] with [alpha] over an opaque [background]. */
    fun over(
        foreground: Int,
        alpha: Double,
        background: Int,
    ): Int {
        fun ch(shift: Int): Int {
            val f = (foreground shr shift) and 0xFF
            val b = (background shr shift) and 0xFF
            return (f * alpha + b * (1 - alpha)).toInt().coerceIn(0, 255)
        }
        return (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    /** Simulates how [rgb] appears with full-severity [deficiency]. */
    fun simulate(rgb: Int, deficiency: Deficiency): Int {
        val m = when (deficiency) {
            Deficiency.None -> return rgb
            Deficiency.Protanopia -> PROTANOPIA
            Deficiency.Deuteranopia -> DEUTERANOPIA
            Deficiency.Tritanopia -> TRITANOPIA
        }
        val (r, g, b) = linearRgb(rgb)
        val r2 = (m[0] * r + m[1] * g + m[2] * b).coerceIn(0.0, 1.0)
        val g2 = (m[3] * r + m[4] * g + m[5] * b).coerceIn(0.0, 1.0)
        val b2 = (m[6] * r + m[7] * g + m[8] * b).coerceIn(0.0, 1.0)
        return (encode(r2) shl 16) or (encode(g2) shl 8) or encode(b2)
    }

    /** CIEDE76 distance in CIELAB. Roughly: under 10 is hard to tell apart at a glance, over 20 is clear. */
    fun deltaE(a: Int, b: Int): Double {
        val la = lab(a)
        val lb = lab(b)
        return sqrt((la[0] - lb[0]).pow(2) + (la[1] - lb[1]).pow(2) + (la[2] - lb[2]).pow(2))
    }

    /** CIELAB lightness (0 to 100). */
    fun lightness(rgb: Int): Double = lab(rgb)[0]

    /** Hue distance is irrelevant for monochrome comparisons; this compares lightness only. */
    fun lightnessDelta(a: Int, b: Int): Double = abs(lightness(a) - lightness(b))

    private fun linearRgb(rgb: Int): Triple<Double, Double, Double> = Triple(
        decode((rgb shr 16) and 0xFF),
        decode((rgb shr 8) and 0xFF),
        decode(rgb and 0xFF),
    )

    private fun decode(channel: Int): Double {
        val c = channel / 255.0
        return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun encode(linear: Double): Int {
        val c = if (linear <= 0.0031308) linear * 12.92 else 1.055 * linear.pow(1 / 2.4) - 0.055
        return (c * 255).toInt().coerceIn(0, 255)
    }

    private fun lab(rgb: Int): DoubleArray {
        val (r, g, b) = linearRgb(rgb)
        val x = (0.4124 * r + 0.3576 * g + 0.1805 * b) / 0.95047
        val y = 0.2126 * r + 0.7152 * g + 0.0722 * b
        val z = (0.0193 * r + 0.1192 * g + 0.9505 * b) / 1.08883
        fun f(t: Double) = if (t > 216.0 / 24389.0) cbrt(t) else (24389.0 / 27.0 * t + 16) / 116
        val fx = f(x)
        val fy = f(y)
        val fz = f(z)
        return doubleArrayOf(116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz))
    }

    // Machado et al. 2009, severity 1.0, applied in linear RGB.
    private val PROTANOPIA = doubleArrayOf(
        0.152286, 1.052583, -0.204868,
        0.114503, 0.786281, 0.099216,
        -0.003882, -0.048116, 1.051998,
    )
    private val DEUTERANOPIA = doubleArrayOf(
        0.367322, 0.860646, -0.227968,
        0.280085, 0.672501, 0.047413,
        -0.011820, 0.042940, 0.968881,
    )
    private val TRITANOPIA = doubleArrayOf(
        1.255528, -0.076749, -0.178779,
        -0.078411, 0.930809, 0.147602,
        0.004733, 0.691367, 0.303900,
    )
}
