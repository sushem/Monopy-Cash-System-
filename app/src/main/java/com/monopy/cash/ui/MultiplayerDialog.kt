package com.monopy.cash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.monopy.cash.net.LanSession
import com.monopy.cash.net.NetRole

/**
 * One device hosts the game and the others join it over the same Wi-Fi by typing the
 * host's address.
 */
@Composable
fun MultiplayerDialog(
    role: NetRole,
    suggestedName: String,
    onHost: () -> Unit,
    onJoin: (address: String, name: String) -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit
) {
    var address by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(suggestedName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("एकत्र खेळा · Play together") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (role) {
                    is NetRole.Hosting -> {
                        Text(
                            text = "हा फोन खेळ चालवत आहे · This device is hosting.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "इतरांनी हा पत्ता टाका · Others join at",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${role.address}:${LanSession.PORT}",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "${role.players} जोडलेले · ${role.players} connected",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    is NetRole.Joined -> {
                        Text(
                            text = if (role.connected) {
                                "जोडले गेले · Connected to ${role.address}"
                            } else {
                                "जोडणी तुटली · Not connected to ${role.address}"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (role.error != null) {
                            Text(
                                text = role.error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            text = "यजमानाच्या फोनवर खेळ चालतो; तुमची पाळी आल्यावर फासे टाका.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    NetRole.Offline -> {
                        Text(
                            text = "एक फोन खेळ चालवतो, बाकीचे त्याला जोडतात — सर्व एकाच Wi-Fi वर हवेत.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = onHost, modifier = Modifier.fillMaxWidth()) {
                            Text("खेळ चालवा · Host the game")
                        }
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("तुमचे नाव · Your name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("यजमानाचा पत्ता · Host address") },
                            placeholder = { Text("192.168.1.5") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedButton(
                            onClick = { onJoin(address, name) },
                            enabled = address.isNotBlank() && name.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("सामील व्हा · Join")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("बंद · Close") } },
        dismissButton = {
            if (role != NetRole.Offline) {
                TextButton(onClick = onStop) { Text("थांबवा · Disconnect") }
            }
        }
    )
}
