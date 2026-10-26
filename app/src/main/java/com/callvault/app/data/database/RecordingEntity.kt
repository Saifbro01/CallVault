package com.callvault.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.callvault.app.data.model.CallType

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val contactName: String? = null,
    val phoneNumber: String? = null,
    val callType: CallType = CallType.MANUAL,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val isFavorite: Boolean = false,
    val audioFormat: String = "m4a",
    val waveformData: String = ""
)