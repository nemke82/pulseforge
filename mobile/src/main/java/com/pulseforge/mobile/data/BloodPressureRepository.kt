package com.pulseforge.mobile.data

import com.pulseforge.shared.algorithm.PttBpEstimator
import com.pulseforge.shared.model.BloodPressureMeasurement
import com.pulseforge.shared.model.CalibrationPoint
import com.pulseforge.shared.model.CalibrationProfile
import com.pulseforge.shared.model.SensorSample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BloodPressureRepository {

    private val _measurements = MutableStateFlow<List<BloodPressureMeasurement>>(
        listOf(
            BloodPressureMeasurement(
                timestampMs = System.currentTimeMillis() - 3600000 * 2,
                systolic = 122,
                diastolic = 81,
                heartRate = 68,
                pttMs = 215.0
            ),
            BloodPressureMeasurement(
                timestampMs = System.currentTimeMillis() - 3600000 * 8,
                systolic = 126,
                diastolic = 83,
                heartRate = 74,
                pttMs = 208.0
            ),
            BloodPressureMeasurement(
                timestampMs = System.currentTimeMillis() - 3600000 * 24,
                systolic = 118,
                diastolic = 78,
                heartRate = 65,
                pttMs = 228.0
            )
        )
    )
    val measurements: StateFlow<List<BloodPressureMeasurement>> = _measurements.asStateFlow()

    private val _calibrationProfile = MutableStateFlow(
        CalibrationProfile(
            points = listOf(
                CalibrationPoint(120, 80, 225.0, 68.0, System.currentTimeMillis() - 86400000 * 2),
                CalibrationPoint(128, 84, 205.0, 75.0, System.currentTimeMillis() - 86400000 * 2),
                CalibrationPoint(118, 77, 235.0, 64.0, System.currentTimeMillis() - 86400000 * 2)
            )
        )
    )
    val calibrationProfile: StateFlow<CalibrationProfile> = _calibrationProfile.asStateFlow()

    private val _liveSensorStream = MutableStateFlow<List<SensorSample>>(emptyList())
    val liveSensorStream: StateFlow<List<SensorSample>> = _liveSensorStream.asStateFlow()

    fun addMeasurement(measurement: BloodPressureMeasurement) {
        val current = _measurements.value.toMutableList()
        if (current.none { it.id == measurement.id || it.timestampMs == measurement.timestampMs }) {
            current.add(0, measurement)
            _measurements.value = current.sortedByDescending { it.timestampMs }
        }
    }

    fun mergeHistory(newItems: List<BloodPressureMeasurement>) {
        val currentMap = _measurements.value.associateBy { it.id }.toMutableMap()
        for (item in newItems) {
            currentMap[item.id] = item
        }
        _measurements.value = currentMap.values.sortedByDescending { it.timestampMs }
    }

    fun addCalibrationPoint(point: CalibrationPoint) {
        val currentProfile = _calibrationProfile.value
        val updatedPoints = currentProfile.points.toMutableList()
        updatedPoints.add(point)
        _calibrationProfile.value = PttBpEstimator.calculateCalibrationProfile(
            userId = currentProfile.profileId,
            points = updatedPoints
        )
    }

    fun resetCalibration() {
        _calibrationProfile.value = CalibrationProfile()
    }

    fun updateLiveSample(sample: SensorSample) {
        val current = _liveSensorStream.value.toMutableList()
        if (current.size > 120) {
            current.removeAt(0)
        }
        current.add(sample)
        _liveSensorStream.value = current
    }
}
