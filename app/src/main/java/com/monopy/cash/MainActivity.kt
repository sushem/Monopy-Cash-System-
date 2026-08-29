package com.monopy.cash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
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
import com.monopy.cash.ui.ConfirmDialog
import com.monopy.cash.ui.EditAccountDialog
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

    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddPlayer by remember { mutableStateOf(false) }
    var showEditFor by remember { mutableStateOf<String?>(null) }
    var showNewGameConfirm by remember { mutableStateOf(false) }

    val selected = accounts.firstOrNull { it.id == selectedId }

    if (selected != null) {
        BackHandler { selectedId = null }
        AccountDetailScreen(
            account = selected,
            otherAccounts = accounts.filterNot { it.id == selected.id },
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
            onUndo = { transactionId -> store.undoTransaction(selected.id, transactionId) }
        )
    } else {
        AccountListScreen(
            accounts = accounts,
            onOpenAccount = { selectedId = it },
            onAddPlayer = { showAddPlayer = true },
            onNewGame = { showNewGameConfirm = true }
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
