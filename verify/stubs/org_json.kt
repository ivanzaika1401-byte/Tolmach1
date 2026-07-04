package org.json
class JSONObject() {
    constructor(source: String) : this()
    fun put(key: String, value: Any?): JSONObject = this
    fun getString(key: String): String = ""
    fun getBoolean(key: String): Boolean = false
    fun optString(key: String, fallback: String): String = fallback
    fun getJSONArray(key: String): JSONArray = JSONArray()
    override fun toString(): String = "[]"
}
class JSONArray() {
    constructor(source: String) : this()
    fun length(): Int = 0
    fun put(value: Any?): JSONArray = this
    fun getJSONObject(index: Int): JSONObject = JSONObject()
    override fun toString(): String = "[]"
}
