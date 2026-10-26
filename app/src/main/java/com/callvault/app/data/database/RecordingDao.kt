package com.callvault.app.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings WHERE deletedAt IS NULL ORDER BY timestamp DESC")
    fun observeActive(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE deletedAt IS NOT NULL")
    suspend fun getTrashed(): List<RecordingEntity>

    @Query("SELECT * FROM recordings WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun getExpired(cutoff: Long): List<RecordingEntity>

    @Query("SELECT fileName FROM recordings")
    suspend fun getAllNames(): List<String>

    @Query("SELECT * FROM recordings WHERE deletedAt IS NULL")
    suspend fun getAll(): List<RecordingEntity>

    @Insert
    suspend fun insert(recording: RecordingEntity): Long

    @Query("UPDATE recordings SET isFavorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: Long, fav: Boolean)

    @Query("UPDATE recordings SET contactName = :name WHERE id = :id")
    suspend fun setContactName(id: Long, name: String?)

    @Query("UPDATE recordings SET deletedAt = :ts WHERE id = :id")
    suspend fun setDeletedAt(id: Long, ts: Long?)

    @Query("UPDATE recordings SET deletedAt = NULL WHERE deletedAt IS NOT NULL")
    suspend fun restoreAll()

    @Delete
    suspend fun delete(recording: RecordingEntity)
}
