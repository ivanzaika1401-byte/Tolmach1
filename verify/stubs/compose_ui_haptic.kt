package androidx.compose.ui.hapticfeedback
class HapticFeedbackType private constructor() {
    companion object { val LongPress = HapticFeedbackType() }
}
class HapticFeedback { fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {} }
