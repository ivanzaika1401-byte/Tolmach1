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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import app.tolmach.engine.AudioRoutes
import app.tolmach.ui.TolmachColors
import app.tolmach.ui.TolmachTheme
import java.io.File
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            TolmachTheme {
                TolmachApp()
            }
        }
    }
}

private const val TAB_TRANSLATOR = 0
private const val TAB_PHRASES = 1
private const val TAB_CALL = 2
private const val TAB_SETTINGS = 3

@Composable
fun TolmachApp(viewModel: TranslatorViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val s = strings(state.appLanguage)
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(TAB_TRANSLATOR) }
    var showGlossary by remember { mutableStateOf(false) }
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

    BackHandler(enabled = state.callState != "idle") {
        viewModel.endCall()
    }

    LaunchedEffect(state.callState) {
        if (state.callState == "connected") {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

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
            context.startActivity(Intent.createChooser(intent, s.chooserVoice))
        }
        viewModel.clearShareAudio()
    }

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

    val entrance = remember {
        MutableTransitionState(false).apply { targetState = true }
    }
    AnimatedVisibility(
        visibleState = entrance,
        enter = fadeIn(tween(400)) + slideInVertically(initialOffsetY = { it / 24 }),
    ) {

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AppNavBar(
                s = s,
                selected = selectedTab,
                callActive = state.callState == "connected",
                onSelect = { selectedTab = it },
            )
        },
    ) { padding ->
        Box(
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
            Crossfade(targetState = selectedTab, label = "tabs") { tab ->
                when (tab) {
                TAB_TRANSLATOR -> TranslatorScreen(
                    state = state,
                    s = s,
                    onToggleListen = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        withMicPermission(viewModel::toggleListening)
                    },
                    onReply = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        withMicPermission(viewModel::startRussianReply)
                    },
                    onOpenPhrases = { selectedTab = TAB_PHRASES },
                    onOpenSettings = { selectedTab = TAB_SETTINGS },
                    onRetryModels = viewModel::retryModelDownload,
                    onStopSpeaking = viewModel::stopSpeaking,
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
                            Intent.createChooser(intent, s.chooserMessage),
                        )
                    },
                    onShareAudio = viewModel::shareMessageAudio,
                    onCompose = { showCompose = true },
                    onShare = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, viewModel.transcriptText())
                        }
                        context.startActivity(
                            Intent.createChooser(intent, s.chooserTranscript),
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
                                    Intent.createChooser(intent, s.chooserTranscriptFile),
                                )
                            }
                        }
                    },
                    onClear = {
                        viewModel.clearConversation()
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = s.clearedSnack,
                                actionLabel = s.undoAction,
                                duration = SnackbarDuration.Short,
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.undoClear()
                            }
                        }
                    },
                )

                TAB_PHRASES -> PhrasesScreen(
                    state = state,
                    onPick = { phrase ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.speakPhrase(phrase)
                        selectedTab = if (state.callState == "connected") {
                            TAB_CALL
                        } else {
                            TAB_TRANSLATOR
                        }
                    },
                    onRemoveCustom = viewModel::removeCustomPhrase,
                    onAddOwn = { showCompose = true },
                )

                TAB_CALL -> CallScreen(
                    state = state,
                    onCreate = { withMicPermission(viewModel::startCallAsCaller) },
                    onBeginJoin = viewModel::beginJoin,
                    onSubmitJoinCode = { code ->
                        withMicPermission { viewModel.submitJoinCode(code) }
                    },
                    onSubmitAnswerCode = viewModel::submitAnswerCode,
                    onToggleMute = viewModel::toggleCallMute,
                    onToggleSpeaker = viewModel::toggleCallSpeaker,
                    onOpenPhrases = { selectedTab = TAB_PHRASES },
                    onOpenCompose = { showCompose = true },
                    onEnd = viewModel::endCall,
                )

                TAB_SETTINGS -> SettingsScreen(
                    state = state,
                    viewModel = viewModel,
                    onOpenGlossary = { showGlossary = true },
                    onOpenPreflight = { showPreflight = true },
                    onOpenAbout = { showAbout = true },
                )
                }
            }
        }
    }
    }

    if (state.showLanguagePicker) {
        LanguagePickerDialog(onPick = viewModel::setAppLanguage)
    }

    if (state.showGuide && !state.showLanguagePicker) {
        GuideDialog(s = s, onDismiss = viewModel::dismissGuide)
    }

    if (showGlossary) {
        GlossaryDialog(
            s = s,
            entries = state.glossary,
            onAdd = viewModel::addGlossaryEntry,
            onRemove = viewModel::removeGlossaryEntry,
            onDismiss = { showGlossary = false },
        )
    }

    if (showCompose) {
        ComposeDialog(
            s = s,
            onSpeak = viewModel::composeAndSpeak,
            onSave = viewModel::addCustomPhrase,
            onDismiss = { showCompose = false },
        )
    }

    if (showAbout) {
        AboutDialog(s = s, onDismiss = { showAbout = false })
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
            s = s,
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

    val pendingReply = state.pendingReply
    if (pendingReply != null) {
        ReplyConfirmDialog(
            s = s,
            initial = pendingReply,
            onConfirm = viewModel::confirmReply,
            onCancel = viewModel::cancelReply,
        )
    }
}

// ---------- Выбор языка при первом входе ----------

@Composable
private fun LanguagePickerDialog(onPick: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Язык интерфейса · 界面语言") },
        text = {
            Column {
                LanguageChoice(
                    title = RuStrings.languageRu,
                    subtitle = RuStrings.pickerRuSub,
                    onClick = { onPick("ru") },
                )
                Spacer(Modifier.height(10.dp))
                LanguageChoice(
                    title = RuStrings.languageZh,
                    subtitle = ZhStrings.pickerZhSub,
                    onClick = { onPick("zh") },
                )
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun LanguageChoice(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
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
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------- Гид ----------

@Composable
private fun GuideDialog(s: AppStrings, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(s.guideTitle) },
        text = {
            Column {
                GuideRow(
                    icon = Icons.Filled.GraphicEq,
                    title = s.guideListenTitle,
                    text = s.guideListenText,
                )
                Spacer(Modifier.height(12.dp))
                GuideRow(
                    icon = Icons.Filled.Mic,
                    title = s.guideReplyTitle,
                    text = s.guideReplyText,
                )
                Spacer(Modifier.height(12.dp))
                GuideRow(
                    icon = Icons.Filled.Bolt,
                    title = s.guideMoreTitle,
                    text = s.guideMoreText,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = s.guideFootnote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(s.gotIt) }
        },
    )
}

@Composable
private fun GuideRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    text: String,
) {
    Row {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TolmachColors.Jade,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------- Нижняя навигация ----------

@Composable
private fun AppNavBar(
    s: AppStrings,
    selected: Int,
    callActive: Boolean,
    onSelect: (Int) -> Unit,
) {
    val items = listOf(
        Triple(TAB_TRANSLATOR, s.tabTranslator, Icons.Filled.Translate),
        Triple(TAB_PHRASES, s.tabPhrases, Icons.Filled.Bolt),
        Triple(TAB_CALL, s.tabCall, Icons.Filled.Call),
        Triple(TAB_SETTINGS, s.tabSettings, Icons.Filled.Settings),
    )
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            items.forEach { (index, label, icon) ->
                NavigationBarItem(
                    selected = selected == index,
                    onClick = { onSelect(index) },
                    icon = {
                        val tint = when {
                            index == TAB_CALL && callActive -> TolmachColors.Gold
                            else -> null
                        }
                        if (tint != null) {
                            Icon(imageVector = icon, contentDescription = label, tint = tint)
                        } else {
                            Icon(imageVector = icon, contentDescription = label)
                        }
                    },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TolmachColors.Jade,
                        selectedTextColor = TolmachColors.Jade,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

// ---------- Вкладка «Перевод» ----------

@Composable
private fun TranslatorScreen(
    state: UiState,
    s: AppStrings,
    onToggleListen: () -> Unit,
    onReply: () -> Unit,
    onOpenPhrases: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetryModels: () -> Unit,
    onStopSpeaking: () -> Unit,
    onReplay: (ChatMessage) -> Unit,
    onShareText: (ChatMessage) -> Unit,
    onShareAudio: (ChatMessage) -> Unit,
    onCompose: () -> Unit,
    onShare: () -> Unit,
    onShareFile: () -> Unit,
    onClear: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            s = s,
            hasMessages = state.messages.isNotEmpty(),
            onCompose = onCompose,
            onShare = onShare,
            onShareFile = onShareFile,
            onClear = onClear,
        )
        StatusStrip(
            state = state,
            s = s,
            onRetryModels = onRetryModels,
            onOpenSettings = onOpenSettings,
        )
        RoutesLine(state = state, s = s, onOpenSettings = onOpenSettings)
        SessionLine(state = state, s = s)
        ConversationList(
            state = state,
            s = s,
            onReplay = onReplay,
            onShareText = onShareText,
            onShareAudio = onShareAudio,
            operatorZh = state.appLanguage == "zh",
            modifier = Modifier.weight(1f),
        )
        LiveLine(state = state, s = s, onStopSpeaking = onStopSpeaking)
        ControlDeck(
            state = state,
            s = s,
            onToggleListen = onToggleListen,
            onReply = onReply,
            onPhrasebook = onOpenPhrases,
        )
    }
}

@Composable
private fun TopBar(
    s: AppStrings,
    hasMessages: Boolean,
    onCompose: () -> Unit,
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
                    text = "  ·  " + s.brandTagline,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onCompose) {
            Icon(
                imageVector = Icons.Filled.Keyboard,
                contentDescription = s.composeTooltip,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = s.menuTooltip,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(s.menuCompose) },
                    onClick = { menuOpen = false; onCompose() },
                )
                DropdownMenuItem(
                    text = { Text(s.menuShareTranscript) },
                    enabled = hasMessages,
                    onClick = { menuOpen = false; onShare() },
                )
                DropdownMenuItem(
                    text = { Text(s.menuShareFile) },
                    enabled = hasMessages,
                    onClick = { menuOpen = false; onShareFile() },
                )
                DropdownMenuItem(
                    text = { Text(s.menuClear) },
                    enabled = hasMessages,
                    onClick = { menuOpen = false; onClear() },
                )
            }
        }
    }
}

// ---------- Приборная строка ----------

@Composable
private fun StatusStrip(
    state: UiState,
    s: AppStrings,
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
                    state.useDeepL && state.deepLKey.isNotBlank() -> s.translationDeepL
                    state.modelsReady -> s.translationReady
                    state.modelDownloadFailed -> s.translationError
                    else -> s.translationLoading
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
                    state.bluetoothConnected && state.wiredConnected -> s.headphonesTwo
                    state.bluetoothConnected -> s.headphonesBt
                    state.wiredConnected -> s.headphonesWired
                    else -> s.headphonesNone
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
                    "yue-Hant-HK" -> s.dialectCantonese
                    "zh-TW" -> s.dialectTaiwan
                    else -> s.dialectMandarin
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
            Text(s.retryModels)
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

private fun routeLabel(s: AppStrings, route: String): String = when (route) {
    AudioRoutes.BLUETOOTH -> s.routeBluetooth
    AudioRoutes.WIRED -> s.routeWired
    AudioRoutes.SPEAKER -> s.routeSpeaker
    else -> s.routeAuto
}

@Composable
private fun RoutesLine(state: UiState, s: AppStrings, onOpenSettings: () -> Unit) {
    Text(
        text = s.routeRussianTo + routeLabel(s, state.russianRoute) +
            "   ·   " + s.routeChineseTo + routeLabel(s, state.chineseRoute),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier
            .padding(start = 2.dp, top = 8.dp)
            .clickable(onClick = onOpenSettings),
    )
}

@Composable
private fun SessionLine(state: UiState, s: AppStrings) {
    val start = state.sessionStart
    if (start != null && state.messages.isNotEmpty()) {
        Text(
            text = s.sessionLine(start, state.messages.size),
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
    s: AppStrings,
    onReplay: (ChatMessage) -> Unit,
    onShareText: (ChatMessage) -> Unit,
    onShareAudio: (ChatMessage) -> Unit,
    operatorZh: Boolean,
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
            val breathe = rememberInfiniteTransition(label = "empty")
            val emptyAlpha by breathe.animateFloat(
                initialValue = 0.35f,
                targetValue = 0.75f,
                animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
                label = "emptyAlpha",
            )
            Icon(
                imageVector = Icons.Filled.GraphicEq,
                contentDescription = null,
                tint = TolmachColors.Jade.copy(alpha = emptyAlpha),
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = s.emptyTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = s.emptyBody,
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
                            s = s,
                            operatorZh = operatorZh,
                            onReplay = { onReplay(message) },
                            onShareText = { onShareText(message) },
                            onShareAudio = { onShareAudio(message) },
                        )
                    }
                } else {
                    MessageBubble(
                        message = message,
                        s = s,
                        operatorZh = operatorZh,
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
    s: AppStrings,
    operatorZh: Boolean,
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
    val haptics = LocalHapticFeedback.current

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            shape = shape,
            color = container,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .widthIn(max = 330.dp)
                .clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    clipboard.setText(AnnotatedString(primaryText + "\n" + secondaryText))
                    Toast.makeText(context, s.copiedToast, Toast.LENGTH_SHORT).show()
                },
        ) {
            Column(Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = (if (fromPartner) s.partnerLabel else s.youLabel).uppercase() +
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
                            contentDescription = s.replayTooltip,
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
                                contentDescription = s.shareTooltip,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = shareMenu,
                            onDismissRequest = { shareMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(s.shareAsText) },
                                onClick = { shareMenu = false; onShareText() },
                            )
                            if (fromPartner == operatorZh) {
                                DropdownMenuItem(
                                    text = { Text(s.shareAsVoice) },
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

// ---------- Живая строка ----------

@Composable
private fun LiveLine(state: UiState, s: AppStrings, onStopSpeaking: () -> Unit) {
    val text = when {
        state.speaking -> s.speakingNow
        state.listening && state.partial.isNotBlank() -> state.partial
        state.listening && state.mode == Mode.LISTEN_CHINESE -> s.listeningPartner
        state.listening && state.mode == Mode.REPLY_RUSSIAN -> s.speakRussianNow
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
                            contentDescription = s.stopSpeakTooltip,
                            tint = TolmachColors.Coral,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

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
    s: AppStrings,
    onToggleListen: () -> Unit,
    onReply: () -> Unit,
    onPhrasebook: () -> Unit,
) {
    val listening = state.mode == Mode.LISTEN_CHINESE
    val replying = state.mode == Mode.REPLY_RUSSIAN

    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DeckSideButton(
                icon = Icons.Filled.Bolt,
                label = s.deckPhrases,
                active = false,
                accent = TolmachColors.Gold,
                modifier = Modifier.weight(1f),
                onClick = onPhrasebook,
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1.4f),
            ) {
                ListenOrb(listening = listening, s = s, onClick = onToggleListen)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (listening) s.deckStop else s.deckListen,
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
                label = s.deckReply,
                active = replying,
                accent = TolmachColors.Jade,
                modifier = Modifier.weight(1f),
                onClick = onReply,
            )
        }
    }
}

@Composable
private fun ListenOrb(listening: Boolean, s: AppStrings, onClick: () -> Unit) {
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
                    contentDescription = if (active) s.deckStop else s.deckListen,
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
                tint = if (active) {
                    TolmachColors.JadeDeep
                } else {
                    accent
                },
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

// ---------- Вкладка «Фразы» ----------

@Composable
private fun PhrasesScreen(
    state: UiState,
    onPick: (Phrase) -> Unit,
    onRemoveCustom: (Phrase) -> Unit,
    onAddOwn: () -> Unit,
) {
    val s = strings(state.appLanguage)
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
        ) {
            Text(
                text = s.phrasesTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onAddOwn) { Text(s.phrasesAdd) }
        }
        Text(
            text = s.phrasesHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            if (state.customPhrases.isNotEmpty()) {
                Text(
                    text = s.myPhrases.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TolmachColors.Gold,
                    modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
                )
                state.customPhrases.forEach { phrase ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        PhraseRow(
                            phrase = phrase,
                            onClick = { onPick(phrase) },
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onRemoveCustom(phrase) }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = s.deleteTooltip,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    PhraseDivider()
                }
            }
            BuiltInPhrases.groupBy { it.category }.forEach { (category, phrases) ->
                Text(
                    text = s.categoryLabel(category).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TolmachColors.Gold,
                    modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
                )
                phrases.forEach { phrase ->
                    PhraseRow(
                        phrase = phrase,
                        onClick = { onPick(phrase) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PhraseDivider()
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PhraseRow(phrase: Phrase, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
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
}

@Composable
private fun PhraseDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

// ---------- Вкладка «Настройки» ----------

@Composable
private fun SettingsScreen(
    state: UiState,
    viewModel: TranslatorViewModel,
    onOpenGlossary: () -> Unit,
    onOpenPreflight: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val s = strings(state.appLanguage)
    val operatorZh = state.appLanguage == "zh"
    val quietRoute = if (operatorZh) state.chineseRoute else state.russianRoute
    val loudRoute = if (operatorZh) state.russianRoute else state.chineseRoute
    fun applyPreset(quiet: String, loud: String) {
        if (operatorZh) {
            viewModel.setChineseRoute(quiet)
            viewModel.setRussianRoute(loud)
        } else {
            viewModel.setRussianRoute(quiet)
            viewModel.setChineseRoute(loud)
        }
    }
    val languages = listOf(
        "zh-CN" to s.dialectMandarinFull,
        "zh-TW" to s.dialectTaiwanFull,
        "yue-Hant-HK" to s.dialectCantoneseFull,
    )
    val russianRoutes = listOf(
        AudioRoutes.SYSTEM to s.ruRouteAuto,
        AudioRoutes.BLUETOOTH to s.ruRouteBt,
        AudioRoutes.WIRED to s.ruRouteWired,
        AudioRoutes.SPEAKER to s.ruRouteSpeaker,
    )
    val chineseRoutes = listOf(
        AudioRoutes.SPEAKER to s.zhRouteSpeaker,
        AudioRoutes.WIRED to s.zhRouteWired,
        AudioRoutes.BLUETOOTH to s.zhRouteBt,
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = s.settingsTitle,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(s.sectionLanguage)
            RouteOption(
                label = s.languageRu,
                selected = state.appLanguage == "ru",
                onClick = { viewModel.setAppLanguage("ru") },
            )
            RouteOption(
                label = s.languageZh,
                selected = state.appLanguage == "zh",
                onClick = { viewModel.setAppLanguage("zh") },
            )

            SectionHeader(s.sectionTools)
            PresetRow(
                title = s.toolPreflight,
                subtitle = s.toolPreflightHint,
                onClick = onOpenPreflight,
            )
            PresetRow(
                title = s.toolGlossary,
                subtitle = s.toolGlossaryHint,
                onClick = onOpenGlossary,
            )
            PresetRow(
                title = s.toolGuide,
                subtitle = s.toolGuideHint,
                onClick = viewModel::showGuideAgain,
            )
            PresetRow(
                title = s.toolAbout,
                subtitle = s.toolAboutHint,
                onClick = onOpenAbout,
            )

            SectionHeader(s.sectionPresets)
            PresetRow(
                title = s.presetSpeakerTitle,
                active = quietRoute == AudioRoutes.SYSTEM && loudRoute == AudioRoutes.SPEAKER,
                subtitle = s.presetSpeakerHint,
                onClick = {
                    applyPreset(AudioRoutes.SYSTEM, AudioRoutes.SPEAKER)
                },
            )
            PresetRow(
                title = s.presetTwoPairsTitle,
                active = quietRoute == AudioRoutes.BLUETOOTH && loudRoute == AudioRoutes.WIRED,
                subtitle = s.presetTwoPairsHint,
                onClick = {
                    applyPreset(AudioRoutes.BLUETOOTH, AudioRoutes.WIRED)
                },
            )
            PresetRow(
                title = s.presetRemoteTitle,
                active = quietRoute == AudioRoutes.SYSTEM && loudRoute == AudioRoutes.SPEAKER,
                subtitle = s.presetRemoteHint,
                onClick = {
                    applyPreset(AudioRoutes.SYSTEM, AudioRoutes.SPEAKER)
                },
            )
            PresetRow(
                title = s.presetPartnerBtTitle,
                active = quietRoute == AudioRoutes.SPEAKER && loudRoute == AudioRoutes.BLUETOOTH,
                subtitle = s.presetPartnerBtHint,
                onClick = {
                    applyPreset(AudioRoutes.SPEAKER, AudioRoutes.BLUETOOTH)
                },
            )

            SectionHeader(s.sectionDialect)
            languages.forEach { (tag, label) ->
                RouteOption(
                    label = label,
                    selected = state.chineseLanguageTag == tag,
                    onClick = { viewModel.setChineseLanguage(tag) },
                )
            }

            SectionHeader(s.sectionRuRoute)
            russianRoutes.forEach { (route, label) ->
                RouteOption(
                    label = label,
                    selected = state.russianRoute == route,
                    onClick = { viewModel.setRussianRoute(route) },
                )
            }
            TextButton(
                onClick = viewModel::testRussianVoice,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            ) { Text(s.testRuChannel) }

            SectionHeader(s.sectionZhRoute)
            chineseRoutes.forEach { (route, label) ->
                RouteOption(
                    label = label,
                    selected = state.chineseRoute == route,
                    onClick = { viewModel.setChineseRoute(route) },
                )
            }
            TextButton(
                onClick = viewModel::testChineseVoice,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            ) { Text(s.testZhChannel) }

            SectionHeader(s.sectionEngine)
            RouteOption(
                label = s.engineOffline,
                selected = !state.useDeepL,
                onClick = { viewModel.setUseDeepL(false) },
            )
            RouteOption(
                label = s.engineDeepL,
                selected = state.useDeepL,
                onClick = { viewModel.setUseDeepL(true) },
            )
            if (state.useDeepL) {
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.deepLKey,
                    onValueChange = viewModel::setDeepLKey,
                    label = { Text(s.deepLKeyLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = s.deepLHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionHeader(s.sectionRate)
            Text(
                text = s.rateLine("%.2f".format(state.chineseRate)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = state.chineseRate,
                onValueChange = viewModel::setChineseRate,
                valueRange = 0.7f..1.2f,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionHeader(s.sectionConfirm)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = s.confirmTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = s.confirmHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.confirmReply,
                    onCheckedChange = viewModel::setConfirmReply,
                )
            }

            SectionHeader(s.sectionRuVoice)
            if (state.ruVoices.isEmpty()) {
                Text(
                    text = s.voicesLoading,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                RouteOption(
                    label = s.voiceSystemDefault,
                    selected = state.russianVoice.isBlank(),
                    onClick = { viewModel.setRussianVoice("") },
                )
                state.ruVoices.forEach { (name, label) ->
                    RouteOption(
                        label = label,
                        selected = state.russianVoice == name,
                        onClick = { viewModel.setRussianVoice(name) },
                    )
                }
            }

            SectionHeader(s.sectionZhVoice)
            if (state.zhVoices.isEmpty()) {
                Text(
                    text = s.voicesLoading,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                RouteOption(
                    label = s.voiceSystemDefault,
                    selected = state.chineseVoice.isBlank(),
                    onClick = { viewModel.setChineseVoice("") },
                )
                state.zhVoices.forEach { (name, label) ->
                    RouteOption(
                        label = label,
                        selected = state.chineseVoice == name,
                        onClick = { viewModel.setChineseVoice(name) },
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = s.settingsFootnote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = TolmachColors.Gold,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
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
private fun PresetRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (active) TolmachColors.Jade else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            if (active) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(TolmachColors.Jade, CircleShape),
                )
            }
        }
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

// ---------- Диалоги ----------

@Composable
private fun GlossaryDialog(
    s: AppStrings,
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
        title = { Text(s.glossaryTitle) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = s.glossaryHint,
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
                            text = entry.from + " → " + entry.to,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onRemove(entry) }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = s.deleteTooltip,
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
                    label = { Text(s.glossaryFrom) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = to,
                    onValueChange = { to = it },
                    label = { Text(s.glossaryTo) },
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
            ) { Text(s.addAction) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.doneAction) }
        },
    )
}

@Composable
private fun ComposeDialog(
    s: AppStrings,
    onSpeak: (String) -> Unit,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(s.composeTitle) },
        text = {
            Column {
                Text(
                    text = s.composeHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(s.composeLabel) },
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
            ) { Text(s.composeSpeak) }
        },
        dismissButton = {
            Row {
                TextButton(
                    enabled = text.isNotBlank(),
                    onClick = {
                        onSave(text)
                        onDismiss()
                    },
                ) { Text(s.composeSave) }
                TextButton(onClick = onDismiss) { Text(s.cancelAction) }
            }
        },
    )
}

@Composable
private fun AboutDialog(s: AppStrings, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: ""
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(s.aboutTitle(version)) },
        text = {
            Column {
                Text(
                    text = s.aboutLine1,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = s.aboutLine2,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(s.doneAction) }
        },
    )
}

@Composable
private fun PreflightDialog(
    s: AppStrings,
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
        title = { Text(s.preflightTitle) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                CheckRow(
                    label = s.checkMic,
                    ok = micGranted,
                    detail = if (micGranted) null else s.checkMicHintNo,
                )
                CheckRow(
                    label = s.checkTranslation,
                    ok = translationOk,
                    detail = when {
                        deepLActive -> s.checkTranslationDeepL
                        state.modelsReady -> s.checkTranslationOffline
                        else -> s.checkTranslationNo
                    },
                )
                CheckRow(
                    label = s.checkRuVoice,
                    ok = state.voiceRussianOk,
                    detail = if (state.voiceRussianOk) null else s.checkVoiceHintNo,
                )
                CheckRow(
                    label = s.checkZhVoice,
                    ok = state.voiceChineseOk,
                    detail = if (state.voiceChineseOk) null else s.checkVoiceHintNo,
                )
                if (!state.voiceRussianOk || !state.voiceChineseOk) {
                    TextButton(
                        onClick = onOpenTtsSettings,
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                    ) { Text(s.openTtsSettings) }
                }
                CheckRow(
                    label = s.checkHeadphones,
                    ok = headphonesOk,
                    detail = when {
                        state.bluetoothConnected && state.wiredConnected -> s.headphonesDetailTwo
                        state.bluetoothConnected -> s.headphonesDetailBt
                        state.wiredConnected -> s.headphonesDetailWired
                        else -> s.headphonesDetailNone
                    },
                )
                CheckRow(
                    label = s.checkVolume,
                    ok = volumePercent >= 25,
                    detail = s.volumeDetail(volumePercent),
                )
                Spacer(Modifier.height(6.dp))
                Row {
                    TextButton(
                        onClick = onTestRussian,
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                    ) { Text(s.testRuShort) }
                    Spacer(Modifier.width(10.dp))
                    TextButton(
                        onClick = onTestChinese,
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                    ) { Text(s.testZhShort) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(s.doneAction) }
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
    s: AppStrings,
    initial: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var text by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(s.replyConfirmTitle) },
        text = {
            Column {
                Text(
                    text = s.replyConfirmHint,
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
            ) { Text(s.replyConfirmSpeak) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(s.cancelAction) }
        },
    )
}
