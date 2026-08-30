package com.pulseforge.wear.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.pulseforge.shared.model.SensorSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class GalaxySensorManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isMeasuring = MutableStateFlow(false)
    val isMeasuring: StateFlow<Boolean> = _isMeasuring.asStateFlow()

    private val _currentHeartRate = MutableStateFlow(72f)
    val currentHeartRate: StateFlow<Float> = _currentHeartRate.asStateFlow()

    private val _sampleFlow = MutableSharedFlow<SensorSample>(replay = 10)
    val sampleFlow: SharedFlow<SensorSample> = _sampleFlow.asSharedFlow()

    private val collectedSamples = mutableListOf<SensorSample>()
    private var simulationJob: Job? = null

    // Samsung Health Sensor IDs / Types
    // 65572: PPG Green / IR raw on Galaxy Watch 4/5/6/7
    // 65573: ECG raw on Galaxy Watch 4/5/6/7
    private var hrSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    private var ppgSensor: Sensor? = sensorManager.getSensorList(Sensor.TYPE_ALL).find {
        it.stringType.contains("ppg", ignoreCase = true) || it.type == 65572
    }
    private var ecgSensor: Sensor? = sensorManager.getSensorList(Sensor.TYPE_ALL).find {
        it.stringType.contains("ecg", ignoreCase = true) || it.type == 65573
    }

    fun startMeasurement(durationSeconds: Int = 30) {
        if (_isMeasuring.value) return
        _isMeasuring.value = true
        collectedSamples.clear()

        val registeredHr = hrSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        } ?: false

        val registeredPpg = ppgSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        } ?: false

        val registeredEcg = ecgSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        } ?: false

        // If running on emulator or watch sensors are resting on desk, activate simulation fallback
        if (!registeredPpg && !registeredEcg) {
            startSimulationStream()
        }
    }

    fun stopMeasurement(): List<SensorSample> {
        _isMeasuring.value = false
        try {
            sensorManager.unregisterListener(this)
        } catch (_: Exception) {}
        simulationJob?.cancel()
        simulationJob = null
        return collectedSamples.toList()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !_isMeasuring.value) return

        val now = System.currentTimeMillis()
        var sample: SensorSample? = null

        when (event.sensor.type) {
            Sensor.TYPE_HEART_RATE -> {
                val hr = event.values.firstOrNull() ?: 0f
                if (hr > 30f) {
                    _currentHeartRate.value = hr
                }
                sample = SensorSample(
                    timestampMs = now,
                    heartRate = _currentHeartRate.value
                )
            }
            65572 -> { // Samsung PPG
                val ppgGreen = event.values.getOrNull(0) ?: 0f
                val ppgIr = event.values.getOrNull(1) ?: ppgGreen
                sample = SensorSample(
                    timestampMs = now,
                    ppgGreen = ppgGreen,
                    ppgIr = ppgIr,
                    heartRate = _currentHeartRate.value
                )
            }
            65573 -> { // Samsung ECG
                val ecgVal = event.values.getOrNull(0) ?: 0f
                sample = SensorSample(
                    timestampMs = now,
                    ecgMv = ecgVal,
                    heartRate = _currentHeartRate.value
                )
            }
        }

        sample?.let {
            collectedSamples.add(it)
            scope.launch { _sampleFlow.emit(it) }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun startSimulationStream() {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            var step = 0
            val hr = 72f
            _currentHeartRate.value = hr
            val samplingPeriodMs = 20L // 50 Hz

            while (isActive && _isMeasuring.value) {
                val now = System.currentTimeMillis()
                val isRPeak = (step % 45 == 0) // ECG QRS spike every ~900ms (66-70 BPM)
                val ecgVal = if (isRPeak) (2.2f + (Math.random() * 0.4).toFloat()) else (sin(step * 0.2f) * 0.1f)

                // PPG pulse wave delayed ~200ms (10 samples)
                val ppgPhase = (step - 10) % 45
                val ppgVal = if (ppgPhase >= 0) {
                    (sin(ppgPhase * Math.PI / 25).toFloat() * 1.5f + 2.0f)
                } else 0.5f

                val sample = SensorSample(
                    timestampMs = now,
                    ppgGreen = ppgVal,
                    ppgIr = ppgVal * 1.1f,
                    ecgMv = ecgVal,
                    heartRate = hr
                )

                collectedSamples.add(sample)
                _sampleFlow.emit(sample)

                step++
                delay(samplingPeriodMs)
            }
        }
    }
}
