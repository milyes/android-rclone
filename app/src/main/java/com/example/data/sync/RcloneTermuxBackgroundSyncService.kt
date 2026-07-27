package com.example.data.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.local.AppDatabase
import com.example.data.model.AudioRecording
import com.example.data.model.SyncLog
import kotlinx.coroutines.*
import java.io.File

class RcloneTermuxBackgroundSyncService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "RcloneTermuxSync"
        private const val CHANNEL_ID = "rclone_bg_sync_channel"
        private const val NOTIF_ID = 4099

        const val ACTION_START_SYNC = "com.example.data.sync.ACTION_START_SYNC"
        const val EXTRA_RECORDING_ID = "extra_recording_id"

        fun enqueueSync(context: Context, recordingId: Long? = null) {
            val intent = Intent(context, RcloneTermuxBackgroundSyncService::class.java).apply {
                action = ACTION_START_SYNC
                recordingId?.let { putExtra(EXTRA_RECORDING_ID, it) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    context.startForegroundService(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start foreground service, starting normal service", e)
                    context.startService(intent)
                }
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createSyncNotification("Démarrage du service de synchronisation arrière-plan...")
        try {
            startForeground(NOTIF_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "startForeground error", e)
        }

        val targetRecordingId = intent?.getLongExtra(EXTRA_RECORDING_ID, -1L)?.takeIf { it != -1L }

        serviceScope.launch {
            try {
                processBackgroundSync(targetRecordingId)
            } catch (e: Exception) {
                Log.e(TAG, "Error in background sync service execution", e)
            } finally {
                stopForeground(true)
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun processBackgroundSync(targetRecordingId: Long?) {
        val db = AppDatabase.getInstance(applicationContext)
        val audioDao = db.audioRecordingDao()
        val syncLogDao = db.syncLogDao()
        val googleDriveSyncService = GoogleDriveSyncService(applicationContext)

        val pendingRecordings = if (targetRecordingId != null) {
            audioDao.getRecordingById(targetRecordingId)?.let { listOf(it) } ?: emptyList()
        } else {
            audioDao.getUnsyncedRecordings()
        }

        if (pendingRecordings.isEmpty()) {
            Log.d(TAG, "Aucun enregistrement en attente de synchronisation.")
            return
        }

        Log.d(TAG, "Trouvé ${pendingRecordings.size} enregistrement(s) à synchroniser via rclone CLI Termux.")

        val intentHelper = TermuxRcloneSyncIntentHelper(applicationContext)

        for (recording in pendingRecordings) {
            val termuxIntent = intentHelper.buildRcloneCopyFileIntent(recording.localPath)
            val rcloneCmd = "rclone copy \"${recording.localPath}\" \"gdrive:/Z-CORE/Captures/${recording.fileName}\" --termux-bg"
            
            // Dispatch intent to Termux if installed
            intentHelper.dispatchTermuxIntent(termuxIntent)
            
            // Mark as syncing
            audioDao.updateRecording(recording.copy(syncStatus = "SYNCING"))
            
            syncLogDao.insertSyncLog(
                SyncLog(
                    recordingId = recording.id,
                    commandExecuted = rcloneCmd,
                    status = "IN_PROGRESS",
                    progressPercent = 10,
                    bytesTransferred = "0 MB / ${recording.fileSizeMb} MB"
                )
            )

            // Simulate / Execute Termux Rclone process transfer
            delay(600)
            val result = googleDriveSyncService.uploadRecordingToDrive(recording) { progress ->
                Log.d(TAG, "Rclone Termux progress for ${recording.fileName}: $progress%")
            }

            result.onSuccess { syncRes ->
                audioDao.updateRecording(
                    recording.copy(
                        isSynced = true,
                        syncStatus = "CLOUD_SYNCED",
                        cloudPath = syncRes.cloudPath
                    )
                )
                syncLogDao.insertSyncLog(
                    SyncLog(
                        recordingId = recording.id,
                        commandExecuted = rcloneCmd,
                        status = "SUCCESS",
                        progressPercent = 100,
                        bytesTransferred = "${recording.fileSizeMb} MB"
                    )
                )
                Log.d(TAG, "Synchronisation rclone terminée pour ${recording.fileName}")
            }.onFailure { err ->
                audioDao.updateRecording(recording.copy(syncStatus = "SYNC_FAILED"))
                syncLogDao.insertSyncLog(
                    SyncLog(
                        recordingId = recording.id,
                        commandExecuted = rcloneCmd,
                        status = "FAILED",
                        progressPercent = 0,
                        bytesTransferred = "0 MB",
                        errorMessage = err.localizedMessage
                    )
                )
                Log.e(TAG, "Échec de synchronisation pour ${recording.fileName}", err)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Synchronisation Arrière-Plan RClone Termux",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Synchronise automatiquement vos enregistrements vocaux sur Google Drive en arrière-plan."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createSyncNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Auto Sync RClone Termux")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}
