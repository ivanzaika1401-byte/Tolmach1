package org.json

private fun escape(value: String): String {
    val sb = StringBuilder()
    for (ch in value) {
        when (ch) {
            '\\' -> sb.append("\\\\")
            '"' -> sb.append("\\\"")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> sb.append(ch)
        }
    }
    return sb.toString()
}

private class Parser(private val src: String) {
    private var pos = 0

    fun parseValue(): Any? {
        skipWs()
        return when (src[pos]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't' -> { pos += 4; true }
            'f' -> { pos += 5; false }
            'n' -> { pos += 4; null }
            else -> parseNumber()
        }
    }

    fun parseObject(): JSONObject {
        val obj = JSONObject()
        pos++ // {
        skipWs()
        if (src[pos] == '}') { pos++; return obj }
        while (true) {
            skipWs()
            val key = parseString()
            skipWs()
            pos++ // :
            obj.put(key, parseValue())
            skipWs()
            if (src[pos] == ',') { pos++; continue }
            pos++ // }
            return obj
        }
    }

    fun parseArray(): JSONArray {
        val arr = JSONArray()
        pos++ // [
        skipWs()
        if (src[pos] == ']') { pos++; return arr }
        while (true) {
            arr.put(parseValue())
            skipWs()
            if (src[pos] == ',') { pos++; continue }
            pos++ // ]
            return arr
        }
    }

    private fun parseString(): String {
        pos++ // "
        val sb = StringBuilder()
        while (true) {
            val ch = src[pos]
            if (ch == '"') { pos++; return sb.toString() }
            if (ch == '\\') {
                pos++
                when (src[pos]) {
                    '\\' -> sb.append('\\')
                    '"' -> sb.append('"')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    else -> sb.append(src[pos])
                }
                pos++
            } else {
                sb.append(ch)
                pos++
            }
        }
    }

    private fun parseNumber(): Any {
        val start = pos
        while (pos < src.length && (src[pos].isDigit() || src[pos] == '-' || src[pos] == '.')) pos++
        val raw = src.substring(start, pos)
        return if ('.' in raw) raw.toDouble() else raw.toLong()
    }

    private fun skipWs() {
        while (pos < src.length && src[pos].isWhitespace()) pos++
    }
}

class JSONObject() {
    private val map = LinkedHashMap<String, Any?>()

    constructor(source: String) : this() {
        val parsed = Parser(source).parseValue()
        if (parsed is JSONObject) map.putAll(parsed.map)
    }

    fun put(key: String, value: Any?): JSONObject {
        map[key] = value
        return this
    }

    fun getString(key: String): String = map[key] as String
    fun getBoolean(key: String): Boolean = map[key] as Boolean
    fun optString(key: String, fallback: String): String =
        map[key] as? String ?: fallback
    fun getJSONArray(key: String): JSONArray = map[key] as JSONArray

    override fun toString(): String {
        val sb = StringBuilder("{")
        var first = true
        for ((key, value) in map) {
            if (!first) sb.append(",")
            first = false
            sb.append('"').append(escape(key)).append("\":")
            appendValue(sb, value)
        }
        return sb.append("}").toString()
    }
}

class JSONArray() {
    private val list = ArrayList<Any?>()

    constructor(source: String) : this() {
        val parsed = Parser(source).parseValue()
        if (parsed is JSONArray) list.addAll(parsed.raw())
    }

    internal fun raw(): List<Any?> = list

    fun put(value: Any?): JSONArray {
        list.add(value)
        return this
    }

    fun length(): Int = list.size
    fun getJSONObject(index: Int): JSONObject = list[index] as JSONObject

    override fun toString(): String {
        val sb = StringBuilder("[")
        var first = true
        for (value in list) {
            if (!first) sb.append(",")
            first = false
            appendValue(sb, value)
        }
        return sb.append("]").toString()
    }
}

private fun appendValue(sb: StringBuilder, value: Any?) {
    when (value) {
        null -> sb.append("null")
        is String -> sb.append('"').append(escape(value)).append('"')
        is Boolean -> sb.append(value)
        is JSONObject -> sb.append(value.toString())
        is JSONArray -> sb.append(value.toString())
        else -> sb.append(value.toString())
    }
}
