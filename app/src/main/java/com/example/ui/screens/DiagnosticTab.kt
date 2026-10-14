package com.example.ui.screens

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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DiagnosticReport
import com.example.model.DiagnosticStatus
import com.example.model.SeedVcConfig
import com.example.model.Song
import com.example.ui.theme.NeonCyan
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
fun DiagnosticTab(
    song: Song?,
    report: DiagnosticReport?,
    config: SeedVcConfig,
    onToggleBugWrongBinding: (Boolean) -> Unit,
    onToggleBugFakeCopy: (Boolean) -> Unit,
    onToggleBugCacheStale: (Boolean) -> Unit,
    onAutoFixAll: () -> Unit,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Master Auto-Fix Action Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = StudioSurfaceVariant
            )
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
                            text = "AI 跟唱三大必查卡点排查中心",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "精准检测并修复‘跟唱时放出来的还是原唱’典型缺陷",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Diagnostic Tool",
                        tint = StudioGold,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Button(
                    onClick = onAutoFixAll,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("auto_fix_all_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = "Auto Fix",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "一键自动修复三大卡点并重新推理",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
            }
        }

        // ==========================================
        // 1. 卡点一：播放器绑定的音轨变量检查
        // ==========================================
        DiagnosticCardItem(
            cardIndex = "1",
            title = "卡点一: 播放器绑定的音轨变量排查",
            problemDesc = "最常见低级 Bug: MP3 分离出伴奏 (inst.wav) 和原唱干声 (orig_vocal.wav)，克隆生成了 cloned_vocal.wav，但播放器代码传进去的人声路径还是写成了原唱干声！",
            status = report?.checkpoint1Status ?: DiagnosticStatus.WARNING,
            statusTitle = report?.checkpoint1Title ?: "排查中...",
            detailMessage = report?.checkpoint1Detail ?: "",
            codeSnippet = """
// ❌ 错误代码 (导致跟唱听到原唱):
player.load(inst_path, orig_vocal_path);

// ✅ 正确修复 (必须加载克隆干声):
player.load(inst_path, cloned_vocal_path);
            """.trimIndent(),
            currentTrackInfo = "当前播放器绑定音轨: [伴奏: ${report?.loadedInstFile ?: "无"}] + [人声: ${report?.loadedVocalFile ?: "无"}]",
            isSimulateBugChecked = config.simulateWrongPlayerBinding,
            onSimulateBugChanged = onToggleBugWrongBinding,
            simulateBugLabel = "模拟卡点一 Bug (故意传错原唱路径)"
        )

        // ==========================================
        // 2. 卡点二：声音克隆 (SVC) 假逻辑与模型排查
        // ==========================================
        DiagnosticCardItem(
            cardIndex = "2",
            title = "卡点二: 歌声转换 (SVC) 环节与假逻辑排查",
            problemDesc = "千万不能用普通的 TTS 做唱歌克隆！很多半成品 Demo 为了界面跑通，在克隆函数里写了假逻辑 (shutil.copy 或报错 fallback 回退原唱干声)。必须接入真实 Seed-VC 模型并开启 --f0-condition True！",
            status = report?.checkpoint2Status ?: DiagnosticStatus.WARNING,
            statusTitle = report?.checkpoint2Title ?: "排查中...",
            detailMessage = report?.checkpoint2Detail ?: "",
            codeSnippet = """
# 真实 Seed-VC 推理核心配置 (解决假逻辑):
cmd = [
    "python", "inference.py",
    "--source", orig_vocal_path,
    "--target", user_dry_voice_path,
    "--output", output_cloned_path,
    "--f0-condition", "True",     # 必须保留音高旋律!
    "--auto-f0-adjust", "True",   # 男女声音域对齐
    "--length-adjust", "1.0"      # 时长严密对齐伴奏
]
            """.trimIndent(),
            currentTrackInfo = "原唱 Hash: ${report?.origVocalHash ?: "NONE"} | 克隆 Hash: ${report?.clonedVocalHash ?: "NONE"} ${if (report?.isSameAudioHash == true) "(🚨 相同！检测到假逻辑)" else "(✅ 独立真实音色)"}",
            isSimulateBugChecked = config.simulateBugFakeCopy,
            onSimulateBugChanged = onToggleBugFakeCopy,
            simulateBugLabel = "模拟卡点二 Bug (假逻辑/直接复制原唱)"
        )

        // ==========================================
        // 3. 卡点三：前后端音频缓存与时间戳排查
        // ==========================================
        DiagnosticCardItem(
            cardIndex = "3",
            title = "卡点三: 前后端音频缓存与时间戳排查",
            problemDesc = "很多时候后端其实已经生成了 cloned_vocal.wav，但因为每次保存的文件名都叫同一个固定名字，播放器直接从磁盘或内存中播放了上一次缓存的旧音频！",
            status = report?.checkpoint3Status ?: DiagnosticStatus.WARNING,
            statusTitle = report?.checkpoint3Title ?: "排查中...",
            detailMessage = report?.checkpoint3Detail ?: "",
            codeSnippet = """
// ❌ 错误做法 (固定静态名称，引发浏览器/播放器缓存):
val path = "output/cloned_vocal.wav"

// ✅ 正确修复 (动态时间戳/随机数防重):
val path = "output/cloned_vocal_${'$'}{System.currentTimeMillis()}.wav"
            """.trimIndent(),
            currentTrackInfo = "时间戳标记: ${if (report?.hasCacheTimestamp == true) "已启用 (防缓存有效)" else "未启用 (存在缓存隐患)"} | 历史缓存文件: ${report?.cachedFileCount ?: 0} 个",
            isSimulateBugChecked = config.simulateCacheStale,
            onSimulateBugChanged = onToggleBugCacheStale,
            simulateBugLabel = "模拟卡点三 Bug (使用固定静态文件名)",
            extraAction = {
                OutlinedButton(
                    onClick = onClearCache,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cached,
                        contentDescription = "Clear Cache",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "立即清除音频缓存文件", color = NeonCyan, fontSize = 11.sp)
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DiagnosticCardItem(
    cardIndex: String,
    title: String,
    problemDesc: String,
    status: DiagnosticStatus,
    statusTitle: String,
    detailMessage: String,
    codeSnippet: String,
    currentTrackInfo: String,
    isSimulateBugChecked: Boolean,
    onSimulateBugChanged: (Boolean) -> Unit,
    simulateBugLabel: String,
    extraAction: (@Composable () -> Unit)? = null
) {
    val statusColor = when (status) {
        DiagnosticStatus.PASSED -> StudioSuccess
        DiagnosticStatus.WARNING -> StudioGold
        DiagnosticStatus.FAILED -> StudioError
    }

    val statusIcon = when (status) {
        DiagnosticStatus.PASSED -> Icons.Default.CheckCircle
        DiagnosticStatus.WARNING -> Icons.Default.Warning
        DiagnosticStatus.FAILED -> Icons.Default.Error
    }

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
            // Header with status indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cardIndex,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        text = title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Icon(
                    imageVector = statusIcon,
                    contentDescription = "Status",
                    tint = statusColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Problem Brief
            Text(
                text = problemDesc,
                color = TextSecondary,
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )

            // Live Diagnostic Evaluation Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = statusTitle,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                    Text(
                        text = detailMessage,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            // Real-time track metrics info
            Text(
                text = currentTrackInfo,
                color = NeonCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            // Code Snippet Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF07050E))
                    .padding(10.dp)
            ) {
                Text(
                    text = codeSnippet,
                    color = Color(0xFFE2E8F0),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 14.sp
                )
            }

            // Simulate Bug Toggle (for verification & education)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioSurface)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = simulateBugLabel,
                    color = if (isSimulateBugChecked) StudioError else TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = if (isSimulateBugChecked) FontWeight.SemiBold else FontWeight.Normal
                )

                Switch(
                    checked = isSimulateBugChecked,
                    onCheckedChange = onSimulateBugChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = StudioError,
                        checkedTrackColor = StudioError.copy(alpha = 0.3f)
                    )
                )
            }

            extraAction?.invoke()
        }
    }
}
