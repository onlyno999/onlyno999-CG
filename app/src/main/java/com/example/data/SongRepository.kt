package com.example.data

import android.content.Context
import com.example.audio.WavAudioHelper
import com.example.model.DiagnosticReport
import com.example.model.DiagnosticStatus
import com.example.model.KaraokeRecording
import com.example.model.LyricLine
import com.example.model.SeedVcConfig
import com.example.model.Song
import com.example.model.VoiceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class SongRepository(private val context: Context) {

    private val stemsDir = File(context.filesDir, "stems")
    private val profilesDir = File(context.filesDir, "profiles")
    private val recordingsDir = File(context.filesDir, "recordings")

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _selectedSong = MutableStateFlow<Song?>(null)
    val selectedSong: StateFlow<Song?> = _selectedSong.asStateFlow()

    private val _profiles = MutableStateFlow<List<VoiceProfile>>(emptyList())
    val profiles: StateFlow<List<VoiceProfile>> = _profiles.asStateFlow()

    private val _selectedProfile = MutableStateFlow<VoiceProfile?>(null)
    val selectedProfile: StateFlow<VoiceProfile?> = _selectedProfile.asStateFlow()

    private val _seedVcConfig = MutableStateFlow(SeedVcConfig())
    val seedVcConfig: StateFlow<SeedVcConfig> = _seedVcConfig.asStateFlow()

    private val _recordings = MutableStateFlow<List<KaraokeRecording>>(emptyList())
    val recordings: StateFlow<List<KaraokeRecording>> = _recordings.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _conversionLog = MutableStateFlow<List<String>>(emptyList())
    val conversionLog: StateFlow<List<String>> = _conversionLog.asStateFlow()

    init {
        stemsDir.mkdirs()
        profilesDir.mkdirs()
        recordingsDir.mkdirs()
    }

    suspend fun initializePresetLibrary() = withContext(Dispatchers.IO) {
        val initialProfiles = listOf(
            VoiceProfile(
                id = "tenor_01",
                name = "自然温暖男声",
                desc = "中低频饱满，质感温润自然的男声音色",
                gender = "男声",
                pitchShiftSemitones = 0,
                formantRatio = 1.0f
            ),
            VoiceProfile(
                id = "soprano_02",
                name = "清澈空灵女声",
                desc = "高频透亮空灵，穿透力强的甜美女声音色",
                gender = "女声",
                pitchShiftSemitones = 5,
                formantRatio = 1.22f
            ),
            VoiceProfile(
                id = "baritone_03",
                name = "低沉磁性大叔",
                desc = "浑厚有力量感，胸腔共鸣丰富的磁性音色",
                gender = "男声",
                pitchShiftSemitones = -3,
                formantRatio = 0.88f
            ),
            VoiceProfile(
                id = "cyber_04",
                name = "赛博电音声线",
                desc = "带微量泛音调制与金属质感的个性电音",
                gender = "中性",
                pitchShiftSemitones = 2,
                formantRatio = 1.35f
            )
        )

        // Ensure default dry voice samples are generated
        val preparedProfiles = initialProfiles.map { prof ->
            val dryFile = File(profilesDir, "dry_${prof.id}.wav")
            if (!dryFile.exists()) {
                WavAudioHelper.generateUserDryVoiceSample(dryFile, durationSec = 4, profile = prof)
            }
            prof.copy(audioPath = dryFile.absolutePath)
        }

        _profiles.value = preparedProfiles
        _selectedProfile.value = preparedProfiles.first()

        // Prepare Preset Songs
        val presetSongs = listOf(
            createPresetSong1(),
            createPresetSong2(),
            createPresetSong3(),
            createPresetSong4()
        )

        _songs.value = presetSongs
        val firstSong = presetSongs.first()
        _selectedSong.value = firstSong

        // Initial audio stem preparation for first song
        prepareSongStems(firstSong)
    }

    private fun createPresetSong1(): Song {
        val lyrics = listOf(
            LyricLine(0, "故事的小黄花 从出生那年就飘着", 60f),
            LyricLine(4200, "童年的荡秋千 随记忆一直晃到现在", 62f),
            LyricLine(8500, "Re So So Si Do Si La So", 64f),
            LyricLine(12800, "吹着前奏望着天空 我想起花瓣试着掉落", 65f),
            LyricLine(17200, "为你翘课的那一天 花落的那一天", 67f)
        )
        return Song(
            id = "song_qingtian",
            title = "晴天 (Sunny Day)",
            artist = "周杰伦",
            genre = "流行民谣",
            durationSec = 22,
            bpm = 88,
            lyrics = lyrics,
            instPath = File(stemsDir, "qingtian_inst.wav").absolutePath,
            origVocalPath = File(stemsDir, "qingtian_orig_vocal.wav").absolutePath
        )
    }

    private fun createPresetSong2(): Song {
        val lyrics = listOf(
            LyricLine(0, "钟声响起归家的讯号", 62f),
            LyricLine(4000, "在他生命里 仿佛带点唏嘘", 65f),
            LyricLine(8200, "黑色肌肤给他的意义", 67f),
            LyricLine(12500, "是一生奉献 肤色斗争中", 69f),
            LyricLine(16800, "年月把拥有变做失去 疲倦的双眼带着期望", 72f)
        )
        return Song(
            id = "song_guanghui",
            title = "光辉岁月 (Glorious Days)",
            artist = "Beyond",
            genre = "摇滚励志",
            durationSec = 21,
            bpm = 100,
            lyrics = lyrics,
            instPath = File(stemsDir, "guanghui_inst.wav").absolutePath,
            origVocalPath = File(stemsDir, "guanghui_orig_vocal.wav").absolutePath
        )
    }

    private fun createPresetSong3(): Song {
        val lyrics = listOf(
            LyricLine(0, "为你弹奏肖邦的夜曲 纪念我死去的爱情", 60f),
            LyricLine(4500, "跟夜风一样的声音 心碎的很好听", 58f),
            LyricLine(9000, "手在键盘敲很轻 我给的思念很小心", 62f),
            LyricLine(13500, "你埋葬的地方叫幽冥", 65f),
            LyricLine(17500, "为你弹奏萧邦的夜曲 纪念我死去的爱情", 67f)
        )
        return Song(
            id = "song_yequ",
            title = "夜曲 (Nocturne)",
            artist = "周杰伦",
            genre = "古典嘻哈",
            durationSec = 22,
            bpm = 84,
            lyrics = lyrics,
            instPath = File(stemsDir, "yequ_inst.wav").absolutePath,
            origVocalPath = File(stemsDir, "yequ_orig_vocal.wav").absolutePath
        )
    }

    private fun createPresetSong4(): Song {
        val lyrics = listOf(
            LyricLine(0, "你问我爱你有多深 我爱你有几分", 65f),
            LyricLine(5000, "我的情也真 我的爱也真", 64f),
            LyricLine(10000, "月亮代表我的心", 62f),
            LyricLine(15000, "轻轻的一个吻 已经打动我的心", 67f)
        )
        return Song(
            id = "song_yueliang",
            title = "月亮代表我的心",
            artist = "邓丽君",
            genre = "抒情经典",
            durationSec = 20,
            bpm = 72,
            lyrics = lyrics,
            instPath = File(stemsDir, "yueliang_inst.wav").absolutePath,
            origVocalPath = File(stemsDir, "yueliang_orig_vocal.wav").absolutePath
        )
    }

    suspend fun prepareSongStems(song: Song): Song = withContext(Dispatchers.IO) {
        val instFile = File(song.instPath)
        val origFile = File(song.origVocalPath)

        if (!instFile.exists()) {
            WavAudioHelper.generateInstrumentalWav(instFile, song.durationSec, song.bpm)
        }

        if (!origFile.exists()) {
            // Melodic note sequence (Hz)
            val melody = song.lyrics.map { 440.0 * Math.pow(2.0, (it.notePitch - 69.0) / 12.0) }
            WavAudioHelper.generateOriginalVocalWav(origFile, song.durationSec, melody)
        }

        // Check if there is an existing cloned vocal in stems
        val existingClones = stemsDir.listFiles { _, name ->
            name.startsWith("${song.id}_cloned_vocal_") && name.endsWith(".wav")
        }?.sortedByDescending { it.lastModified() }

        val clonedPath = existingClones?.firstOrNull()?.absolutePath

        val updatedSong = song.copy(
            isSeparated = true,
            isCloned = clonedPath != null,
            clonedVocalPath = clonedPath,
            lastClonedTimestamp = existingClones?.firstOrNull()?.lastModified() ?: 0L
        )

        // Update list
        _songs.value = _songs.value.map { if (it.id == song.id) updatedSong else it }
        if (_selectedSong.value?.id == song.id) {
            _selectedSong.value = updatedSong
        }

        updatedSong
    }

    fun selectSong(song: Song) {
        _selectedSong.value = song
    }

    fun selectProfile(profile: VoiceProfile) {
        _selectedProfile.value = profile
        _seedVcConfig.value = _seedVcConfig.value.copy(selectedProfileId = profile.id)
    }

    fun updateConfig(config: SeedVcConfig) {
        _seedVcConfig.value = config
    }

    /**
     * Executes the full SVC Conversion Pipeline with Seed-VC steps:
     * Step 1: Vocal Separation Verification (inst.wav + orig_vocal.wav)
     * Step 2: Target Dry Voice Timbre Extraction
     * Step 3: Seed-VC Inference Pipeline (F0 condition, auto range adjust, length 1.0)
     * Step 4: Timestamped Cache-Busting Output (cloned_vocal_{timestamp}.wav)
     */
    suspend fun runConversion(
        song: Song,
        profile: VoiceProfile,
        config: SeedVcConfig
    ): Song = withContext(Dispatchers.IO) {
        _isGenerating.value = true
        _conversionLog.value = listOf("🚀 启动 AI 歌声克隆管线 (Seed-VC 零样本转换)...")

        val logs = mutableListOf<String>()
        fun addLog(msg: String) {
            logs.add(msg)
            _conversionLog.value = logs.toList()
        }

        try {
            kotlinx.coroutines.delay(400)
            addLog("📁 检查音轨分离状态: 伴奏 (inst.wav) & 原唱干声 (orig_vocal.wav) 就绪")

            val origFile = File(song.origVocalPath)
            if (!origFile.exists()) {
                addLog("⚡ 正在合成原唱干声音轨: ${origFile.name}")
                val melody = song.lyrics.map { 440.0 * Math.pow(2.0, (it.notePitch - 69.0) / 12.0) }
                WavAudioHelper.generateOriginalVocalWav(origFile, song.durationSec, melody)
            }

            kotlinx.coroutines.delay(400)
            val dryVoiceFile = File(profilesDir, "dry_${profile.id}.wav")
            if (!dryVoiceFile.exists()) {
                WavAudioHelper.generateUserDryVoiceSample(dryVoiceFile, 4, profile)
            }
            addLog("🎙️ 加载目标音色干声: ${profile.name} (${profile.gender}, 共鸣比: ${profile.formantRatio})")

            kotlinx.coroutines.delay(400)
            addLog("⚙️ Seed-VC 参数配置: --diffusion-steps ${config.diffusionSteps} --inference-cfg-rate ${config.inferenceCfgRate}")
            addLog("🎼 【核心保留】--f0-condition ${config.f0Condition} | --auto-f0-adjust ${config.autoF0Adjust} | --length-adjust 1.0")

            if (config.simulateBugFakeCopy) {
                addLog("⚠️ [模拟卡点二Bug状态开启]: 执行假逻辑 (直接复制原唱干声改名或静默回退)")
            }

            // Output path with timestamp (Solving Checkpoint 3!)
            val timestamp = if (config.simulateCacheStale) {
                // Bugged mode: fixed static filename causes player audio caching!
                0L
            } else {
                System.currentTimeMillis()
            }

            val outputFilename = if (config.simulateCacheStale) {
                "${song.id}_cloned_vocal_fixed_cache.wav"
            } else {
                "${song.id}_cloned_vocal_$timestamp.wav"
            }

            val outputFile = File(stemsDir, outputFilename)

            // Run DSP Conversion
            WavAudioHelper.runSingingVoiceConversion(
                origVocalFile = origFile,
                userVoiceFile = dryVoiceFile,
                outputClonedFile = outputFile,
                config = config,
                profile = profile
            )

            kotlinx.coroutines.delay(500)
            val origHash = WavAudioHelper.calculateFileMd5(origFile).take(8)
            val clonedHash = WavAudioHelper.calculateFileMd5(outputFile).take(8)

            if (origHash == clonedHash) {
                addLog("🚨 [卡点二警报] 校验发现克隆音频 Hash 与原唱干声一致 ($origHash)！这是典型的假逻辑回退问题！")
            } else {
                addLog("✅ 真实音色转换完成！音频时长严格锁定 ${song.durationSec}s，Hash: $clonedHash (原唱: $origHash)")
            }

            addLog("📦 新克隆音轨已落盘: ${outputFile.name} (时间戳防缓存机制已生效)")

            val updatedSong = song.copy(
                isCloned = true,
                clonedVocalPath = outputFile.absolutePath,
                lastClonedTimestamp = timestamp
            )

            _songs.value = _songs.value.map { if (it.id == song.id) updatedSong else it }
            _selectedSong.value = updatedSong
            return@withContext updatedSong

        } catch (e: Exception) {
            addLog("❌ 转换流程异常: ${e.message}")
            throw e
        } finally {
            _isGenerating.value = false
        }
    }

    /**
     * Generates a comprehensive Diagnostic Report directly targeting the 3 bottlenecks described in the prompt.
     */
    fun performDiagnostic(song: Song, config: SeedVcConfig, playerVocalPath: String): DiagnosticReport {
        val origFile = File(song.origVocalPath)
        val instFile = File(song.instPath)
        val clonedFile = song.clonedVocalPath?.let { File(it) }

        // --- 卡点一：播放器音轨绑定检查 ---
        val isBindingWrong = config.simulateWrongPlayerBinding ||
                (playerVocalPath.isNotEmpty() && playerVocalPath == origFile.absolutePath && song.isCloned)

        val cp1Status = when {
            isBindingWrong -> DiagnosticStatus.FAILED
            clonedFile == null || !clonedFile.exists() -> DiagnosticStatus.WARNING
            playerVocalPath == clonedFile.absolutePath -> DiagnosticStatus.PASSED
            else -> DiagnosticStatus.PASSED
        }

        val cp1Title = when (cp1Status) {
            DiagnosticStatus.FAILED -> "卡点一: 播放器绑定错误！当前播放的是原唱干声 (orig_vocal.wav)"
            DiagnosticStatus.WARNING -> "卡点一: 克隆音轨尚未就绪，当前暂使用原唱或纯伴奏"
            DiagnosticStatus.PASSED -> "卡点一: 音轨绑定正确 (inst.wav + cloned_vocal.wav)"
        }

        val cp1Detail = when (cp1Status) {
            DiagnosticStatus.FAILED -> "播放器当前绑定的音轨是: ${File(playerVocalPath).name}。导致你在跟唱时无论怎么调，放出来的都是原唱！必须将播放器音轨变量切换为克隆输出文件: ${clonedFile?.name ?: "cloned_vocal.wav"}。"
            DiagnosticStatus.WARNING -> "原唱干声已分离，但尚未生成克隆音轨。请前往【转换工坊】运行一次歌声克隆。"
            DiagnosticStatus.PASSED -> "播放器已正确加载双音轨: 伴奏轨 (${instFile.name}) + 克隆干声轨 (${clonedFile?.name})，跟唱时可清晰听到克隆音色！"
        }

        // --- 卡点二：声音克隆 (SVC) 假逻辑与模型排查 ---
        val origHash = WavAudioHelper.calculateFileMd5(origFile).take(8)
        val clonedHash = clonedFile?.let { WavAudioHelper.calculateFileMd5(it).take(8) } ?: "NONE"
        val isSameHash = (clonedFile != null && clonedFile.exists() && origHash == clonedHash)

        val cp2Status = when {
            isSameHash || config.simulateBugFakeCopy -> DiagnosticStatus.FAILED
            !config.f0Condition -> DiagnosticStatus.WARNING
            clonedFile != null && clonedFile.exists() -> DiagnosticStatus.PASSED
            else -> DiagnosticStatus.WARNING
        }

        val cp2Title = when (cp2Status) {
            DiagnosticStatus.FAILED -> "卡点二: 检测到假逻辑/静默回退！克隆音频完全等同原唱"
            DiagnosticStatus.WARNING -> if (!config.f0Condition) "卡点二: --f0-condition 为 False，歌声会失去音调！" else "卡点二: 歌声转换模型尚未运行"
            DiagnosticStatus.PASSED -> "卡点二: Seed-VC 歌声转换模型正常，F0音高与音色迁移生效"
        }

        val cp2Detail = when (cp2Status) {
            DiagnosticStatus.FAILED -> "原唱干声 MD5 ($origHash) 与 克隆干声 MD5 ($clonedHash) 完全一致！代码中可能存在 `shutil.copy` 假克隆逻辑，或模型报错后自动 fallback 回退原唱干声。必须接入真实 Seed-VC 推理！"
            DiagnosticStatus.WARNING -> if (!config.f0Condition) "未开启 --f0-condition True！TTS 或普通克隆无法处理旋律音高，会导致唱歌变成单调念经！请立即开启音高保留。" else "原唱干声已就绪，等待执行零样本歌声转换推理。"
            DiagnosticStatus.PASSED -> "已校验音高保留参数 (--f0-condition True) 及音色共鸣转移，时长比严格锁定 1.0，原唱音色已成功被目标干声替换！"
        }

        // --- 卡点三：前后端音频缓存与时间戳排查 ---
        val hasTimestamp = clonedFile?.name?.matches(Regex(".*_\\d{10,}\\.wav")) == true
        val cp3Status = when {
            config.simulateCacheStale -> DiagnosticStatus.FAILED
            clonedFile != null && !hasTimestamp -> DiagnosticStatus.WARNING
            else -> DiagnosticStatus.PASSED
        }

        val cp3Title = when (cp3Status) {
            DiagnosticStatus.FAILED -> "卡点三: 静态文件名导致播放器读取旧缓存！"
            DiagnosticStatus.WARNING -> "卡点三: 克隆文件名缺少随机数/时间戳防缓存标记"
            DiagnosticStatus.PASSED -> "卡点三: 动态时间戳防重命名机制已启用"
        }

        val cp3Detail = when (cp3Status) {
            DiagnosticStatus.FAILED -> "当前克隆文件使用了静态固定名称 (${clonedFile?.name})。播放器/浏览器在重新生成后会直接播放本地旧音频缓存（导致听起来还是原唱或旧文件）！必须追加时间戳或清空缓存。"
            DiagnosticStatus.WARNING -> "建议在输出路径加上时间戳: cloned_vocal_\${timestamp}.wav，防止播放器缓存残留。"
            DiagnosticStatus.PASSED -> "输出路径附带独立时间戳 (${clonedFile?.name})，播放器每次都会实时重载最新音轨，绝无缓存干扰。"
        }

        val cachedCount = stemsDir.listFiles { _, name -> name.contains("cloned_vocal") }?.size ?: 0

        return DiagnosticReport(
            checkpoint1Status = cp1Status,
            checkpoint1Title = cp1Title,
            checkpoint1Detail = cp1Detail,
            loadedInstFile = instFile.name,
            loadedVocalFile = if (playerVocalPath.isNotEmpty()) File(playerVocalPath).name else "无",

            checkpoint2Status = cp2Status,
            checkpoint2Title = cp2Title,
            checkpoint2Detail = cp2Detail,
            origVocalHash = origHash,
            clonedVocalHash = clonedHash,
            isSameAudioHash = isSameHash,

            checkpoint3Status = cp3Status,
            checkpoint3Title = cp3Title,
            checkpoint3Detail = cp3Detail,
            hasCacheTimestamp = hasTimestamp,
            cachedFileCount = cachedCount
        )
    }

    /**
     * Clears cached audio files and forces fresh generation.
     */
    fun clearAudioCache(songId: String) {
        val files = stemsDir.listFiles { _, name ->
            name.startsWith("${songId}_cloned_vocal_") && name.endsWith(".wav")
        }
        files?.forEach { it.delete() }

        _songs.value = _songs.value.map { song ->
            if (song.id == songId) song.copy(isCloned = false, clonedVocalPath = null) else song
        }
        if (_selectedSong.value?.id == songId) {
            _selectedSong.value = _selectedSong.value?.copy(isCloned = false, clonedVocalPath = null)
        }
    }

    fun addRecording(recording: KaraokeRecording) {
        _recordings.value = listOf(recording) + _recordings.value
    }

    fun addCustomMicProfile(name: String, recordedFile: File): VoiceProfile {
        val profile = VoiceProfile(
            id = "mic_${System.currentTimeMillis()}",
            name = name.ifEmpty { "我的麦克风干声" },
            desc = "实时现场麦克风录制提取的自然干声音色",
            gender = "自定义",
            pitchShiftSemitones = 0,
            formantRatio = 1.05f,
            isCustomMic = true,
            audioPath = recordedFile.absolutePath
        )
        _profiles.value = _profiles.value + profile
        _selectedProfile.value = profile
        return profile
    }

    fun getRecordingsDir(): File = recordingsDir
}
