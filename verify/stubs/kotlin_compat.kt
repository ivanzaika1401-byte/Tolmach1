// Полифилл stdlib 1.6 для проверочного компилятора 1.3 — только для verify.
package app.tolmach
fun <T> buildList(builderAction: MutableList<T>.() -> Unit): List<T> =
    mutableListOf<T>().apply(builderAction)

fun StringBuilder.appendLine(value: String): StringBuilder = append(value).append('\n')
fun StringBuilder.appendLine(): StringBuilder = append('\n')
