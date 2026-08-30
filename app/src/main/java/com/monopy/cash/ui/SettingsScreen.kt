package com.monopy.cash.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.monopy.cash.model.Currencies
import com.monopy.cash.model.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    onBack: () -> Unit,
    onSelectCurrency: (String) -> Unit,
    onAddQuickAmount: (Long) -> Unit,
    onRemoveQuickAmount: (Long) -> Unit
) {
    var newAmount by remember { mutableStateOf("") }
    val pendingAmount = newAmount.toLongOrNull() ?: 0L
    val atLimit = settings.quickAmounts.size >= Settings.MAX_QUICK_AMOUNTS

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    text = "Currency",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(Currencies, key = { it.label }) { currency ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectCurrency(currency.symbol) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currency.symbol == settings.currencySymbol,
                        onClick = { onSelectCurrency(currency.symbol) }
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(currency.label, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = formatMoney(1500, currency.symbol),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Quick cash shortcuts",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
            item {
                Text(
                    text = "These are the one-tap amounts on the cash and transfer screens.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (settings.quickAmounts.isEmpty()) {
                item {
                    Text(
                        text = "No shortcuts — amounts are typed in by hand.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(settings.quickAmounts, key = { it }) { amount ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InputChip(
                        selected = false,
                        onClick = { onRemoveQuickAmount(amount) },
                        label = { Text(money(amount)) },
                        trailingIcon = {
                            Icon(Icons.Filled.Close, contentDescription = "Remove ${money(amount)}")
                        }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newAmount,
                        onValueChange = { input -> newAmount = input.filter { it.isDigit() }.take(9).trimStart('0') },
                        label = { Text("New shortcut") },
                        singleLine = true,
                        enabled = !atLimit,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        enabled = !atLimit && pendingAmount > 0 && pendingAmount !in settings.quickAmounts,
                        onClick = {
                            onAddQuickAmount(pendingAmount)
                            newAmount = ""
                        }
                    ) { Text("Add") }
                }
            }

            if (atLimit) {
                item {
                    Text(
                        text = "That is the most shortcuts that fit — remove one to add another.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}
