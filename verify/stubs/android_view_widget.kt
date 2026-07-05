package android.view
class Window { fun addFlags(flags: Int) {}; fun clearFlags(flags: Int) {} }
class WindowManager { class LayoutParams { companion object { const val FLAG_KEEP_SCREEN_ON = 128 } } }
