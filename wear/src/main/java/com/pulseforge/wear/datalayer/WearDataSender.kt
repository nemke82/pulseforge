package com.pulseforge.wear.datalayer

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.pulseforge.shared.model.BloodPressureMeasurement
import com.pulseforge.shared.model.DataLayerConstants
import com.pulseforge.shared.model.SensorSample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

class WearDataSender(private val context: Context) {

    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)
    private val dataClient = Wearable.getDataClient(context)

    suspend fun checkPhoneConnected(): Boolean = withContext(Dispatchers.IO) {
        try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun sendSample(sample: SensorSample) = withContext(Dispatchers.IO) {
        try {
            val nodes = nodeClient.connectedNodes.await()
            val json = JSONObject().apply {
                put("t", sample.timestampMs)
                put("ppg", sample.ppgGreen)
                put("ir", sample.ppgIr)
                put("ecg", sample.ecgMv)
                put("hr", sample.heartRate)
            }.toString().toByteArray(Charsets.UTF_8)

            for (node in nodes) {
                messageClient.sendMessage(node.id, DataLayerConstants.PATH_LIVE_SAMPLE_STREAM, json).await()
            }
        } catch (_: Exception) {}
    }

    suspend fun sendMeasurementResult(measurement: BloodPressureMeasurement) = withContext(Dispatchers.IO) {
        try {
            val putDataMapRequest = PutDataMapRequest.create(DataLayerConstants.PATH_MEASUREMENT_RESULT).apply {
                dataMap.putString("id", measurement.id)
                dataMap.putLong("timestamp", measurement.timestampMs)
                dataMap.putInt("sys", measurement.systolic)
                dataMap.putInt("dia", measurement.diastolic)
                dataMap.putInt("hr", measurement.heartRate)
                dataMap.putDouble("ptt", measurement.pttMs)
                dataMap.putFloat("confidence", measurement.confidence)
            }
            val request = putDataMapRequest.asPutDataRequest().setUrgent()
            dataClient.putDataItem(request).await()

            // Also send as direct message
            val nodes = nodeClient.connectedNodes.await()
            val json = JSONObject().apply {
                put("id", measurement.id)
                put("timestamp", measurement.timestampMs)
                put("sys", measurement.systolic)
                put("dia", measurement.diastolic)
                put("hr", measurement.heartRate)
                put("ptt", measurement.pttMs)
                put("confidence", measurement.confidence)
            }.toString().toByteArray(Charsets.UTF_8)

            for (node in nodes) {
                messageClient.sendMessage(node.id, DataLayerConstants.PATH_MEASUREMENT_RESULT, json).await()
            }
        } catch (_: Exception) {}
    }

    suspend fun sendAllHistory(jsonHistory: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) return@withContext false

            val payload = jsonHistory.toByteArray(Charsets.UTF_8)
            for (node in nodes) {
                messageClient.sendMessage(node.id, DataLayerConstants.PATH_HISTORY_SYNC_RESPONSE, payload).await()
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
