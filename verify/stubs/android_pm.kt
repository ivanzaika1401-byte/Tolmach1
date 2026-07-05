package android.content.pm
class PackageInfo { val versionName: String? = "stub" }
class PackageManager {
    fun getPackageInfo(packageName: String, flags: Int): PackageInfo = PackageInfo()
    companion object { const val PERMISSION_GRANTED = 0 }
}
