package com.example.data.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.data.model.AudioRecording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

class SafDriveIntegrationService(private val context: Context) {
    companion object {
        private const val TAG = "SafDriveIntegration"
    }

    suspend fun uploadToPickedFolder(
        recording: AudioRecording,
        folderUri: Uri,
        onProgress: suspend (Int) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(recording.localPath)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("Local file not found"))
            }

            Log.d(TAG, "Uploading \${recording.fileName} to SAF Uri: \$folderUri")
            onProgress(10)

            val rootFolder = DocumentFile.fromTreeUri(context, folderUri)
                ?: return@withContext Result.failure(Exception("Cannot access selected folder"))

            val mimeType = when {
                recording.fileName.endsWith(".wav", ignoreCase = true) -> "audio/wav"
                recording.fileName.endsWith(".mp3", ignoreCase = true) -> "audio/mp3"
                else -> "audio/m4a"
            }

            // Create new file in the picked folder
            val newFile = rootFolder.createFile(mimeType, recording.fileName)
                ?: return@withContext Result.failure(Exception("Failed to create file in folder"))

            onProgress(30)

            val outputStream = context.contentResolver.openOutputStream(newFile.uri)
                ?: return@withContext Result.failure(Exception("Failed to open output stream"))

            FileInputStream(file).use { input ->
                outputStream.use { output ->
                    val buffer = ByteArray(8192)
                    var bytesCopied = 0L
                    val totalBytes = file.length()
                    var length: Int
                    
                    while (input.read(buffer).also { length = it } > 0) {
                        output.write(buffer, 0, length)
                        bytesCopied += length
                        
                        if (totalBytes > 0) {
                            val percent = (bytesCopied.toFloat() / totalBytes.toFloat() * 60).toInt()
                            onProgress(30 + percent) // 30 to 90
                        }
                    }
                }
            }

            onProgress(100)
            Result.success(newFile.uri.toString())
        } catch (e: Exception) {
            Log.e(TAG, "SAF Upload Failed", e)
            Result.failure(e)
        }
    }
}
