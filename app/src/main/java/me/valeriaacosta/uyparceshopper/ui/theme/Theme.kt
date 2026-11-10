package me.valeriaacosta.uyparceshopper.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val colores = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    secondary = Yellow,
    onSecondary = Navy,
    background = Bg,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Field,
    onSurfaceVariant = Muted,
    outline = Line,
    error = Red
)

@Composable
fun UyParceTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colores, typography = Tipografia, content = content)
}
