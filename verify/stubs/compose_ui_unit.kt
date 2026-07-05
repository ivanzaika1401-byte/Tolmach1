package androidx.compose.ui.unit
class Dp(val value: Float)
val Int.dp: Dp get() = Dp(toFloat())
val Float.dp: Dp get() = Dp(this)
val Double.dp: Dp get() = Dp(toFloat())
class TextUnit(val value: Float)
val Int.sp: TextUnit get() = TextUnit(toFloat())
val Double.sp: TextUnit get() = TextUnit(toFloat())
