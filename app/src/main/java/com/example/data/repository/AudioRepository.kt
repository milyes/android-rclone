package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.AudioRecording
import com.example.data.model.CommandMacro
import com.example.data.model.SyncLog
import com.example.data.model.RcloneRemote
import com.example.data.service.GeminiAudioAnalysisResult
import com.example.data.service.GeminiAudioAnalysisService
import com.example.data.service.GoogleDriveApiService
import com.example.data.service.GoogleDriveApiServiceImpl
import com.example.data.service.DriveAudioFile
import com.example.data.service.DriveUploadResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRepository(
    private val db: AppDatabase,
    private val context: Context? = null
) {

    private val geminiAudioService by lazy {
        context?.let { GeminiAudioAnalysisService(it) }
    }

    val googleDriveApiService: GoogleDriveApiService? by lazy {
        context?.let { GoogleDriveApiServiceImpl(it) }
    }

    val allRecordings: Flow<List<AudioRecording>> = db.audioRecordingDao().getAllRecordings()
    val allSyncLogs: Flow<List<SyncLog>> = db.syncLogDao().getAllSyncLogs()
    val allMacros: Flow<List<CommandMacro>> = db.commandMacroDao().getAllMacros()
    val allRemotes: Flow<List<RcloneRemote>> = db.rcloneRemoteDao().getAllRemotes()

    fun searchRecordings(query: String): Flow<List<AudioRecording>> {
        val dateFormatIso = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateFormatFr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateFormatShort = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val queryLower = query.lowercase().trim()

        return db.audioRecordingDao().getAllRecordings().map { list ->
            list.filter { item ->
                val dateStr1 = dateFormatIso.format(Date(item.timestamp)).lowercase()
                val dateStr2 = dateFormatFr.format(Date(item.timestamp)).lowercase()
                val dateStr3 = dateFormatShort.format(Date(item.timestamp)).lowercase()
                val timestampRaw = item.timestamp.toString()

                item.fileName.lowercase().contains(queryLower) ||
                item.title.lowercase().contains(queryLower) ||
                item.localPath.lowercase().contains(queryLower) ||
                dateStr1.contains(queryLower) ||
                dateStr2.contains(queryLower) ||
                dateStr3.contains(queryLower) ||
                timestampRaw.contains(queryLower)
            }
        }
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        if (db.audioRecordingDao().getCount() == 0) {
            val initialRecordings = listOf(
                AudioRecording(
                    title = "Flux WAY Vocal Capture",
                    fileName = "enregistrement_ghost_vocal.wav",
                    durationSeconds = 60,
                    fileSizeMb = 10.4f,
                    localPath = "./storage/enregistrement_ghost_vocal.wav",
                    cloudPath = "gdrive:/Z-CORE/Captures/enregistrement_ghost_vocal.wav",
                    isSynced = false,
                    syncStatus = "LOCAL_ONLY",
                    sourceStream = "Flux WAY (Z-CORE)",
                    timestamp = System.currentTimeMillis() - 3600000,
                    transcription = "Capture vocale d'une minute issue du flux WAY. Écoutable et prêt pour transfert cloud vers gdrive:/Z-CORE/Captures/.",
                    aiSummary = "Enregistrement vocal de 60s capturé en local sur l'instance Z-CORE. Prêt à être transféré via rclone vers Google Drive.",
                    waveformData = "20,45,80,65,30,90,70,85,95,60,40,75,85,90,65,50,80,95,70,40"
                ),
                AudioRecording(
                    title = "Archives Vocales 2023",
                    fileName = "enregistrement2023-02-06 07-11-38.wav",
                    durationSeconds = 185,
                    fileSizeMb = 31.2f,
                    localPath = "./storage/archives/enregistrement2023-02-06.wav",
                    cloudPath = "gdrive:/Archives/2023/enregistrement2023-02-06 07-11-38.wav",
                    isSynced = true,
                    syncStatus = "CLOUD_SYNCED",
                    sourceStream = "Drive Archive",
                    timestamp = System.currentTimeMillis() - 86400000000L,
                    transcription = "Archive audio historique de février 2023 déjà présente sur le Google Drive principal.",
                    aiSummary = "Fichier audio archivé de 3m05s. Sauvegarde confirmée sur le stockage cloud distant.",
                    waveformData = "15,30,40,50,45,60,70,65,80,75,60,50,40,30,25,35,45,55,60,50"
                )
            )
            for (rec in initialRecordings) {
                db.audioRecordingDao().insertRecording(rec)
            }
        }

        if (db.commandMacroDao().getCount() == 0) {
            val initialMacros = listOf(
                CommandMacro(
                    name = "Sync Ghost Vocal to Drive",
                    category = "RCLONE",
                    commandText = "rclone copy ./storage/enregistrement_ghost_vocal.wav gdrive:/Z-CORE/Captures/",
                    description = "Copie chiffrée de l'enregistrement Z-CORE vers Google Drive."
                ),
                CommandMacro(
                    name = "Sync Root Drive Folder",
                    category = "RCLONE",
                    commandText = "rclone copy ./storage/enregistrement_ghost_vocal.wav gdrive:/",
                    description = "Envoie le fichier directement à la racine de Google Drive."
                ),
                CommandMacro(
                    name = "Rclone Cloud Cleanup (>30 days)",
                    category = "CLEANUP",
                    commandText = "rclone delete gdrive:/Z-CORE/Captures/ --min-age 30d",
                    description = "Supprime les fichiers vieux de plus de 30 jours sur Google Drive."
                ),
                CommandMacro(
                    name = "Rclone Empty Trash",
                    category = "CLEANUP",
                    commandText = "rclone cleanup gdrive:",
                    description = "Vide la corbeille Google Drive pour libérer de l'espace cloud."
                ),
                CommandMacro(
                    name = "Local Cache Cleanup",
                    category = "CLEANUP",
                    commandText = "rm -rf ~/.cache/* && echo 'Cache vidé'",
                    description = "Supprime les fichiers temporaires locaux du terminal Termux."
                ),
                CommandMacro(
                    name = "Setup Termux API Tools",
                    category = "TERMUX",
                    commandText = "pkg update && pkg install termux-api",
                    description = "Installe les extensions Termux API pour gérer le matériel et l'affichage."
                ),
                CommandMacro(
                    name = "Acquire Wake Lock",
                    category = "TERMUX",
                    commandText = "termux-wake-lock",
                    description = "Empêche l'écran de se mettre en veille pendant les transferts."
                ),
                CommandMacro(
                    name = "Release Wake Lock",
                    category = "TERMUX",
                    commandText = "termux-wake-unlock",
                    description = "Permet à l'écran de se remettre en veille normalement."
                ),
                CommandMacro(
                    name = "ADB Wake Screen",
                    category = "ADB",
                    commandText = "adb shell input keyevent 26",
                    description = "Simule l'appui sur le bouton d'alimentation pour réveiller le téléphone."
                ),
                CommandMacro(
                    name = "ADB Swipe Unlock",
                    category = "ADB",
                    commandText = "adb shell input swipe 500 1500 500 500 200",
                    description = "Simule un glissement vers le haut pour afficher l'écran de déverrouillage."
                )
            )
            db.commandMacroDao().insertAll(initialMacros)
        }
    }

    suspend fun insertRecording(recording: AudioRecording): Long = withContext(Dispatchers.IO) {
        db.audioRecordingDao().insertRecording(recording)
    }

    suspend fun updateRecording(recording: AudioRecording) = withContext(Dispatchers.IO) {
        db.audioRecordingDao().updateRecording(recording)
    }

    suspend fun deleteRecording(id: Long) = withContext(Dispatchers.IO) {
        db.audioRecordingDao().deleteRecording(id)
    }

    suspend fun logSyncOperation(
        recordingId: Long?,
        command: String,
        status: String,
        progress: Int,
        bytes: String,
        errorMsg: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val log = SyncLog(
            recordingId = recordingId,
            commandExecuted = command,
            status = status,
            progressPercent = progress,
            bytesTransferred = bytes,
            errorMessage = errorMsg
        )
        db.syncLogDao().insertSyncLog(log)
    }

    suspend fun toggleFavoriteMacro(id: Long, currentFav: Boolean) = withContext(Dispatchers.IO) {
        db.commandMacroDao().toggleFavorite(id, !currentFav)
    }

    suspend fun saveMacro(macro: CommandMacro): Long = withContext(Dispatchers.IO) {
        db.commandMacroDao().insertMacro(macro)
    }

    suspend fun saveRcloneRemote(remote: RcloneRemote): Long = withContext(Dispatchers.IO) {
        db.rcloneRemoteDao().insertRemote(remote)
    }

    suspend fun deleteRcloneRemote(remote: RcloneRemote) = withContext(Dispatchers.IO) {
        db.rcloneRemoteDao().deleteRemote(remote)
    }

    suspend fun analyzeAudioWithGemini(
        recording: AudioRecording,
        userPrompt: String? = null,
        apiKey: String
    ): GeminiAudioAnalysisResult = withContext(Dispatchers.IO) {
        val service = geminiAudioService
        if (service != null) {
            val result = service.analyzeAudioFile(recording, userPrompt, apiKey)
            if (result.isSuccess && result.transcription.isNotBlank() && result.transcription != recording.transcription) {
                db.audioRecordingDao().updateRecording(recording.copy(
                    transcription = result.transcription,
                    aiSummary = result.summary
                ))
            } else if (result.isSuccess) {
                db.audioRecordingDao().updateRecording(recording.copy(aiSummary = result.summary))
            }
            result
        } else {
            GeminiAudioAnalysisResult(
                summary = "Analyse Locale Z-CORE: '${recording.title}' est une capture audio de ${recording.durationSeconds}s (${recording.fileSizeMb}MB). Format .wav prêt pour diffusion ou transfert cloud RClone.",
                transcription = recording.transcription ?: "",
                isSuccess = false
            )
        }
    }

    suspend fun generateAiSummary(recording: AudioRecording, apiKey: String): String = withContext(Dispatchers.IO) {
        val result = analyzeAudioWithGemini(recording, null, apiKey)
        result.summary
    }

    // --- Google Drive API Service Layer Operations ---

    suspend fun listDriveAudioFiles(
        folderId: String? = null,
        query: String? = null,
        pageSize: Int = 50,
        accessToken: String? = null
    ): Result<List<DriveAudioFile>> = withContext(Dispatchers.IO) {
        val service = googleDriveApiService ?: return@withContext Result.failure(Exception("GoogleDriveApiService indisponible"))
        service.listAudioFiles(folderId, query, pageSize, accessToken)
    }

    suspend fun uploadAudioToDrive(
        recording: AudioRecording,
        folderId: String? = null,
        accessToken: String? = null,
        onProgress: (suspend (Int) -> Unit)? = null
    ): Result<DriveUploadResult> = withContext(Dispatchers.IO) {
        val service = googleDriveApiService ?: return@withContext Result.failure(Exception("GoogleDriveApiService indisponible"))
        service.uploadAudioFile(recording, folderId, accessToken, onProgress)
    }

    suspend fun searchDriveAudioFiles(
        keyword: String,
        accessToken: String? = null
    ): Result<List<DriveAudioFile>> = withContext(Dispatchers.IO) {
        val service = googleDriveApiService ?: return@withContext Result.failure(Exception("GoogleDriveApiService indisponible"))
        service.searchAudioFiles(keyword, accessToken)
    }

    suspend fun deleteDriveAudioFile(
        fileId: String,
        accessToken: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val service = googleDriveApiService ?: return@withContext Result.failure(Exception("GoogleDriveApiService indisponible"))
        service.deleteAudioFile(fileId, accessToken)
    }
}
