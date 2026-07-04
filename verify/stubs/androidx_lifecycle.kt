package androidx.lifecycle
import android.app.Application
abstract class ViewModel { protected open fun onCleared() {} }
open class AndroidViewModel(private val application: Application) : ViewModel() {
    fun <T : Application> getApplication(): T {
        @Suppress("UNCHECKED_CAST")
        return application as T
    }
}
val ViewModel.viewModelScope: kotlinx.coroutines.CoroutineScope
    get() = kotlinx.coroutines.CoroutineScope()
