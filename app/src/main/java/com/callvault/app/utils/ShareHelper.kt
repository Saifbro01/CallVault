package com.callvault.app.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.callvault.app.data.database.RecordingEntity
import java.io.File

object ShareHelper {
    fun share(context: Context, rec: RecordingEntity) {
        try {
            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", File(rec.filePath))
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "audio/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "Share recording").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
