package dev.studyshield.overlay

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.studyshield.domain.ActiveReminder
import dev.studyshield.domain.BackgroundMode
import dev.studyshield.domain.BubbleAlignment
import dev.studyshield.domain.BubbleAnimation
import dev.studyshield.domain.ResolvedSide
import dev.studyshield.media.PromptAudioResult
import dev.studyshield.ui.StudyShieldTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FocusOverlayContent(
    reminder: ActiveReminder,
    dialogueText: String,
    audioResult: PromptAudioResult,
    layout: ResolvedOverlayLayout,
    onReturnHome: () -> Unit,
    onPauseFiveMinutes: () -> Unit
) {
    StudyShieldTheme {
        val wallpaper by rememberImageBitmap(reminder.companionPack?.wallpaperUri)
        val characterImage by rememberImageBitmap(reminder.companionPack?.characterImageUri)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF102A2A))
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(20.dp)
        ) {
            if (wallpaper != null && reminder.profile.layout.backgroundMode == BackgroundMode.DimmedWallpaper) {
                Image(
                    bitmap = requireNotNull(wallpaper),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(
                            x = maxWidth * layout.wallpaperOffsetX * 0.25f,
                            y = maxHeight * layout.wallpaperOffsetY * 0.25f
                        )
                        .graphicsLayer(
                            scaleX = layout.wallpaperScale,
                            scaleY = layout.wallpaperScale
                        ),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF102A2A).copy(alpha = layout.overlayDim.coerceIn(0.25f, 0.95f)))
            )
            val characterSize = (156.dp * layout.characterScale).coerceBetween(96.dp, 260.dp)
            val characterX = maxWidth.availableFor(characterSize) * layout.characterX
            val characterY = maxHeight.availableFor(characterSize) * layout.characterY
            val bubbleWidth = if (maxWidth < 560.dp) maxWidth else 520.dp
            val bubbleX = maxWidth.availableFor(bubbleWidth) * layout.bubbleX
            val estimatedBubbleHeight = 220.dp
            val bubbleY = maxHeight.availableFor(estimatedBubbleHeight) * layout.bubbleY
            var bubbleVisible by remember { mutableStateOf(layout.reduceMotion || layout.animation == BubbleAnimation.None) }
            LaunchedEffect(layout.reduceMotion, layout.animation) {
                bubbleVisible = true
            }
            val bubbleAlpha by animateFloatAsState(
                targetValue = if (bubbleVisible) 1f else 0f,
                animationSpec = if (layout.reduceMotion || layout.animation == BubbleAnimation.None) {
                    snap()
                } else {
                    tween(durationMillis = 220)
                },
                label = "bubbleAlpha"
            )
            val bubbleOffsetY = if (!layout.reduceMotion && layout.animation == BubbleAnimation.FadeDown) {
                16.dp * (1f - bubbleAlpha)
            } else {
                0.dp
            }

            CompanionFigure(
                modifier = Modifier
                    .offset(x = characterX, y = characterY)
                    .alpha(layout.characterOpacity),
                size = characterSize,
                side = layout.side,
                image = characterImage
            )

            Surface(
                modifier = Modifier
                    .offset(x = bubbleX, y = bubbleY + bubbleOffsetY)
                    .width(bubbleWidth)
                    .alpha(bubbleAlpha),
                color = Color(0xFFF8F7F2),
                contentColor = Color(0xFF1F2937),
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Currently ${reminder.profile.name}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = dialogueText,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = layout.bubbleAlignment.toTextAlign()
                    )
                    if (audioResult is PromptAudioResult.TextFallback) {
                        Text(
                            text = "Voice unavailable: ${audioResult.reason}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF715C00)
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .offset(y = maxHeight.availableFor(116.dp))
                    .fillMaxWidth(),
                color = Color(0xFFF8F7F2),
                contentColor = Color(0xFF1F2937),
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 5.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onReturnHome
                    ) {
                        Icon(Icons.Outlined.Home, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Return home")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onPauseFiveMinutes
                    ) {
                        Icon(Icons.Outlined.PauseCircle, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Pause 5 min")
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanionFigure(
    modifier: Modifier,
    size: Dp,
    side: ResolvedSide,
    image: ImageBitmap?
) {
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = modifier.size(size),
            contentScale = ContentScale.Fit
        )
        return
    }
    Surface(
        modifier = modifier.size(size),
        color = Color(0xFFD7EEF4),
        contentColor = Color(0xFF006C6A),
        shape = CircleShape
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (side == ResolvedSide.Left) "Study\nGuide" else "Guide\nStudy",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun rememberImageBitmap(uriString: String?): State<ImageBitmap?> {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, key1 = uriString) {
        value = withContext(Dispatchers.IO) {
            if (uriString.isNullOrBlank()) {
                null
            } else {
                runCatching {
                    context.contentResolver.openInputStream(uriString.toUri())?.use { input ->
                        BitmapFactory.decodeStream(input)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }
}

private fun Dp.availableFor(itemSize: Dp): Dp = if (this > itemSize) this - itemSize else 0.dp

private fun Dp.coerceBetween(min: Dp, max: Dp): Dp {
    return when {
        this < min -> min
        this > max -> max
        else -> this
    }
}

private fun BubbleAlignment.toTextAlign(): TextAlign {
    return when (this) {
        BubbleAlignment.Start -> TextAlign.Start
        BubbleAlignment.Center -> TextAlign.Center
        BubbleAlignment.End -> TextAlign.End
    }
}
