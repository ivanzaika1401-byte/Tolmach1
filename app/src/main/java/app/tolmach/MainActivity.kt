package app.tolmach

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import app.tolmach.engine.AudioRoutes
import java.io.File
import app.tolmach.ui.TolmachColors
import app.tolmach.ui.TolmachTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TolmachTheme {
                TolmachApp()
            }
        }
    }
}

@Composable
fun TolmachApp(viewModel: TranslatorViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var showGlossary by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showPhrasebook by remember { mutableStateOf(false) }
    var showCompose by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showPreflight by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) pendingAction?.invoke()
        pendingAction = null
    }

    fun withMicPermission(action: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            action()
        } else {
            pendingAction = action
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(state.error) {
        val message = state.error
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // Системная «Назад» в звонке корректно завершает его, а не роняет приложение.
    BackHandler(enabled = state.callState != "idle") {
        viewModel.closeCallUi()
    }

    // Тактильный отклик в момент установления соединения.
    LaunchedEffect(state.callState) {
        if (state.callState == "connected") {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Готовое китайское аудио — в системный шаринг (WeChat, Telegram, WhatsApp).
    LaunchedEffect(state.shareAudioPath) {
        val path = state.shareAudioPath ?: return@LaunchedEffect
        runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".files",
                File(path),
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/wav"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Отправить голосом"))
        }
        viewModel.clearShareAudio()
    }

    // Во время сеанса перевода экран не гаснет.
    val activity = context as? Activity
    DisposableEffect(state.mode, state.callState) {
        val window = activity?.window
        if (state.mode != Mode.IDLE || state.callState != "idle") {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    Box(modifier = Modifier.fillMaxSize()) {

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0xFF111820),
                        0.35f to TolmachColors.Bg,
                        1.0f to TolmachColors.Bg,
                    ),
                )
                .padding(padding)
                .padding(horizontal = 18.dp),
        ) {
            TopBar(
                hasMessages = state.messages.isNotEmpty(),
                onCompose = { showCompose = true },
                onAbout = { showAbout = true },
                onPreflight = { showPreflight = true },
                onCall = viewModel::openCallMenu,
                onOpenGlossary = { showGlossary = true },
                onOpenSettings = { showSettings = true },
                onShare = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, viewModel.transcriptText())
                    }
                    context.startActivity(
                        Intent.createChooser(intent, "Отправить стенограмму"),
                    )
                },
                onShareFile = {
                    val path = viewModel.exportTranscriptFile()
                    if (path != null) {
                        runCatching {
                            val uri = FileProvider.getUriForFile(
                                context,
                                context.packageName + ".files",
                                File(path),
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(intent, "Стенограмма файлом"),
                            )
                        }
                    }
                },
                onClear = {
                    viewModel.clearConversation()
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "Стенограмма очищена",
                            actionLabel = "Вернуть",
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undoClear()
                        }
                    }
                },
            )
            StatusStrip(
                state = state,
                onRetryModels = viewModel::retryModelDownload,
                onOpenSettings = { showSettings = true },
            )
            RoutesLine(state = state, onOpenSettings = { showSettings = true })
            SessionLine(state)
            ConversationList(
                state = state,
                onReplay = viewModel::replayMessage,
                onShareText = { message ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            message.original + "\n" + message.translated,
                        )
                    }
                    context.startActivity(
                        Intent.createChooser(intent, "Отправить сообщение"),
                    )
                },
                onShareAudio = viewModel::shareMessageAudio,
                modifier = Modifier.weight(1f),
            )
            LiveLine(state, onStopSpeaking = viewModel::stopSpeaking)
            ControlDeck(
                state = state,
                onToggleListen = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    withMicPermission(viewModel::toggleListening)
                },
                onReply = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    withMicPermission(viewModel::startRussianReply)
                },
                onPhrasebook = { showPhrasebook = true },
            )
        }
    }

        AnimatedVisibility(
            visible = state.callState != "idle",
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150)),
        ) {
            CallOverlay(
                state = state,
                errorText = state.error,
                onCreate = { withMicPermission(viewModel::startCallAsCaller) },
                onBeginJoin = viewModel::beginJoin,
                onSubmitJoinCode = { code ->
                    withMicPermission { viewModel.submitJoinCode(code) }
                },
                onSubmitAnswerCode = viewModel::submitAnswerCode,
                onToggleMute = viewModel::toggleCallMute,
                onToggleSpeaker = viewModel::toggleCallSpeaker,
                onOpenPhrases = { showPhrasebook = true },
                onOpenCompose = { showCompose = true },
                onEnd = viewModel::endCall,
                onClose = viewModel::closeCallUi,
            )
        }
    }

    if (showGlossary) {
        GlossaryDialog(
            entries = state.glossary,
            onAdd = viewModel::addGlossaryEntry,
            onRemove = viewModel::removeGlossaryEntry,
            onDismiss = { showGlossary = false },
        )
    }

    if (showSettings) {
        SettingsDialog(
            currentTag = state.chineseLanguageTag,
            onSelectTag = viewModel::setChineseLanguage,
            russianRoute = state.russianRoute,
            chineseRoute = state.chineseRoute,
            onRussianRoute = viewModel::setRussianRoute,
            onChineseRoute = viewModel::setChineseRoute,
            onTestRussian = viewModel::testRussianVoice,
            onTestChinese = viewModel::testChineseVoice,
            useDeepL = state.useDeepL,
            deepLKey = state.deepLKey,
            onUseDeepL = viewModel::setUseDeepL,
            onDeepLKey = viewModel::setDeepLKey,
            chineseRate = state.chineseRate,
            onChineseRate = viewModel::setChineseRate,
            confirmReply = state.confirmReply,
            onConfirmReply = viewModel::setConfirmReply,
            russianVoice = state.russianVoice,
            chineseVoice = state.chineseVoice,
            onRussianVoice = viewModel::setRussianVoice,
            onChineseVoice = viewModel::setChineseVoice,
            ruVoices = state.ruVoices,
            zhVoices = state.zhVoices,
            onDismiss = { showSettings = false },
        )
    }

    if (showPhrasebook) {
        PhrasebookDialog(
            customPhrases = state.customPhrases,
            onPick = { phrase ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                showPhrasebook = false
                viewModel.speakPhrase(phrase)
            },
            onRemoveCustom = viewModel::removeCustomPhrase,
            onAddOwn = {
                showPhrasebook = false
                showCompose = true
            },
            onDismiss = { showPhrasebook = false },
        )
    }

    if (showCompose) {
        ComposeDialog(
            onSpeak = viewModel::composeAndSpeak,
            onSave = viewModel::addCustomPhrase,
            onDismiss = { showCompose = false },
        )
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }

    val pendingReply = state.pendingReply
    if (pendingReply != null) {
        ReplyConfirmDialog(
            initial = pendingReply,
            onConfirm = viewModel::confirmReply,
            onCancel = viewModel::cancelReply,
        )
    }

    if (showPreflight) {
        val micGranted = remember(showPreflight) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
        }
        val volumePercent = remember(showPreflight) { viewModel.mediaVolumePercent() }
        PreflightDialog(
            state = state,
            micGranted = micGranted,
            volumePercent = volumePercent,
            onTestRussian = viewModel::testRussianVoice,
            onTestChinese = viewModel::testChineseVoice,
            onOpenTtsSettings = {
                runCatching {
                    context.startActivity(Intent("com.android.settings.TTS_SETTINGS"))
                }
            },
            onDismiss = { showPreflight = false },
        )
    }
}

// ---------- Шапка с брендом и меню ----------

@Composable
private fun TopBar(
    hasMessages: Boolean,
    onCompose: () -> Unit,
    onAbout: () -> Unit,
    onPreflight: () -> Unit,
    onCall: () -> Unit,
    onOpenGlossary: () -> Unit,
    onOpenSettings: () -> Unit,
    onShare: () -> Unit,
    onShareFile: () -> Unit,
    onClear: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 14.dp, bottom = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Толмач",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Row {
                Text(
                    text = "AGROPLANET",
                    style = MaterialTheme.typography.labelSmall,
                    color = TolmachColors.Gold,
                )
                Text(
                    text = "  ·  переговоры с Китаем",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onCompose) {
            Icon(
                imageVector = Icons.Filled.Keyboard,
                contentDescription = "Написать по-русски",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Меню",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Написать по-русски") },
                    onClick = { menuOpen = false; onCompose() },
                )
                DropdownMenuItem(
                    text = { Text("Проверка перед встречей") },
                    onClick = { menuOpen = false; onPreflight() },
                )
                DropdownMenuItem(
                    text = { Text("Защищённый звонок · бета") },
                    onClick = { menuOpen = false; onCall() },
                )
                DropdownMenuItem(
                    text = { Text("Словарь терминов") },
                    onClick = { menuOpen = false; onOpenGlossary() },
                )
                DropdownMenuItem(
                    text = { Text("Отправить стенограмму") },
                    enabled = hasMessages,
                    onClick = { menuOpen = false; onShare() },
                )
                DropdownMenuItem(
                    text = { Text("Стенограмма файлом (.txt)") },
                    enabled = hasMessages,
                    onClick = { menuOpen = false; onShareFile() },
                )
                DropdownMenuItem(
                    text = { Text("Очистить беседу") },
                    enabled = hasMessages,
                    onClick = { menuOpen = false; onClear() },
                )
                DropdownMenuItem(
                    text = { Text("Настройки") },
                    onClick = { menuOpen = false; onOpenSettings() },
                )
                DropdownMenuItem(
                    text = { Text("О приложении") },
                    onClick = { menuOpen = false; onAbout() },
                )
            }
        }
    }
}

// ---------- Приборная строка статусов ----------

@Composable
private fun StatusStrip(
    state: UiState,
    onRetryModels: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenSettings),
    ) {
        Row(
            modifier = Modifier.height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusCell(
                label = when {
                    state.useDeepL && state.deepLKey.isNotBlank() -> "Перевод: DeepL"
                    state.modelsReady -> "Перевод: готов"
                    state.modelDownloadFailed -> "Перевод: ошибка"
                    else -> "Загрузка…"
                },
                dot = when {
                    state.useDeepL && state.deepLKey.isNotBlank() -> TolmachColors.Gold
                    state.modelsReady -> TolmachColors.Jade
                    state.modelDownloadFailed -> TolmachColors.Coral
                    else -> TolmachColors.TextDim
                },
                pulsing = !state.modelsReady && !state.modelDownloadFailed,
                modifier = Modifier.weight(1f),
            )
            StripDivider()
            StatusCell(
                label = when {
                    state.bluetoothConnected && state.wiredConnected -> "Наушники: 2 пары"
                    state.bluetoothConnected -> "Наушники: BT"
                    state.wiredConnected -> "Наушники: провод"
                    else -> "Наушники: нет"
                },
                dot = if (state.bluetoothConnected || state.wiredConnected) {
                    TolmachColors.Jade
                } else {
                    TolmachColors.TextDim
                },
                modifier = Modifier.weight(1f),
            )
            StripDivider()
            StatusCell(
                label = when (state.chineseLanguageTag) {
                    "yue-Hant-HK" -> "Кантонский"
                    "zh-TW" -> "Тайвань"
                    else -> "Путунхуа"
                },
                dot = TolmachColors.Jade,
                modifier = Modifier.weight(1f),
            )
        }
    }
    AnimatedVisibility(
        visible = state.modelDownloadFailed,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        TextButton(
            onClick = onRetryModels,
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
        ) {
            Text("Повторить загрузку моделей")
        }
    }
}

@Composable
private fun StatusCell(
    label: String,
    dot: Color,
    modifier: Modifier = Modifier,
    pulsing: Boolean = false,
) {
    val dotAlpha = if (pulsing) {
        val transition = rememberInfiniteTransition(label = "dot")
        val value by transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "dotAlpha",
        )
        value
    } else {
        1f
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(dot.copy(alpha = dotAlpha), CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun StripDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .padding(vertical = 10.dp)
            .background(MaterialTheme.colorScheme.outline),
    )
}

private fun routeLabel(route: String): String = when (route) {
    AudioRoutes.BLUETOOTH -> "Bluetooth"
    AudioRoutes.WIRED -> "провод/USB"
    AudioRoutes.SPEAKER -> "динамик"
    else -> "авто"
}

@Composable
private fun RoutesLine(state: UiState, onOpenSettings: () -> Unit) {
    Text(
        text = "Русский → " + routeLabel(state.russianRoute) +
            "   ·   Китайский → " + routeLabel(state.chineseRoute),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier
            .padding(start = 2.dp, top = 8.dp)
            .clickable(onClick = onOpenSettings),
    )
}

@Composable
private fun SessionLine(state: UiState) {
    val start = state.sessionStart
    if (start != null && state.messages.isNotEmpty()) {
        Text(
            text = "Сеанс с $start · реплик: ${state.messages.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp, top = 5.dp),
        )
    } else {
        Spacer(Modifier.height(4.dp))
    }
}

// ---------- Лента переговоров ----------

@Composable
private fun ConversationList(
    state: UiState,
    onReplay: (ChatMessage) -> Unit,
    onShareText: (ChatMessage) -> Unit,
    onShareAudio: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    if (state.messages.isEmpty()) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.GraphicEq,
                contentDescription = null,
                tint = TolmachColors.Jade.copy(alpha = 0.55f),
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Телефон — на стол, микрофоном к собеседнику",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Зелёная кнопка — перевод партнёра тихо придёт вам " +
                    "в наушники. «Ответить» — ваша русская фраза прозвучит " +
                    "по-китайски. «Фразы» — готовые формулировки Agroplanet " +
                    "в одно касание.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 26.dp),
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 10.dp),
        ) {
            items(state.messages, key = { it.id }) { message ->
                val isNewest = message.id == state.messages.lastOrNull()?.id
                if (isNewest) {
                    val appear = remember {
                        MutableTransitionState(false).apply { targetState = true }
                    }
                    AnimatedVisibility(
                        visibleState = appear,
                        enter = fadeIn(tween(220)) +
                            slideInVertically(initialOffsetY = { it / 3 }),
                    ) {
                        MessageBubble(
                            message = message,
                            onReplay = { onReplay(message) },
                            onShareText = { onShareText(message) },
                            onShareAudio = { onShareAudio(message) },
                        )
                    }
                } else {
                    MessageBubble(
                            message = message,
                            onReplay = { onReplay(message) },
                            onShareText = { onShareText(message) },
                            onShareAudio = { onShareAudio(message) },
                        )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onReplay: () -> Unit,
    onShareText: () -> Unit,
    onShareAudio: () -> Unit,
) {
    val fromPartner = message.fromChinese
    val alignment = if (fromPartner) Alignment.CenterStart else Alignment.CenterEnd
    val container = if (fromPartner) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val borderColor = if (fromPartner) {
        MaterialTheme.colorScheme.outline
    } else {
        TolmachColors.Jade.copy(alpha = 0.35f)
    }
    val shape = if (fromPartner) {
        RoundedCornerShape(topStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 6.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
    }
    val primaryText = if (fromPartner) message.translated else message.original
    val secondaryText = if (fromPartner) message.original else message.translated

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            shape = shape,
            color = container,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .widthIn(max = 330.dp)
                .clickable {
                    clipboard.setText(AnnotatedString(primaryText + "\n" + secondaryText))
                    Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
                },
        ) {
            Column(Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = (if (fromPartner) "Партнёр" else "Вы").uppercase() +
                            "  ·  " + message.time,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (fromPartner) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            TolmachColors.Jade
                        },
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onReplay, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = "Озвучить ещё раз",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Box {
                        var shareMenu by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { shareMenu = true },
                            modifier = Modifier.size(30.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Отправить",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = shareMenu,
                            onDismissRequest = { shareMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Текстом") },
                                onClick = { shareMenu = false; onShareText() },
                            )
                            if (!fromPartner) {
                                DropdownMenuItem(
                                    text = { Text("Голосом · аудио по-китайски") },
                                    onClick = { shareMenu = false; onShareAudio() },
                                )
                            }
                        }
                    }
                }
                Text(
                    text = primaryText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(end = 6.dp),
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        }
    }
}

// ---------- Живая строка распознавания ----------

@Composable
private fun LiveLine(state: UiState, onStopSpeaking: () -> Unit) {
    val text = when {
        state.speaking -> "Озвучиваю перевод…"
        state.listening && state.partial.isNotBlank() -> state.partial
        state.listening && state.mode == Mode.LISTEN_CHINESE -> "Слушаю собеседника…"
        state.listening && state.mode == Mode.REPLY_RUSSIAN -> "Говорите по-русски…"
        else -> null
    }
    var lastText by remember { mutableStateOf("") }
    if (text != null) lastText = text

    AnimatedVisibility(
        visible = text != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EqualizerBars(level = state.micLevel, color = TolmachColors.Jade)
                Spacer(Modifier.width(9.dp))
                Text(
                    text = lastText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    modifier = Modifier.weight(1f),
                )
                if (state.speaking) {
                    IconButton(onClick = onStopSpeaking, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = "Остановить озвучку",
                            tint = TolmachColors.Coral,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Живой индикатор: полоски дышат от реального уровня микрофона. */
@Composable
private fun EqualizerBars(level: Float, color: Color) {
    val smooth by animateFloatAsState(
        targetValue = level,
        animationSpec = tween(90),
        label = "micLevel",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(18.dp),
    ) {
        listOf(0.65f, 1f, 0.8f).forEach { factor ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 1.5.dp)
                    .width(3.dp)
                    .height((4f + smooth * 14f * factor).dp)
                    .background(color, RoundedCornerShape(2.dp)),
            )
        }
    }
}

// ---------- Пульт управления ----------

@Composable
private fun ControlDeck(
    state: UiState,
    onToggleListen: () -> Unit,
    onReply: () -> Unit,
    onPhrasebook: () -> Unit,
) {
    val listening = state.mode == Mode.LISTEN_CHINESE
    val replying = state.mode == Mode.REPLY_RUSSIAN

    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DeckSideButton(
                icon = Icons.Filled.Bolt,
                label = "Фразы",
                active = false,
                accent = TolmachColors.Gold,
                modifier = Modifier.weight(1f),
                onClick = onPhrasebook,
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1.4f),
            ) {
                ListenOrb(listening = listening, onClick = onToggleListen)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (listening) "Остановить" else "Слушать китайский",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (listening) {
                        TolmachColors.Coral
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
            }
            DeckSideButton(
                icon = Icons.Filled.Mic,
                label = "Ответить",
                active = replying,
                accent = TolmachColors.Jade,
                modifier = Modifier.weight(1f),
                onClick = onReply,
            )
        }
    }
}

@Composable
private fun ListenOrb(listening: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "orb")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse",
    )
    val ringAlpha = ((1.45f - pulse) / 0.45f) * 0.45f

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(104.dp)) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .border(1.dp, TolmachColors.Jade.copy(alpha = 0.14f), CircleShape),
        )
        if (listening) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                        alpha = ringAlpha
                    }
                    .border(2.dp, TolmachColors.Jade, CircleShape),
            )
        }
        val background = if (listening) {
            Brush.verticalGradient(
                listOf(TolmachColors.JadeBright, TolmachColors.Jade),
            )
        } else {
            Brush.verticalGradient(
                listOf(TolmachColors.SurfaceHigh, TolmachColors.Surface),
            )
        }
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(background)
                .border(
                    width = 1.dp,
                    color = if (listening) TolmachColors.JadeBright else TolmachColors.Jade.copy(alpha = 0.6f),
                    shape = CircleShape,
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(targetState = listening, label = "orbIcon") { active ->
                Icon(
                    imageVector = if (active) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = if (active) "Остановить" else "Слушать китайский",
                    tint = if (active) TolmachColors.JadeDeep else TolmachColors.Jade,
                    modifier = Modifier.size(34.dp),
                )
            }
        }
    }
}

@Composable
private fun DeckSideButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(
                    if (active) accent else MaterialTheme.colorScheme.surface,
                )
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) TolmachColors.JadeDeep else accent,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

// ---------- Разговорник Agroplanet ----------

@Composable
private fun PhrasebookDialog(
    customPhrases: List<Phrase>,
    onPick: (Phrase) -> Unit,
    onRemoveCustom: (Phrase) -> Unit,
    onAddOwn: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Быстрые фразы") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "Нажмите фразу — телефон сразу произнесёт её " +
                        "по-китайски и добавит в ленту переговоров.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (customPhrases.isNotEmpty()) {
                    Text(
                        text = "МОИ ФРАЗЫ",
                        style = MaterialTheme.typography.labelSmall,
                        color = TolmachColors.Gold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                    customPhrases.forEach { phrase ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onPick(phrase) }
                                    .padding(vertical = 8.dp),
                            ) {
                                Text(
                                    text = phrase.russian,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = phrase.chinese,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { onRemoveCustom(phrase) }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Удалить",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                }
                BuiltInPhrases.groupBy { it.category }.forEach { (category, phrases) ->
                    Text(
                        text = category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TolmachColors.Gold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                    phrases.forEach { phrase ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(phrase) }
                                .padding(vertical = 8.dp),
                        ) {
                            Text(
                                text = phrase.russian,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = phrase.chinese,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        },
        dismissButton = {
            TextButton(onClick = onAddOwn) { Text("Добавить свою") }
        },
    )
}

// ---------- Диалоги ----------

@Composable
private fun GlossaryDialog(
    entries: List<GlossaryEntry>,
    onAdd: (String, String) -> Unit,
    onRemove: (GlossaryEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Словарь терминов") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Замены применяются к готовому переводу — сорта, " +
                        "ГОСТы, базисы поставки. Пример: «инкотермс» → «Incoterms».",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                entries.forEach { entry ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "${entry.from} → ${entry.to}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onRemove(entry) }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = from,
                    onValueChange = { from = it },
                    label = { Text("Как переводит сейчас") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = to,
                    onValueChange = { to = it },
                    label = { Text("Как должно быть") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (from.isNotBlank() && to.isNotBlank()) {
                        onAdd(from.trim(), to.trim())
                        from = ""
                        to = ""
                    }
                },
            ) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Готово") }
        },
    )
}

@Composable
private fun SettingsDialog(
    currentTag: String,
    onSelectTag: (String) -> Unit,
    russianRoute: String,
    chineseRoute: String,
    onRussianRoute: (String) -> Unit,
    onChineseRoute: (String) -> Unit,
    onTestRussian: () -> Unit,
    onTestChinese: () -> Unit,
    useDeepL: Boolean,
    deepLKey: String,
    onUseDeepL: (Boolean) -> Unit,
    onDeepLKey: (String) -> Unit,
    chineseRate: Float,
    onChineseRate: (Float) -> Unit,
    confirmReply: Boolean,
    onConfirmReply: (Boolean) -> Unit,
    russianVoice: String,
    chineseVoice: String,
    onRussianVoice: (String) -> Unit,
    onChineseVoice: (String) -> Unit,
    ruVoices: List<Pair<String, String>>,
    zhVoices: List<Pair<String, String>>,
    onDismiss: () -> Unit,
) {
    val languages = listOf(
        "zh-CN" to "Путунхуа — стандартный китайский (рекомендуется)",
        "zh-TW" to "Тайваньский мандарин",
        "yue-Hant-HK" to "Кантонский — Гонконг, Гуандун (экспериментально)",
    )
    val russianRoutes = listOf(
        AudioRoutes.SYSTEM to "Авто — как решит телефон",
        AudioRoutes.BLUETOOTH to "Bluetooth-наушники",
        AudioRoutes.WIRED to "Проводные / USB-наушники",
        AudioRoutes.SPEAKER to "Динамик телефона",
    )
    val chineseRoutes = listOf(
        AudioRoutes.SPEAKER to "Динамик телефона (рекомендуется)",
        AudioRoutes.WIRED to "Проводные / USB — вторая пара",
        AudioRoutes.BLUETOOTH to "Bluetooth-наушники",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Настройки") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                SectionHeader("Быстрые схемы звука")
                PresetRow(
                    title = "Динамик для собеседника",
                    subtitle = "Русский — в ваши наушники, китайский — из динамика телефона.",
                    onClick = {
                        onRussianRoute(AudioRoutes.SYSTEM)
                        onChineseRoute(AudioRoutes.SPEAKER)
                    },
                )
                PresetRow(
                    title = "Две пары наушников",
                    subtitle = "Русский — в Bluetooth, китайский — в проводные/USB.",
                    onClick = {
                        onRussianRoute(AudioRoutes.BLUETOOTH)
                        onChineseRoute(AudioRoutes.WIRED)
                    },
                )
                PresetRow(
                    title = "Дистанционный звонок",
                    subtitle = "Созвон идёт на втором устройстве на громкой связи рядом: " +
                        "русский — вам, китайский — из динамика в его микрофон.",
                    onClick = {
                        onRussianRoute(AudioRoutes.SYSTEM)
                        onChineseRoute(AudioRoutes.SPEAKER)
                    },
                )
                PresetRow(
                    title = "Наушник у собеседника",
                    subtitle = "Китайский — в Bluetooth собеседнику, русский — из динамика вам.",
                    onClick = {
                        onRussianRoute(AudioRoutes.SPEAKER)
                        onChineseRoute(AudioRoutes.BLUETOOTH)
                    },
                )

                SectionHeader("Язык собеседника")
                languages.forEach { (tag, label) ->
                    RouteOption(
                        label = label,
                        selected = currentTag == tag,
                        onClick = { onSelectTag(tag) },
                    )
                }

                SectionHeader("Русский перевод — куда звучит")
                russianRoutes.forEach { (route, label) ->
                    RouteOption(
                        label = label,
                        selected = russianRoute == route,
                        onClick = { onRussianRoute(route) },
                    )
                }
                TextButton(
                    onClick = onTestRussian,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                ) { Text("Проверить русский канал") }

                SectionHeader("Китайский перевод — куда звучит")
                chineseRoutes.forEach { (route, label) ->
                    RouteOption(
                        label = label,
                        selected = chineseRoute == route,
                        onClick = { onChineseRoute(route) },
                    )
                }
                TextButton(
                    onClick = onTestChinese,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                ) { Text("Проверить китайский канал") }

                SectionHeader("Движок перевода")
                RouteOption(
                    label = "Офлайн — ML Kit, работает без интернета",
                    selected = !useDeepL,
                    onClick = { onUseDeepL(false) },
                )
                RouteOption(
                    label = "DeepL — максимум качества (интернет + ключ)",
                    selected = useDeepL,
                    onClick = { onUseDeepL(true) },
                )
                if (useDeepL) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = deepLKey,
                        onValueChange = onDeepLKey,
                        label = { Text("Ключ DeepL API") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Бесплатный ключ: deepl.com → тариф «DeepL API Free», " +
                            "500 000 знаков в месяц. Без интернета или при любой " +
                            "ошибке приложение мгновенно переключится на офлайн-перевод.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                SectionHeader("Скорость китайской озвучки")
                Text(
                    text = "%.2f".format(chineseRate) +
                        "× — медленнее звучит разборчивее для собеседника",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = chineseRate,
                    onValueChange = onChineseRate,
                    valueRange = 0.7f..1.2f,
                    modifier = Modifier.fillMaxWidth(),
                )

                SectionHeader("Ответ по-русски")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Проверять фразу перед озвучкой",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = "покажет распознанный текст — можно поправить " +
                                "до того, как собеседник услышит перевод",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = confirmReply, onCheckedChange = onConfirmReply)
                }

                SectionHeader("Голос русской озвучки")
                if (ruVoices.isEmpty()) {
                    Text(
                        text = "Голоса появятся после инициализации синтеза",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    RouteOption(
                        label = "Системный по умолчанию",
                        selected = russianVoice.isBlank(),
                        onClick = { onRussianVoice("") },
                    )
                    ruVoices.forEach { (name, label) ->
                        RouteOption(
                            label = label,
                            selected = russianVoice == name,
                            onClick = { onRussianVoice(name) },
                        )
                    }
                }

                SectionHeader("Голос китайской озвучки")
                if (zhVoices.isEmpty()) {
                    Text(
                        text = "Голоса появятся после инициализации синтеза",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    RouteOption(
                        label = "Системный по умолчанию",
                        selected = chineseVoice.isBlank(),
                        onClick = { onChineseVoice("") },
                    )
                    zhVoices.forEach { (name, label) ->
                        RouteOption(
                            label = label,
                            selected = chineseVoice == name,
                            onClick = { onChineseVoice(name) },
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Схема «две пары наушников»: русская сторона — Bluetooth, " +
                        "китайская — проводные или USB-C наушники. Два Bluetooth " +
                        "с разным звуком Android не поддерживает — это ограничение " +
                        "системы на любых телефонах. Вторую пару берите без " +
                        "микрофона, иначе телефон начнёт слушать её микрофон " +
                        "вместо своего. Если выбранных наушников нет, звук мягко " +
                        "уйдёт на динамик (китайский) или системный маршрут (русский).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Готово") }
        },
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = TolmachColors.Gold,
        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
    )
}

@Composable
private fun RouteOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun ComposeDialog(
    onSpeak: (String) -> Unit,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Написать по-русски") },
        text = {
            Column {
                Text(
                    text = "Телефон переведёт фразу и произнесёт её по-китайски. " +
                        "Удобно в шумном зале и для точных формулировок. " +
                        "«В разговорник» — сохранит фразу с переводом в «Мои фразы».",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Ваша фраза") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = {
                    onSpeak(text)
                    onDismiss()
                },
            ) { Text("Перевести и озвучить") }
        },
        dismissButton = {
            Row {
                TextButton(
                    enabled = text.isNotBlank(),
                    onClick = {
                        onSave(text)
                        onDismiss()
                    },
                ) { Text("В разговорник") }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        },
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: ""
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Толмач $version") },
        text = {
            Column {
                Text(
                    text = "Переводчик переговоров для ТОО «AGROPLANET», Костанай.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Перевод и лента работают офлайн. Русский канал — " +
                        "наушники вашей стороны, китайский — динамик или вторая " +
                        "пара. Юридически значимые условия контракта " +
                        "перепроверяйте с живым переводчиком.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Готово") }
        },
    )
}

@Composable
private fun PresetRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun PreflightDialog(
    state: UiState,
    micGranted: Boolean,
    volumePercent: Int,
    onTestRussian: () -> Unit,
    onTestChinese: () -> Unit,
    onOpenTtsSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val deepLActive = state.useDeepL && state.deepLKey.isNotBlank()
    val translationOk = state.modelsReady || deepLActive
    val headphonesOk = state.bluetoothConnected || state.wiredConnected

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Проверка перед встречей") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                CheckRow(
                    label = "Микрофон разрешён",
                    ok = micGranted,
                    detail = if (micGranted) {
                        null
                    } else {
                        "Android спросит разрешение при первом нажатии записи"
                    },
                )
                CheckRow(
                    label = "Перевод готов",
                    ok = translationOk,
                    detail = when {
                        deepLActive -> "движок DeepL, офлайн — как страховка"
                        state.modelsReady -> "офлайн-модели на месте"
                        else -> "модели ещё не скачаны — нужен интернет"
                    },
                )
                CheckRow(
                    label = "Русский голос установлен",
                    ok = state.voiceRussianOk,
                    detail = if (state.voiceRussianOk) null else "докачайте в настройках синтеза речи",
                )
                CheckRow(
                    label = "Китайский голос установлен",
                    ok = state.voiceChineseOk,
                    detail = if (state.voiceChineseOk) null else "докачайте в настройках синтеза речи",
                )
                if (!state.voiceRussianOk || !state.voiceChineseOk) {
                    TextButton(
                        onClick = onOpenTtsSettings,
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                    ) { Text("Открыть настройки синтеза") }
                }
                CheckRow(
                    label = "Наушники",
                    ok = headphonesOk,
                    detail = when {
                        state.bluetoothConnected && state.wiredConnected -> "две пары: Bluetooth и провод/USB"
                        state.bluetoothConnected -> "Bluetooth подключены"
                        state.wiredConnected -> "проводные/USB подключены"
                        else -> "нет — русский перевод пойдёт в динамик"
                    },
                )
                CheckRow(
                    label = "Громкость медиа",
                    ok = volumePercent >= 25,
                    detail = "$volumePercent%" +
                        if (volumePercent < 25) " — прибавьте боковыми кнопками" else "",
                )
                Spacer(Modifier.height(6.dp))
                Row {
                    TextButton(
                        onClick = onTestRussian,
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                    ) { Text("Тест русского") }
                    Spacer(Modifier.width(10.dp))
                    TextButton(
                        onClick = onTestChinese,
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                    ) { Text("Тест китайского") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Готово") }
        },
    )
}

@Composable
private fun CheckRow(label: String, ok: Boolean, detail: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    if (ok) TolmachColors.Jade else TolmachColors.Coral,
                    CircleShape,
                ),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReplyConfirmDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var text by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Проверьте фразу") },
        text = {
            Column {
                Text(
                    text = "Так вас услышал телефон. Поправьте при необходимости — " +
                        "и собеседник получит точный перевод.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = { onConfirm(text) },
            ) { Text("Озвучить по-китайски") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Отмена") }
        },
    )
}
