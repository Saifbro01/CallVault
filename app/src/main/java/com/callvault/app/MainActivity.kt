package com.callvault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.utils.CompatibilityChecker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CallVaultTheme {
                Surface(color = MaterialTheme.colorScheme.background) { Home() }
            }
        }
    }
}

@Composable
private fun Home() {
    val report = CompatibilityChecker.generateReport()
    Column(Modifier.statusBarsPadding().padding(24.dp)) {
        Text("CallVault", style = MaterialTheme.typography.headlineMedium)
        Text("Device: ${report.manufacturer} ${report.model}")
        Text(report.notes)
    }
}
