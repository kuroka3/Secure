package io.github.kuroka3.secure.api

import io.github.kuroka3.JSONFile
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import java.io.File

object DataManager {

    private lateinit var file: JSONFile
    private lateinit var ary: JSONArray
    val values: List<Pair<String, String>>
        get() = ary.map { it as JSONObject }.map { it["name"].toString() to it["value"].toString() }

    fun init(path: File) {
        file = JSONFile(path, "values.json")
        if (!file.isFile) {
            file.createNewFile()
            file.saveJSON(JSONObject())
        }

        ary = file.jsonObject?.get("raw") as JSONArray? ?: JSONArray()
    }

    fun add(name: String, value: String) {
        ary.add(JSONObject().apply { this["name"] = name; this["value"] = value })
        save()
    }

    fun move(fromIndex: Int, toIndex: Int) {
        val temp = ary[fromIndex]
        ary.removeAt(fromIndex)
        ary.add(toIndex, temp)
        save()
    }

    fun remove(index: Int) {
        ary.removeAt(index)
        save()
    }

    private fun save() {
        file.saveJSON(JSONObject().apply { this["raw"] = ary })
    }
}