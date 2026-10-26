package com.callvault.app.ui

import android.app.Application
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
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
import com.callvault.app.utils.Lang
import com.callvault.app.utils.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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

    var message by mutableStateOf<String?>(null)
    var bangla by mutableStateOf(prefs.bangla)
        private set

    init {
        Lang.bangla = prefs.bangla
        purgeExpired()
    }

    fun clearMessage() { message = null }

    fun updateBangla(v: Boolean) { prefs.bangla = v; bangla = v; Lang.bangla = v }

    fun trashMany(list: List<RecordingEntity>) {
        list.forEach { if (playingId == it.id) stopPlayback() }
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            list.forEach { dao.setDeletedAt(it.id, now) }
        }
    }

    private val audioExt = setOf("amr", "3gp", "m4a", "mp3", "aac", "wav", "ogg", "opus", "mp4")

    fun importFromFolder(uri: Uri) {
        val ctx = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                try {
                    ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) { /* not persistable, fine */ }
                val root = DocumentFile.fromTreeUri(ctx, uri)
                if (root == null) {
                    message = tr("Could not open that folder", "ফোল্ডারটি খোলা যায়নি")
                    return@launch
                }
                val existing = dao.getAllNames().toHashSet()
                val dir = File(ctx.filesDir, "recordings").apply { mkdirs() }
                var count = 0

                fun walk(d: DocumentFile) {
                    for (f in d.listFiles()) {
                        if (f.isDirectory) { walk(f); continue }
                        val orig = f.name ?: continue
                        val ext = orig.substringAfterLast('.', "").lowercase()
                        val isAudio = (f.type?.startsWith("audio/") == true) || ext in audioExt
                        if (!isAudio) continue
                        val name = "imp_$orig"
                        if (name in existing) continue
                        val out = File(dir, name)
                        val copied = ctx.contentResolver.openInputStream(f.uri)?.use { input ->
                            out.outputStream().use { o -> input.copyTo(o) }
                            true
                        } ?: false
                        if (!copied) continue
                        val dur = try {
                            val r = MediaMetadataRetriever()
                            r.setDataSource(out.absolutePath)
                            val d2 = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                            r.release()
                            d2
                        } catch (e: Exception) { 0L }
                        val lower = orig.lowercase()
                        val type = when {
                            lower.contains("incoming") -> CallType.INCOMING
                            lower.contains("outgoing") -> CallType.OUTGOING
                            else -> CallType.MANUAL
                        }
                        val title = orig.substringBeforeLast('.').replace('_', ' ').trim()
                        kotlinx.coroutines.runBlocking {
                            dao.insert(
                                RecordingEntity(
                                    fileName = name,
                                    filePath = out.absolutePath,
                                    contactName = title,
                                    callType = type,
                                    timestamp = if (f.lastModified() > 0) f.lastModified() else System.currentTimeMillis(),
                                    durationMs = dur,
                                    fileSizeBytes = out.length(),
                                    audioFormat = ext.ifEmpty { "m4a" }
                                )
                            )
                        }
                        existing.add(name)
                        count++
                    }
                }
                walk(root)
                message = if (count == 0) tr("No new audio files found", "নতুন কোনো অডিও ফাইল পাওয়া যায়নি")
                else tr("Imported $count recordings", "$count টি রেকর্ডিং ইম্পোর্ট হয়েছে")
            } catch (e: Exception) {
                message = tr("Import failed: ${e.localizedMessage}", "ইম্পোর্ট ব্যর্থ: ${e.localizedMessage}")
            }
        }
    }

    fun exportBackup(uri: Uri) {
        val ctx = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = dao.getAll()
                var count = 0
                ctx.contentResolver.openOutputStream(uri)?.use { os ->
                    ZipOutputStream(os).use { z ->
                        list.forEach { r ->
                            val f = File(r.filePath)
                            if (f.exists()) {
                                z.putNextEntry(ZipEntry(f.name))
                                f.inputStream().use { it.copyTo(z) }
                                z.closeEntry()
                                count++
                            }
                        }
                    }
                }
                message = tr("Backup saved ($count recordings)", "ব্যাকআপ সেভ হয়েছে ($count টি রেকর্ডিং)")
            } catch (e: Exception) {
                message = tr("Backup failed: ${e.localizedMessage}", "ব্যাকআপ ব্যর্থ: ${e.localizedMessage}")
            }
        }
    }

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
