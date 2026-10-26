package com.callvault.app.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.callvault.app.CallVaultApplication
import com.callvault.app.data.database.RecordingEntity
import com.callvault.app.data.model.CallType
import com.callvault.app.data.prefs.Prefs
import com.callvault.app.recording.RecordingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as CallVaultApplication).database.recordingDao()
    private val prefs = Prefs(app)
    private val dayMs = 24L * 60 * 60 * 1000

    val recordings: StateFlow<List<RecordingEntity>> =
        dao.observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val trash: StateFlow<List<RecordingEntity>> =
        dao.observeTrash().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var pinEnabled by mutableStateOf(prefs.hasPin())
        private set
    var locked by mutableStateOf(prefs.hasPin())
    var autoRecord by mutableStateOf(prefs.autoRecord)
        private set
    var retentionDays by mutableStateOf(prefs.retentionDays)
        private set
    var biometrics by mutableStateOf(prefs.biometrics)
        private set
    var playingId by mutableStateOf<Long?>(null)
        private set

    var progress by mutableStateOf(0f)
        private set
    var bitrate by mutableStateOf(prefs.bitrateKbps)
        private set

    private var player: ExoPlayer? = null
    private var progressJob: Job? = null

    init { purgeExpired() }

    // ---- recording ----
    fun startManual() {
        val ctx = getApplication<Application>()
        ctx.startForegroundService(
            Intent(ctx, RecordingService::class.java)
                .setAction(RecordingService.ACTION_START)
                .putExtra(RecordingService.EXTRA_TYPE, CallType.MANUAL.name)
        )
    }

    fun stopManual() {
        val ctx = getApplication<Application>()
        ctx.startService(Intent(ctx, RecordingService::class.java).setAction(RecordingService.ACTION_STOP))
    }

    // ---- playback ----
    fun togglePlay(rec: RecordingEntity) {
        val p = player ?: ExoPlayer.Builder(getApplication()).build().also {
            it.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) playingId = null
                }
                override fun onPlayerError(error: PlaybackException) { playingId = null }
            })
            player = it
        }
        if (playingId == rec.id) {
            p.pause()
            playingId = null
            return
        }
        p.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(rec.filePath))))
        p.prepare()
        p.play()
        playingId = rec.id
        progress = 0f
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (playingId != null) {
                val d = p.duration
                if (d > 0) progress = (p.currentPosition.toFloat() / d).coerceIn(0f, 1f)
                delay(250)
            }
        }
    }

    fun seekTo(fraction: Float) {
        val p = player ?: return
        val d = p.duration
        if (d > 0) { p.seekTo((d * fraction).toLong()); progress = fraction }
    }

    private fun stopPlayback() {
        player?.stop()
        playingId = null
    }

    // ---- library actions ----
    fun rename(rec: RecordingEntity, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.setContactName(rec.id, name.trim().ifEmpty { null })
        }
    }

    fun toggleFavorite(rec: RecordingEntity) {
        viewModelScope.launch(Dispatchers.IO) { dao.setFavorite(rec.id, !rec.isFavorite) }
    }

    fun moveToTrash(rec: RecordingEntity) {
        if (playingId == rec.id) stopPlayback()
        viewModelScope.launch(Dispatchers.IO) { dao.setDeletedAt(rec.id, System.currentTimeMillis()) }
    }

    fun restore(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { dao.setDeletedAt(id, null) }
    }

    fun restoreAll() {
        viewModelScope.launch(Dispatchers.IO) { dao.restoreAll() }
    }

    fun deleteForever(rec: RecordingEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            File(rec.filePath).delete()
            dao.delete(rec)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.getTrashed().forEach {
                File(it.filePath).delete()
                dao.delete(it)
            }
        }
    }

    private fun purgeExpired() {
        viewModelScope.launch(Dispatchers.IO) {
            val cutoff = System.currentTimeMillis() - prefs.retentionDays * dayMs
            dao.getExpired(cutoff).forEach {
                File(it.filePath).delete()
                dao.delete(it)
            }
        }
    }

    // ---- settings ----
    fun updateAutoRecord(v: Boolean) { prefs.autoRecord = v; autoRecord = v }

    fun updateRetention(days: Int) { prefs.retentionDays = days; retentionDays = days; purgeExpired() }

    fun updateBitrate(k: Int) { prefs.bitrateKbps = k; bitrate = k }

    fun updateBiometrics(v: Boolean) { prefs.biometrics = v; biometrics = v }

    fun setPin(pin: String) { prefs.setPin(pin); pinEnabled = true }

    fun removePin() { prefs.clearPin(); pinEnabled = false; locked = false }

    fun verifyPin(pin: String): Boolean {
        val ok = prefs.checkPin(pin)
        if (ok) locked = false
        return ok
    }

    fun lockIfNeeded() { if (pinEnabled) locked = true }

    override fun onCleared() {
        player?.release()
        player = null
    }
}
