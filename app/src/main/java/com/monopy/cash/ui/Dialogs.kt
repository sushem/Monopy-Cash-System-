package com.monopy.cash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.monopy.cash.model.Account
import com.monopy.cash.model.DEFAULT_STARTING_CASH
import com.monopy.cash.model.PlayerColors

/** Horizontal strip of token colours; the selected one is ticked. */
@Composable
fun ColorPicker(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(PlayerColors) { color ->
            val isSelected = color == selected
            Row(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.toComposeColor())
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color(0x33000000),
                        shape = CircleShape
                    )
                    .clickable { onSelect(color) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = color.toComposeColor().contrastingContentColor()
                    )
                }
            }
        }
    }
}

@Composable
fun AddPlayerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: Int, startingCash: Long, withdrawFromBank: Boolean) -> Unit,
    suggestedColor: Int
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableIntStateOf(suggestedColor) }
    var startingCash by remember { mutableStateOf(DEFAULT_STARTING_CASH.toString()) }
    var withdrawFromBank by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add player") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = startingCash,
                    onValueChange = { input -> startingCash = input.filter { it.isDigit() }.take(9) },
                    label = { Text("Starting cash") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Token colour", style = MaterialTheme.typography.labelLarge)
                ColorPicker(selected = color, onSelect = { color = it })
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { withdrawFromBank = !withdrawFromBank }
                ) {
                    Checkbox(checked = withdrawFromBank, onCheckedChange = { withdrawFromBank = it })
                    Text("Take starting cash from the bank", modifier = Modifier.padding(start = 4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onConfirm(name.trim(), color, startingCash.toLongOrNull() ?: 0L, withdrawFromBank)
                }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditAccountDialog(
    account: Account,
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: Int) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(account.name) }
    var color by remember { mutableIntStateOf(account.colorArgb) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (account.isBank) "Edit bank" else "Edit player") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Token colour", style = MaterialTheme.typography.labelLarge)
                ColorPicker(selected = color, onSelect = { color = it })
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Remove player", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim(), color) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
