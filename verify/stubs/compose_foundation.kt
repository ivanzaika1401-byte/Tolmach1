package androidx.compose.foundation
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
class BorderStroke(width: Dp, color: Color)
private object DefaultShape : Shape
fun Modifier.background(color: Color, shape: Shape = DefaultShape): Modifier = this
fun Modifier.background(brush: Brush): Modifier = this
fun Modifier.border(width: Dp, color: Color, shape: Shape): Modifier = this
fun Modifier.clickable(enabled: Boolean = true, onClick: () -> Unit): Modifier = this
class ScrollState
fun rememberScrollState(): ScrollState = ScrollState()
fun Modifier.verticalScroll(state: ScrollState): Modifier = this
