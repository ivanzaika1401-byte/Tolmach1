package androidx.activity.compose
import androidx.activity.ComponentActivity
fun ComponentActivity.setContent(content: () -> Unit) { content() }
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {}
class ManagedActivityResultLauncher { fun launch(input: String) {} }
fun rememberLauncherForActivityResult(
    contract: androidx.activity.result.contract.ActivityResultContracts.RequestPermission,
    onResult: (Boolean) -> Unit
): ManagedActivityResultLauncher = ManagedActivityResultLauncher()
