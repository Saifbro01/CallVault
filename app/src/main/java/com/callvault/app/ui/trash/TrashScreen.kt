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
import com.callvault.app.utils.tr

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
            title = { Text(tr("Empty trash?", "ট্র্যাশ খালি করবেন?")) },
            text = { Text(tr("All recordings in trash will be deleted permanently.", "ট্র্যাশের সব রেকর্ডিং চিরতরে মুছে যাবে।")) },
            confirmButton = { TextButton(onClick = { onEmptyTrash(); confirmEmpty = false }) { Text(tr("Delete all", "সব মুছুন")) } },
            dismissButton = { TextButton(onClick = { confirmEmpty = false }) { Text(tr("Cancel", "বাতিল")) } }
        )
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            tr("Items are deleted automatically after $retentionDays days.", "$retentionDays দিন পর আইটেম নিজে থেকে মুছে যাবে।"),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        if (trashedRecordings.isNotEmpty()) {
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onRestoreAll) { Text(tr("Restore all", "সব রিস্টোর")) }
                OutlinedButton(onClick = { confirmEmpty = true }) { Text(tr("Empty trash", "ট্র্যাশ খালি করুন")) }
            }
        }
        if (trashedRecordings.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(tr("Trash is empty", "ট্র্যাশ খালি")) }
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
                                Text(rec.contactName ?: rec.phoneNumber ?: tr("Recording", "রেকর্ডিং"), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${Format.date(rec.timestamp)}  •  ${tr("$left days left", "$left দিন বাকি")}",
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
