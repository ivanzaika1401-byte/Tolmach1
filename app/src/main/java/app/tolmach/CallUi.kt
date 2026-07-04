package app.tolmach

import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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

/** Полноэкранный слой защищённого P2P-звонка. */
@Composable
fun CallOverlay(
    state: UiState,
    errorText: String?,
    onCreate: () -> Unit,
    onBeginJoin: () -> Unit,
    onSubmitJoinCode: (String) -> Unit,
    onSubmitAnswerCode: (String) -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onOpenPhrases: () -> Unit,
    onOpenCompose: () -> Unit,
    onEnd: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current

    fun sendCode(code: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Код для защищённого звонка «Толмач»:\n$code",
            )
        }
        context.startActivity(Intent.createChooser(intent, "Отправить код"))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TolmachColors.Bg.copy(alpha = 0.985f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(30.dp))
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
                    text = "Сквозное шифрование DTLS-SRTP",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = callStatusTitle(state),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            if (errorText != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TolmachColors.Coral,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(18.dp))

            when (state.callState) {
                "menu" -> CallMenu(onCreate, onBeginJoin, onClose)

                "preparing", "preparing_answer" -> Text(
                    text = "Готовлю код соединения…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                "wait_answer" -> CodeExchange(
                    myCode = state.callLocalCode,
                    inputLabel = "Вставьте код-ответ собеседника",
                    submitLabel = "Соединить",
                    hint = "Шаг 1: отправьте свой код собеседнику. " +
                        "Шаг 2: вставьте его код-ответ и нажмите «Соединить».",
                    onSend = { sendCode(state.callLocalCode) },
                    onSubmit = onSubmitAnswerCode,
                )

                "join_input" -> JoinInput(onSubmitJoinCode)

                "answer_ready" -> CodeExchange(
                    myCode = state.callLocalCode,
                    inputLabel = null,
                    submitLabel = null,
                    hint = "Отправьте этот код-ответ звонящему — соединение " +
                        "установится автоматически.",
                    onSend = { sendCode(state.callLocalCode) },
                    onSubmit = null,
                )

                "connecting" -> Text(
                    text = "Устанавливаю прямое соединение…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                "connected" -> ConnectedControls(
                    state = state,
                    onToggleMute = onToggleMute,
                    onToggleSpeaker = onToggleSpeaker,
                    onOpenPhrases = onOpenPhrases,
                    onOpenCompose = onOpenCompose,
                )

                "failed" -> Text(
                    text = "Соединение не удалось. Прямые P2P-звонки блокируются " +
                        "некоторыми сетями и фаерволом КНР — попробуйте другой " +
                        "интернет или схему «второе устройство».",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(22.dp))
            if (state.callState == "connected") {
                EndCallButton(onEnd)
            } else if (state.callState != "menu") {
                TextButton(onClick = onClose) { Text("Отменить") }
            }
            Spacer(Modifier.height(26.dp))
        }
    }
}

private fun callStatusTitle(state: UiState): String = when (state.callState) {
    "menu" -> "Защищённый звонок"
    "connected" -> formatCallTime(state.callSeconds)
    "failed" -> "Не удалось соединиться"
    else -> "Подготовка звонка"
}

private fun formatCallTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
private fun CallMenu(
    onCreate: () -> Unit,
    onBeginJoin: () -> Unit,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        CallChoice(
            title = "Создать звонок",
            subtitle = "Получите код и отправьте его собеседнику любым мессенджером.",
            onClick = onCreate,
        )
        Spacer(Modifier.height(10.dp))
        CallChoice(
            title = "Присоединиться",
            subtitle = "Вставьте код, который прислал звонящий.",
            onClick = onBeginJoin,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Звук идёт напрямую телефон-телефон, без серверов, всегда " +
                "зашифрован. В звонке работают «Фразы» и «Написать» — китайская " +
                "озвучка уходит собеседнику. Честно: сети КНР блокируют прямые " +
                "P2P-соединения — с материковым Китаем нужен VPN у партнёра; " +
                "синхронный перевод его речи в звонке на одном телефоне " +
                "невозможен (микрофон занят звонком) — для этого схема " +
                "«второе устройство».",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onClose) { Text("Закрыть") }
    }
}

@Composable
private fun CallChoice(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline,
        ),
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
private fun JoinInput(onSubmit: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text("Код приглашения") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        val clipboard = LocalClipboardManager.current
        Row {
            TextButton(
                onClick = { clipboard.getText()?.text?.let { code = it } },
            ) { Text("Вставить из буфера") }
            TextButton(
                enabled = code.isNotBlank(),
                onClick = { onSubmit(code) },
            ) { Text("Продолжить") }
        }
    }
}

@Composable
private fun CodeExchange(
    myCode: String,
    inputLabel: String?,
    submitLabel: String?,
    hint: String,
    onSend: () -> Unit,
    onSubmit: ((String) -> Unit)?,
) {
    var answer by remember { mutableStateOf("") }
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
        val clipboard = LocalClipboardManager.current
        Row {
            TextButton(onClick = onSend) { Text("Отправить код") }
            TextButton(
                onClick = { clipboard.setText(AnnotatedString(myCode)) },
            ) { Text("Копировать") }
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
                ) { Text("Вставить из буфера") }
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
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onOpenPhrases: () -> Unit,
    onOpenCompose: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Говорите — связь прямая и зашифрованная. «Фразы» и «Написать» " +
                "озвучат китайский прямо в звонок.",
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
                label = if (state.callMuted) "Вкл. микр." else "Микрофон",
                active = !state.callMuted,
                onClick = onToggleMute,
            )
            CallRoundButton(
                icon = Icons.Filled.Bolt,
                label = "Фразы",
                active = false,
                accentGold = true,
                onClick = onOpenPhrases,
            )
            CallRoundButton(
                icon = Icons.Filled.Keyboard,
                label = "Написать",
                active = false,
                onClick = onOpenCompose,
            )
            CallRoundButton(
                icon = Icons.Filled.VolumeUp,
                label = if (state.callSpeakerOn) "Динамик" else "У уха",
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
private fun EndCallButton(onEnd: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(TolmachColors.Coral, CircleShape)
            .clickable(onClick = onEnd),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.CallEnd,
            contentDescription = "Завершить",
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

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
        if (connected) {
            Box(
                modifier = Modifier
                    .size(96.dp)
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
                .size(96.dp)
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
