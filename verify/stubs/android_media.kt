package android.media
class AudioDeviceInfo(val type: Int) {
    companion object {
        const val TYPE_BUILTIN_SPEAKER = 2; const val TYPE_WIRED_HEADSET = 3
        const val TYPE_WIRED_HEADPHONES = 4; const val TYPE_BLUETOOTH_A2DP = 8
        const val TYPE_USB_DEVICE = 11; const val TYPE_USB_HEADSET = 22
        const val TYPE_BLE_HEADSET = 26
    }
}
open class AudioDeviceCallback {
    open fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {}
    open fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {}
}
class AudioManager {
    var mode: Int = 0
    var isSpeakerphoneOn: Boolean = false
    fun getDevices(flags: Int): Array<AudioDeviceInfo> = emptyArray()
    fun getStreamVolume(streamType: Int): Int = 9
    fun getStreamMaxVolume(streamType: Int): Int = 15
    fun registerAudioDeviceCallback(callback: AudioDeviceCallback, handler: android.os.Handler?) {}
    fun unregisterAudioDeviceCallback(callback: AudioDeviceCallback) {}
    companion object {
        const val GET_DEVICES_OUTPUTS = 2
        const val STREAM_MUSIC = 3
        const val MODE_NORMAL = 0
        const val MODE_IN_COMMUNICATION = 3
    }
}
class AudioAttributes private constructor() {
    class Builder {
        fun setUsage(usage: Int): Builder = this
        fun setContentType(type: Int): Builder = this
        fun build(): AudioAttributes = AudioAttributes()
    }
    companion object { const val USAGE_MEDIA = 1; const val CONTENT_TYPE_SPEECH = 1 }
}
class MediaPlayer {
    fun setAudioAttributes(attributes: AudioAttributes) {}
    fun setDataSource(path: String) {}
    fun prepare() {}
    fun setPreferredDevice(device: AudioDeviceInfo): Boolean = true
    fun setOnCompletionListener(listener: (MediaPlayer) -> Unit) {}
    fun start() {}
    fun release() {}
}
