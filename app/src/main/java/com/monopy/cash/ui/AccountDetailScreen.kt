package com.monopy.cash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.monopy.cash.model.Account
import com.monopy.cash.model.Asset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    account: Account,
    otherAccounts: List<Account>,
    quickAmounts: List<Long>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAdjust: (amount: Long) -> Unit,
    onTransfer: (counterpartyId: String, amount: Long, isIncoming: Boolean) -> Unit,
    onUndo: (transactionId: String) -> Unit,
    onBorrow: () -> Unit,
    onRepay: () -> Unit,
    onAddAsset: () -> Unit,
    onEditAsset: (assetId: String) -> Unit,
    onChangeHouses: (assetId: String, delta: Int) -> Unit
) {
    var isAdding by remember { mutableStateOf(true) }
    var amountText by remember { mutableStateOf("") }
    var counterpartyId by remember { mutableStateOf<String?>(null) }

    val amount = amountText.toLongOrNull() ?: 0L
    val counterparty = otherAccounts.firstOrNull { it.id == counterpartyId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(account.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit name and colour")
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { BalanceCard(account) }

            if (!account.isBank) {
                item { LoanCard(account = account, onBorrow = onBorrow, onRepay = onRepay) }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ModeButton(
                        label = "Add",
                        icon = Icons.Filled.Add,
                        selected = isAdding,
                        modifier = Modifier.weight(1f)
                    ) { isAdding = true }
                    ModeButton(
                        label = "Subtract",
                        icon = Icons.Filled.Remove,
                        selected = !isAdding,
                        modifier = Modifier.weight(1f)
                    ) { isAdding = false }
                }
            }

            item {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input -> amountText = input.filter { it.isDigit() }.take(9).trimStart('0') },
                    label = { Text("Amount") },
                    prefix = { Text("$") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickAmounts) { quick ->
                        FilterChip(
                            selected = false,
                            onClick = { amountText = (amount + quick).toString() },
                            label = { Text("+${money(quick)}") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { amountText = "" },
                            label = { Text("Clear") }
                        )
                    }
                }
            }

            item {
                Text(
                    text = if (isAdding) "Money comes from" else "Money goes to",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = counterpartyId == null,
                            onClick = { counterpartyId = null },
                            label = { Text("Nobody (adjust only)") }
                        )
                    }
                    items(otherAccounts, key = { it.id }) { other ->
                        FilterChip(
                            selected = counterpartyId == other.id,
                            onClick = { counterpartyId = if (counterpartyId == other.id) null else other.id },
                            label = { Text(other.name) }
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val value = amount
                        if (value > 0) {
                            val other = counterparty
                            if (other == null) {
                                onAdjust(if (isAdding) value else -value)
                            } else {
                                onTransfer(other.id, value, isAdding)
                            }
                            amountText = ""
                        }
                    },
                    enabled = amount > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAdding) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                ) {
                    val target = counterparty?.let { if (isAdding) " from ${it.name}" else " to ${it.name}" } ?: ""
                    Text(
                        text = if (isAdding) "Add ${money(amount)}$target" else "Subtract ${money(amount)}$target",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (!account.isBank) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (account.assets.isEmpty()) {
                                "Properties"
                            } else {
                                "Properties (${account.assets.size}) · ${account.assets.sumOf { it.houses }} houses"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onAddAsset) { Text("Add property") }
                    }
                }

                if (account.assets.isEmpty()) {
                    item {
                        Text(
                            text = "No properties yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(account.assets, key = { "asset-" + it.id }) { asset ->
                    AssetRow(
                        asset = asset,
                        onEdit = { onEditAsset(asset.id) },
                        onChangeHouses = { delta -> onChangeHouses(asset.id, delta) }
                    )
                }
            }

            item {
                Text(
                    text = "History",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (account.transactions.isEmpty()) {
                item {
                    Text(
                        text = "No money has moved yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(account.transactions.asReversed(), key = { it.id }) { transaction ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = signedMoney(transaction.amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (transaction.amount < 0) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                        Text(
                            text = "${transaction.note} · ${formatTime(transaction.timestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onUndo(transaction.id) }) {
                        Icon(Icons.Filled.Undo, contentDescription = "Undo this entry")
                    }
                }
            }
        }
    }
}

@Composable
private fun LoanCard(account: Account, onBorrow: () -> Unit, onRepay: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (account.loan > 0) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Owed to the bank",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = money(account.loan),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Net " + money(account.balance - account.loan),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 10.dp)
            ) {
                OutlinedButton(onClick = onBorrow, modifier = Modifier.weight(1f)) {
                    Text("Take loan")
                }
                OutlinedButton(
                    onClick = onRepay,
                    enabled = account.loan > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Repay")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetRow(asset: Asset, onEdit: () -> Unit, onChangeHouses: (Int) -> Unit) {
    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = asset.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = houseLabel(asset.houses),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HouseStepper(houses = asset.houses, onChange = { onChangeHouses(it - asset.houses) })
        }
    }
}

@Composable
private fun BalanceCard(account: Account) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TokenAvatar(account, size = 56)
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = if (account.isBank) "Bank balance" else "Cash on hand",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = money(account.balance),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (account.balance < 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun ModeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) {
            Icon(icon, contentDescription = null)
            Box(modifier = Modifier.padding(start = 6.dp)) { Text(label) }
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Icon(icon, contentDescription = null)
            Box(modifier = Modifier.padding(start = 6.dp)) { Text(label) }
        }
    }
}
