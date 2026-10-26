package com.callvault.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.callvault.app.R

// Connect with Developer & About Section
@Composable
fun SocialMediaClickableRow(
    displayName: String,
    platformName: String,
    iconResId: Int,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(id = iconResId), contentDescription = "$platformName Icon", tint = iconTint)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = platformName, style = MaterialTheme.typography.labelSmall)
        }
        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "Open Profile")
    }
}

fun openSocialMediaLink(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open link: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun SettingsScreen(
    autoRecord: Boolean,
    onAutoRecord: (Boolean) -> Unit,
    retentionDays: Int,
    onRetention: (Int) -> Unit,
    pinEnabled: Boolean,
    onSetPin: (String) -> Unit,
    onRemovePin: () -> Unit,
    biometrics: Boolean,
    onBiometrics: (Boolean) -> Unit,
    compat: com.callvault.app.data.model.CompatibilityReport,
    bitrate: Int,
    onBitrate: (Int) -> Unit,
    recordingCount: Int,
    totalBytes: Long
) {
    var showPin by remember { mutableStateOf(false) }

    if (showPin) {
        var pin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPin = false },
            title = { Text("Set 4-digit PIN") },
            text = {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { v -> if (v.length <= 4 && v.all { it.isDigit() }) pin = v },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { onSetPin(pin); showPin = false }, enabled = pin.length == 4) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showPin = false }) { Text("Cancel") } }
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionTitle("Recording")
        ListItem(
            headlineContent = { Text("Auto-record calls") },
            supportingContent = { Text("Start recording when a call is answered") },
            trailingContent = { Switch(checked = autoRecord, onCheckedChange = onAutoRecord) }
        )
        ListItem(
            headlineContent = { Text("Audio quality") },
            supportingContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(64 to "Low", 96 to "Standard", 128 to "High").forEach { (k, label) ->
                        FilterChip(selected = bitrate == k, onClick = { onBitrate(k) }, label = { Text(label) })
                    }
                }
            }
        )
        ListItem(
            headlineContent = { Text("Trash retention") },
            supportingContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 30, 90).forEach { d ->
                        FilterChip(selected = retentionDays == d, onClick = { onRetention(d) }, label = { Text("$d days") })
                    }
                }
            }
        )
        SectionTitle("Security")
        ListItem(
            headlineContent = { Text(if (pinEnabled) "Change PIN" else "Set PIN lock") },
            supportingContent = { Text("Protect your recordings with a 4-digit PIN") },
            modifier = Modifier.clickable { showPin = true }
        )
        if (pinEnabled) {
            ListItem(
                headlineContent = { Text("Fingerprint unlock") },
                trailingContent = { Switch(checked = biometrics, onCheckedChange = onBiometrics) }
            )
            ListItem(
                headlineContent = { Text("Remove PIN lock") },
                modifier = Modifier.clickable { onRemovePin() }
            )
        }
        SectionTitle("Storage")
        ListItem(
            headlineContent = { Text("$recordingCount recordings") },
            supportingContent = { Text(com.callvault.app.utils.Format.size(totalBytes) + " used") }
        )
        SectionTitle("About this phone")
        ListItem(
            headlineContent = { Text("${compat.manufacturer} ${compat.model}") },
            supportingContent = { Text(compat.notes) }
        )
        ListItem(
            headlineContent = { Text("Note") },
            supportingContent = {
                Text("Android limits call audio for third-party apps. Recordings use the microphone, so the other person is usually clear only on speaker.")
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}
