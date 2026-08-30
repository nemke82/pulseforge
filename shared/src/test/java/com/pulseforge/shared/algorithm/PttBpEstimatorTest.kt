package com.pulseforge.shared.algorithm

import com.pulseforge.shared.model.CalibrationPoint
import com.pulseforge.shared.model.SensorSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class PttBpEstimatorTest {

    @Test
    fun testPttAndBpEstimation() {
        val samples = mutableListOf<SensorSample>()
        val now = System.currentTimeMillis()

        // Generate synthetic ECG R-peak and PPG pulse delay (~200ms delay)
        for (i in 0 until 500) {
            val t = now + (i * 20L) // 50 Hz sampling
            val isEcgPeak = (i % 50 == 0)
            val ecgMv = if (isEcgPeak) 2.5f else 0.1f
            // PPG peak ~200ms later (10 samples later)
            val ppgPhase = (i - 10) % 50
            val ppgVal = sin(ppgPhase * 2 * Math.PI / 50).toFloat()

            samples.add(
                SensorSample(
                    timestampMs = t,
                    ppgGreen = ppgVal,
                    ppgIr = ppgVal,
                    ecgMv = ecgMv,
                    heartRate = 60f
                )
            )
        }

        val result = PttBpEstimator.estimateBloodPressure(samples)
        assertTrue(result.systolic in 90..160)
        assertTrue(result.diastolic in 55..100)
        assertEquals(60, result.heartRate)
    }

    @Test
    fun testCalibrationProfile() {
        val points = listOf(
            CalibrationPoint(cuffSys = 120, cuffDia = 80, pttMs = 230.0, heartRate = 70.0),
            CalibrationPoint(cuffSys = 135, cuffDia = 88, pttMs = 195.0, heartRate = 78.0),
            CalibrationPoint(cuffSys = 115, cuffDia = 75, pttMs = 250.0, heartRate = 65.0)
        )

        val profile = PttBpEstimator.calculateCalibrationProfile("test_user", points)
        assertTrue(profile.isCalibrated)
        assertTrue(profile.aSys < 0) // Inverse relationship between PTT and Blood Pressure
    }
}
