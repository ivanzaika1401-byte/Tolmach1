package androidx.compose.animation
import androidx.compose.animation.core.AnimSpec
import androidx.compose.animation.core.MutableTransitionState
class EnterTransition { operator fun plus(other: EnterTransition): EnterTransition = this }
class ExitTransition { operator fun plus(other: ExitTransition): ExitTransition = this }
fun fadeIn(animationSpec: AnimSpec = AnimSpec()): EnterTransition = EnterTransition()
fun fadeOut(animationSpec: AnimSpec = AnimSpec()): ExitTransition = ExitTransition()
fun expandVertically(): EnterTransition = EnterTransition()
fun shrinkVertically(): ExitTransition = ExitTransition()
fun slideInVertically(initialOffsetY: (Int) -> Int): EnterTransition = EnterTransition()
fun AnimatedVisibility(
    visible: Boolean,
    enter: EnterTransition = EnterTransition(),
    exit: ExitTransition = ExitTransition(),
    content: () -> Unit
) { content() }
fun AnimatedVisibility(
    visibleState: MutableTransitionState<Boolean>,
    enter: EnterTransition = EnterTransition(),
    content: () -> Unit
) { content() }
fun <T> Crossfade(targetState: T, label: String = "", content: (T) -> Unit) {
    content(targetState)
}
