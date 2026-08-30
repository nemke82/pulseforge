package com.pulseforge.wear.datalayer

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.pulseforge.shared.model.DataLayerConstants
import com.pulseforge.wear.data.WearHistoryManager
import com.pulseforge.wear.reminder.WearReminderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class WearMessageListenerService : WearableListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            DataLayerConstants.PATH_REQUEST_HISTORY_SYNC -> {
                val historyManager = WearHistoryManager(applicationContext)
                val dataSender = WearDataSender(applicationContext)
                scope.launch {
                    val jsonHistory = historyManager.getAllAsJson()
                    dataSender.sendAllHistory(jsonHistory)
                }
            }
            DataLayerConstants.PATH_REMINDER_UPDATE -> {
                try {
                    val jsonStr = String(messageEvent.data, Charsets.UTF_8)
                    val json = JSONObject(jsonStr)
                    val intervalHours = json.optInt("interval_hours", 0)
                    val reminderManager = WearReminderManager(applicationContext)
                    reminderManager.setIntervalHours(intervalHours)
                } catch (_: Exception) {}
            }
        }
    }
}
