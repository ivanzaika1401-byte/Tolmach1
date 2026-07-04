package kotlinx.coroutines
object Dispatchers { val IO: Any = Any() }
suspend fun <T> withContext(context: Any, block: suspend () -> T): T = block()
class Job { fun cancel() {} }
class CoroutineScope
/** No-op: тело не исполняется — детерминизм smoke-запуска. */
fun CoroutineScope.launch(block: suspend CoroutineScope.() -> Unit): Job = Job()
suspend fun delay(timeMillis: Long) {}
