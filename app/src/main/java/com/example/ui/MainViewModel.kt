package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.DualTrackPlayer
import com.example.audio.PlayerPlaybackState
import com.example.data.SongRepository
import com.example.model.DiagnosticReport
import com.example.model.KaraokeRecording
import com.example.model.SeedVcConfig
import com.example.model.Song
import com.example.model.VocalMode
import com.example.model.VoiceProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SongRepository(application)
    val player = DualTrackPlayer(application, viewModelScope)

    val songs: StateFlow<List<Song>> = repository.songs
    val selectedSong: StateFlow<Song?> = repository.selectedSong
    val profiles: StateFlow<List<VoiceProfile>> = repository.profiles
    val selectedProfile: StateFlow<VoiceProfile?> = repository.selectedProfile
    val seedVcConfig: StateFlow<SeedVcConfig> = repository.seedVcConfig
    val recordings: StateFlow<List<KaraokeRecording>> = repository.recordings
    val isGenerating: StateFlow<Boolean> = repository.isGenerating
    val conversionLog: StateFlow<List<String>> = repository.conversionLog

    val playerState: StateFlow<PlayerPlaybackState> = player.state

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Real-time dynamic diagnostic report combined from current song, player, and config
    val diagnosticReport: StateFlow<DiagnosticReport?> = combine(
        selectedSong,
        seedVcConfig,
        playerState
    ) { song, config, pState ->
        if (song != null) {
            repository.performDiagnostic(song, config, pState.activeVocalPath)
        } else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            repository.initializePresetLibrary()
            selectedSong.value?.let { song ->
                loadSongIntoPlayer(song)
            }
        }
    }

    fun selectSong(song: Song) {
        viewModelScope.launch {
            repository.selectSong(song)
            val prepared = repository.prepareSongStems(song)
            loadSongIntoPlayer(prepared)
        }
    }

    private fun loadSongIntoPlayer(song: Song) {
        val instFile = File(song.instPath)
        val origVocalFile = File(song.origVocalPath)
        val clonedVocalFile = song.clonedVocalPath?.let { File(it) }

        player.loadTracks(
            instFile = instFile,
            origVocalFile = origVocalFile,
            clonedVocalFile = clonedVocalFile,
            forceWrongBindingBug = seedVcConfig.value.simulateWrongPlayerBinding
        )
    }

    fun togglePlayPause() {
        player.togglePlayPause()
    }

    fun seekTo(positionMs: Int) {
        player.seekTo(positionMs)
    }

    fun setInstVolume(volume: Float) {
        player.setInstVolume(volume)
    }

    fun setVocalVolume(volume: Float) {
        player.setVocalVolume(volume)
    }

    fun switchVocalMode(mode: VocalMode) {
        player.switchVocalMode(mode, seedVcConfig.value.simulateWrongPlayerBinding)
    }

    fun selectProfile(profile: VoiceProfile) {
        repository.selectProfile(profile)
    }

    fun updateConfig(config: SeedVcConfig) {
        repository.updateConfig(config)
        // If simulated binding toggled, reload tracks to reflect in player immediately
        selectedSong.value?.let { song ->
            loadSongIntoPlayer(song)
        }
    }

    fun startConversion() {
        val song = selectedSong.value ?: return
        val profile = selectedProfile.value ?: return
        val config = seedVcConfig.value

        viewModelScope.launch {
            try {
                player.pause()
                val updated = repository.runConversion(song, profile, config)
                loadSongIntoPlayer(updated)
                _statusMessage.value = "🎉 AI 歌声克隆完成！已自动加载到跟唱播放器"
            } catch (e: Exception) {
                _statusMessage.value = "❌ 转换失败: ${e.message}"
            }
        }
    }

    // --- 卡点排查专项调试与一键修复 ---

    fun toggleBugWrongBinding(simulateBug: Boolean) {
        val updated = seedVcConfig.value.copy(simulateWrongPlayerBinding = simulateBug)
        updateConfig(updated)
        _statusMessage.value = if (simulateBug) {
            "⚠️ 已模拟【卡点一】: 播放器被强制绑定到原唱干声！"
        } else {
            "✅ 【卡点一】已修复: 播放器正确绑定克隆干声音轨！"
        }
    }

    fun toggleBugFakeCopy(simulateBug: Boolean) {
        val updated = seedVcConfig.value.copy(simulateBugFakeCopy = simulateBug)
        updateConfig(updated)
        _statusMessage.value = if (simulateBug) {
            "⚠️ 已模拟【卡点二】: 开启假逻辑/静默回退原唱"
        } else {
            "✅ 【卡点二】已修复: 开启真实 Seed-VC 歌声转换模型"
        }
    }

    fun toggleBugCacheStale(simulateBug: Boolean) {
        val updated = seedVcConfig.value.copy(simulateCacheStale = simulateBug)
        updateConfig(updated)
        _statusMessage.value = if (simulateBug) {
            "⚠️ 已模拟【卡点三】: 使用固定文件名 (引发播放器缓存问题)"
        } else {
            "✅ 【卡点三】已修复: 启用动态时间戳防重命名机制"
        }
    }

    fun autoFixAllBottlenecks() {
        val cleanConfig = SeedVcConfig(
            f0Condition = true,
            autoF0Adjust = true,
            diffusionSteps = 30,
            lengthAdjust = 1.0f,
            inferenceCfgRate = 0.7f,
            selectedProfileId = selectedProfile.value?.id ?: "tenor_01",
            simulateWrongPlayerBinding = false,
            simulateBugFakeCopy = false,
            simulateCacheStale = false
        )
        updateConfig(cleanConfig)
        selectedSong.value?.let { song ->
            repository.clearAudioCache(song.id)
            startConversion()
        }
        _statusMessage.value = "🛠️ 三大卡点已全部一键修复！重新运行真实 Seed-VC 推理中..."
    }

    fun clearAudioCache() {
        selectedSong.value?.let { song ->
            repository.clearAudioCache(song.id)
            loadSongIntoPlayer(song)
            _statusMessage.value = "🧹 音频缓存已彻底清除"
        }
    }

    // --- 实时麦克风跟唱录音 ---

    fun toggleSingAlongRecording() {
        if (playerState.value.isRecordingMic) {
            val currentSong = selectedSong.value
            val recording = player.stopSingAlongRecording(currentSong?.title ?: "跟唱作品")
            if (recording != null) {
                repository.addRecording(recording)
                _statusMessage.value = "🏆 跟唱录音已保存！AI得分: ${recording.score} 分"
            }
        } else {
            player.startSingAlongRecording(repository.getRecordingsDir())
            _statusMessage.value = "🎙️ 开始跟唱录制，请跟随伴奏与克隆干声演唱！"
        }
    }

    fun dismissStatusMessage() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        player.stop()
    }
}
