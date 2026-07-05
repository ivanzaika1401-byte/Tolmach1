package androidx.core.content
object ContextCompat {
    fun checkSelfPermission(context: android.content.Context, permission: String): Int = 0
}
object FileProvider {
    fun getUriForFile(
        context: android.content.Context,
        authority: String,
        file: java.io.File
    ): android.net.Uri = android.net.Uri()
}
