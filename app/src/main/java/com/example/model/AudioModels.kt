package com.example.model

enum class StemType {
    INSTRUMENTAL,
    ORIG_VOCAL,
    CLONED_VOCAL,
    USER_DRY_VOICE,
    LIVE_MIC
}

enum class VocalMode(val label: String, val tag: String) {
    CLONED_AI("AI克隆音色", "CLONED"),
    ORIGINAL("原唱音色", "ORIGINAL"),
    MUTED("纯伴奏 (KTV)", "MUTED")
}

data class LyricLine(
    val timeMs: Long,
    val text: String,
    val notePitch: Float // MIDI pitch or relative note frequency (e.g. 60.0 = Middle C)
)

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val genre: String,
    val durationSec: Int,
    val bpm: Int,
    val lyrics: List<LyricLine>,
    val instPath: String,
    val origVocalPath: String,
    val clonedVocalPath: String? = null,
    val lastClonedTimestamp: Long = 0L,
    val isSeparated: Boolean = true,
    val isCloned: Boolean = false
)

data class SeedVcConfig(
    val f0Condition: Boolean = true,       // --f0-condition True: 保留原唱旋律音高
    val autoF0Adjust: Boolean = true,      // --auto-f0-adjust True: 自动对齐男女声音域
    val diffusionSteps: Int = 30,          // --diffusion-steps 30: 推理步数 25~50
    val lengthAdjust: Float = 1.0f,        // --length-adjust 1.0: 严格保持音频时长一致
    val inferenceCfgRate: Float = 0.7f,    // --inference-cfg-rate 0.7: 引导强度
    val selectedProfileId: String = "tenor_01",
    // 模拟测试开关：用于让用户直观复现与修复三大卡点
    val simulateWrongPlayerBinding: Boolean = false, // 卡点一：播放器错误绑定回原唱干声
    val simulateBugFakeCopy: Boolean = false,        // 卡点二：假逻辑/报错静默回退原唱
    val simulateCacheStale: Boolean = false          // 卡点三：不加时间戳导致缓存复用
)

data class VoiceProfile(
    val id: String,
    val name: String,
    val desc: String,
    val gender: String,
    val pitchShiftSemitones: Int,
    val formantRatio: Float,
    val isCustomMic: Boolean = false,
    val audioPath: String? = null
)

enum class DiagnosticStatus {
    PASSED,
    WARNING,
    FAILED
}

data class DiagnosticReport(
    val checkpoint1Status: DiagnosticStatus,
    val checkpoint1Title: String,
    val checkpoint1Detail: String,
    val loadedInstFile: String,
    val loadedVocalFile: String,

    val checkpoint2Status: DiagnosticStatus,
    val checkpoint2Title: String,
    val checkpoint2Detail: String,
    val origVocalHash: String,
    val clonedVocalHash: String,
    val isSameAudioHash: Boolean,

    val checkpoint3Status: DiagnosticStatus,
    val checkpoint3Title: String,
    val checkpoint3Detail: String,
    val hasCacheTimestamp: Boolean,
    val cachedFileCount: Int
)

data class KaraokeRecording(
    val id: String,
    val songTitle: String,
    val filePath: String,
    val recordedAt: Long,
    val durationMs: Long,
    val vocalModeUsed: VocalMode,
    val score: Int
)
