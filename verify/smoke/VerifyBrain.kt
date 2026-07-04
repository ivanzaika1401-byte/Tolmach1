package app.tolmach

import app.tolmach.engine.AudioRoutes

fun main() {
    var checks = 0
    fun ok(condition: Boolean, name: String) {
        require(condition) { "ПРОВАЛ: $name" }
        checks++
    }

    val app = android.app.Application()
    var brain = TranslatorViewModel(app)

    val start = brain.state.value
    ok(start.mode == Mode.IDLE, "старт в покое")
    ok(!start.modelsReady, "модели честно не готовы")
    ok(start.chineseRoute == AudioRoutes.SPEAKER, "китайский по умолчанию в динамик")
    ok(start.russianRoute == AudioRoutes.SYSTEM, "русский по умолчанию системно")
    ok(start.callState == "idle", "звонок не активен")

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

    ok(brain.mediaVolumePercent() == 60, "процент громкости: 9 из 15 = 60")

    brain.setConfirmReply(true)
    brain.setChineseRate(0.8f)
    ok(brain.state.value.confirmReply, "режим проверки ответа")
    ok(brain.state.value.chineseRate == 0.8f, "скорость речи")

    brain.openCallMenu()
    ok(brain.state.value.callState == "menu", "меню звонка открылось")
    brain.closeCallUi()
    ok(brain.state.value.callState == "idle", "звонок закрыт")

    brain = TranslatorViewModel(android.app.Application())
    val reborn = brain.state.value
    ok(reborn.chineseLanguageTag == "yue-Hant-HK", "диалект пережил перезапуск")
    ok(reborn.glossary.size == 1, "глоссарий пережил перезапуск")
    ok(reborn.russianRoute == AudioRoutes.BLUETOOTH, "маршрут RU пережил перезапуск")
    ok(reborn.confirmReply, "режим проверки пережил перезапуск")
    ok(reborn.chineseRate == 0.8f, "скорость пережила перезапуск")

    println("МОЗГ ЗАПУЩЕН И ЖИВ: пройдено проверок: " + checks)
}
