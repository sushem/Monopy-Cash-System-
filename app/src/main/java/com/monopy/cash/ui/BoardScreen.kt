package com.monopy.cash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.monopy.cash.model.Account
import com.monopy.cash.model.BAIL
import com.monopy.cash.model.BoardData
import com.monopy.cash.model.BoardState
import com.monopy.cash.model.ChanceDeck

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardScreen(
    accounts: List<Account>,
    board: BoardState,
    /** The player this device plays as, or null when it plays for everyone. */
    myPlayerId: String?,
    netLabel: String,
    onOpenMultiplayer: () -> Unit,
    onStart: () -> Unit,
    onRoll: () -> Unit,
    onBuy: () -> Unit,
    onDecline: () -> Unit,
    onBail: () -> Unit,
    onEndTurn: () -> Unit,
    onBuild: (tileIndex: Int, delta: Int) -> Unit,
    onDismissCard: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    var openTile by remember { mutableStateOf<Int?>(null) }

    val players = accounts.filterNot { it.isBank }
    val current = board.currentPlayerId?.let { id -> accounts.firstOrNull { it.id == id } }
    val canAct = myPlayerId == null || myPlayerId == board.currentPlayerId
    val jailed = current != null && board.isJailed(current.id)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("फळा · Board") },
                actions = {
                    IconButton(onClick = onOpenMultiplayer) {
                        Icon(Icons.Filled.Wifi, contentDescription = "Multiplayer")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = bottomBar
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = netLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )

            BoardGrid(
                board = board,
                accounts = accounts,
                onTileClick = { openTile = it },
                centre = { BoardCentre(board = board, currentName = current?.name) }
            )

            if (!board.started) {
                Button(
                    onClick = onStart,
                    enabled = players.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Casino, contentDescription = null)
                    Text("  खेळ सुरू करा · Start game")
                }
                Text(
                    text = if (players.isEmpty()) {
                        "आधी खेळाडू जोडा · Add players on the Cash tab first."
                    } else {
                        "${players.size} खेळाडू तयार · ${players.size} players ready. Everyone starts on GO."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                TurnPanel(
                    board = board,
                    currentName = current?.name,
                    currentColor = current?.colorArgb,
                    canAct = canAct,
                    jailed = jailed,
                    onRoll = onRoll,
                    onBail = onBail,
                    onEndTurn = onEndTurn
                )
            }

            if (board.log.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "खेळाची नोंद · Table log",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        board.log.takeLast(12).asReversed().forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.height(12.dp))
        }
    }

    val tileIndex = openTile
    if (tileIndex != null) {
        val tile = BoardData.tile(tileIndex)
        val ownerId = board.ownerOf(tileIndex)
        TileCardDialog(
            tile = tile,
            board = board,
            accounts = accounts,
            canBuild = ownerId != null &&
                (myPlayerId == null || myPlayerId == ownerId) &&
                board.ownsWholeGroup(ownerId, tile.group),
            onBuild = { delta -> onBuild(tileIndex, delta) },
            onDismiss = { openTile = null }
        )
    }

    val buyIndex = board.awaitingBuy
    if (buyIndex != null && canAct) {
        val tile = BoardData.tile(buyIndex)
        val buyer = current
        val affordable = (buyer?.balance ?: 0L) >= tile.price
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDecline,
            title = { Text("${tile.name} घ्यायचे? · Buy ${tile.latin}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("किंमत · Price ${money(tile.price)}")
                    Text(
                        text = if (affordable) {
                            "${buyer?.name} कडे ${money(buyer?.balance ?: 0L)} आहेत."
                        } else {
                            "${buyer?.name} कडे पुरेसे पैसे नाहीत — घेतल्यास खाते उणे जाईल."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (affordable) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            },
            confirmButton = { TextButton(onClick = onBuy) { Text("घ्या · Buy") } },
            dismissButton = { TextButton(onClick = onDecline) { Text("नको · Pass") } }
        )
    }

    val cardId = board.lastCard
    if (cardId != null) {
        val card = ChanceDeck.card(cardId)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismissCard,
            title = { Text("सुवर्ण संधी · Golden chance") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(card.text, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = card.latin,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = { TextButton(onClick = onDismissCard) { Text("ठीक · OK") } }
        )
    }
}

@Composable
private fun BoardCentre(board: BoardState, currentName: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "नवा व्यापार",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        if (board.dice.size == 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DieFace(board.dice[0])
                DieFace(board.dice[1])
            }
        } else {
            Text(
                text = "फासे टाका · Roll the dice",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (board.started && currentName != null) {
            Text(
                text = "$currentName ची पाळी",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TurnPanel(
    board: BoardState,
    currentName: String?,
    currentColor: Int?,
    canAct: Boolean,
    jailed: Boolean,
    onRoll: () -> Unit,
    onBail: () -> Unit,
    onEndTurn: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = currentColor?.toComposeColor()?.copy(alpha = 0.18f)
                ?: MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = when {
                    !canAct -> "${currentName ?: "?"} खेळत आहे · waiting for ${currentName ?: "the other device"}"
                    jailed -> "$currentName तुरुंगात · in jail: roll doubles or pay bail"
                    board.rolled -> "$currentName ची पाळी संपली · turn done"
                    else -> "$currentName ची पाळी · your move"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onRoll,
                    enabled = canAct && !board.rolled && board.awaitingBuy == null,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Casino, contentDescription = null)
                    Text("  फासे · Roll")
                }
                OutlinedButton(
                    onClick = onEndTurn,
                    enabled = canAct && board.awaitingBuy == null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("पाळी संपली · End turn")
                }
            }

            if (jailed) {
                OutlinedButton(
                    onClick = onBail,
                    enabled = canAct,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("जामीन भरा ${money(BAIL)} · Pay bail")
                }
            }
        }
    }
}
