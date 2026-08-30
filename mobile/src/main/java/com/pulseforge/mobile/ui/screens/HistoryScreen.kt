package com.pulseforge.mobile.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.pulseforge.mobile.datalayer.PhoneDataLayerManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(dataLayerManager: PhoneDataLayerManager? = null) {
    val measurements by BloodPressureRepository.measurements.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }

    val avgSys = if (measurements.isNotEmpty()) measurements.map { it.systolic }.average().toInt() else 0
    val avgDia = if (measurements.isNotEmpty()) measurements.map { it.diastolic }.average().toInt() else 0
    val avgHr = if (measurements.isNotEmpty()) measurements.map { it.heartRate }.average().toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MEASUREMENT HISTORY",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${measurements.size} total readings recorded",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            if (dataLayerManager != null) {
                IconButton(
                    onClick = {
                        if (!isSyncing) {
                            coroutineScope.launch {
                                isSyncing = true
                                dataLayerManager.requestHistorySync()
                                delay(1500)
                                isSyncing = false
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync",
                        tint = if (isSyncing) ElectricBlue else NeonGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Summary Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "AVG BLOOD PRESSURE", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text(text = "$avgSys / $avgDia", fontSize = 22.sp, fontWeight = FontWeight.Black, color = NeonGreen)
                    Text(text = "mmHg", fontSize = 10.sp, color = TextSecondary)
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(BorderColor)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "AVG HEART RATE", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text(text = "$avgHr", fontSize = 22.sp, fontWeight = FontWeight.Black, color = ElectricBlue)
                    Text(text = "BPM", fontSize = 10.sp, color = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // History Log List
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(measurements) { m ->
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
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(m.category.colorHex))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${m.systolic} / ${m.diastolic} mmHg",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = "${m.category.label} • PTT: ${m.pttMs.toInt()}ms",
                                    color = Color(m.category.colorHex),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${m.heartRate} BPM",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(m.timestampMs)),
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
