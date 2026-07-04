package kotlinx.coroutines.tasks
import com.google.android.gms.tasks.Task
@Suppress("UNCHECKED_CAST")
suspend fun <T> Task<T>.await(): T = Any() as T
