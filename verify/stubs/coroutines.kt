package kotlinx.coroutines

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

object Dispatchers { val IO: Any = Any() }

suspend fun <T> withContext(context: Any, block: suspend () -> T): T = block()

class Job { fun cancel() {} }

class CoroutineScope

/**
 * Трамплин: блок исполняется синхронно до завершения (наши стабы не
 * приостанавливаются). Исключения пробрасываются — smoke их увидит.
 * ВНИМАНИЕ: бесконечные циклы с delay зациклятся — smoke не должен
 * доводить звонок до connected (тикер).
 */
fun CoroutineScope.launch(block: suspend CoroutineScope.() -> Unit): Job {
    block.startCoroutine(
        this,
        object : Continuation<Unit> {
            override val context: CoroutineContext = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) {
                val failure = result.exceptionOrNull()
                if (failure != null && failure !is StubDelayLimit) {
                    throw failure
                }
            }
        }
    )
    return Job()
}

class StubDelayLimit : RuntimeException()

private var delayCalls = 0

/** Тикеры вида while(true){delay(...)} детерминированно завершаются. */
suspend fun delay(timeMillis: Long) {
    delayCalls++
    if (delayCalls > 5) {
        delayCalls = 0
        throw StubDelayLimit()
    }
}
