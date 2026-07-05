package com.google.mlkit.nl.translate

import com.google.android.gms.tasks.Task

object TranslateLanguage {
    const val CHINESE = "zh"
    const val RUSSIAN = "ru"
}

class TranslatorOptions private constructor() {
    class Builder {
        fun setSourceLanguage(lang: String): Builder = this
        fun setTargetLanguage(lang: String): Builder = this
        fun build(): TranslatorOptions = TranslatorOptions()
    }
}

interface Translator {
    fun translate(text: String): Task<String>
    fun downloadModelIfNeeded(
        conditions: com.google.mlkit.common.model.DownloadConditions
    ): Task<Void>
    fun close()
}

object Translation {
    fun getClient(options: TranslatorOptions): Translator = object : Translator {
        override fun translate(text: String): Task<String> =
            Task("[fake-translate] " + text)
        override fun downloadModelIfNeeded(
            conditions: com.google.mlkit.common.model.DownloadConditions
        ): Task<Void> = Task(null)
        override fun close() {}
    }
}
