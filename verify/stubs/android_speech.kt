package android.speech
import android.content.Context
import android.content.Intent
import android.os.Bundle
interface RecognitionListener {
    fun onReadyForSpeech(params: Bundle?); fun onBeginningOfSpeech()
    fun onRmsChanged(rmsdB: Float); fun onBufferReceived(buffer: ByteArray?)
    fun onEndOfSpeech(); fun onError(error: Int)
    fun onResults(results: Bundle?); fun onPartialResults(partialResults: Bundle?)
    fun onEvent(eventType: Int, params: Bundle?)
}
class SpeechRecognizer private constructor() {
    fun setRecognitionListener(listener: RecognitionListener) {}
    fun startListening(intent: Intent) {}
    fun cancel() {}
    fun destroy() {}
    companion object {
        const val RESULTS_RECOGNITION = "results"
        const val ERROR_NO_MATCH = 7; const val ERROR_SPEECH_TIMEOUT = 6
        const val ERROR_CLIENT = 5; const val ERROR_INSUFFICIENT_PERMISSIONS = 9
        const val ERROR_RECOGNIZER_BUSY = 8; const val ERROR_NETWORK = 2
        const val ERROR_NETWORK_TIMEOUT = 1
        fun isRecognitionAvailable(context: Context): Boolean = true
        fun createSpeechRecognizer(context: Context): SpeechRecognizer = SpeechRecognizer()
    }
}
object RecognizerIntent {
    const val ACTION_RECOGNIZE_SPEECH = "a"; const val EXTRA_LANGUAGE_MODEL = "b"
    const val LANGUAGE_MODEL_FREE_FORM = "c"; const val EXTRA_LANGUAGE = "d"
    const val EXTRA_LANGUAGE_PREFERENCE = "e"; const val EXTRA_PARTIAL_RESULTS = "f"
    const val EXTRA_MAX_RESULTS = "g"; const val EXTRA_CALLING_PACKAGE = "h"
    const val EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS = "i"
    const val EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS = "j"
}
