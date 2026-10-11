package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.viewmodel.AudioSyncViewModel
import com.example.data.viewmodel.RecordingViewModel
import com.example.ui.components.AddAudioNoteDialog
import com.example.ui.components.AudioPlayerCard
import com.example.ui.components.RecordingComponent
import com.google.accompanist.permissions.ExperimentalPermissionsApi

@Composable
fun RecordingScreen(
    recordingViewModel: RecordingViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        RecordingComponent(viewModel = recordingViewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RecordingScreen(
    viewModel: AudioSyncViewModel,
    recordingViewModel: RecordingViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playerState by viewModel.playerState.collectAsState()
    val recordings by viewModel.recordings.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var selectedScreenMode by remember { mutableIntStateOf(0) } // 0 = Studio MediaRecorder, 1 = Bibliothèque
    var showAddManualDialog by remember { mutableStateOf(false) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            recordingViewModel.importAudioFile(it, context)
        }
    }

    if (showAddManualDialog) {
        AddAudioNoteDialog(
            onDismiss = { showAddManualDialog = false },
            onConfirm = { title, durationSec, note ->
                recordingViewModel.addManualRecording(title, durationSec, note)
                showAddManualDialog = false
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tab Selector between MediaRecorder Studio and Audio Vault
            TabRow(
                selectedTabIndex = selectedScreenMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("recording_screen_tabs"),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Tab(
                    selected = selectedScreenMode == 0,
                    onClick = { selectedScreenMode = 0 },
                    modifier = Modifier.testTag("tab_media_recorder_studio"),
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Capture MediaRecorder", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
                Tab(
                    selected = selectedScreenMode == 1,
                    onClick = { selectedScreenMode = 1 },
                    modifier = Modifier.testTag("tab_audio_vault_library"),
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.LibraryMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Bibliothèque (${recordings.size})", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            if (selectedScreenMode == 0) {
                // MediaRecorder Capture Studio with full playback controls
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    RecordingComponent(
                        viewModel = recordingViewModel,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // Library of recordings with search, quick add/import, and AudioPlayerCard
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Rechercher...") },
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
                                .weight(1f)
                                .testTag("search_recordings_input_history"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        FilledTonalIconButton(
                            onClick = { audioPickerLauncher.launch("audio/*") },
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("btn_library_import_audio"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.UploadFile, contentDescription = "Importer un fichier audio")
                        }

                        IconButton(
                            onClick = { showAddManualDialog = true },
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                .testTag("btn_library_add_manual"),
                        ) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = "Ajouter un enregistrement", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }

                    if (recordings.isEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Aucun enregistrement dans la voûte.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { showAddManualDialog = true },
                                    modifier = Modifier.testTag("btn_empty_add_recording")
                                ) {
                                    Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ajouter un enregistrement")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 72.dp)
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
                                    onDeleteClick = { viewModel.deleteRecording(item) },
                                    onRenameClick = { newTitle -> viewModel.renameRecording(item, newTitle) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button for Quick Add
        if (selectedScreenMode == 1) {
            FloatingActionButton(
                onClick = { showAddManualDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("fab_add_recording_screen"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Ajouter un enregistrement")
            }
        }
    }
}
