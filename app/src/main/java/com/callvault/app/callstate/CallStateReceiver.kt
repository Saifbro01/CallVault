package com.callvault.app.callstate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Call state handling goes here (start/stop RecordingService).
    }
}
