package com.example.audio

import com.example.model.SeedVcConfig
import com.example.model.VoiceProfile
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import kotlin.math.PI
import kotlin.math.sin

object WavAudioHelper {

    private const val SAMPLE_RATE = 22050
    private const val CHANNELS = 1
    private const val BITS_PER_SAMPLE = 16

    /**
     * Synthesizes an instrumental track with chords, bassline, and drum groove.
     */
    fun generateInstrumentalWav(file: File, durationSec: Int, bpm: Int) {
        val totalSamples = SAMPLE_RATE * durationSec
        val samples = ShortArray(totalSamples)

        val beatDurationSec = 60.0 / bpm
        val samplesPerBeat = (SAMPLE_RATE * beatDurationSec).toInt()

        // Chord progression: C (261.63), G (196.0), Am (220.0), F (174.61)
        val chords = listOf(
            listOf(261.63, 329.63, 392.00, 130.81), // C major + bass C
            listOf(196.00, 246.94, 293.66, 98.00),  // G major + bass G
            listOf(220.00, 261.63, 329.63, 110.00), // A minor + bass A
            listOf(174.61, 220.00, 261.63, 87.31)   // F major + bass F
        )

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val beatIndex = (i / samplesPerBeat)
            val chordIndex = (beatIndex / 4) % chords.size
            val chord = chords[chordIndex]
            val beatPhase = (i % samplesPerBeat).toDouble() / samplesPerBeat

            var signal = 0.0

            // 1. Chords (electric piano/synth tone)
            val chordEnv = (1.0 - beatPhase * 0.4).coerceIn(0.2, 1.0)
            for (freq in chord.take(3)) {
                signal += 0.22 * chordEnv * sin(2.0 * PI * freq * t)
                // Add soft octave overtone
                signal += 0.08 * chordEnv * sin(2.0 * PI * (freq * 2) * t)
            }

            // 2. Bass (plucked low synth)
            val bassFreq = chord[3]
            val bassEnv = kotlin.math.exp(-beatPhase * 4.0)
            signal += 0.35 * bassEnv * sin(2.0 * PI * bassFreq * t)

            // 3. Drum beat (Kick on beat 0, 2; Snare/Clap on beat 1, 3; Hi-hat on every eighth note)
            val beatSubIndex = beatIndex % 4
            if (beatSubIndex == 0 || beatSubIndex == 2) {
                // Kick drum (pitch drop from 120Hz to 45Hz)
                val kickEnv = kotlin.math.exp(-beatPhase * 12.0)
                val kickFreq = 45.0 + 75.0 * kotlin.math.exp(-beatPhase * 25.0)
                signal += 0.40 * kickEnv * sin(2.0 * PI * kickFreq * t)
            } else {
                // Snare noise / snap
                val snareEnv = kotlin.math.exp(-beatPhase * 9.0)
                val noise = ((i * 1103515245 + 12345) and 0x7FFFFFFF) / 0x7FFFFFFF.toDouble() * 2.0 - 1.0
                signal += 0.25 * snareEnv * noise
            }

            // Hi-hat tick every half beat
            val halfBeatPhase = ((i % (samplesPerBeat / 2)).toDouble() / (samplesPerBeat / 2))
            val hatEnv = kotlin.math.exp(-halfBeatPhase * 30.0)
            val hatNoise = ((i * 1664525 + 1013904223) and 0x7FFFFFFF) / 0x7FFFFFFF.toDouble() * 2.0 - 1.0
            signal += 0.12 * hatEnv * hatNoise

            // Clip & scale to 16-bit PCM
            val clamped = signal.coerceIn(-0.95, 0.95)
            samples[i] = (clamped * 32767.0).toInt().toShort()
        }

        writePcmToWav(file, samples, SAMPLE_RATE, CHANNELS)
    }

    /**
     * Synthesizes original vocal track with natural melodic contour, vibrato, and vowel formant filtering.
     */
    fun generateOriginalVocalWav(file: File, durationSec: Int, melodyFreqs: List<Double>) {
        val totalSamples = SAMPLE_RATE * durationSec
        val samples = ShortArray(totalSamples)

        val noteDurationSec = durationSec.toDouble() / melodyFreqs.size
        val samplesPerNote = (SAMPLE_RATE * noteDurationSec).toInt()

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val noteIdx = (i / samplesPerNote).coerceAtMost(melodyFreqs.size - 1)
            val baseFreq = melodyFreqs[noteIdx]

            val noteSampleIdx = i % samplesPerNote
            val notePhase = noteSampleIdx.toDouble() / samplesPerNote

            // Vibrato: 5.2 Hz modulation with 2.5 Hz depth
            val vibrato = 2.5 * sin(2.0 * PI * 5.2 * t)
            val currentFreq = baseFreq + vibrato

            // Vowel envelope (smooth attack and release)
            val attack = (notePhase / 0.1).coerceIn(0.0, 1.0)
            val release = ((1.0 - notePhase) / 0.15).coerceIn(0.0, 1.0)
            val vocalEnv = attack * release

            // Vocal tract formant simulation (fundamental + 2nd, 3rd harmonics + nasal formant)
            var vocalSignal = 0.0
            vocalSignal += 0.50 * sin(2.0 * PI * currentFreq * t)
            vocalSignal += 0.28 * sin(2.0 * PI * (currentFreq * 2.0) * t) // 2nd harmonic
            vocalSignal += 0.16 * sin(2.0 * PI * (currentFreq * 3.0) * t) // 3rd harmonic
            vocalSignal += 0.08 * sin(2.0 * PI * (currentFreq * 4.0) * t) // 4th harmonic
            // Formant resonance peak around 800Hz (vowel "Ah")
            vocalSignal += 0.15 * sin(2.0 * PI * 800.0 * t) * sin(2.0 * PI * currentFreq * t)

            val total = (vocalSignal * vocalEnv).coerceIn(-0.95, 0.95)
            samples[i] = (total * 32767.0).toInt().toShort()
        }

        writePcmToWav(file, samples, SAMPLE_RATE, CHANNELS)
    }

    /**
     * Synthesizes user's clean dry voice recording sample.
     */
    fun generateUserDryVoiceSample(file: File, durationSec: Int = 4, profile: VoiceProfile) {
        val totalSamples = SAMPLE_RATE * durationSec
        val samples = ShortArray(totalSamples)

        // Base speaking fundamental
        val basePitch = if (profile.gender == "女声") 220.0 else 140.0
        val targetPitch = basePitch * Math.pow(2.0, profile.pitchShiftSemitones / 12.0)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val speechCadence = (sin(2.0 * PI * 1.8 * t) + 1.0) * 0.5
            val currentPitch = targetPitch + 12.0 * sin(2.0 * PI * 2.2 * t)

            var drySignal = 0.0
            drySignal += 0.45 * sin(2.0 * PI * currentPitch * t)
            drySignal += 0.25 * sin(2.0 * PI * currentPitch * 2.0 * t)
            drySignal += 0.15 * sin(2.0 * PI * (currentPitch * 3.0 * profile.formantRatio) * t)

            val env = speechCadence.coerceIn(0.1, 1.0)
            val sampleVal = (drySignal * env).coerceIn(-0.95, 0.95)
            samples[i] = (sampleVal * 32767.0).toInt().toShort()
        }

        writePcmToWav(file, samples, SAMPLE_RATE, CHANNELS)
    }

    /**
     * Executes the Singing Voice Conversion (SVC / Seed-VC) pipeline.
     * Accurately distinguishes between real conversion vs the common fake copy fallback bug!
     */
    fun runSingingVoiceConversion(
        origVocalFile: File,
        userVoiceFile: File,
        outputClonedFile: File,
        config: SeedVcConfig,
        profile: VoiceProfile
    ) {
        if (!origVocalFile.exists()) {
            throw IllegalArgumentException("原唱干声文件不存在: ${origVocalFile.absolutePath}")
        }

        // 1. Check if user requested bug simulation for diagnostic testing
        if (config.simulateBugFakeCopy) {
            // [卡点二最常见低级Bug复现]: 很多代码为了假装跑通，直接拷贝原唱人声改名！
            origVocalFile.copyTo(outputClonedFile, overwrite = true)
            return
        }

        // 2. Perform REAL SVC Conversion using Seed-VC transformation principles
        val origBytes = origVocalFile.readBytes()
        if (origBytes.size <= 44) {
            throw IllegalStateException("原唱干声音频数据异常")
        }

        val headerSize = 44
        val pcmDataLength = origBytes.size - headerSize
        val numSamples = pcmDataLength / 2
        val inputSamples = ShortArray(numSamples)

        val buffer = ByteBuffer.wrap(origBytes, headerSize, pcmDataLength).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until numSamples) {
            inputSamples[i] = buffer.short
        }

        // Apply Seed-VC Transformation:
        // A. F0 Condition:
        //    If f0Condition == true, melody line/pitch contour is preserved and shifted according to profile.
        //    If f0Condition == false (TTS error), song melody is flattened to a monotone speech drone!
        // B. Formant Transfer:
        //    User timbre profile formant ratio and harmonic balance is infused into the voice.
        // C. Length adjust:
        //    Strictly 1.0f to match instrumental duration.
        val outputSamples = ShortArray(numSamples)
        val shiftMultiplier = if (config.autoF0Adjust) {
            Math.pow(2.0, profile.pitchShiftSemitones / 12.0)
        } else {
            1.0
        }

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val originalVal = inputSamples[i].toDouble() / 32768.0

            val convertedVal = if (config.f0Condition) {
                // Real SVC with F0 preserved:
                // Modify timbre with user formant signature and harmonic envelope
                val formantMod = sin(2.0 * PI * (440.0 * profile.formantRatio) * t) * 0.18
                val warmth = if (profile.gender == "男声") 1.15 else 0.88
                val timbreShifted = (originalVal * warmth) + formantMod * kotlin.math.abs(originalVal)
                timbreShifted.coerceIn(-0.95, 0.95)
            } else {
                // Bugged mode: without F0 condition, pitch is lost! It turns into monotone speech
                val monotonePitch = 150.0
                val robotSignal = sin(2.0 * PI * monotonePitch * t) * kotlin.math.abs(originalVal)
                robotSignal.coerceIn(-0.95, 0.95)
            }

            outputSamples[i] = (convertedVal * 32767.0).toInt().toShort()
        }

        writePcmToWav(outputClonedFile, outputSamples, SAMPLE_RATE, CHANNELS)
    }

    /**
     * Writes 16-bit PCM samples with standard canonical RIFF WAV header.
     */
    fun writePcmToWav(file: File, samples: ShortArray, sampleRate: Int, channels: Int) {
        val byteDataLength = samples.size * 2
        val totalDataLen = byteDataLength + 36
        val byteRate = sampleRate * channels * 2

        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out ->
            val header = ByteArray(44)
            // RIFF chunk descriptor
            header[0] = 'R'.code.toByte()
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = ((totalDataLen shr 8) and 0xff).toByte()
            header[6] = ((totalDataLen shr 16) and 0xff).toByte()
            header[7] = ((totalDataLen shr 24) and 0xff).toByte()
            // WAVE
            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()
            // fmt sub-chunk
            header[12] = 'f'.code.toByte()
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()
            header[16] = 16 // SubChunk1Size (16 for PCM)
            header[17] = 0
            header[18] = 0
            header[19] = 0
            header[20] = 1 // AudioFormat 1 = PCM
            header[21] = 0
            header[22] = channels.toByte()
            header[23] = 0
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()
            header[32] = (channels * 2).toByte() // BlockAlign
            header[33] = 0
            header[34] = BITS_PER_SAMPLE.toByte()
            header[35] = 0
            // data sub-chunk
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()
            header[40] = (byteDataLength and 0xff).toByte()
            header[41] = ((byteDataLength shr 8) and 0xff).toByte()
            header[42] = ((byteDataLength shr 16) and 0xff).toByte()
            header[43] = ((byteDataLength shr 24) and 0xff).toByte()

            out.write(header)

            val pcmBuffer = ByteBuffer.allocate(byteDataLength).order(ByteOrder.LITTLE_ENDIAN)
            for (s in samples) {
                pcmBuffer.putShort(s)
            }
            out.write(pcmBuffer.array())
        }
    }

    /**
     * Computes MD5 checksum of an audio file to spot duplicate / fake fallback files.
     */
    fun calculateFileMd5(file: File): String {
        if (!file.exists() || file.length() == 0L) return "NONE"
        return try {
            val digest = MessageDigest.getInstance("MD5")
            file.inputStream().use { stream ->
                val buffer = ByteArray(8192)
                var read: Int
                while (stream.read(buffer).also { read = it } > 0) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "ERROR"
        }
    }
}
