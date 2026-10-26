package com.callvault.app.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun VaultLockScreen(
    biometricsEnabled: Boolean,
    onVerifyPin: (String) -> Boolean,
    onBiometric: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { if (biometricsEnabled) onBiometric() }

    fun press(d: String) {
        error = false
        if (d == "<") { pin = pin.dropLast(1); return }
        pin += d
        if (pin.length == 4) {
            if (!onVerifyPin(pin)) error = true
            pin = ""
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("Enter PIN", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) { i ->
                    Box(
                        Modifier.size(16.dp).clip(CircleShape).background(
                            if (i < pin.length) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (error) "Wrong PIN, try again" else " ",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            val rows = listOf(
                listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "<")
            )
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(Modifier.size(72.dp))
                        } else {
                            FilledTonalButton(
                                onClick = { press(key) },
                                modifier = Modifier.size(72.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text(if (key == "<") "⌫" else key, style = MaterialTheme.typography.titleLarge) }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            if (biometricsEnabled) {
                TextButton(onClick = onBiometric) { Text("Use fingerprint") }
            }
        }
    }
}
