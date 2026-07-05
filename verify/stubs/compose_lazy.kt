package androidx.compose.foundation.lazy
import androidx.compose.foundation.layout.ArrangementSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
class LazyListState { suspend fun animateScrollToItem(index: Int) {} }
fun rememberLazyListState(): LazyListState = LazyListState()
interface LazyListScope
private object LazyScopeInstance : LazyListScope
fun <T> LazyListScope.items(
    items: List<T>,
    key: ((T) -> Any)? = null,
    itemContent: (T) -> Unit
) {}
fun LazyColumn(
    state: LazyListState = LazyListState(),
    modifier: Modifier = Modifier,
    verticalArrangement: ArrangementSpec = Arrangement.Center,
    contentPadding: PaddingValues = PaddingValues(),
    content: LazyListScope.() -> Unit
) { LazyScopeInstance.content() }
