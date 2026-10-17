package com.arnavgames.cloakrun.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedurally generated avatar. The same [seed] always yields the same glass orb: two hues,
 * a pattern and a rotation derived from a stable hash. No photos are ever uploaded (Cloud Storage
 * is not available on the free plan, and avatars should not identify real faces anyway).
 */
@Composable
fun CloakAvatar(
    seed: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val spec = remember(seed) { AvatarSpec.from(seed) }
    Canvas(
        modifier
            .size(size)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
    ) {
        val r = this.size.minDimension / 2
        val orb = Path().apply { addOval(androidx.compose.ui.geometry.Rect(center, r)) }
        clipPath(orb) {
            drawRect(
                Brush.linearGradient(
                    listOf(spec.colorA, spec.colorB),
                    start = Offset(0f, 0f),
                    end = Offset(this.size.width, this.size.height),
                ),
            )
            rotate(spec.rotationDeg) {
                val line = Stroke(width = r * 0.16f)
                when (spec.pattern) {
                    AvatarPattern.Rings -> for (i in 1..3) {
                        drawCircle(
                            Color.White.copy(alpha = 0.22f),
                            radius = r * 0.32f * i,
                            center = Offset(center.x, center.y + r * 0.55f),
                            style = line,
                        )
                    }

                    AvatarPattern.Bands -> for (i in -2..2) {
                        val y = center.y + i * r * 0.42f
                        drawLine(Color.White.copy(alpha = 0.2f), Offset(0f, y), Offset(this.size.width, y), strokeWidth = r * 0.14f)
                    }

                    AvatarPattern.Facets -> {
                        val facet = Path().apply {
                            for (k in 0 until spec.facets) {
                                val a = 2 * PI * k / spec.facets
                                val p = Offset(center.x + (r * 0.8f * cos(a)).toFloat(), center.y + (r * 0.8f * sin(a)).toFloat())
                                if (k == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                            }
                            close()
                        }
                        drawPath(facet, Color.White.copy(alpha = 0.16f))
                        drawPath(facet, Color.White.copy(alpha = 0.3f), style = Stroke(width = r * 0.06f))
                    }

                    AvatarPattern.Orbit -> {
                        drawCircle(Color.White.copy(alpha = 0.2f), radius = r * 0.6f, style = Stroke(width = r * 0.08f))
                        drawCircle(Color.White.copy(alpha = 0.55f), radius = r * 0.14f, center = Offset(center.x + r * 0.6f, center.y))
                    }
                }
            }
            // Glass: a soft top-left highlight and a darker lower rim.
            drawCircle(
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(center.x - r * 0.4f, center.y - r * 0.45f),
                    radius = r * 0.8f,
                ),
            )
            drawCircle(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f))),
            )
        }
        drawCircle(Color.White.copy(alpha = 0.35f), radius = r - 0.75.dp.toPx(), style = Stroke(1.5.dp.toPx()))
    }
}

internal enum class AvatarPattern { Rings, Bands, Facets, Orbit }

@Immutable
internal data class AvatarSpec(
    val colorA: Color,
    val colorB: Color,
    val pattern: AvatarPattern,
    val rotationDeg: Float,
    val facets: Int,
) {
    companion object {
        /** FNV-1a: stable across platforms and app versions, unlike String.hashCode guarantees. */
        internal fun hash(seed: String): Long {
            var h = 0xcbf29ce484222325uL
            for (b in seed.encodeToByteArray()) {
                h = h xor b.toUByte().toULong()
                h *= 0x100000001b3uL
            }
            return h.toLong()
        }

        private val teamHueRanges = listOf(20f..45f, 172f..200f)

        internal fun avoidTeamHues(hue: Float): Float {
            var h = hue
            repeat(teamHueRanges.size) {
                teamHueRanges.firstOrNull { h in it }?.let { h = (it.endInclusive + 8f) % 360f }
            }
            return h
        }

        /** MurmurHash3 fmix64: spreads FNV's weakly mixed high bits so every field is uniform. */
        internal fun mix(value: Long): Long {
            var k = value
            k = k xor (k ushr 33)
            k *= -0xae502812aa7333L
            k = k xor (k ushr 33)
            k *= -0x3b314601e57a13adL
            k = k xor (k ushr 33)
            return k
        }

        fun from(seed: String): AvatarSpec {
            val h = mix(hash(seed))
            fun bits(shift: Int, mod: Int): Int = ((h ushr shift) and 0xFFFF).toInt() % mod
            // Hues skip the team colours (cyan hiders, amber seekers) so an avatar is never read as a team marker.
            val hueA = avoidTeamHues(bits(0, 360).toFloat())
            val hueB = avoidTeamHues((hueA + 40f + bits(16, 90)) % 360f)
            return AvatarSpec(
                colorA = Color.hsv(hueA, 0.55f, 0.85f),
                colorB = Color.hsv(hueB, 0.7f, 0.45f),
                pattern = AvatarPattern.entries[bits(32, AvatarPattern.entries.size)],
                rotationDeg = bits(40, 360).toFloat(),
                facets = 5 + bits(48, 4),
            )
        }
    }
}
