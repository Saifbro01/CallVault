package com.callvault.app.ui.recordings

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

@Composable
fun Waveform(data: String, progress: Float, modifier: Modifier = Modifier) {
    val bars = remember(data) { data.split(",").mapNotNull { it.trim().toIntOrNull() } }
    if (bars.isEmpty()) return
    val played = MaterialTheme.colorScheme.primary
    val rest = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        val w = size.width / bars.size
        bars.forEachIndexed { i, v ->
            val h = (v / 100f).coerceIn(0.08f, 1f) * size.height
            drawRect(
                color = if (i.toFloat() / bars.size < progress) played else rest,
                topLeft = Offset(i * w + w * 0.15f, (size.height - h) / 2f),
                size = Size(w * 0.7f, h)
            )
        }
    }
}
