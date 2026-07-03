package app.tolmach.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Дизайн-система «Толмач»: единственный акцент — нефрит (камень деловых
// подарков в Китае), всё остальное — тихий графит. Тема всегда тёмная:
// одно выверенное состояние вместо двух полупроверенных.
object TolmachColors {
    val Jade = Color(0xFF3FBF8F)
    val JadeBright = Color(0xFF4CD49F)
    val JadeDeep = Color(0xFF06271B)
    val Bg = Color(0xFF0C0F12)
    val Surface = Color(0xFF14181E)
    val SurfaceHigh = Color(0xFF1A2129)
    val Hairline = Color(0xFF232B34)
    val Text = Color(0xFFECEFF3)
    val TextDim = Color(0xFF8E99A6)
    val Coral = Color(0xFFE5716A)
}

private val DarkColors = darkColorScheme(
    primary = TolmachColors.Jade,
    onPrimary = TolmachColors.JadeDeep,
    primaryContainer = Color(0xFF17372B),
    onPrimaryContainer = Color(0xFFC5EDDB),
    background = TolmachColors.Bg,
    onBackground = TolmachColors.Text,
    surface = TolmachColors.Surface,
    onSurface = TolmachColors.Text,
    surfaceVariant = TolmachColors.SurfaceHigh,
    onSurfaceVariant = TolmachColors.TextDim,
    outline = TolmachColors.Hairline,
    outlineVariant = Color(0xFF1C232B),
    error = TolmachColors.Coral,
    errorContainer = Color(0xFF4A1F1D),
    onErrorContainer = Color(0xFFF3C6C2),
)

@Composable
fun TolmachTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = TolmachTypography,
        content = content,
    )
}
