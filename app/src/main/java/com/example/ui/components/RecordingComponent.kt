package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AudioRecording
import com.example.data.viewmodel.RecordingUiState
import com.example.data.viewmodel.PlaybackUiState
import com.example.data.viewmodel.RecordingViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.Locale

/**
 * RecordingComponent
 *
 * Dedicated Jetpack Compose UI component utilizing MediaRecorder for audio capture,
 * complete with Start, Stop, and Playback controls.
 *
 * - LICENSE: NETSECUREPRO.CA
 * - ENGINE: GEMIN CORE V7 PRO FINAL
 * - SIGNATURE: IA_LOGIC_SINGNATURE:ZOUBIROU-IA-2025
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecordingComponent(
    modifier: Modifier = Modifier,
    viewModel: RecordingViewModel = viewModel(),
    onRecordingSaved: ((AudioRecording) -> Unit)? = null
) {
    val context = LocalContext.current
    val recordingState by viewModel.recordingState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val recordings by viewModel.recordings.collectAsState()
    val lastCaptured by viewModel.lastCapturedRecording.collectAsState()

    var showAddManualDialog by remember { mutableStateOf(false) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.importAudioFile(it, context)
        }
    }

    val micPermissionState = rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO)
    val notifPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
    } else null

    var customTitleInput by remember { mutableStateOf("capture_vocale") }

    LaunchedEffect(lastCaptured) {
        lastCaptured?.let { onRecordingSaved?.invoke(it) }
    }

    if (showAddManualDialog) {
        AddAudioNoteDialog(
            onDismiss = { showAddManualDialog = false },
            onConfirm = { title, durationSec, note ->
                viewModel.addManualRecording(title, durationSec, note)
                showAddManualDialog = false
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("media_recorder_component"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ==========================================
        // SYSTEM BADGE & LICENSE HEADER
        // ==========================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Licence Certifiée",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "NETSECUREPRO.CA • GEMIN CORE V7 PRO FINAL",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "IA_LOGIC_SINGNATURE:ZOUBIROU-IA-2025",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ==========================================
        // QUICK ADD / IMPORT ACTIONS BAR
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = { audioPickerLauncher.launch("audio/*") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_import_audio_file"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.UploadFile,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Importer Audio", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            Button(
                onClick = { showAddManualDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_add_manual_audio"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ajouter Note", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // ==========================================
        // PERMISSION BANNER
        // ==========================================
        if (!micPermissionState.status.isGranted) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("mic_permission_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MicOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Permission micro requise pour MediaRecorder",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Button(
                        onClick = { micPermissionState.launchPermissionRequest() },
                        modifier = Modifier.testTag("grant_mic_permission_button")
                    ) {
                        Text("Activer")
                    }
                }
            }
        }

        // Error message snackbar / banner
        recordingState.errorMessage?.let { errorMsg ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.clearError() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Fermer",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // ==========================================
        // MEDIA RECORDER CAPTURE STUDIO CARD
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("media_recorder_capture_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status Header Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = when {
                        recordingState.isRecording && !recordingState.isPaused -> MaterialTheme.colorScheme.errorContainer
                        recordingState.isPaused -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (recordingState.isRecording && !recordingState.isPaused) {
                            val infiniteTransition = rememberInfiniteTransition(label = "BlinkRec")
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 0.2f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(600, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "Alpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = alpha))
                            )
                        } else {
                            Icon(
                                imageVector = if (recordingState.isPaused) Icons.Filled.Pause else Icons.Filled.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (recordingState.isPaused) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Text(
                            text = when {
                                recordingState.isRecording && !recordingState.isPaused -> "ENREGISTREMENT ACTIF (AAC 44.1kHz)"
                                recordingState.isPaused -> "CAPTURE EN PAUSE"
                                else -> "STUDIO MEDIARECORDER PRÊT"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                recordingState.isRecording && !recordingState.isPaused -> MaterialTheme.colorScheme.onErrorContainer
                                recordingState.isPaused -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Timer Display
                Text(
                    text = formatTimerTime(recordingState.durationSeconds),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (recordingState.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("media_recorder_timer_text")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Live Audio Waveform Visualizer
                AudioWaveformVisualizer(
                    amplitudes = recordingState.liveAmplitudes,
                    isLive = recordingState.isRecording && !recordingState.isPaused,
                    barColor = if (recordingState.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("media_recorder_waveform")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Recording Title Input (when idle)
                if (!recordingState.isRecording) {
                    OutlinedTextField(
                        value = customTitleInput,
                        onValueChange = { customTitleInput = it },
                        label = { Text("Titre de l'enregistrement") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recording_title_input_field"),
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.Edit, contentDescription = null)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ==========================================
                // START / STOP CONTROLS
                // ==========================================
                Button(
                    onClick = {
                        if (!micPermissionState.status.isGranted) {
                            micPermissionState.launchPermissionRequest()
                        } else {
                            if (notifPermissionState != null && !notifPermissionState.status.isGranted) {
                                notifPermissionState.launchPermissionRequest()
                            }
                            if (recordingState.isRecording) {
                                viewModel.stopRecording()
                            } else {
                                viewModel.startRecording(customTitleInput)
                            }
                        }
                    },
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth(0.9f)
                        .testTag("start_stop_recording_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (recordingState.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(
                        imageVector = if (recordingState.isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (recordingState.isRecording) "Stop Capture" else "Start Capture",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (recordingState.isRecording) "Arrêter & Enregistrer" else "Démarrer l'enregistrement",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                // Pause / Resume and Cancel buttons
                if (recordingState.isRecording) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pause / Resume Button
                        IconButton(
                            onClick = { viewModel.togglePauseRecording() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("pause_resume_button")
                        ) {
                            Icon(
                                imageVector = if (recordingState.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = if (recordingState.isPaused) "Reprendre" else "Pause",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Cancel / Discard Button
                        IconButton(
                            onClick = { viewModel.cancelRecording() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f))
                                .testTag("cancel_recording_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteForever,
                                contentDescription = "Annuler capture",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // DEDICATED PLAYBACK CONTROLS PANEL
        // ==========================================
        PlaybackControlsPanel(
            playbackState = playbackState,
            lastCaptured = lastCaptured,
            onPlayPause = {
                val target = playbackState.currentPlayingPath ?: lastCaptured?.localPath
                if (target != null) {
                    viewModel.playAudio(target, lastCaptured?.id, lastCaptured?.durationSeconds ?: 0)
                }
            },
            onStop = { viewModel.stopPlayback() },
            onSeek = { seconds -> viewModel.seekTo(seconds) },
            onSpeedChange = { speed -> viewModel.setPlaybackSpeed(speed) },
            onSkip = { offset -> viewModel.skipSeconds(offset) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // RECENT RECORDINGS LIST WITH QUICK PLAYBACK
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Fichiers Enregistrés (${recordings.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "MediaRecorder AAC",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (recordings.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Box(
                    modifier = Modifier.padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun fichier enregistré. Appuyez sur le bouton micro ci-dessus pour capturer votre premier audio.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recordings.take(5).forEach { recording ->
                    CapturedRecordingItem(
                        recording = recording,
                        isPlaying = playbackState.isPlaying && playbackState.currentPlayingPath == recording.localPath,
                        onPlayClick = { viewModel.playRecording(recording) },
                        onDeleteClick = { viewModel.deleteRecording(recording) }
                    )
                }
            }
        }
    }
}

/**
 * PlaybackControlsPanel
 *
 * Dedicated playback controls UI card for playing captured audio files,
 * with Seek bar, Play/Pause, Rewind, Fast Forward, and Speed controls.
 */
@Composable
fun PlaybackControlsPanel(
    playbackState: PlaybackUiState,
    lastCaptured: AudioRecording?,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Int) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSkip: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasAudio = playbackState.currentPlayingPath != null || lastCaptured != null
    val totalSeconds = playbackState.durationSeconds.coerceAtLeast(lastCaptured?.durationSeconds ?: 0)
    val currentSeconds = playbackState.currentPositionSeconds

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("playback_controls_panel"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Track information
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
                    Icon(
                        imageVector = Icons.Filled.Audiotrack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = lastCaptured?.title ?: (playbackState.currentPlayingPath?.substringAfterLast("/") ?: "Contrôleur de Lecture Audio"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (playbackState.isPlaying) "Lecture en cours • ${playbackState.playbackSpeed}x"
                            else if (playbackState.isPaused) "Lecture en pause"
                            else if (hasAudio) "Prêt pour lecture"
                            else "Aucun audio chargé",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (playbackState.isPlaying || playbackState.isPaused) {
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("stop_playback_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = "Arrêter lecture",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Seek Slider
            val sliderValue = if (totalSeconds > 0) {
                (currentSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Slider(
                value = sliderValue,
                onValueChange = { ratio ->
                    val target = (ratio * totalSeconds).toInt()
                    onSeek(target)
                },
                enabled = hasAudio && totalSeconds > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("playback_seek_slider")
            )

            // Timestamps: Current / Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTimerTime(currentSeconds),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTimerTime(totalSeconds),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Playback Buttons (Rewind 5s, Play/Pause, Forward 5s)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rewind 5s
                IconButton(
                    onClick = { onSkip(-5) },
                    enabled = hasAudio,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("rewind_5s_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay5,
                        contentDescription = "Reculer 5s",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Primary Play / Pause Button
                FilledIconButton(
                    onClick = onPlayPause,
                    enabled = hasAudio,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("play_pause_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Lire",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Forward 5s
                IconButton(
                    onClick = { onSkip(5) },
                    enabled = hasAudio,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("forward_5s_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Forward5,
                        contentDescription = "Avancer 5s",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Speed chips selector (0.75x, 1.0x, 1.25x, 1.5x, 2.0x)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vitesse: ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                    val isSelected = playbackState.playbackSpeed == speed
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .clickable { onSpeedChange(speed) }
                            .testTag("speed_${speed}x")
                    ) {
                        Text(
                            text = "${speed}x",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * CapturedRecordingItem
 *
 * Single item row for recorded audio files with play/stop and delete actions.
 */
@Composable
fun CapturedRecordingItem(
    recording: AudioRecording,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recording_item_${recording.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Play / Pause Icon Button
            FilledIconButton(
                onClick = onPlayClick,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("play_recording_btn_${recording.id}"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Metadata Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recording.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${recording.fileName} • ${formatTimerTime(recording.durationSeconds)} • ${recording.fileSizeMb} MB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Delete Action
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("delete_recording_btn_${recording.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Supprimer",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatTimerTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
}

/**
 * AddAudioNoteDialog
 * Dialog for adding a voice memo or manual recording to the vault
 */
@Composable
fun AddAudioNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, durationSec: Int, note: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("15") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AddCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ajouter un Enregistrement")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Moteur: GEMIN CORE V7 PRO FINAL • NETSECUREPRO.CA",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de l'audio") },
                    placeholder = { Text("ex: Réunion Ghost, Note Rapide") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_dialog_title_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) durationText = it },
                    label = { Text("Durée estimée (secondes)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_dialog_duration_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes / Transcription") },
                    placeholder = { Text("Description ou transcription...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_dialog_notes_input"),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = title.ifBlank { "Note Vocale" }
                    val finalDur = durationText.toIntOrNull()?.coerceAtLeast(1) ?: 15
                    onConfirm(finalTitle, finalDur, note)
                },
                modifier = Modifier.testTag("add_dialog_confirm_btn")
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("add_dialog_cancel_btn")
            ) {
                Text("Annuler")
            }
        }
    )
}
