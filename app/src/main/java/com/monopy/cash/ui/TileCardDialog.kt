package com.monopy.cash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.monopy.cash.model.Account
import com.monopy.cash.model.BoardState
import com.monopy.cash.model.Tile
import com.monopy.cash.model.TileType

/** The title deed, the way the printed card reads: price, rent ladder and what it costs to build. */
@Composable
fun TileCardDialog(
    tile: Tile,
    board: BoardState,
    accounts: List<Account>,
    canBuild: Boolean,
    onBuild: (delta: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val owner = board.ownerOf(tile.index)?.let { id -> accounts.firstOrNull { it.id == id } }
    val houses = board.housesOn(tile.index)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                if (tile.colorArgb != 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .background(tile.colorArgb.toComposeColor())
                    )
                }
                Text(tile.name, fontWeight = FontWeight.Bold)
                Text(
                    text = tile.latin,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                if (tile.isOwnable) {
                    CardRow("किंमत · Price", money(tile.price))
                }

                when (tile.type) {
                    TileType.CITY -> {
                        CardRow("भाडे · Rent", money(tile.rent.firstOrNull() ?: 0L))
                        tile.rent.drop(1).forEachIndexed { step, amount ->
                            val label = if (step == 4) "हॉटेलसह · with hotel" else "${step + 1} घरे · ${step + 1} houses"
                            CardRow(label, money(amount))
                        }
                        CardRow("घराची किंमत · House cost", money(tile.houseCost))
                        Text(
                            text = "संपूर्ण रंग असल्यास कोरे भाडे दुप्पट · Rent doubles on an undeveloped full set.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    TileType.TRANSPORT -> tile.rent.forEachIndexed { owned, amount ->
                        CardRow("${owned + 1} मालकीचे · ${owned + 1} owned", money(amount))
                    }

                    TileType.UTILITY -> {
                        CardRow("एक असल्यास · One owned", "२०० × फासे · 200 × dice")
                        CardRow("दोन्ही असल्यास · Both owned", "५०० × फासे · 500 × dice")
                    }

                    TileType.RESORT -> tile.rent.forEachIndexed { owned, amount ->
                        CardRow("${owned + 1} मालकीचे · ${owned + 1} owned", money(amount))
                    }

                    TileType.TAX -> CardRow("कर · Tax", money(tile.price))

                    else -> Text(
                        text = tile.latin,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (tile.isOwnable) {
                    CardRow(
                        label = "मालक · Owner",
                        value = owner?.name ?: "कोणी नाही · nobody"
                    )
                }

                if (tile.type == TileType.CITY && owner != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Text(
                            text = if (houses >= 5) "हॉटेल · Hotel" else "$houses घरे · $houses houses",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (canBuild) {
                            IconButton(onClick = { onBuild(-1) }, enabled = houses > 0) {
                                Icon(Icons.Filled.Remove, contentDescription = "Sell a house")
                            }
                            IconButton(onClick = { onBuild(1) }, enabled = houses < 5) {
                                Icon(Icons.Filled.Add, contentDescription = "Build a house")
                            }
                        }
                    }
                    if (!canBuild && board.ownsWholeGroup(owner.id, tile.group)) {
                        Text(
                            text = "मालकच बांधू शकतो · Only the owner can build here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("बंद · Close") } }
    )
}

@Composable
private fun CardRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
