package dev.studyshield.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StudyShieldColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF006C6A),
    onPrimary = Color.White,
    secondary = Color(0xFFC75C46),
    onSecondary = Color.White,
    tertiary = Color(0xFF4D6B7C),
    background = Color(0xFFF8F7F2),
    onBackground = Color(0xFF1F2937),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F2937),
    surfaceVariant = Color(0xFFE6E3D9),
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFF8B8A82)
)

@Composable
fun StudyShieldTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StudyShieldColors,
        content = content
    )
}
