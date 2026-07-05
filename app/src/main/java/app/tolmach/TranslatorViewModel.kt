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
import app.tolmach.engine.CallEngine
import app.tolmach.engine.SpeechEngine
import app.tolmach.engine.TranslationEngine
import app.tolmach.engine.VoiceOutput
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
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
    val voiceRussianOk: Boolean = true,
    val voiceChineseOk: Boolean = true,
    val micLevel: Float = 0f,
    val confirmReply: Boolean = false,
    val pendingReply: String? = null,
    val shareAudioPath: String? = null,
    val showGuide: Boolean = false,
    val appLanguage: String = "ru",
    val showLanguagePicker: Boolean = false,
    val callState: String = "idle",
    val callLocalCode: String = "",
    val callSeconds: Int = 0,
    val callMuted: Boolean = false,
    val callSpeakerOn: Boolean = true,
    val russianVoice: String = "",
    val chineseVoice: String = "",
    val ruVoices: List<Pair<String, String>> = emptyList(),
    val zhVoices: List<Pair<String, String>> = emptyList(),
    val russianRoute: String = AudioRoutes.SYSTEM,
    val chineseRoute: String = AudioRoutes.SPEAKER,
    val useDeepL: Boolean = false,
    val deepLKey: String = "",
    val chineseRate: Float = 0.95f,
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
    private var lastClearedMessages: List<ChatMessage> = emptyList()
    private var lastClearedStart: String? = null
    private var callEngine: CallEngine? = null
    private var callTicker: Job? = null

    private val translation = TranslationEngine()

    private val speech: SpeechEngine = SpeechEngine(
        context = app,
        onPartial = { text -> _state.update { it.copy(partial = text) } },
        onFinal = { text -> mainHandler.post { handleFinalResult(text) } },
        onError = { message ->
            recognitionErrorStreak++
            if (recognitionErrorStreak >= 3) {
                recognitionErrorStreak = 0
                hardStopRecognition()
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
        onStateChange = { active ->
            _state.update {
                it.copy(listening = active, micLevel = if (active) it.micLevel else 0f)
            }
        },
        onRms = { level -> _state.update { it.copy(micLevel = level) } },
    )

    private val voice: VoiceOutput = VoiceOutput(
        context = app,
        onSpeakingChanged = { speaking ->
            _state.update { it.copy(speaking = speaking) }
            if (!speaking) resumeAfterSpeech()
        },
        onReady = { russianOk, chineseOk ->
            _state.update {
                it.copy(voiceRussianOk = russianOk, voiceChineseOk = chineseOk)
            }
            if (!russianOk || !chineseOk) {
                val missing = buildList<String> {
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
        onVoicesLoaded = { russian, chinese ->
            _state.update { it.copy(ruVoices = russian, zhVoices = chinese) }
        },
        onVoiceMissing = { chinese ->
            _state.update {
                it.copy(error = strings(it.appLanguage).voiceMissing(chinese))
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
                useDeepL = prefs.getBoolean("use_deepl", false),
                deepLKey = prefs.getString("deepl_key", "") ?: "",
                chineseRate = prefs.getFloat("zh_rate", 0.95f),
                confirmReply = prefs.getBoolean("confirm_reply", false),
            showGuide = !prefs.getBoolean("guide_shown", false),
            appLanguage = prefs.getString("app_language", "ru") ?: "ru",
            showLanguagePicker = prefs.getString("app_language", null) == null,
                russianVoice = prefs.getString("voice_ru", "") ?: "",
                chineseVoice = prefs.getString("voice_zh", "") ?: "",
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

    /** Движок перевода: офлайн ML Kit или DeepL со своим ключом. */
    fun setUseDeepL(enabled: Boolean) {
        prefs.edit().putBoolean("use_deepl", enabled).apply()
        _state.update { it.copy(useDeepL = enabled) }
    }

    fun setDeepLKey(key: String) {
        prefs.edit().putString("deepl_key", key).apply()
        _state.update { it.copy(deepLKey = key) }
    }

    /** Проверять распознанную русскую фразу перед переводом и озвучкой. */
    fun setConfirmReply(enabled: Boolean) {
        prefs.edit().putBoolean("confirm_reply", enabled).apply()
        _state.update { it.copy(confirmReply = enabled) }
    }

    fun setRussianVoice(name: String) {
        prefs.edit().putString("voice_ru", name).apply()
        _state.update { it.copy(russianVoice = name) }
    }

    fun setChineseVoice(name: String) {
        prefs.edit().putString("voice_zh", name).apply()
        _state.update { it.copy(chineseVoice = name) }
    }

    /**
     * Голосовое для мессенджера: синтезирует китайский перевод вашей реплики
     * в аудиофайл. UI подхватит путь и откроет системный шаринг.
     */
    fun shareMessageAudio(message: ChatMessage) {
        if (message.fromChinese) return
        val dir = File(getApplication<Application>().cacheDir, "shared").apply { mkdirs() }
        val out = File(dir, "tolmach_voice_${message.id}.wav")
        voice.synthesizeChineseFile(
            text = message.translated,
            rate = _state.value.chineseRate,
            voiceName = _state.value.chineseVoice,
            outFile = out,
        ) { success ->
            mainHandler.post {
                _state.update {
                    it.copy(
                        shareAudioPath = if (success) out.absolutePath else null,
                        error = if (success) {
                            it.error
                        } else {
                            strings(it.appLanguage).shareAudioFailed
                        },
                    )
                }
            }
        }
    }

    /** Краткий гид: показывается при первом запуске, повторно — из Настроек. */
    fun dismissGuide() {
        prefs.edit().putBoolean("guide_shown", true).apply()
        _state.update { it.copy(showGuide = false) }
    }

    /** Язык интерфейса: выбирается при первом входе и в Настройках. */
    fun setAppLanguage(language: String) {
        prefs.edit().putString("app_language", language).apply()
        _state.update { it.copy(appLanguage = language, showLanguagePicker = false) }
    }

    private fun callErrorText(key: String): String {
        val s = strings(_state.value.appLanguage)
        return when {
            key == "invite" -> s.callErrInvite
            key == "answer" -> s.callErrAnswer
            key == "ice" -> s.callErrIce
            key.startsWith("prepare:") -> s.callErrPrepare(key.removePrefix("prepare:"))
            key.startsWith("apply:") -> s.callErrApply(key.removePrefix("apply:"))
            else -> key
        }
    }

    fun showGuideAgain() {
        _state.update { it.copy(showGuide = true) }
    }

    fun clearShareAudio() {
        _state.update { it.copy(shareAudioPath = null) }
    }

    /** Мгновенно оборвать текущую озвучку; прослушивание возобновится само. */
    fun stopSpeaking() {
        voice.stop()
    }

    /** Скорость китайской озвучки: медленнее — разборчивее для собеседника. */
    fun setChineseRate(rate: Float) {
        prefs.edit().putFloat("zh_rate", rate).apply()
        _state.update { it.copy(chineseRate = rate) }
    }

    /** Проверка русского канала — тестовая фраза по текущему маршруту. */
    fun testRussianVoice() {
        speech.stop()
        voice.stop()
        voice.speakRussian(
            "Проверка. Русский перевод будет звучать здесь.",
            _state.value.russianRoute,
            _state.value.russianVoice,
        )
    }

    /** Проверка китайского канала — тестовая фраза по текущему маршруту. */
    fun testChineseVoice() {
        speech.stop()
        voice.stop()
        voice.speakChineseAloud("声音测试。中文翻译将从这里播放。", _state.value.chineseRoute, _state.value.chineseRate, _state.value.chineseVoice)
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
            voice.speakRussian(
                message.translated,
                _state.value.russianRoute,
                _state.value.russianVoice,
            )
        } else {
            voice.speakChineseAloud(message.translated, effectiveChineseRoute(), _state.value.chineseRate, _state.value.chineseVoice)
        }
    }

    /** Быстрая фраза из разговорника: сразу в ленту и вслух по-китайски. */
    fun speakPhrase(phrase: Phrase) {
        if (_state.value.mode == Mode.REPLY_RUSSIAN) return
        speech.stop()
        voice.stop()
        addMessage(original = phrase.russian, translated = phrase.chinese, fromChinese = false)
        voice.speakChineseAloud(phrase.chinese, effectiveChineseRoute(), _state.value.chineseRate, _state.value.chineseVoice)
    }

    /** Набранная вручную русская фраза: перевести, показать в ленте и озвучить. */
    fun composeAndSpeak(russian: String) {
        val text = russian.trim()
        if (text.isEmpty()) return
        speech.stop()
        voice.stop()
        viewModelScope.launch {
            try {
                val translated = applyGlossary(translation.translate(
                        text,
                        toChinese = true,
                        useDeepL = _state.value.useDeepL,
                        deepLKey = _state.value.deepLKey,
                    ))
                addMessage(original = text, translated = translated, fromChinese = false)
                voice.speakChineseAloud(translated, effectiveChineseRoute(), _state.value.chineseRate, _state.value.chineseVoice)
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = translationFailureMessage(e))
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
                val translated = applyGlossary(translation.translate(
                        text,
                        toChinese = true,
                        useDeepL = _state.value.useDeepL,
                        deepLKey = _state.value.deepLKey,
                    ))
                val updated = _state.value.customPhrases + Phrase(
                    russian = text,
                    chinese = translated,
                    category = "Мои фразы",
                )
                _state.update {
                    it.copy(
                        customPhrases = updated,
                        error = strings(it.appLanguage).savedToMyPhrases,
                    )
                }
                persistCustomPhrases(updated)
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = translationFailureMessage(e))
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
        lastClearedMessages = _state.value.messages
        lastClearedStart = _state.value.sessionStart
        _state.update { it.copy(messages = emptyList(), sessionStart = null) }
        persistHistory()
    }

    /** «Вернуть» из снекбара — восстанавливает случайно очищенную стенограмму. */
    fun undoClear() {
        if (lastClearedMessages.isEmpty()) return
        _state.update {
            it.copy(messages = lastClearedMessages, sessionStart = lastClearedStart)
        }
        lastClearedMessages = emptyList()
        lastClearedStart = null
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

    /**
     * Стенограмма файлом .txt — для вложения в письмо и архива сделки.
     * Возвращает путь в кэше; UI отправит через FileProvider.
     */
    fun exportTranscriptFile(): String? = runCatching {
        val dir = File(getApplication<Application>().cacheDir, "shared").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
        val file = File(dir, "Agroplanet_peregovory_$stamp.txt")
        file.writeText(transcriptText())
        file.absolutePath
    }.getOrNull()

    // ---------- Внутренняя логика ----------

    /** Человеческое объяснение вместо общего «не удался». */
    private fun translationFailureMessage(e: Exception): String {
        val s = strings(_state.value.appLanguage)
        val deepLActive = _state.value.useDeepL && _state.value.deepLKey.isNotBlank()
        return if (!_state.value.modelsReady && !deepLActive) {
            s.modelsNotReady
        } else {
            s.translationFailed(e.message)
        }
    }

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
                    val translated = applyGlossary(
                        translation.translate(
                            text,
                            toChinese = false,
                            useDeepL = _state.value.useDeepL,
                            deepLKey = _state.value.deepLKey,
                        ),
                    )
                    addMessage(original = text, translated = translated, fromChinese = true)
                    voice.speakRussian(
                        translated,
                        _state.value.russianRoute,
                        _state.value.russianVoice,
                    )
                } catch (e: Exception) {
                    _state.update {
                        it.copy(error = translationFailureMessage(e))
                    }
                    restartRecognitionSoon(_state.value.chineseLanguageTag)
                }
            }

            Mode.REPLY_RUSSIAN -> {
                if (_state.value.confirmReply) {
                    _state.update { it.copy(pendingReply = text) }
                } else {
                    translateAndSpeakReply(text)
                }
            }

            Mode.IDLE -> Unit
        }
    }

    private fun translateAndSpeakReply(text: String) {
        viewModelScope.launch {
            try {
                val translated = applyGlossary(
                    translation.translate(
                        text,
                        toChinese = true,
                        useDeepL = _state.value.useDeepL,
                        deepLKey = _state.value.deepLKey,
                    ),
                )
                addMessage(original = text, translated = translated, fromChinese = false)
                voice.speakChineseAloud(translated, _state.value.chineseRoute, _state.value.chineseRate, _state.value.chineseVoice)
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = translationFailureMessage(e))
                }
                finishReply()
            }
        }
    }

    /** Пользователь подтвердил (и, возможно, поправил) распознанную фразу. */
    fun confirmReply(edited: String) {
        _state.update { it.copy(pendingReply = null) }
        val text = edited.trim()
        if (text.isEmpty()) finishReply() else translateAndSpeakReply(text)
    }

    /** Пользователь отменил ответ — возвращаемся к прослушиванию. */
    fun cancelReply() {
        _state.update { it.copy(pendingReply = null) }
        finishReply()
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
    /** Вынесено в метод: прямая ссылка на speech из его же инициализатора запрещена. */
    private fun hardStopRecognition() {
        speech.stop()
    }

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

    // ---------- Защищённый P2P-звонок ----------

    fun openCallMenu() {
        speech.stop()
        voice.stop()
        _state.update { it.copy(mode = Mode.IDLE, callState = "menu", callLocalCode = "") }
    }

    fun startCallAsCaller() {
        ensureCallEngine().startAsCaller()
    }

    fun beginJoin() {
        _state.update { it.copy(callState = "join_input") }
    }

    fun submitJoinCode(code: String) {
        ensureCallEngine().startAsJoiner(code)
    }

    fun submitAnswerCode(code: String) {
        callEngine?.acceptAnswer(code)
    }

    /** Динамик или «у уха» — как в обычном звонке. */
    fun toggleCallSpeaker() {
        val speakerOn = !_state.value.callSpeakerOn
        runCatching {
            audioManager.isSpeakerphoneOn = speakerOn
        }
        _state.update { it.copy(callSpeakerOn = speakerOn) }
    }

    fun toggleCallMute() {
        val muted = !_state.value.callMuted
        callEngine?.setMuted(muted)
        _state.update { it.copy(callMuted = muted) }
    }

    fun endCall() {
        callTicker?.cancel()
        callTicker = null
        callEngine?.close()
        callEngine = null
        restoreAudioAfterCall()
        _state.update {
            it.copy(
                callState = "idle",
                callLocalCode = "",
                callSeconds = 0,
                callMuted = false,
                callSpeakerOn = true,
            )
        }
    }

    fun closeCallUi() = endCall()

    private fun ensureCallEngine(): CallEngine {
        val existing = callEngine
        if (existing != null) return existing
        val created = CallEngine(
            context = getApplication(),
            onState = { newState -> handleCallState(newState) },
            onLocalCode = { code ->
                val phase = if (_state.value.callState == "join_input" ||
                    _state.value.callState == "preparing_answer"
                ) {
                    "answer_ready"
                } else {
                    "wait_answer"
                }
                _state.update { it.copy(callLocalCode = code, callState = phase) }
            },
            onError = { key -> _state.update { it.copy(error = callErrorText(key)) } },
        )
        callEngine = created
        return created
    }

    private fun handleCallState(newState: String) {
        when (newState) {
            "preparing" -> {
                val phase = if (_state.value.callState == "join_input") {
                    "preparing_answer"
                } else {
                    "preparing"
                }
                _state.update { it.copy(callState = phase) }
            }

            "connected" -> {
                if (_state.value.callState != "connected") {
                    enterCallAudioMode()
                    startCallTicker()
                }
                _state.update { it.copy(callState = "connected") }
            }

            else -> _state.update { it.copy(callState = newState) }
        }
    }

    private fun startCallTicker() {
        callTicker?.cancel()
        callTicker = viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.update { it.copy(callSeconds = it.callSeconds + 1) }
            }
        }
    }

    private fun enterCallAudioMode() {
        runCatching {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = true
        }
    }

    private fun restoreAudioAfterCall() {
        runCatching {
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = AudioManager.MODE_NORMAL
        }
    }

    /** В активном звонке китайская озвучка всегда идёт в динамик — в канал. */
    private fun effectiveChineseRoute(): String =
        if (_state.value.callState == "connected") {
            AudioRoutes.SPEAKER
        } else {
            _state.value.chineseRoute
        }

    /** Текущая громкость медиа в процентах — для проверки перед встречей. */
    fun mediaVolumePercent(): Int {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (max <= 0) return 100
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / max
    }

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
        endCall()
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
        speech.stop()
        voice.shutdown()
        translation.close()
        super.onCleared()
    }
}
