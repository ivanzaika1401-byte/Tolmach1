package androidx.compose.foundation.layout
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
class ArrangementSpec
object Arrangement {
    val Center = ArrangementSpec()
    fun spacedBy(space: Dp): ArrangementSpec = ArrangementSpec()
}
class PaddingValues(
    horizontal: Dp = Dp(0f),
    vertical: Dp = Dp(0f)
)
interface RowScope { fun Modifier.weight(weight: Float): Modifier }
interface ColumnScope { fun Modifier.weight(weight: Float): Modifier }
private object RowScopeInstance : RowScope {
    override fun Modifier.weight(weight: Float): Modifier = this
}
private object ColumnScopeInstance : ColumnScope {
    override fun Modifier.weight(weight: Float): Modifier = this
}
fun Row(
    modifier: Modifier = Modifier,
    horizontalArrangement: ArrangementSpec = Arrangement.Center,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: RowScope.() -> Unit
) { RowScopeInstance.content() }
fun Column(
    modifier: Modifier = Modifier,
    verticalArrangement: ArrangementSpec = Arrangement.Center,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: ColumnScope.() -> Unit
) { ColumnScopeInstance.content() }
fun Box(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: () -> Unit = {}
) { content() }
fun Spacer(modifier: Modifier) {}
fun Modifier.fillMaxSize(): Modifier = this
fun Modifier.fillMaxWidth(): Modifier = this
fun Modifier.fillMaxHeight(): Modifier = this
fun Modifier.height(height: Dp): Modifier = this
fun Modifier.width(width: Dp): Modifier = this
fun Modifier.size(size: Dp): Modifier = this
fun Modifier.heightIn(max: Dp): Modifier = this
fun Modifier.widthIn(max: Dp): Modifier = this
fun Modifier.padding(all: Dp): Modifier = this
fun Modifier.padding(horizontal: Dp = Dp(0f), vertical: Dp = Dp(0f)): Modifier = this
fun Modifier.padding(
    start: Dp = Dp(0f),
    top: Dp = Dp(0f),
    end: Dp = Dp(0f),
    bottom: Dp = Dp(0f)
): Modifier = this
fun Modifier.padding(paddingValues: PaddingValues): Modifier = this
