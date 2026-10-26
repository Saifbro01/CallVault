package com.callvault.app.ui.recordings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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

@Composable
fun RecordingsScreen(
    recordings: List<RecordingEntity>,
    playingId: Long?,
    isRecording: Boolean,
    onToggleRecord: () -> Unit,
    onPlay: (RecordingEntity) -> Unit,
    onFavorite: (RecordingEntity) -> Unit,
    onTrash: (RecordingEntity) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var favOnly by remember { mutableStateOf(false) }

    val filtered = remember(recordings, query, favOnly) {
        val fFull = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val fShort = SimpleDateFormat("MMM d", Locale.getDefault())
        val fIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val q = query.trim().lowercase(Locale.getDefault())
        recordings.filter { rec ->
            if (favOnly && !rec.isFavorite) return@filter false
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

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by number or date (e.g. Oct 24)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = !favOnly, onClick = { favOnly = false }, label = { Text("All") })
                FilterChip(selected = favOnly, onClick = { favOnly = true }, label = { Text("Favorites") })
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
                        RecordingRow(rec, rec.id == playingId, onPlay, onFavorite, onTrash)
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
    onPlay: (RecordingEntity) -> Unit,
    onFavorite: (RecordingEntity) -> Unit,
    onTrash: (RecordingEntity) -> Unit
) {
    val title = rec.contactName ?: rec.phoneNumber ?: when (rec.callType) {
        CallType.INCOMING -> "Incoming call"
        CallType.OUTGOING -> "Outgoing call"
        CallType.MANUAL -> "Voice recording"
    }
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
