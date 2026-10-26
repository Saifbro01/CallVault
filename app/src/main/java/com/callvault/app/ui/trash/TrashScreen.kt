package com.callvault.app.ui.trash

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.callvault.app.data.database.RecordingEntity

@Composable
fun TrashScreen(
    trashedRecordings: List<RecordingEntity>,
    retentionDays: Int = 30,
    onRestoreRecording: (Long) -> Unit,
    onRestoreAll: () -> Unit,
    onPermanentDelete: (RecordingEntity) -> Unit,
    onEmptyTrash: () -> Unit,
    onBack: () -> Unit
) {
    // 30-day safe recovery list with countdowns and restore/purge actions
}