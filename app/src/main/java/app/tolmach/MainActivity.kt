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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
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
                .padding(horizontal = 16.dp),
        ) {
            Header(
                state = state,
                onSettings = { showSettings = true },
                onRetryModels = viewModel::retryModelDownload,
            )
            ConversationList(state, modifier = Modifier.weight(1f))
            LiveLine(state)
            Controls(
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

@Composable
private fun Header(
    state: UiState,
    onSettings: () -> Unit,
    onRetryModels: () -> Unit,
) {
    Column(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Толмач",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "Живой перевод переговоров: китайский — русский",
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
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(
                text = when {
                    state.modelsReady -> "Модели готовы"
                    state.modelDownloadFailed -> "Модели: ошибка"
                    else -> "Загружаю модели…"
                },
                ok = state.modelsReady,
            )
            StatusChip(
                text = if (state.headsetConnected) "Наушники: есть" else "Наушники: нет",
                ok = state.headsetConnected,
            )
            StatusChip(
                text = when (state.chineseLanguageTag) {
                    "yue-Hant-HK" -> "Кантонский"
                    "zh-TW" -> "Тайвань"
                    else -> "Путунхуа"
                },
                ok = true,
            )
        }
        if (state.modelDownloadFailed) {
            TextButton(onClick = onRetryModels, contentPadding = PaddingValues(0.dp)) {
                Text("Повторить загрузку моделей")
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, ok: Boolean) {
    val color = if (ok) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
        contentColor = color,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun ConversationList(state: UiState, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    if (state.messages.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "Положите телефон ближе к собеседнику и нажмите " +
                    "«Слушать китайский» — перевод придёт вам в наушники.\n\n" +
                    "Чтобы ответить, нажмите «Ответить по-русски»: телефон " +
                    "озвучит вашу фразу по-китайски через динамик.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp),
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(state.messages, key = { it.id }) { message ->
                MessageBubble(message)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val fromPartner = message.fromChinese
    val alignment = if (fromPartner) Alignment.CenterStart else Alignment.CenterEnd
    val container = if (fromPartner) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val primaryText = if (fromPartner) message.translated else message.original
    val secondaryText = if (fromPartner) message.original else message.translated

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = container,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clickable {
                    clipboard.setText(AnnotatedString(primaryText + "\n" + secondaryText))
                    Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
                },
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = if (fromPartner) {
                        "Партнёр · ${message.time}"
                    } else {
                        "Вы · ${message.time}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(primaryText, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

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
                .padding(bottom = 8.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            }
        }
    }
}

@Composable
private fun Controls(
    state: UiState,
    onToggleListen: () -> Unit,
    onReply: () -> Unit,
    onGlossary: () -> Unit,
    onShare: () -> Unit,
    onClear: () -> Unit,
) {
    val listening = state.mode == Mode.LISTEN_CHINESE

    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Button(
            onClick = onToggleListen,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = if (listening) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            } else {
                ButtonDefaults.buttonColors()
            },
        ) {
            Icon(
                imageVector = if (listening) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = null,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (listening) "Остановить" else "Слушать китайский",
                fontSize = 17.sp,
            )
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onReply,
            enabled = state.mode != Mode.REPLY_RUSSIAN,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("Ответить по-русски")
        }

        Spacer(Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(onClick = onGlossary) {
                Text("Словарь")
            }
            TextButton(onClick = onShare, enabled = state.messages.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("Стенограмма")
            }
            TextButton(onClick = onClear, enabled = state.messages.isNotEmpty()) {
                Text("Очистить")
            }
        }
    }
}

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
        title = { Text("Словарь терминов") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Замены применяются к готовому переводу — названия " +
                        "товаров, марки стали, термины поставок. Пример: " +
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
