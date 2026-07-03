package app.tolmach.engine

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

/**
 * Перевод китайский <-> русский на базе ML Kit.
 *
 * Модели (~30-40 МБ на пару) скачиваются один раз при первом запуске,
 * дальше перевод работает полностью офлайн — важно для поездок в КНР,
 * где сервисы Google недоступны.
 */
class TranslationEngine {

    private val zhToRu: Translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.CHINESE)
            .setTargetLanguage(TranslateLanguage.RUSSIAN)
            .build(),
    )

    private val ruToZh: Translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.RUSSIAN)
            .setTargetLanguage(TranslateLanguage.CHINESE)
            .build(),
    )

    /** Скачивает офлайн-модели, если их ещё нет на устройстве. */
    suspend fun downloadModels(allowMobileData: Boolean) {
        val conditions = DownloadConditions.Builder()
            .apply { if (!allowMobileData) requireWifi() }
            .build()
        zhToRu.downloadModelIfNeeded(conditions).await()
        ruToZh.downloadModelIfNeeded(conditions).await()
    }

    suspend fun translateChineseToRussian(text: String): String =
        zhToRu.translate(text).await()

    suspend fun translateRussianToChinese(text: String): String =
        ruToZh.translate(text).await()

    fun close() {
        zhToRu.close()
        ruToZh.close()
    }
}
