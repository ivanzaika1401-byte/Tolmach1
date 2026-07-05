package androidx.compose.runtime
import kotlin.reflect.KProperty
annotation class Composable
class MutableState<T>(var value: T)
fun <T> mutableStateOf(value: T): MutableState<T> = MutableState(value)
fun mutableIntStateOf(value: Int): MutableState<Int> = MutableState(value)
operator fun <T> MutableState<T>.getValue(thisRef: Any?, property: KProperty<*>): T = value
operator fun <T> MutableState<T>.setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
    value = newValue
}
fun <T> remember(calculation: () -> T): T = calculation()
fun <T> remember(key1: Any?, calculation: () -> T): T = calculation()
fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsState(): MutableState<T> =
    MutableState(value)
fun LaunchedEffect(key1: Any?, block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {}
class DisposableEffectResult
class DisposableEffectScope {
    fun onDispose(onDisposeEffect: () -> Unit): DisposableEffectResult = DisposableEffectResult()
}
fun DisposableEffect(
    key1: Any?,
    key2: Any?,
    effect: DisposableEffectScope.() -> DisposableEffectResult
) {}
fun rememberCoroutineScope(): kotlinx.coroutines.CoroutineScope =
    kotlinx.coroutines.CoroutineScope()
