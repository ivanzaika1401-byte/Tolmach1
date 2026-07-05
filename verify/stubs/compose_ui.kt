package androidx.compose.ui
interface Modifier { companion object : Modifier }
class Alignment private constructor() {
    class Horizontal
    class Vertical
    companion object {
        val Center = Alignment()
        val CenterStart = Alignment()
        val CenterEnd = Alignment()
        val CenterVertically = Vertical()
        val CenterHorizontally = Horizontal()
    }
}
