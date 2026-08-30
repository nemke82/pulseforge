package com.pulseforge.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulseforge.mobile.data.BloodPressureRepository
import com.pulseforge.mobile.ui.theme.BorderColor
import com.pulseforge.mobile.ui.theme.DarkSurface
import com.pulseforge.mobile.ui.theme.DarkSurfaceElevated
import com.pulseforge.mobile.ui.theme.ElectricBlue
import com.pulseforge.mobile.ui.theme.NeonGreen
import com.pulseforge.mobile.ui.theme.TextMuted
import com.pulseforge.mobile.ui.theme.TextPrimary
import com.pulseforge.mobile.ui.theme.TextSecondary
import com.pulseforge.mobile.ui.theme.WarningOrange
import com.pulseforge.shared.model.CalibrationPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CalibrationWizardScreen() {
    val calibrationProfile by BloodPressureRepository.calibrationProfile.collectAsState()
    var inputSys by remember { mutableStateOf("") }
    var inputDia by remember { mutableStateOf("") }
    var inputPtt by remember { mutableStateOf("210") }
    var inputHr by remember { mutableStateOf("72") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "CUFF CALIBRATION",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Personalize PTT mathematical formula with 3 Omron cuff points",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Calibration Progress / Status Card
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
                    Text(
                        text = if (calibrationProfile.isCalibrated) "CALIBRATED PROFILE ACTIVE" else "CALIBRATION INCOMPLETE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (calibrationProfile.isCalibrated) NeonGreen else WarningOrange
                    )
                    Text(
                        text = "${calibrationProfile.points.size} / 3 Points",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3 Step Circles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (step in 1..3) {
                        val isDone = calibrationProfile.points.size >= step
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isDone) NeonGreen else DarkSurfaceElevated)
                                    .border(1.dp, if (isDone) NeonGreen else BorderColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDone) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text(
                                        text = "$step",
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            if (step < 3) {
                                Box(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(2.dp)
                                        .background(if (calibrationProfile.points.size > step) NeonGreen else BorderColor)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Enter Measurement Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Add Reference Cuff Reading",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Take a simultaneous measurement with your Omron arm cuff",
                    fontSize = 11.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = inputSys,
                        onValueChange = { inputSys = it },
                        label = { Text("Cuff SYS") },
                        placeholder = { Text("120") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    OutlinedTextField(
                        value = inputDia,
                        onValueChange = { inputDia = it },
                        label = { Text("Cuff DIA") },
                        placeholder = { Text("80") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val sys = inputSys.toIntOrNull() ?: 120
                        val dia = inputDia.toIntOrNull() ?: 80
                        val ptt = inputPtt.toDoubleOrNull() ?: 210.0
                        val hr = inputHr.toDoubleOrNull() ?: 72.0

                        BloodPressureRepository.addCalibrationPoint(
                            CalibrationPoint(
                                cuffSys = sys,
                                cuffDia = dia,
                                pttMs = ptt,
                                heartRate = hr
                            )
                        )
                        inputSys = ""
                        inputDia = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text(
                        text = "SAVE CALIBRATION POINT",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Calibration Points List
        Text(
            text = "RECORDED CALIBRATION POINTS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        calibrationProfile.points.forEachIndexed { i, p ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#${i + 1}",
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${p.cuffSys} / ${p.cuffDia} mmHg",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "PTT: ${p.pttMs.toInt()} ms  •  HR: ${p.heartRate.toInt()} BPM",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Text(
                        text = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(p.timestampMs)),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
