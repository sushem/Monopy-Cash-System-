package com.monopy.cash.net

import com.monopy.cash.data.GameStore
import com.monopy.cash.model.DEFAULT_STARTING_CASH
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

/** Where this device sits in a shared game. */
sealed interface NetRole {
    data object Offline : NetRole
    data class Hosting(val address: String, val players: Int) : NetRole
    data class Joined(val address: String, val connected: Boolean, val error: String? = null) : NetRole
}

/**
 * Play on one table over the local network: one device hosts and the rest join it by
 * address. The host owns the rules — it applies every action and hands the whole game
 * back out, so no device can drift from the others.
 */
class LanSession(private val store: GameStore) {

    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _role = MutableStateFlow<NetRole>(NetRole.Offline)
    val role: StateFlow<NetRole> = _role.asStateFlow()

    /** On a joined device, the player this device plays as. */
    private val _myPlayerId = MutableStateFlow<String?>(null)
    val myPlayerId: StateFlow<String?> = _myPlayerId.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var clientWriter: BufferedWriter? = null
    private val links = CopyOnWriteArrayList<ClientLink>()
    private var loop: Job? = null

    val isHost: Boolean get() = _role.value is NetRole.Hosting
    val isClient: Boolean get() = _role.value is NetRole.Joined

    // ---- hosting ---------------------------------------------------------------

    fun host() {
        stop()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        store.onStateChanged = { broadcastState() }
        _role.value = NetRole.Hosting(localAddress(), 0)
        loop = scope.launch {
            runCatching {
                val server = ServerSocket(PORT)
                serverSocket = server
                while (!server.isClosed) {
                    val socket = server.accept()
                    val link = ClientLink(socket)
                    links += link
                    updateHostRole()
                    scope.launch { readFrom(link) }
                }
            }
        }
    }

    private suspend fun readFrom(link: ClientLink) {
        runCatching {
            val reader = BufferedReader(InputStreamReader(link.socket.getInputStream()))
            while (true) {
                val line = reader.readLine() ?: break
                handleFromClient(link, JSONObject(line))
            }
        }
        links.remove(link)
        link.close()
        updateHostRole()
    }

    private fun handleFromClient(link: ClientLink, message: JSONObject) {
        when (message.optString("t")) {
            "hello" -> {
                val name = message.optString("name").ifBlank { "Player" }
                val color = message.optInt("color")
                val playerId = store.claimPlayer(name, color, DEFAULT_STARTING_CASH)
                link.playerId = playerId
                link.send(JSONObject().put("t", "you").put("id", playerId).toString())
                updateHostRole()
                broadcastState()
            }

            "action" -> {
                applyAction(message, link.playerId)
                // applyAction mutates the store, which broadcasts through onStateChanged.
            }
        }
    }

    private fun updateHostRole() {
        val current = _role.value
        if (current is NetRole.Hosting) {
            _role.value = current.copy(players = links.size)
        }
    }

    private fun broadcastState() {
        if (!isHost) return
        val line = JSONObject().put("t", "state").put("payload", store.exportState()).toString()
        links.forEach { it.send(line) }
    }

    // ---- joining ---------------------------------------------------------------

    fun join(address: String, playerName: String, color: Int) {
        stop()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        _role.value = NetRole.Joined(address, connected = false)
        loop = scope.launch {
            val result = runCatching {
                val socket = Socket(InetAddress.getByName(address.trim()), PORT)
                clientSocket = socket
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                clientWriter = writer
                _role.value = NetRole.Joined(address, connected = true)

                writer.write(JSONObject().put("t", "hello").put("name", playerName).put("color", color).toString())
                writer.newLine()
                writer.flush()

                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                while (true) {
                    val line = reader.readLine() ?: break
                    handleFromHost(JSONObject(line))
                }
            }
            val failure = result.exceptionOrNull()
            _role.value = NetRole.Joined(
                address = address,
                connected = false,
                error = failure?.let { it.message ?: it.javaClass.simpleName }
            )
        }
    }

    private fun handleFromHost(message: JSONObject) {
        when (message.optString("t")) {
            "state" -> message.optJSONObject("payload")?.let { store.importState(it) }
            "you" -> _myPlayerId.value = message.optString("id").takeIf { it.isNotBlank() }
        }
    }

    /** Client side: ask the host to do something. */
    fun send(action: JSONObject) {
        val writer = clientWriter ?: return
        scope.launch {
            runCatching {
                synchronized(writer) {
                    writer.write(action.toString())
                    writer.newLine()
                    writer.flush()
                }
            }
        }
    }

    // ---- shared ----------------------------------------------------------------

    /** Runs an action against the rules. On the host this is where joined devices land. */
    fun applyAction(message: JSONObject, senderId: String?) {
        val board = store.board.value
        val current = board.currentPlayerId
        // A joined device may only play its own turn; the host plays for anyone.
        val mayPlay = senderId == null || senderId == current

        when (message.optString("a")) {
            "roll" -> if (mayPlay) store.rollDice()
            "buy" -> if (mayPlay) store.buyTile()
            "decline" -> if (mayPlay) store.declineTile()
            "card" -> store.dismissCard()
            "bail" -> if (mayPlay) store.payBail()
            "end" -> if (mayPlay) store.endTurn()
            "start" -> store.startBoardGame()
            "build" -> {
                val tile = message.optInt("tile", -1)
                if (tile >= 0 && (senderId == null || board.ownerOf(tile) == senderId)) {
                    store.buildHouse(tile, message.optInt("delta"))
                }
            }

            "adjust" -> store.adjust(
                message.optString("id"),
                message.optLong("amount"),
                message.optString("note").ifBlank { "Adjusted" }
            )

            "transfer" -> store.transfer(
                message.optString("from"),
                message.optString("to"),
                message.optLong("amount")
            )

            "loan" -> store.borrowFromBank(message.optString("id"), message.optLong("amount"))
            "repay" -> store.repayLoan(message.optString("id"), message.optLong("amount"))
        }
    }

    fun stop() {
        store.onStateChanged = null
        runCatching { serverSocket?.close() }
        runCatching { clientSocket?.close() }
        links.forEach { it.close() }
        links.clear()
        serverSocket = null
        clientSocket = null
        clientWriter = null
        _myPlayerId.value = null
        loop?.cancel()
        runCatching { scope.cancel() }
        _role.value = NetRole.Offline
    }

    private inner class ClientLink(val socket: Socket) {
        private val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
        var playerId: String? = null

        fun send(line: String) {
            runCatching {
                synchronized(writer) {
                    writer.write(line)
                    writer.newLine()
                    writer.flush()
                }
            }
        }

        fun close() {
            runCatching { socket.close() }
        }
    }

    companion object {
        const val PORT = 8742

        /** This device's address on the Wi-Fi, for the others to type in. */
        fun localAddress(): String = runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.contains('.') == true }
                ?.hostAddress
        }.getOrNull() ?: "unavailable"
    }
}
