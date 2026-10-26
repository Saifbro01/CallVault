package com.callvault.app.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.callvault.app.CallVaultApplication
import com.callvault.app.MainActivity
import com.callvault.app.data.database.RecordingEntity
import com.callvault.app.data.model.CallType
import com.callvault.app.data.prefs.Prefs
import com.callvault.app.utils.ContactLookup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RecordingService : Service() {

    companion object {
        const val ACTION_START = "com.callvault.app.START"
        const val ACTION_STOP = "com.callvault.app.STOP"
        const val ACTION_TOGGLE = "com.callvault.app.TOGGLE"
        const val EXTRA_TYPE = "type"
        const val EXTRA_NUMBER = "number"
        private const val CHANNEL_ID = "recording"
        private const val NOTIFICATION_ID = 42

        private val _isRecording = MutableStateFlow(false)
        val isRecording: StateFlow<Boolean> = _isRecording
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var engine: AudioRecorderEngine? = null
    private var callType = CallType.MANUAL
    private var number: String? = null
    private var sampler: Job? = null
    private val samples = mutableListOf<Int>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording(intent)
            ACTION_STOP -> stopRecording()
            ACTION_TOGGLE -> if (engine != null) stopRecording() else startRecording(intent)
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun enterForeground(): Boolean {
        createChannel()
        return try {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun startRecording(intent: Intent) {
        if (!enterForeground()) {
            stopSelf()
            return
        }
        if (engine != null) return
        callType = runCatching { CallType.valueOf(intent.getStringExtra(EXTRA_TYPE) ?: "") }
            .getOrDefault(CallType.MANUAL)
        number = intent.getStringExtra(EXTRA_NUMBER)

        val eng = AudioRecorderEngine(this)
        if (eng.startRecording(callType, number).isFailure) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        engine = eng
        synchronized(samples) { samples.clear() }
        sampler = scope.launch {
            while (isActive) {
                val a = eng.amplitude()
                synchronized(samples) { samples.add(a) }
                delay(100)
            }
        }
        _isRecording.value = true
    }

    private fun buildWaveform(): String {
        val s = synchronized(samples) { samples.toList() }
        if (s.isEmpty()) return ""
        val n = 48
        val max = (s.maxOrNull() ?: 1).coerceAtLeast(1)
        return (0 until n).joinToString(",") { i ->
            val from = i * s.size / n
            val to = maxOf(from + 1, (i + 1) * s.size / n).coerceAtMost(s.size)
            val seg = s.subList(from, to)
            (seg.average() * 100 / max).toInt().toString()
        }
    }

    private fun stopRecording() {
        val eng = engine
        engine = null
        _isRecording.value = false
        sampler?.cancel()
        if (eng == null) {
            stopSelf()
            return
        }
        val type = callType
        val target = number
        val wave = buildWaveform()
        val result = eng.stopRecording()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        result.onSuccess { (file, duration) ->
            scope.launch {
                val dao = (application as CallVaultApplication).database.recordingDao()
                dao.insert(
                    RecordingEntity(
                        fileName = file.name,
                        filePath = file.absolutePath,
                        contactName = ContactLookup.name(this@RecordingService, target),
                        phoneNumber = target,
                        callType = type,
                        timestamp = System.currentTimeMillis() - duration,
                        durationMs = duration,
                        fileSizeBytes = file.length(),
                        waveformData = wave
                    )
                )
                stopSelf()
            }
        }.onFailure { stopSelf() }
    }

    override fun onDestroy() {
        val eng = engine
        if (eng != null) {
            eng.stopRecording().onSuccess { (file, duration) ->
                val dao = (application as CallVaultApplication).database.recordingDao()
                kotlinx.coroutines.runBlocking {
                    dao.insert(
                        RecordingEntity(
                            fileName = file.name, filePath = file.absolutePath, phoneNumber = number,
                            callType = callType, durationMs = duration, fileSizeBytes = file.length()
                        )
                    )
                }
            }
            engine = null
        }
        sampler?.cancel()
        _isRecording.value = false
        super.onDestroy()
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Recording", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun buildNotification(): Notification {
        val bn = Prefs(this).bangla
        val stop = PendingIntent.getService(
            this, 0,
            Intent(this, RecordingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val open = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(if (bn) "CallVault রেকর্ড করছে" else "CallVault is recording")
            .setContentText(if (bn) "শেষ করতে Stop চাপুন" else "Tap Stop to finish and save")
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(0, if (bn) "থামান" else "Stop", stop)
            .build()
    }
}
