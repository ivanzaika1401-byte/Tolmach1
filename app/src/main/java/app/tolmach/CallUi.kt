package app.tolmach

import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tolmach.ui.TolmachColors

/** Вкладка «Звонок»: защищённый P2P-звонок, полностью двуязычный. */
@Composable
fun CallScreen(
    state: UiState,
    onCreate: () -> Unit,
    onBeginJoin: () -> Unit,
    onSubmitJoinCode: (String) -> Unit,
    onSubmitAnswerCode: (String) -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onOpenPhrases: () -> Unit,
    onOpenCompose: () -> Unit,
    onEnd: () -> Unit,
) {
    val s = strings(state.appLanguage)
    val context = LocalContext.current

    fun sendCode(code: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, s.callShareText(code))
        }
        context.startActivity(Intent.createChooser(intent, s.callChooserTitle))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(26.dp))
        CallOrb(connected = state.callState == "connected")
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = TolmachColors.Jade,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = s.callEncryption,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = callStatusTitle(state, s),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))

        Crossfade(targetState = state.callState, label = "callPhase") { phase ->
            when (phase) {
                "idle" -> CallIntro(s = s, onCreate = onCreate, onBeginJoin = onBeginJoin)

                "preparing", "preparing_answer" -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = s.callPreparingCode,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    WaitingDots()
                }

                "wait_answer" -> CodeExchange(
                    s = s,
                    myCode = state.callLocalCode,
                    inputLabel = s.callAnswerLabel,
                    submitLabel = s.callConnectAction,
                    hint = s.callWaitHint,
                    onSend = { sendCode(state.callLocalCode) },
                    onSubmit = onSubmitAnswerCode,
                )

                "join_input" -> JoinInput(s = s, onSubmit = onSubmitJoinCode)

                "answer_ready" -> CodeExchange(
                    s = s,
                    myCode = state.callLocalCode,
                    inputLabel = null,
                    submitLabel = null,
                    hint = s.callAnswerReadyHint,
                    onSend = { sendCode(state.callLocalCode) },
                    onSubmit = null,
                )

                "connecting" -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = s.callConnecting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    WaitingDots()
                }

                "connected" -> ConnectedControls(
                    state = state,
                    s = s,
                    onToggleMute = onToggleMute,
                    onToggleSpeaker = onToggleSpeaker,
                    onOpenPhrases = onOpenPhrases,
                    onOpenCompose = onOpenCompose,
                )

                "failed" -> Text(
                    text = s.callFailedBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        if (state.callState == "connected") {
            EndCallButton(s = s, onEnd = onEnd)
        } else if (state.callState != "idle") {
            TextButton(onClick = onEnd) { Text(s.callCancel) }
        }
        Spacer(Modifier.height(20.dp))
    }
}

private fun callStatusTitle(state: UiState, s: AppStrings): String = when (state.callState) {
    "idle" -> s.callTitleIdle
    "connected" -> formatCallTime(state.callSeconds)
    "failed" -> s.callTitleFailed
    else -> s.callTitlePreparing
}

/** Три пульсирующие точки — «приложение думает». */
@Composable
private fun WaitingDots() {
    val transition = rememberInfiniteTransition(label = "dots")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Restart),
        label = "dotsProgress",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        for (index in 0..2) {
            val phase = (progress + index * 0.33f) % 1f
            val dotAlpha = 0.25f + 0.75f * (if (phase < 0.5f) phase * 2f else (1f - phase) * 2f)
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer { alpha = dotAlpha }
                    .background(TolmachColors.Jade, CircleShape),
            )
        }
    }
}

private fun formatCallTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
private fun CallIntro(s: AppStrings, onCreate: () -> Unit, onBeginJoin: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        CallChoice(
            title = s.callCreate,
            subtitle = s.callCreateHint,
            onClick = onCreate,
        )
        Spacer(Modifier.height(10.dp))
        CallChoice(
            title = s.callJoin,
            subtitle = s.callJoinHint,
            onClick = onBeginJoin,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = s.callIntroBody,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CallChoice(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TolmachColors.Jade,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun JoinInput(s: AppStrings, onSubmit: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text(s.callInviteLabel) },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row {
            TextButton(
                onClick = { clipboard.getText()?.text?.let { code = it } },
            ) { Text(s.callPaste) }
            TextButton(
                enabled = code.isNotBlank(),
                onClick = { onSubmit(code) },
            ) { Text(s.callContinue) }
        }
    }
}

@Composable
private fun CodeExchange(
    s: AppStrings,
    myCode: String,
    inputLabel: String?,
    submitLabel: String?,
    hint: String,
    onSend: () -> Unit,
    onSubmit: ((String) -> Unit)?,
) {
    var answer by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (myCode.length > 60) myCode.take(60) + "…" else myCode,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(10.dp),
                maxLines = 2,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row {
            TextButton(onClick = onSend) { Text(s.callSendCode) }
            TextButton(
                onClick = { clipboard.setText(AnnotatedString(myCode)) },
            ) { Text(s.callCopy) }
        }
        if (inputLabel != null && submitLabel != null && onSubmit != null) {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it },
                label = { Text(inputLabel) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Row {
                TextButton(
                    onClick = { clipboard.getText()?.text?.let { answer = it } },
                ) { Text(s.callPaste) }
                TextButton(
                    enabled = answer.isNotBlank(),
                    onClick = { onSubmit(answer) },
                ) { Text(submitLabel) }
            }
        }
    }
}

@Composable
private fun ConnectedControls(
    state: UiState,
    s: AppStrings,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onOpenPhrases: () -> Unit,
    onOpenCompose: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = s.callConnectedHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CallRoundButton(
                icon = if (state.callMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                label = if (state.callMuted) s.callMicOff else s.callMicOn,
                active = !state.callMuted,
                onClick = onToggleMute,
            )
            CallRoundButton(
                icon = Icons.Filled.Bolt,
                label = s.callPhrases,
                active = false,
                accentGold = true,
                onClick = onOpenPhrases,
            )
            CallRoundButton(
                icon = Icons.Filled.Keyboard,
                label = s.callCompose,
                active = false,
                onClick = onOpenCompose,
            )
            CallRoundButton(
                icon = Icons.Filled.VolumeUp,
                label = if (state.callSpeakerOn) s.callSpeaker else s.callEarpiece,
                active = state.callSpeakerOn,
                onClick = onToggleSpeaker,
            )
        }
    }
}

@Composable
private fun CallRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    accentGold: Boolean = false,
    enabled: Boolean = true,
) {
    val accent = if (accentGold) TolmachColors.Gold else TolmachColors.Jade
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    if (active) accent else TolmachColors.Surface,
                    CircleShape,
                )
                .border(1.dp, TolmachColors.Hairline, CircleShape)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) TolmachColors.JadeDeep else accent,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EndCallButton(s: AppStrings, onEnd: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(TolmachColors.Coral, CircleShape)
            .clickable(onClick = onEnd),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.CallEnd,
            contentDescription = s.callEndTooltip,
            tint = TolmachColors.Bg,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun CallOrb(connected: Boolean) {
    val transition = rememberInfiniteTransition(label = "callOrb")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "callPulse",
    )
    val ringAlpha = ((1.4f - pulse) / 0.4f) * 0.4f

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(112.dp)) {
        if (connected) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                        alpha = ringAlpha
                    }
                    .border(2.dp, TolmachColors.Jade, CircleShape),
            )
        }
        Box(
            modifier = Modifier
                .size(92.dp)
                .background(TolmachColors.Surface, CircleShape)
                .border(
                    1.dp,
                    if (connected) TolmachColors.Jade else TolmachColors.Hairline,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = if (connected) TolmachColors.Jade else TolmachColors.TextDim,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}
