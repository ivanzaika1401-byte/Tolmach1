package com.google.mlkit.common.model
class DownloadConditions private constructor() {
    class Builder {
        fun requireWifi(): Builder = this
        fun build(): DownloadConditions = DownloadConditions()
    }
}
