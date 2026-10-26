package com.callvault.app.ui.trash

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.callvault.app.data.database.RecordingEntity
import com.callvault.app.utils.Format

@Composable
fun TrashScreen(
    trashedRecordings: List<RecordingEntity>,
    retentionDays: Int,
    onRestoreRecording: (Long) -> Unit,
    onRestoreAll: () -> Unit,
    onPermanentDelete: (RecordingEntity) -> Unit,
    onEmptyTrash: () -> Unit
) {
    var confirmEmpty by remember { mutableStateOf(false) }
    val dayMs = 24L * 60 * 60 * 1000

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            title = { Text("Empty trash?") },
            text = { Text("All recordings in trash will be deleted permanently.") },
            confirmButton = { TextButton(onClick = { onEmptyTrash(); confirmEmpty = false }) { Text("Delete all") } },
            dismissButton = { TextButton(onClick = { confirmEmpty = false }) { Text("Cancel") } }
        )
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Items are deleted automatically after $retentionDays days.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        if (trashedRecordings.isNotEmpty()) {
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onRestoreAll) { Text("Restore all") }
                OutlinedButton(onClick = { confirmEmpty = true }) { Text("Empty trash") }
            }
        }
        if (trashedRecordings.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Trash is empty") }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trashedRecordings, key = { it.id }) { rec ->
                    val now = System.currentTimeMillis()
                    val left = (((rec.deletedAt ?: now) + retentionDays * dayMs - now + dayMs - 1) / dayMs)
                        .coerceAtLeast(0)
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(rec.contactName ?: rec.phoneNumber ?: "Recording", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${Format.date(rec.timestamp)}  •  $left days left",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { onRestoreRecording(rec.id) }) {
                                Icon(Icons.Default.Restore, contentDescription = "Restore")
                            }
                            IconButton(onClick = { onPermanentDelete(rec) }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Delete forever")
                            }
                        }
                    }
                }
            }
        }
    }
}
