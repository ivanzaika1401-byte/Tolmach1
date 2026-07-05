package app.tolmach

/**
 * Двуязычный слой интерфейса. Все видимые пользователю строки — здесь.
 * Любая пропущенная позиция ловится типовой компиляцией пирамиды.
 */
interface AppStrings {
    // Навигация
    val tabTranslator: String
    val tabPhrases: String
    val tabCall: String
    val tabSettings: String

    // Шапка и меню
    val brandTagline: String
    val menuCompose: String
    val menuShareTranscript: String
    val menuShareFile: String
    val menuClear: String
    val composeTooltip: String
    val menuTooltip: String

    // Приборная строка
    val translationDeepL: String
    val translationReady: String
    val translationError: String
    val translationLoading: String
    val headphonesTwo: String
    val headphonesBt: String
    val headphonesWired: String
    val headphonesNone: String
    val dialectMandarin: String
    val dialectTaiwan: String
    val dialectCantonese: String
    val retryModels: String

    // Маршруты и сеанс
    val routeRussianTo: String
    val routeChineseTo: String
    val routeBluetooth: String
    val routeWired: String
    val routeSpeaker: String
    val routeAuto: String
    fun sessionLine(start: String, count: Int): String

    // Пустой экран
    val emptyTitle: String
    val emptyBody: String

    // Лента
    val youLabel: String
    val partnerLabel: String
    val replayTooltip: String
    val shareTooltip: String
    val shareAsText: String
    val shareAsVoice: String
    val copiedToast: String
    val chooserMessage: String
    val chooserTranscript: String
    val chooserTranscriptFile: String
    val chooserVoice: String

    // Живая строка и пульт
    val speakingNow: String
    val listeningPartner: String
    val speakRussianNow: String
    val stopSpeakTooltip: String
    val deckPhrases: String
    val deckListen: String
    val deckStop: String
    val deckReply: String

    // Снекбар очистки
    val clearedSnack: String
    val undoAction: String

    // Экран «Фразы»
    val phrasesTitle: String
    val phrasesAdd: String
    val phrasesHint: String
    val myPhrases: String
    val deleteTooltip: String
    fun categoryLabel(category: String): String

    // Экран «Звонок»
    val callTitleIdle: String
    val callTitlePreparing: String
    val callTitleFailed: String
    val callEncryption: String
    val callCreate: String
    val callCreateHint: String
    val callJoin: String
    val callJoinHint: String
    val callIntroBody: String
    val callPreparingCode: String
    val callWaitHint: String
    val callAnswerLabel: String
    val callConnectAction: String
    val callAnswerReadyHint: String
    val callConnecting: String
    val callConnectedHint: String
    val callFailedBody: String
    val callInviteLabel: String
    val callContinue: String
    val callSendCode: String
    val callCopy: String
    val callPaste: String
    val callCancel: String
    val callMicOn: String
    val callMicOff: String
    val callSpeaker: String
    val callEarpiece: String
    val callPhrases: String
    val callCompose: String
    val callEndTooltip: String
    fun callShareText(code: String): String
    val callChooserTitle: String

    // Настройки
    val settingsTitle: String
    val sectionLanguage: String
    val languageRu: String
    val languageZh: String
    val sectionTools: String
    val toolPreflight: String
    val toolPreflightHint: String
    val toolGlossary: String
    val toolGlossaryHint: String
    val toolGuide: String
    val toolGuideHint: String
    val toolAbout: String
    val toolAboutHint: String
    val sectionPresets: String
    val presetSpeakerTitle: String
    val presetSpeakerHint: String
    val presetTwoPairsTitle: String
    val presetTwoPairsHint: String
    val presetRemoteTitle: String
    val presetRemoteHint: String
    val presetPartnerBtTitle: String
    val presetPartnerBtHint: String
    val sectionDialect: String
    val dialectMandarinFull: String
    val dialectTaiwanFull: String
    val dialectCantoneseFull: String
    val sectionRuRoute: String
    val sectionZhRoute: String
    val ruRouteAuto: String
    val ruRouteBt: String
    val ruRouteWired: String
    val ruRouteSpeaker: String
    val zhRouteSpeaker: String
    val zhRouteWired: String
    val zhRouteBt: String
    val testRuChannel: String
    val testZhChannel: String
    val sectionEngine: String
    val engineOffline: String
    val engineDeepL: String
    val deepLKeyLabel: String
    val deepLHint: String
    val sectionRate: String
    fun rateLine(rate: String): String
    val sectionConfirm: String
    val confirmTitle: String
    val confirmHint: String
    val sectionRuVoice: String
    val sectionZhVoice: String
    val voicesLoading: String
    val voiceSystemDefault: String
    val settingsFootnote: String

    // Диалоги
    val guideTitle: String
    val guideListenTitle: String
    val guideListenText: String
    val guideReplyTitle: String
    val guideReplyText: String
    val guideMoreTitle: String
    val guideMoreText: String
    val guideFootnote: String
    val gotIt: String

    val pickerTitle: String
    val pickerRuSub: String
    val pickerZhSub: String

    val glossaryTitle: String
    val glossaryHint: String
    val glossaryFrom: String
    val glossaryTo: String
    val addAction: String
    val doneAction: String

    val composeTitle: String
    val composeHint: String
    val composeLabel: String
    val composeSpeak: String
    val composeSave: String
    val cancelAction: String

    fun aboutTitle(version: String): String
    val aboutLine1: String
    val aboutLine2: String

    val preflightTitle: String
    val checkMic: String
    val checkMicHintNo: String
    val checkTranslation: String
    val checkTranslationDeepL: String
    val checkTranslationOffline: String
    val checkTranslationNo: String
    val checkRuVoice: String
    val checkZhVoice: String
    val checkVoiceHintNo: String
    val openTtsSettings: String
    val checkHeadphones: String
    val headphonesDetailTwo: String
    val headphonesDetailBt: String
    val headphonesDetailWired: String
    val headphonesDetailNone: String
    val checkVolume: String
    fun volumeDetail(percent: Int): String
    val testRuShort: String
    val testZhShort: String

    val replyConfirmTitle: String
    val replyConfirmHint: String
    val replyConfirmSpeak: String

    // Сообщения от логики
    fun voiceMissing(chinese: Boolean): String
    val modelsNotReady: String
    fun translationFailed(reason: String?): String
    val savedToMyPhrases: String
    val shareAudioFailed: String
    val callErrInvite: String
    val callErrAnswer: String
    fun callErrPrepare(detail: String): String
    fun callErrApply(detail: String): String
    val callErrIce: String
}

object RuStrings : AppStrings {
    override val tabTranslator = "Перевод"
    override val tabPhrases = "Фразы"
    override val tabCall = "Звонок"
    override val tabSettings = "Настройки"

    override val brandTagline = "переговоры с Китаем"
    override val menuCompose = "Написать по-русски"
    override val menuShareTranscript = "Отправить стенограмму"
    override val menuShareFile = "Стенограмма файлом (.txt)"
    override val menuClear = "Очистить беседу"
    override val composeTooltip = "Написать по-русски"
    override val menuTooltip = "Меню"

    override val translationDeepL = "Перевод: DeepL"
    override val translationReady = "Перевод: готов"
    override val translationError = "Перевод: ошибка"
    override val translationLoading = "Загрузка…"
    override val headphonesTwo = "Наушники: 2 пары"
    override val headphonesBt = "Наушники: BT"
    override val headphonesWired = "Наушники: провод"
    override val headphonesNone = "Наушники: нет"
    override val dialectMandarin = "Путунхуа"
    override val dialectTaiwan = "Тайвань"
    override val dialectCantonese = "Кантонский"
    override val retryModels = "Повторить загрузку моделей"

    override val routeRussianTo = "Русский → "
    override val routeChineseTo = "Китайский → "
    override val routeBluetooth = "Bluetooth"
    override val routeWired = "провод/USB"
    override val routeSpeaker = "динамик"
    override val routeAuto = "авто"
    override fun sessionLine(start: String, count: Int) =
        "Сеанс с " + start + " · реплик: " + count

    override val emptyTitle = "Телефон — на стол, микрофоном к собеседнику"
    override val emptyBody = "Зелёная кнопка — перевод партнёра тихо придёт вам " +
        "в наушники. «Ответить» — ваша русская фраза прозвучит по-китайски. " +
        "Вкладка «Фразы» — готовые формулировки Agroplanet в одно касание."

    override val youLabel = "Вы"
    override val partnerLabel = "Партнёр"
    override val replayTooltip = "Озвучить ещё раз"
    override val shareTooltip = "Отправить"
    override val shareAsText = "Текстом"
    override val shareAsVoice = "Голосом · аудио по-китайски"
    override val copiedToast = "Скопировано"
    override val chooserMessage = "Отправить сообщение"
    override val chooserTranscript = "Отправить стенограмму"
    override val chooserTranscriptFile = "Стенограмма файлом"
    override val chooserVoice = "Отправить голосом"

    override val speakingNow = "Озвучиваю перевод…"
    override val listeningPartner = "Слушаю собеседника…"
    override val speakRussianNow = "Говорите по-русски…"
    override val stopSpeakTooltip = "Остановить озвучку"
    override val deckPhrases = "Фразы"
    override val deckListen = "Слушать китайский"
    override val deckStop = "Остановить"
    override val deckReply = "Ответить"

    override val clearedSnack = "Стенограмма очищена"
    override val undoAction = "Вернуть"

    override val phrasesTitle = "Быстрые фразы"
    override val phrasesAdd = "Добавить"
    override val phrasesHint = "Нажмите фразу — телефон произнесёт её по-китайски " +
        "и добавит в ленту. В активном звонке — прямо в канал собеседнику."
    override val myPhrases = "Мои фразы"
    override val deleteTooltip = "Удалить"
    override fun categoryLabel(category: String) = category

    override val callTitleIdle = "Защищённый звонок"
    override val callTitlePreparing = "Подготовка звонка"
    override val callTitleFailed = "Не удалось соединиться"
    override val callEncryption = "Сквозное шифрование DTLS-SRTP"
    override val callCreate = "Создать звонок"
    override val callCreateHint = "Получите код и отправьте его собеседнику любым мессенджером."
    override val callJoin = "Присоединиться"
    override val callJoinHint = "Вставьте код, который прислал звонящий."
    override val callIntroBody = "Работает через любой интернет — мобильный 4G/5G " +
        "или Wi-Fi, общая сеть не нужна: стороны могут быть в разных странах. " +
        "Голос всегда зашифрован; сложные NAT дожимает встроенный " +
        "TURN-ретранслятор (передаёт только зашифрованные пакеты). В звонке " +
        "работают «Фразы» и «Написать» — китайская озвучка уходит собеседнику. " +
        "Честно: материковый Китай глушит P2P на любом подключении — партнёру " +
        "нужен VPN; синхронный перевод его речи на одном телефоне невозможен " +
        "(микрофон занят звонком) — для этого схема «второе устройство»."
    override val callPreparingCode = "Готовлю код соединения…"
    override val callWaitHint = "Шаг 1: отправьте свой код собеседнику. " +
        "Шаг 2: вставьте его код-ответ и нажмите «Соединить»."
    override val callAnswerLabel = "Вставьте код-ответ собеседника"
    override val callConnectAction = "Соединить"
    override val callAnswerReadyHint = "Отправьте этот код-ответ звонящему — " +
        "соединение установится автоматически."
    override val callConnecting = "Устанавливаю прямое соединение…"
    override val callConnectedHint = "Говорите — связь прямая и зашифрованная. " +
        "«Фразы» и «Написать» озвучат китайский прямо в звонок."
    override val callFailedBody = "Соединение не удалось. Переключите одну из " +
        "сторон между Wi-Fi и мобильным интернетом и попробуйте снова; фаервол " +
        "КНР блокирует P2P на любом подключении — там нужен VPN или схема " +
        "«второе устройство»."
    override val callInviteLabel = "Код приглашения"
    override val callContinue = "Продолжить"
    override val callSendCode = "Отправить код"
    override val callCopy = "Копировать"
    override val callPaste = "Вставить из буфера"
    override val callCancel = "Отменить"
    override val callMicOn = "Микрофон"
    override val callMicOff = "Вкл. микр."
    override val callSpeaker = "Динамик"
    override val callEarpiece = "У уха"
    override val callPhrases = "Фразы"
    override val callCompose = "Написать"
    override val callEndTooltip = "Завершить"
    override fun callShareText(code: String) =
        "Код для защищённого звонка «Толмач»:\n" + code
    override val callChooserTitle = "Отправить код"

    override val settingsTitle = "Настройки"
    override val sectionLanguage = "Язык приложения · App language"
    override val languageRu = "Русский"
    override val languageZh = "中文（简体）"
    override val sectionTools = "Инструменты"
    override val toolPreflight = "Проверка перед встречей"
    override val toolPreflightHint = "Чек-лист: микрофон, перевод, голоса, наушники, громкость."
    override val toolGlossary = "Словарь терминов"
    override val toolGlossaryHint = "Свои замены в переводе: сорта, ГОСТы, базисы поставки."
    override val toolGuide = "Краткий гид"
    override val toolGuideHint = "Три касания к сути: слушать, ответить, фразы и звонок."
    override val toolAbout = "О приложении"
    override val toolAboutHint = "Версия и памятка по каналам звука."
    override val sectionPresets = "Быстрые схемы звука"
    override val presetSpeakerTitle = "Динамик для собеседника"
    override val presetSpeakerHint = "Русский — в ваши наушники, китайский — из динамика телефона."
    override val presetTwoPairsTitle = "Две пары наушников"
    override val presetTwoPairsHint = "Русский — в Bluetooth, китайский — в проводные/USB."
    override val presetRemoteTitle = "Дистанционный звонок"
    override val presetRemoteHint = "Созвон идёт на втором устройстве на громкой связи рядом: " +
        "русский — вам, китайский — из динамика в его микрофон."
    override val presetPartnerBtTitle = "Наушник у собеседника"
    override val presetPartnerBtHint = "Китайский — в Bluetooth собеседнику, русский — из динамика вам."
    override val sectionDialect = "Язык собеседника"
    override val dialectMandarinFull = "Путунхуа — стандартный китайский (рекомендуется)"
    override val dialectTaiwanFull = "Тайваньский мандарин"
    override val dialectCantoneseFull = "Кантонский — Гонконг, Гуандун (экспериментально)"
    override val sectionRuRoute = "Русский перевод — куда звучит"
    override val sectionZhRoute = "Китайский перевод — куда звучит"
    override val ruRouteAuto = "Авто — как решит телефон"
    override val ruRouteBt = "Bluetooth-наушники"
    override val ruRouteWired = "Проводные / USB-наушники"
    override val ruRouteSpeaker = "Динамик телефона"
    override val zhRouteSpeaker = "Динамик телефона (рекомендуется)"
    override val zhRouteWired = "Проводные / USB — вторая пара"
    override val zhRouteBt = "Bluetooth-наушники"
    override val testRuChannel = "Проверить русский канал"
    override val testZhChannel = "Проверить китайский канал"
    override val sectionEngine = "Движок перевода"
    override val engineOffline = "Офлайн — ML Kit, работает без интернета"
    override val engineDeepL = "DeepL — максимум качества (интернет + ключ)"
    override val deepLKeyLabel = "Ключ DeepL API"
    override val deepLHint = "Бесплатный ключ: deepl.com → тариф «DeepL API Free», " +
        "500 000 знаков в месяц. Без интернета приложение мгновенно " +
        "переключится на офлайн-перевод."
    override val sectionRate = "Скорость китайской озвучки"
    override fun rateLine(rate: String) =
        rate + "× — медленнее звучит разборчивее для собеседника"
    override val sectionConfirm = "Ответ по-русски"
    override val confirmTitle = "Проверять фразу перед озвучкой"
    override val confirmHint = "покажет распознанный текст — можно поправить " +
        "до того, как собеседник услышит перевод"
    override val sectionRuVoice = "Голос русской озвучки"
    override val sectionZhVoice = "Голос китайской озвучки"
    override val voicesLoading = "Голоса появятся после инициализации синтеза"
    override val voiceSystemDefault = "Системный по умолчанию"
    override val settingsFootnote = "Две пары: русская сторона — Bluetooth, китайская — " +
        "проводные или USB-C наушники (лучше без микрофона). Два Bluetooth " +
        "с разным звуком Android не поддерживает. Если выбранных наушников " +
        "нет, звук мягко уйдёт на динамик (китайский) или системный маршрут (русский)."

    override val guideTitle = "Как пользоваться"
    override val guideListenTitle = "Слушать китайский"
    override val guideListenText = "Телефон на стол микрофоном к собеседнику, " +
        "зелёная кнопка — перевод придёт вам тихо в наушники."
    override val guideReplyTitle = "Ответить"
    override val guideReplyText = "Скажите фразу по-русски — телефон произнесёт " +
        "её по-китайски из динамика."
    override val guideMoreTitle = "Фразы и Звонок"
    override val guideMoreText = "Готовые деловые фразы — во вкладке «Фразы»; " +
        "защищённый звонок с партнёром — во вкладке «Звонок»."
    override val guideFootnote = "Гид всегда доступен: Настройки → Инструменты."
    override val gotIt = "Понятно"

    override val pickerTitle = "Язык интерфейса · 界面语言"
    override val pickerRuSub = "Интерфейс на русском"
    override val pickerZhSub = "中文界面"

    override val glossaryTitle = "Словарь терминов"
    override val glossaryHint = "Замены применяются к готовому переводу — сорта, " +
        "ГОСТы, базисы поставки. Пример: «инкотермс» → «Incoterms»."
    override val glossaryFrom = "Как переводит сейчас"
    override val glossaryTo = "Как должно быть"
    override val addAction = "Добавить"
    override val doneAction = "Готово"

    override val composeTitle = "Написать по-русски"
    override val composeHint = "Телефон переведёт фразу и произнесёт её " +
        "по-китайски. «В разговорник» — сохранит фразу с переводом в «Мои фразы»."
    override val composeLabel = "Ваша фраза"
    override val composeSpeak = "Перевести и озвучить"
    override val composeSave = "В разговорник"
    override val cancelAction = "Отмена"

    override fun aboutTitle(version: String) = "Толмач " + version
    override val aboutLine1 = "Переводчик переговоров для ТОО «AGROPLANET», Костанай."
    override val aboutLine2 = "Перевод и лента работают офлайн. Русский канал — " +
        "наушники вашей стороны, китайский — динамик или вторая пара. " +
        "Юридически значимые условия контракта перепроверяйте с живым переводчиком."

    override val preflightTitle = "Проверка перед встречей"
    override val checkMic = "Микрофон разрешён"
    override val checkMicHintNo = "Android спросит разрешение при первом нажатии записи"
    override val checkTranslation = "Перевод готов"
    override val checkTranslationDeepL = "движок DeepL, офлайн — как страховка"
    override val checkTranslationOffline = "офлайн-модели на месте"
    override val checkTranslationNo = "модели ещё не скачаны — нужен интернет"
    override val checkRuVoice = "Русский голос установлен"
    override val checkZhVoice = "Китайский голос установлен"
    override val checkVoiceHintNo = "докачайте в настройках синтеза речи"
    override val openTtsSettings = "Открыть настройки синтеза"
    override val checkHeadphones = "Наушники"
    override val headphonesDetailTwo = "две пары: Bluetooth и провод/USB"
    override val headphonesDetailBt = "Bluetooth подключены"
    override val headphonesDetailWired = "проводные/USB подключены"
    override val headphonesDetailNone = "нет — русский перевод пойдёт в динамик"
    override val checkVolume = "Громкость медиа"
    override fun volumeDetail(percent: Int) =
        percent.toString() + "%" + if (percent < 25) " — прибавьте боковыми кнопками" else ""
    override val testRuShort = "Тест русского"
    override val testZhShort = "Тест китайского"

    override val replyConfirmTitle = "Проверьте фразу"
    override val replyConfirmHint = "Так вас услышал телефон. Поправьте при " +
        "необходимости — и собеседник получит точный перевод."
    override val replyConfirmSpeak = "Озвучить по-китайски"

    override fun voiceMissing(chinese: Boolean): String {
        val lang = if (chinese) "китайского" else "русского"
        return "Нет голоса для " + lang + " языка. Настройки Android → " +
            "Специальные возможности → Синтез речи → докачайте язык " +
            "(движок Google) и повторите."
    }
    override val modelsNotReady = "Модели перевода ещё не скачаны: вверху должен " +
        "гореть статус «Перевод: готов». Включите интернет и нажмите " +
        "«Повторить загрузку моделей»."
    override fun translationFailed(reason: String?) =
        "Перевод не удался: " + (reason ?: "попробуйте ещё раз")
    override val savedToMyPhrases = "Сохранено в раздел «Мои фразы»."
    override val shareAudioFailed = "Не удалось подготовить аудио — проверьте китайский голос."
    override val callErrInvite = "Код приглашения не распознан — скопируйте его целиком."
    override val callErrAnswer = "Код ответа не распознан — скопируйте его целиком."
    override fun callErrPrepare(detail: String) = "Не удалось подготовить звонок: " + detail
    override fun callErrApply(detail: String) = "Не удалось применить код: " + detail
    override val callErrIce = "Соединение не пробилось через сеть — попробуйте " +
        "другой Wi-Fi или мобильный интернет."
}

object ZhStrings : AppStrings {
    override val tabTranslator = "翻译"
    override val tabPhrases = "短语"
    override val tabCall = "通话"
    override val tabSettings = "设置"

    override val brandTagline = "中哈商务谈判"
    override val menuCompose = "输入俄语句子"
    override val menuShareTranscript = "发送谈判记录"
    override val menuShareFile = "记录导出为文件 (.txt)"
    override val menuClear = "清空对话"
    override val composeTooltip = "输入俄语句子"
    override val menuTooltip = "菜单"

    override val translationDeepL = "翻译：DeepL"
    override val translationReady = "翻译：就绪"
    override val translationError = "翻译：出错"
    override val translationLoading = "加载中…"
    override val headphonesTwo = "耳机：两副"
    override val headphonesBt = "耳机：蓝牙"
    override val headphonesWired = "耳机：有线"
    override val headphonesNone = "耳机：未连接"
    override val dialectMandarin = "普通话"
    override val dialectTaiwan = "台湾"
    override val dialectCantonese = "粤语"
    override val retryModels = "重新下载翻译模型"

    override val routeRussianTo = "俄语 → "
    override val routeChineseTo = "中文 → "
    override val routeBluetooth = "蓝牙"
    override val routeWired = "有线/USB"
    override val routeSpeaker = "扬声器"
    override val routeAuto = "自动"
    override fun sessionLine(start: String, count: Int) =
        "会话开始 " + start + " · 共 " + count + " 条"

    override val emptyTitle = "把手机放在桌上，麦克风朝向对方"
    override val emptyBody = "按绿色按钮，对方的中文会译成俄语送入耳机；" +
        "按«回复»说俄语，手机用中文朗读给对方。" +
        "«短语»页有现成的商务用语，一键播放。"

    override val youLabel = "我方"
    override val partnerLabel = "对方"
    override val replayTooltip = "再播放一次"
    override val shareTooltip = "发送"
    override val shareAsText = "以文字发送"
    override val shareAsVoice = "以语音发送（中文音频）"
    override val copiedToast = "已复制"
    override val chooserMessage = "发送消息"
    override val chooserTranscript = "发送谈判记录"
    override val chooserTranscriptFile = "发送记录文件"
    override val chooserVoice = "发送语音"

    override val speakingNow = "正在朗读译文…"
    override val listeningPartner = "正在聆听对方…"
    override val speakRussianNow = "请说俄语…"
    override val stopSpeakTooltip = "停止朗读"
    override val deckPhrases = "短语"
    override val deckListen = "聆听中文"
    override val deckStop = "停止"
    override val deckReply = "回复"

    override val clearedSnack = "记录已清空"
    override val undoAction = "撤销"

    override val phrasesTitle = "常用短语"
    override val phrasesAdd = "添加"
    override val phrasesHint = "点击短语即用中文朗读并写入记录；" +
        "通话中会直接送入通话声道。"
    override val myPhrases = "我的短语"
    override val deleteTooltip = "删除"
    override fun categoryLabel(category: String): String = when (category) {
        "Знакомство" -> "开场与介绍"
        "Продукция и качество" -> "产品与质量"
        "Цена и условия" -> "价格与条款"
        "Логистика" -> "物流运输"
        "Документы" -> "单证文件"
        "Ход переговоров" -> "谈判进程"
        "Мои фразы" -> "我的短语"
        else -> category
    }

    override val callTitleIdle = "加密通话"
    override val callTitlePreparing = "正在准备通话"
    override val callTitleFailed = "连接失败"
    override val callEncryption = "端到端加密 DTLS-SRTP"
    override val callCreate = "发起通话"
    override val callCreateHint = "生成连接码，通过任意聊天软件发给对方。"
    override val callJoin = "加入通话"
    override val callJoinHint = "粘贴发起方发来的连接码。"
    override val callIntroBody = "任意网络均可使用：手机流量或Wi-Fi，双方无需" +
        "同一网络，可身处不同国家。语音全程加密；内置TURN中继可穿透复杂NAT" +
        "（只转发加密数据包）。通话中可用«短语»和«输入»——中文语音直接送给" +
        "对方。提示：中国大陆网络会屏蔽P2P，需开启VPN。"
    override val callPreparingCode = "正在生成连接码…"
    override val callWaitHint = "第一步：把您的连接码发给对方。" +
        "第二步：粘贴对方的应答码，点«连接»。"
    override val callAnswerLabel = "粘贴对方的应答码"
    override val callConnectAction = "连接"
    override val callAnswerReadyHint = "把此应答码发回给发起方，连接将自动建立。"
    override val callConnecting = "正在建立直连…"
    override val callConnectedHint = "请讲话——连接为直连并已加密。" +
        "«短语»和«输入»会把中文直接播入通话。"
    override val callFailedBody = "连接失败。请把其中一方在Wi-Fi与手机流量之间" +
        "切换后重试；中国大陆网络屏蔽P2P，需使用VPN。"
    override val callInviteLabel = "连接码"
    override val callContinue = "继续"
    override val callSendCode = "发送连接码"
    override val callCopy = "复制"
    override val callPaste = "从剪贴板粘贴"
    override val callCancel = "取消"
    override val callMicOn = "麦克风"
    override val callMicOff = "开麦"
    override val callSpeaker = "扬声器"
    override val callEarpiece = "听筒"
    override val callPhrases = "短语"
    override val callCompose = "输入"
    override val callEndTooltip = "挂断"
    override fun callShareText(code: String) = "«Толмач»加密通话连接码：\n" + code
    override val callChooserTitle = "发送连接码"

    override val settingsTitle = "设置"
    override val sectionLanguage = "App language · Язык приложения"
    override val languageRu = "Русский"
    override val languageZh = "中文（简体）"
    override val sectionTools = "工具"
    override val toolPreflight = "会前检查"
    override val toolPreflightHint = "清单：麦克风、翻译、语音包、耳机、音量。"
    override val toolGlossary = "术语词典"
    override val toolGlossaryHint = "自定义译文替换：品种、标准、贸易术语。"
    override val toolGuide = "快速指南"
    override val toolGuideHint = "三步上手：聆听、回复、短语与通话。"
    override val toolAbout = "关于应用"
    override val toolAboutHint = "版本信息与声道说明。"
    override val sectionPresets = "音频快捷方案"
    override val presetSpeakerTitle = "扬声器给对方"
    override val presetSpeakerHint = "俄语进您的耳机，中文从手机扬声器播出。"
    override val presetTwoPairsTitle = "两副耳机"
    override val presetTwoPairsHint = "俄语走蓝牙，中文走有线/USB耳机。"
    override val presetRemoteTitle = "远程通话"
    override val presetRemoteHint = "通话在旁边第二台设备免提进行：" +
        "俄语给您，中文由扬声器播入其麦克风。"
    override val presetPartnerBtTitle = "对方戴耳机"
    override val presetPartnerBtHint = "中文走蓝牙给对方，俄语从扬声器给您。"
    override val sectionDialect = "对方语言"
    override val dialectMandarinFull = "普通话——标准中文（推荐）"
    override val dialectTaiwanFull = "台湾华语"
    override val dialectCantoneseFull = "粤语——香港、广东（试验）"
    override val sectionRuRoute = "俄语译文输出到"
    override val sectionZhRoute = "中文译文输出到"
    override val ruRouteAuto = "自动——由系统决定"
    override val ruRouteBt = "蓝牙耳机"
    override val ruRouteWired = "有线 / USB 耳机"
    override val ruRouteSpeaker = "手机扬声器"
    override val zhRouteSpeaker = "手机扬声器（推荐）"
    override val zhRouteWired = "有线 / USB——第二副耳机"
    override val zhRouteBt = "蓝牙耳机"
    override val testRuChannel = "测试俄语声道"
    override val testZhChannel = "测试中文声道"
    override val sectionEngine = "翻译引擎"
    override val engineOffline = "离线——ML Kit，无需网络"
    override val engineDeepL = "DeepL——质量最佳（需网络与密钥）"
    override val deepLKeyLabel = "DeepL API 密钥"
    override val deepLHint = "免费密钥：deepl.com →«DeepL API Free»，" +
        "每月50万字符。断网时自动切换到离线翻译。"
    override val sectionRate = "中文朗读速度"
    override fun rateLine(rate: String) = rate + "×——慢一点对方听得更清楚"
    override val sectionConfirm = "俄语回复"
    override val confirmTitle = "朗读前先确认文字"
    override val confirmHint = "先显示识别出的文字，可修改后再播放给对方"
    override val sectionRuVoice = "俄语朗读语音"
    override val sectionZhVoice = "中文朗读语音"
    override val voicesLoading = "语音初始化后将显示可选语音"
    override val voiceSystemDefault = "系统默认"
    override val settingsFootnote = "两副耳机方案：俄语侧用蓝牙，中文侧用有线或" +
        "USB-C耳机（最好不带麦克风）。安卓不支持两副蓝牙分声道。" +
        "所选耳机不在时，声音自动回落到扬声器（中文）或系统路径（俄语）。"

    override val guideTitle = "使用指南"
    override val guideListenTitle = "聆听中文"
    override val guideListenText = "手机放桌上麦克风朝向对方，按绿色按钮，" +
        "译文将轻声送入您的耳机。"
    override val guideReplyTitle = "回复"
    override val guideReplyText = "说一句俄语，手机会用中文从扬声器朗读给对方。"
    override val guideMoreTitle = "短语与通话"
    override val guideMoreText = "现成商务短语在«短语»页；" +
        "与伙伴的加密通话在«通话»页。"
    override val guideFootnote = "指南随时可在：设置 → 工具 中打开。"
    override val gotIt = "知道了"

    override val pickerTitle = "界面语言 · Язык интерфейса"
    override val pickerRuSub = "Интерфейс на русском"
    override val pickerZhSub = "中文界面"

    override val glossaryTitle = "术语词典"
    override val glossaryHint = "替换作用于译文成品：品种、标准、贸易术语。" +
        "例：«инкотермс» → «Incoterms»。"
    override val glossaryFrom = "当前译法"
    override val glossaryTo = "应译为"
    override val addAction = "添加"
    override val doneAction = "完成"

    override val composeTitle = "输入俄语句子"
    override val composeHint = "输入俄语，手机将译成中文并朗读。" +
        "«存入短语»会把该句连同译文保存到«我的短语»。"
    override val composeLabel = "您的句子（俄语）"
    override val composeSpeak = "翻译并朗读"
    override val composeSave = "存入短语"
    override val cancelAction = "取消"

    override fun aboutTitle(version: String) = "Толмач " + version
    override val aboutLine1 = "哈萨克斯坦AGROPLANET公司（科斯塔奈）谈判翻译应用。"
    override val aboutLine2 = "翻译与记录可离线使用。俄语声道走您一侧耳机，" +
        "中文走扬声器或第二副耳机。合同的法律条款请以专业译员核对为准。"

    override val preflightTitle = "会前检查"
    override val checkMic = "麦克风已授权"
    override val checkMicHintNo = "首次录音时系统会请求权限"
    override val checkTranslation = "翻译已就绪"
    override val checkTranslationDeepL = "DeepL引擎，离线作为备份"
    override val checkTranslationOffline = "离线模型已就位"
    override val checkTranslationNo = "模型尚未下载——需要网络"
    override val checkRuVoice = "俄语语音已安装"
    override val checkZhVoice = "中文语音已安装"
    override val checkVoiceHintNo = "请在系统语音合成设置中下载"
    override val openTtsSettings = "打开语音合成设置"
    override val checkHeadphones = "耳机"
    override val headphonesDetailTwo = "两副：蓝牙与有线/USB"
    override val headphonesDetailBt = "蓝牙已连接"
    override val headphonesDetailWired = "有线/USB已连接"
    override val headphonesDetailNone = "未连接——俄语将从扬声器播出"
    override val checkVolume = "媒体音量"
    override fun volumeDetail(percent: Int) =
        percent.toString() + "%" + if (percent < 25) "——请用侧键调高" else ""
    override val testRuShort = "测试俄语"
    override val testZhShort = "测试中文"

    override val replyConfirmTitle = "请确认句子"
    override val replyConfirmHint = "这是手机识别到的内容，可修改后再让对方听到准确译文。"
    override val replyConfirmSpeak = "用中文朗读"

    override fun voiceMissing(chinese: Boolean): String {
        val lang = if (chinese) "中文" else "俄语"
        return "缺少" + lang + "语音包。请到系统设置 → 无障碍 → " +
            "文字转语音，下载对应语音（Google引擎）后重试。"
    }
    override val modelsNotReady = "翻译模型尚未下载：顶部状态应显示«翻译：就绪»。" +
        "请连接网络并点«重新下载翻译模型»。"
    override fun translationFailed(reason: String?) =
        "翻译失败：" + (reason ?: "请重试")
    override val savedToMyPhrases = "已保存到«我的短语»。"
    override val shareAudioFailed = "音频生成失败——请检查中文语音包。"
    override val callErrInvite = "无法识别连接码——请完整复制。"
    override val callErrAnswer = "无法识别应答码——请完整复制。"
    override fun callErrPrepare(detail: String) = "通话准备失败：" + detail
    override fun callErrApply(detail: String) = "应用连接码失败：" + detail
    override val callErrIce = "网络未能建立连接——请更换Wi-Fi或手机流量后重试。"
}

fun strings(language: String): AppStrings =
    if (language == "zh") ZhStrings else RuStrings
