package app.tolmach

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
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
    var showGlossary by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
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

    // Во время сеанса перевода экран не гаснет.
    val activity = context as? Activity
    DisposableEffect(state.mode) {
        val window = activity?.window
        if (state.mode != Mode.IDLE) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp),
        ) {
            TopBar(onSettings = { showSettings = true })
            StatusStrip(state = state, onRetryModels = viewModel::retryModelDownload)
            ConversationList(
                state = state,
                onReplay = viewModel::replayMessage,
                modifier = Modifier.weight(1f),
            )
            LiveLine(state)
            ControlDeck(
                state = state,
                onToggleListen = { withMicPermission(viewModel::toggleListening) },
                onReply = { withMicPermission(viewModel::startRussianReply) },
                onGlossary = { showGlossary = true },
                onShare = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, viewModel.transcriptText())
                    }
                    context.startActivity(
                        Intent.createChooser(intent, "Отправить стенограмму"),
                    )
                },
                onClear = viewModel::clearConversation,
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
            onSelect = viewModel::setChineseLanguage,
            onDismiss = { showSettings = false },
        )
    }
}

// ---------- Шапка ----------

@Composable
private fun TopBar(onSettings: () -> Unit) {
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
            Text(
                text = "Синхронный перевод переговоров",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onSettings) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Настройки",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------- Приборная строка статусов ----------

@Composable
private fun StatusStrip(state: UiState, onRetryModels: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusCell(
                label = when {
                    state.modelsReady -> "Перевод: готов"
                    state.modelDownloadFailed -> "Перевод: ошибка"
                    else -> "Загрузка…"
                },
                dot = when {
                    state.modelsReady -> TolmachColors.Jade
                    state.modelDownloadFailed -> TolmachColors.Coral
                    else -> TolmachColors.TextDim
                },
                modifier = Modifier.weight(1f),
            )
            StripDivider()
            StatusCell(
                label = if (state.headsetConnected) "Наушники: есть" else "Наушники: нет",
                dot = if (state.headsetConnected) TolmachColors.Jade else TolmachColors.TextDim,
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
    if (state.modelDownloadFailed) {
        TextButton(
            onClick = onRetryModels,
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
        ) {
            Text("Повторить загрузку моделей")
        }
    } else {
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun StatusCell(label: String, dot: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(dot, CircleShape),
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

// ---------- Лента переговоров ----------

@Composable
private fun ConversationList(
    state: UiState,
    onReplay: (ChatMessage) -> Unit,
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
                text = "Нажмите зелёную кнопку — перевод партнёра придёт вам " +
                    "в наушники. Кнопка «Ответить» озвучит вашу русскую фразу " +
                    "по-китайски через динамик.",
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
                MessageBubble(message = message, onReplay = { onReplay(message) })
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, onReplay: () -> Unit) {
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
private fun LiveLine(state: UiState) {
    val text = when {
        state.speaking -> "Озвучиваю перевод…"
        state.listening && state.partial.isNotBlank() -> state.partial
        state.listening && state.mode == Mode.LISTEN_CHINESE -> "Слушаю собеседника…"
        state.listening && state.mode == Mode.REPLY_RUSSIAN -> "Говорите по-русски…"
        else -> null
    }
    if (text != null) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.GraphicEq,
                    contentDescription = null,
                    tint = TolmachColors.Jade,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                )
            }
        }
    }
}

// ---------- Пульт управления ----------

@Composable
private fun ControlDeck(
    state: UiState,
    onToggleListen: () -> Unit,
    onReply: () -> Unit,
    onGlossary: () -> Unit,
    onShare: () -> Unit,
    onClear: () -> Unit,
) {
    val listening = state.mode == Mode.LISTEN_CHINESE
    val replying = state.mode == Mode.REPLY_RUSSIAN
    var menuOpen by remember { mutableStateOf(false) }

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
            // Ответить по-русски
            DeckSideButton(
                icon = Icons.Filled.Mic,
                label = "Ответить",
                active = replying,
                modifier = Modifier.weight(1f),
                onClick = onReply,
            )
            // Главная кнопка-сфера: слушать китайский
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
            // Ещё: словарь, стенограмма, очистить
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                DeckSideButton(
                    icon = Icons.Filled.MoreHoriz,
                    label = "Ещё",
                    active = false,
                    onClick = { menuOpen = true },
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Словарь терминов") },
                        onClick = { menuOpen = false; onGlossary() },
                    )
                    DropdownMenuItem(
                        text = { Text("Отправить стенограмму") },
                        enabled = state.messages.isNotEmpty(),
                        onClick = { menuOpen = false; onShare() },
                    )
                    DropdownMenuItem(
                        text = { Text("Очистить беседу") },
                        enabled = state.messages.isNotEmpty(),
                        onClick = { menuOpen = false; onClear() },
                    )
                }
            }
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
            Icon(
                imageVector = if (listening) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = if (listening) "Остановить" else "Слушать китайский",
                tint = if (listening) TolmachColors.JadeDeep else TolmachColors.Jade,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

@Composable
private fun DeckSideButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(
                    if (active) TolmachColors.Jade else MaterialTheme.colorScheme.surface,
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
                    MaterialTheme.colorScheme.onSurfaceVariant
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
                    text = "Замены применяются к готовому переводу — названия " +
                        "товаров, марки, термины поставок. Пример: " +
                        "«инкотермс» → «Incoterms».",
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
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        "zh-CN" to "Путунхуа — стандартный китайский (рекомендуется)",
        "zh-TW" to "Тайваньский мандарин",
        "yue-Hant-HK" to "Кантонский — Гонконг, Гуандун (экспериментально)",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Язык собеседника") },
        text = {
            Column {
                options.forEach { (tag, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(tag) },
                    ) {
                        RadioButton(
                            selected = currentTag == tag,
                            onClick = { onSelect(tag) },
                        )
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Деловые переговоры в Китае ведутся на путунхуа — его " +
                        "распознавание самое точное. Кантонский распознаётся, но " +
                        "перевод с него слабее: другая лексика и иероглифика. " +
                        "Региональные диалекты (шанхайский, сычуаньский и др.) " +
                        "не поддерживает ни одна массовая система в мире.",
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
