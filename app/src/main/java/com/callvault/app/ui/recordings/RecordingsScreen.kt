package com.callvault.app.ui.recordings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import com.callvault.app.utils.tr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class FilterMode(val en: String, val bn: String) {
    ALL("All", "সব"), FAV("Favorites", "প্রিয়"), IN("Incoming", "ইনকামিং"),
    OUT("Outgoing", "আউটগোয়িং"), MANUAL("Manual", "ম্যানুয়াল")
}

private enum class SortMode(val en: String, val bn: String) {
    NEWEST("Newest", "নতুন আগে"), OLDEST("Oldest", "পুরনো আগে"),
    LONGEST("Longest", "লম্বা আগে"), LARGEST("Largest", "বড় আগে")
}

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
    onTrashMany: (List<RecordingEntity>) -> Unit,
    onShare: (RecordingEntity) -> Unit,
    onRename: (RecordingEntity, String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(FilterMode.ALL) }
    var sort by remember { mutableStateOf(SortMode.NEWEST) }
    var renaming by remember { mutableStateOf<RecordingEntity?>(null) }
    var selected by remember { mutableStateOf(setOf<Long>()) }

    val filtered = remember(recordings, query, filter, sort) {
        val fFull = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val fShort = SimpleDateFormat("MMM d", Locale.getDefault())
        val fIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val q = query.trim().lowercase(Locale.getDefault())
        val list = recordings.filter { rec ->
            val typeOk = when (filter) {
                FilterMode.FAV -> rec.isFavorite
                FilterMode.IN -> rec.callType == CallType.INCOMING
                FilterMode.OUT -> rec.callType == CallType.OUTGOING
                FilterMode.MANUAL -> rec.callType == CallType.MANUAL
                FilterMode.ALL -> true
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
        when (sort) {
            SortMode.NEWEST -> list.sortedByDescending { it.timestamp }
            SortMode.OLDEST -> list.sortedBy { it.timestamp }
            SortMode.LONGEST -> list.sortedByDescending { it.durationMs }
            SortMode.LARGEST -> list.sortedByDescending { it.fileSizeBytes }
        }
    }

    renaming?.let { target ->
        var text by remember(target.id) { mutableStateOf(target.contactName ?: "") }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(tr("Rename", "নাম পরিবর্তন")) },
            text = {
                OutlinedTextField(
                    value = text, onValueChange = { text = it }, singleLine = true,
                    placeholder = { Text(tr("Name", "নাম")) }
                )
            },
            confirmButton = {
                TextButton(onClick = { onRename(target, text); renaming = null }) { Text(tr("Save", "সেভ")) }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text(tr("Cancel", "বাতিল")) } }
        )
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            if (selected.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tr("${selected.size} selected", "${selected.size} টি সিলেক্ট করা"),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = {
                        onTrashMany(recordings.filter { it.id in selected })
                        selected = emptySet()
                    }) { Text(tr("Move to trash", "ট্র্যাশে পাঠান")) }
                    TextButton(onClick = { selected = emptySet() }) { Text(tr("Cancel", "বাতিল")) }
                }
            } else {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(tr("Search by name, number or date (e.g. Oct 24)", "নাম, নাম্বার বা তারিখ দিয়ে খুঁজুন")) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterMode.values().forEach { f ->
                    FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(tr(f.en, f.bn)) })
                }
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr("Sort:", "সাজান:"), style = MaterialTheme.typography.labelMedium)
                SortMode.values().forEach { s ->
                    FilterChip(selected = sort == s, onClick = { sort = s }, label = { Text(tr(s.en, s.bn)) })
                }
            }
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (recordings.isEmpty()) tr("No recordings yet.\nTap Record to start.", "এখনো কোনো রেকর্ডিং নেই।\nশুরু করতে Record চাপুন।")
                        else tr("No matches.", "কিছু পাওয়া যায়নি।"),
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
                            rec = rec,
                            playing = rec.id == playingId,
                            progress = progress,
                            selecting = selected.isNotEmpty(),
                            isSelected = rec.id in selected,
                            onToggleSelect = {
                                selected = if (rec.id in selected) selected - rec.id else selected + rec.id
                            },
                            onPlay = onPlay, onSeek = onSeek, onFavorite = onFavorite,
                            onTrash = onTrash, onShare = onShare, onRename = { renaming = it }
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
            text = { Text(if (isRecording) tr("Stop", "থামান") else tr("Record", "রেকর্ড")) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecordingRow(
    rec: RecordingEntity,
    playing: Boolean,
    progress: Float,
    selecting: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onPlay: (RecordingEntity) -> Unit,
    onSeek: (Float) -> Unit,
    onFavorite: (RecordingEntity) -> Unit,
    onTrash: (RecordingEntity) -> Unit,
    onShare: (RecordingEntity) -> Unit,
    onRename: (RecordingEntity) -> Unit
) {
    val title = rec.contactName ?: rec.phoneNumber ?: when (rec.callType) {
        CallType.INCOMING -> tr("Incoming call", "ইনকামিং কল")
        CallType.OUTGOING -> tr("Outgoing call", "আউটগোয়িং কল")
        CallType.MANUAL -> tr("Voice recording", "ভয়েস রেকর্ডিং")
    }
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(
            onClick = { if (selecting) onToggleSelect() },
            onLongClick = onToggleSelect
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
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
            Waveform(
                data = rec.waveformData,
                progress = if (playing) progress else 0f,
                modifier = Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 8.dp)
            )
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
