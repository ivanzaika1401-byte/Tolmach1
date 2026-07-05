package androidx.compose.ui.graphics
class Color(val value: Long) {
    fun copy(alpha: Float): Color = this
}
interface Shape
class Brush private constructor() {
    companion object {
        fun verticalGradient(vararg colorStops: Pair<Float, Color>): Brush = Brush()
        fun verticalGradient(colors: List<Color>): Brush = Brush()
    }
}
class GraphicsLayerScope { var scaleX = 1f; var scaleY = 1f; var alpha = 1f }
fun androidx.compose.ui.Modifier.graphicsLayer(
    block: GraphicsLayerScope.() -> Unit
): androidx.compose.ui.Modifier = this
