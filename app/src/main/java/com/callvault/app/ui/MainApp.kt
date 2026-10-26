package com.callvault.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.callvault.app.recording.RecordingService
import com.callvault.app.ui.navigation.Screen
import com.callvault.app.ui.recordings.RecordingsScreen
import com.callvault.app.ui.settings.SettingsScreen
import com.callvault.app.ui.trash.TrashScreen
import com.callvault.app.utils.CompatibilityChecker
import com.callvault.app.utils.ShareHelper
import com.callvault.app.utils.tr
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

private data class Tab(val screen: Screen, val label: String, val icon: ImageVector)

@Composable
fun MainApp(vm: MainViewModel, onToggleRecord: (Boolean) -> Unit) {
    val nav = rememberNavController()
    val recordings by vm.recordings.collectAsState()
    val trash by vm.trash.collectAsState()
    val isRecording by RecordingService.isRecording.collectAsState()
    val compat = CompatibilityChecker.generateReport()
    val context = LocalContext.current
    LaunchedEffect(vm.message) {
        vm.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            vm.clearMessage()
        }
    }

    val tabs = listOf(
        Tab(Screen.Recordings, tr("Recordings", "রেকর্ডিং"), Icons.Default.Mic),
        Tab(Screen.Trash, tr("Trash", "ট্র্যাশ"), Icons.Default.Delete),
        Tab(Screen.Settings, tr("Settings", "সেটিংস"), Icons.Default.Settings)
    )
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.screen.route,
                        onClick = {
                            nav.navigate(tab.screen.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = Screen.Recordings.route, modifier = Modifier.padding(padding)) {
            composable(Screen.Recordings.route) {
                RecordingsScreen(
                    recordings = recordings,
                    playingId = vm.playingId,
                    progress = vm.progress,
                    isRecording = isRecording,
                    onToggleRecord = { onToggleRecord(isRecording) },
                    onPlay = vm::togglePlay,
                    onSeek = vm::seekTo,
                    onFavorite = vm::toggleFavorite,
                    onTrash = vm::moveToTrash,
                    onTrashMany = vm::trashMany,
                    onShare = { ShareHelper.share(context, it) },
                    onRename = vm::rename
                )
            }
            composable(Screen.Trash.route) {
                TrashScreen(
                    trashedRecordings = trash,
                    retentionDays = vm.retentionDays,
                    onRestoreRecording = vm::restore,
                    onRestoreAll = vm::restoreAll,
                    onPermanentDelete = vm::deleteForever,
                    onEmptyTrash = vm::emptyTrash
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    vm = vm,
                    compat = compat,
                    recordingCount = recordings.size,
                    totalBytes = recordings.sumOf { it.fileSizeBytes }
                )
            }
        }
    }
}
