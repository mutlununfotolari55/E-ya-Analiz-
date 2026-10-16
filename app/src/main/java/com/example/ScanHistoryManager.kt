package com.example

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ScanHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val previewText: String,
    val fullResponse: String,
    val imageUris: List<String> = emptyList(),
    val location: String? = null
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale("tr"))
            return sdf.format(Date(timestamp))
        }
}

object ScanHistoryManager {
    private const val PREFS_NAME = "evren_scan_history"
    private const val KEY_HISTORY = "scans_json"
    private const val MAX_HISTORY_COUNT = 30

    fun getHistory(context: Context): List<ScanHistoryItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val list = mutableListOf<ScanHistoryItem>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val imageUris = mutableListOf<String>()
                val imgArr = obj.optJSONArray("imageUris")
                if (imgArr != null) {
                    for (j in 0 until imgArr.length()) {
                        imageUris.add(imgArr.getString(j))
                    }
                }
                list.add(
                    ScanHistoryItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        title = obj.optString("title", "Görsel Analizi"),
                        previewText = obj.optString("previewText", ""),
                        fullResponse = obj.optString("fullResponse", ""),
                        imageUris = imageUris,
                        location = if (obj.has("location") && !obj.isNull("location")) obj.getString("location") else null
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun addScan(context: Context, item: ScanHistoryItem) {
        val current = getHistory(context).toMutableList()
        // Remove duplicate if same id exists
        current.removeAll { it.id == item.id }
        current.add(0, item)
        if (current.size > MAX_HISTORY_COUNT) {
            current.removeAt(current.size - 1)
        }
        saveHistory(context, current)
    }

    fun deleteScan(context: Context, id: String) {
        val current = getHistory(context).toMutableList()
        current.removeAll { it.id == id }
        saveHistory(context, current)
    }

    fun clearHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun saveHistory(context: Context, list: List<ScanHistoryItem>) {
        try {
            val jsonArray = JSONArray()
            for (item in list) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("timestamp", item.timestamp)
                obj.put("title", item.title)
                obj.put("previewText", item.previewText)
                obj.put("fullResponse", item.fullResponse)
                val imgArr = JSONArray()
                item.imageUris.forEach { imgArr.put(it) }
                obj.put("imageUris", imgArr)
                if (item.location != null) {
                    obj.put("location", item.location)
                }
                jsonArray.put(obj)
            }
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
