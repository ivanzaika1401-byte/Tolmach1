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
import app.tolmach.engine.AudioRoutes
import app.tolmach.engine.SpeechEngine
import app.tolmach.engine.TranslationEngine
import app.tolmach.engine.VoiceOutput
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
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
    val sessionStart: String? = null,
    val modelsReady: Boolean = false,
    val modelDownloadFailed: Boolean = false,
    val modelStatus: String = "Готовлю офлайн-модели перевода…",
    val chineseLanguageTag: String = "zh-CN",
    val bluetoothConnected: Boolean = false,
    val wiredConnected: Boolean = false,
    val russianRoute: String = AudioRoutes.SYSTEM,
    val chineseRoute: String = AudioRoutes.SPEAKER,
    val glossary: List<GlossaryEntry> = emptyList(),
    val customPhrases: List<Phrase> = emptyList(),
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
    private var recognitionErrorStreak = 0
    private var volumeHintShown = false

    private val translation = TranslationEngine()

    private val speech = SpeechEngine(
        context = app,
        onPartial = { text -> _state.update { it.copy(partial = text) } },
        onFinal = { text -> mainHandler.post { handleFinalResult(text) } },
        onError = { message ->
            recognitionErrorStreak++
            if (recognitionErrorStreak >= 3) {
                recognitionErrorStreak = 0
                speech.stop()
                _state.update {
                    it.copy(
                        mode = Mode.IDLE,
                        error = message + " Прослушивание остановлено после трёх сбоев " +
                            "подряд — проверьте связь или сервис Google и нажмите кнопку снова.",
                    )
                }
            } else {
                _state.update { it.copy(error = message) }
                mainHandler.post { resumeAfterInterruption() }
            }
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
        val restoredMessages = loadHistory()
        messageCounter = restoredMessages.size.toLong()
        _state.update {
            it.copy(
                glossary = loadGlossary(),
                customPhrases = loadCustomPhrases(),
                messages = restoredMessages,
                sessionStart = prefs.getString("session_start", null),
                chineseLanguageTag = prefs.getString("chinese_language", "zh-CN") ?: "zh-CN",
                russianRoute = prefs.getString("route_ru", AudioRoutes.SYSTEM)
                    ?: AudioRoutes.SYSTEM,
                chineseRoute = prefs.getString("route_zh", AudioRoutes.SPEAKER)
                    ?: AudioRoutes.SPEAKER,
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
            recognitionErrorStreak = 0
            maybeWarnLowVolume()
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
        recognitionErrorStreak = 0
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

    /** Куда звучит русский перевод (наушники русской стороны). */
    fun setRussianRoute(route: String) {
        prefs.edit().putString("route_ru", route).apply()
        _state.update { it.copy(russianRoute = route) }
    }

    /** Куда звучит китайский перевод (динамик или наушники китайской стороны). */
    fun setChineseRoute(route: String) {
        prefs.edit().putString("route_zh", route).apply()
        _state.update { it.copy(chineseRoute = route) }
    }

    /** Проверка русского канала — тестовая фраза по текущему маршруту. */
    fun testRussianVoice() {
        speech.stop()
        voice.stop()
        voice.speakRussian(
            "Проверка. Русский перевод будет звучать здесь.",
            _state.value.russianRoute,
        )
    }

    /** Проверка китайского канала — тестовая фраза по текущему маршруту. */
    fun testChineseVoice() {
        speech.stop()
        voice.stop()
        voice.speakChineseAloud("声音测试。中文翻译将从这里播放。", _state.value.chineseRoute)
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
            voice.speakRussian(message.translated, _state.value.russianRoute)
        } else {
            voice.speakChineseAloud(message.translated, _state.value.chineseRoute)
        }
    }

    /** Быстрая фраза из разговорника: сразу в ленту и вслух по-китайски. */
    fun speakPhrase(phrase: Phrase) {
        if (_state.value.mode == Mode.REPLY_RUSSIAN) return
        speech.stop()
        voice.stop()
        addMessage(original = phrase.russian, translated = phrase.chinese, fromChinese = false)
        voice.speakChineseAloud(phrase.chinese, _state.value.chineseRoute)
    }

    /** Набранная вручную русская фраза: перевести, показать в ленте и озвучить. */
    fun composeAndSpeak(russian: String) {
        val text = russian.trim()
        if (text.isEmpty()) return
        speech.stop()
        voice.stop()
        viewModelScope.launch {
            try {
                val translated = applyGlossary(translation.translateRussianToChinese(text))
                addMessage(original = text, translated = translated, fromChinese = false)
                voice.speakChineseAloud(translated, _state.value.chineseRoute)
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = "Перевод не удался: ${e.message ?: "проверьте модели"}")
                }
            }
        }
    }

    /** Своя фраза в разговорник: переводим и сохраняем в раздел «Мои фразы». */
    fun addCustomPhrase(russian: String) {
        val text = russian.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            try {
                val translated = applyGlossary(translation.translateRussianToChinese(text))
                val updated = _state.value.customPhrases + Phrase("Мои фразы", text, translated)
                _state.update {
                    it.copy(
                        customPhrases = updated,
                        error = "Сохранено в раздел «Мои фразы».",
                    )
                }
                persistCustomPhrases(updated)
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = "Не удалось перевести фразу: ${e.message ?: "проверьте модели"}")
                }
            }
        }
    }

    fun removeCustomPhrase(phrase: Phrase) {
        val updated = _state.value.customPhrases - phrase
        _state.update { it.copy(customPhrases = updated) }
        persistCustomPhrases(updated)
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
        _state.update { it.copy(messages = emptyList(), sessionStart = null) }
        persistHistory()
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
        appendLine("AGROPLANET — переговоры, $header")
        appendLine("Реплик: ${_state.value.messages.size} · составлено приложением «Толмач»")
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

        recognitionErrorStreak = 0

        when (_state.value.mode) {
            Mode.LISTEN_CHINESE -> viewModelScope.launch {
                try {
                    val translated = applyGlossary(translation.translateChineseToRussian(text))
                    addMessage(original = text, translated = translated, fromChinese = true)
                    voice.speakRussian(translated, _state.value.russianRoute)
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
                    voice.speakChineseAloud(translated, _state.value.chineseRoute)
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
        _state.update {
            it.copy(
                messages = it.messages + message,
                sessionStart = it.sessionStart ?: message.time,
            )
        }
        persistHistory()
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

    private fun persistHistory() {
        val current = _state.value
        val editor = prefs.edit()
        if (current.messages.isEmpty()) {
            editor.remove("history").remove("session_start")
        } else {
            val array = JSONArray()
            current.messages.takeLast(200).forEach { m ->
                array.put(
                    JSONObject()
                        .put("o", m.original)
                        .put("t", m.translated)
                        .put("f", m.fromChinese)
                        .put("time", m.time),
                )
            }
            editor.putString("history", array.toString())
            editor.putString("session_start", current.sessionStart ?: current.messages.first().time)
        }
        editor.apply()
    }

    private fun loadHistory(): List<ChatMessage> = runCatching {
        val raw = prefs.getString("history", null) ?: return emptyList()
        val array = JSONArray(raw)
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            ChatMessage(
                id = index + 1L,
                original = item.getString("o"),
                translated = item.getString("t"),
                fromChinese = item.getBoolean("f"),
                time = item.optString("time", ""),
            )
        }
    }.getOrDefault(emptyList())

    private fun persistCustomPhrases(phrases: List<Phrase>) {
        val array = JSONArray()
        phrases.forEach { p ->
            array.put(JSONObject().put("r", p.russian).put("c", p.chinese))
        }
        prefs.edit().putString("custom_phrases", array.toString()).apply()
    }

    private fun loadCustomPhrases(): List<Phrase> = runCatching {
        val raw = prefs.getString("custom_phrases", null) ?: return emptyList()
        val array = JSONArray(raw)
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            Phrase("Мои фразы", item.getString("r"), item.getString("c"))
        }
    }.getOrDefault(emptyList())

    private fun maybeWarnLowVolume() {
        if (volumeHintShown) return
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (max > 0 && current * 4 < max) {
            volumeHintShown = true
            _state.update {
                it.copy(
                    error = "Громкость медиа почти на нуле — прибавьте её боковыми " +
                        "кнопками, иначе перевод будет не слышно.",
                )
            }
        }
    }

    private fun updateHeadsetState() {
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val bluetooth = outputs.any { device ->
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    device.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
        }
        val wired = outputs.any { device ->
            device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_USB_DEVICE
        }
        _state.update { it.copy(bluetoothConnected = bluetooth, wiredConnected = wired) }
    }

    override fun onCleared() {
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
        speech.stop()
        voice.shutdown()
        translation.close()
        super.onCleared()
    }
}
