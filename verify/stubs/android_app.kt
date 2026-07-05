package android.app
open class Application : android.content.Context()
open class Activity : android.content.Context() {
    open val window: android.view.Window? = android.view.Window()
}
