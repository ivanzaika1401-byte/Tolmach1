package kotlinx.coroutines.flow
interface StateFlow<T> { val value: T }
class MutableStateFlow<T>(initial: T) : StateFlow<T> {
    override var value: T = initial
}
fun <T> MutableStateFlow<T>.asStateFlow(): StateFlow<T> = this
fun <T> MutableStateFlow<T>.update(function: (T) -> T) { value = function(value) }
