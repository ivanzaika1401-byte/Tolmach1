package app.tolmach.engine

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Гибридный перевод китайский <-> русский.
 *
 * Базовый слой — офлайн-модели ML Kit: работают без интернета, что критично
 * для КНР. Опциональный верхний слой — DeepL API (максимальное качество
 * перевода на рынке): включается ключом пользователя в настройках.
 * Любой сбой сети или API мгновенно и незаметно откатывает на офлайн —
 * встреча не зависит от Wi-Fi.
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

    /**
     * Универсальный перевод: DeepL при включённом ключе, офлайн ML Kit
     * как основа и как страховка при любой ошибке сети.
     */
    suspend fun translate(
        text: String,
        toChinese: Boolean,
        useDeepL: Boolean,
        deepLKey: String,
    ): String {
        if (useDeepL && deepLKey.isNotBlank()) {
            val online = runCatching { deepL(text, toChinese, deepLKey) }.getOrNull()
            if (!online.isNullOrBlank()) return online
        }
        return if (toChinese) {
            ruToZh.translate(text).await()
        } else {
            zhToRu.translate(text).await()
        }
    }

    private suspend fun deepL(
        text: String,
        toChinese: Boolean,
        apiKey: String,
    ): String = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        val endpoint = if (key.endsWith(":fx")) {
            "https://api-free.deepl.com/v2/translate"
        } else {
            "https://api.deepl.com/v2/translate"
        }
        val target = if (toChinese) "ZH" else "RU"
        val body = "text=" + URLEncoder.encode(text, "UTF-8") + "&target_lang=" + target

        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 4000
            connection.readTimeout = 6000
            connection.setRequestProperty("Authorization", "DeepL-Auth-Key $key")
            connection.setRequestProperty(
                "Content-Type",
                "application/x-www-form-urlencoded; charset=utf-8",
            )
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("DeepL HTTP $code")
            JSONObject(response)
                .getJSONArray("translations")
                .getJSONObject(0)
                .getString("text")
        } finally {
            connection.disconnect()
        }
    }

    fun close() {
        zhToRu.close()
        ruToZh.close()
    }
}
