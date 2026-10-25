package com.monopy.cash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monopy.cash.data.GameStore
import com.monopy.cash.model.PlayerColors
import com.monopy.cash.net.LanSession
import com.monopy.cash.net.NetRole
import com.monopy.cash.ui.AccountDetailScreen
import com.monopy.cash.ui.AccountListScreen
import com.monopy.cash.ui.AddPlayerDialog
import com.monopy.cash.ui.AmountDialog
import com.monopy.cash.ui.AssetDialog
import com.monopy.cash.ui.BoardScreen
import com.monopy.cash.ui.ConfirmDialog
import com.monopy.cash.ui.EditAccountDialog
import com.monopy.cash.ui.LocalCurrency
import com.monopy.cash.ui.MultiplayerDialog
import com.monopy.cash.ui.SettingsScreen
import com.monopy.cash.ui.TransferDialog
import com.monopy.cash.ui.money
import com.monopy.cash.ui.theme.MonopyTheme
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private lateinit var lan: LanSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = GameStore.get(this)
        lan = LanSession(store)
        setContent {
            MonopyTheme {
                MonopyApp(store, lan)
            }
        }
    }

    override fun onDestroy() {
        lan.stop()
        super.onDestroy()
    }
}

@Composable
private fun MonopyApp(store: GameStore, lan: LanSession) {
    val accounts by store.accounts.collectAsStateWithLifecycle()
    val settings by store.settings.collectAsStateWithLifecycle()
    val board by store.board.collectAsStateWithLifecycle()
    val role by lan.role.collectAsStateWithLifecycle()
    val myPlayerId by lan.myPlayerId.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddPlayer by remember { mutableStateOf(false) }
    var showEditFor by remember { mutableStateOf<String?>(null) }
    var showNewGameConfirm by remember { mutableStateOf(false) }
    var loanDialog by remember { mutableStateOf<LoanDialog?>(null) }
    var assetDialog by remember { mutableStateOf<AssetDialogTarget?>(null) }
    var showTransfer by remember { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showMultiplayer by remember { mutableStateOf(false) }

    val selected = accounts.firstOrNull { it.id == selectedId }
    val isGuest = role is NetRole.Joined

    /** Routes a game action: a joined device asks the host, everyone else just plays it. */
    fun act(action: String, build: JSONObject.() -> Unit = {}) {
        val message = JSONObject().put("t", "action").put("a", action).apply(build)
        if (lan.isClient) lan.send(message) else lan.applyAction(message, null)
    }

    val netLabel = when (val current = role) {
        is NetRole.Hosting -> "यजमान · Hosting at ${current.address}:${LanSession.PORT} · ${current.players} joined"
        is NetRole.Joined ->
            if (current.connected) {
                "जोडलेले · Joined ${current.address}"
            } else {
                "जोडणी नाही · Not connected to ${current.address}"
            }

        NetRole.Offline -> "एकाच फोनवर · This device only — open the Wi-Fi icon to play together"
    }

    val bottomBar: @Composable () -> Unit = {
        NavigationBar {
            NavigationBarItem(
                selected = tab == 0,
                onClick = { tab = 0 },
                icon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null) },
                label = { Text("पैसे · Cash") }
            )
            NavigationBarItem(
                selected = tab == 1,
                onClick = { tab = 1 },
                icon = { Icon(Icons.Filled.Casino, contentDescription = null) },
                label = { Text("फळा · Board") }
            )
        }
    }

    CompositionLocalProvider(LocalCurrency provides settings.currencySymbol) {
        when {
            showSettings -> {
                BackHandler { showSettings = false }
                SettingsScreen(
                    settings = settings,
                    onBack = { showSettings = false },
                    onSelectCurrency = { symbol -> store.setCurrency(symbol) },
                    onAddQuickAmount = { amount -> store.addQuickAmount(amount) },
                    onRemoveQuickAmount = { amount -> store.removeQuickAmount(amount) }
                )
            }

            selected != null -> {
                BackHandler { selectedId = null }
                AccountDetailScreen(
                    account = selected,
                    otherAccounts = accounts.filterNot { it.id == selected.id },
                    quickAmounts = settings.quickAmounts,
                    isGuest = isGuest,
                    onBack = { selectedId = null },
                    onEdit = { showEditFor = selected.id },
                    onAdjust = { amount ->
                        act("adjust") {
                            put("id", selected.id)
                            put("amount", amount)
                            put("note", if (amount >= 0) "Added" else "Removed")
                        }
                    },
                    onTransfer = { counterpartyId, amount, isIncoming ->
                        act("transfer") {
                            put("from", if (isIncoming) counterpartyId else selected.id)
                            put("to", if (isIncoming) selected.id else counterpartyId)
                            put("amount", amount)
                        }
                    },
                    onUndo = { transactionId -> store.undoTransaction(selected.id, transactionId) },
                    onBorrow = { loanDialog = LoanDialog.Borrow },
                    onRepay = { loanDialog = LoanDialog.Repay },
                    onAddAsset = { assetDialog = AssetDialogTarget.New },
                    onEditAsset = { assetId -> assetDialog = AssetDialogTarget.Existing(assetId) },
                    onChangeHouses = { assetId, delta -> store.changeHouses(selected.id, assetId, delta) }
                )
            }

            tab == 1 -> BoardScreen(
                accounts = accounts,
                board = board,
                myPlayerId = myPlayerId,
                netLabel = netLabel,
                onOpenMultiplayer = { showMultiplayer = true },
                onStart = { act("start") },
                onRoll = { act("roll") },
                onBuy = { act("buy") },
                onDecline = { act("decline") },
                onBail = { act("bail") },
                onEndTurn = { act("end") },
                onBuild = { tileIndex, delta ->
                    act("build") {
                        put("tile", tileIndex)
                        put("delta", delta)
                    }
                },
                onDismissCard = { act("card") },
                bottomBar = bottomBar
            )

            else -> AccountListScreen(
                accounts = accounts,
                isGuest = isGuest,
                onOpenAccount = { selectedId = it },
                onAddPlayer = { showAddPlayer = true },
                onTransfer = { showTransfer = true },
                onOpenSettings = { showSettings = true },
                onOpenMultiplayer = { showMultiplayer = true },
                onNewGame = { showNewGameConfirm = true },
                bottomBar = bottomBar
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
                onDelete = if (editing.isBank || isGuest) {
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
                    act(if (borrowing) "loan" else "repay") {
                        put("id", selected.id)
                        put("amount", amount)
                    }
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

        if (showTransfer) {
            TransferDialog(
                accounts = accounts,
                quickAmounts = settings.quickAmounts,
                onDismiss = { showTransfer = false },
                onTransfer = { fromId, toId, amount ->
                    act("transfer") {
                        put("from", fromId)
                        put("to", toId)
                        put("amount", amount)
                    }
                }
            )
        }

        if (showMultiplayer) {
            MultiplayerDialog(
                role = role,
                suggestedName = accounts.firstOrNull { !it.isBank }?.name.orEmpty().ifBlank { "Player" },
                onHost = { lan.host() },
                onJoin = { address, name ->
                    lan.join(address, name, PlayerColors.random())
                    showMultiplayer = false
                },
                onStop = {
                    lan.stop()
                    showMultiplayer = false
                },
                onDismiss = { showMultiplayer = false }
            )
        }

        if (showNewGameConfirm) {
            ConfirmDialog(
                title = "Start a new game?",
                message = "This removes every player, clears the board and resets the bank. It cannot be undone.",
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
