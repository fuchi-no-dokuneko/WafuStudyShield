package dev.studyshield.overlay

import dev.studyshield.domain.OverlayLayout
import dev.studyshield.domain.ResolvedSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutResolverTest {
    @Test
    fun rightSideKeepsBubbleOnOppositeSideAndInsideInsets() {
        val resolved = LayoutResolver().resolve(
            layout = OverlayLayout(characterAnchorX = 0.95f, bubbleAnchorX = 0.96f),
            side = ResolvedSide.Right,
            safeInsets = SafeInsetsFraction(left = 0.10f, top = 0.10f, right = 0.10f, bottom = 0.10f)
        )

        assertEquals(ResolvedSide.Right, resolved.side)
        assertTrue(resolved.characterX <= 0.90f)
        assertTrue(resolved.bubbleX <= 0.44f)
        assertTrue(resolved.bubbleX >= 0.10f)
    }

    @Test
    fun leftSideKeepsBubbleOnOppositeSideAndInsideInsets() {
        val resolved = LayoutResolver().resolve(
            layout = OverlayLayout(characterAnchorX = 0.05f, bubbleAnchorX = 0.04f),
            side = ResolvedSide.Left,
            safeInsets = SafeInsetsFraction(left = 0.10f, top = 0.10f, right = 0.10f, bottom = 0.10f)
        )

        assertEquals(ResolvedSide.Left, resolved.side)
        assertTrue(resolved.characterX >= 0.10f)
        assertTrue(resolved.bubbleX >= 0.56f)
        assertTrue(resolved.bubbleX <= 0.90f)
    }

    @Test
    fun carriesWallpaperCropValuesIntoResolvedLayout() {
        val resolved = LayoutResolver().resolve(
            layout = OverlayLayout(
                wallpaperScale = 1.6f,
                wallpaperOffsetX = 0.4f,
                wallpaperOffsetY = -0.3f
            ),
            side = ResolvedSide.Right
        )

        assertEquals(1.6f, resolved.wallpaperScale, 0.0001f)
        assertEquals(0.4f, resolved.wallpaperOffsetX, 0.0001f)
        assertEquals(-0.3f, resolved.wallpaperOffsetY, 0.0001f)
    }
}
