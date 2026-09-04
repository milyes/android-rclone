package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.RcloneRemote
import kotlinx.coroutines.flow.Flow

@Dao
interface RcloneRemoteDao {
    @Query("SELECT * FROM rclone_remotes")
    fun getAllRemotes(): Flow<List<RcloneRemote>>

    @Insert
    suspend fun insertRemote(remote: RcloneRemote): Long

    @Update
    suspend fun updateRemote(remote: RcloneRemote)

    @Delete
    suspend fun deleteRemote(remote: RcloneRemote)
}
