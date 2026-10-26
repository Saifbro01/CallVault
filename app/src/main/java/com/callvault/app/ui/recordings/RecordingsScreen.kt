package com.callvault.app.ui.recordings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.callvault.app.data.database.RecordingEntity
import com.callvault.app.data.model.CallType
import com.callvault.app.utils.Format
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val filters = listOf("All", "Favorites", "Incoming", "Outgoing", "Manual")

@Composable
fun RecordingsScreen(
    recordings: List<RecordingEntity>,
    playingId: Long?,
    progress: Float,
    isRecording: Boolean,
    onToggleRecord: () -> Unit,
    onPlay: (RecordingEntity) -> Unit,
    onSeek: (Float) -> Unit,
    onFavorite: (RecordingEntity) -> Unit,
    onTrash: (RecordingEntity) -> Unit,
    onShare: (RecordingEntity) -> Unit,
    onRename: (RecordingEntity, String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    var renaming by remember { mutableStateOf<RecordingEntity?>(null) }

    val filtered = remember(recordings, query, filter) {
        val fFull = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val fShort = SimpleDateFormat("MMM d", Locale.getDefault())
        val fIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val q = query.trim().lowercase(Locale.getDefault())
        recordings.filter { rec ->
            val typeOk = when (filter) {
                "Favorites" -> rec.isFavorite
                "Incoming" -> rec.callType == CallType.INCOMING
                "Outgoing" -> rec.callType == CallType.OUTGOING
                "Manual" -> rec.callType == CallType.MANUAL
                else -> true
            }
            if (!typeOk) return@filter false
            if (q.isEmpty()) return@filter true
            val d = Date(rec.timestamp)
            rec.fileName.lowercase().contains(q) ||
                (rec.contactName?.lowercase()?.contains(q) == true) ||
                (rec.phoneNumber?.contains(q) == true) ||
                fFull.format(d).lowercase().contains(q) ||
                fShort.format(d).lowercase().contains(q) ||
                fIso.format(d).contains(q)
        }
    }

    renaming?.let { target ->
        var text by remember(target.id) { mutableStateOf(target.contactName ?: "") }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true, placeholder = { Text("Name") })
            },
            confirmButton = { TextButton(onClick = { onRename(target, text); renaming = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel") } }
        )
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by name, number or date (e.g. Oct 24)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { f ->
                    FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f) })
                }
            }
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (recordings.isEmpty()) "No recordings yet.\nTap Record to start." else "No matches.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { rec ->
                        RecordingRow(
                            rec, rec.id == playingId, progress,
                            onPlay, onSeek, onFavorite, onTrash, onShare, { renaming = it }
                        )
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onToggleRecord,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            icon = {
                Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null)
            },
            text = { Text(if (isRecording) "Stop" else "Record") }
        )
    }
}

@Composable
private fun RecordingRow(
    rec: RecordingEntity,
    playing: Boolean,
    progress: Float,
    onPlay: (RecordingEntity) -> Unit,
    onSeek: (Float) -> Unit,
    onFavorite: (RecordingEntity) -> Unit,
    onTrash: (RecordingEntity) -> Unit,
    onShare: (RecordingEntity) -> Unit,
    onRename: (RecordingEntity) -> Unit
) {
    val title = rec.contactName ?: rec.phoneNumber ?: when (rec.callType) {
        CallType.INCOMING -> "Incoming call"
        CallType.OUTGOING -> "Outgoing call"
        CallType.MANUAL -> "Voice recording"
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onPlay(rec) }) {
                    Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play")
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${Format.date(rec.timestamp)}  •  ${Format.duration(rec.durationMs)}  •  ${Format.size(rec.fileSizeBytes)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (playing) {
                Slider(value = progress, onValueChange = onSeek, modifier = Modifier.padding(horizontal = 8.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { onRename(rec) }) { Icon(Icons.Default.Edit, contentDescription = "Rename") }
                IconButton(onClick = { onShare(rec) }) { Icon(Icons.Default.Share, contentDescription = "Share") }
                IconButton(onClick = { onFavorite(rec) }) {
                    Icon(
                        if (rec.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite"
                    )
                }
                IconButton(onClick = { onTrash(rec) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Move to trash")
                }
            }
        }
    }
}
