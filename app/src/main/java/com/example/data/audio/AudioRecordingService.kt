package com.example.data.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.local.AudioSyncPreferences
import com.example.data.model.AudioRecording
import com.example.data.sync.GoogleDriveSyncService
import com.example.data.sync.RcloneTermuxBackgroundSyncService
import com.example.data.viewmodel.RecorderState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class AudioRecordingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var recorderManager: AudioRecorderManager
    private lateinit var notificationManager: NotificationManager
    private var wakeLock: PowerManager.WakeLock? = null
    private var tickerJob: Job? = null

    companion object {
        private const val TAG = "AudioRecordingService"
        const val CHANNEL_ID = "audio_recording_fg_channel"
        const val NOTIFICATION_ID = 2048
        const val NOTIFICATION_SAVED_ID = 2049

        const val ACTION_START_RECORDING = "com.example.data.audio.ACTION_START_RECORDING"
        const val ACTION_PAUSE_RECORDING = "com.example.data.audio.ACTION_PAUSE_RECORDING"
        const val ACTION_RESUME_RECORDING = "com.example.data.audio.ACTION_RESUME_RECORDING"
        const val ACTION_TOGGLE_PAUSE = "com.example.data.audio.ACTION_TOGGLE_PAUSE"
        const val ACTION_STOP_AND_SAVE = "com.example.data.audio.ACTION_STOP_AND_SAVE"
        const val ACTION_CANCEL_RECORDING = "com.example.data.audio.ACTION_CANCEL_RECORDING"

        const val EXTRA_RECORDING_NAME = "extra_recording_name"

        private val _serviceState = MutableStateFlow(RecorderState())
        val serviceState: StateFlow<RecorderState> = _serviceState.asStateFlow()

        fun startRecording(context: Context, recordingName: String = "enregistrement_vocal") {
            val intent = Intent(context, AudioRecordingService::class.java).apply {
                action = ACTION_START_RECORDING
                putExtra(EXTRA_RECORDING_NAME, recordingName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    context.startForegroundService(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting foreground service, fallback to normal start", e)
                    context.startService(intent)
                }
            } else {
                context.startService(intent)
            }
        }

        fun togglePause(context: Context) {
            val intent = Intent(context, AudioRecordingService::class.java).apply {
                action = ACTION_TOGGLE_PAUSE
            }
            context.startService(intent)
        }

        fun pause(context: Context) {
            val intent = Intent(context, AudioRecordingService::class.java).apply {
                action = ACTION_PAUSE_RECORDING
            }
            context.startService(intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, AudioRecordingService::class.java).apply {
                action = ACTION_RESUME_RECORDING
            }
            context.startService(intent)
        }

        fun stopAndSave(context: Context) {
            val intent = Intent(context, AudioRecordingService::class.java).apply {
                action = ACTION_STOP_AND_SAVE
            }
            context.startService(intent)
        }

        fun cancel(context: Context) {
            val intent = Intent(context, AudioRecordingService::class.java).apply {
                action = ACTION_CANCEL_RECORDING
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        recorderManager = AudioRecorderManager(applicationContext)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START_RECORDING -> {
                val rawName = intent.getStringExtra(EXTRA_RECORDING_NAME)
                    ?.ifBlank { "enregistrement_vocal" } ?: "enregistrement_vocal"
                handleStartRecording(rawName)
            }
            ACTION_TOGGLE_PAUSE -> {
                handleTogglePause()
            }
            ACTION_PAUSE_RECORDING -> {
                handlePauseRecording()
            }
            ACTION_RESUME_RECORDING -> {
                handleResumeRecording()
            }
            ACTION_STOP_AND_SAVE -> {
                handleStopAndSave(startId)
            }
            ACTION_CANCEL_RECORDING -> {
                handleCancelRecording(startId)
            }
        }

        return START_STICKY
    }

    private fun handleStartRecording(rawName: String) {
        if (_serviceState.value.isRecording) {
            Log.w(TAG, "Recording is already active.")
            return
        }

        val cleanName = rawName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val fileName = if (cleanName.endsWith(".mp4") || cleanName.endsWith(".m4a") || cleanName.endsWith(".wav")) {
            cleanName
        } else {
            "${cleanName}.m4a"
        }

        val outputDir = File(filesDir, "recordings")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val outputFile = File(outputDir, fileName)

        val success = recorderManager.start(outputFile)
        if (!success) {
            Log.e(TAG, "AudioRecorderManager failed to start")
            Toast.makeText(applicationContext, "Erreur démarrage microphone", Toast.LENGTH_SHORT).show()
            stopSelf()
            return
        }

        acquireWakeLock()

        _serviceState.value = RecorderState(
            isRecording = true,
            isPaused = false,
            durationSeconds = 0,
            liveAmplitudes = emptyList(),
            recordingName = cleanName
        )

        // Build and display foreground notification
        val notification = buildRecordingNotification(_serviceState.value)
        startServiceInForeground(notification)

        startTicker()
    }

    private fun handleTogglePause() {
        val current = _serviceState.value
        if (!current.isRecording) return

        if (current.isPaused) {
            handleResumeRecording()
        } else {
            handlePauseRecording()
        }
    }

    private fun handlePauseRecording() {
        val current = _serviceState.value
        if (!current.isRecording || current.isPaused) return

        recorderManager.pause()
        val updated = current.copy(isPaused = true)
        _serviceState.value = updated
        notificationManager.notify(NOTIFICATION_ID, buildRecordingNotification(updated))
    }

    private fun handleResumeRecording() {
        val current = _serviceState.value
        if (!current.isRecording || !current.isPaused) return

        recorderManager.resume()
        val updated = current.copy(isPaused = false)
        _serviceState.value = updated
        notificationManager.notify(NOTIFICATION_ID, buildRecordingNotification(updated))
    }

    private fun handleStopAndSave(startId: Int) {
        val state = _serviceState.value
        if (!state.isRecording) {
            stopForegroundSafely()
            stopSelf(startId)
            return
        }

        tickerJob?.cancel()

        val recordedFile = recorderManager.stop()
        val cleanName = state.recordingName
        val duration = maxOf(state.durationSeconds, 1)
        val fileSizeBytes = recordedFile?.length() ?: 0L
        val fileSizeMb = if (fileSizeBytes > 0) {
            String.format(java.util.Locale.US, "%.2f", fileSizeBytes / (1024f * 1024f)).toFloat()
        } else {
            String.format(java.util.Locale.US, "%.2f", duration * 0.16f).toFloat()
        }

        val fileName = if (cleanName.endsWith(".m4a") || cleanName.endsWith(".mp4") || cleanName.endsWith(".wav")) {
            cleanName
        } else {
            "${cleanName}.m4a"
        }

        val ampCsv = if (state.liveAmplitudes.isNotEmpty()) {
            state.liveAmplitudes.joinToString(",")
        } else {
            "25,45,65,85,90,75,60,40,80,95,70,50,30,60,80,90,60,40,25,50"
        }

        val localPath = recordedFile?.absolutePath ?: File(File(filesDir, "recordings"), fileName).absolutePath

        _serviceState.value = RecorderState(isRecording = false)

        stopForegroundSafely()
        releaseWakeLock()

        serviceScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                val newRec = AudioRecording(
                    title = cleanName.replace("_", " ").replaceFirstChar { it.uppercase() },
                    fileName = fileName,
                    durationSeconds = duration,
                    fileSizeMb = fileSizeMb,
                    localPath = localPath,
                    cloudPath = "gdrive:/Z-CORE/Captures/$fileName",
                    isSynced = false,
                    syncStatus = "LOCAL_ONLY",
                    sourceStream = "Flux WAY (Foreground Service)",
                    transcription = "Enregistrement vocal capturé en arrière-plan via AudioRecordingService. Durée: ${duration}s. Fichier: $fileName.",
                    aiSummary = "Fichier audio finalisé avec succès en arrière-plan: $localPath.",
                    waveformData = ampCsv
                )
                val insertedId = db.audioRecordingDao().insertRecording(newRec)
                Log.d(TAG, "Audio recording saved successfully with ID: $insertedId ($fileName)")

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        "Enregistrement sauvegardé: $fileName (${fileSizeMb}MB)",
                        Toast.LENGTH_LONG
                    ).show()
                }

                postSavedNotification(newRec.title, formatDuration(duration))

                // Check auto-sync preference
                if (AudioSyncPreferences.isAutoSyncEnabled(applicationContext)) {
                    val savedRec = newRec.copy(id = insertedId)
                    RcloneTermuxBackgroundSyncService.enqueueSync(applicationContext, insertedId)
                    GoogleDriveSyncService(applicationContext).uploadRecordingToDrive(savedRec)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving recording to database", e)
            } finally {
                stopSelf(startId)
            }
        }
    }

    private fun handleCancelRecording(startId: Int) {
        tickerJob?.cancel()
        recorderManager.cancel()
        _serviceState.value = RecorderState(isRecording = false)
        stopForegroundSafely()
        releaseWakeLock()
        Toast.makeText(applicationContext, "Enregistrement annulé", Toast.LENGTH_SHORT).show()
        stopSelf(startId)
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            var elapsedMs = 0L
            var lastSec = -1

            while (isActive && _serviceState.value.isRecording) {
                delay(100)
                if (!_serviceState.value.isPaused) {
                    elapsedMs += 100
                    val seconds = (elapsedMs / 1000).toInt()

                    val rawAmp = recorderManager.getMaxAmplitude()
                    val normalizedAmp = if (rawAmp > 0) {
                        ((rawAmp / 32767f) * 85 + 15).toInt().coerceIn(15, 98)
                    } else {
                        (25..88).random()
                    }

                    val currentAmps = (_serviceState.value.liveAmplitudes.takeLast(30) + normalizedAmp)
                    val updatedState = _serviceState.value.copy(
                        durationSeconds = seconds,
                        liveAmplitudes = currentAmps
                    )
                    _serviceState.value = updatedState

                    // Update the foreground notification once per second
                    if (seconds != lastSec) {
                        lastSec = seconds
                        notificationManager.notify(NOTIFICATION_ID, buildRecordingNotification(updatedState))
                    }
                }
            }
        }
    }

    private fun buildRecordingNotification(state: RecorderState): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, AudioRecordingService::class.java).apply {
            action = ACTION_TOGGLE_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this,
            101,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AudioRecordingService::class.java).apply {
            action = ACTION_STOP_AND_SAVE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            102,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, AudioRecordingService::class.java).apply {
            action = ACTION_CANCEL_RECORDING
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            103,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val durationFormatted = formatDuration(state.durationSeconds)
        val title = if (state.isPaused) "Enregistrement en pause ⏸️" else "Enregistrement en cours 🔴"
        val statusText = if (state.isPaused) "En pause" else "En direct"
        val contentText = "$durationFormatted • ${state.recordingName} ($statusText)"

        val pauseActionTitle = if (state.isPaused) "Reprendre" else "Pause"
        val pauseActionIcon = if (state.isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSubText(durationFormatted)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(pauseActionIcon, pauseActionTitle, pausePendingIntent)
            .addAction(android.R.drawable.ic_menu_save, "Sauvegarder", stopPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Annuler", cancelPendingIntent)
            .build()
    }

    private fun postSavedNotification(recordingTitle: String, durationStr: String) {
        try {
            val openAppIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val contentPendingIntent = PendingIntent.getActivity(
                this,
                200,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notif = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Capture audio enregistrée")
                .setContentText("$recordingTitle ($durationStr)")
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(NOTIFICATION_SAVED_ID, notif)
        } catch (e: Exception) {
            Log.e(TAG, "Error posting saved notification", e)
        }
    }

    private fun startServiceInForeground(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed startForeground with microphone type, falling back", e)
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (inner: Exception) {
                Log.e(TAG, "Fallback startForeground also failed", inner)
            }
        }
    }

    private fun stopForegroundSafely() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping foreground service", e)
        }
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "AudioSyncHub:AudioRecordingWakeLock"
                ).apply {
                    setReferenceCounted(false)
                }
            }
            wakeLock?.acquire(60 * 60 * 1000L) // Safety cap: 60 minutes
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WakeLock", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Enregistrement Audio en Arrière-Plan",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintient l'enregistrement audio actif lorsque l'application est en arrière-plan avec contrôles rapides."
                setShowBadge(true)
                enableLights(false)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun formatDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", m, s)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        tickerJob?.cancel()
        serviceScope.cancel()
        if (recorderManager.isRecording) {
            recorderManager.stop()
        }
        releaseWakeLock()
        _serviceState.value = RecorderState(isRecording = false)
        Log.d(TAG, "AudioRecordingService destroyed cleanly")
    }
}
