package com.pulseforge.wear.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.pulseforge.shared.algorithm.PttBpEstimator
import com.pulseforge.shared.model.BloodPressureMeasurement
import com.pulseforge.shared.model.SensorSample
import com.pulseforge.wear.datalayer.WearDataSender
import com.pulseforge.wear.sensor.GalaxySensorManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WearApp(
    sensorManager: GalaxySensorManager,
    dataSender: WearDataSender
) {
    val coroutineScope = rememberCoroutineScope()
    val isMeasuring by sensorManager.isMeasuring.collectAsState()
    val heartRate by sensorManager.currentHeartRate.collectAsState()

    var isPhoneConnected by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableIntStateOf(30) }
    var lastResult by remember { mutableStateOf<BloodPressureMeasurement?>(null) }
    val wavePoints = remember { mutableStateListOf<Float>() }

    // Check phone connection on start
    LaunchedEffect(Unit) {
        isPhoneConnected = dataSender.checkPhoneConnected()
    }

    // Collect sensor samples for live waveform & streaming
    LaunchedEffect(isMeasuring) {
        if (isMeasuring) {
            wavePoints.clear()
            sensorManager.sampleFlow.collect { sample ->
                val v = if (sample.ppgGreen != 0f) sample.ppgGreen else sample.ecgMv
                if (wavePoints.size > 50) wavePoints.removeAt(0)
                wavePoints.add(v)

                // Stream to phone
                dataSender.sendSample(sample)
            }
        }
    }

    // Measurement countdown timer
    LaunchedEffect(isMeasuring) {
        if (isMeasuring) {
            countdownSeconds = 30
            while (countdownSeconds > 0 && sensorManager.isMeasuring.value) {
                delay(1000)
                countdownSeconds--
            }
            if (sensorManager.isMeasuring.value) {
                // Done measuring!
                val samples = sensorManager.stopMeasurement()
                val result = PttBpEstimator.estimateBloodPressure(samples)
                lastResult = result
                dataSender.sendMeasurementResult(result)
            }
        }
    }

    Scaffold(
        timeText = { TimeText() },
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B12))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isMeasuring) {
                // Measuring View
                MeasuringScreen(
                    countdown = countdownSeconds,
                    heartRate = heartRate.toInt(),
                    wavePoints = wavePoints,
                    onCancel = {
                        sensorManager.stopMeasurement()
                    }
                )
            } else if (lastResult != null) {
                // Result View
                ResultScreen(
                    result = lastResult!!,
                    onDismiss = { lastResult = null },
                    onRemeaure = {
                        lastResult = null
                        sensorManager.startMeasurement(30)
                    }
                )
            } else {
                // Idle / Start View
                IdleHomeScreen(
                    isPhoneConnected = isPhoneConnected,
                    onStartMeasure = {
                        sensorManager.startMeasurement(30)
                    }
                )
            }
        }
    }
}

@Composable
fun IdleHomeScreen(
    isPhoneConnected: Boolean,
    onStartMeasure: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "PULSEFORGE",
            color = Color(0xFF00E676),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isPhoneConnected) Color(0xFF00E676) else Color(0xFFFF9100))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isPhoneConnected) "Phone Paired" else "Standalone",
                color = Color(0xFF8B949E),
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onStartMeasure,
            modifier = Modifier
                .size((76 * pulseScale).dp)
                .clip(CircleShape),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF00E676)
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "START",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
                Text(
                    text = "BP & ECG",
                    color = Color.Black.copy(alpha = 0.7f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Rest wrist on table",
            color = Color(0xFF8B949E),
            fontSize = 9.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MeasuringScreen(
    countdown: Int,
    heartRate: Int,
    wavePoints: List<Float>,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = (30 - countdown) / 30f,
            modifier = Modifier.fillMaxSize(0.92f),
            strokeWidth = 4.dp,
            indicatorColor = Color(0xFF00E676),
            trackColor = Color(0xFF1E293B)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "${countdown}s",
                color = Color(0xFF00E676),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (heartRate > 0) "$heartRate BPM" else "Detecting...",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Waveform visualizer
            WaveformCanvas(
                points = wavePoints,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(28.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onCancel,
                modifier = Modifier
                    .height(28.dp)
                    .width(70.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF263238)
                )
            ) {
                Text("Stop", color = Color(0xFFFF5252), fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun WaveformCanvas(
    points: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas

        val minV = points.minOrNull() ?: 0f
        val maxV = points.maxOrNull() ?: 1f
        val range = if (maxV - minV > 0.001f) maxV - minV else 1f

        val stepX = size.width / (points.size - 1)
        val path = Path()

        points.forEachIndexed { index, value ->
            val x = index * stepX
            val normalizedY = (value - minV) / range
            val y = size.height - (normalizedY * size.height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = Color(0xFF00E676),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun ResultScreen(
    result: BloodPressureMeasurement,
    onDismiss: () -> Unit,
    onRemeaure: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "BP ESTIMATE",
            color = Color(0xFF8B949E),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "${result.systolic}",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "/",
                color = Color(0xFF8B949E),
                fontSize = 22.sp,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            Text(
                text = "${result.diastolic}",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = " mmHg",
                color = Color(0xFF8B949E),
                fontSize = 9.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Text(
            text = "${result.category.label} • ${result.heartRate} BPM",
            color = Color(result.category.colorHex),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onRemeaure,
                modifier = Modifier
                    .height(28.dp)
                    .width(60.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E293B))
            ) {
                Text("Retry", fontSize = 10.sp, color = Color.White)
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .height(28.dp)
                    .width(60.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00E676))
            ) {
                Text("Done", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
