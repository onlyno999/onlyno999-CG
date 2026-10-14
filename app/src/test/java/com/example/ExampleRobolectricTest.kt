package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.WavAudioHelper
import com.example.model.SeedVcConfig
import com.example.model.VoiceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AI跟唱", appName)
    }

    @Test
    fun `verify wav generator and fake logic detection`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testDir = File(context.cacheDir, "test_audio").apply { mkdirs() }

        val origFile = File(testDir, "orig_vocal.wav")
        val userVoiceFile = File(testDir, "user_voice.wav")
        val clonedRealFile = File(testDir, "cloned_real.wav")
        val clonedFakeFile = File(testDir, "cloned_fake.wav")

        val melody = listOf(261.63, 293.66, 329.63, 349.23)
        WavAudioHelper.generateOriginalVocalWav(origFile, 2, melody)
        assertTrue(origFile.exists() && origFile.length() > 44)

        val profile = VoiceProfile(
            id = "test_tenor",
            name = "Test Tenor",
            desc = "Test",
            gender = "男声",
            pitchShiftSemitones = 0,
            formantRatio = 1.0f
        )
        WavAudioHelper.generateUserDryVoiceSample(userVoiceFile, 2, profile)

        // 1. Run Real SVC Conversion
        val realConfig = SeedVcConfig(f0Condition = true, simulateBugFakeCopy = false)
        WavAudioHelper.runSingingVoiceConversion(origFile, userVoiceFile, clonedRealFile, realConfig, profile)
        assertTrue(clonedRealFile.exists())

        // 2. Run Fake Copy Conversion (Simulating Bug 2)
        val fakeConfig = SeedVcConfig(simulateBugFakeCopy = true)
        WavAudioHelper.runSingingVoiceConversion(origFile, userVoiceFile, clonedFakeFile, fakeConfig, profile)

        val origMd5 = WavAudioHelper.calculateFileMd5(origFile)
        val realMd5 = WavAudioHelper.calculateFileMd5(clonedRealFile)
        val fakeMd5 = WavAudioHelper.calculateFileMd5(clonedFakeFile)

        // Real conversion must have distinct audio hash from original
        assertNotEquals(origMd5, realMd5)
        // Fake copy conversion erroneously has identical hash to original
        assertEquals(origMd5, fakeMd5)
    }
}
