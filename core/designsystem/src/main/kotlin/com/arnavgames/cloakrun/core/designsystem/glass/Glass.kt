package com.arnavgames.cloakrun.core.designsystem.glass

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlin.random.Random

/** The backdrop that glass surfaces blur. Provided by [GlassScene]; null outside one. */
val LocalGlassBackdrop = staticCompositionLocalOf<HazeState?> { null }

/** The glass tier for this device and the user's performance setting. */
val LocalGlassQuality = staticCompositionLocalOf { GlassQuality.Full }

/** Shared, filtered device tilt that drives specular highlights. Read it only in draw scope. */
val LocalTilt = staticCompositionLocalOf<State<Tilt>> { StaticTilt }

private object StaticTilt : State<Tilt> {
    override val value: Tilt = Tilt.Level
}

/** How a glass surface looks. Build it with [GlassDefaults]. */
@Immutable
data class GlassStyle(
    val tint: Color,
    val fallbackTint: Color,
    val blurRadius: Dp,
    val rimHighlight: Color,
    val rimShade: Color,
    val grainAlpha: Float,
    val specularAlpha: Float,
    val shadowColor: Color,
    val shadowRadius: Dp,
    val shadowOffsetY: Dp,
)

object GlassDefaults {
    /** Cards and panels: 8 percent tint, medium blur. */
    @Composable
    fun surface(): GlassStyle = style(tintBoost = 0f, blur = CloakTheme.blur.surface)

    /** Sheets and dialogs: denser tint and stronger blur. */
    @Composable
    fun sheet(): GlassStyle = style(tintBoost = 0.06f, blur = CloakTheme.blur.sheet)

    /** Small chrome over busy content (map HUD, chips): light blur, a little more tint. */
    @Composable
    fun chrome(): GlassStyle = style(tintBoost = 0.04f, blur = CloakTheme.blur.chrome)

    @Composable
    private fun style(tintBoost: Float, blur: Dp): GlassStyle {
        val c = CloakTheme.colors
        val tint = c.glassTint.copy(alpha = (c.glassTint.alpha + tintBoost).coerceAtMost(1f))
        // Without blur the backdrop shows through sharply, so the fallback tint is denser and
        // carries some of the base colour to keep text contrast.
        val fallback = c.backgroundRaised.copy(alpha = if (c.isDark) 0.86f else 0.9f)
        return GlassStyle(
            tint = tint,
            fallbackTint = tint.compositeOver(fallback),
            blurRadius = blur,
            rimHighlight = c.glassEdgeHighlight,
            rimShade = c.glassEdgeShade,
            grainAlpha = if (c.isDark) 0.05f else 0.035f,
            specularAlpha = if (c.isDark) 0.09f else 0.22f,
            shadowColor = c.glassShadow,
            shadowRadius = 28.dp,
            shadowOffsetY = 12.dp,
        )
    }
}

/**
 * Hosts a blurred backdrop for the glass in [content]. Everything drawn by [backdrop] becomes
 * the source that [GlassSurface]s inside [content] blur.
 */
@Composable
fun GlassScene(
    backdrop: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val haze = rememberHazeState()
    Box(modifier) {
        Box(Modifier.matchParentSize().hazeSource(haze), content = backdrop)
        CompositionLocalProvider(LocalGlassBackdrop provides haze) {
            content()
        }
    }
}

/**
 * The Frost Glass material: a blurred backdrop (when available), an 8 to 16 percent tint, a 1dp
 * gradient rim (bright top-left, faint bottom-right) traced along [shape], film grain, a
 * tilt-reactive specular sweep and a soft tinted shadow.
 *
 * On devices without blur ([GlassQuality.Tint]) a denser frosted tint stands in.
 */
@Composable
fun GlassSurface(
    shape: Shape,
    modifier: Modifier = Modifier,
    style: GlassStyle = GlassDefaults.surface(),
    elevated: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.glass(shape, style, currentGlassEnvironment(), elevated), content = content)
}

/** What a glass surface needs from its surroundings: the backdrop to blur, the quality tier and tilt. */
@Stable
class GlassEnvironment internal constructor(
    internal val backdrop: HazeState?,
    internal val quality: GlassQuality,
    internal val tilt: State<Tilt>,
)

/** Captures the glass environment of the current composition, for use with [Modifier.glass]. */
@Composable
@ReadOnlyComposable
fun currentGlassEnvironment(): GlassEnvironment = GlassEnvironment(LocalGlassBackdrop.current, LocalGlassQuality.current, LocalTilt.current)

/**
 * The Frost Glass material as a plain modifier, for components that draw their own content
 * (buttons, pills). Prefer [GlassSurface] elsewhere.
 */
fun Modifier.glass(
    shape: Shape,
    style: GlassStyle,
    environment: GlassEnvironment,
    elevated: Boolean = true,
): Modifier {
    val backdrop = environment.backdrop
    val quality = environment.quality
    val radius = if (quality == GlassQuality.Reduced) style.blurRadius * REDUCED_BLUR_FACTOR else style.blurRadius
    val grain = if (quality == GlassQuality.Full) style.grainAlpha else 0f
    val shadowed = if (elevated) {
        dropShadow(
            shape,
            Shadow(radius = style.shadowRadius, color = style.shadowColor, offset = DpOffset(0.dp, style.shadowOffsetY)),
        )
    } else {
        this
    }
    val material = if (backdrop != null && quality.blurs) {
        Modifier.hazeBlur(
            input = HazeInput.Sources(backdrop),
            style = HazeBlurStyle {
                blurRadius(radius)
                noiseFactor(0f)
                colorEffects(listOf(HazeColorEffect.tint(style.tint)))
                fallbackColorEffect(HazeColorEffect.tint(style.fallbackTint))
            },
        )
    } else {
        Modifier.background(style.fallbackTint)
    }
    return shadowed
        .clip(shape)
        .then(material)
        .glassOverlay(shape, style, grain, environment.tilt)
}

/** Rim, grain and specular, drawn over the content's background but under its children. */
private fun Modifier.glassOverlay(
    shape: Shape,
    style: GlassStyle,
    grainAlpha: Float,
    tilt: State<Tilt>,
): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply { addOutline(outline) }
    val rimBrush = Brush.linearGradient(
        0f to style.rimHighlight,
        0.45f to style.rimHighlight.copy(alpha = style.rimHighlight.alpha * 0.35f),
        1f to style.rimShade,
        start = Offset.Zero,
        end = Offset(size.width, size.height),
    )
    // The path is already clipped, so a 2dp stroke shows exactly 1dp inside the edge.
    val rimStroke = Stroke(width = 2.dp.toPx())
    val grainBrush = ShaderBrush(ImageShader(GrainTexture.bitmap, TileMode.Repeated, TileMode.Repeated))
    onDrawBehind {
        if (grainAlpha > 0f) drawRect(grainBrush, alpha = grainAlpha, blendMode = BlendMode.Overlay)
        if (style.specularAlpha > 0f) {
            val t = tilt.value
            val center = size.width * (0.32f + t.x * 0.4f)
            val lift = size.height * t.y * 0.3f
            drawRect(
                Brush.linearGradient(
                    0f to Color.Transparent,
                    0.5f to Color.White.copy(alpha = style.specularAlpha),
                    1f to Color.Transparent,
                    start = Offset(center - size.width * 0.35f, -lift),
                    end = Offset(center + size.width * 0.15f, size.height - lift),
                ),
                blendMode = BlendMode.Plus,
            )
        }
        drawPath(path, rimBrush, style = rimStroke)
    }
}

private const val REDUCED_BLUR_FACTOR = 0.6f

/** A small tileable monochrome noise texture, generated once per process. */
private object GrainTexture {
    private const val SIZE = 96

    val bitmap: ImageBitmap by lazy {
        val random = Random(SEED)
        val pixels = IntArray(SIZE * SIZE) {
            val v = random.nextInt(256)
            (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        Bitmap.createBitmap(pixels, SIZE, SIZE, Bitmap.Config.ARGB_8888).asImageBitmap()
    }

    private const val SEED = 0x0C10A4
}
