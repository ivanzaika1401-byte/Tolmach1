package app.tolmach.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Нефрит — камень деловых подарков в Китае. Один акцент, всё остальное тихое.
private val Jade = Color(0xFF3FBF8F)
private val JadeDeep = Color(0xFF0B2E22)

private val DarkColors = darkColorScheme(
    primary = Jade,
    onPrimary = JadeDeep,
    primaryContainer = Color(0xFF1D4536),
    onPrimaryContainer = Color(0xFFC5EDDB),
    background = Color(0xFF101418),
    onBackground = Color(0xFFE7EAEE),
    surface = Color(0xFF151A20),
    onSurface = Color(0xFFE7EAEE),
    surfaceVariant = Color(0xFF1E252D),
    onSurfaceVariant = Color(0xFF9AA5B1),
    outline = Color(0xFF3A434D),
    error = Color(0xFFE5716A),
    errorContainer = Color(0xFF5C2321),
    onErrorContainer = Color(0xFFF3C6C2),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1D7A57),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFEBDD),
    onPrimaryContainer = JadeDeep,
    background = Color(0xFFFAFAF7),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFEBEFEA),
    onSurfaceVariant = Color(0xFF5B655F),
    outline = Color(0xFFC3CCC5),
)

@Composable
fun TolmachTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
