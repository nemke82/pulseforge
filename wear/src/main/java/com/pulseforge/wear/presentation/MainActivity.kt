package com.pulseforge.wear.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.pulseforge.wear.datalayer.WearDataSender
import com.pulseforge.wear.presentation.theme.PulseForgeWearTheme
import com.pulseforge.wear.sensor.GalaxySensorManager

class MainActivity : ComponentActivity() {

    private lateinit var sensorManager: GalaxySensorManager
    private lateinit var dataSender: WearDataSender

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = GalaxySensorManager(this)
        dataSender = WearDataSender(this)

        requestSensorPermissions()

        setContent {
            PulseForgeWearTheme {
                WearApp(
                    sensorManager = sensorManager,
                    dataSender = dataSender
                )
            }
        }
    }

    private fun requestSensorPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.BODY_SENSORS
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.BODY_SENSORS_BACKGROUND)
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
    }
}
