package org.webrtc

class SessionDescription(val type: Type, val description: String) {
    enum class Type {
        OFFER, ANSWER;
        fun canonicalForm(): String = name.toLowerCase()
        companion object {
            fun fromCanonicalForm(canonical: String): Type =
                if (canonical == "answer") ANSWER else OFFER
        }
    }
}
class IceCandidate
class MediaStream
class DataChannel
class AudioSource { fun dispose() {} }
class AudioTrack {
    fun setEnabled(enabled: Boolean): Boolean = true
    fun dispose() {}
}
class RtpSender
class MediaConstraints {
    class KeyValuePair(key: String, value: String)
    val mandatory: MutableList<KeyValuePair> = mutableListOf()
}
interface SdpObserver {
    fun onCreateSuccess(description: SessionDescription?)
    fun onSetSuccess()
    fun onCreateFailure(error: String?)
    fun onSetFailure(error: String?)
}

/**
 * Симуляция сигнализации: createOffer/createAnswer тут же отдают SDP,
 * setLocalDescription завершает «сбор кандидатов» (ICE COMPLETE),
 * setRemoteDescription с ANSWER немедленно «соединяет» — полный цикл
 * рукопожатия исполняется синхронно и детерминированно.
 */
class PeerConnection internal constructor(private val observer: Observer) {
    enum class IceGatheringState { NEW, GATHERING, COMPLETE }
    enum class IceConnectionState {
        NEW, CHECKING, CONNECTED, COMPLETED, FAILED, DISCONNECTED, CLOSED
    }
    enum class SignalingState { STABLE }
    enum class PeerConnectionState { NEW, CONNECTING, CONNECTED, DISCONNECTED, FAILED, CLOSED }
    enum class SdpSemantics { PLAN_B, UNIFIED_PLAN }

    interface Observer {
        fun onSignalingChange(state: SignalingState?)
        fun onIceConnectionChange(state: IceConnectionState?)
        fun onIceConnectionReceivingChange(receiving: Boolean)
        fun onIceGatheringChange(state: IceGatheringState?)
        fun onIceCandidate(candidate: IceCandidate?)
        fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?)
        fun onAddStream(stream: MediaStream?)
        fun onRemoveStream(stream: MediaStream?)
        fun onDataChannel(channel: DataChannel?)
        fun onRenegotiationNeeded()
        fun onConnectionChange(newState: PeerConnectionState?) {}
    }
    class IceServer private constructor() {
        class Builder {
            fun setUsername(username: String): Builder = this
            fun setPassword(password: String): Builder = this
            fun createIceServer(): IceServer = IceServer()
        }
        companion object { fun builder(uri: String): Builder = Builder() }
    }
    open class RTCConfiguration(iceServers: List<IceServer>) {
        var sdpSemantics: SdpSemantics = SdpSemantics.PLAN_B
    }

    var localDescription: SessionDescription? = null
        private set

    fun createOffer(sdpObserver: SdpObserver, constraints: MediaConstraints) {
        sdpObserver.onCreateSuccess(
            SessionDescription(SessionDescription.Type.OFFER, "sdp-offer-stub")
        )
    }
    fun createAnswer(sdpObserver: SdpObserver, constraints: MediaConstraints) {
        sdpObserver.onCreateSuccess(
            SessionDescription(SessionDescription.Type.ANSWER, "sdp-answer-stub")
        )
    }
    fun setLocalDescription(sdpObserver: SdpObserver, description: SessionDescription) {
        localDescription = description
        sdpObserver.onSetSuccess()
        observer.onIceGatheringChange(IceGatheringState.COMPLETE)
    }
    fun setRemoteDescription(sdpObserver: SdpObserver, description: SessionDescription) {
        sdpObserver.onSetSuccess()
        if (description.type == SessionDescription.Type.ANSWER) {
            observer.onIceConnectionChange(IceConnectionState.CONNECTED)
        }
    }
    fun addTrack(track: AudioTrack, streamIds: List<String>): RtpSender = RtpSender()
    fun close() {}
    fun dispose() {}
}

class PeerConnectionFactory private constructor() {
    class InitializationOptions private constructor() {
        class Builder {
            fun createInitializationOptions(): InitializationOptions = InitializationOptions()
        }
        companion object {
            fun builder(context: android.content.Context): Builder = Builder()
        }
    }
    class Builder {
        fun setAudioDeviceModule(module: org.webrtc.audio.JavaAudioDeviceModule): Builder = this
        fun createPeerConnectionFactory(): PeerConnectionFactory = PeerConnectionFactory()
    }
    fun createPeerConnection(
        config: PeerConnection.RTCConfiguration,
        observer: PeerConnection.Observer
    ): PeerConnection? = PeerConnection(observer)
    fun createAudioSource(constraints: MediaConstraints): AudioSource = AudioSource()
    fun createAudioTrack(id: String, source: AudioSource?): AudioTrack = AudioTrack()
    fun dispose() {}
    companion object {
        fun initialize(options: InitializationOptions) {}
        fun builder(): Builder = Builder()
    }
}
