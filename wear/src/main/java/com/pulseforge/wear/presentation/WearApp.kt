package com.pulseforge.wear.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.pulseforge.shared.algorithm.PttBpEstimator
import com.pulseforge.shared.model.BloodPressureMeasurement
import com.pulseforge.wear.data.WearHistoryManager
import com.pulseforge.wear.datalayer.WearDataSender
import com.pulseforge.wear.reminder.WearReminderManager
import com.pulseforge.wear.sensor.GalaxySensorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class WearScreen {
    HOME,
    MEASURING,
    RESULT,
    HISTORY,
    REMINDERS
}

@Composable
fun WearApp(
    sensorManager: GalaxySensorManager,
    dataSender: WearDataSender,
    historyManager: WearHistoryManager,
    reminderManager: WearReminderManager,
    initialAutoStart: Boolean = false,
    setKeepScreenOn: (Boolean) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val isMeasuring by sensorManager.isMeasuring.collectAsState()
    val heartRate by sensorManager.currentHeartRate.collectAsState()

    var currentScreen by remember { mutableStateOf(if (initialAutoStart) WearScreen.MEASURING else WearScreen.HOME) }
    var isPhoneConnected by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableIntStateOf(30) }
    var lastResult by remember { mutableStateOf<BloodPressureMeasurement?>(null) }
    var historyList by remember { mutableStateOf(historyManager.getHistory()) }
    var syncFeedback by remember { mutableStateOf<String?>(null) }
    val wavePoints = remember { mutableStateListOf<Float>() }

    // Check phone connection on launch
    LaunchedEffect(Unit) {
        isPhoneConnected = dataSender.checkPhoneConnected()
    }

    // Live sensor streaming during measurement
    LaunchedEffect(isMeasuring) {
        if (isMeasuring) {
            wavePoints.clear()
            sensorManager.sampleFlow.collect { sample ->
                val v = if (sample.ppgGreen != 0f) sample.ppgGreen else sample.ecgMv
                if (wavePoints.size > 45) wavePoints.removeAt(0)
                wavePoints.add(v)
                dataSender.sendSample(sample)
            }
        }
    }

    // Dedicated Measurement Lifecycle keyed on currentScreen
    LaunchedEffect(currentScreen) {
        if (currentScreen == WearScreen.MEASURING) {
            setKeepScreenOn(true)
            sensorManager.startMeasurement(30)
            countdownSeconds = 30

            while (countdownSeconds > 0) {
                delay(1000)
                countdownSeconds--
            }

            // Stop sensor measurement and calculate BP estimate
            val samples = sensorManager.stopMeasurement()
            val result = PttBpEstimator.estimateBloodPressure(samples)
            lastResult = result

            // Save reading to local watch history
            try {
                historyManager.addMeasurement(result)
                historyList = historyManager.getHistory()
            } catch (_: Exception) {}

            // Switch to RESULT screen immediately
            currentScreen = WearScreen.RESULT

            // Send to phone asynchronously in background so network/timeout never blocks the UI
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    dataSender.sendMeasurementResult(result)
                    val jsonHistory = historyManager.getAllAsJson()
                    dataSender.sendAllHistory(jsonHistory)
                } catch (_: Exception) {}
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
            when (currentScreen) {
                WearScreen.HOME -> {
                    setKeepScreenOn(false)
                    HomeScreen(
                        isPhoneConnected = isPhoneConnected,
                        latestReading = historyList.firstOrNull(),
                        syncFeedback = syncFeedback,
                        onStartMeasure = {
                            currentScreen = WearScreen.MEASURING
                        },
                        onOpenHistory = {
                            historyList = historyManager.getHistory()
                            currentScreen = WearScreen.HISTORY
                        },
                        onOpenReminders = {
                            currentScreen = WearScreen.REMINDERS
                        },
                        onTriggerSync = {
                            coroutineScope.launch {
                                syncFeedback = "Syncing..."
                                val jsonHistory = historyManager.getAllAsJson()
                                val success = dataSender.sendAllHistory(jsonHistory)
                                syncFeedback = if (success) "Synced!" else "Phone offline"
                                delay(2000)
                                syncFeedback = null
                            }
                        }
                    )
                }

                WearScreen.MEASURING -> {
                    setKeepScreenOn(true)
                    MeasuringScreen(
                        countdown = countdownSeconds,
                        heartRate = heartRate.toInt(),
                        wavePoints = wavePoints,
                        onCancel = {
                            sensorManager.stopMeasurement()
                            currentScreen = WearScreen.HOME
                            setKeepScreenOn(false)
                        }
                    )
                }

                WearScreen.RESULT -> {
                    setKeepScreenOn(true)
                    lastResult?.let { result ->
                        ResultScreen(
                            result = result,
                            onDismiss = {
                                currentScreen = WearScreen.HOME
                                setKeepScreenOn(false)
                            },
                            onRemeaure = {
                                currentScreen = WearScreen.MEASURING
                            }
                        )
                    } ?: run {
                        currentScreen = WearScreen.HOME
                    }
                }

                WearScreen.HISTORY -> {
                    setKeepScreenOn(false)
                    WatchHistoryScreen(
                        history = historyList,
                        onClear = {
                            historyManager.clearHistory()
                            historyList = emptyList()
                        },
                        onSync = {
                            coroutineScope.launch {
                                val jsonHistory = historyManager.getAllAsJson()
                                dataSender.sendAllHistory(jsonHistory)
                            }
                        },
                        onBack = { currentScreen = WearScreen.HOME }
                    )
                }

                WearScreen.REMINDERS -> {
                    setKeepScreenOn(false)
                    ReminderSettingsScreen(
                        reminderManager = reminderManager,
                        onBack = { currentScreen = WearScreen.HOME }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    isPhoneConnected: Boolean,
    latestReading: BloodPressureMeasurement?,
    syncFeedback: String?,
    onStartMeasure: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenReminders: () -> Unit,
    onTriggerSync: () -> Unit
) {
    val listState = rememberScalingLazyListState()
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

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "PULSEFORGE",
                color = Color(0xFF00E676),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (isPhoneConnected) Color(0xFF00E676) else Color(0xFFFF9100))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = syncFeedback ?: (if (isPhoneConnected) "Phone Paired" else "Standalone"),
                    color = if (syncFeedback != null) Color(0xFF00E676) else Color(0xFF8B949E),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Big Measure Button
        item {
            Button(
                onClick = onStartMeasure,
                modifier = Modifier
                    .size((70 * pulseScale).dp)
                    .clip(CircleShape),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00E676))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "START",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "MEASURE",
                        color = Color.Black.copy(alpha = 0.7f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Latest BP Summary Chip
        item {
            if (latestReading != null) {
                Text(
                    text = "Last: ${latestReading.systolic}/${latestReading.diastolic} mmHg • ${latestReading.heartRate} bpm",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            } else {
                Text(
                    text = "Rest wrist on table",
                    color = Color(0xFF8B949E),
                    fontSize = 9.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        // Navigation Quick Action Buttons
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // History Button
                CompactButton(
                    onClick = onOpenHistory,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF161E2E))
                ) {
                    Text("📋", fontSize = 11.sp)
                }

                // Reminders Button
                CompactButton(
                    onClick = onOpenReminders,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF161E2E))
                ) {
                    Text("⏰", fontSize = 11.sp)
                }

                // Sync Button
                CompactButton(
                    onClick = onTriggerSync,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF161E2E))
                ) {
                    Text("🔄", fontSize = 11.sp)
                }
            }
        }
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
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF263238))
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

@Composable
fun WatchHistoryScreen(
    history: List<BloodPressureMeasurement>,
    onClear: () -> Unit,
    onSync: () -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "HISTORY (${history.size})",
                color = Color(0xFF00E676),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (history.isEmpty()) {
            item {
                Text(
                    text = "No readings saved yet",
                    color = Color(0xFF8B949E),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        } else {
            items(history) { item ->
                Chip(
                    onClick = {},
                    colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF161E2E)),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(vertical = 2.dp),
                    label = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${item.systolic}/${item.diastolic} mmHg",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(item.timestampMs)),
                                    color = Color(0xFF8B949E),
                                    fontSize = 8.sp
                                )
                            }
                            Text(
                                text = "${item.heartRate} bpm",
                                color = Color(item.category.colorHex),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                )
            }

            item {
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompactButton(
                        onClick = onSync,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00E676))
                    ) {
                        Text("Sync", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    CompactButton(
                        onClick = onClear,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF263238))
                    ) {
                        Text("Clear", color = Color(0xFFFF5252), fontSize = 9.sp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            CompactButton(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E293B))
            ) {
                Text("Back", color = Color.White, fontSize = 9.sp)
            }
        }
    }
}

@Composable
fun ReminderSettingsScreen(
    reminderManager: WearReminderManager,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    var currentInterval by remember { mutableIntStateOf(reminderManager.getIntervalHours()) }

    val options = listOf(
        0 to "Off",
        1 to "Every 1 Hour",
        2 to "Every 2 Hours",
        4 to "Every 4 Hours",
        6 to "Every 6 Hours"
    )

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "REMINDERS",
                color = Color(0xFF00E676),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(options) { (hours, label) ->
            val isSelected = currentInterval == hours
            Chip(
                onClick = {
                    currentInterval = hours
                    reminderManager.setIntervalHours(hours)
                },
                colors = ChipDefaults.chipColors(
                    backgroundColor = if (isSelected) Color(0xFF00E676) else Color(0xFF161E2E)
                ),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = 2.dp),
                label = {
                    Text(
                        text = label,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            CompactButton(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E293B))
            ) {
                Text("Done", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
