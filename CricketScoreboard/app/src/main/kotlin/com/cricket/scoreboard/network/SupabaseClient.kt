package com.cricket.scoreboard.network

import android.content.Context
import android.content.SharedPreferences
import com.cricket.scoreboard.data.CricketStorage
import com.cricket.scoreboard.model.Match
import com.cricket.scoreboard.model.Team
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object SupabaseClient {

    private const val PREFS_NAME = "supabase_config_prefs"
    private const val KEY_URL = "supabase_url"
    private const val KEY_ANON_KEY = "supabase_anon_key"
    private const val KEY_AUTO_SYNC = "supabase_auto_sync"

    // Default Supabase project configuration (can be updated via in-app dialog)
    const val DEFAULT_URL = ""
    const val DEFAULT_KEY = ""

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isConfigured(context: Context): Boolean {
        val url = getUrl(context)
        val key = getAnonKey(context)
        return url.isNotBlank() && key.isNotBlank() && url.startsWith("https://")
    }

    fun getUrl(context: Context): String {
        return getPrefs(context).getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL
    }

    fun getAnonKey(context: Context): String {
        return getPrefs(context).getString(KEY_ANON_KEY, DEFAULT_KEY) ?: DEFAULT_KEY
    }

    fun isAutoSyncEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_SYNC, true)
    }

    fun saveConfig(context: Context, url: String, anonKey: String, autoSync: Boolean = true) {
        val cleanUrl = url.trim().removeSuffix("/")
        getPrefs(context).edit()
            .putString(KEY_URL, cleanUrl)
            .putString(KEY_ANON_KEY, anonKey.trim())
            .putBoolean(KEY_AUTO_SYNC, autoSync)
            .apply()
    }

    /**
     * Publishes or updates a live match on Supabase database in real time.
     */
    suspend fun upsertLiveMatch(context: Context, matchCode: String, match: Match): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured(context)) return@withContext false

        try {
            val baseUrl = getUrl(context)
            val anonKey = getAnonKey(context)
            val storage = CricketStorage(context)
            val matchJson = storage.matchToJson(match)

            val payload = JSONObject().apply {
                put("match_code", matchCode.trim().uppercase())
                put("match_id", match.id)
                put("team_a_name", match.teamA.name)
                put("team_b_name", match.teamB.name)
                put("match_data", matchJson)
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                put("last_updated", sdf.format(Date()))
            }

            val endpoint = "$baseUrl/rest/v1/live_matches"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $anonKey")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "resolution=merge-duplicates")
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Fetches live match score from Supabase database by match code.
     */
    suspend fun fetchLiveMatch(context: Context, matchCode: String): Match? = withContext(Dispatchers.IO) {
        if (!isConfigured(context)) return@withContext null

        try {
            val baseUrl = getUrl(context)
            val anonKey = getAnonKey(context)
            val codeClean = matchCode.trim().uppercase()

            val endpoint = "$baseUrl/rest/v1/live_matches?match_code=eq.$codeClean&select=match_data"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $anonKey")
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode in 200..299) {
                val responseStr = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
                conn.disconnect()

                val array = JSONArray(responseStr)
                if (array.length() > 0) {
                    val row = array.getJSONObject(0)
                    val matchDataObj = row.getJSONObject("match_data")
                    val storage = CricketStorage(context)
                    return@withContext storage.jsonToMatch(matchDataObj)
                }
            } else {
                conn.disconnect()
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Cloud sync: uploads a custom team to Supabase saved_teams table.
     */
    suspend fun uploadTeam(context: Context, team: Team): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured(context)) return@withContext false

        try {
            val baseUrl = getUrl(context)
            val anonKey = getAnonKey(context)
            val storage = CricketStorage(context)
            val teamJson = storage.teamToJson(team)

            val payload = JSONObject().apply {
                put("id", team.id)
                put("name", team.name)
                put("players", teamJson.getJSONArray("players"))
                put("player_count", team.players.size)
            }

            val endpoint = "$baseUrl/rest/v1/saved_teams"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $anonKey")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "resolution=merge-duplicates")
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(payload.toString()) }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Cloud sync: downloads saved teams from Supabase.
     */
    suspend fun fetchTeams(context: Context): List<Team> = withContext(Dispatchers.IO) {
        if (!isConfigured(context)) return@withContext emptyList()

        try {
            val baseUrl = getUrl(context)
            val anonKey = getAnonKey(context)

            val endpoint = "$baseUrl/rest/v1/saved_teams?select=*"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $anonKey")
            }

            if (conn.responseCode in 200..299) {
                val responseStr = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
                conn.disconnect()

                val array = JSONArray(responseStr)
                val list = mutableListOf<Team>()
                val storage = CricketStorage(context)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val teamObj = JSONObject().apply {
                        put("id", obj.getString("id"))
                        put("name", obj.getString("name"))
                        put("players", obj.getJSONArray("players"))
                    }
                    list.add(storage.jsonToTeam(teamObj))
                }
                list
            } else {
                conn.disconnect()
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Cloud sync: uploads completed match scorecard to Supabase match_history table.
     */
    suspend fun uploadMatchHistory(context: Context, match: Match): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured(context)) return@withContext false

        try {
            val baseUrl = getUrl(context)
            val anonKey = getAnonKey(context)
            val storage = CricketStorage(context)
            val matchJson = storage.matchToJson(match)

            val payload = JSONObject().apply {
                put("id", match.id)
                put("date", match.date)
                put("team_a_name", match.teamA.name)
                put("team_b_name", match.teamB.name)
                put("result_message", match.resultMessage ?: "")
                put("match_data", matchJson)
            }

            val endpoint = "$baseUrl/rest/v1/match_history"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $anonKey")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "resolution=merge-duplicates")
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(payload.toString()) }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Tests connection to Supabase project.
     */
    suspend fun testConnection(urlStr: String, keyStr: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanUrl = urlStr.trim().removeSuffix("/")
        val cleanKey = keyStr.trim()

        if (cleanUrl.isBlank() || cleanKey.isBlank()) {
            return@withContext Pair(false, "Project URL and Anon Key cannot be empty.")
        }
        if (!cleanUrl.startsWith("https://")) {
            return@withContext Pair(false, "Project URL must begin with https://")
        }

        try {
            val endpoint = "$cleanUrl/rest/v1/live_matches?limit=1"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("apikey", cleanKey)
                setRequestProperty("Authorization", "Bearer $cleanKey")
            }

            val responseCode = conn.responseCode
            conn.disconnect()

            if (responseCode in 200..299) {
                Pair(true, "Connected to Supabase successfully!")
            } else if (responseCode == 404) {
                Pair(true, "Connected to Supabase! (Note: run supabase_schema.sql to create tables)")
            } else if (responseCode == 401 || responseCode == 403) {
                Pair(false, "Invalid Supabase Anon Key (HTTP $responseCode).")
            } else {
                Pair(false, "Supabase returned HTTP $responseCode")
            }
        } catch (e: Exception) {
            Pair(false, "Connection failed: ${e.localizedMessage ?: "Network error"}")
        }
    }
}
