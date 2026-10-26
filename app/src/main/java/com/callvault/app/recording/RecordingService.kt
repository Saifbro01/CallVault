package com.callvault.app.recording

import android.app.Service
import android.content.Intent
import android.os.IBinder

class RecordingService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
