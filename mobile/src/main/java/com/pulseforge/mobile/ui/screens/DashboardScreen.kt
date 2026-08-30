package com.pulseforge.mobile.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulseforge.mobile.data.BloodPressureRepository
import com.pulseforge.mobile.datalayer.PhoneDataLayerManager
import com.pulseforge.mobile.ui.theme.BorderColor
import com.pulseforge.mobile.ui.theme.DarkSurface
import com.pulseforge.mobile.ui.theme.DarkSurfaceElevated
import com.pulseforge.mobile.ui.theme.ElectricBlue
import com.pulseforge.mobile.ui.theme.NeonGreen
import com.pulseforge.mobile.ui.theme.TextMuted
import com.pulseforge.mobile.ui.theme.TextPrimary
import com.pulseforge.mobile.ui.theme.TextSecondary
import com.pulseforge.mobile.ui.theme.WarningOrange
import com.pulseforge.shared.model.BloodPressureMeasurement
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    dataLayerManager: PhoneDataLayerManager,
    onNavigateToSignal: () -> Unit,
    onNavigateToCalibration: () -> Unit
) {
    val measurements by BloodPressureRepository.measurements.collectAsState()
    val calibrationProfile by BloodPressureRepository.calibrationProfile.collectAsState()
    val latest = measurements.firstOrNull()
    val coroutineScope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }
    var syncStatusText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PULSEFORGE",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonGreen,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Galaxy Watch Sensor Telemetry",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            // Watch connection & Sync badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceElevated)
                    .clickable {
                        if (!isSyncing) {
                            coroutineScope.launch {
                                isSyncing = true
                                syncStatusText = "Syncing..."
                                val success = dataLayerManager.requestHistorySync()
                                syncStatusText = if (success) "Synced!" else "Watch offline"
                                delay(2000)
                                isSyncing = false
                                syncStatusText = null
                            }
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isSyncing) ElectricBlue else NeonGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (isSyncing) Icons.Default.Refresh else Icons.Default.Watch,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = syncStatusText ?: "Sync Watch",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Calibration Reminder Banner if not 3 points
        AnimatedVisibility(visible = !calibrationProfile.isCalibrated) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { onNavigateToCalibration() },
                colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarningOrange.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = WarningOrange,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Calibration Required",
                            fontWeight = FontWeight.Bold,
                            color = WarningOrange,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${calibrationProfile.points.size}/3 cuff measurements recorded. Tap to calibrate.",
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Primary BP Hero Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LATEST ESTIMATE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )

                    latest?.let {
                        Text(
                            text = SimpleDateFormat("HH:mm, MMM d", Locale.getDefault()).format(Date(it.timestampMs)),
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (latest != null) {
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "${latest.systolic}",
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            text = "/",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Text(
                            text = "${latest.diastolic}",
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            text = "mmHg",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 12.dp, start = 6.dp)
                        )
                    }

                    // Category Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(latest.category.colorHex).copy(alpha = 0.2f))
                            .border(1.dp, Color(latest.category.colorHex), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = latest.category.label.uppercase(),
                            color = Color(latest.category.colorHex),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Secondary Vitals (HR & PTT)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${latest.heartRate} BPM",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Text(text = "Heart Rate", fontSize = 11.sp, color = TextMuted)
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(BorderColor)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${latest.pttMs.toInt()} ms",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlue
                            )
                            Text(text = "Pulse Transit (PTT)", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                } else {
                    Text(
                        text = "No readings yet",
                        color = TextMuted,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action button: Trigger Watch Measure
                Button(
                    onClick = {
                        coroutineScope.launch {
                            dataLayerManager.triggerWatchMeasurement()
                        }
                        onNavigateToSignal()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "START WATCH MEASUREMENT",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Readings List
        Text(
            text = "RECENT READINGS",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        measurements.take(5).forEach { m ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(m.category.colorHex))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${m.systolic} / ${m.diastolic} mmHg",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                            Text(
                                text = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(m.timestampMs)),
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Text(
                        text = "${m.heartRate} BPM",
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
