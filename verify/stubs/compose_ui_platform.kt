package androidx.compose.ui.platform
class ClipboardManager {
    fun setText(annotatedString: androidx.compose.ui.text.AnnotatedString) {}
    fun getText(): androidx.compose.ui.text.AnnotatedString? = null
}
object LocalClipboardManager { val current: ClipboardManager get() = ClipboardManager() }
object LocalContext { val current: android.content.Context get() = android.content.Context() }
object LocalHapticFeedback {
    val current: androidx.compose.ui.hapticfeedback.HapticFeedback
        get() = androidx.compose.ui.hapticfeedback.HapticFeedback()
}
