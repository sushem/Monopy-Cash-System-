package com.monopy.cash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monopy.cash.model.Account
import com.monopy.cash.model.BoardData
import com.monopy.cash.model.BoardState
import com.monopy.cash.model.Tile
import com.monopy.cash.model.TileType

/** The whole board: 40 squares around an 11x11 grid, with the tokens standing on them. */
@Composable
fun BoardGrid(
    board: BoardState,
    accounts: List<Account>,
    onTileClick: (Int) -> Unit,
    centre: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
    ) {
        val cell = maxWidth / 11
        val playersOn = accounts.filterNot { it.isBank }.groupBy { board.positionOf(it.id) }

        for (index in 0 until BoardData.size) {
            val (row, column) = BoardData.cell(index)
            TileView(
                tile = BoardData.tile(index),
                ownerColor = board.ownerOf(index)
                    ?.let { id -> accounts.firstOrNull { it.id == id }?.colorArgb?.toComposeColor() },
                houses = board.housesOn(index),
                tokens = playersOn[index].orEmpty().map { it.colorArgb.toComposeColor() },
                modifier = Modifier
                    .offset(x = cell * column, y = cell * row)
                    .size(cell)
                    .clickable { onTileClick(index) }
            )
        }

        Box(
            modifier = Modifier
                .offset(x = cell, y = cell)
                .size(cell * 9)
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            centre()
        }
    }
}

@Composable
private fun TileView(
    tile: Tile,
    ownerColor: Color?,
    houses: Int,
    tokens: List<Color>,
    modifier: Modifier = Modifier
) {
    val background = when (tile.type) {
        TileType.GO, TileType.JAIL, TileType.REST, TileType.GO_TO_JAIL ->
            MaterialTheme.colorScheme.primaryContainer

        TileType.CHANCE -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = modifier
            .padding(0.5.dp)
            .background(background)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (tile.colorArgb != 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .background(tile.colorArgb.toComposeColor())
                )
            }
            Text(
                text = tile.name,
                fontSize = 6.sp,
                lineHeight = 7.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 1.dp)
            )
            if (tile.price > 0 && tile.isOwnable) {
                Text(
                    text = tile.price.toString(),
                    fontSize = 5.sp,
                    lineHeight = 6.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (ownerColor != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(1.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(ownerColor)
            )
        }

        if (houses > 0) {
            Text(
                text = if (houses >= 5) "H" else houses.toString(),
                fontSize = 6.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(horizontal = 1.dp)
            )
        }

        if (tokens.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 1.dp)
            ) {
                tokens.take(6).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(0.5.dp, Color.White, CircleShape)
                    )
                }
            }
        }
    }
}

/** One die, drawn as pips. */
@Composable
fun DieFace(value: Int, size: Int = 44) {
    val pips = when (value) {
        1 -> setOf(4)
        2 -> setOf(0, 8)
        3 -> setOf(0, 4, 8)
        4 -> setOf(0, 2, 6, 8)
        5 -> setOf(0, 2, 4, 6, 8)
        else -> setOf(0, 2, 3, 5, 6, 8)
    }
    Column(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 6).dp))
            .background(Color.White)
            .border(1.dp, Color(0xFF9E9E9E), RoundedCornerShape((size / 6).dp))
            .padding(size.dp / 10),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        for (row in 0 until 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (column in 0 until 3) {
                    val filled = (row * 3 + column) in pips
                    Box(
                        modifier = Modifier
                            .size((size / 7).dp)
                            .clip(CircleShape)
                            .background(if (filled) Color(0xFF212121) else Color.Transparent)
                    )
                }
            }
        }
    }
}
