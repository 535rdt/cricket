package com.cricket.scoreboard.network

import android.content.Context
import com.cricket.scoreboard.data.CricketStorage
import com.cricket.scoreboard.model.Match
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.*

data class BroadcastInfo(
    val matchCode: String,
    val localWebUrl: String,
    val isBroadcasting: Boolean
)

object LiveSharingManager {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var watcherJob: Job? = null

    var isBroadcasting: Boolean = false
        private set
    var currentMatchCode: String = ""
        private set
    var currentWebUrl: String = ""
        private set

    var isSpectating: Boolean = false
        private set
    var spectatingTarget: String = ""
        private set

    @Volatile
    private var cachedMatch: Match? = null
    @Volatile
    private var cachedMatchJson: String = "{}"

    private const val DEFAULT_PORT = 8080

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "127.0.0.1"
            val validAddrs = mutableListOf<String>()
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress ?: continue
                        if (!ip.startsWith("127.")) {
                            // Prioritize hotspot (192.168.43.x, 192.168.50.x) or wlan
                            if (intf.name.contains("ap", ignoreCase = true) || ip.startsWith("192.168.43.") || ip.startsWith("192.168.50.")) {
                                return ip
                            }
                            validAddrs.add(ip)
                        }
                    }
                }
            }
            if (validAddrs.isNotEmpty()) {
                return validAddrs.first()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "127.0.0.1"
    }

    fun startBroadcasting(context: Context, match: Match): BroadcastInfo {
        cachedMatch = match
        val storage = CricketStorage(context)
        cachedMatchJson = storage.matchToJsonString(match)

        if (currentMatchCode.isEmpty() || !isBroadcasting) {
            val shortId = match.id.replace("-", "").takeLast(4).uppercase()
            currentMatchCode = "CRIC-$shortId"
        }

        val ip = getLocalIpAddress()
        var port = DEFAULT_PORT

        // Stop existing server if running
        stopServer()

        try {
            serverSocket = ServerSocket(port)
        } catch (e: Exception) {
            try {
                port = 8081
                serverSocket = ServerSocket(port)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }

        currentWebUrl = "http://$ip:$port"
        isBroadcasting = true

        // Start local HTTP server loop
        serverJob = scope.launch {
            val ss = serverSocket ?: return@launch
            while (isActive && !ss.isClosed) {
                try {
                    val client = ss.accept()
                    launch { handleClientRequest(client) }
                } catch (e: Exception) {
                    if (!isActive) break
                }
            }
        }

        // Broadcast initial state to cloud & Supabase
        broadcastToCloud(context, match, cachedMatchJson)

        return BroadcastInfo(currentMatchCode, currentWebUrl, true)
    }

    fun onMatchUpdated(context: Context, match: Match) {
        if (!isBroadcasting) return
        cachedMatch = match
        val storage = CricketStorage(context)
        cachedMatchJson = storage.matchToJsonString(match)

        // Publish to Supabase and cloud relay asynchronously
        broadcastToCloud(context, match, cachedMatchJson)
    }

    private fun broadcastToCloud(context: Context? = null, match: Match? = null, jsonStr: String) {
        if (currentMatchCode.isEmpty()) return
        scope.launch {
            // 1. Upsert to Supabase if configured and enabled
            if (context != null && match != null && SupabaseClient.isConfigured(context) && SupabaseClient.isAutoSyncEnabled(context)) {
                try {
                    SupabaseClient.upsertLiveMatch(context, currentMatchCode, match)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 2. Supplementary pub-sub relay fallback
            try {
                val cleanCode = currentMatchCode.replace("-", "").lowercase()
                val url = URL("https://ntfy.sh/cricket_live_$cleanCode")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Title", "Cricket Score Update")
                }
                conn.outputStream.use { os ->
                    os.write(jsonStr.toByteArray(Charsets.UTF_8))
                    os.flush()
                }
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                // Ignore transient network errors
            }
        }
    }

    private fun handleClientRequest(socket: Socket) {
        try {
            socket.soTimeout = 4000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            val line = reader.readLine() ?: return socket.close()

            val parts = line.split(" ")
            val method = if (parts.isNotEmpty()) parts[0] else "GET"
            val path = if (parts.size > 1) parts[1] else "/"

            val writer = OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8)

            when {
                path == "/api/match" || path.startsWith("/api/match?") -> {
                    val json = cachedMatchJson
                    val bytes = json.toByteArray(Charsets.UTF_8)
                    writer.write("HTTP/1.1 200 OK\r\n")
                    writer.write("Content-Type: application/json; charset=utf-8\r\n")
                    writer.write("Content-Length: ${bytes.size}\r\n")
                    writer.write("Access-Control-Allow-Origin: *\r\n")
                    writer.write("Access-Control-Allow-Methods: GET, OPTIONS\r\n")
                    writer.write("Connection: close\r\n\r\n")
                    writer.write(json)
                    writer.flush()
                }
                method == "OPTIONS" -> {
                    writer.write("HTTP/1.1 200 OK\r\n")
                    writer.write("Access-Control-Allow-Origin: *\r\n")
                    writer.write("Access-Control-Allow-Methods: GET, OPTIONS\r\n")
                    writer.write("Access-Control-Allow-Headers: *\r\n")
                    writer.write("Content-Length: 0\r\n")
                    writer.write("Connection: close\r\n\r\n")
                    writer.flush()
                }
                else -> {
                    // Serve HTML
                    val html = LiveHtmlGenerator.generateHtml(cachedMatch, currentMatchCode)
                    val bytes = html.toByteArray(Charsets.UTF_8)
                    writer.write("HTTP/1.1 200 OK\r\n")
                    writer.write("Content-Type: text/html; charset=utf-8\r\n")
                    writer.write("Content-Length: ${bytes.size}\r\n")
                    writer.write("Access-Control-Allow-Origin: *\r\n")
                    writer.write("Connection: close\r\n\r\n")
                    writer.write(html)
                    writer.flush()
                }
            }
            socket.close()
        } catch (e: Exception) {
            try { socket.close() } catch (ignored: Exception) {}
        }
    }

    fun stopBroadcasting() {
        isBroadcasting = false
        stopServer()
    }

    private fun stopServer() {
        try {
            serverJob?.cancel()
            serverJob = null
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Viewer / Spectator Mode ---
    fun startWatching(
        context: Context,
        input: String,
        onMatchUpdate: (Match) -> Unit,
        onError: (String) -> Unit,
        onConnectionStateChanged: ((isReconnecting: Boolean) -> Unit)? = null
    ) {
        stopWatching()
        val target = input.trim()
        if (target.isEmpty()) {
            onError("Please enter a Match Code or Host IP")
            return
        }

        isSpectating = true
        spectatingTarget = target
        val storage = CricketStorage(context)

        val isLocalAddress = target.contains(".") || target.startsWith("http://")

        watcherJob = scope.launch {
            var consecutiveErrors = 0
            while (isActive && isSpectating) {
                try {
                    var liveMatch: Match? = null

                    // 1. Check Supabase first if configured
                    if (!isLocalAddress && SupabaseClient.isConfigured(context)) {
                        try {
                            liveMatch = SupabaseClient.fetchLiveMatch(context, target)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    // 2. Fall back to local server or pub/sub relay
                    if (liveMatch == null) {
                        val jsonStr = if (isLocalAddress) {
                            fetchFromLocalServer(target)
                        } else {
                            fetchFromCloud(target)
                        }

                        if (!jsonStr.isNullOrEmpty()) {
                            liveMatch = storage.jsonStringToMatch(jsonStr)
                        }
                    }

                    if (liveMatch != null) {
                        consecutiveErrors = 0
                        // Automatically save shared match to local storage for instant reconnect & offline viewing
                        storage.saveSharedMatch(target, liveMatch)
                        withContext(Dispatchers.Main) {
                            onConnectionStateChanged?.invoke(false)
                            onMatchUpdate(liveMatch)
                        }
                    } else {
                        consecutiveErrors++
                    }

                    if (consecutiveErrors in 3..5) {
                        withContext(Dispatchers.Main) {
                            onConnectionStateChanged?.invoke(true)
                        }
                    } else if (consecutiveErrors >= 6) {
                        withContext(Dispatchers.Main) {
                            onConnectionStateChanged?.invoke(true)
                            onError("Connection lag / reconnecting... Viewing cached scoreboard.")
                        }
                    }
                } catch (e: Exception) {
                    consecutiveErrors++
                    if (consecutiveErrors >= 6) {
                        withContext(Dispatchers.Main) {
                            onConnectionStateChanged?.invoke(true)
                            onError("Network hiccup: retrying live match automatically...")
                        }
                    }
                }
                delay(if (consecutiveErrors > 3) 2500 else 1800) // Adaptive poll rate
            }
        }
    }

    fun stopWatching() {
        isSpectating = false
        spectatingTarget = ""
        watcherJob?.cancel()
        watcherJob = null
    }

    private fun fetchUrl(urlStr: String, connectTimeoutMs: Int = 6000, readTimeoutMs: Int = 6000): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = true
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else null
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun fetchFromLocalServer(addressInput: String): String? {
        val cleanUrl = if (addressInput.startsWith("http://") || addressInput.startsWith("https://")) {
            if (addressInput.endsWith("/api/match")) addressInput else "${addressInput.trimEnd('/')}/api/match"
        } else {
            val portPart = if (!addressInput.contains(":")) ":8080" else ""
            "http://$addressInput$portPart/api/match"
        }

        return fetchUrl(cleanUrl, 3000, 3000)
    }

    private fun fetchFromCloud(matchCode: String): String? {
        val raw = matchCode.replace("-", "").trim().lowercase()
        val cleanCode = if (!raw.startsWith("cric") && raw.length == 4) "cric$raw" else raw
        val urlStr = "https://ntfy.sh/cricket_live_$cleanCode/json?poll=1"
        val response = fetchUrl(urlStr, 5000, 5000) ?: return null

        val lines = response.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return null

        // Parse lines in reverse order (newest broadcast event first)
        for (line in lines.reversed()) {
            try {
                val obj = JSONObject(line)

                // 1. Check if ntfy stored payload as an attachment (standard for payloads > 4KB)
                if (obj.has("attachment")) {
                    val attachObj = obj.optJSONObject("attachment")
                    val fileUrl = attachObj?.optString("url")
                    if (!fileUrl.isNullOrEmpty()) {
                        val attachmentContent = fetchUrl(fileUrl, 7000, 7000)
                        if (!attachmentContent.isNullOrBlank() && attachmentContent.trimStart().startsWith("{")) {
                            return attachmentContent
                        }
                    }
                }

                // 2. Check if payload was sent inline in the 'message' field
                if (obj.has("message")) {
                    val message = obj.optString("message", "")
                    if (message.trimStart().startsWith("{")) {
                        return message
                    }
                }
            } catch (e: Exception) {
                // If the line itself is raw JSON
                if (line.trimStart().startsWith("{")) {
                    return line
                }
            }
        }
        return null
    }
}
