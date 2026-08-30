package com.pulseforge.wear.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.pulseforge.wear.data.WearHistoryManager
import com.pulseforge.wear.datalayer.WearDataSender
import com.pulseforge.wear.presentation.theme.PulseForgeWearTheme
import com.pulseforge.wear.reminder.WearReminderManager
import com.pulseforge.wear.sensor.GalaxySensorManager

class MainActivity : ComponentActivity() {

    private lateinit var sensorManager: GalaxySensorManager
    private lateinit var dataSender: WearDataSender
    private lateinit var historyManager: WearHistoryManager
    private lateinit var reminderManager: WearReminderManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen awake while in foreground so measurement is never interrupted
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        sensorManager = GalaxySensorManager(this)
        dataSender = WearDataSender(this)
        historyManager = WearHistoryManager(this)
        reminderManager = WearReminderManager(this)

        requestRequiredPermissions()

        val autoStart = intent?.getBooleanExtra("ACTION_START_MEASURE", false) ?: false

        setContent {
            PulseForgeWearTheme {
                WearApp(
                    sensorManager = sensorManager,
                    dataSender = dataSender,
                    historyManager = historyManager,
                    reminderManager = reminderManager,
                    initialAutoStart = autoStart,
                    setKeepScreenOn = { keepOn ->
                        if (keepOn) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }
                )
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.BODY_SENSORS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.BODY_SENSORS_BACKGROUND)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.stopMeasurement()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
