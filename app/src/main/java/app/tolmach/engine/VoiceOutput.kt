package app.tolmach.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.File
import java.util.Locale

private const val RUSSIAN_UTTERANCE = "tolmach-russian"
private const val CHINESE_FILE_UTTERANCE = "tolmach-chinese-file"

/**
 * Озвучивание переводов с раздельной маршрутизацией звука:
 *
 *  - Русский перевод идёт обычным путём (TextToSpeech.speak) — когда подключены
 *    Bluetooth-наушники, Android сам направит его туда. Слышите только вы.
 *
 *  - Китайский перевод должен слышать собеседник, поэтому фраза синтезируется
 *    в файл и проигрывается через MediaPlayer с принудительной маршрутизацией
 *    на встроенный динамик телефона (setPreferredDevice, Android 9+).
 *    На Android 8 звук пойдёт по стандартному маршруту.
 */
class VoiceOutput(
    private val context: Context,
    private val onSpeakingChanged: (Boolean) -> Unit,
    private val onReady: (russianAvailable: Boolean, chineseAvailable: Boolean) -> Unit,
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val chineseFile = File(context.cacheDir, "tolmach_zh_out.wav")

    private var tts: TextToSpeech? = null
    private var player: MediaPlayer? = null

    @Volatile
    private var speaking = false

    init {
        tts = TextToSpeech(context) { status ->
            val engine = tts
            if (status == TextToSpeech.SUCCESS && engine != null) {
                engine.setOnUtteranceProgressListener(progressListener)
                val russianOk =
                    engine.isLanguageAvailable(Locale("ru", "RU")) >= TextToSpeech.LANG_AVAILABLE
                val chineseOk =
                    engine.isLanguageAvailable(Locale.SIMPLIFIED_CHINESE) >= TextToSpeech.LANG_AVAILABLE
                mainHandler.post { onReady(russianOk, chineseOk) }
            } else {
                mainHandler.post { onReady(false, false) }
            }
        }
    }

    /** Русский перевод — в наушники (стандартный аудиомаршрут). */
    fun speakRussian(text: String) {
        val engine = tts ?: return
        engine.setLanguage(Locale("ru", "RU"))
        setSpeaking(true)
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, RUSSIAN_UTTERANCE)
        if (result != TextToSpeech.SUCCESS) setSpeaking(false)
    }

    /** Китайский перевод — вслух, через динамик телефона, для собеседника. */
    fun speakChineseAloud(text: String) {
        val engine = tts ?: return
        engine.setLanguage(Locale.SIMPLIFIED_CHINESE)
        setSpeaking(true)
        val result =
            engine.synthesizeToFile(text, Bundle(), chineseFile, CHINESE_FILE_UTTERANCE)
        if (result != TextToSpeech.SUCCESS) setSpeaking(false)
    }

    /** Прерывает любое текущее озвучивание. */
    fun stop() {
        tts?.stop()
        releasePlayer()
        setSpeaking(false)
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
    }

    private val progressListener = object : UtteranceProgressListener() {

        override fun onStart(utteranceId: String?) {
            if (utteranceId == RUSSIAN_UTTERANCE) setSpeaking(true)
        }

        override fun onDone(utteranceId: String?) {
            when (utteranceId) {
                RUSSIAN_UTTERANCE -> setSpeaking(false)
                CHINESE_FILE_UTTERANCE -> mainHandler.post { playChineseFile() }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            setSpeaking(false)
        }
    }

    private fun playChineseFile() {
        releasePlayer()
        try {
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            mediaPlayer.setDataSource(chineseFile.absolutePath)
            mediaPlayer.prepare()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val speaker = audioManager
                    .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                    .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                if (speaker != null) mediaPlayer.setPreferredDevice(speaker)
            }
            mediaPlayer.setOnCompletionListener {
                releasePlayer()
                setSpeaking(false)
            }
            player = mediaPlayer
            mediaPlayer.start()
        } catch (_: Exception) {
            releasePlayer()
            setSpeaking(false)
        }
    }

    private fun releasePlayer() {
        player?.let { runCatching { it.release() } }
        player = null
    }

    private fun setSpeaking(value: Boolean) {
        if (speaking == value) return
        speaking = value
        mainHandler.post { onSpeakingChanged(value) }
    }
}
