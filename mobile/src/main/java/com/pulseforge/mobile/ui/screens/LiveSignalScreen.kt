package com.pulseforge.mobile.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulseforge.mobile.data.BloodPressureRepository
import com.pulseforge.mobile.ui.theme.BorderColor
import com.pulseforge.mobile.ui.theme.DarkSurface
import com.pulseforge.mobile.ui.theme.ElectricBlue
import com.pulseforge.mobile.ui.theme.NeonGreen
import com.pulseforge.mobile.ui.theme.TextMuted
import com.pulseforge.mobile.ui.theme.TextPrimary
import com.pulseforge.mobile.ui.theme.TextSecondary
import com.pulseforge.shared.algorithm.PttBpEstimator

@Composable
fun LiveSignalScreen() {
    val liveSamples by BloodPressureRepository.liveSensorStream.collectAsState()
    val ptt = PttBpEstimator.calculatePulseTransitTime(liveSamples)

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
                    text = "LIVE SENSOR STREAM",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Raw PPG Photoplethysmogram + ECG",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NeonGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "50 Hz Stream",
                    fontSize = 12.sp,
                    color = NeonGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PTT Indicator Card
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
                    Text(text = "PULSE TRANSIT TIME (Δt)", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text(text = "${ptt.toInt()} ms", fontSize = 26.sp, fontWeight = FontWeight.Black, color = ElectricBlue)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "ESTIMATED BP", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    val estBp = PttBpEstimator.estimateBloodPressure(liveSamples)
                    Text(text = "${estBp.systolic}/${estBp.diastolic}", fontSize = 26.sp, fontWeight = FontWeight.Black, color = NeonGreen)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PPG Waveform Card
        SignalOscilloscopeCard(
            title = "OPTICAL PPG (GREEN / INFRARED)",
            subtitle = "Pulse Arrival & Dicrotic Notch",
            lineColor = NeonGreen,
            values = liveSamples.map { if (it.ppgGreen != 0f) it.ppgGreen else it.ppgIr }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ECG Waveform Card
        SignalOscilloscopeCard(
            title = "ECG ELECTROCARDIOGRAM (mV)",
            subtitle = "QRS Complex & R-Peak Detection",
            lineColor = Color(0xFFFF5252),
            values = liveSamples.map { it.ecgMv }
        )
    }
}

@Composable
fun SignalOscilloscopeCard(
    title: String,
    subtitle: String,
    lineColor: Color,
    values: List<Float>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = lineColor)
                    Text(text = subtitle, fontSize = 10.sp, color = TextMuted)
                }
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = lineColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Oscilloscope Grid & Path
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF070B12), RoundedCornerShape(8.dp))
                    .padding(4.dp)
            ) {
                // Draw grid lines
                val gridRows = 4
                val gridCols = 8
                val rowHeight = size.height / gridRows
                val colWidth = size.width / gridCols

                for (r in 1..gridRows) {
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(0f, r * rowHeight),
                        end = Offset(size.width, r * rowHeight),
                        strokeWidth = 1f
                    )
                }
                for (c in 1..gridCols) {
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(c * colWidth, 0f),
                        end = Offset(c * colWidth, size.height),
                        strokeWidth = 1f
                    )
                }

                if (values.size < 2) return@Canvas

                val minV = values.minOrNull() ?: 0f
                val maxV = values.maxOrNull() ?: 1f
                val range = if (maxV - minV > 0.001f) maxV - minV else 1f

                val stepX = size.width / (values.size - 1)
                val path = Path()

                values.forEachIndexed { i, v ->
                    val x = i * stepX
                    val normY = (v - minV) / range
                    val y = size.height - (normY * (size.height - 10f)) - 5f
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}
