package app.tolmach

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Целостность разговорника Agroplanet: выполняется в облаке
 * при каждой сборке, до компиляции APK.
 */
class PhrasesTest {

    private val cjk = Regex("[\\u4e00-\\u9fff]")
    private val cyrillic = Regex("[А-Яа-яЁё]")

    @Test
    fun phrasebookIsComplete() {
        assertTrue("слишком мало фраз", BuiltInPhrases.size >= 15)
        val categories = BuiltInPhrases.map { it.category }.distinct()
        assertTrue(
            "категории или их порядок изменились: $categories",
            categories == listOf(
                "Знакомство",
                "Продукция и качество",
                "Цена и условия",
                "Логистика",
                "Ход переговоров",
            ),
        )
    }

    @Test
    fun everyPhraseIsSound() {
        BuiltInPhrases.forEach { phrase ->
            assertTrue("пустой русский текст", phrase.russian.isNotBlank())
            assertTrue(
                "нет иероглифов: ${phrase.russian}",
                cjk.containsMatchIn(phrase.chinese),
            )
            assertFalse(
                "кириллица попала в китайский: ${phrase.russian}",
                cyrillic.containsMatchIn(phrase.chinese),
            )
        }
    }
}
