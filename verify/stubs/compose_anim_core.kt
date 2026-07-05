package androidx.compose.animation.core
class AnimSpec
class Easing
val FastOutSlowInEasing = Easing()
fun tween(durationMillis: Int, easing: Easing = FastOutSlowInEasing): AnimSpec = AnimSpec()
enum class RepeatMode { Restart, Reverse }
fun infiniteRepeatable(animation: AnimSpec, repeatMode: RepeatMode = RepeatMode.Restart): AnimSpec =
    AnimSpec()
class InfiniteTransition
fun InfiniteTransition.animateFloat(
    initialValue: Float,
    targetValue: Float,
    animationSpec: AnimSpec,
    label: String = ""
): androidx.compose.runtime.MutableState<Float> =
    androidx.compose.runtime.MutableState(initialValue)
fun rememberInfiniteTransition(label: String = ""): InfiniteTransition = InfiniteTransition()
fun animateFloatAsState(
    targetValue: Float,
    animationSpec: AnimSpec = AnimSpec(),
    label: String = ""
): androidx.compose.runtime.MutableState<Float> =
    androidx.compose.runtime.MutableState(targetValue)
class MutableTransitionState<T>(initialState: T) { var targetState: T = initialState }
