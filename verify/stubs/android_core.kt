package android.content
import java.io.File
open class Context {
    open fun getSystemService(name: String): Any? =
        if (name == AUDIO_SERVICE) android.media.AudioManager() else null
    open val cacheDir: File = File("/tmp")
    open val packageName: String = "app.tolmach"
    open fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
        MemoryPrefs.of(name)
    companion object {
        const val AUDIO_SERVICE = "audio"
        const val MODE_PRIVATE = 0
    }
}
class Intent(action: String) {
    fun putExtra(key: String, value: String): Intent = this
    fun putExtra(key: String, value: Boolean): Intent = this
    fun putExtra(key: String, value: Int): Intent = this
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
/** Рабочее in-memory хранилище: персистентность проверяется по-настоящему. */
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
