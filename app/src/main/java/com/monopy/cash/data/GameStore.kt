package com.monopy.cash.data

import android.content.Context
import com.monopy.cash.model.Account
import com.monopy.cash.model.Asset
import com.monopy.cash.model.DEFAULT_BANK_CASH
import com.monopy.cash.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/**
 * Holds every account for the current game and persists it to shared preferences
 * so a game survives leaving the app.
 */
class GameStore private constructor(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _accounts = MutableStateFlow(load())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    fun account(id: String?): Account? = _accounts.value.firstOrNull { it.id == id }

    fun addPlayer(name: String, colorArgb: Int, startingCash: Long, withdrawFromBank: Boolean) {
        val player = Account(
            name = name.ifBlank { "Player ${_accounts.value.size}" },
            colorArgb = colorArgb,
            balance = startingCash,
            transactions = if (startingCash != 0L) {
                listOf(Transaction(amount = startingCash, note = "Starting cash"))
            } else {
                emptyList()
            }
        )
        update { accounts ->
            val withPlayer = accounts + player
            if (withdrawFromBank && startingCash != 0L) {
                withPlayer.map { account ->
                    if (account.isBank) account.applyChange(-startingCash, "Starting cash to ${player.name}") else account
                }
            } else {
                withPlayer
            }
        }
    }

    fun editAccount(id: String, name: String, colorArgb: Int) {
        update { accounts ->
            accounts.map { account ->
                if (account.id == id) account.copy(name = name.ifBlank { account.name }, colorArgb = colorArgb) else account
            }
        }
    }

    fun removePlayer(id: String) {
        update { accounts -> accounts.filterNot { it.id == id && !it.isBank } }
    }

    /** Adds (positive) or removes (negative) cash on a single account. */
    fun adjust(id: String, amount: Long, note: String) {
        if (amount == 0L) return
        update { accounts ->
            accounts.map { account -> if (account.id == id) account.applyChange(amount, note) else account }
        }
    }

    /** Moves [amount] of cash from one account to another, recording both sides. */
    fun transfer(fromId: String, toId: String, amount: Long) {
        if (amount <= 0L || fromId == toId) return
        val from = account(fromId) ?: return
        val to = account(toId) ?: return
        update { accounts ->
            accounts.map { account ->
                when (account.id) {
                    fromId -> account.applyChange(-amount, "To ${to.name}")
                    toId -> account.applyChange(amount, "From ${from.name}")
                    else -> account
                }
            }
        }
    }

    /**
     * Lends [amount] from the bank to a player: cash goes up, and the same amount is
     * added to the player's loan counter, which is tracked apart from their balance.
     */
    fun borrowFromBank(playerId: String, amount: Long) {
        if (amount <= 0L) return
        val player = account(playerId) ?: return
        if (player.isBank) return
        update { accounts ->
            accounts.map { account ->
                when {
                    account.id == playerId ->
                        account.applyChange(amount, "Loan from bank", loanDelta = amount)
                    account.isBank -> account.applyChange(-amount, "Loan to ${player.name}")
                    else -> account
                }
            }
        }
    }

    /** Pays cash back to the bank, clearing that much of the player's loan. */
    fun repayLoan(playerId: String, amount: Long) {
        val player = account(playerId) ?: return
        if (player.isBank) return
        val repayment = amount.coerceAtMost(player.loan)
        if (repayment <= 0L) return
        update { accounts ->
            accounts.map { account ->
                when {
                    account.id == playerId ->
                        account.applyChange(-repayment, "Loan repayment", loanDelta = -repayment)
                    account.isBank -> account.applyChange(repayment, "Repayment from ${player.name}")
                    else -> account
                }
            }
        }
    }

    fun addAsset(playerId: String, name: String, houses: Int) {
        if (name.isBlank()) return
        val asset = Asset(name = name.trim(), houses = houses.coerceIn(0, Asset.MAX_HOUSES))
        updateAccount(playerId) { it.copy(assets = it.assets + asset) }
    }

    fun editAsset(playerId: String, assetId: String, name: String, houses: Int) {
        if (name.isBlank()) return
        updateAccount(playerId) { account ->
            account.copy(
                assets = account.assets.map { asset ->
                    if (asset.id == assetId) {
                        asset.copy(name = name.trim(), houses = houses.coerceIn(0, Asset.MAX_HOUSES))
                    } else {
                        asset
                    }
                }
            )
        }
    }

    /** Adds or removes a single house on an asset, clamped to 0..[Asset.MAX_HOUSES]. */
    fun changeHouses(playerId: String, assetId: String, delta: Int) {
        updateAccount(playerId) { account ->
            account.copy(
                assets = account.assets.map { asset ->
                    if (asset.id == assetId) {
                        asset.copy(houses = (asset.houses + delta).coerceIn(0, Asset.MAX_HOUSES))
                    } else {
                        asset
                    }
                }
            )
        }
    }

    fun removeAsset(playerId: String, assetId: String) {
        updateAccount(playerId) { account -> account.copy(assets = account.assets.filterNot { it.id == assetId }) }
    }

    /** Reverses a transaction and drops it from the history. */
    fun undoTransaction(accountId: String, transactionId: String) {
        update { accounts ->
            accounts.map { account ->
                if (account.id != accountId) {
                    account
                } else {
                    val transaction = account.transactions.firstOrNull { it.id == transactionId } ?: return@map account
                    account.copy(
                        balance = account.balance - transaction.amount,
                        loan = account.loan - transaction.loanDelta,
                        transactions = account.transactions.filterNot { it.id == transactionId }
                    )
                }
            }
        }
    }

    /** Clears every account and starts over with a fresh bank. */
    fun newGame() {
        update { defaultAccounts() }
    }

    private fun updateAccount(id: String, transform: (Account) -> Account) {
        update { accounts -> accounts.map { if (it.id == id) transform(it) else it } }
    }

    private fun Account.applyChange(amount: Long, note: String, loanDelta: Long = 0) = copy(
        balance = balance + amount,
        loan = loan + loanDelta,
        transactions = transactions + Transaction(amount = amount, note = note, loanDelta = loanDelta)
    )

    private fun update(transform: (List<Account>) -> List<Account>) {
        val updated = transform(_accounts.value)
        _accounts.value = updated
        save(updated)
    }

    private fun save(accounts: List<Account>) {
        val array = JSONArray()
        accounts.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_ACCOUNTS, array.toString()).apply()
    }

    private fun load(): List<Account> {
        val raw = prefs.getString(KEY_ACCOUNTS, null) ?: return defaultAccounts()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { Account.fromJson(array.getJSONObject(it)) }
        }.getOrNull()?.takeIf { list -> list.any { it.isBank } } ?: defaultAccounts()
    }

    private fun defaultAccounts() = listOf(
        Account(
            id = Account.BANK_ID,
            name = "Bank",
            colorArgb = 0xFF0F6B3F.toInt(),
            balance = DEFAULT_BANK_CASH,
            isBank = true
        )
    )

    companion object {
        private const val PREFS_NAME = "monopy_cash"
        private const val KEY_ACCOUNTS = "accounts"

        @Volatile
        private var instance: GameStore? = null

        fun get(context: Context): GameStore = instance ?: synchronized(this) {
            instance ?: GameStore(context.applicationContext).also { instance = it }
        }
    }
}
