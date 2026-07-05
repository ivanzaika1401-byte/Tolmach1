package android.widget
class Toast {
    fun show() {}
    companion object {
        const val LENGTH_SHORT = 0
        fun makeText(context: android.content.Context, text: String, duration: Int): Toast = Toast()
    }
}
