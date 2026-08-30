package com.pulseforge.mobile.datalayer

import android.content.Context
import com.google.android.gms.wearable.Wearable
import com.pulseforge.shared.model.DataLayerConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

class PhoneDataLayerManager(private val context: Context) {

    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    suspend fun isGalaxyWatchConnected(): Boolean = withContext(Dispatchers.IO) {
        try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getConnectedNodeName(): String? = withContext(Dispatchers.IO) {
        try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.firstOrNull()?.displayName
        } catch (_: Exception) {
            null
        }
    }

    suspend fun triggerWatchMeasurement() = withContext(Dispatchers.IO) {
        try {
            val nodes = nodeClient.connectedNodes.await()
            val json = JSONObject().apply {
                put("cmd", "START")
            }.toString().toByteArray(Charsets.UTF_8)

            for (node in nodes) {
                messageClient.sendMessage(node.id, DataLayerConstants.PATH_START_MEASUREMENT, json).await()
            }
        } catch (_: Exception) {}
    }

    suspend fun requestHistorySync(): Boolean = withContext(Dispatchers.IO) {
        try {
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) return@withContext false

            val json = JSONObject().apply {
                put("cmd", "SYNC_ALL")
            }.toString().toByteArray(Charsets.UTF_8)

            for (node in nodes) {
                messageClient.sendMessage(node.id, DataLayerConstants.PATH_REQUEST_HISTORY_SYNC, json).await()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun sendReminderSettingsToWatch(intervalHours: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) return@withContext false

            val json = JSONObject().apply {
                put("interval_hours", intervalHours)
            }.toString().toByteArray(Charsets.UTF_8)

            for (node in nodes) {
                messageClient.sendMessage(node.id, DataLayerConstants.PATH_REMINDER_UPDATE, json).await()
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
