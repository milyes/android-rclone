package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioRecording
import com.example.data.viewmodel.AudioSyncViewModel
import com.example.ui.components.AudioPlayerCard
import com.example.ui.components.AudioWaveformVisualizer
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RecorderVaultScreen(
    viewModel: AudioSyncViewModel,
    modifier: Modifier = Modifier
) {
    val recordings by viewModel.recordings.collectAsState()
    val recorderState by viewModel.recorderState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedIds by viewModel.selectedRecordingIds.collectAsState()

    val micPermissionState = rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO)
    val notifPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
    } else null

    var customNameInput by remember { mutableStateOf("enregistrement_ghost_vocal") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Recording Studio Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("live_recorder_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Permission Warning Banner if not granted
                if (!micPermissionState.status.isGranted) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
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
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Permission microphone requise pour enregistrer via MediaRecorder API.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { micPermissionState.launchPermissionRequest() },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Autoriser", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "FLUX WAY - Capture Vocale",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Enregistrement local Z-CORE",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Visual Recording Status Indicator Badge
                    val (statusText, statusBgColor, statusTextColor) = when {
                        recorderState.isRecording && recorderState.isPaused -> Triple(
                            "EN PAUSE",
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        recorderState.isRecording -> Triple(
                            "ENREGISTREMENT EN COURS",
                            MaterialTheme.colorScheme.errorContainer,
                            MaterialTheme.colorScheme.onErrorContainer
                        )
                        else -> Triple(
                            "PRÊT À ENREGISTRER",
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Surface(
                        color = statusBgColor,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("recording_status_indicator")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusTextColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusTextColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (recorderState.isRecording) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Service d'arrière-plan actif • Notification permanente",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Timer Display
                Text(
                    text = formatTimer(recorderState.durationSeconds),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (recorderState.isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Live Amplitude Waveform & Level Meter
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recording_waveform_container"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (recorderState.isRecording) {
                        val latestAmp = recorderState.liveAmplitudes.lastOrNull() ?: 20
                        val dbLevel = if (latestAmp > 0) ((latestAmp / 100f) * 50 - 50).toInt() else -50
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SIGNAL AUDIO DYNAMIQUE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Niveau: ${latestAmp}% (${dbLevel} dB)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    AudioWaveformVisualizer(
                        amplitudes = recorderState.liveAmplitudes,
                        isLive = recorderState.isRecording,
                        barColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("live_amplitude_visualizer")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // File Name Input if not recording
                if (!recorderState.isRecording) {
                    OutlinedTextField(
                        value = customNameInput,
                        onValueChange = { customNameInput = it },
                        label = { Text("Nom de l'enregistrement (.wav)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recording_name_input"),
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.Mic, contentDescription = null)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Controls Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!recorderState.isRecording) {
                        Button(
                            onClick = {
                                if (micPermissionState.status.isGranted) {
                                    if (notifPermissionState != null && !notifPermissionState.status.isGranted) {
                                        notifPermissionState.launchPermissionRequest()
                                    }
                                    viewModel.startRecording(customNameInput)
                                } else {
                                    micPermissionState.launchPermissionRequest()
                                }
                            },
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("start_recording_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(25.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.RadioButtonChecked, contentDescription = "Démarrer l'enregistrement")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Démarrer l'enregistrement", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.togglePauseRecording() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("pause_recording_btn")
                        ) {
                            Icon(
                                imageVector = if (recorderState.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = if (recorderState.isPaused) "Reprendre" else "Pause"
                            )
                        }

                        IconButton(
                            onClick = { viewModel.cancelRecording() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f))
                                .testTag("cancel_recording_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Annuler l'enregistrement",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        Button(
                            onClick = { viewModel.stopAndSaveRecording() },
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("stop_recording_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(25.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Stop, contentDescription = "Arrêter l'enregistrement")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Arrêter & Sauvegarder", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Search & Library Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Bibliothèque Vocale Z-CORE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isSelectionMode) {
                    Text(
                        text = "${selectedIds.size} / ${recordings.size} sélectionné(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "${recordings.size} fichier(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                var showVaultCameraDialog by remember { mutableStateOf(false) }

                if (showVaultCameraDialog) {
                    com.example.ui.components.CameraCaptureDialog(
                        onDismissRequest = { showVaultCameraDialog = false }
                    )
                }

                IconButton(
                    onClick = { showVaultCameraDialog = true },
                    modifier = Modifier.testTag("vault_quick_camera_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = "Capture photo caméra",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleSelectionMode() },
                    modifier = Modifier.testTag("toggle_selection_mode_btn")
                ) {
                    Icon(
                        imageVector = if (isSelectionMode) Icons.Filled.ChecklistRtl else Icons.Filled.Checklist,
                        contentDescription = "Mode sélection en masse",
                        tint = if (isSelectionMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        AnimatedVisibility(visible = isSelectionMode) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("batch_selection_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            if (selectedIds.size == recordings.size) {
                                viewModel.clearSelection()
                            } else {
                                viewModel.selectAllRecordings(recordings.map { it.id })
                            }
                        },
                        modifier = Modifier.testTag("select_all_btn")
                    ) {
                        Text(
                            text = if (selectedIds.size == recordings.size && recordings.isNotEmpty()) "Désélectionner tout" else "Tout sélectionner",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { viewModel.massSyncSelectedRecordingsWithRclone() },
                        enabled = selectedIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("batch_rclone_sync_btn"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mass-Sync Rclone (${selectedIds.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Rechercher par nom de fichier ou horodatage (ex: ghost_vocal, 2023, .wav)...") },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Search, contentDescription = "Recherche")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Effacer")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_recordings_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Recordings List
        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aucun enregistrement trouvé",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(recordings, key = { it.id }) { rec ->
                    val isSelected = selectedIds.contains(rec.id)
                    AudioPlayerCard(
                        recording = rec,
                        playerState = playerState,
                        onPlayPause = { viewModel.togglePlayPause(rec) },
                        onSeek = { seconds -> viewModel.seekTo(seconds, rec) },
                        onSpeedChange = { speed -> viewModel.setPlaybackSpeed(speed) },
                        onSyncClick = { viewModel.runRcloneSync(rec) },
                        onAiAnalyzeClick = {
                            viewModel.analyzeRecordingWithAi(rec)
                            viewModel.setTab(com.example.data.viewmodel.NavigationTab.AI_STUDIO)
                        },
                        onDeleteClick = { viewModel.deleteRecording(rec) },
                        onRenameClick = { newTitle -> viewModel.renameRecording(rec, newTitle) },
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onToggleSelect = { viewModel.toggleRecordingSelection(rec.id) }
                    )
                }
            }
        }
    }
}

private fun formatTimer(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
