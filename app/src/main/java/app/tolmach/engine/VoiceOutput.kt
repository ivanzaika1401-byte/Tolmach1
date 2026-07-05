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

/** Маршруты вывода звука. Хранятся в настройках как строки. */
object AudioRoutes {
    const val SYSTEM = "system"       // как решит Android (обычно Bluetooth, если подключён)
    const val BLUETOOTH = "bluetooth" // принудительно в Bluetooth-наушники
    const val WIRED = "wired"         // проводные или USB-наушники (вторая пара)
    const val SPEAKER = "speaker"     // динамик телефона
}

private const val RU_DIRECT = "tolmach-ru-direct"
private const val RU_FILE = "tolmach-ru-file"
private const val ZH_FILE = "tolmach-zh-file"
private const val SHARE_FILE = "tolmach-share-file"

/**
 * Озвучивание переводов с гибкой маршрутизацией на два независимых канала.
 *
 * Схема «две пары наушников»: русский перевод — в Bluetooth (русская сторона),
 * китайский — в проводные/USB-наушники (китайская сторона). Каналы играют
 * по очереди (полудуплекс), поэтому одновременный вывод не требуется —
 * каждый канал просто адресуется своему устройству через setPreferredDevice.
 *
 * Русский с маршрутом SYSTEM идёт напрямую (минимальная задержка);
 * любой принудительный маршрут — через синтез в файл и MediaPlayer
 * с выбранным устройством вывода (Android 9+; на Android 8 — системный маршрут).
 */
class VoiceOutput(
    private val context: Context,
    private val onSpeakingChanged: (Boolean) -> Unit,
    private val onReady: (russianAvailable: Boolean, chineseAvailable: Boolean) -> Unit,
    private val onVoicesLoaded: (
        russian: List<Pair<String, String>>,
        chinese: List<Pair<String, String>>,
    ) -> Unit = { _, _ -> },
    private val onVoiceMissing: (chinese: Boolean) -> Unit = {},
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val russianFile = File(context.cacheDir, "tolmach_ru_out.wav")
    private val chineseFile = File(context.cacheDir, "tolmach_zh_out.wav")

    private var tts: TextToSpeech? = null
    private var player: MediaPlayer? = null
    private val pendingRoutes = HashMap<String, String>()
    private var shareCallback: ((Boolean) -> Unit)? = null

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
                val russianVoices = voicesFor(engine, Locale("ru", "RU"))
                val chineseVoices = voicesFor(engine, Locale.SIMPLIFIED_CHINESE)
                mainHandler.post {
                    onReady(russianOk, chineseOk)
                    onVoicesLoaded(russianVoices, chineseVoices)
                }
            } else {
                mainHandler.post { onReady(false, false) }
            }
        }
    }

    /** Русский перевод — по выбранному маршруту и выбранным голосом. */
    fun speakRussian(text: String, route: String, voiceName: String) {
        val engine = tts ?: return
        if (!applyVoice(engine, Locale("ru", "RU"), voiceName)) {
            reportVoiceMissing(chinese = false)
            return
        }
        engine.setSpeechRate(1.0f)
        if (route == AudioRoutes.SYSTEM) {
            setSpeaking(true)
            val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, RU_DIRECT)
            if (result != TextToSpeech.SUCCESS) setSpeaking(false)
        } else {
            speakViaFile(engine, text, route, russianFile, RU_FILE)
        }
    }

    /** Китайский перевод — маршрут, скорость и голос настраиваются. */
    fun speakChineseAloud(text: String, route: String, rate: Float, voiceName: String) {
        val engine = tts ?: return
        if (!applyVoice(engine, Locale.SIMPLIFIED_CHINESE, voiceName)) {
            reportVoiceMissing(chinese = true)
            return
        }
        engine.setSpeechRate(rate)
        speakViaFile(engine, text, route, chineseFile, ZH_FILE)
    }

    /**
     * Тихий синтез китайской фразы в файл — для отправки голосовым
     * сообщением в WeChat, Telegram или WhatsApp. Ничего не проигрывает.
     */
    fun synthesizeChineseFile(
        text: String,
        rate: Float,
        voiceName: String,
        outFile: File,
        onDone: (Boolean) -> Unit,
    ) {
        val engine = tts
        if (engine == null) {
            onDone(false)
            return
        }
        if (!applyVoice(engine, Locale.SIMPLIFIED_CHINESE, voiceName)) {
            mainHandler.post { onVoiceMissing(true) }
            onDone(false)
            return
        }
        engine.setSpeechRate(rate)
        shareCallback = onDone
        val result = engine.synthesizeToFile(text, Bundle(), outFile, SHARE_FILE)
        if (result != TextToSpeech.SUCCESS) {
            shareCallback = null
            onDone(false)
        }
    }

    /** Прерывает любое текущее озвучивание. */
    fun stop() {
        tts?.stop()
        pendingRoutes.clear()
        releasePlayer()
        setSpeaking(false)
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
    }

    /** Устанавливает выбранный голос; при его отсутствии — язык по умолчанию. */
    private fun applyVoice(
        engine: TextToSpeech,
        locale: Locale,
        voiceName: String,
    ): Boolean {
        val voice = if (voiceName.isBlank()) {
            null
        } else {
            runCatching { engine.voices }.getOrNull()
                ?.firstOrNull { it.name == voiceName }
        }
        return if (voice != null) {
            engine.voice = voice
            true
        } else {
            engine.setLanguage(locale) >= 0
        }
    }

    /** До шести голосов языка: локальные первыми, с человеческими подписями. */
    private fun voicesFor(
        engine: TextToSpeech,
        locale: Locale,
    ): List<Pair<String, String>> {
        val all = runCatching { engine.voices }.getOrNull() ?: return emptyList()
        return all
            .filter { it.locale.language == locale.language }
            .sortedWith(compareBy({ it.isNetworkConnectionRequired }, { it.name }))
            .take(6)
            .mapIndexed { index, voice ->
                val kind = if (voice.isNetworkConnectionRequired) "сеть" else "офлайн"
                voice.name to "Голос ${index + 1} · $kind"
            }
    }

    /** Голоса нет: сообщаем и эмулируем цикл озвучки для штатного продолжения. */
    private fun reportVoiceMissing(chinese: Boolean) {
        mainHandler.post { onVoiceMissing(chinese) }
        setSpeaking(true)
        setSpeaking(false)
    }

    private fun speakViaFile(
        engine: TextToSpeech,
        text: String,
        route: String,
        file: File,
        utteranceId: String,
    ) {
        setSpeaking(true)
        pendingRoutes[utteranceId] = route
        val result = engine.synthesizeToFile(text, Bundle(), file, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            pendingRoutes.remove(utteranceId)
            setSpeaking(false)
        }
    }

    private val progressListener = object : UtteranceProgressListener() {

        override fun onStart(utteranceId: String?) {
            if (utteranceId == RU_DIRECT) setSpeaking(true)
        }

        override fun onDone(utteranceId: String?) {
            when (utteranceId) {
                RU_DIRECT -> setSpeaking(false)
                RU_FILE -> mainHandler.post {
                    playFile(russianFile, pendingRoutes.remove(RU_FILE) ?: AudioRoutes.SYSTEM)
                }
                ZH_FILE -> mainHandler.post {
                    playFile(chineseFile, pendingRoutes.remove(ZH_FILE) ?: AudioRoutes.SPEAKER)
                }
                SHARE_FILE -> mainHandler.post {
                    shareCallback?.invoke(true)
                    shareCallback = null
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            if (utteranceId == SHARE_FILE) {
                mainHandler.post {
                    shareCallback?.invoke(false)
                    shareCallback = null
                }
                return
            }
            if (utteranceId != null) pendingRoutes.remove(utteranceId)
            setSpeaking(false)
        }
    }

    private fun playFile(file: File, route: String) {
        releasePlayer()
        try {
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            mediaPlayer.setDataSource(file.absolutePath)
            mediaPlayer.prepare()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                resolveDevice(route)?.let { mediaPlayer.setPreferredDevice(it) }
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

    /**
     * Подбирает физическое устройство под маршрут. Если нужного нет —
     * мягкая деградация: китайский упадёт на динамик, русский — на систему.
     */
    private fun resolveDevice(route: String): AudioDeviceInfo? {
        if (route == AudioRoutes.SYSTEM) return null
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        fun firstOf(types: IntArray): AudioDeviceInfo? =
            outputs.firstOrNull { types.contains(it.type) }

        return when (route) {
            AudioRoutes.SPEAKER -> firstOf(intArrayOf(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER))

            AudioRoutes.WIRED -> firstOf(
                intArrayOf(
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_USB_HEADSET,
                    AudioDeviceInfo.TYPE_USB_DEVICE,
                ),
            ) ?: firstOf(intArrayOf(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER))

            AudioRoutes.BLUETOOTH -> firstOf(
                intArrayOf(
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLE_HEADSET,
                ),
            )

            else -> null
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
