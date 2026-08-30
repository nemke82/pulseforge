package com.pulseforge.wear.data

import android.content.Context
import android.content.SharedPreferences
import com.pulseforge.shared.model.BloodPressureMeasurement
import org.json.JSONArray
import org.json.JSONObject

class WearHistoryManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("pulseforge_wear_history", Context.MODE_PRIVATE)

    fun addMeasurement(measurement: BloodPressureMeasurement) {
        val list = getHistory().toMutableList()
        list.add(0, measurement)
        // Keep max 50 recent records on watch
        val trimmed = if (list.size > 50) list.take(50) else list
        saveList(trimmed)
    }

    fun getHistory(): List<BloodPressureMeasurement> {
        val rawJson = prefs.getString("history_json", null) ?: return emptyList()
        val list = mutableListOf<BloodPressureMeasurement>()
        try {
            val jsonArray = JSONArray(rawJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    BloodPressureMeasurement(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        timestampMs = obj.optLong("timestamp", System.currentTimeMillis()),
                        systolic = obj.optInt("sys", 120),
                        diastolic = obj.optInt("dia", 80),
                        heartRate = obj.optInt("hr", 70),
                        pttMs = obj.optDouble("ptt", 220.0),
                        confidence = obj.optDouble("confidence", 0.9).toFloat()
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun getAllAsJson(): String {
        val list = getHistory()
        val jsonArray = JSONArray()
        for (m in list) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("timestamp", m.timestampMs)
                put("sys", m.systolic)
                put("dia", m.diastolic)
                put("hr", m.heartRate)
                put("ptt", m.pttMs)
                put("confidence", m.confidence)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    fun clearHistory() {
        prefs.edit().remove("history_json").apply()
    }

    private fun saveList(list: List<BloodPressureMeasurement>) {
        val jsonArray = JSONArray()
        for (m in list) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("timestamp", m.timestampMs)
                put("sys", m.systolic)
                put("dia", m.diastolic)
                put("hr", m.heartRate)
                put("ptt", m.pttMs)
                put("confidence", m.confidence)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("history_json", jsonArray.toString()).apply()
    }
}
