package androidx.compose.ui.text.font
class FontWeight private constructor() {
    companion object {
        val Normal = FontWeight(); val Medium = FontWeight()
        val SemiBold = FontWeight(); val Bold = FontWeight()
    }
}
class Font(resId: Int, weight: FontWeight)
class FontFamily(vararg fonts: Font)
