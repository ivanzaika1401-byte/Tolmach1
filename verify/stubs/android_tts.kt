package android.speech.tts
import android.content.Context
import android.os.Bundle
import java.io.File
import java.util.Locale
class Voice(val name: String, val locale: Locale, val isNetworkConnectionRequired: Boolean)
abstract class UtteranceProgressListener {
    abstract fun onStart(utteranceId: String?)
    abstract fun onDone(utteranceId: String?)
    abstract fun onError(utteranceId: String?)
}
class TextToSpeech(context: Context, onInit: (Int) -> Unit) {
    var voice: Voice? = null
    val voices: Set<Voice>? = emptySet()
    fun setOnUtteranceProgressListener(listener: UtteranceProgressListener): Int = 0
    fun isLanguageAvailable(locale: Locale): Int = 0
    fun setLanguage(locale: Locale): Int = 0
    fun setSpeechRate(rate: Float): Int = 0
    fun speak(text: String, queueMode: Int, params: Bundle?, utteranceId: String): Int = 0
    fun synthesizeToFile(text: String, params: Bundle, file: File, utteranceId: String): Int = 0
    fun stop(): Int = 0
    fun shutdown() {}
    companion object {
        const val SUCCESS = 0; const val QUEUE_FLUSH = 0; const val LANG_AVAILABLE = 0
    }
}
