package com.arnavgames.cloakrun.core.designsystem.shape

import android.graphics.Region
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import kotlin.math.pow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SquircleShapeTest {
    private val density = Density(1f)

    private fun outline(radius: Dp, size: Size): Outline = SquircleShape(radius).createOutline(size, LayoutDirection.Ltr, density)

    @Test
    fun outlineFillsExactlyTheRequestedBounds() {
        val size = Size(300f, 200f)
        val bounds = outline(28.dp, size).bounds
        assertThat(bounds.left).isWithin(0.5f).of(0f)
        assertThat(bounds.top).isWithin(0.5f).of(0f)
        assertThat(bounds.right).isWithin(0.5f).of(300f)
        assertThat(bounds.bottom).isWithin(0.5f).of(200f)
    }

    @Test
    fun zeroRadius_isAPlainRectangle() {
        assertThat(outline(0.dp, Size(100f, 100f))).isInstanceOf(Outline.Rectangle::class.java)
    }

    @Test
    fun superellipseCorner_isFullerThanACircularArc() {
        // At 45 degrees a superellipse (n = 5) bulges further toward the corner than a circle.
        val n = SquircleShape.DEFAULT_EXPONENT
        val superellipse = 2.0.pow(-1.0 / n)
        val circle = 2.0.pow(-1.0 / 2)
        assertThat(superellipse).isGreaterThan(circle)
    }

    private fun containsPoint(
        path: androidx.compose.ui.graphics.Path,
        w: Int,
        h: Int,
        x: Int,
        y: Int,
    ): Boolean {
        val region = Region()
        region.setPath(path.asAndroidPath(), Region(0, 0, w, h))
        return region.contains(x, y)
    }

    @Test
    fun capsuleCorner_followsTheCircle() {
        // n = 2 on a 200 x 60 box with extent 30: the cap is a semicircle of radius 30.
        // Its 45 degree point sits at (8.8, 8.8): (10, 10) is inside, (7, 7) is outside.
        val path = SquircleShape.squirclePath(Size(200f, 60f), extent = 30f, n = 2f)
        assertThat(containsPoint(path, 200, 60, 10, 10)).isTrue()
        assertThat(containsPoint(path, 200, 60, 7, 7)).isFalse()
    }

    @Test
    fun continuousCorner_reachesFurtherIntoTheCornerThanACircle() {
        // n = 5 with the same extent puts the 45 degree point at (3.9, 3.9), so (6, 6) is inside
        // the squircle even though it is outside the circular corner.
        val squircle = SquircleShape.squirclePath(Size(200f, 200f), extent = 30f, n = 5f)
        val circle = SquircleShape.squirclePath(Size(200f, 200f), extent = 30f, n = 2f)
        assertThat(containsPoint(squircle, 200, 200, 6, 6)).isTrue()
        assertThat(containsPoint(circle, 200, 200, 6, 6)).isFalse()
    }

    @Test
    fun equalShapes_areEqual() {
        assertThat(SquircleShape(28.dp)).isEqualTo(SquircleShape(28.dp))
        assertThat(SquircleShape(28.dp)).isNotEqualTo(SquircleShape(40.dp))
    }
}
