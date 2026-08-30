package com.monopy.cash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monopy.cash.data.GameStore
import com.monopy.cash.model.PlayerColors
import com.monopy.cash.ui.AccountDetailScreen
import com.monopy.cash.ui.AccountListScreen
import com.monopy.cash.ui.AddPlayerDialog
import com.monopy.cash.ui.AmountDialog
import com.monopy.cash.ui.AssetDialog
import com.monopy.cash.ui.ConfirmDialog
import com.monopy.cash.ui.EditAccountDialog
import com.monopy.cash.ui.LocalCurrency
import com.monopy.cash.ui.SettingsScreen
import com.monopy.cash.ui.TransferDialog
import com.monopy.cash.ui.money
import com.monopy.cash.ui.theme.MonopyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = GameStore.get(this)
        setContent {
            MonopyTheme {
                MonopyApp(store)
            }
        }
    }
}

@Composable
private fun MonopyApp(store: GameStore) {
    val accounts by store.accounts.collectAsStateWithLifecycle()
    val settings by store.settings.collectAsStateWithLifecycle()

    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddPlayer by remember { mutableStateOf(false) }
    var showEditFor by remember { mutableStateOf<String?>(null) }
    var showNewGameConfirm by remember { mutableStateOf(false) }
    var loanDialog by remember { mutableStateOf<LoanDialog?>(null) }
    var showTransfer by remember { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var assetDialog by remember { mutableStateOf<AssetDialogTarget?>(null) }

    val selected = accounts.firstOrNull { it.id == selectedId }

    CompositionLocalProvider(LocalCurrency provides settings.currencySymbol) {
        if (showSettings) {
            BackHandler { showSettings = false }
            SettingsScreen(
                settings = settings,
                onBack = { showSettings = false },
                onSelectCurrency = { symbol -> store.setCurrency(symbol) },
                onAddQuickAmount = { amount -> store.addQuickAmount(amount) },
                onRemoveQuickAmount = { amount -> store.removeQuickAmount(amount) }
            )
        } else if (selected != null) {
            BackHandler { selectedId = null }
            AccountDetailScreen(
                account = selected,
                otherAccounts = accounts.filterNot { it.id == selected.id },
                quickAmounts = settings.quickAmounts,
                onBack = { selectedId = null },
                onEdit = { showEditFor = selected.id },
                onAdjust = { amount ->
                    store.adjust(selected.id, amount, if (amount >= 0) "Added" else "Removed")
                },
                onTransfer = { counterpartyId, amount, isIncoming ->
                    if (isIncoming) {
                        store.transfer(fromId = counterpartyId, toId = selected.id, amount = amount)
                    } else {
                        store.transfer(fromId = selected.id, toId = counterpartyId, amount = amount)
                    }
                },
                onUndo = { transactionId -> store.undoTransaction(selected.id, transactionId) },
                onBorrow = { loanDialog = LoanDialog.Borrow },
                onRepay = { loanDialog = LoanDialog.Repay },
                onAddAsset = { assetDialog = AssetDialogTarget.New },
                onEditAsset = { assetId -> assetDialog = AssetDialogTarget.Existing(assetId) },
                onChangeHouses = { assetId, delta -> store.changeHouses(selected.id, assetId, delta) }
            )
        } else {
            AccountListScreen(
                accounts = accounts,
                onOpenAccount = { selectedId = it },
                onAddPlayer = { showAddPlayer = true },
                onTransfer = { showTransfer = true },
                onOpenSettings = { showSettings = true },
                onNewGame = { showNewGameConfirm = true }
            )
        }

        if (showTransfer) {
            TransferDialog(
                accounts = accounts,
                quickAmounts = settings.quickAmounts,
                onDismiss = { showTransfer = false },
                onTransfer = { fromId, toId, amount -> store.transfer(fromId, toId, amount) }
            )
        }

        if (showAddPlayer) {
            val used = accounts.map { it.colorArgb }.toSet()
            AddPlayerDialog(
                suggestedColor = PlayerColors.firstOrNull { it !in used } ?: PlayerColors.first(),
                onDismiss = { showAddPlayer = false },
                onConfirm = { name, color, startingCash, withdrawFromBank ->
                    store.addPlayer(name, color, startingCash, withdrawFromBank)
                    showAddPlayer = false
                }
            )
        }

        val editing = accounts.firstOrNull { it.id == showEditFor }
        if (editing != null) {
            EditAccountDialog(
                account = editing,
                onDismiss = { showEditFor = null },
                onConfirm = { name, color ->
                    store.editAccount(editing.id, name, color)
                    showEditFor = null
                },
                onDelete = if (editing.isBank) {
                    null
                } else {
                    {
                        store.removePlayer(editing.id)
                        showEditFor = null
                        selectedId = null
                    }
                }
            )
        }

        if (selected != null && loanDialog != null) {
            val borrowing = loanDialog == LoanDialog.Borrow
            AmountDialog(
                title = if (borrowing) "Take a loan" else "Repay the bank",
                label = "Amount",
                confirmLabel = if (borrowing) "Borrow" else "Repay",
                supportingText = if (borrowing) {
                    "The bank hands over the cash and the same amount is added to what ${selected.name} owes."
                } else {
                    "Outstanding loan: ${money(selected.loan)}"
                },
                initialAmount = if (borrowing) 0L else selected.loan,
                onDismiss = { loanDialog = null },
                onConfirm = { amount ->
                    if (borrowing) store.borrowFromBank(selected.id, amount) else store.repayLoan(selected.id, amount)
                    loanDialog = null
                }
            )
        }

        val assetTarget = assetDialog
        if (selected != null && assetTarget != null) {
            val existing = (assetTarget as? AssetDialogTarget.Existing)
                ?.let { target -> selected.assets.firstOrNull { it.id == target.assetId } }
            if (assetTarget is AssetDialogTarget.New || existing != null) {
                AssetDialog(
                    existing = existing,
                    onDismiss = { assetDialog = null },
                    onConfirm = { name, houses ->
                        if (existing == null) {
                            store.addAsset(selected.id, name, houses)
                        } else {
                            store.editAsset(selected.id, existing.id, name, houses)
                        }
                        assetDialog = null
                    },
                    onDelete = existing?.let {
                        {
                            store.removeAsset(selected.id, it.id)
                            assetDialog = null
                        }
                    }
                )
            }
        }

        if (showNewGameConfirm) {
            ConfirmDialog(
                title = "Start a new game?",
                message = "This removes every player and resets the bank. It cannot be undone.",
                confirmLabel = "New game",
                onDismiss = { showNewGameConfirm = false },
                onConfirm = {
                    store.newGame()
                    selectedId = null
                    showNewGameConfirm = false
                }
            )
        }
    }
}

private enum class LoanDialog { Borrow, Repay }

private sealed interface AssetDialogTarget {
    data object New : AssetDialogTarget
    data class Existing(val assetId: String) : AssetDialogTarget
}
