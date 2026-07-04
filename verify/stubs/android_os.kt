package android.os
class Looper private constructor() {
    companion object { fun getMainLooper(): Looper = Looper() }
}
class Handler(looper: Looper) { fun post(block: () -> Unit): Boolean { block(); return true } }
class Bundle { fun getStringArrayList(key: String): java.util.ArrayList<String>? = null }
object Build {
    object VERSION { const val SDK_INT = 34 }
    object VERSION_CODES { const val P = 28; const val S = 31 }
}
