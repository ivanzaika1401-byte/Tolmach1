package app.tolmach

import android.app.Application
import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.tolmach.engine.SpeechEngine
import app.tolmach.engine.TranslationEngine
import app.tolmach.engine.VoiceOutput
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Mode { IDLE, LISTEN_CHINESE, REPLY_RUSSIAN }

data class ChatMessage(
    val id: Long,
    val original: String,
    val translated: String,
    val fromChinese: Boolean,
    val time: String,
)

data class GlossaryEntry(val from: String, val to: String)

data class UiState(
    val mode: Mode = Mode.IDLE,
    val listening: Boolean = false,
    val speaking: Boolean = false,
    val partial: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val modelsReady: Boolean = false,
    val modelDownloadFailed: Boolean = false,
    val modelStatus: String = "Готовлю офлайн-модели перевода…",
    val chineseLanguageTag: String = "zh-CN",
    val headsetConnected: Boolean = false,
    val glossary: List<GlossaryEntry> = emptyList(),
    val error: String? = null,
)

class TranslatorViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val prefs = app.getSharedPreferences("tolmach", Context.MODE_PRIVATE)
    private val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private var messageCounter = 0L
    private var resumeListeningAfterReply = false

    private val translation = TranslationEngine()

    private val speech = SpeechEngine(
        context = app,
        onPartial = { text -> _state.update { it.copy(partial = text) } },
        onFinal = { text -> mainHandler.post { handleFinalResult(text) } },
        onError = { message ->
            _state.update { it.copy(error = message) }
            mainHandler.post { resumeAfterInterruption() }
        },
        onStateChange = { active -> _state.update { it.copy(listening = active) } },
    )

    private val voice = VoiceOutput(
        context = app,
        onSpeakingChanged = { speaking ->
            _state.update { it.copy(speaking = speaking) }
            if (!speaking) resumeAfterSpeech()
        },
        onReady = { russianOk, chineseOk ->
            if (!russianOk || !chineseOk) {
                val missing = buildList {
                    if (!russianOk) add("русского")
                    if (!chineseOk) add("китайского")
                }.joinToString(" и ")
                _state.update {
                    it.copy(
                        error = "В синтезаторе речи нет голоса для $missing языка. " +
                            "Откройте настройки Android → Специальные возможности → " +
                            "Синтез речи и докачайте языки.",
                    )
                }
            }
        },
    )

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) =
            updateHeadsetState()

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) =
            updateHeadsetState()
    }

    init {
        _state.update {
            it.copy(
                glossary = loadGlossary(),
                chineseLanguageTag = prefs.getString("chinese_language", "zh-CN") ?: "zh-CN",
            )
        }
        updateHeadsetState()
        audioManager.registerAudioDeviceCallback(deviceCallback, mainHandler)
        downloadModelsAsync()
    }

    // ---------- Действия пользователя ----------

    /** Кнопка «Слушать китайский» — включает и выключает непрерывное прослушивание. */
    fun toggleListening() {
        if (_state.value.mode == Mode.LISTEN_CHINESE) {
            _state.update { it.copy(mode = Mode.IDLE, partial = "") }
            speech.stop()
            voice.stop()
        } else {
            resumeListeningAfterReply = false
            voice.stop()
            _state.update { it.copy(mode = Mode.LISTEN_CHINESE, partial = "") }
            speech.start(_state.value.chineseLanguageTag)
        }
    }

    /**
     * Кнопка «Ответить по-русски»: записывает одну вашу фразу и озвучивает её
     * по-китайски через динамик. Если до этого шло прослушивание китайского,
     * оно автоматически возобновится после ответа.
     */
    fun startRussianReply() {
        if (_state.value.mode == Mode.REPLY_RUSSIAN) return
        resumeListeningAfterReply = _state.value.mode == Mode.LISTEN_CHINESE
        speech.stop()
        voice.stop()
        _state.update { it.copy(mode = Mode.REPLY_RUSSIAN, partial = "") }
        speech.start("ru-RU")
    }

    /** Смена языка собеседника: путунхуа, тайваньский мандарин или кантонский. */
    fun setChineseLanguage(tag: String) {
        prefs.edit().putString("chinese_language", tag).apply()
        val wasListening = _state.value.mode == Mode.LISTEN_CHINESE
        _state.update { it.copy(chineseLanguageTag = tag) }
        if (wasListening) {
            speech.stop()
            speech.start(tag)
        }
    }

    /**
     * Повторная озвучка реплики (кнопка-динамик в пузыре): фразы партнёра —
     * снова в наушники, ваши фразы — снова по-китайски через динамик.
     * Прослушивание при этом ставится на паузу и само возобновится.
     */
    fun replayMessage(message: ChatMessage) {
        if (_state.value.mode == Mode.REPLY_RUSSIAN) return
        speech.stop()
        voice.stop()
        if (message.fromChinese) {
            voice.speakRussian(message.translated)
        } else {
            voice.speakChineseAloud(message.translated)
        }
    }

    /** Повторная попытка скачать офлайн-модели перевода — без перезапуска приложения. */
    fun retryModelDownload() {
        if (_state.value.modelsReady) return
        _state.update {
            it.copy(modelDownloadFailed = false, modelStatus = "Загружаю модели перевода…")
        }
        downloadModelsAsync()
    }

    fun clearConversation() {
        _state.update { it.copy(messages = emptyList()) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun addGlossaryEntry(from: String, to: String) {
        val updated = _state.value.glossary
            .filterNot { it.from.equals(from, ignoreCase = true) } + GlossaryEntry(from, to)
        _state.update { it.copy(glossary = updated) }
        persistGlossary(updated)
    }

    fun removeGlossaryEntry(entry: GlossaryEntry) {
        val updated = _state.value.glossary - entry
        _state.update { it.copy(glossary = updated) }
        persistGlossary(updated)
    }

    /** Готовая стенограмма переговоров для отправки или сохранения. */
    fun transcriptText(): String = buildString {
        val header = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale("ru")).format(Date())
        appendLine("Переговоры — $header")
        appendLine("Составлено приложением «Толмач»")
        appendLine()
        _state.value.messages.forEach { message ->
            val speaker = if (message.fromChinese) "Партнёр" else "Вы"
            appendLine("[${message.time}] $speaker: ${message.original}")
            appendLine("    Перевод: ${message.translated}")
            appendLine()
        }
    }

    // ---------- Внутренняя логика ----------

    private fun downloadModelsAsync() {
        viewModelScope.launch {
            try {
                translation.downloadModels(allowMobileData = true)
                _state.update {
                    it.copy(
                        modelsReady = true,
                        modelDownloadFailed = false,
                        modelStatus = "Модели перевода готовы, работают офлайн",
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        modelDownloadFailed = true,
                        modelStatus = "Модели не загрузились — нужен интернет",
                        error = "Не удалось скачать модели перевода. Проверьте интернет " +
                            "и нажмите «Повторить загрузку моделей».",
                    )
                }
            }
        }
    }

    private fun handleFinalResult(raw: String) {
        val text = raw.trim()
        _state.update { it.copy(partial = "") }

        if (text.isEmpty()) {
            // Тишина: в режиме прослушивания просто продолжаем слушать.
            resumeAfterInterruption()
            return
        }

        when (_state.value.mode) {
            Mode.LISTEN_CHINESE -> viewModelScope.launch {
                try {
                    val translated = applyGlossary(translation.translateChineseToRussian(text))
                    addMessage(original = text, translated = translated, fromChinese = true)
                    voice.speakRussian(translated)
                } catch (e: Exception) {
                    _state.update {
                        it.copy(error = "Перевод не удался: ${e.message ?: "проверьте модели"}")
                    }
                    restartRecognitionSoon(_state.value.chineseLanguageTag)
                }
            }

            Mode.REPLY_RUSSIAN -> viewModelScope.launch {
                try {
                    val translated = applyGlossary(translation.translateRussianToChinese(text))
                    addMessage(original = text, translated = translated, fromChinese = false)
                    voice.speakChineseAloud(translated)
                } catch (e: Exception) {
                    _state.update {
                        it.copy(error = "Перевод не удался: ${e.message ?: "проверьте модели"}")
                    }
                    finishReply()
                }
            }

            Mode.IDLE -> Unit
        }
    }

    /** После окончания озвучки возвращаемся к прослушиванию (полудуплекс). */
    private fun resumeAfterSpeech() {
        if (speech.isListening) return
        when (_state.value.mode) {
            Mode.LISTEN_CHINESE -> restartRecognitionSoon(_state.value.chineseLanguageTag)
            Mode.REPLY_RUSSIAN -> finishReply()
            Mode.IDLE -> Unit
        }
    }

    /** После тишины или сбоя распознавания продолжаем текущий режим. */
    private fun resumeAfterInterruption() {
        if (speech.isListening || _state.value.speaking) return
        when (_state.value.mode) {
            Mode.LISTEN_CHINESE -> restartRecognitionSoon(_state.value.chineseLanguageTag)
            Mode.REPLY_RUSSIAN -> finishReply()
            Mode.IDLE -> Unit
        }
    }

    private fun finishReply() {
        val next = if (resumeListeningAfterReply) Mode.LISTEN_CHINESE else Mode.IDLE
        resumeListeningAfterReply = false
        _state.update { it.copy(mode = next) }
        if (next == Mode.LISTEN_CHINESE) {
            restartRecognitionSoon(_state.value.chineseLanguageTag)
        }
    }

    private fun restartRecognitionSoon(languageTag: String) {
        viewModelScope.launch {
            delay(300)
            val current = _state.value
            val expectedMode =
                if (languageTag == "ru-RU") Mode.REPLY_RUSSIAN else Mode.LISTEN_CHINESE
            if (current.mode == expectedMode && !current.speaking && !speech.isListening) {
                speech.start(languageTag)
            }
        }
    }

    private fun addMessage(original: String, translated: String, fromChinese: Boolean) {
        val message = ChatMessage(
            id = ++messageCounter,
            original = original,
            translated = translated,
            fromChinese = fromChinese,
            time = timeFormat.format(Date()),
        )
        _state.update { it.copy(messages = it.messages + message) }
    }

    private fun applyGlossary(text: String): String {
        var result = text
        for (entry in _state.value.glossary) {
            result = result.replace(entry.from, entry.to, ignoreCase = true)
        }
        return result
    }

    private fun loadGlossary(): List<GlossaryEntry> =
        prefs.getString("glossary", "").orEmpty()
            .lineSequence()
            .mapNotNull { line ->
                val parts = line.split("|||")
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    GlossaryEntry(parts[0], parts[1])
                } else {
                    null
                }
            }
            .toList()

    private fun persistGlossary(entries: List<GlossaryEntry>) {
        prefs.edit()
            .putString("glossary", entries.joinToString("\n") { "${it.from}|||${it.to}" })
            .apply()
    }

    private fun updateHeadsetState() {
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val connected = outputs.any { device ->
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    device.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
        }
        _state.update { it.copy(headsetConnected = connected) }
    }

    override fun onCleared() {
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
        speech.stop()
        voice.shutdown()
        translation.close()
        super.onCleared()
    }
}
