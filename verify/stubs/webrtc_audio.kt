package org.webrtc.audio
class JavaAudioDeviceModule private constructor() {
    class Builder { fun createAudioDeviceModule(): JavaAudioDeviceModule = JavaAudioDeviceModule() }
    companion object { fun builder(context: android.content.Context): Builder = Builder() }
}
