package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DiagnosticReport
import com.example.model.DiagnosticStatus
import com.example.model.Song
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.StudioError
import com.example.ui.theme.StudioGold
import com.example.ui.theme.StudioSuccess
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StudioHeader(
    currentSong: Song?,
    diagnosticReport: DiagnosticReport?,
    onDiagnosticClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasError = diagnosticReport?.let {
        it.checkpoint1Status == DiagnosticStatus.FAILED ||
                it.checkpoint2Status == DiagnosticStatus.FAILED ||
                it.checkpoint3Status == DiagnosticStatus.FAILED
    } ?: false

    val hasWarning = !hasError && (diagnosticReport?.let {
        it.checkpoint1Status == DiagnosticStatus.WARNING ||
                it.checkpoint2Status == DiagnosticStatus.WARNING ||
                it.checkpoint3Status == DiagnosticStatus.WARNING
    } ?: false)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Title & Tagline
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(NeonCyan, NeonViolet)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Studio Logo",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {
                Text(
                    text = "AI 跟唱工坊",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Seed-VC 零样本歌声克隆与双轨跟唱",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Diagnostic Badge Button (Direct access to the 3-checkpoint solver)
        val badgeColor = when {
            hasError -> StudioError
            hasWarning -> StudioGold
            else -> StudioSuccess
        }

        val badgeText = when {
            hasError -> "3处卡点警报"
            hasWarning -> "排查就绪"
            else -> "管线正常"
        }

        val badgeIcon = when {
            hasError -> Icons.Default.BugReport
            hasWarning -> Icons.Default.Warning
            else -> Icons.Default.CheckCircle
        }

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(badgeColor.copy(alpha = 0.16f))
                .clickable { onDiagnosticClick() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("diagnostic_badge_btn"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = badgeIcon,
                contentDescription = "Diagnostic Status",
                tint = badgeColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = badgeText,
                color = badgeColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
