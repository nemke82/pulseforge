package com.pulseforge.mobile.datalayer

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.pulseforge.mobile.data.BloodPressureRepository
import com.pulseforge.shared.model.BloodPressureMeasurement
import com.pulseforge.shared.model.DataLayerConstants
import com.pulseforge.shared.model.SensorSample
import org.json.JSONObject

class PhoneWearableListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            DataLayerConstants.PATH_LIVE_SAMPLE_STREAM -> {
                try {
                    val jsonStr = String(messageEvent.data, Charsets.UTF_8)
                    val json = JSONObject(jsonStr)
                    val sample = SensorSample(
                        timestampMs = json.optLong("t", System.currentTimeMillis()),
                        ppgGreen = json.optDouble("ppg", 0.0).toFloat(),
                        ppgIr = json.optDouble("ir", 0.0).toFloat(),
                        ecgMv = json.optDouble("ecg", 0.0).toFloat(),
                        heartRate = json.optDouble("hr", 0.0).toFloat()
                    )
                    BloodPressureRepository.updateLiveSample(sample)
                } catch (_: Exception) {}
            }
            DataLayerConstants.PATH_MEASUREMENT_RESULT -> {
                try {
                    val jsonStr = String(messageEvent.data, Charsets.UTF_8)
                    val json = JSONObject(jsonStr)
                    val measurement = BloodPressureMeasurement(
                        id = json.optString("id", java.util.UUID.randomUUID().toString()),
                        timestampMs = json.optLong("timestamp", System.currentTimeMillis()),
                        systolic = json.optInt("sys", 120),
                        diastolic = json.optInt("dia", 80),
                        heartRate = json.optInt("hr", 70),
                        pttMs = json.optDouble("ptt", 220.0),
                        confidence = json.optDouble("confidence", 0.9).toFloat()
                    )
                    BloodPressureRepository.addMeasurement(measurement)
                } catch (_: Exception) {}
            }
            DataLayerConstants.PATH_HISTORY_SYNC_RESPONSE -> {
                try {
                    val jsonStr = String(messageEvent.data, Charsets.UTF_8)
                    val jsonArray = org.json.JSONArray(jsonStr)
                    val list = mutableListOf<BloodPressureMeasurement>()
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
                    BloodPressureRepository.mergeHistory(list)
                } catch (_: Exception) {}
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val uri = event.dataItem.uri
                if (uri.path == DataLayerConstants.PATH_MEASUREMENT_RESULT) {
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val measurement = BloodPressureMeasurement(
                        id = dataMap.getString("id", java.util.UUID.randomUUID().toString()),
                        timestampMs = dataMap.getLong("timestamp", System.currentTimeMillis()),
                        systolic = dataMap.getInt("sys", 120),
                        diastolic = dataMap.getInt("dia", 80),
                        heartRate = dataMap.getInt("hr", 70),
                        pttMs = dataMap.getDouble("ptt", 220.0),
                        confidence = dataMap.getFloat("confidence", 0.9f)
                    )
                    BloodPressureRepository.addMeasurement(measurement)
                }
            }
        }
    }
}
