package android.content
import java.io.File
open class Context {
    open fun getSystemService(name: String): Any? =
        if (name == AUDIO_SERVICE) android.media.AudioManager() else null
    open val cacheDir: File = File("/tmp")
    open val packageName: String = "app.tolmach"
    open val packageManager: android.content.pm.PackageManager
        get() = android.content.pm.PackageManager()
    open fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
        MemoryPrefs.of(name)
    open fun startActivity(intent: Intent) {}
    companion object {
        const val AUDIO_SERVICE = "audio"
        const val MODE_PRIVATE = 0
    }
}
class Intent(action: String) {
    var type: String? = null
    fun putExtra(key: String, value: String): Intent = this
    fun putExtra(key: String, value: Boolean): Intent = this
    fun putExtra(key: String, value: Int): Intent = this
    fun putExtra(key: String, value: android.net.Uri): Intent = this
    fun addFlags(flags: Int): Intent = this
    companion object {
        const val ACTION_SEND = "android.intent.action.SEND"
        const val EXTRA_TEXT = "android.intent.extra.TEXT"
        const val EXTRA_STREAM = "android.intent.extra.STREAM"
        const val FLAG_GRANT_READ_URI_PERMISSION = 1
        fun createChooser(target: Intent, title: CharSequence?): Intent = target
    }
}
interface SharedPreferences {
    fun getString(key: String, defValue: String?): String?
    fun getBoolean(key: String, defValue: Boolean): Boolean
    fun getFloat(key: String, defValue: Float): Float
    fun edit(): Editor
    interface Editor {
        fun putString(key: String, value: String?): Editor
        fun putBoolean(key: String, value: Boolean): Editor
        fun putFloat(key: String, value: Float): Editor
        fun remove(key: String): Editor
        fun apply()
    }
}
class MemoryPrefs private constructor() : SharedPreferences {
    private val map = HashMap<String, Any?>()
    override fun getString(key: String, defValue: String?): String? =
        map[key] as? String ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        map[key] as? Boolean ?: defValue
    override fun getFloat(key: String, defValue: Float): Float =
        map[key] as? Float ?: defValue
    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            map[key] = value; return this
        }
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            map[key] = value; return this
        }
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            map[key] = value; return this
        }
        override fun remove(key: String): SharedPreferences.Editor {
            map.remove(key); return this
        }
        override fun apply() {}
    }
    companion object {
        private val stores = HashMap<String, MemoryPrefs>()
        fun of(name: String): MemoryPrefs = stores.getOrPut(name) { MemoryPrefs() }
    }
}
