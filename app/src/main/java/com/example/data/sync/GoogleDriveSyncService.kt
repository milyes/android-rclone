package com.example.data.sync

import android.content.Context
import com.example.data.model.AudioRecording
import com.example.data.service.GoogleDriveApiService
import com.example.data.service.GoogleDriveApiServiceImpl
import com.example.data.service.DriveAudioFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleDriveSyncService(
    private val context: Context,
    private val driveApiService: GoogleDriveApiService = GoogleDriveApiServiceImpl(context)
) {

    companion object {
        const val DRIVE_FOLDER_NAME = GoogleDriveApiServiceImpl.DEFAULT_FOLDER_NAME
        private const val TAG = "GoogleDriveSyncService"
    }

    /**
     * Uploads an audio recording file to the dedicated Google Drive folder via GoogleDriveApiService.
     * Progress updates are published via onProgress callback.
     */
    suspend fun uploadRecordingToDrive(
        recording: AudioRecording,
        accessToken: String? = null,
        onProgress: (suspend (Int) -> Unit)? = null
    ): Result<SyncResult> = withContext(Dispatchers.IO) {
        val result = driveApiService.uploadAudioFile(
            recording = recording,
            folderId = null,
            accessToken = accessToken,
            onProgress = onProgress
        )

        result.map { uploadRes ->
            SyncResult(
                driveFileId = uploadRes.fileId,
                cloudPath = uploadRes.cloudPath,
                bytesTransferred = "${recording.fileSizeMb} MB"
            )
        }
    }

    suspend fun listRemoteDriveAudios(
        query: String? = null,
        accessToken: String? = null
    ): Result<List<DriveAudioFile>> = withContext(Dispatchers.IO) {
        driveApiService.listAudioFiles(query = query, accessToken = accessToken)
    }

    data class SyncResult(
        val driveFileId: String,
        val cloudPath: String,
        val bytesTransferred: String
    )
}

