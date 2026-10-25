package com.monopy.cash.data

import com.monopy.cash.model.Account
import com.monopy.cash.model.BAIL
import com.monopy.cash.model.BoardData
import com.monopy.cash.model.BoardState
import com.monopy.cash.model.ChanceDeck
import com.monopy.cash.model.ChanceEffect
import com.monopy.cash.model.SALARY
import com.monopy.cash.model.Tile
import com.monopy.cash.model.TileType
import com.monopy.cash.model.Transaction
import kotlin.random.Random

/** Cash and board together — one turn of play takes a snapshot and returns the next one. */
data class GameSnapshot(val accounts: List<Account>, val board: BoardState)

/**
 * The rules. Every action is a pure function of the snapshot, which keeps the host and
 * the devices joined to it in step: the host runs the rule, then hands out the result.
 */
object BoardEngine {

    fun start(snapshot: GameSnapshot): GameSnapshot {
        val players = snapshot.accounts.filterNot { it.isBank }.map { it.id }
        if (players.isEmpty()) return snapshot
        return snapshot.copy(
            board = BoardState(
                started = true,
                order = players,
                turn = 0,
                positions = players.associateWith { 0 },
                log = listOf("खेळ सुरू · Game on")
            )
        )
    }

    fun stop(snapshot: GameSnapshot): GameSnapshot = snapshot.copy(board = BoardState())

    fun roll(snapshot: GameSnapshot, random: Random = Random.Default): GameSnapshot {
        val board = snapshot.board
        val playerId = board.currentPlayerId ?: return snapshot
        if (!board.started || board.rolled || board.awaitingBuy != null) return snapshot

        val first = random.nextInt(1, 7)
        val second = random.nextInt(1, 7)
        val doubles = first == second
        val rolled = snapshot.copy(
            board = board.copy(dice = listOf(first, second), lastCard = null)
        )
        val name = rolled.name(playerId)

        if (board.isJailed(playerId)) {
            return if (doubles) {
                rolled
                    .withBoard { it.copy(jailed = it.jailed - playerId, rolled = true) }
                    .log("$name ने दुहेरी टाकून तुरुंगातून सुटका केली · $name rolls doubles and leaves jail")
                    .move(playerId, first + second)
            } else {
                val left = (board.jailed[playerId] ?: 1) - 1
                rolled
                    .withBoard { state ->
                        state.copy(
                            jailed = if (left > 0) state.jailed + (playerId to left) else state.jailed - playerId,
                            rolled = true
                        )
                    }
                    .log(
                        if (left > 0) {
                            "$name तुरुंगातच · $name stays in jail ($left to go)"
                        } else {
                            "$name ची तुरुंगातून सुटका · $name is out of jail next turn"
                        }
                    )
            }
        }

        val runs = if (doubles) board.doublesRun + 1 else 0
        if (runs == 3) {
            return rolled
                .withBoard { it.copy(doublesRun = 0, rolled = true) }
                .log("$name ने तीनदा दुहेरी टाकले · three doubles in a row")
                .sendToJail(playerId)
        }

        return rolled
            .withBoard { it.copy(doublesRun = runs, rolled = !doubles) }
            .log("$name ने $first + $second टाकले · $name rolls ${first + second}")
            .move(playerId, first + second)
    }

    fun buy(snapshot: GameSnapshot): GameSnapshot {
        val index = snapshot.board.awaitingBuy ?: return snapshot
        val playerId = snapshot.board.currentPlayerId ?: return snapshot
        val tile = BoardData.tile(index)
        val name = snapshot.name(playerId)
        return snapshot
            .pay(playerId, snapshot.bankId(), tile.price, "${tile.latin} खरेदी · bought ${tile.latin}")
            .withBoard { it.copy(owners = it.owners + (index to playerId), awaitingBuy = null) }
            .log("$name ने ${tile.name} घेतले · $name buys ${tile.latin}")
    }

    fun declineBuy(snapshot: GameSnapshot): GameSnapshot =
        snapshot.withBoard { it.copy(awaitingBuy = null) }

    fun dismissCard(snapshot: GameSnapshot): GameSnapshot =
        snapshot.withBoard { it.copy(lastCard = null) }

    fun payBail(snapshot: GameSnapshot): GameSnapshot {
        val playerId = snapshot.board.currentPlayerId ?: return snapshot
        if (!snapshot.board.isJailed(playerId)) return snapshot
        val name = snapshot.name(playerId)
        return snapshot
            .pay(playerId, snapshot.bankId(), BAIL, "जामीन · bail")
            .withBoard { it.copy(jailed = it.jailed - playerId) }
            .log("$name ने जामीन भरला · $name pays bail")
    }

    fun endTurn(snapshot: GameSnapshot): GameSnapshot {
        val board = snapshot.board
        if (!board.started || board.order.isEmpty()) return snapshot
        return snapshot.withBoard {
            it.copy(
                turn = (it.turn + 1) % it.order.size,
                rolled = false,
                doublesRun = 0,
                awaitingBuy = null,
                lastCard = null
            )
        }
    }

    /** Puts up or sells back a house. [delta] is +1 or -1. */
    fun build(snapshot: GameSnapshot, tileIndex: Int, delta: Int): GameSnapshot {
        val tile = BoardData.tile(tileIndex)
        if (tile.type != TileType.CITY) return snapshot
        val ownerId = snapshot.board.ownerOf(tileIndex) ?: return snapshot
        if (!snapshot.board.ownsWholeGroup(ownerId, tile.group)) return snapshot

        val current = snapshot.board.housesOn(tileIndex)
        val next = (current + delta).coerceIn(0, 5)
        if (next == current) return snapshot
        val name = snapshot.name(ownerId)

        return if (delta > 0) {
            snapshot
                .pay(ownerId, snapshot.bankId(), tile.houseCost, "${tile.latin} बांधकाम · house on ${tile.latin}")
                .withBoard { it.copy(houses = it.houses + (tileIndex to next)) }
                .log("$name ने ${tile.name} वर घर बांधले · $name builds on ${tile.latin}")
        } else {
            snapshot
                .pay(snapshot.bankId(), ownerId, tile.houseCost / 2, "${tile.latin} घर विक्री · sold house")
                .withBoard { it.copy(houses = it.houses + (tileIndex to next)) }
                .log("$name ने ${tile.name} वरचे घर विकले · $name sells a house")
        }
    }

    /** Rent due on [tile], given who owns it and what the dice showed. */
    fun rentFor(board: BoardState, tile: Tile, ownerId: String): Long {
        val diceTotal = board.dice.sum().coerceAtLeast(2)
        return when (tile.type) {
            TileType.CITY -> {
                val houses = board.housesOn(tile.index)
                val base = tile.rent.getOrElse(houses) { tile.rent.lastOrNull() ?: 0L }
                if (houses == 0 && board.ownsWholeGroup(ownerId, tile.group)) base * 2 else base
            }

            TileType.TRANSPORT -> {
                val owned = board.ownedInGroup(ownerId, tile.group)
                tile.rent.getOrElse(owned - 1) { tile.rent.firstOrNull() ?: 0L }
            }

            TileType.UTILITY -> {
                val owned = board.ownedInGroup(ownerId, tile.group)
                diceTotal * if (owned > 1) 500L else 200L
            }

            TileType.RESORT -> {
                val owned = board.ownedInGroup(ownerId, tile.group)
                tile.rent.getOrElse(owned - 1) { tile.rent.firstOrNull() ?: 0L }
            }

            else -> 0
        }
    }

    // ---- internals -------------------------------------------------------------

    private fun GameSnapshot.move(
        playerId: String,
        steps: Int,
        collectSalary: Boolean = true,
        depth: Int = 0
    ): GameSnapshot {
        val from = board.positionOf(playerId)
        val to = ((from + steps) % BoardData.size + BoardData.size) % BoardData.size
        val passedGo = steps > 0 && to < from
        val moved = withBoard { it.copy(positions = it.positions + (playerId to to)) }
        val paid = if (passedGo && collectSalary) {
            moved
                .pay(bankId(), playerId, SALARY, "पगार · salary")
                .log("${name(playerId)} ला पगार · ${name(playerId)} collects ${SALARY}")
        } else {
            moved
        }
        return paid.resolveLanding(playerId, to, depth)
    }

    private fun GameSnapshot.jumpTo(
        playerId: String,
        index: Int,
        collectSalary: Boolean,
        depth: Int
    ): GameSnapshot {
        val from = board.positionOf(playerId)
        val steps = ((index - from) % BoardData.size + BoardData.size) % BoardData.size
        return move(playerId, if (steps == 0) BoardData.size else steps, collectSalary, depth)
    }

    private fun GameSnapshot.resolveLanding(playerId: String, index: Int, depth: Int): GameSnapshot {
        val tile = BoardData.tile(index)
        val playerName = name(playerId)

        return when {
            tile.isOwnable -> {
                val ownerId = board.ownerOf(index)
                when {
                    ownerId == null -> withBoard { it.copy(awaitingBuy = index) }
                    ownerId == playerId -> this
                    else -> {
                        val rent = rentFor(board, tile, ownerId)
                        pay(playerId, ownerId, rent, "${tile.latin} भाडे · rent")
                            .log("$playerName ने ${name(ownerId)} ला ${tile.name} चे भाडे दिले · pays rent $rent")
                    }
                }
            }

            tile.type == TileType.TAX ->
                pay(playerId, bankId(), tile.price, "${tile.latin} · tax")
                    .log("$playerName ने ${tile.name} भरला · $playerName pays ${tile.price} tax")

            tile.type == TileType.GO_TO_JAIL -> sendToJail(playerId)

            tile.type == TileType.CHANCE -> drawChance(playerId, depth)

            else -> this
        }
    }

    private fun GameSnapshot.drawChance(playerId: String, depth: Int): GameSnapshot {
        val card = ChanceDeck.cards.random()
        val shown = withBoard { it.copy(lastCard = card.id) }.log("${name(playerId)}: ${card.latin}")
        val others = accounts.filterNot { it.isBank || it.id == playerId }.map { it.id }

        return when (val effect = card.effect) {
            is ChanceEffect.Collect -> shown.pay(bankId(), playerId, effect.amount, card.latin)
            is ChanceEffect.Pay -> shown.pay(playerId, bankId(), effect.amount, card.latin)
            is ChanceEffect.GoToJail -> shown.sendToJail(playerId)

            is ChanceEffect.MoveTo ->
                if (depth >= 2) shown else shown.jumpTo(playerId, effect.index, effect.collectSalary, depth + 1)

            is ChanceEffect.MoveBack ->
                if (depth >= 2) {
                    shown
                } else {
                    shown.move(playerId, -effect.steps, collectSalary = false, depth = depth + 1)
                }

            is ChanceEffect.CollectFromEach ->
                others.fold(shown) { acc, other -> acc.pay(other, playerId, effect.amount, card.latin) }

            is ChanceEffect.PayEach ->
                others.fold(shown) { acc, other -> acc.pay(playerId, other, effect.amount, card.latin) }

            is ChanceEffect.PayPerHouse -> {
                val houses = board.houses.filterKeys { board.ownerOf(it) == playerId }.values.sum()
                shown.pay(playerId, bankId(), effect.perHouse * houses, card.latin)
            }
        }
    }

    private fun GameSnapshot.sendToJail(playerId: String): GameSnapshot = withBoard {
        it.copy(
            positions = it.positions + (playerId to 10),
            jailed = it.jailed + (playerId to 3),
            doublesRun = 0,
            rolled = true,
            awaitingBuy = null
        )
    }.log("${name(playerId)} तुरुंगात · ${name(playerId)} goes to jail")

    private fun GameSnapshot.withBoard(transform: (BoardState) -> BoardState) =
        copy(board = transform(board))

    private fun GameSnapshot.log(line: String): GameSnapshot =
        withBoard { it.copy(log = (it.log + line).takeLast(BoardState.MAX_LOG)) }

    private fun GameSnapshot.bankId(): String =
        accounts.firstOrNull { it.isBank }?.id ?: Account.BANK_ID

    private fun GameSnapshot.name(id: String): String =
        accounts.firstOrNull { it.id == id }?.name ?: "?"

    /** Moves cash between two accounts, recording it on both sides. */
    private fun GameSnapshot.pay(fromId: String, toId: String, amount: Long, note: String): GameSnapshot {
        if (amount <= 0 || fromId == toId) return this
        val fromName = name(fromId)
        val toName = name(toId)
        return copy(
            accounts = accounts.map { account ->
                when (account.id) {
                    fromId -> account.record(-amount, "$note → $toName")
                    toId -> account.record(amount, "$note ← $fromName")
                    else -> account
                }
            }
        )
    }

    private fun Account.record(amount: Long, note: String) = copy(
        balance = balance + amount,
        transactions = transactions + Transaction(amount = amount, note = note)
    )
}
