package com.callvault.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.callvault.app.R
import com.callvault.app.data.model.CompatibilityReport
import com.callvault.app.ui.MainViewModel
import com.callvault.app.utils.Format
import com.callvault.app.utils.PhoneSetup
import com.callvault.app.utils.tr

private const val FACEBOOK_URL = "https://www.facebook.com/share/1M9ethZGMB/?mibextid=wwXIfr"
private const val INSTAGRAM_URL = "https://www.instagram.com/fakesaif_7"

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
    vm: MainViewModel,
    compat: CompatibilityReport,
    recordingCount: Int,
    totalBytes: Long
) {
    val context = LocalContext.current
    var showPin by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.importFromFolder(uri)
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> if (uri != null) vm.exportBackup(uri) }

    if (showPin) {
        var pin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPin = false },
            title = { Text(tr("Set 4-digit PIN", "৪ সংখ্যার পিন দিন")) },
            text = {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { v -> if (v.length <= 4 && v.all { it.isDigit() }) pin = v },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.setPin(pin); showPin = false }, enabled = pin.length == 4) {
                    Text(tr("Save", "সেভ"))
                }
            },
            dismissButton = { TextButton(onClick = { showPin = false }) { Text(tr("Cancel", "বাতিল")) } }
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionTitle(tr("Language", "ভাষা"))
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !vm.bangla, onClick = { vm.updateBangla(false) }, label = { Text("English") })
            FilterChip(selected = vm.bangla, onClick = { vm.updateBangla(true) }, label = { Text("বাংলা") })
        }

        SectionTitle(tr("Recording", "রেকর্ডিং"))
        ListItem(
            headlineContent = { Text(tr("Auto-record calls", "কল অটো-রেকর্ড")) },
            supportingContent = { Text(tr("Start recording when a call is answered", "কল রিসিভ করলে রেকর্ড শুরু হবে")) },
            trailingContent = { Switch(checked = vm.autoRecord, onCheckedChange = vm::updateAutoRecord) }
        )
        ListItem(
            headlineContent = { Text(tr("Audio quality", "অডিও কোয়ালিটি")) },
            supportingContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        64 to tr("Low", "কম"),
                        96 to tr("Standard", "মাঝারি"),
                        128 to tr("High", "উচ্চ")
                    ).forEach { (k, label) ->
                        FilterChip(selected = vm.bitrate == k, onClick = { vm.updateBitrate(k) }, label = { Text(label) })
                    }
                }
            }
        )
        ListItem(
            headlineContent = { Text(tr("Trash retention", "ট্র্যাশে রাখার সময়")) },
            supportingContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 30, 90).forEach { d ->
                        FilterChip(
                            selected = vm.retentionDays == d,
                            onClick = { vm.updateRetention(d) },
                            label = { Text(tr("$d days", "$d দিন")) }
                        )
                    }
                }
            }
        )

        SectionTitle(tr("Security", "নিরাপত্তা"))
        ListItem(
            headlineContent = { Text(if (vm.pinEnabled) tr("Change PIN", "পিন বদলান") else tr("Set PIN lock", "পিন লক চালু করুন")) },
            supportingContent = { Text(tr("Protect your recordings with a 4-digit PIN", "৪ সংখ্যার পিন দিয়ে রেকর্ডিং সুরক্ষিত রাখুন")) },
            modifier = Modifier.clickable { showPin = true }
        )
        if (vm.pinEnabled) {
            ListItem(
                headlineContent = { Text(tr("Fingerprint unlock", "ফিঙ্গারপ্রিন্ট আনলক")) },
                trailingContent = { Switch(checked = vm.biometrics, onCheckedChange = vm::updateBiometrics) }
            )
            ListItem(
                headlineContent = { Text(tr("Remove PIN lock", "পিন লক বন্ধ করুন")) },
                modifier = Modifier.clickable { vm.removePin() }
            )
        }

        SectionTitle(tr("Backup & Import", "ব্যাকআপ ও ইম্পোর্ট"))
        ListItem(
            headlineContent = { Text(tr("Export backup (ZIP)", "ব্যাকআপ এক্সপোর্ট (ZIP)")) },
            supportingContent = { Text(tr("Save all recordings into one ZIP file", "সব রেকর্ডিং একটা ZIP ফাইলে সেভ করুন")) },
            modifier = Modifier.clickable { exportLauncher.launch("CallVault-backup.zip") }
        )
        ListItem(
            headlineContent = { Text(tr("Import from phone folder", "ফোনের ফোল্ডার থেকে ইম্পোর্ট")) },
            supportingContent = {
                Text(
                    tr(
                        "Pick the folder of your phone's own call recorder (e.g. Recordings / Call Recordings) to bring those calls into CallVault",
                        "ফোনের নিজস্ব কল রেকর্ডারের ফোল্ডার (যেমন Recordings / Call Recordings) বেছে নিন, ওই কলগুলো CallVault-এ চলে আসবে"
                    )
                )
            },
            modifier = Modifier.clickable { importLauncher.launch(null) }
        )

        SectionTitle(tr("Phone setup (for reliable recording)", "ফোন সেটআপ (ঠিকমতো রেকর্ডের জন্য)"))
        ListItem(
            headlineContent = { Text(tr("Allow background battery use", "ব্যাকগ্রাউন্ডে ব্যাটারি ব্যবহারের অনুমতি")) },
            supportingContent = { Text(tr("Stops the phone from killing the recorder", "ফোন যাতে রেকর্ডার বন্ধ করে না দেয়")) },
            modifier = Modifier.clickable { PhoneSetup.requestBatteryExemption(context) }
        )
        ListItem(
            headlineContent = { Text(tr("Open auto-start / app settings", "অটো-স্টার্ট / অ্যাপ সেটিংস খুলুন")) },
            supportingContent = { Text(tr("Turn on auto-launch for CallVault", "CallVault-এর জন্য অটো-লঞ্চ চালু করুন")) },
            modifier = Modifier.clickable { PhoneSetup.openAutoStart(context) }
        )
        if (PhoneSetup.isOppoFamily()) {
            ListItem(
                headlineContent = { Text(tr("OPPO / ColorOS guide", "OPPO / ColorOS গাইড")) },
                supportingContent = {
                    Text(
                        tr(
                            "1) Settings > Apps > App management > CallVault > Battery usage > Allow background activity, and turn on Auto-launch.\n2) Open Recent apps and lock CallVault (pull the card down / lock icon).\n3) For the other person's voice, use speaker, or enable the phone's own call recording (Phone app > Settings > Call recording) and import it from the folder above.",
                            "১) Settings > Apps > App management > CallVault > Battery usage > Allow background activity দিন এবং Auto-launch চালু করুন।\n২) Recent apps খুলে CallVault লক করুন।\n৩) অন্য পাশের কথা পেতে স্পিকার দিন, অথবা ফোনের নিজস্ব কল রেকর্ডিং (Phone অ্যাপ > Settings > Call recording) চালু করে উপরের ফোল্ডার থেকে ইম্পোর্ট করুন।"
                        )
                    )
                }
            )
        }
        ListItem(
            headlineContent = { Text(tr("Quick tile & widget", "কুইক টাইল ও উইজেট")) },
            supportingContent = {
                Text(
                    tr(
                        "Add the Record tile to your notification shade, or add the CallVault widget to your home screen, to start/stop recording in one tap.",
                        "নোটিফিকেশন শেডে Record টাইল বা হোম স্ক্রিনে CallVault উইজেট যোগ করুন, এক ট্যাপেই রেকর্ড শুরু/শেষ হবে।"
                    )
                )
            }
        )

        SectionTitle(tr("Storage", "স্টোরেজ"))
        ListItem(
            headlineContent = { Text(tr("$recordingCount recordings", "$recordingCount টি রেকর্ডিং")) },
            supportingContent = { Text(Format.size(totalBytes) + tr(" used", " ব্যবহৃত")) }
        )

        SectionTitle(tr("About this phone", "এই ফোন সম্পর্কে"))
        ListItem(
            headlineContent = { Text("${compat.manufacturer} ${compat.model}") },
            supportingContent = { Text(compat.notes) }
        )
        ListItem(
            headlineContent = { Text(tr("Note", "জেনে রাখুন")) },
            supportingContent = {
                Text(
                    tr(
                        "Android limits call audio for third-party apps. Recordings use the microphone, so the other person is usually clear only on speaker.",
                        "অ্যান্ড্রয়েড থার্ড-পার্টি অ্যাপকে কলের অডিও দেয় না। রেকর্ড মাইক থেকে হয়, তাই অন্য পাশের কথা সাধারণত স্পিকারে ভালো আসে।"
                    )
                )
            }
        )

        SectionTitle(tr("Developer", "ডেভেলপার"))
        ListItem(
            headlineContent = { Text(tr("Developed by", "ডেভেলপ করেছেন")) },
            supportingContent = { Text("Saifullah Al Saif\nMd. Shafiqul Islam", style = MaterialTheme.typography.bodyLarge) }
        )
        SocialMediaClickableRow(
            displayName = "Saifullah Al Saif",
            platformName = "Facebook",
            iconResId = R.drawable.ic_facebook,
            iconTint = Color.Unspecified,
            onClick = { openSocialMediaLink(context, FACEBOOK_URL) }
        )
        SocialMediaClickableRow(
            displayName = "Fakesaif_7",
            platformName = "Instagram",
            iconResId = R.drawable.ic_instagram,
            iconTint = Color.Unspecified,
            onClick = { openSocialMediaLink(context, INSTAGRAM_URL) }
        )
        Spacer(Modifier.height(24.dp))
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
