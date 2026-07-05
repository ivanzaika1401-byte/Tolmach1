package app.tolmach

import app.tolmach.engine.AudioRoutes

fun main() {
    var checks = 0
    fun ok(condition: Boolean, name: String) {
        require(condition) { "ПРОВАЛ: " + name }
        checks++
    }

    val app = android.app.Application()
    var brain = TranslatorViewModel(app)

    val start = brain.state.value
    ok(start.mode == Mode.IDLE, "старт в покое")
    ok(start.modelsReady, "модели готовы (трамплин исполнил загрузку)")
    ok(start.chineseRoute == AudioRoutes.SPEAKER, "китайский по умолчанию в динамик")
    ok(start.russianRoute == AudioRoutes.SYSTEM, "русский по умолчанию системно")
    ok(start.callState == "idle", "звонок не активен")
    ok(start.showGuide, "гид показывается при первом запуске")
    brain.dismissGuide()
    ok(!brain.state.value.showGuide, "гид закрыт")
    ok(start.appLanguage == "ru", "язык по умолчанию — русский")
    ok(start.showLanguagePicker, "выбор языка предлагается при первом входе")
    brain.setAppLanguage("zh")
    ok(
        brain.state.value.appLanguage == "zh" && !brain.state.value.showLanguagePicker,
        "язык переключён на китайский, пикер закрыт",
    )

    brain.setChineseLanguage("yue-Hant-HK")
    ok(brain.state.value.chineseLanguageTag == "yue-Hant-HK", "смена диалекта")

    brain.setRussianRoute(AudioRoutes.BLUETOOTH)
    brain.setChineseRoute(AudioRoutes.WIRED)
    ok(brain.state.value.russianRoute == AudioRoutes.BLUETOOTH, "маршрут RU")
    ok(brain.state.value.chineseRoute == AudioRoutes.WIRED, "маршрут ZH")

    brain.addGlossaryEntry("инкотермс", "Incoterms")
    ok(brain.state.value.glossary.size == 1, "термин в глоссарии")

    brain.speakPhrase(BuiltInPhrases.first())
    ok(brain.state.value.messages.size == 1, "фраза легла в ленту")
    ok(brain.state.value.sessionStart != null, "сеанс открыт")

    val transcript = brain.transcriptText()
    ok("AGROPLANET" in transcript, "бренд в стенограмме")
    ok(BuiltInPhrases.first().chinese in transcript, "иероглифы в стенограмме")

    val exported = brain.exportTranscriptFile()
    ok(exported != null && java.io.File(exported).exists(), "экспорт .txt на диск")

    brain.clearConversation()
    ok(brain.state.value.messages.isEmpty(), "очистка")
    brain.undoClear()
    ok(brain.state.value.messages.size == 1, "«Вернуть» вернул стенограмму")

    // Живая ветка перевода через трамплин корутин и фейк-переводчик
    brain.composeAndSpeak("Договор по инкотермс готов")
    val composed = brain.state.value.messages.last()
    ok(brain.state.value.messages.size == 2, "набранная фраза легла в ленту")
    ok("[fake-translate]" in composed.translated, "перевод прошёл через движок")
    ok("Incoterms" in composed.translated, "глоссарий применён к переводу")

    brain.addCustomPhrase("Сколько тонн в одном вагоне?")
    val custom = brain.state.value.customPhrases
    ok(custom.size == 1, "своя фраза добавлена")
    ok("[fake-translate]" in custom.first().chinese, "своя фраза переведена")

    ok(brain.mediaVolumePercent() == 60, "процент громкости: 9 из 15 = 60")

    brain.setConfirmReply(true)
    brain.setChineseRate(0.8f)
    ok(brain.state.value.confirmReply, "режим проверки ответа")
    ok(brain.state.value.chineseRate == 0.8f, "скорость речи")

    brain.openCallMenu()
    ok(brain.state.value.callState == "menu", "меню звонка открылось")
    brain.closeCallUi()
    ok(brain.state.value.callState == "idle", "звонок закрыт")

    // Перезапуск: настройки, глоссарий, ЛЕНТА и СВОИ ФРАЗЫ обязаны выжить
    brain = TranslatorViewModel(android.app.Application())
    val reborn = brain.state.value
    ok(reborn.chineseLanguageTag == "yue-Hant-HK", "диалект пережил перезапуск")
    ok(reborn.glossary.size == 1, "глоссарий пережил перезапуск")
    ok(reborn.russianRoute == AudioRoutes.BLUETOOTH, "маршрут RU пережил перезапуск")
    ok(reborn.confirmReply, "режим проверки пережил перезапуск")
    ok(reborn.chineseRate == 0.8f, "скорость пережила перезапуск")
    ok(reborn.messages.size == 2, "ЛЕНТА пережила перезапуск (живой JSON)")
    ok(reborn.sessionStart != null, "начало сеанса пережило перезапуск")
    ok(reborn.customPhrases.size == 1, "свои фразы пережили перезапуск")
    ok(!reborn.showGuide, "гид не повторяется после перезапуска")
    ok(
        reborn.appLanguage == "zh" && !reborn.showLanguagePicker,
        "выбранный язык пережил перезапуск",
    )
    ok(
        reborn.messages.last().translated == composed.translated,
        "текст реплики восстановлен дословно",
    )

    // ===== Полное рукопожатие защищённого звонка: A создаёт, B отвечает =====
    val partner = TranslatorViewModel(android.app.Application())

    brain.startCallAsCaller()
    ok(brain.state.value.callState == "wait_answer", "A: код готов, ждёт ответа")
    val inviteCode = brain.state.value.callLocalCode
    ok(inviteCode.isNotBlank(), "A: код-приглашение не пуст")

    partner.beginJoin()
    ok(partner.state.value.callState == "join_input", "B: экран ввода кода")
    partner.submitJoinCode(inviteCode)
    ok(partner.state.value.callState == "answer_ready", "B: код-ответ готов")
    val answerCode = partner.state.value.callLocalCode
    ok(answerCode.isNotBlank() && answerCode != inviteCode, "B: ответ отличается от приглашения")

    brain.submitAnswerCode(answerCode)
    ok(brain.state.value.callState == "connected", "A: СОЕДИНЕНИЕ УСТАНОВЛЕНО")
    ok(brain.state.value.callSeconds == 5, "A: тикер отсчитал (стаб-предел 5)")

    brain.toggleCallMute()
    ok(brain.state.value.callMuted, "A: микрофон выключен")
    brain.toggleCallMute()
    ok(!brain.state.value.callMuted, "A: микрофон включён обратно")
    brain.toggleCallSpeaker()
    ok(!brain.state.value.callSpeakerOn, "A: режим «у уха»")

    brain.endCall()
    ok(
        brain.state.value.callState == "idle" &&
            brain.state.value.callSeconds == 0 &&
            brain.state.value.callLocalCode.isEmpty(),
        "A: звонок завершён, состояние очищено",
    )
    partner.endCall()
    ok(partner.state.value.callState == "idle", "B: завершил корректно")

    println("МОЗГ ЗАПУЩЕН И ЖИВ: пройдено проверок: " + checks)
    println("CHECKS_PASSED=" + checks)
}
