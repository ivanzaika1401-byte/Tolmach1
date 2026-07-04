package app.tolmach.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import org.json.JSONObject
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.audio.JavaAudioDeviceModule

/**
 * Зашифрованный P2P-звонок без серверов.
 *
 * Технология — WebRTC (на ней звонят Telegram и WhatsApp): медиапоток
 * всегда шифруется DTLS-SRTP, отключить это невозможно по спецификации.
 * Сигнализация — «ручная»: стороны обмениваются кодом-приглашением через
 * любой мессенджер, дальше звук идёт напрямую телефон-телефон.
 *
 * NAT-пробой — публичные STUN Google; TURN-ретранслятора нет (он требует
 * сервер), поэтому за симметричными NAT и фаерволом КНР соединение может
 * не установиться — это честное ограничение серверлесс-схемы.
 */
class CallEngine(
    private val context: Context,
    private val onState: (String) -> Unit,
    private val onLocalCode: (String) -> Unit,
    private val onError: (String) -> Unit,
) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var factory: PeerConnectionFactory? = null
    private var peer: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localTrack: AudioTrack? = null
    private var codeSent = false

    fun startAsCaller() {
        codeSent = false
        setUp()
        val constraints = offerConstraints()
        peer?.createOffer(
            sdpObserver(onCreated = { description ->
                peer?.setLocalDescription(sdpObserver(), description)
            }),
            constraints,
        )
        post { onState("preparing") }
    }

    fun startAsJoiner(inviteCode: String) {
        codeSent = false
        setUp()
        val remote = decode(inviteCode)
        if (remote == null) {
            post { onError("Код приглашения не распознан — скопируйте его целиком.") }
            post { onState("failed") }
            return
        }
        peer?.setRemoteDescription(
            sdpObserver(onSet = {
                peer?.createAnswer(
                    sdpObserver(onCreated = { description ->
                        peer?.setLocalDescription(sdpObserver(), description)
                    }),
                    offerConstraints(),
                )
            }),
            remote,
        )
        post { onState("preparing") }
    }

    /** Звонящий вставляет код-ответ собеседника — соединение стартует. */
    fun acceptAnswer(answerCode: String) {
        val remote = decode(answerCode)
        if (remote == null) {
            post { onError("Код ответа не распознан — скопируйте его целиком.") }
            return
        }
        peer?.setRemoteDescription(sdpObserver(), remote)
        post { onState("connecting") }
    }

    fun setMuted(muted: Boolean) {
        localTrack?.setEnabled(!muted)
    }

    fun close() {
        runCatching { peer?.close() }
        runCatching { peer?.dispose() }
        peer = null
        runCatching { localTrack?.dispose() }
        localTrack = null
        runCatching { audioSource?.dispose() }
        audioSource = null
        runCatching { factory?.dispose() }
        factory = null
    }

    // ---------- внутреннее ----------

    private fun setUp() {
        close()
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions(),
        )
        val audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .createAudioDeviceModule()
        factory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .createPeerConnectionFactory()

        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
                .createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302")
                .createIceServer(),
        )
        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        peer = factory?.createPeerConnection(config, observer)

        audioSource = factory?.createAudioSource(MediaConstraints())
        localTrack = factory?.createAudioTrack("tolmach-audio", audioSource)
        val track = localTrack
        if (track != null) {
            peer?.addTrack(track, listOf("tolmach-stream"))
        }
    }

    private fun offerConstraints() = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
    }

    /** Когда сбор сетевых кандидатов завершён — код готов к отправке. */
    private fun publishLocalCode() {
        if (codeSent) return
        val description = peer?.localDescription ?: return
        codeSent = true
        val json = JSONObject()
            .put("t", description.type.canonicalForm())
            .put("s", description.description)
        val code = Base64.encodeToString(
            json.toString().toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP,
        )
        post { onLocalCode(code) }
    }

    private fun decode(code: String): SessionDescription? = runCatching {
        val json = JSONObject(
            String(Base64.decode(code.trim(), Base64.NO_WRAP), Charsets.UTF_8),
        )
        SessionDescription(
            SessionDescription.Type.fromCanonicalForm(json.getString("t")),
            json.getString("s"),
        )
    }.getOrNull()

    private val observer = object : PeerConnection.Observer {
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
            if (state == PeerConnection.IceGatheringState.COMPLETE) {
                mainHandler.post { publishLocalCode() }
            }
        }

        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
            when (state) {
                PeerConnection.IceConnectionState.CONNECTED,
                PeerConnection.IceConnectionState.COMPLETED,
                -> post { onState("connected") }

                PeerConnection.IceConnectionState.FAILED ->
                    post {
                        onError(
                            "Соединение не пробилось через сеть — попробуйте другой " +
                                "Wi-Fi или мобильный интернет.",
                        )
                        onState("failed")
                    }

                PeerConnection.IceConnectionState.DISCONNECTED ->
                    post { onState("connecting") }

                else -> Unit
            }
        }

        override fun onConnectionChange(state: PeerConnection.PeerConnectionState?) {
            if (state == PeerConnection.PeerConnectionState.CONNECTED) {
                post { onState("connected") }
            }
        }

        override fun onSignalingChange(state: PeerConnection.SignalingState?) = Unit
        override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
        override fun onIceCandidate(candidate: IceCandidate?) = Unit
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
        override fun onAddStream(stream: MediaStream?) = Unit
        override fun onRemoveStream(stream: MediaStream?) = Unit
        override fun onDataChannel(channel: DataChannel?) = Unit
        override fun onRenegotiationNeeded() = Unit
    }

    private fun sdpObserver(
        onCreated: (SessionDescription) -> Unit = {},
        onSet: () -> Unit = {},
    ) = object : SdpObserver {
        override fun onCreateSuccess(description: SessionDescription?) {
            if (description != null) mainHandler.post { onCreated(description) }
        }

        override fun onSetSuccess() {
            mainHandler.post { onSet() }
        }

        override fun onCreateFailure(error: String?) {
            post { onError("Не удалось подготовить звонок: ${error ?: ""}") }
        }

        override fun onSetFailure(error: String?) {
            post { onError("Не удалось применить код: ${error ?: ""}") }
        }
    }

    private fun post(block: () -> Unit) = mainHandler.post(block)
}
