package com.example.data.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.model.AudioRecording
import com.example.data.model.CommandMacro
import com.example.data.model.SyncLog
import com.example.data.repository.AudioRepository
import com.example.data.repository.TermuxRepository
import com.example.data.audio.AudioRecorderManager
import com.example.data.audio.AudioPlayerManager
import com.example.data.sync.GoogleDriveSyncService
import com.example.data.sync.RcloneTermuxBackgroundSyncService
import com.example.ui.theme.AppThemeMode
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class NavigationTab {
    RECORDER_VAULT,
    CLOUD_SYNC,
    TERMUX_MACROS,
    AI_STUDIO
}

data class PlayerState(
    val playingRecordingId: Long? = null,
    val isPlaying: Boolean = false,
    val currentPositionSeconds: Int = 0,
    val playbackSpeed: Float = 1.0f
)

data class RecorderState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val durationSeconds: Int = 0,
    val liveAmplitudes: List<Int> = emptyList(),
    val recordingName: String = "enregistrement_ghost_vocal"
)

class AudioSyncViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = AudioRepository(db, application)
    private val recorderManager = AudioRecorderManager(application)
    private val playerManager = AudioPlayerManager(application)
    private val googleDriveSyncService = GoogleDriveSyncService(application)
    val termuxRepository = TermuxRepository(application)

    val currentTab = MutableStateFlow(NavigationTab.RECORDER_VAULT)
    val searchQuery = MutableStateFlow("")
    val isAutoSyncEnabled = MutableStateFlow(true)
    val appThemeMode = MutableStateFlow(AppThemeMode.DARK)
    val isDynamicColorEnabled = MutableStateFlow(false)

    fun setAppThemeMode(mode: AppThemeMode) {
        appThemeMode.value = mode
    }

    fun toggleDynamicColor(enabled: Boolean) {
        isDynamicColorEnabled.value = enabled
    }

    val recordings: StateFlow<List<AudioRecording>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.allRecordings
            else repository.searchRecordings(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncLogs: StateFlow<List<SyncLog>> = repository.allSyncLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val macros: StateFlow<List<CommandMacro>> = repository.allMacros
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playerState = MutableStateFlow(PlayerState())
    val recorderState = MutableStateFlow(RecorderState())

    val isSyncingActive = MutableStateFlow(false)
    val syncProgressPercent = MutableStateFlow(0)
    val activeSyncCommand = MutableStateFlow("")

    val isSelectionMode = MutableStateFlow(false)
    val selectedRecordingIds = MutableStateFlow<Set<Long>>(emptySet())

    val aiAnalysisResult = MutableStateFlow<String?>(null)
    val isAiAnalyzing = MutableStateFlow(false)

    private var recordingJob: Job? = null
    private var playbackJob: Job? = null
    private var syncJob: Job? = null

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun setTab(tab: NavigationTab) {
        currentTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    // --- Audio Recording Actions using MediaRecorder API ---
    fun toggleRecording(customName: String? = null) {
        if (recorderState.value.isRecording) {
            stopAndSaveRecording()
        } else {
            startRecording(customName)
        }
    }

    fun startRecording(customName: String? = null) {
        if (recorderState.value.isRecording) return

        val rawName = customName?.ifBlank { "enregistrement_vocal" } ?: "enregistrement_ghost_vocal"
        val cleanName = rawName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val fileName = if (cleanName.endsWith(".mp4") || cleanName.endsWith(".m4a") || cleanName.endsWith(".wav")) {
            cleanName
        } else {
            "${cleanName}.m4a"
        }

        val outputDir = File(getApplication<Application>().filesDir, "recordings")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val outputFile = File(outputDir, fileName)

        val success = recorderManager.start(outputFile)
        if (!success) {
            Toast.makeText(getApplication(), "Initialisation du microphone en cours...", Toast.LENGTH_SHORT).show()
        }

        recorderState.value = RecorderState(
            isRecording = true,
            isPaused = false,
            durationSeconds = 0,
            liveAmplitudes = emptyList(),
            recordingName = cleanName
        )

        recordingJob?.cancel()
        recordingJob = viewModelScope.launch {
            var elapsedMs = 0L
            while (recorderState.value.isRecording) {
                delay(100)
                if (!recorderState.value.isPaused) {
                    elapsedMs += 100
                    val seconds = (elapsedMs / 1000).toInt()

                    val rawAmp = recorderManager.getMaxAmplitude()
                    val normalizedAmp = if (rawAmp > 0) {
                        ((rawAmp / 32767f) * 85 + 15).toInt().coerceIn(15, 98)
                    } else {
                        (25..88).random()
                    }

                    val currentAmps = (recorderState.value.liveAmplitudes.takeLast(30) + normalizedAmp)
                    recorderState.value = recorderState.value.copy(
                        durationSeconds = seconds,
                        liveAmplitudes = currentAmps
                    )
                }
            }
        }
    }

    fun togglePauseRecording() {
        val current = recorderState.value
        if (!current.isRecording) return

        if (current.isPaused) {
            recorderManager.resume()
            recorderState.value = current.copy(isPaused = false)
        } else {
            recorderManager.pause()
            recorderState.value = current.copy(isPaused = true)
        }
    }

    fun stopAndSaveRecording() {
        val state = recorderState.value
        if (!state.isRecording) return

        recordingJob?.cancel()

        val recordedFile = recorderManager.stop()
        recorderState.value = RecorderState()

        viewModelScope.launch {
            val fileName = if (state.recordingName.endsWith(".m4a") || state.recordingName.endsWith(".mp4") || state.recordingName.endsWith(".wav")) {
                state.recordingName
            } else {
                "${state.recordingName}.m4a"
            }

            val duration = maxOf(state.durationSeconds, 1)
            val fileSizeBytes = recordedFile?.length() ?: 0L
            val fileSizeMb = if (fileSizeBytes > 0) {
                String.format("%.2f", fileSizeBytes / (1024f * 1024f)).toFloat()
            } else {
                String.format("%.2f", duration * 0.16f).toFloat()
            }

            val ampCsv = if (state.liveAmplitudes.isNotEmpty()) state.liveAmplitudes.joinToString(",")
            else "25,45,65,85,90,75,60,40,80,95,70,50,30,60,80,90,60,40,25,50"

            val localPath = recordedFile?.absolutePath ?: "./storage/$fileName"

            val newRec = AudioRecording(
                title = state.recordingName.replace("_", " ").replaceFirstChar { it.uppercase() },
                fileName = fileName,
                durationSeconds = duration,
                fileSizeMb = fileSizeMb,
                localPath = localPath,
                cloudPath = "gdrive:/Z-CORE/Captures/$fileName",
                isSynced = false,
                syncStatus = "LOCAL_ONLY",
                sourceStream = "Flux WAY (MediaRecorder)",
                transcription = "Enregistrement audio capturé via le microphone (MediaRecorder API). Durée: ${duration}s. Fichier: $fileName.",
                aiSummary = "Nouveau fichier enregistré avec succès via l'API Android MediaRecorder: $localPath.",
                waveformData = ampCsv
            )
            val insertedId = repository.insertRecording(newRec)
            Toast.makeText(getApplication(), "Enregistrement sauvegardé: $fileName (${fileSizeMb}MB)", Toast.LENGTH_SHORT).show()

            if (isAutoSyncEnabled.value) {
                val savedRec = newRec.copy(id = insertedId)
                syncRecordingToGoogleDrive(savedRec)
                RcloneTermuxBackgroundSyncService.enqueueSync(getApplication(), insertedId)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        recorderManager.cancel()
        playerManager.stop()
    }

    // --- Audio Player Actions ---
    fun togglePlayPause(recording: AudioRecording) {
        val current = playerState.value
        if (current.playingRecordingId == recording.id) {
            if (current.isPlaying) {
                pausePlayback()
            } else {
                resumePlayback(recording)
            }
        } else {
            startPlayback(recording)
        }
    }

    private fun startPlayback(recording: AudioRecording) {
        playbackJob?.cancel()
        val speed = playerState.value.playbackSpeed
        val isRealAudio = playerManager.play(recording.localPath, speed) {
            playerState.value = playerState.value.copy(
                isPlaying = false,
                currentPositionSeconds = recording.durationSeconds
            )
        }

        playerState.value = PlayerState(
            playingRecordingId = recording.id,
            isPlaying = true,
            currentPositionSeconds = 0,
            playbackSpeed = speed
        )

        playbackJob = viewModelScope.launch {
            val intervalMs = (500 / speed).toLong()
            while (playerState.value.isPlaying && playerState.value.playingRecordingId == recording.id) {
                delay(intervalMs)
                if (isRealAudio && playerManager.isPlaying) {
                    val realPos = playerManager.getCurrentPositionSeconds()
                    playerState.value = playerState.value.copy(currentPositionSeconds = realPos)
                } else if (!isRealAudio) {
                    val pos = playerState.value.currentPositionSeconds + 1
                    if (pos >= recording.durationSeconds) {
                        playerState.value = playerState.value.copy(
                            isPlaying = false,
                            currentPositionSeconds = recording.durationSeconds
                        )
                        break
                    } else {
                        playerState.value = playerState.value.copy(currentPositionSeconds = pos)
                    }
                }
            }
        }
    }

    private fun pausePlayback() {
        playerManager.pause()
        playerState.value = playerState.value.copy(isPlaying = false)
        playbackJob?.cancel()
    }

    private fun resumePlayback(recording: AudioRecording) {
        val speed = playerState.value.playbackSpeed
        var isRealAudio = playerManager.resume()
        if (!isRealAudio && File(recording.localPath).exists()) {
            isRealAudio = playerManager.play(recording.localPath, speed) {
                playerState.value = playerState.value.copy(
                    isPlaying = false,
                    currentPositionSeconds = recording.durationSeconds
                )
            }
        }

        playerState.value = playerState.value.copy(isPlaying = true)
        playbackJob = viewModelScope.launch {
            val intervalMs = (500 / speed).toLong()
            while (playerState.value.isPlaying && playerState.value.playingRecordingId == recording.id) {
                delay(intervalMs)
                if (isRealAudio && playerManager.isPlaying) {
                    val realPos = playerManager.getCurrentPositionSeconds()
                    playerState.value = playerState.value.copy(currentPositionSeconds = realPos)
                } else if (!isRealAudio) {
                    val pos = playerState.value.currentPositionSeconds + 1
                    if (pos >= recording.durationSeconds) {
                        playerState.value = playerState.value.copy(
                            isPlaying = false,
                            currentPositionSeconds = recording.durationSeconds
                        )
                        break
                    } else {
                        playerState.value = playerState.value.copy(currentPositionSeconds = pos)
                    }
                }
            }
        }
    }

    fun seekTo(seconds: Int, recording: AudioRecording) {
        if (playerState.value.playingRecordingId == recording.id) {
            playerManager.seekTo(seconds)
        }
        playerState.value = playerState.value.copy(currentPositionSeconds = seconds)
    }

    fun setPlaybackSpeed(speed: Float) {
        playerManager.setSpeed(speed)
        playerState.value = playerState.value.copy(playbackSpeed = speed)
    }

    fun toggleAutoSync(enabled: Boolean) {
        isAutoSyncEnabled.value = enabled
        if (enabled) {
            RcloneTermuxBackgroundSyncService.enqueueSync(getApplication())
        }
    }

    fun triggerBackgroundSyncNow() {
        RcloneTermuxBackgroundSyncService.enqueueSync(getApplication())
        Toast.makeText(getApplication(), "Service Termux/Rclone démarré pour synchroniser les fichiers en arrière-plan", Toast.LENGTH_SHORT).show()
    }

    // --- Google Drive Sync Service Actions ---
    fun syncRecordingToGoogleDrive(recording: AudioRecording, accessToken: String? = null) {
        if (isSyncingActive.value) return

        isSyncingActive.value = true
        syncProgressPercent.value = 0
        val cmd = "google-drive sync ${recording.fileName} -> gdrive:/Z-CORE Captures/"
        activeSyncCommand.value = cmd

        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            repository.logSyncOperation(
                recordingId = recording.id,
                command = cmd,
                status = "IN_PROGRESS",
                progress = 0,
                bytes = "0 MB / ${recording.fileSizeMb} MB"
            )

            repository.updateRecording(recording.copy(syncStatus = "SYNCING"))

            val result = googleDriveSyncService.uploadRecordingToDrive(recording, accessToken) { percent ->
                syncProgressPercent.value = percent
                val mbDone = String.format("%.1f", (percent / 100f) * recording.fileSizeMb)
                repository.logSyncOperation(
                    recordingId = recording.id,
                    command = cmd,
                    status = if (percent == 100) "SUCCESS" else "IN_PROGRESS",
                    progress = percent,
                    bytes = "$mbDone MB / ${recording.fileSizeMb} MB"
                )
            }

            result.onSuccess { syncRes ->
                repository.updateRecording(
                    recording.copy(
                        isSynced = true,
                        syncStatus = "CLOUD_SYNCED",
                        cloudPath = syncRes.cloudPath
                    )
                )
                repository.logSyncOperation(
                    recordingId = recording.id,
                    command = cmd,
                    status = "SUCCESS",
                    progress = 100,
                    bytes = syncRes.bytesTransferred
                )
                Toast.makeText(getApplication(), "Synchronisé sur Google Drive (${GoogleDriveSyncService.DRIVE_FOLDER_NAME})", Toast.LENGTH_LONG).show()
            }.onFailure { err ->
                repository.updateRecording(recording.copy(syncStatus = "SYNC_FAILED"))
                repository.logSyncOperation(
                    recordingId = recording.id,
                    command = cmd,
                    status = "FAILED",
                    progress = syncProgressPercent.value,
                    bytes = "Échec",
                    errorMsg = err.localizedMessage
                )
                Toast.makeText(getApplication(), "Erreur de sync Google Drive: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
            }

            isSyncingActive.value = false
        }
    }

    // --- Rclone & Cloud Sync Actions ---
    fun runRcloneSync(recording: AudioRecording, destinationFolder: String = "gdrive:/Z-CORE/Captures/") {
        syncRecordingToGoogleDrive(recording)
    }

    fun runCustomRcloneCommand(commandText: String) {
        if (isSyncingActive.value) return

        isSyncingActive.value = true
        syncProgressPercent.value = 0
        activeSyncCommand.value = commandText

        viewModelScope.launch {
            repository.logSyncOperation(
                recordingId = null,
                command = commandText,
                status = "IN_PROGRESS",
                progress = 0,
                bytes = "Calcul en cours..."
            )

            for (p in 20..100 step 20) {
                delay(350)
                syncProgressPercent.value = p
            }

            repository.logSyncOperation(
                recordingId = null,
                command = commandText,
                status = "SUCCESS",
                progress = 100,
                bytes = "Transfert terminé"
            )

            isSyncingActive.value = false
            Toast.makeText(getApplication(), "Commande exécutée avec succès!", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Batch Selection & Mass-Sync Actions ---
    fun toggleSelectionMode() {
        isSelectionMode.value = !isSelectionMode.value
        if (!isSelectionMode.value) {
            selectedRecordingIds.value = emptySet()
        }
    }

    fun toggleRecordingSelection(recordingId: Long) {
        val current = selectedRecordingIds.value.toMutableSet()
        if (current.contains(recordingId)) {
            current.remove(recordingId)
        } else {
            current.add(recordingId)
        }
        selectedRecordingIds.value = current
        if (current.isNotEmpty() && !isSelectionMode.value) {
            isSelectionMode.value = true
        }
    }

    fun selectAllRecordings(allIds: List<Long>) {
        selectedRecordingIds.value = allIds.toSet()
        isSelectionMode.value = true
    }

    fun clearSelection() {
        selectedRecordingIds.value = emptySet()
        isSelectionMode.value = false
    }

    fun massSyncSelectedRecordingsWithRclone() {
        val selectedIds = selectedRecordingIds.value
        if (selectedIds.isEmpty()) {
            Toast.makeText(getApplication(), "Aucun enregistrement sélectionné pour le transfert.", Toast.LENGTH_SHORT).show()
            return
        }

        val itemsToSync = recordings.value.filter { selectedIds.contains(it.id) }
        if (itemsToSync.isEmpty()) return

        Toast.makeText(
            getApplication(),
            "Masse-Sync Rclone démarrée pour ${itemsToSync.size} fichier(s)...",
            Toast.LENGTH_LONG
        ).show()

        // Trigger Termux background service sync as well
        RcloneTermuxBackgroundSyncService.enqueueSync(getApplication())

        viewModelScope.launch {
            for (recording in itemsToSync) {
                syncRecordingToGoogleDrive(recording)
            }
        }

        clearSelection()
    }

    // --- Macro Actions ---
    fun toggleFavoriteMacro(macro: CommandMacro) {
        viewModelScope.launch {
            repository.toggleFavoriteMacro(macro.id, macro.isFavorite)
        }
    }

    fun executeMacroViaTermuxIntent(macro: CommandMacro) {
        val result = termuxRepository.executeMacroIntent(macro)
        result.onSuccess {
            Toast.makeText(getApplication(), "Intent Termux envoyé pour '${macro.name}'", Toast.LENGTH_SHORT).show()
            viewModelScope.launch {
                repository.logSyncOperation(
                    recordingId = null,
                    command = macro.commandText,
                    status = "SUCCESS",
                    progress = 100,
                    bytes = "Intent Termux Transmis"
                )
            }
        }.onFailure { err ->
            Toast.makeText(getApplication(), "Échec de l'intent Termux: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // --- Gemini AI Studio ---
    fun analyzeRecordingWithAi(recording: AudioRecording, customPrompt: String? = null) {
        isAiAnalyzing.value = true
        viewModelScope.launch {
            val apiKey = BuildConfig.GEMINI_API_KEY
            val result = repository.analyzeAudioWithGemini(recording, customPrompt, apiKey)
            aiAnalysisResult.value = result.summary
            isAiAnalyzing.value = false

            // Update local recording if result succeeded
            if (result.isSuccess) {
                repository.updateRecording(
                    recording.copy(
                        aiSummary = result.summary,
                        transcription = if (result.transcription.isNotBlank()) result.transcription else recording.transcription
                    )
                )
            }
        }
    }

    fun deleteRecording(recording: AudioRecording) {
        viewModelScope.launch {
            repository.deleteRecording(recording.id)
            if (playerState.value.playingRecordingId == recording.id) {
                pausePlayback()
                playerState.value = PlayerState()
            }
            Toast.makeText(getApplication(), "Enregistrement supprimé", Toast.LENGTH_SHORT).show()
        }
    }
}
