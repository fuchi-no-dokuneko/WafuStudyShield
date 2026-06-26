package dev.studyshield.overlay

import dev.studyshield.domain.OverlayLayout
import dev.studyshield.domain.ResolvedSide
import dev.studyshield.domain.BubbleAlignment
import dev.studyshield.domain.BubbleAnimation
import kotlin.math.max
import kotlin.math.min

data class SafeInsetsFraction(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    init {
        listOf(left, top, right, bottom).forEach {
            require(it in 0f..0.45f) { "Safe inset fractions must be between 0 and 0.45." }
        }
    }
}

data class ResolvedOverlayLayout(
    val side: ResolvedSide,
    val characterX: Float,
    val characterY: Float,
    val bubbleX: Float,
    val bubbleY: Float,
    val characterScale: Float,
    val characterOpacity: Float,
    val bubbleAlignment: BubbleAlignment,
    val animation: BubbleAnimation,
    val overlayDim: Float,
    val wallpaperScale: Float,
    val wallpaperOffsetX: Float,
    val wallpaperOffsetY: Float,
    val reduceMotion: Boolean
)

class LayoutResolver {
    fun resolve(
        layout: OverlayLayout,
        side: ResolvedSide,
        safeInsets: SafeInsetsFraction = SafeInsetsFraction(0.04f, 0.05f, 0.04f, 0.08f)
    ): ResolvedOverlayLayout {
        val characterX = when (side) {
            ResolvedSide.Left -> min(layout.characterAnchorX, 0.36f)
            ResolvedSide.Right -> max(layout.characterAnchorX, 0.64f)
        }
        val bubbleX = when (side) {
            ResolvedSide.Left -> max(layout.bubbleAnchorX, 0.56f)
            ResolvedSide.Right -> min(layout.bubbleAnchorX, 0.44f)
        }
        return ResolvedOverlayLayout(
            side = side,
            characterX = clamp(characterX, safeInsets.left, 1f - safeInsets.right),
            characterY = clamp(layout.characterAnchorY, safeInsets.top, 1f - safeInsets.bottom),
            bubbleX = clamp(bubbleX, safeInsets.left, 1f - safeInsets.right),
            bubbleY = clamp(layout.bubbleAnchorY, safeInsets.top, 1f - safeInsets.bottom),
            characterScale = layout.characterScale,
            characterOpacity = layout.characterOpacity,
            bubbleAlignment = layout.bubbleAlignment,
            animation = layout.animation,
            overlayDim = layout.overlayDim,
            wallpaperScale = layout.wallpaperScale,
            wallpaperOffsetX = layout.wallpaperOffsetX,
            wallpaperOffsetY = layout.wallpaperOffsetY,
            reduceMotion = layout.reduceMotion
        )
    }

    private fun clamp(value: Float, min: Float, max: Float): Float {
        return value.coerceIn(min, max)
    }
}
