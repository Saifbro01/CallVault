package com.callvault.app.utils

import android.os.Build
import com.callvault.app.data.model.CompatibilityReport
import com.callvault.app.data.model.DeviceSupportLevel

object CompatibilityChecker {
    fun generateReport(): CompatibilityReport {
        val maker = Build.MANUFACTURER.lowercase()
        val (level, note) = when {
            maker.contains("oppo") || maker.contains("samsung") || maker.contains("xiaomi") ->
                DeviceSupportLevel.PARTIAL to "Vendor call-recording hooks may be available."
            else -> DeviceSupportLevel.PARTIAL to "Microphone-based recording only."
        }
        return CompatibilityReport(Build.MANUFACTURER, Build.MODEL, level, note)
    }
}
