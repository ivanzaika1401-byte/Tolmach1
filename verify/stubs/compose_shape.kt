package androidx.compose.foundation.shape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
object CircleShape : Shape
private object RoundedStub : Shape
fun RoundedCornerShape(size: Dp): Shape = RoundedStub
fun RoundedCornerShape(topStart: Dp, topEnd: Dp, bottomEnd: Dp, bottomStart: Dp): Shape =
    RoundedStub
