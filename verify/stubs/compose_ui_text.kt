package androidx.compose.ui.text
class AnnotatedString(val text: String)
class TextStyle(
    val fontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    val fontWeight: androidx.compose.ui.text.font.FontWeight? = null,
    val fontSize: androidx.compose.ui.unit.TextUnit? = null,
    val lineHeight: androidx.compose.ui.unit.TextUnit? = null,
    val letterSpacing: androidx.compose.ui.unit.TextUnit? = null
)
