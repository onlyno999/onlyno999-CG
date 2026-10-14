package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlayerPlaybackState
import com.example.model.SeedVcConfig
import com.example.model.Song
import com.example.model.VocalMode
import com.example.ui.components.DualStemVisualizer
import com.example.ui.components.F0PitchCurveView
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioError
import com.example.ui.theme.StudioGold
import com.example.ui.theme.StudioSuccess
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.io.File

@Composable
fun PlayerTab(
    song: Song?,
    playerState: PlayerPlaybackState,
    seedVcConfig: SeedVcConfig,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onSwitchVocalMode: (VocalMode) -> Unit,
    onSetInstVolume: (Float) -> Unit,
    onSetVocalVolume: (Float) -> Unit,
    onToggleSingAlongRecording: () -> Unit,
    onNavigateToDiagnostic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showMixerPanel by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Current Song Card & Stems Meta
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = StudioCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song?.title ?: "未选择歌曲",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${song?.artist ?: "未知歌手"} • ${song?.genre ?: "流行"} • BPM ${song?.bpm ?: 90}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    // Separation badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "双轨分离就绪",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Synchronized Karaoke Lyrics Display
                val currentPos = playerState.currentPositionMs
                val lyrics = song?.lyrics ?: emptyList()
                val currentLineIndex = lyrics.indexOfLast { it.timeMs <= currentPos }.coerceAtLeast(0)
                val currentLyric = lyrics.getOrNull(currentLineIndex)?.text ?: "准备开始跟唱..."
                val nextLyric = lyrics.getOrNull(currentLineIndex + 1)?.text ?: ""

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioSurfaceVariant.copy(alpha = 0.6f))
                        .padding(vertical = 12.dp, horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = currentLyric,
                            color = StudioGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        if (nextLyric.isNotEmpty()) {
                            Text(
                                text = nextLyric,
                                color = TextTertiary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // F0 Melody pitch curve guide
                F0PitchCurveView(
                    lyrics = lyrics,
                    currentPosMs = currentPos,
                    durationMs = playerState.durationMs,
                    f0ConditionEnabled = seedVcConfig.f0Condition
                )
            }
        }

        // 2. Real-Time Audio Track Binding Inspector (Addresses Checkpoint 1 directly!)
        val loadedVocalName = if (playerState.activeVocalPath.isNotEmpty()) {
            File(playerState.activeVocalPath).name
        } else "无"

        val isBuggedBinding = seedVcConfig.simulateWrongPlayerBinding ||
                (loadedVocalName.contains("orig_vocal") && playerState.vocalMode == VocalMode.CLONED_AI)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isBuggedBinding) StudioError.copy(alpha = 0.12f) else StudioSurface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Track Path",
                            tint = if (isBuggedBinding) StudioError else NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "播放器实时绑定音轨检查 (卡点一排查)",
                            color = if (isBuggedBinding) StudioError else TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    Text(
                        text = if (isBuggedBinding) "❌ 绑定错误 (原唱)" else "✅ 绑定正常",
                        color = if (isBuggedBinding) StudioError else StudioSuccess,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                (if (isBuggedBinding) StudioError else StudioSuccess).copy(alpha = 0.15f)
                            )
                            .clickable { onNavigateToDiagnostic() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "伴奏轨: ${File(playerState.activeInstPath).name.ifEmpty { "inst.wav" }}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "人声轨: $loadedVocalName",
                        color = if (isBuggedBinding) StudioError else NeonViolet,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 3. Dual Stem Animated Visualizer
        DualStemVisualizer(
            isPlaying = playerState.isPlaying,
            vocalMode = playerState.vocalMode,
            waveAmplitudes = playerState.waveAmplitudes,
            instVolume = playerState.instVolume,
            vocalVolume = playerState.vocalVolume,
            isRecordingMic = playerState.isRecordingMic,
            micAmplitude = playerState.micAmplitude
        )

        // 4. Vocal Track Switcher Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StudioCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "跟唱人声音色切换",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "自由对比克隆与原唱",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VocalMode.values().forEach { mode ->
                        val isSelected = playerState.vocalMode == mode
                        val btnColor = when (mode) {
                            VocalMode.CLONED_AI -> NeonViolet
                            VocalMode.ORIGINAL -> NeonPink
                            VocalMode.MUTED -> StudioGold
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) btnColor.copy(alpha = 0.22f) else StudioSurface
                                )
                                .clickable { onSwitchVocalMode(mode) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = mode.label,
                                    color = if (isSelected) btnColor else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = when (mode) {
                                        VocalMode.CLONED_AI -> "用户音色"
                                        VocalMode.ORIGINAL -> "原版音轨"
                                        VocalMode.MUTED -> "伴奏独播"
                                    },
                                    color = if (isSelected) btnColor.copy(alpha = 0.8f) else TextTertiary,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Dual Stem Volume Mixer Sliders
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StudioCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "双轨混音台 (Mixer Console)",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "伴奏: ${(playerState.instVolume * 100).toInt()}% | 人声: ${(playerState.vocalVolume * 100).toInt()}%",
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                }

                // Instrumental volume slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "伴奏", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(32.dp))
                    Slider(
                        value = playerState.instVolume,
                        onValueChange = onSetInstVolume,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = StudioSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Vocal volume slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "人声", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(32.dp))
                    Slider(
                        value = playerState.vocalVolume,
                        onValueChange = onSetVocalVolume,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonViolet,
                            activeTrackColor = NeonViolet,
                            inactiveTrackColor = StudioSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 6. Timeline Progress Slider
        val currentMs = playerState.currentPositionMs
        val durationMs = playerState.durationMs.coerceAtLeast(1)
        val progress = (currentMs.toFloat() / durationMs).coerceIn(0f, 1f)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Slider(
                value = progress,
                onValueChange = { newProgress ->
                    onSeekTo((newProgress * durationMs).toInt())
                },
                colors = SliderDefaults.colors(
                    thumbColor = StudioGold,
                    activeTrackColor = StudioGold,
                    inactiveTrackColor = StudioSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatTime(currentMs), color = TextTertiary, fontSize = 11.sp)
                Text(text = formatTime(durationMs), color = TextTertiary, fontSize = 11.sp)
            }
        }

        // 7. Master Playback & Sing-Along Recording Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Replay / Rewind button
            IconButton(
                onClick = { onSeekTo(0) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = "Rewind",
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Main Play/Pause Button
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(NeonCyan, NeonViolet)
                        )
                    )
                    .clickable { onTogglePlayPause() }
                    .testTag("play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Mic Sing-Along Recording Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (playerState.isRecordingMic) StudioError else StudioGold.copy(alpha = 0.2f)
                    )
                    .clickable { onToggleSingAlongRecording() }
                    .testTag("sing_along_mic_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (playerState.isRecordingMic) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Record Sing-Along",
                    tint = if (playerState.isRecordingMic) Color.White else StudioGold,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

private fun formatTime(ms: Int): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}
