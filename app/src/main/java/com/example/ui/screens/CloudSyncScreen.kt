package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioRecording
import com.example.data.viewmodel.AudioSyncViewModel
import com.example.ui.theme.AmberPending
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSynced

enum class SyncFilter {
    ALL,
    UNSYNCED,
    SYNCED,
    SYNCING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncScreen(
    viewModel: AudioSyncViewModel,
    modifier: Modifier = Modifier
) {
    val syncLogs by viewModel.syncLogs.collectAsState()
    val isSyncing by viewModel.isSyncingActive.collectAsState()
    val syncProgress by viewModel.syncProgressPercent.collectAsState()
    val activeCommand by viewModel.activeSyncCommand.collectAsState()
    val recordings by viewModel.recordings.collectAsState()
    val isAutoSyncEnabled by viewModel.isAutoSyncEnabled.collectAsState()

    var selectedFilter by remember { mutableStateOf(SyncFilter.ALL) }
    var customCommandInput by remember {
        mutableStateOf("rclone copy ./storage/enregistrement_ghost_vocal.wav gdrive:/Z-CORE/Captures/")
    }

    // Stats calculations
    val totalFiles = recordings.size
    val totalSizeMb = recordings.sumOf { it.fileSizeMb.toDouble() }
    val syncedCount = recordings.count { it.isSynced || it.syncStatus == "CLOUD_SYNCED" }
    val pendingCount = recordings.count { !it.isSynced && it.syncStatus != "SYNCING" && it.syncStatus != "SYNC_FAILED" }
    val syncingCount = recordings.count { it.syncStatus == "SYNCING" }
    val failedCount = recordings.count { it.syncStatus == "SYNC_FAILED" || it.syncStatus == "ERROR" }

    val filteredRecordings = remember(recordings, selectedFilter) {
        when (selectedFilter) {
            SyncFilter.ALL -> recordings
            SyncFilter.UNSYNCED -> recordings.filter { !it.isSynced && it.syncStatus != "CLOUD_SYNCED" }
            SyncFilter.SYNCED -> recordings.filter { it.isSynced || it.syncStatus == "CLOUD_SYNCED" }
            SyncFilter.SYNCING -> recordings.filter { it.syncStatus == "SYNCING" }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Cloud Sync Engine Header & Overview Banner ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cloud_status_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Title Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.CloudSync,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Google Drive Sync Engine",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Bridge Cloud RClone & Termux (Z-CORE)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = EmeraldSynced.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSynced.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(EmeraldSynced, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CONNECTÉ",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSynced
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Synchronized Audio Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SyncMetricBox(
                            label = "Fichiers Synchro",
                            value = "$syncedCount/$totalFiles",
                            subText = "${String.format("%.1f", totalSizeMb)} MB",
                            color = EmeraldSynced,
                            icon = Icons.Filled.CloudDone,
                            modifier = Modifier.weight(1f)
                        )

                        SyncMetricBox(
                            label = "En Attente",
                            value = "$pendingCount",
                            subText = "Local Only",
                            color = AmberPending,
                            icon = Icons.Outlined.CloudUpload,
                            modifier = Modifier.weight(1f)
                        )

                        SyncMetricBox(
                            label = "Erreurs",
                            value = "$failedCount",
                            subText = "À re-tester",
                            color = if (failedCount > 0) CrimsonError else MaterialTheme.colorScheme.onSurfaceVariant,
                            icon = Icons.Outlined.ErrorOutline,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Auto-Sync Switch & Manual Batch Trigger
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Sync Arrière-Plan (Rclone)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Upload automatique des nouveaux enregistrements vers gdrive:/Z-CORE/Captures/",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isAutoSyncEnabled,
                            onCheckedChange = { viewModel.toggleAutoSync(it) },
                            modifier = Modifier.testTag("auto_sync_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val unsynced = recordings.filter { !it.isSynced }
                                if (unsynced.isNotEmpty()) {
                                    unsynced.forEach { viewModel.syncRecordingToGoogleDrive(it) }
                                } else {
                                    viewModel.triggerBackgroundSyncNow()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("bg_sync_trigger_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (pendingCount > 0) "Synchroniser Tout ($pendingCount)" else "Lancer Sync Arrière-Plan",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Progress Bar if Sync active
                    AnimatedVisibility(visible = isSyncing) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Transfert Rclone en cours...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "$syncProgress%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = syncProgress / 100f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .testTag("sync_progress_bar"),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activeCommand,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // --- 2. Local Audio File Sync Dashboard Section ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("file_sync_dashboard_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Section Header & Search/Filter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.FolderZip,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tableau des Fichiers Audio Local",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "${filteredRecordings.size} élément(s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filter Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFilter == SyncFilter.ALL,
                                onClick = { selectedFilter = SyncFilter.ALL },
                                label = { Text("Tous ($totalFiles)") },
                                leadingIcon = { Icon(Icons.Outlined.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.testTag("sync_filter_chip_all")
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == SyncFilter.UNSYNCED,
                                onClick = { selectedFilter = SyncFilter.UNSYNCED },
                                label = { Text("Non Synchro ($pendingCount)") },
                                leadingIcon = { Icon(Icons.Outlined.CloudQueue, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberPending.copy(alpha = 0.2f),
                                    selectedLabelColor = AmberPending
                                ),
                                modifier = Modifier.testTag("sync_filter_chip_unsynced")
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == SyncFilter.SYNCED,
                                onClick = { selectedFilter = SyncFilter.SYNCED },
                                label = { Text("Drive Synchro ($syncedCount)") },
                                leadingIcon = { Icon(Icons.Outlined.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldSynced.copy(alpha = 0.2f),
                                    selectedLabelColor = EmeraldSynced
                                ),
                                modifier = Modifier.testTag("sync_filter_chip_synced")
                            )
                        }
                        if (syncingCount > 0) {
                            item {
                                FilterChip(
                                    selected = selectedFilter == SyncFilter.SYNCING,
                                    onClick = { selectedFilter = SyncFilter.SYNCING },
                                    label = { Text("En cours ($syncingCount)") },
                                    leadingIcon = { CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Local Audio Files Sync List
                    if (filteredRecordings.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.SearchOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Aucun fichier audio trouvé pour ce filtre.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            filteredRecordings.forEach { recording ->
                                LocalAudioFileSyncItem(
                                    recording = recording,
                                    onSyncClick = { viewModel.syncRecordingToGoogleDrive(recording) },
                                    isSyncing = isSyncing && activeCommand.contains(recording.fileName)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. Interactive Rclone Command Console Launcher ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Console Commandes RClone & Termux",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Outlined.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customCommandInput,
                        onValueChange = { customCommandInput = it },
                        label = { Text("Commande RClone CLI") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.Terminal, contentDescription = null)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rclone_command_input"),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                customCommandInput = "rclone copy ./storage/ gdrive:/Z-CORE/Captures/ --progress"
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("rclone copy", fontSize = 11.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                customCommandInput = "rclone sync ./storage/ gdrive:/Z-CORE/Captures/ --delete-after"
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("rclone sync", fontSize = 11.sp, maxLines = 1)
                        }

                        Button(
                            onClick = { viewModel.runCustomRcloneCommand(customCommandInput) },
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("execute_rclone_btn")
                        ) {
                            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exécuter", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // --- 4. Recent Cloud Transfer Logs ---
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Historique des Transferts Cloud",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.triggerBackgroundSyncNow() }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Rafraîchir",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (syncLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucun historique de synchronisation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        syncLogs.take(8).forEach { log ->
                            SyncLogItem(log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalAudioFileSyncItem(
    recording: AudioRecording,
    onSyncClick: () -> Unit,
    isSyncing: Boolean
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (recording.isSynced || recording.syncStatus == "CLOUD_SYNCED") EmeraldSynced.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sync_file_item_${recording.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Audio Info Column
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = when {
                            recording.isSynced || recording.syncStatus == "CLOUD_SYNCED" -> EmeraldSynced.copy(alpha = 0.15f)
                            recording.syncStatus == "SYNCING" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            recording.syncStatus == "SYNC_FAILED" -> CrimsonError.copy(alpha = 0.15f)
                            else -> AmberPending.copy(alpha = 0.15f)
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when {
                                    recording.isSynced || recording.syncStatus == "CLOUD_SYNCED" -> Icons.Filled.CloudDone
                                    recording.syncStatus == "SYNCING" -> Icons.Filled.Sync
                                    recording.syncStatus == "SYNC_FAILED" -> Icons.Filled.CloudOff
                                    else -> Icons.Outlined.CloudUpload
                                },
                                contentDescription = null,
                                tint = when {
                                    recording.isSynced || recording.syncStatus == "CLOUD_SYNCED" -> EmeraldSynced
                                    recording.syncStatus == "SYNCING" -> MaterialTheme.colorScheme.primary
                                    recording.syncStatus == "SYNC_FAILED" -> CrimsonError
                                    else -> AmberPending
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = recording.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${recording.fileName} • ${recording.durationSeconds}s • ${recording.fileSizeMb} MB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Sync Action Button / Badge
                if (recording.isSynced || recording.syncStatus == "CLOUD_SYNCED") {
                    Surface(
                        color = EmeraldSynced.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = EmeraldSynced,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Drive Synchro",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSynced,
                                fontSize = 10.sp
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onSyncClick,
                        enabled = !isSyncing,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("sync_single_file_btn_${recording.id}")
                    ) {
                        if (isSyncing || recording.syncStatus == "SYNCING") {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.8.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("En cours...", fontSize = 10.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Uploader Drive", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Rclone Cloud Target Path Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudQueue,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = recording.cloudPath.ifBlank { "gdrive:/Z-CORE/Captures/${recording.fileName}" },
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SyncMetricBox(
    label: String,
    value: String,
    subText: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
fun SyncLogItem(log: com.example.data.model.SyncLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.commandExecuted,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${log.bytesTransferred} • Progression: ${log.progressPercent}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = when (log.status) {
                    "SUCCESS" -> EmeraldSynced.copy(alpha = 0.2f)
                    "IN_PROGRESS" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else -> CrimsonError.copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = log.status,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (log.status) {
                        "SUCCESS" -> EmeraldSynced
                        "IN_PROGRESS" -> MaterialTheme.colorScheme.primary
                        else -> CrimsonError
                    },
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

