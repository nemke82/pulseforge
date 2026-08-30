package com.pulseforge.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulseforge.mobile.data.BloodPressureRepository
import com.pulseforge.mobile.ui.theme.BorderColor
import com.pulseforge.mobile.ui.theme.DarkSurface
import com.pulseforge.mobile.ui.theme.NeonGreen
import com.pulseforge.mobile.ui.theme.TextMuted
import com.pulseforge.mobile.ui.theme.TextPrimary
import com.pulseforge.mobile.ui.theme.TextSecondary
import com.pulseforge.mobile.ui.theme.WarningOrange

import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.rememberCoroutineScope
import com.pulseforge.mobile.datalayer.PhoneDataLayerManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(dataLayerManager: PhoneDataLayerManager? = null) {
    var syncHealthConnect by remember { mutableStateOf(true) }
    var autoMeasureOnWrist by remember { mutableStateOf(false) }
    var selectedWatchReminder by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "SETTINGS & SYNC",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Device management, reminders and health options",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Card: Galaxy Watch Reminders
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Galaxy Watch Reminders",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Vibrating reminder on your smartwatch to take a Blood Pressure reading",
                    color = TextMuted,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                val reminderOptions = listOf(0 to "Off", 1 to "1 hr", 2 to "2 hrs", 4 to "4 hrs", 6 to "6 hrs")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    reminderOptions.forEach { (hours, label) ->
                        val isSelected = selectedWatchReminder == hours
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedWatchReminder = hours
                                dataLayerManager?.let { mgr ->
                                    coroutineScope.launch {
                                        mgr.sendReminderSettingsToWatch(hours)
                                    }
                                }
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonGreen,
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0xFF1B2438),
                                labelColor = TextPrimary
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 1: Data Integration
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sync with Android Health Connect",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Save BloodPressure and HeartRate records to Samsung Health / Google Fit ecosystem",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = syncHealthConnect,
                        onCheckedChange = { syncHealthConnect = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = NeonGreen
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Background Auto-Measure",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Hourly background PTT sampling when watch detects stationary state",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = autoMeasureOnWrist,
                        onCheckedChange = { autoMeasureOnWrist = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = NeonGreen
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 2: Reset Calibration
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Calibration Profile Management",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Text(
                    text = "Reset recorded cuff points if you switched watches or changed cuff apparatus",
                    color = TextMuted,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { BloodPressureRepository.resetCalibration() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningOrange),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Reset All Calibration Data")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 3: About
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = NeonGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "About PulseForge v1.0.0",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Standalone PPG + ECG Pulse Transit Time Blood Pressure estimation suite for Samsung Galaxy Watch (Wear OS 3/4/5). Not a certified medical device.",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}
