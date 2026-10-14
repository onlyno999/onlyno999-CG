package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.DiagnosticStatus
import com.example.ui.components.StudioHeader
import com.example.ui.screens.DiagnosticTab
import com.example.ui.screens.LibraryTab
import com.example.ui.screens.PlayerTab
import com.example.ui.screens.SvcStudioTab
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioError
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

enum class StudioScreen(val label: String, val tag: String) {
    PLAYER("跟唱台", "tab_player"),
    SVC_STUDIO("转换工坊", "tab_svc"),
    DIAGNOSTIC("卡点诊断", "tab_diagnostic"),
    LIBRARY("曲库作品", "tab_library")
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(StudioScreen.PLAYER) }
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedSong by viewModel.selectedSong.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val seedVcConfig by viewModel.seedVcConfig.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val recordings by viewModel.recordings.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val conversionLog by viewModel.conversionLog.collectAsState()
    val diagnosticReport by viewModel.diagnosticReport.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    // Request audio permission for live sing-along recording
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
    }

    // Display status messages
    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissStatusMessage()
        }
    }

    val hasDiagnosticError = diagnosticReport?.let {
        it.checkpoint1Status == DiagnosticStatus.FAILED ||
                it.checkpoint2Status == DiagnosticStatus.FAILED ||
                it.checkpoint3Status == DiagnosticStatus.FAILED
    } ?: false

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground),
        containerColor = StudioBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            StudioHeader(
                currentSong = selectedSong,
                diagnosticReport = diagnosticReport,
                onDiagnosticClick = { currentTab = StudioScreen.DIAGNOSTIC },
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = StudioSurface,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("studio_bottom_nav"),
                tonalElevation = 8.dp
            ) {
                StudioScreen.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            if (tab == StudioScreen.DIAGNOSTIC && hasDiagnosticError) {
                                BadgedBox(badge = { Badge { Text("!") } }) {
                                    Icon(
                                        imageVector = Icons.Default.BugReport,
                                        contentDescription = tab.label,
                                        tint = if (isSelected) StudioError else TextSecondary
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = when (tab) {
                                        StudioScreen.PLAYER -> Icons.Default.Mic
                                        StudioScreen.SVC_STUDIO -> Icons.Default.AutoAwesome
                                        StudioScreen.DIAGNOSTIC -> Icons.Default.BugReport
                                        StudioScreen.LIBRARY -> Icons.Default.QueueMusic
                                    },
                                    contentDescription = tab.label,
                                    tint = if (isSelected) NeonCyan else TextTertiary
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                color = if (isSelected) NeonCyan else TextTertiary
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = NeonCyan.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentTab, label = "tab_fade") { tab ->
                when (tab) {
                    StudioScreen.PLAYER -> {
                        PlayerTab(
                            song = selectedSong,
                            playerState = playerState,
                            seedVcConfig = seedVcConfig,
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onSeekTo = { viewModel.seekTo(it) },
                            onSwitchVocalMode = { viewModel.switchVocalMode(it) },
                            onSetInstVolume = { viewModel.setInstVolume(it) },
                            onSetVocalVolume = { viewModel.setVocalVolume(it) },
                            onToggleSingAlongRecording = {
                                if (hasMicPermission) {
                                    viewModel.toggleSingAlongRecording()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            onNavigateToDiagnostic = { currentTab = StudioScreen.DIAGNOSTIC }
                        )
                    }

                    StudioScreen.SVC_STUDIO -> {
                        SvcStudioTab(
                            song = selectedSong,
                            profiles = profiles,
                            selectedProfile = selectedProfile,
                            config = seedVcConfig,
                            isGenerating = isGenerating,
                            conversionLog = conversionLog,
                            onSelectProfile = { viewModel.selectProfile(it) },
                            onUpdateConfig = { viewModel.updateConfig(it) },
                            onStartConversion = { viewModel.startConversion() }
                        )
                    }

                    StudioScreen.DIAGNOSTIC -> {
                        DiagnosticTab(
                            song = selectedSong,
                            report = diagnosticReport,
                            config = seedVcConfig,
                            onToggleBugWrongBinding = { viewModel.toggleBugWrongBinding(it) },
                            onToggleBugFakeCopy = { viewModel.toggleBugFakeCopy(it) },
                            onToggleBugCacheStale = { viewModel.toggleBugCacheStale(it) },
                            onAutoFixAll = { viewModel.autoFixAllBottlenecks() },
                            onClearCache = { viewModel.clearAudioCache() }
                        )
                    }

                    StudioScreen.LIBRARY -> {
                        LibraryTab(
                            songs = songs,
                            selectedSong = selectedSong,
                            profiles = profiles,
                            recordings = recordings,
                            onSelectSong = { viewModel.selectSong(it) },
                            onNavigateToPlayer = { currentTab = StudioScreen.PLAYER }
                        )
                    }
                }
            }
        }
    }
}
