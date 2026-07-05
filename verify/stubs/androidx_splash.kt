package androidx.core.splashscreen
class SplashScreen private constructor() {
    companion object {
        fun androidx.activity.ComponentActivity.installSplashScreen(): SplashScreen =
            SplashScreen()
    }
}
