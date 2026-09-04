package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AudioRecording
import com.example.data.model.CommandMacro
import com.example.data.model.SyncLog
import com.example.data.model.RcloneRemote

@Database(
    entities = [AudioRecording::class, SyncLog::class, CommandMacro::class, RcloneRemote::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioRecordingDao(): AudioRecordingDao
    abstract fun syncLogDao(): SyncLogDao
    abstract fun commandMacroDao(): CommandMacroDao
    abstract fun rcloneRemoteDao(): RcloneRemoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "audio_sync_hub.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
