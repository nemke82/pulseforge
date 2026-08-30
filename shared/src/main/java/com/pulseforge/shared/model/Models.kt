package com.pulseforge.shared.model

import java.io.Serializable

/**
 * Single raw sensor sample collected from Galaxy Watch
 */
data class SensorSample(
    val timestampMs: Long,
    val ppgGreen: Float = 0f,
    val ppgIr: Float = 0f,
    val ppgRed: Float = 0f,
    val ecgMv: Float = 0f,
    val heartRate: Float = 0f,
    val ibiMs: Float = 0f
) : Serializable

/**
 * Calculated Blood Pressure and vitals reading
 */
data class BloodPressureMeasurement(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val systolic: Int,
    val diastolic: Int,
    val heartRate: Int,
    val pttMs: Double,
    val confidence: Float = 1.0f,
    val source: String = "GALAXY_WATCH_PTT"
) : Serializable {
    val category: BpCategory
        get() = when {
            systolic < 120 && diastolic < 80 -> BpCategory.OPTIMAL
            systolic in 120..129 && diastolic < 80 -> BpCategory.ELEVATED
            systolic in 130..139 || diastolic in 80..89 -> BpCategory.HYPERTENSION_STAGE_1
            systolic in 140..179 || diastolic in 90..119 -> BpCategory.HYPERTENSION_STAGE_2
            else -> BpCategory.HYPERTENSIVE_CRISIS
        }
}

enum class BpCategory(val label: String, val colorHex: Long) {
    OPTIMAL("Normal", 0xFF00E676),
    ELEVATED("Elevated", 0xFFFFD600),
    HYPERTENSION_STAGE_1("Stage 1", 0xFFFF9100),
    HYPERTENSION_STAGE_2("Stage 2", 0xFFFF3D00),
    HYPERTENSIVE_CRISIS("Crisis", 0xFFD50000)
}

/**
 * Reference calibration point comparing cuff reading with watch sensor timing
 */
data class CalibrationPoint(
    val cuffSys: Int,
    val cuffDia: Int,
    val pttMs: Double,
    val heartRate: Double,
    val timestampMs: Long = System.currentTimeMillis()
) : Serializable

/**
 * Personal calibration mathematical profile for Blood Pressure estimation
 * SYS = a_sys * PTT + b_sys * HR + c_sys
 * DIA = a_dia * PTT + b_dia * HR + c_dia
 */
data class CalibrationProfile(
    val profileId: String = "default_user",
    val aSys: Double = -0.15, // PTT negative correlation with SYS
    val bSys: Double = 0.35,  // HR positive correlation with SYS
    val cSys: Double = 145.0, // Intercept
    val aDia: Double = -0.09, // PTT negative correlation with DIA
    val bDia: Double = 0.20,  // HR correlation with DIA
    val cDia: Double = 95.0,  // Intercept
    val points: List<CalibrationPoint> = emptyList(),
    val lastCalibratedMs: Long = System.currentTimeMillis()
) : Serializable {
    val isCalibrated: Boolean get() = points.size >= 3
}

/**
 * Constants for Google Play Services Wearable Data Layer communications
 */
object DataLayerConstants {
    const val PATH_START_MEASUREMENT = "/pulseforge/start_measurement"
    const val PATH_STOP_MEASUREMENT = "/pulseforge/stop_measurement"
    const val PATH_LIVE_SAMPLE_STREAM = "/pulseforge/live_sample_stream"
    const val PATH_MEASUREMENT_RESULT = "/pulseforge/measurement_result"
    const val PATH_SYNC_CALIBRATION = "/pulseforge/sync_calibration"
    const val PATH_DEVICE_STATUS = "/pulseforge/device_status"

    const val KEY_SAMPLE_PAYLOAD = "key_sample_payload"
    const val KEY_RESULT_PAYLOAD = "key_result_payload"
    const val KEY_CALIBRATION_PAYLOAD = "key_calibration_payload"
    const val KEY_COMMAND = "key_command"
}
