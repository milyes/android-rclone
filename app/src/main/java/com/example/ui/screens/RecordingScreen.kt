package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.animation.core.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.viewmodel.AudioSyncViewModel
import com.example.ui.components.AudioPlayerCard
import com.example.ui.components.AudioWaveformVisualizer
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RecordingScreen(
    viewModel: AudioSyncViewModel,
    modifier: Modifier = Modifier
) {
    val recorderState by viewModel.recorderState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val recordings by viewModel.recordings.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val micPermissionState = rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO)
    val notifPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
    } else null
    var customNameInput by remember { mutableStateOf("enregistrement_vocal") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Permission Banner
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
                    Text(
                        text = "Permission microphone requise",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Button(
                        onClick = { micPermissionState.launchPermissionRequest() },
                        modifier = Modifier.testTag("request_mic_permission_btn")
                    ) {
                        Text("Autoriser")
                    }
                }
            }
        }

        // Live Recording Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("recording_studio_card"),
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
                Text(
                    text = if (recorderState.isRecording) "ENREGISTREMENT EN COURS" else "STUDIO DE CAPTURE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (recorderState.isRecording) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp)
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
                                text = "Service d'arrière-plan actif • Notification continue",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Timer Display
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (recorderState.isRecording) {
                        val infiniteTransition = rememberInfiniteTransition(label = "Blink")
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "Alpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = alpha))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = formatRecordingTime(recorderState.durationSeconds),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (recorderState.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("recording_timer_text")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Audio Waveform Visualizer
                AudioWaveformVisualizer(
                    amplitudes = recorderState.liveAmplitudes,
                    isLive = recorderState.isRecording,
                    barColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("recording_waveform_visualizer")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Title Input (when idle)
                if (!recorderState.isRecording) {
                    OutlinedTextField(
                        value = customNameInput,
                        onValueChange = { customNameInput = it },
                        label = { Text("Titre de l'enregistrement") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recording_title_input"),
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.Mic, contentDescription = null)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Main Recording Toggle Button (MediaRecorder API)
                Button(
                    onClick = {
                        if (!micPermissionState.status.isGranted) {
                            micPermissionState.launchPermissionRequest()
                        } else {
                            if (notifPermissionState != null && !notifPermissionState.status.isGranted) {
                                notifPermissionState.launchPermissionRequest()
                            }
                            viewModel.toggleRecording(customNameInput)
                        }
                    },
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth(0.85f)
                        .testTag("toggle_recording_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (recorderState.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(
                        imageVector = if (recorderState.isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (recorderState.isRecording) "Arrêter l'enregistrement" else "Démarrer l'enregistrement"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (recorderState.isRecording) "Arrêter et Sauvegarder" else "Démarrer l'enregistrement",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                if (recorderState.isRecording) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.togglePauseRecording() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("pause_resume_recording_button")
                        ) {
                            Icon(
                                imageVector = if (recorderState.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = if (recorderState.isPaused) "Reprendre" else "Mettre en pause"
                            )
                        }

                        IconButton(
                            onClick = { viewModel.cancelRecording() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f))
                                .testTag("cancel_recording_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Annuler l'enregistrement",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent Saved Audio Snippets List Header & Search Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Enregistrements Récents",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = "${recordings.size} fichier(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Rechercher par nom ou horodatage (ex: .wav, 2026, 07-24)...") },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Search, contentDescription = "Rechercher")
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
                .padding(bottom = 8.dp)
                .testTag("search_recordings_input_history"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(recordings, key = { it.id }) { item ->
                AudioPlayerCard(
                    recording = item,
                    playerState = playerState,
                    onPlayPause = { viewModel.togglePlayPause(item) },
                    onSeek = { seconds -> viewModel.seekTo(seconds, item) },
                    onSpeedChange = { speed -> viewModel.setPlaybackSpeed(speed) },
                    onSyncClick = { viewModel.runRcloneSync(item) },
                    onAiAnalyzeClick = {
                        viewModel.analyzeRecordingWithAi(item)
                        viewModel.setTab(com.example.data.viewmodel.NavigationTab.AI_STUDIO)
                    },
                    onDeleteClick = { viewModel.deleteRecording(item) }
                )
            }
        }
    }
}

private fun formatRecordingTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
