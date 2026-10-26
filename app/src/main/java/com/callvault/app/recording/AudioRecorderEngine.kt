package com.callvault.app.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.callvault.app.data.model.CallType
import java.io.File

class AudioRecorderEngine(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAt = 0L

    fun startRecording(callType: CallType, target: String?): Result<File> = runCatching {
        val dir = File(context.filesDir, "recordings").apply { mkdirs() }
        val file = File(dir, "${callType.name}_${target ?: "unknown"}_${System.currentTimeMillis()}.m4a")
        val mr = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        val source = if (callType == CallType.MANUAL) MediaRecorder.AudioSource.MIC
                     else MediaRecorder.AudioSource.VOICE_COMMUNICATION
        mr.setAudioSource(source)
        mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mr.setOutputFile(file.absolutePath)
        mr.prepare()
        mr.start()
        recorder = mr
        outputFile = file
        startedAt = System.currentTimeMillis()
        file
    }

    fun stopRecording(): Result<Pair<File, Long>> = runCatching {
        val file = outputFile ?: error("Not recording")
        val duration = System.currentTimeMillis() - startedAt
        try { recorder?.stop() } finally { recorder?.release(); recorder = null; outputFile = null }
        require(file.length() > 1024) { "Recording too short" }
        file to duration
    }
}
