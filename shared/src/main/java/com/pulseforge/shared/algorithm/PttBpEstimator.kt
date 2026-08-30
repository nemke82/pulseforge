package com.pulseforge.shared.algorithm

import com.pulseforge.shared.model.BloodPressureMeasurement
import com.pulseforge.shared.model.CalibrationPoint
import com.pulseforge.shared.model.CalibrationProfile
import com.pulseforge.shared.model.SensorSample
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object PttBpEstimator {

    /**
     * Finds ECG R-peaks in raw mV signal buffer using a moving threshold algorithm
     */
    fun findEcgRPeaks(samples: List<SensorSample>, minPeakDistanceMs: Long = 400L): List<Long> {
        if (samples.size < 5) return emptyList()
        val peaks = mutableListOf<Long>()
        val values = samples.map { it.ecgMv }
        val avg = values.average().toFloat()
        val maxVal = values.maxOrNull() ?: avg
        val threshold = avg + (maxVal - avg) * 0.55f

        var lastPeakTime = 0L

        for (i in 2 until samples.size - 2) {
            val curr = samples[i]
            if (curr.ecgMv > threshold &&
                curr.ecgMv > samples[i - 1].ecgMv &&
                curr.ecgMv > samples[i - 2].ecgMv &&
                curr.ecgMv >= samples[i + 1].ecgMv &&
                curr.ecgMv >= samples[i + 2].ecgMv
            ) {
                if (curr.timestampMs - lastPeakTime >= minPeakDistanceMs) {
                    peaks.add(curr.timestampMs)
                    lastPeakTime = curr.timestampMs
                }
            }
        }
        return peaks
    }

    /**
     * Finds the systolic onset (foot or inflection point) on the PPG wave following an ECG R-peak
     */
    fun findPpgFootAfterTime(samples: List<SensorSample>, afterTimestampMs: Long, searchWindowMs: Long = 450L): Long? {
        val window = samples.filter { it.timestampMs in afterTimestampMs..(afterTimestampMs + searchWindowMs) }
        if (window.size < 3) return null

        // Find the minimum (foot) of the PPG waveform before it ascends
        var minVal = Float.MAX_VALUE
        var minTime: Long? = null

        for (sample in window) {
            val ppg = if (sample.ppgGreen != 0f) sample.ppgGreen else sample.ppgIr
            if (ppg < minVal) {
                minVal = ppg
                minTime = sample.timestampMs
            }
        }
        return minTime
    }

    /**
     * Calculates the mean Pulse Transit Time (PTT in ms) from synchronised ECG and PPG sample buffer
     */
    fun calculatePulseTransitTime(samples: List<SensorSample>): Double {
        if (samples.size < 20) return 220.0 // Baseline fallback PTT in ms

        val ecgPeaks = findEcgRPeaks(samples)
        if (ecgPeaks.isEmpty()) {
            // If ECG is not present/noisy, estimate from PPG pulse cycle (IBI / morphology)
            val avgHr = samples.map { it.heartRate }.filter { it > 40f }.average()
            val hr = if (avgHr.isNaN() || avgHr == 0.0) 72.0 else avgHr
            // Physiological approximation when ECG electrode is untouched
            return max(160.0, min(320.0, 310.0 - (hr * 1.1)))
        }

        val pttList = mutableListOf<Double>()
        for (rPeakTime in ecgPeaks) {
            val ppgFoot = findPpgFootAfterTime(samples, rPeakTime)
            if (ppgFoot != null && ppgFoot > rPeakTime) {
                val diff = (ppgFoot - rPeakTime).toDouble()
                if (diff in 100.0..450.0) {
                    pttList.add(diff)
                }
            }
        }

        return if (pttList.isNotEmpty()) {
            pttList.average()
        } else {
            220.0
        }
    }

    /**
     * Calibrate profile parameters from 3 Omron/cuff measurement points using least-squares / regression
     */
    fun calculateCalibrationProfile(
        userId: String = "user_default",
        points: List<CalibrationPoint>
    ): CalibrationProfile {
        if (points.size < 3) {
            return CalibrationProfile(profileId = userId, points = points)
        }

        // Regression for SYS: SYS = a * PTT + b * HR + c
        // Since PTT decreases as BP rises, a is negative
        val avgPtt = points.map { it.pttMs }.average()
        val avgHr = points.map { it.heartRate }.average()
        val avgSys = points.map { it.cuffSys.toDouble() }.average()
        val avgDia = points.map { it.cuffDia.toDouble() }.average()

        var numSys = 0.0
        var denPtt = 0.0
        var numDia = 0.0

        for (p in points) {
            val dPtt = p.pttMs - avgPtt
            val dSys = p.cuffSys - avgSys
            val dDia = p.cuffDia - avgDia
            numSys += dPtt * dSys
            numDia += dPtt * dDia
            denPtt += dPtt * dPtt
        }

        val aSys = if (denPtt > 0.001) min(-0.05, numSys / denPtt) else -0.18
        val aDia = if (denPtt > 0.001) min(-0.03, numDia / denPtt) else -0.10
        val bSys = 0.30
        val bDia = 0.18

        val cSys = avgSys - (aSys * avgPtt) - (bSys * avgHr)
        val cDia = avgDia - (aDia * avgPtt) - (bDia * avgHr)

        return CalibrationProfile(
            profileId = userId,
            aSys = aSys,
            bSys = bSys,
            cSys = cSys,
            aDia = aDia,
            bDia = bDia,
            cDia = cDia,
            points = points,
            lastCalibratedMs = System.currentTimeMillis()
        )
    }

    /**
     * Estimates SYS and DIA from sensor buffer and user calibration profile
     */
    fun estimateBloodPressure(
        samples: List<SensorSample>,
        profile: CalibrationProfile = CalibrationProfile()
    ): BloodPressureMeasurement {
        val pttMs = calculatePulseTransitTime(samples)
        val validHr = samples.map { it.heartRate.toDouble() }.filter { it > 40.0 }
        val hr = if (validHr.isNotEmpty()) validHr.average() else 72.0

        val rawSys = (profile.aSys * pttMs) + (profile.bSys * hr) + profile.cSys
        val rawDia = (profile.aDia * pttMs) + (profile.bDia * hr) + profile.cDia

        // Clamping to physiologically valid ranges
        val finalSys = max(80, min(210, rawSys.roundToInt()))
        val finalDia = max(50, min(130, rawDia.roundToInt()))
        val finalHr = max(40, min(190, hr.roundToInt()))

        // Confidence score based on sample count & ECG detection
        val ecgPeaks = findEcgRPeaks(samples)
        val confidence = if (ecgPeaks.size >= 5) 0.95f else if (samples.size > 100) 0.85f else 0.60f

        return BloodPressureMeasurement(
            systolic = finalSys,
            diastolic = finalDia,
            heartRate = finalHr,
            pttMs = pttMs,
            confidence = confidence
        )
    }
}
