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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SeedVcConfig
import com.example.model.Song
import com.example.model.VoiceProfile
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioError
import com.example.ui.theme.StudioGold
import com.example.ui.theme.StudioSuccess
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.StudioWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun SvcStudioTab(
    song: Song?,
    profiles: List<VoiceProfile>,
    selectedProfile: VoiceProfile?,
    config: SeedVcConfig,
    isGenerating: Boolean,
    conversionLog: List<String>,
    onSelectProfile: (VoiceProfile) -> Unit,
    onUpdateConfig: (SeedVcConfig) -> Unit,
    onStartConversion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showAdvancedParams by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Banner explaining Seed-VC vs standard TTS
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = NeonViolet.copy(alpha = 0.12f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "SVC Concept",
                    tint = NeonViolet,
                    modifier = Modifier.size(20.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "为什么必须使用 SVC (歌声转换) 而非 TTS？",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "普通的 TTS（如 CosyVoice / Edge-TTS）只负责文字朗读，不懂音阶旋律。要将原唱变成自己的声音，必须使用 Seed-VC 这类零样本歌声转换模型，严格锁定 F0 音高曲线与 1.0 时长比例！",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 1. Pipeline Steps Overview Card
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
                Text(
                    text = "AI 歌声转换推理管线",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                // Step 1: Track separation
                PipelineStepItem(
                    stepNum = "1",
                    title = "音源分离 (Source Separation)",
                    subtitle = "原曲拆解为伴奏轨 (inst.wav) + 原唱干声 (orig_vocal.wav)",
                    isCompleted = song?.isSeparated == true,
                    activeColor = NeonCyan
                )

                // Step 2: Target voice timbre
                PipelineStepItem(
                    stepNum = "2",
                    title = "目标音色干声采样 (Target Timbre)",
                    subtitle = "当前选择: ${selectedProfile?.name ?: "默认男声"} (${selectedProfile?.gender})",
                    isCompleted = selectedProfile != null,
                    activeColor = NeonViolet
                )

                // Step 3: Seed-VC conversion
                PipelineStepItem(
                    stepNum = "3",
                    title = "Seed-VC 零样本转换 (SVC Inference)",
                    subtitle = "--f0-condition ${config.f0Condition} | --length-adjust 1.0 | Steps: ${config.diffusionSteps}",
                    isCompleted = song?.isCloned == true,
                    activeColor = StudioGold
                )

                // Step 4: Timestamp output
                PipelineStepItem(
                    stepNum = "4",
                    title = "双轨跟唱加载 (Cache-Busting Output)",
                    subtitle = "生成带时间戳克隆干声并绑定至跟唱播放器",
                    isCompleted = song?.isCloned == true,
                    activeColor = StudioSuccess
                )
            }
        }

        // 2. Target Voice Profile Selector
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
                        text = "选择克隆目标音色 (Dry Voice)",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${profiles.size} 种音色预设",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    profiles.forEach { prof ->
                        val isSelected = selectedProfile?.id == prof.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) NeonViolet.copy(alpha = 0.2f) else StudioSurface)
                                .clickable { onSelectProfile(prof) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) NeonViolet else StudioSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (prof.isCustomMic) Icons.Default.Mic else Icons.Default.GraphicEq,
                                        contentDescription = prof.name,
                                        tint = if (isSelected) Color.White else TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = prof.name,
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = prof.desc,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = NeonViolet,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Seed-VC Core Parameters Card (Mandatory F0 condition & adjustments)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StudioCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Seed-VC 推理参数配置",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Row(
                        modifier = Modifier
                            .clickable { showAdvancedParams = !showAdvancedParams }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showAdvancedParams) "收起高级项" else "展开高级项",
                            color = NeonCyan,
                            fontSize = 11.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Toggle",
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // A. --f0-condition (MANDATORY for singing!)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(StudioSurface)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "--f0-condition (音高保留)",
                            color = if (config.f0Condition) StudioSuccess else StudioWarning,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (config.f0Condition) "【极关键】已开启，完整保留歌曲旋律与转音唱腔" else "❌ 已关闭！歌声将失去音高，退化为单调读词！",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }

                    Switch(
                        checked = config.f0Condition,
                        onCheckedChange = { onUpdateConfig(config.copy(f0Condition = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StudioSuccess,
                            checkedTrackColor = StudioSuccess.copy(alpha = 0.3f)
                        )
                    )
                }

                // B. --auto-f0-adjust
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(StudioSurface)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "--auto-f0-adjust (音域自动对齐)",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "男女声音域跨度自动八度移频对齐",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }

                    Switch(
                        checked = config.autoF0Adjust,
                        onCheckedChange = { onUpdateConfig(config.copy(autoF0Adjust = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = NeonCyan.copy(alpha = 0.3f)
                        )
                    )
                }

                // Advanced items
                AnimatedVisibility(visible = showAdvancedParams) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // C. --diffusion-steps
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "--diffusion-steps (扩散步数)", color = TextSecondary, fontSize = 12.sp)
                                Text(text = "${config.diffusionSteps} 步", color = StudioGold, fontSize = 12.sp)
                            }
                            Slider(
                                value = config.diffusionSteps.toFloat(),
                                onValueChange = { onUpdateConfig(config.copy(diffusionSteps = it.toInt())) },
                                valueRange = 15f..50f,
                                steps = 35,
                                colors = SliderDefaults.colors(
                                    thumbColor = StudioGold,
                                    activeTrackColor = StudioGold,
                                    inactiveTrackColor = StudioSurfaceVariant
                                )
                            )
                        }

                        // D. --inference-cfg-rate
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "--inference-cfg-rate (引导比率)", color = TextSecondary, fontSize = 12.sp)
                                Text(text = "%.2f".format(config.inferenceCfgRate), color = NeonViolet, fontSize = 12.sp)
                            }
                            Slider(
                                value = config.inferenceCfgRate,
                                onValueChange = { onUpdateConfig(config.copy(inferenceCfgRate = it)) },
                                valueRange = 0.2f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonViolet,
                                    activeTrackColor = NeonViolet,
                                    inactiveTrackColor = StudioSurfaceVariant
                                )
                            )
                        }

                        // E. --length-adjust (Strictly 1.0)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(StudioSurface)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "--length-adjust", color = TextSecondary, fontSize = 11.sp)
                            Text(text = "1.0 (与伴奏对齐严格锁定)", color = StudioSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 4. Start Conversion Primary Action Button
        Button(
            onClick = onStartConversion,
            enabled = !isGenerating,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("start_conversion_btn"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonViolet)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Seed-VC 推理执行中...", color = Color.White, fontWeight = FontWeight.Bold)
            } else {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Convert",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "开始执行 AI 歌声克隆转换",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        // 5. Live Pipeline Console Logs
        if (conversionLog.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF07050E))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "管线终端运行日志 (Terminal Log)",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(text = "Seed-VC Engine", color = TextTertiary, fontSize = 10.sp)
                    }

                    conversionLog.takeLast(6).forEach { logLine ->
                        Text(
                            text = logLine,
                            color = when {
                                logLine.contains("❌") || logLine.contains("🚨") -> StudioError
                                logLine.contains("⚠️") -> StudioGold
                                logLine.contains("✅") -> StudioSuccess
                                else -> TextSecondary
                            },
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun PipelineStepItem(
    stepNum: String,
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    activeColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (isCompleted) activeColor else StudioSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Done",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = stepNum,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isCompleted) TextPrimary else TextSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
            Text(
                text = subtitle,
                color = TextTertiary,
                fontSize = 10.sp
            )
        }
    }
}
