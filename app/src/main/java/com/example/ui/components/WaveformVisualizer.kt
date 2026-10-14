package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LyricLine
import com.example.model.VocalMode
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.StudioGold
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WaveformInactive
import kotlin.math.sin

@Composable
fun DualStemVisualizer(
    isPlaying: Boolean,
    vocalMode: VocalMode,
    waveAmplitudes: List<Float>,
    instVolume: Float,
    vocalVolume: Float,
    isRecordingMic: Boolean,
    micAmplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StudioSurface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Track Legend & VU Indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accompaniment track indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) NeonCyan else WaveformInactive)
                )
                Text(
                    text = "伴奏轨 (inst.wav)",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            // Vocal track indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val vocalColor = when (vocalMode) {
                    VocalMode.CLONED_AI -> NeonViolet
                    VocalMode.ORIGINAL -> NeonPink
                    VocalMode.MUTED -> WaveformInactive
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) vocalColor else WaveformInactive)
                )
                Text(
                    text = when (vocalMode) {
                        VocalMode.CLONED_AI -> "AI克隆轨 (cloned.wav)"
                        VocalMode.ORIGINAL -> "原唱轨 (orig_vocal.wav)"
                        VocalMode.MUTED -> "人声静音 (KTV模式)"
                    },
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            // Mic recording indicator
            if (isRecordingMic) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(StudioGold)
                    )
                    Text(
                        text = "麦克风跟唱中",
                        color = StudioGold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Bouncing Multi-frequency Stem Waveforms Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(StudioSurfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                val width = size.width
                val height = size.height
                val barCount = waveAmplitudes.size
                val barSpacing = width / barCount
                val barWidth = barSpacing * 0.65f

                for (i in 0 until barCount) {
                    val rawAmp = waveAmplitudes.getOrElse(i) { 0.2f }
                    val activeMultiplier = if (isPlaying) pulse else 0.3f

                    // Inst bar (cyan base)
                    val instBarHeight = (rawAmp * height * 0.75f * instVolume * activeMultiplier)
                        .coerceIn(4f, height * 0.85f)

                    // Vocal bar (violet/pink layered)
                    val vocalFactor = if (vocalMode == VocalMode.MUTED) 0f else vocalVolume
                    val vocalBarHeight = (rawAmp * height * 0.85f * vocalFactor * activeMultiplier)
                        .coerceIn(3f, height * 0.95f)

                    val x = i * barSpacing + (barSpacing - barWidth) / 2f
                    val centerY = height / 2f

                    // Draw Accompaniment bar
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(NeonCyan.copy(alpha = 0.8f), NeonCyan.copy(alpha = 0.2f))
                        ),
                        topLeft = Offset(x, centerY - instBarHeight / 2f),
                        size = Size(barWidth, instBarHeight),
                        cornerRadius = CornerRadius(4f, 4f)
                    )

                    // Draw Cloned/Original Vocal bar
                    if (vocalMode != VocalMode.MUTED) {
                        val vColor = if (vocalMode == VocalMode.CLONED_AI) NeonViolet else NeonPink
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(vColor.copy(alpha = 0.9f), vColor.copy(alpha = 0.3f))
                            ),
                            topLeft = Offset(x + barWidth * 0.2f, centerY - vocalBarHeight / 2f),
                            size = Size(barWidth * 0.6f, vocalBarHeight),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                    }

                    // Draw live mic recording spike if active
                    if (isRecordingMic && micAmplitude > 0.05f) {
                        val micH = (micAmplitude * height * 0.95f).coerceIn(4f, height)
                        drawRoundRect(
                            color = StudioGold.copy(alpha = 0.85f),
                            topLeft = Offset(x + barWidth * 0.35f, centerY - micH / 2f),
                            size = Size(barWidth * 0.3f, micH),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun F0PitchCurveView(
    lyrics: List<LyricLine>,
    currentPosMs: Int,
    durationMs: Int,
    f0ConditionEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(StudioSurface)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(44.dp)) {
            val width = size.width
            val height = size.height

            // Pitch guide baseline grid
            drawLine(
                color = WaveformInactive.copy(alpha = 0.5f),
                start = Offset(0f, height * 0.5f),
                end = Offset(width, height * 0.5f),
                strokeWidth = 1f
            )

            if (!f0ConditionEnabled) {
                // Bugged mode: flat line (TTS monotone)
                drawLine(
                    color = NeonPink.copy(alpha = 0.7f),
                    start = Offset(0f, height * 0.5f),
                    end = Offset(width, height * 0.5f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
                return@Canvas
            }

            // Normal mode: dynamic melody curve (F0 pitch contour)
            val path = Path()
            val pointCount = 60
            for (i in 0 until pointCount) {
                val progress = i.toFloat() / pointCount
                val timeAtPoint = progress * durationMs
                // Synthesize smooth contour from lyrics notes
                val activeLyric = lyrics.findLast { it.timeMs <= timeAtPoint } ?: lyrics.firstOrNull()
                val notePitch = activeLyric?.notePitch ?: 64f
                val normPitch = ((notePitch - 58f) / 14f).coerceIn(0.1f, 0.9f)
                val y = height - (normPitch * height)

                val x = progress * width
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    listOf(NeonCyan, NeonViolet, NeonCyan)
                ),
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )

            // Current playhead dot
            val playheadProgress = if (durationMs > 0) (currentPosMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
            val playheadX = playheadProgress * width
            drawCircle(
                color = StudioGold,
                radius = 5.dp.toPx(),
                center = Offset(playheadX, height * 0.5f)
            )
        }
    }
}
