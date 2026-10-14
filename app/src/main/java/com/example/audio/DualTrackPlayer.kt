package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.example.model.KaraokeRecording
import com.example.model.VocalMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class PlayerPlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val instVolume: Float = 0.85f,
    val vocalVolume: Float = 0.90f,
    val vocalMode: VocalMode = VocalMode.CLONED_AI,
    val activeInstPath: String = "",
    val activeVocalPath: String = "",
    val isClonedTrackBound: Boolean = true,
    val isRecordingMic: Boolean = false,
    val micAmplitude: Float = 0f,
    val waveAmplitudes: List<Float> = List(24) { 0.1f }
)

class DualTrackPlayer(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var instPlayer: MediaPlayer? = null
    private var vocalPlayer: MediaPlayer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var recordingStartTimeMs: Long = 0L

    private val _state = MutableStateFlow(PlayerPlaybackState())
    val state: StateFlow<PlayerPlaybackState> = _state.asStateFlow()

    private var pollJob: Job? = null

    private var currentInstFile: File? = null
    private var currentOrigVocalFile: File? = null
    private var currentClonedVocalFile: File? = null

    /**
     * Loads audio tracks for dual-stem synchronized playback.
     * @param instFile Accompaniment track
     * @param origVocalFile Original singer dry stem
     * @param clonedVocalFile Converted AI vocal stem
     * @param forceWrongBindingBug Simulate Bug 1 (wrongly binds orig_vocal instead of cloned_vocal)
     */
    fun loadTracks(
        instFile: File,
        origVocalFile: File,
        clonedVocalFile: File?,
        forceWrongBindingBug: Boolean = false
    ) {
        stop()

        currentInstFile = instFile
        currentOrigVocalFile = origVocalFile
        currentClonedVocalFile = clonedVocalFile

        try {
            // Setup Instrumental Player
            instPlayer = MediaPlayer().apply {
                setDataSource(instFile.absolutePath)
                setOnCompletionListener {
                    handlePlaybackComplete()
                }
                prepare()
            }

            // Determine which vocal file to bind based on mode & bug test flag:
            val targetVocalFile: File? = if (forceWrongBindingBug) {
                // [卡点一最常见低级Bug复现]: 很多代码把原唱干声直接传给了跟唱播放器！
                origVocalFile
            } else {
                when (_state.value.vocalMode) {
                    VocalMode.CLONED_AI -> clonedVocalFile ?: origVocalFile
                    VocalMode.ORIGINAL -> origVocalFile
                    VocalMode.MUTED -> null
                }
            }

            if (targetVocalFile != null && targetVocalFile.exists()) {
                vocalPlayer = MediaPlayer().apply {
                    setDataSource(targetVocalFile.absolutePath)
                    prepare()
                }
            }

            val duration = instPlayer?.duration ?: 0
            val isClonedBound = !forceWrongBindingBug &&
                    (_state.value.vocalMode == VocalMode.CLONED_AI && clonedVocalFile != null)

            _state.value = _state.value.copy(
                durationMs = duration,
                currentPositionMs = 0,
                activeInstPath = instFile.absolutePath,
                activeVocalPath = targetVocalFile?.absolutePath ?: "无人声音轨 (静音/纯伴奏)",
                isClonedTrackBound = isClonedBound
            )

            applyVolumes()
            startPoller()

        } catch (e: Exception) {
            Log.e("DualTrackPlayer", "Error loading audio tracks: ${e.message}")
        }
    }

    fun play() {
        try {
            val iPlayer = instPlayer ?: return
            val vPlayer = vocalPlayer

            // Sync start
            iPlayer.start()
            vPlayer?.start()

            _state.value = _state.value.copy(isPlaying = true)
        } catch (e: Exception) {
            Log.e("DualTrackPlayer", "Play error: ${e.message}")
        }
    }

    fun pause() {
        try {
            instPlayer?.pause()
            vocalPlayer?.pause()
            _state.value = _state.value.copy(isPlaying = false)
        } catch (e: Exception) {
            Log.e("DualTrackPlayer", "Pause error: ${e.message}")
        }
    }

    fun togglePlayPause() {
        if (_state.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Int) {
        try {
            instPlayer?.seekTo(positionMs)
            vocalPlayer?.seekTo(positionMs)
            _state.value = _state.value.copy(currentPositionMs = positionMs)
        } catch (e: Exception) {
            Log.e("DualTrackPlayer", "Seek error: ${e.message}")
        }
    }

    fun setInstVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _state.value = _state.value.copy(instVolume = clamped)
        instPlayer?.setVolume(clamped, clamped)
    }

    fun setVocalVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _state.value = _state.value.copy(vocalVolume = clamped)
        applyVolumes()
    }

    fun switchVocalMode(mode: VocalMode, forceWrongBindingBug: Boolean = false) {
        _state.value = _state.value.copy(vocalMode = mode)
        val wasPlaying = _state.value.isPlaying
        val currentPos = instPlayer?.currentPosition ?: 0

        // Re-bind vocal track
        vocalPlayer?.stop()
        vocalPlayer?.release()
        vocalPlayer = null

        val targetVocalFile: File? = if (forceWrongBindingBug) {
            currentOrigVocalFile
        } else {
            when (mode) {
                VocalMode.CLONED_AI -> currentClonedVocalFile ?: currentOrigVocalFile
                VocalMode.ORIGINAL -> currentOrigVocalFile
                VocalMode.MUTED -> null
            }
        }

        if (targetVocalFile != null && targetVocalFile.exists()) {
            try {
                vocalPlayer = MediaPlayer().apply {
                    setDataSource(targetVocalFile.absolutePath)
                    prepare()
                    seekTo(currentPos)
                    if (wasPlaying) start()
                }
            } catch (e: Exception) {
                Log.e("DualTrackPlayer", "Error switching vocal mode: ${e.message}")
            }
        }

        val isClonedBound = !forceWrongBindingBug && (mode == VocalMode.CLONED_AI && currentClonedVocalFile != null)

        _state.value = _state.value.copy(
            activeVocalPath = targetVocalFile?.absolutePath ?: "无人声音轨 (纯伴奏)",
            isClonedTrackBound = isClonedBound
        )
        applyVolumes()
    }

    private fun applyVolumes() {
        val iVol = _state.value.instVolume
        val vVol = if (_state.value.vocalMode == VocalMode.MUTED) 0f else _state.value.vocalVolume

        instPlayer?.setVolume(iVol, iVol)
        vocalPlayer?.setVolume(vVol, vVol)
    }

    private fun handlePlaybackComplete() {
        _state.value = _state.value.copy(isPlaying = false, currentPositionMs = 0)
        instPlayer?.seekTo(0)
        vocalPlayer?.seekTo(0)
    }

    private fun startPoller() {
        pollJob?.cancel()
        pollJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val iPlayer = instPlayer
                if (iPlayer != null && iPlayer.isPlaying) {
                    val pos = try { iPlayer.currentPosition } catch (e: Exception) { 0 }
                    val dur = try { iPlayer.duration } catch (e: Exception) { 0 }

                    // Generate animated waveform bars
                    val newWaves = List(24) { index ->
                        val phase = (pos / 150.0 + index * 0.45)
                        val amp = (kotlin.math.sin(phase) * 0.4 + 0.55).toFloat()
                        amp.coerceIn(0.12f, 0.95f)
                    }

                    // Read mic level if recording
                    val micAmp = try {
                        val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                        (maxAmp / 32768.0f).coerceIn(0f, 1f)
                    } catch (e: Exception) {
                        0f
                    }

                    _state.value = _state.value.copy(
                        currentPositionMs = pos,
                        durationMs = dur,
                        micAmplitude = micAmp,
                        waveAmplitudes = newWaves
                    )
                }
                delay(60)
            }
        }
    }

    /**
     * Starts live sing-along microphone recording along with karaoke backing track.
     */
    fun startSingAlongRecording(outputDir: File) {
        try {
            val recordFile = File(outputDir, "karaoke_singing_${System.currentTimeMillis()}.mp4")
            currentRecordingFile = recordFile

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(recordFile.absolutePath)
                prepare()
                start()
            }

            recordingStartTimeMs = System.currentTimeMillis()
            _state.value = _state.value.copy(isRecordingMic = true)

            // Start audio backing if not already playing
            if (!_state.value.isPlaying) {
                play()
            }
        } catch (e: Exception) {
            Log.e("DualTrackPlayer", "Failed to start mic recording: ${e.message}")
        }
    }

    /**
     * Stops live sing-along recording and returns the saved performance metadata.
     */
    fun stopSingAlongRecording(songTitle: String): KaraokeRecording? {
        val recorder = mediaRecorder ?: return null
        return try {
            recorder.stop()
            recorder.release()
            mediaRecorder = null

            val duration = System.currentTimeMillis() - recordingStartTimeMs
            _state.value = _state.value.copy(isRecordingMic = false, micAmplitude = 0f)

            val file = currentRecordingFile
            if (file != null && file.exists()) {
                val score = (88 + (duration % 12).toInt()).coerceIn(85, 99)
                KaraokeRecording(
                    id = UUID.randomUUID().toString(),
                    songTitle = songTitle,
                    filePath = file.absolutePath,
                    recordedAt = System.currentTimeMillis(),
                    durationMs = duration,
                    vocalModeUsed = _state.value.vocalMode,
                    score = score
                )
            } else null
        } catch (e: Exception) {
            Log.e("DualTrackPlayer", "Error stopping recording: ${e.message}")
            mediaRecorder = null
            _state.value = _state.value.copy(isRecordingMic = false)
            null
        }
    }

    fun stop() {
        pollJob?.cancel()
        try {
            instPlayer?.stop()
            instPlayer?.release()
        } catch (e: Exception) {}
        instPlayer = null

        try {
            vocalPlayer?.stop()
            vocalPlayer?.release()
        } catch (e: Exception) {}
        vocalPlayer = null

        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {}
        mediaRecorder = null

        _state.value = _state.value.copy(
            isPlaying = false,
            currentPositionMs = 0,
            isRecordingMic = false
        )
    }
}
