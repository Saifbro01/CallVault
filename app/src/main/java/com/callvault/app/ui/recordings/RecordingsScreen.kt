package com.callvault.app.ui.recordings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.callvault.app.data.database.RecordingEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecordingsScreen(
    recordings: List<RecordingEntity>,
    onSelectRecording: (Long) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateFilter by remember { mutableStateOf("ALL") }

    // Enhanced search filtering across contact name, phone number, and multiple date formats
    val filteredRecordings = remember(recordings, searchQuery, selectedDateFilter) {
        val dateFmtFull = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val dateFmtMonthDay = SimpleDateFormat("MMMM d", Locale.getDefault())
        val dateFmtShort = SimpleDateFormat("MMM d", Locale.getDefault())
        val dateFmtIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        recordings.filter { rec ->
            val q = searchQuery.trim().lowercase(Locale.getDefault())
            val dateStrFull = dateFmtFull.format(Date(rec.timestamp)).lowercase()
            val dateStrShort = dateFmtShort.format(Date(rec.timestamp)).lowercase()
            val dateStrIso = dateFmtIso.format(Date(rec.timestamp)).lowercase()

            val matchesSearch = q.isEmpty() ||
                rec.fileName.lowercase().contains(q) ||
                (rec.contactName?.lowercase()?.contains(q) == true) ||
                (rec.phoneNumber?.contains(q) == true) ||
                dateStrFull.contains(q) ||
                dateStrShort.contains(q) ||
                dateStrIso.contains(q)

            matchesSearch
        }
    }

    // Search bar with contact name & date placeholder
    OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search by contact name or date (e.g. Sarah, Oct 24)...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
    )
}