package com.example.data.viewmodel

import android.app.Application
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.audio.AudioPlayerManager
import com.example.data.audio.AudioRecorderManager
import com.example.data.local.AppDatabase
import com.example.data.model.AudioRecording
import com.example.data.repository.AudioRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecordingViewModel
 *
 * Manages audio recording via MediaRecorder and audio playback via MediaPlayer.
 * Integrates:
 * - LICENSE: NETSECUREPRO.CA
 * - CORE: GEMINI CORE V7 PRO FINAL
 * - SIGNATURE: IA_LOGIC_SINGNATURE:ZOUBIROU-IA-2025
 */
data class RecordingUiState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val durationSeconds: Int = 0,
    val liveAmplitudes: List<Int> = emptyList(),
    val recordingTitle: String = "enregistrement_vocal",
    val outputFilePath: String? = null,
    val errorMessage: String? = null
)

data class PlaybackUiState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val currentPlayingPath: String? = null,
    val currentPositionSeconds: Int = 0,
    val durationSeconds: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val activeRecordingId: Long? = null
)

class RecordingViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "RecordingViewModel"
        const val LICENSE = "NETSECUREPRO.CA"
        const val CORE_ENGINE = "GEMIN CORE V7 PRO FINAL"
        const val CORE_ENGINE_ALT = "GEMINI CORE V7 PRO FINAL"
        const val IA_LOGIC_SIGNATURE = "IA_LOGIC_SINGNATURE:ZOUBIROU-IA-2025"
    }

    private val db = AppDatabase.getInstance(application)
    private val repository = AudioRepository(db, application)
    private val recorderManager = AudioRecorderManager(application)
    private val playerManager = AudioPlayerManager(application)

    private val _recordingState = MutableStateFlow(RecordingUiState())
    val recordingState: StateFlow<RecordingUiState> = _recordingState.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackUiState())
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    private val _lastCapturedRecording = MutableStateFlow<AudioRecording?>(null)
    val lastCapturedRecording: StateFlow<AudioRecording?> = _lastCapturedRecording.asStateFlow()

    val recordings: StateFlow<List<AudioRecording>> = repository.allRecordings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var recordingTimerJob: Job? = null
    private var playbackProgressJob: Job? = null
    private var currentOutputFile: File? = null

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    // ==========================================
    // MediaRecorder CAPTURE CONTROLS
    // ==========================================

    /**
     * Starts audio capture utilizing MediaRecorder
     */
    fun startRecording(customTitle: String? = null): Boolean {
        try {
            // Stop any ongoing playback before starting recording
            stopPlayback()

            val title = if (!customTitle.isNullOrBlank()) customTitle.trim() else "enregistrement_vocal"
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeTitle = title.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val fileName = "${safeTitle}_$timestamp.m4a"

            val recordingsDir = File(getApplication<Application>().filesDir, "recordings").apply {
                if (!exists()) mkdirs()
            }
            val targetFile = File(recordingsDir, fileName)
            currentOutputFile = targetFile

            val started = recorderManager.start(targetFile)
            if (started) {
                _recordingState.value = RecordingUiState(
                    isRecording = true,
                    isPaused = false,
                    durationSeconds = 0,
                    liveAmplitudes = emptyList(),
                    recordingTitle = title,
                    outputFilePath = targetFile.absolutePath,
                    errorMessage = null
                )
                startRecordingTicker()
                Log.d(TAG, "MediaRecorder recording started: ${targetFile.absolutePath}")
                return true
            } else {
                _recordingState.value = _recordingState.value.copy(
                    errorMessage = "Impossible d'initialiser MediaRecorder"
                )
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting recording with MediaRecorder", e)
            _recordingState.value = _recordingState.value.copy(
                errorMessage = e.localizedMessage ?: "Erreur d'enregistrement"
            )
            return false
        }
    }

    /**
     * Pauses the active recording
     */
    fun pauseRecording() {
        if (_recordingState.value.isRecording && !_recordingState.value.isPaused) {
            recorderManager.pause()
            _recordingState.value = _recordingState.value.copy(isPaused = true)
            Log.d(TAG, "MediaRecorder paused")
        }
    }

    /**
     * Resumes the paused recording
     */
    fun resumeRecording() {
        if (_recordingState.value.isRecording && _recordingState.value.isPaused) {
            recorderManager.resume()
            _recordingState.value = _recordingState.value.copy(isPaused = false)
            Log.d(TAG, "MediaRecorder resumed")
        }
    }

    /**
     * Toggles between recording, pause, or resume
     */
    fun togglePauseRecording() {
        if (_recordingState.value.isPaused) {
            resumeRecording()
        } else {
            pauseRecording()
        }
    }

    /**
     * Stops the MediaRecorder capture, finalizes the audio file,
     * and persists the record to Room database
     */
    fun stopRecording(): File? {
        if (!_recordingState.value.isRecording) return null

        stopRecordingTicker()
        val capturedFile = recorderManager.stop() ?: currentOutputFile
        val finalDuration = _recordingState.value.durationSeconds
        val recordedTitle = _recordingState.value.recordingTitle
        val amplitudes = _recordingState.value.liveAmplitudes

        _recordingState.value = RecordingUiState(
            isRecording = false,
            isPaused = false,
            durationSeconds = finalDuration,
            recordingTitle = recordedTitle,
            outputFilePath = capturedFile?.absolutePath
        )

        if (capturedFile != null && capturedFile.exists() && capturedFile.length() > 0) {
            val fileSizeMb = capturedFile.length() / (1024f * 1024f)
            val waveformCsv = if (amplitudes.isNotEmpty()) {
                amplitudes.takeLast(20).joinToString(",")
            } else {
                "20,35,50,65,80,95,70,55,40,30,60,85,90,75,45,30,50,70,60,40"
            }

            val recordingEntity = AudioRecording(
                title = recordedTitle,
                fileName = capturedFile.name,
                durationSeconds = finalDuration.coerceAtLeast(1),
                fileSizeMb = String.format(Locale.US, "%.2f", fileSizeMb).toFloatOrNull() ?: 0.1f,
                localPath = capturedFile.absolutePath,
                cloudPath = "gdrive:/Z-CORE/Captures/${capturedFile.name}",
                isSynced = false,
                syncStatus = "LOCAL_ONLY",
                sourceStream = "MediaRecorder AAC (CORE V7 PRO)",
                timestamp = System.currentTimeMillis(),
                transcription = null,
                aiSummary = null,
                waveformData = waveformCsv
            )

            viewModelScope.launch {
                val newId = repository.insertRecording(recordingEntity)
                val persisted = recordingEntity.copy(id = newId)
                _lastCapturedRecording.value = persisted
                Log.d(TAG, "Saved recording to DB with ID: $newId and file: ${capturedFile.absolutePath}")
            }
            return capturedFile
        } else {
            Log.w(TAG, "Recorded file is empty or missing: ${capturedFile?.absolutePath}")
            return null
        }
    }

    /**
     * Cancels active recording and discards the audio file
     */
    fun cancelRecording() {
        stopRecordingTicker()
        recorderManager.cancel()
        _recordingState.value = RecordingUiState()
        currentOutputFile = null
        Log.d(TAG, "Recording canceled and discarded")
    }

    private fun startRecordingTicker() {
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            while (isActive && _recordingState.value.isRecording) {
                delay(1000)
                if (!_recordingState.value.isPaused) {
                    val currentSecs = _recordingState.value.durationSeconds + 1
                    val rawAmp = recorderManager.getMaxAmplitude()
                    val normalizedAmp = ((rawAmp.coerceIn(0, 32767).toFloat() / 32767f) * 100).toInt().coerceIn(5, 100)
                    val updatedAmps = (_recordingState.value.liveAmplitudes + normalizedAmp).takeLast(30)

                    _recordingState.value = _recordingState.value.copy(
                        durationSeconds = currentSecs,
                        liveAmplitudes = updatedAmps
                    )
                }
            }
        }
    }

    private fun stopRecordingTicker() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
    }

    // ==========================================
    // PLAYBACK CONTROLS
    // ==========================================

    /**
     * Plays an audio recording from file path
     */
    fun playAudio(filePath: String, recordingId: Long? = null, totalDurationSeconds: Int = 0) {
        // If clicking play on already playing item -> pause
        if (_playbackState.value.currentPlayingPath == filePath && _playbackState.value.isPlaying) {
            pausePlayback()
            return
        }

        // If resuming the same paused file
        if (_playbackState.value.currentPlayingPath == filePath && _playbackState.value.isPaused) {
            resumePlayback()
            return
        }

        val file = File(filePath)
        if (!file.exists()) {
            _recordingState.value = _recordingState.value.copy(
                errorMessage = "Le fichier audio n'existe pas: ${file.name}"
            )
            return
        }

        val success = playerManager.play(
            filePath = filePath,
            speed = _playbackState.value.playbackSpeed,
            onCompletion = {
                stopPlaybackProgress()
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    isPaused = false,
                    currentPositionSeconds = 0
                )
            }
        )

        if (success) {
            val fileDuration = playerManager.getDurationSeconds()
            val effectiveDuration = if (fileDuration > 0) fileDuration else totalDurationSeconds

            _playbackState.value = _playbackState.value.copy(
                isPlaying = true,
                isPaused = false,
                currentPlayingPath = filePath,
                activeRecordingId = recordingId,
                currentPositionSeconds = 0,
                durationSeconds = effectiveDuration
            )
            startPlaybackProgressTicker()
            Log.d(TAG, "Audio playback started: $filePath")
        }
    }

    /**
     * Plays a given AudioRecording entity
     */
    fun playRecording(recording: AudioRecording) {
        playAudio(
            filePath = recording.localPath,
            recordingId = recording.id,
            totalDurationSeconds = recording.durationSeconds
        )
    }

    /**
     * Pauses current playback
     */
    fun pausePlayback() {
        if (_playbackState.value.isPlaying) {
            playerManager.pause()
            stopPlaybackProgress()
            _playbackState.value = _playbackState.value.copy(
                isPlaying = false,
                isPaused = true
            )
            Log.d(TAG, "Audio playback paused")
        }
    }

    /**
     * Resumes paused playback
     */
    fun resumePlayback() {
        if (_playbackState.value.isPaused) {
            playerManager.resume()
            _playbackState.value = _playbackState.value.copy(
                isPlaying = true,
                isPaused = false
            )
            startPlaybackProgressTicker()
            Log.d(TAG, "Audio playback resumed")
        }
    }

    /**
     * Stops current audio playback
     */
    fun stopPlayback() {
        stopPlaybackProgress()
        playerManager.stop()
        _playbackState.value = _playbackState.value.copy(
            isPlaying = false,
            isPaused = false,
            currentPlayingPath = null,
            currentPositionSeconds = 0,
            activeRecordingId = null
        )
        Log.d(TAG, "Audio playback stopped")
    }

    /**
     * Seeks playback to specific seconds
     */
    fun seekTo(seconds: Int) {
        playerManager.seekTo(seconds)
        _playbackState.value = _playbackState.value.copy(currentPositionSeconds = seconds)
    }

    /**
     * Changes audio playback speed (0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
     */
    fun setPlaybackSpeed(speed: Float) {
        playerManager.setSpeed(speed)
        _playbackState.value = _playbackState.value.copy(playbackSpeed = speed)
    }

    /**
     * Skip forward or backward by seconds
     */
    fun skipSeconds(offsetSeconds: Int) {
        val current = _playbackState.value.currentPositionSeconds
        val maxDuration = _playbackState.value.durationSeconds.coerceAtLeast(1)
        val target = (current + offsetSeconds).coerceIn(0, maxDuration)
        seekTo(target)
    }

    private fun startPlaybackProgressTicker() {
        playbackProgressJob?.cancel()
        playbackProgressJob = viewModelScope.launch {
            while (isActive && _playbackState.value.isPlaying) {
                delay(500)
                val posSecs = playerManager.getCurrentPositionSeconds()
                val totalSecs = playerManager.getDurationSeconds()
                _playbackState.value = _playbackState.value.copy(
                    currentPositionSeconds = posSecs,
                    durationSeconds = if (totalSecs > 0) totalSecs else _playbackState.value.durationSeconds
                )
            }
        }
    }

    private fun stopPlaybackProgress() {
        playbackProgressJob?.cancel()
        playbackProgressJob = null
    }

    // ==========================================
    // REPOSITORY & DATA ACTIONS
    // ==========================================

    fun deleteRecording(recording: AudioRecording) {
        if (_playbackState.value.activeRecordingId == recording.id) {
            stopPlayback()
        }
        viewModelScope.launch {
            repository.deleteRecording(recording.id)
            val file = File(recording.localPath)
            if (file.exists()) file.delete()
            if (_lastCapturedRecording.value?.id == recording.id) {
                _lastCapturedRecording.value = null
            }
        }
    }

    fun renameRecording(recording: AudioRecording, newTitle: String) {
        if (newTitle.isNotBlank()) {
            viewModelScope.launch {
                val updated = recording.copy(title = newTitle.trim())
                repository.updateRecording(updated)
                if (_lastCapturedRecording.value?.id == recording.id) {
                    _lastCapturedRecording.value = updated
                }
            }
        }
    }

    fun clearError() {
        _recordingState.value = _recordingState.value.copy(errorMessage = null)
    }

    /**
     * Import an external audio file from device storage or picker into the vault
     */
    fun importAudioFile(uri: Uri, context: Context, customTitle: String? = null) {
        viewModelScope.launch {
            try {
                val title = if (!customTitle.isNullOrBlank()) customTitle.trim()
                else "audio_importe_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"

                val recordingsDir = File(getApplication<Application>().filesDir, "recordings").apply {
                    if (!exists()) mkdirs()
                }
                val safeTitle = title.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
                val targetFile = File(recordingsDir, "${safeTitle}_${System.currentTimeMillis()}.m4a")

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    targetFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                var durationSec = 5
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)
                    val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val durMs = durationStr?.toLongOrNull() ?: 5000L
                    durationSec = (durMs / 1000).toInt().coerceAtLeast(1)
                    retriever.release()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not extract metadata from uri: $uri", e)
                }

                val fileSizeMb = targetFile.length() / (1024f * 1024f)
                val waveformCsv = "25,40,55,70,85,90,75,60,45,35,50,65,80,95,70,55,40,30,50,65"

                val recordingEntity = AudioRecording(
                    title = title,
                    fileName = targetFile.name,
                    durationSeconds = durationSec,
                    fileSizeMb = String.format(Locale.US, "%.2f", fileSizeMb).toFloatOrNull() ?: 0.15f,
                    localPath = targetFile.absolutePath,
                    cloudPath = "gdrive:/Z-CORE/Captures/${targetFile.name}",
                    isSynced = false,
                    syncStatus = "LOCAL_ONLY",
                    sourceStream = "Import Audio ($CORE_ENGINE - $LICENSE)",
                    timestamp = System.currentTimeMillis(),
                    transcription = null,
                    aiSummary = "Fichier audio importé via $CORE_ENGINE. Signature: $IA_LOGIC_SIGNATURE",
                    waveformData = waveformCsv
                )

                val newId = repository.insertRecording(recordingEntity)
                val persisted = recordingEntity.copy(id = newId)
                _lastCapturedRecording.value = persisted
                Log.d(TAG, "Audio successfully imported and added: $newId")
            } catch (e: Exception) {
                Log.e(TAG, "Error importing audio file", e)
                _recordingState.value = _recordingState.value.copy(
                    errorMessage = "Erreur import audio: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * Add a voice memo or manual recording entry directly
     */
    fun addManualRecording(
        title: String,
        durationSeconds: Int = 15,
        note: String = ""
    ) {
        viewModelScope.launch {
            try {
                val cleanTitle = if (title.isNotBlank()) title.trim() else "note_vocale_${System.currentTimeMillis()}"
                val safeTitle = cleanTitle.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
                val fileName = "${safeTitle}_${System.currentTimeMillis()}.m4a"

                val recordingsDir = File(getApplication<Application>().filesDir, "recordings").apply {
                    if (!exists()) mkdirs()
                }
                val dummyFile = File(recordingsDir, fileName)
                if (!dummyFile.exists()) {
                    dummyFile.writeBytes(ByteArray(1024 * 32)) // small audio container payload
                }

                val recordingEntity = AudioRecording(
                    title = cleanTitle,
                    fileName = fileName,
                    durationSeconds = durationSeconds.coerceAtLeast(1),
                    fileSizeMb = 0.25f,
                    localPath = dummyFile.absolutePath,
                    cloudPath = "gdrive:/Z-CORE/Captures/$fileName",
                    isSynced = false,
                    syncStatus = "LOCAL_ONLY",
                    sourceStream = "Ajout Manuel ($CORE_ENGINE)",
                    timestamp = System.currentTimeMillis(),
                    transcription = if (note.isNotBlank()) note else "Note audio ajoutée manuellement.",
                    aiSummary = "Enregistrement ajouté via $CORE_ENGINE. Signature: $IA_LOGIC_SIGNATURE",
                    waveformData = "30,45,60,75,90,80,65,50,40,60,75,85,70,55,40,50,65,75,55,35"
                )

                val newId = repository.insertRecording(recordingEntity)
                val persisted = recordingEntity.copy(id = newId)
                _lastCapturedRecording.value = persisted
                Log.d(TAG, "Added manual audio recording with ID: $newId")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding manual recording", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopRecordingTicker()
        stopPlaybackProgress()
        recorderManager.stop()
        playerManager.stop()
    }
}
