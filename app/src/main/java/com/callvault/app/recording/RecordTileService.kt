package com.callvault.app.recording

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.callvault.app.data.model.CallType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RecordTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    override fun onStartListening() {
        job = scope.launch { RecordingService.isRecording.collect { update(it) } }
    }

    override fun onStopListening() {
        job?.cancel()
    }

    override fun onClick() {
        try {
            if (RecordingService.isRecording.value) {
                startService(Intent(this, RecordingService::class.java).setAction(RecordingService.ACTION_STOP))
            } else {
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, RecordingService::class.java)
                        .setAction(RecordingService.ACTION_START)
                        .putExtra(RecordingService.EXTRA_TYPE, CallType.MANUAL.name)
                )
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun update(recording: Boolean) {
        qsTile?.apply {
            state = if (recording) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }
}
