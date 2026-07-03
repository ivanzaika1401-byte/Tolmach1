package app.tolmach.engine

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Обёртка над системным SpeechRecognizer.
 *
 * Работает в режиме "одна фраза за запуск": после финального результата
 * ViewModel сама решает, перезапускать ли прослушивание (полудуплекс —
 * пока телефон озвучивает перевод, микрофон молчит, чтобы не ловить эхо).
 *
 * Все методы должны вызываться из главного потока — это требование
 * самого SpeechRecognizer.
 */
class SpeechEngine(
    private val context: Context,
    private val onPartial: (String) -> Unit,
    private val onFinal: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onStateChange: (Boolean) -> Unit,
) {

    private var recognizer: SpeechRecognizer? = null
    private var language: String = "zh-CN"

    var isListening: Boolean = false
        private set

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    /** Запускает распознавание одной фразы на языке [languageTag] (например, "zh-CN" или "ru-RU"). */
    fun start(languageTag: String) {
        if (!isAvailable()) {
            onError("На этом телефоне нет сервиса распознавания речи. Установите приложение Google.")
            return
        }
        language = languageTag
        destroyRecognizer()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).also {
            it.setRecognitionListener(listener)
            it.startListening(buildIntent())
        }
        isListening = true
        onStateChange(true)
    }

    /** Полностью останавливает прослушивание. */
    fun stop() {
        val wasListening = isListening
        isListening = false
        destroyRecognizer()
        if (wasListening) onStateChange(false)
    }

    private fun destroyRecognizer() {
        recognizer?.let {
            runCatching { it.cancel() }
            runCatching { it.destroy() }
        }
        recognizer = null
    }

    private fun buildIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Даём собеседнику договорить: пауза до ~1,2 с не обрывает фразу.
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                1200,
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                1200,
            )
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

    private val listener = object : RecognitionListener {

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            if (!text.isNullOrBlank()) onPartial(text)
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            isListening = false
            onStateChange(false)
            onFinal(text)
        }

        override fun onError(error: Int) {
            isListening = false
            onStateChange(false)
            when (error) {
                // Тишина или неразборчивая речь — не ошибка, просто пустой результат:
                // ViewModel перезапустит прослушивание.
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                SpeechRecognizer.ERROR_CLIENT,
                -> onFinal("")

                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                    onError("Нет доступа к микрофону — разрешите запись звука в настройках.")

                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> onFinal("")

                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                -> onError("Распознаванию нужен интернет — проверьте подключение.")

                else -> onError("Распознавание прервалось (код $error). Пробую снова.")
            }
        }

        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }
}
