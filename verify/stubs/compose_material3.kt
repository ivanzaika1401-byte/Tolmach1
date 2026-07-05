package androidx.compose.material3
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val C0 = Color(0xFF000000)
private val T0 = TextStyle()
private object ShapeStub : Shape

class ColorScheme(
    val primary: Color, val onPrimary: Color,
    val primaryContainer: Color, val onPrimaryContainer: Color,
    val background: Color, val onBackground: Color,
    val surface: Color, val onSurface: Color,
    val surfaceVariant: Color, val onSurfaceVariant: Color,
    val outline: Color, val outlineVariant: Color,
    val error: Color, val onError: Color
)
fun darkColorScheme(
    primary: Color = C0, onPrimary: Color = C0,
    primaryContainer: Color = C0, onPrimaryContainer: Color = C0,
    secondary: Color = C0, onSecondary: Color = C0,
    background: Color = C0, onBackground: Color = C0,
    surface: Color = C0, onSurface: Color = C0,
    surfaceVariant: Color = C0, onSurfaceVariant: Color = C0,
    outline: Color = C0, outlineVariant: Color = C0,
    error: Color = C0, onError: Color = C0,
    errorContainer: Color = C0, onErrorContainer: Color = C0
): ColorScheme = ColorScheme(
    primary, onPrimary, primaryContainer, onPrimaryContainer,
    background, onBackground, surface, onSurface,
    surfaceVariant, onSurfaceVariant, outline, outlineVariant, error, onError
)
class Typography(
    val headlineMedium: TextStyle = T0,
    val titleMedium: TextStyle = T0,
    val bodyLarge: TextStyle = T0,
    val bodyMedium: TextStyle = T0,
    val bodySmall: TextStyle = T0,
    val labelLarge: TextStyle = T0,
    val labelMedium: TextStyle = T0,
    val labelSmall: TextStyle = T0
)
object MaterialTheme {
    val colorScheme: ColorScheme = darkColorScheme()
    val typography: Typography = Typography()
}
fun MaterialTheme(
    colorScheme: ColorScheme,
    typography: Typography,
    content: () -> Unit
) { content() }

fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = C0,
    style: TextStyle = T0,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null
) {}
fun TextButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(),
    content: () -> Unit
) { content() }
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: () -> Unit
) { content() }
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = C0
) {}
fun Surface(
    shape: Shape = ShapeStub,
    color: Color = C0,
    border: BorderStroke? = null,
    modifier: Modifier = Modifier,
    content: () -> Unit
) { content() }
fun Scaffold(
    snackbarHost: () -> Unit = {},
    containerColor: Color = C0,
    bottomBar: () -> Unit = {},
    content: (PaddingValues) -> Unit
) { content(PaddingValues()) }
enum class SnackbarDuration { Short }
enum class SnackbarResult { Dismissed, ActionPerformed }
class SnackbarHostState {
    suspend fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short
    ): SnackbarResult = SnackbarResult.Dismissed
}
fun SnackbarHost(hostState: SnackbarHostState) {}
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: () -> Unit,
    dismissButton: (() -> Unit)? = null,
    title: () -> Unit = {},
    text: () -> Unit = {},
    containerColor: Color = C0
) { title(); text(); confirmButton(); dismissButton?.invoke() }
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: (() -> Unit)? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE
) {}
fun RadioButton(selected: Boolean, onClick: (() -> Unit)?) {}
fun Switch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {}
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    modifier: Modifier = Modifier
) {}
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: () -> Unit
) { content() }
fun DropdownMenuItem(
    text: () -> Unit,
    onClick: () -> Unit,
    enabled: Boolean = true
) { text() }
class NavigationBarItemColors
object NavigationBarItemDefaults {
    fun colors(
        selectedIconColor: Color = C0,
        selectedTextColor: Color = C0,
        indicatorColor: Color = C0,
        unselectedIconColor: Color = C0,
        unselectedTextColor: Color = C0
    ): NavigationBarItemColors = NavigationBarItemColors()
}
fun NavigationBar(
    containerColor: Color = C0,
    tonalElevation: Dp = 0.dp,
    content: () -> Unit
) { content() }
fun NavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: () -> Unit,
    label: (() -> Unit)? = null,
    colors: NavigationBarItemColors = NavigationBarItemDefaults.colors()
) { icon(); label?.invoke() }
