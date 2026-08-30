package com.monopy.cash.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.monopy.cash.model.Account
import kotlinx.coroutines.launch

private val WheelItemHeight = 44.dp

/**
 * Quick transfer: spin a payer and a payee, tap an amount, send. Nothing here that the
 * account screens cannot do, only fewer taps to do it.
 */
@Composable
fun TransferDialog(
    accounts: List<Account>,
    quickAmounts: List<Long>,
    onDismiss: () -> Unit,
    onTransfer: (fromId: String, toId: String, amount: Long) -> Unit
) {
    if (accounts.size < 2) return

    var fromIndex by remember { mutableIntStateOf(0) }
    var toIndex by remember { mutableIntStateOf(1) }
    var amountText by remember { mutableStateOf("") }

    val amount = amountText.toLongOrNull() ?: 0L
    val from = accounts[fromIndex.coerceIn(accounts.indices)]
    val to = accounts[toIndex.coerceIn(accounts.indices)]
    val sameAccount = from.id == to.id

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick transfer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AccountWheel(
                        label = "From",
                        accounts = accounts,
                        initialIndex = fromIndex,
                        onSelect = { fromIndex = it },
                        modifier = Modifier.weight(1f)
                    )
                    AccountWheel(
                        label = "To",
                        accounts = accounts,
                        initialIndex = toIndex,
                        onSelect = { toIndex = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input -> amountText = input.filter { it.isDigit() }.take(9).trimStart('0') },
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (quickAmounts.isNotEmpty()) {
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

                Text(
                    text = if (sameAccount) {
                        "Pick two different accounts."
                    } else {
                        "${from.name} pays ${to.name} ${money(amount)}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (sameAccount) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = amount > 0 && !sameAccount,
                onClick = {
                    onTransfer(from.id, to.id, amount)
                    onDismiss()
                }
            ) { Text("Transfer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** A snapping scroll wheel of accounts; whatever sits in the centre band is selected. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AccountWheel(
    label: String,
    accounts: List<Account>,
    initialIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val scope = rememberCoroutineScope()
    val centered by remember {
        derivedStateOf {
            val itemSize = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1
            val index = listState.firstVisibleItemIndex +
                if (listState.firstVisibleItemScrollOffset > itemSize / 2) 1 else 0
            index.coerceIn(0, accounts.lastIndex)
        }
    }

    LaunchedEffect(centered) { onSelect(centered) }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(modifier = Modifier.height(WheelItemHeight * 3)) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(WheelItemHeight)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    )
            )
            LazyColumn(
                state = listState,
                flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                contentPadding = PaddingValues(vertical = WheelItemHeight),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(accounts, key = { _, account -> account.id }) { index, account ->
                    val isCentered = index == centered
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(WheelItemHeight)
                            .clickable { scope.launch { listState.animateScrollToItem(index) } }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TokenAvatar(account, size = 26)
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isCentered) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
