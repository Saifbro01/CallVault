package com.callvault.app.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Format {
    fun duration(ms: Long): String {
        val s = ms / 1000
        return "%d:%02d".format(s / 60, s % 60)
    }

    fun size(bytes: Long): String =
        if (bytes < 1024L * 1024L) "${bytes / 1024} KB" else "%.1f MB".format(bytes / 1048576.0)

    fun date(ts: Long): String =
        SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(ts))
}
