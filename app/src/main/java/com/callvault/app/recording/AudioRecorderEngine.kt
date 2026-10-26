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

    @Suppress("DEPRECATION")
    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()

    fun startRecording(callType: CallType, target: String?): Result<File> = runCatching {
        val dir = File(context.filesDir, "recordings").apply { mkdirs() }
        val safeTarget = (target ?: "unknown").filter { it.isLetterOrDigit() || it == '+' }
        val file = File(dir, "${callType.name.lowercase()}_${safeTarget}_${System.currentTimeMillis()}.m4a")
        val mr = newRecorder()
        try {
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioSamplingRate(44100)
            mr.setAudioEncodingBitRate(com.callvault.app.data.prefs.Prefs(context).bitrateKbps * 1000)
            mr.setOutputFile(file.absolutePath)
            mr.prepare()
            mr.start()
        } catch (e: Exception) {
            mr.release()
            throw e
        }
        recorder = mr
        outputFile = file
        startedAt = System.currentTimeMillis()
        file
    }

    fun amplitude(): Int = try { recorder?.maxAmplitude ?: 0 } catch (e: Exception) { 0 }

    fun stopRecording(): Result<Pair<File, Long>> = runCatching {
        val file = outputFile ?: error("Not recording")
        val duration = System.currentTimeMillis() - startedAt
        try {
            recorder?.stop()
        } finally {
            recorder?.release()
            recorder = null
            outputFile = null
        }
        if (file.length() <= 1024L) {
            file.delete()
            error("Recording too short")
        }
        file to duration
    }
}
