package com.callvault.app.callstate

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.callvault.app.data.model.CallType
import com.callvault.app.data.prefs.Prefs
import com.callvault.app.recording.RecordingService

class CallStateReceiver : BroadcastReceiver() {

    companion object {
        private var lastState: String = TelephonyManager.EXTRA_STATE_IDLE
        private var lastNumber: String? = null
    }

    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)?.let { lastNumber = it }

        if (!Prefs(context).autoRecord) { lastState = state; return }
        val micOk = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

        try {
            when (state) {
                TelephonyManager.EXTRA_STATE_OFFHOOK -> if (micOk) {
                    val type = if (lastState == TelephonyManager.EXTRA_STATE_RINGING) CallType.INCOMING else CallType.OUTGOING
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, RecordingService::class.java)
                            .setAction(RecordingService.ACTION_START)
                            .putExtra(RecordingService.EXTRA_TYPE, type.name)
                            .putExtra(RecordingService.EXTRA_NUMBER, lastNumber)
                    )
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    if (lastState == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                        context.startService(
                            Intent(context, RecordingService::class.java).setAction(RecordingService.ACTION_STOP)
                        )
                    }
                    lastNumber = null
                }
            }
        } catch (e: Exception) {
            // Android may block starting a microphone service from the background.
        }
        lastState = state
    }
}
