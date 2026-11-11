package com.cricket.scoreboard.data

import android.content.Context
import android.content.SharedPreferences
import com.cricket.scoreboard.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class CricketStorage(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cricket_scoreboard_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACTIVE_MATCH = "key_active_match"
        private const val KEY_MATCH_HISTORY = "key_match_history"
        private const val KEY_SAVED_TEAMS = "key_saved_teams"
        private const val KEY_LAST_TEAMS = "key_last_teams"
        private const val KEY_LAST_SPECTATED_CODE = "key_last_spectated_code"
        private const val KEY_LAST_SPECTATED_MATCH = "key_last_spectated_match"
        private const val KEY_SAVED_SHARED_MATCHES = "key_saved_shared_matches"
        private const val KEY_SAVED_PLAYERS = "key_saved_players"
    }

    fun saveActiveMatch(match: Match?) {
        if (match == null) {
            prefs.edit().remove(KEY_ACTIVE_MATCH).apply()
        } else {
            saveLastMatchTeams(match.teamA, match.teamB)
            val json = matchToJson(match).toString()
            prefs.edit().putString(KEY_ACTIVE_MATCH, json).apply()
        }
    }

    fun getActiveMatch(): Match? {
        val jsonStr = prefs.getString(KEY_ACTIVE_MATCH, null) ?: return null
        return try {
            jsonToMatch(JSONObject(jsonStr))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveCompletedMatch(match: Match) {
        saveLastMatchTeams(match.teamA, match.teamB)
        val history = getMatchHistory().toMutableList()
        val existingIndex = history.indexOfFirst { it.id == match.id }
        if (existingIndex >= 0) {
            history[existingIndex] = match
        } else {
            history.add(0, match)
        }
        val array = JSONArray()
        for (m in history) {
            array.put(matchToJson(m))
        }
        prefs.edit().putString(KEY_MATCH_HISTORY, array.toString()).apply()
    }

    fun getMatchHistory(): List<Match> {
        val jsonStr = prefs.getString(KEY_MATCH_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<Match>()
            for (i in 0 until array.length()) {
                list.add(jsonToMatch(array.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun deleteMatchFromHistory(matchId: String) {
        val history = getMatchHistory().filter { it.id != matchId }
        val array = JSONArray()
        for (m in history) {
            array.put(matchToJson(m))
        }
        prefs.edit().putString(KEY_MATCH_HISTORY, array.toString()).apply()
    }

    // --- Saved Shared Matches & Offline Reconnect Support ---
    fun saveSharedMatch(matchCode: String, match: Match) {
        try {
            val codeClean = matchCode.trim().uppercase()
            val scoreSummary = "${match.currentInnings.battingTeamName} ${match.currentInnings.totalRuns}/${match.currentInnings.wickets} (${match.currentInnings.oversText}/${match.currentInnings.maxOvers} Ov)"

            // 1. Save as latest spectated match
            prefs.edit()
                .putString(KEY_LAST_SPECTATED_CODE, codeClean)
                .putString(KEY_LAST_SPECTATED_MATCH, matchToJson(match).toString())
                .apply()

            // 2. Add or update in saved shared list
            val existingList = getSavedSharedMatches().filter { it.matchCode != codeClean }.toMutableList()
            val newRecord = SavedSharedMatch(
                matchCode = codeClean,
                teamAName = match.teamA.name,
                teamBName = match.teamB.name,
                scoreSummary = scoreSummary,
                lastUpdated = System.currentTimeMillis(),
                match = match
            )
            existingList.add(0, newRecord)

            // Keep top 15 shared matches
            val trimmedList = existingList.take(15)
            val jsonArray = JSONArray()
            for (item in trimmedList) {
                val obj = JSONObject().apply {
                    put("matchCode", item.matchCode)
                    put("teamAName", item.teamAName)
                    put("teamBName", item.teamBName)
                    put("scoreSummary", item.scoreSummary)
                    put("lastUpdated", item.lastUpdated)
                    put("match", matchToJson(item.match))
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_SAVED_SHARED_MATCHES, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getSavedSharedMatches(): List<SavedSharedMatch> {
        val jsonStr = prefs.getString(KEY_SAVED_SHARED_MATCHES, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<SavedSharedMatch>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val m = jsonToMatch(obj.getJSONObject("match"))
                list.add(
                    SavedSharedMatch(
                        matchCode = obj.getString("matchCode"),
                        teamAName = obj.optString("teamAName", m.teamA.name),
                        teamBName = obj.optString("teamBName", m.teamB.name),
                        scoreSummary = obj.optString("scoreSummary", ""),
                        lastUpdated = obj.optLong("lastUpdated", 0L),
                        match = m
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getLastSpectatedMatch(): Pair<String, Match>? {
        val code = prefs.getString(KEY_LAST_SPECTATED_CODE, null) ?: return null
        val matchJson = prefs.getString(KEY_LAST_SPECTATED_MATCH, null) ?: return null
        return try {
            val m = jsonToMatch(JSONObject(matchJson))
            Pair(code, m)
        } catch (e: Exception) {
            null
        }
    }

    fun deleteSavedSharedMatch(matchCode: String) {
        val list = getSavedSharedMatches().filter { it.matchCode != matchCode }
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("matchCode", item.matchCode)
                put("teamAName", item.teamAName)
                put("teamBName", item.teamBName)
                put("scoreSummary", item.scoreSummary)
                put("lastUpdated", item.lastUpdated)
                put("match", matchToJson(item.match))
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_SAVED_SHARED_MATCHES, jsonArray.toString()).apply()
    }

    /**
     * Completely wipes all SharedPreferences and app internal storage files
     * (player photos, cache images, matches, and saved teams).
     */
    fun clearAllData() {
        try {
            prefs.edit().clear().apply()

            val photosDir = File(context.filesDir, "player_photos")
            if (photosDir.exists()) {
                photosDir.deleteRecursively()
            }

            val cacheImages = File(context.cacheDir, "images")
            if (cacheImages.exists()) {
                cacheImages.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Saved Teams Functionality ---
    private val DEMO_TEAM_NAMES = setOf(
        "india", "australia", "csk", "mi",
        "chennai super kings", "mumbai indians",
        "gully stars", "gully warriors", "turf kings",
        "ind", "aus"
    )

    fun purgeAllDemoData() {
        try {
            // 1. Purge saved teams containing demo presets
            val teams = getSavedTeams().filterNot { team ->
                team.id.startsWith("preset_") ||
                team.id.startsWith("demo_") ||
                DEMO_TEAM_NAMES.contains(team.name.trim().lowercase())
            }
            saveAllTeams(teams)

            // 2. Purge match history containing demo teams
            val history = getMatchHistory().filterNot { match ->
                DEMO_TEAM_NAMES.contains(match.teamA.name.trim().lowercase()) ||
                DEMO_TEAM_NAMES.contains(match.teamB.name.trim().lowercase()) ||
                match.id.startsWith("demo_")
            }
            val historyArray = JSONArray()
            for (m in history) {
                historyArray.put(matchToJson(m))
            }
            prefs.edit().putString(KEY_MATCH_HISTORY, historyArray.toString()).apply()

            // 3. Purge active match if it's a demo match
            val active = getActiveMatch()
            if (active != null) {
                if (DEMO_TEAM_NAMES.contains(active.teamA.name.trim().lowercase()) ||
                    DEMO_TEAM_NAMES.contains(active.teamB.name.trim().lowercase()) ||
                    active.id.startsWith("demo_")
                ) {
                    saveActiveMatch(null)
                }
            }

            // 4. Purge last match rematch banner if demo
            val lastTeams = getLastMatchTeams()
            if (lastTeams != null) {
                if (DEMO_TEAM_NAMES.contains(lastTeams.first.name.trim().lowercase()) ||
                    DEMO_TEAM_NAMES.contains(lastTeams.second.name.trim().lowercase()) ||
                    lastTeams.first.id.startsWith("preset_") ||
                    lastTeams.second.id.startsWith("preset_")
                ) {
                    prefs.edit().remove(KEY_LAST_TEAMS).apply()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getSavedTeams(): List<Team> {
        val jsonStr = prefs.getString(KEY_SAVED_TEAMS, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<Team>()
            var hadPresets = false
            for (i in 0 until array.length()) {
                val t = jsonToTeam(array.getJSONObject(i))
                // Exclude any legacy demo presets by ID or name
                val isDemo = t.id.startsWith("preset_") ||
                        t.id.startsWith("demo_") ||
                        DEMO_TEAM_NAMES.contains(t.name.trim().lowercase())
                if (!isDemo) {
                    list.add(t)
                } else {
                    hadPresets = true
                }
            }
            if (hadPresets) {
                saveAllTeams(list)
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveTeam(team: Team) {
        val current = getSavedTeams().toMutableList()
        val existingIndex = current.indexOfFirst { it.id == team.id || it.name.trim().equals(team.name.trim(), ignoreCase = true) }
        if (existingIndex >= 0) {
            current[existingIndex] = team
        } else {
            current.add(0, team)
        }
        saveAllTeams(current)
    }

    fun deleteSavedTeam(teamId: String) {
        val current = getSavedTeams().filter { it.id != teamId }
        saveAllTeams(current)
    }

    private fun saveAllTeams(teams: List<Team>) {
        val array = JSONArray()
        for (t in teams) {
            array.put(teamToJson(t))
        }
        prefs.edit().putString(KEY_SAVED_TEAMS, array.toString()).apply()
    }

    // --- Last Match Teams for Rematch / New Game with Same Teams ---
    fun saveLastMatchTeams(teamA: Team, teamB: Team) {
        val obj = JSONObject()
        obj.put("teamA", teamToJson(teamA))
        obj.put("teamB", teamToJson(teamB))
        prefs.edit().putString(KEY_LAST_TEAMS, obj.toString()).apply()
    }

    fun getLastMatchTeams(): Pair<Team, Team>? {
        val jsonStr = prefs.getString(KEY_LAST_TEAMS, null)
        if (jsonStr != null) {
            try {
                val obj = JSONObject(jsonStr)
                val teamA = jsonToTeam(obj.getJSONObject("teamA"))
                val teamB = jsonToTeam(obj.getJSONObject("teamB"))
                return Pair(teamA, teamB)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val active = getActiveMatch()
        if (active != null) {
            return Pair(active.teamA, active.teamB)
        }
        val history = getMatchHistory()
        if (history.isNotEmpty()) {
            return Pair(history.first().teamA, history.first().teamB)
        }
        return null
    }

    // --- Saved Individual Players (Player Pool / Roster) ---
    fun getSavedPlayers(): List<Player> {
        val jsonStr = prefs.getString(KEY_SAVED_PLAYERS, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<Player>()
            for (i in 0 until array.length()) {
                val po = array.getJSONObject(i)
                list.add(
                    Player(
                        id = po.optString("id", java.util.UUID.randomUUID().toString()),
                        name = po.getString("name"),
                        role = try {
                            PlayerRole.valueOf(po.optString("role", PlayerRole.ALL_ROUNDER.name))
                        } catch (e: Exception) {
                            PlayerRole.ALL_ROUNDER
                        },
                        isCaptain = po.optBoolean("isCaptain", false),
                        isWicketKeeper = po.optBoolean("isWicketKeeper", false),
                        photoUri = po.optString("photoUri").ifEmpty { null }
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun savePlayer(player: Player) {
        val current = getSavedPlayers().toMutableList()
        val index = current.indexOfFirst {
            it.id == player.id || it.name.trim().equals(player.name.trim(), ignoreCase = true)
        }
        if (index >= 0) {
            current[index] = player
        } else {
            current.add(0, player)
        }
        saveAllPlayers(current)
    }

    fun saveAllPlayers(players: List<Player>) {
        val array = JSONArray()
        for (p in players) {
            val po = JSONObject().apply {
                put("id", p.id)
                put("name", p.name.trim())
                put("role", p.role.name)
                put("isCaptain", p.isCaptain)
                put("isWicketKeeper", p.isWicketKeeper)
                if (p.photoUri != null) put("photoUri", p.photoUri)
            }
            array.put(po)
        }
        prefs.edit().putString(KEY_SAVED_PLAYERS, array.toString()).apply()
    }

    fun deleteSavedPlayer(playerId: String) {
        val current = getSavedPlayers().filter { it.id != playerId }
        saveAllPlayers(current)
    }

    fun importPlayersFromTeams(): Int {
        val teams = getSavedTeams()
        val existing = getSavedPlayers().toMutableList()
        var addedCount = 0
        for (team in teams) {
            for (p in team.players) {
                val cleanName = p.name.trim()
                if (cleanName.isNotBlank() &&
                    !cleanName.startsWith("Player ", ignoreCase = true) &&
                    existing.none { it.name.trim().equals(cleanName, ignoreCase = true) }
                ) {
                    existing.add(p.copy(id = java.util.UUID.randomUUID().toString(), isCaptain = false, isWicketKeeper = false))
                    addedCount++
                }
            }
        }
        if (addedCount > 0) {
            saveAllPlayers(existing)
        }
        return addedCount
    }

    // JSON serialization helpers
    fun matchToJson(m: Match): JSONObject {
        val obj = JSONObject()
        obj.put("id", m.id)
        obj.put("date", m.date)
        obj.put("totalOvers", m.totalOvers)
        obj.put("maxOversPerBowler", m.maxOversPerBowler)
        obj.put("playersPerSide", m.playersPerSide)
        obj.put("wideRuns", m.wideRuns)
        obj.put("noBallRuns", m.noBallRuns)
        obj.put("rebowlWide", m.rebowlWide)
        obj.put("rebowlNoBall", m.rebowlNoBall)
        obj.put("freeHitEnabled", m.freeHitEnabled)
        obj.put("currentInningsIndex", m.currentInningsIndex)
        obj.put("currentStrikerId", m.currentStrikerId)
        obj.put("currentNonStrikerId", m.currentNonStrikerId)
        obj.put("currentBowlerId", m.currentBowlerId)
        obj.put("lastBowlerId", m.lastBowlerId ?: "")
        obj.put("isFinished", m.isFinished)
        obj.put("resultMessage", m.resultMessage ?: "")
        obj.put("manOfTheMatch", m.manOfTheMatch ?: "")

        obj.put("teamA", teamToJson(m.teamA))
        obj.put("teamB", teamToJson(m.teamB))

        val tossObj = JSONObject()
        tossObj.put("winnerTeamId", m.tossResult.winnerTeamId)
        tossObj.put("choice", m.tossResult.choice.name)
        obj.put("tossResult", tossObj)

        obj.put("innings1", inningsToJson(m.innings1))
        if (m.innings2 != null) {
            obj.put("innings2", inningsToJson(m.innings2))
        }
        return obj
    }

    fun teamToJson(team: Team): JSONObject {
        val t = JSONObject()
        t.put("id", team.id)
        t.put("name", team.name)
        val pArray = JSONArray()
        for (p in team.players) {
            val po = JSONObject()
            po.put("id", p.id)
            po.put("name", p.name)
            po.put("role", p.role.name)
            po.put("isCaptain", p.isCaptain)
            po.put("isWicketKeeper", p.isWicketKeeper)
            if (p.photoUri != null) po.put("photoUri", p.photoUri)
            pArray.put(po)
        }
        t.put("players", pArray)
        return t
    }

    private fun inningsToJson(inn: Innings): JSONObject {
        val obj = JSONObject()
        obj.put("inningsNumber", inn.inningsNumber)
        obj.put("battingTeamId", inn.battingTeamId)
        obj.put("battingTeamName", inn.battingTeamName)
        obj.put("bowlingTeamId", inn.bowlingTeamId)
        obj.put("bowlingTeamName", inn.bowlingTeamName)
        if (inn.targetScore != null) obj.put("targetScore", inn.targetScore)
        obj.put("totalRuns", inn.totalRuns)
        obj.put("wickets", inn.wickets)
        obj.put("legalBalls", inn.legalBalls)
        obj.put("maxOvers", inn.maxOvers)
        obj.put("wides", inn.wides)
        obj.put("noBalls", inn.noBalls)
        obj.put("legByes", inn.legByes)
        obj.put("byes", inn.byes)
        obj.put("penaltyRuns", inn.penaltyRuns)
        obj.put("isCompleted", inn.isCompleted)

        val bArray = JSONArray()
        for (b in inn.batsmen) {
            val bo = JSONObject()
            bo.put("playerId", b.playerId)
            bo.put("name", b.name)
            bo.put("runs", b.runs)
            bo.put("balls", b.balls)
            bo.put("fours", b.fours)
            bo.put("sixes", b.sixes)
            bo.put("dismissalText", b.dismissalText)
            bo.put("isOut", b.isOut)
            bo.put("isBatting", b.isBatting)
            if (b.photoUri != null) bo.put("photoUri", b.photoUri)
            bArray.put(bo)
        }
        obj.put("batsmen", bArray)

        val bwArray = JSONArray()
        for (bw in inn.bowlers) {
            val bwo = JSONObject()
            bwo.put("playerId", bw.playerId)
            bwo.put("name", bw.name)
            bwo.put("legalBalls", bw.legalBalls)
            bwo.put("maidens", bw.maidens)
            bwo.put("runsConceded", bw.runsConceded)
            bwo.put("wickets", bw.wickets)
            if (bw.photoUri != null) bwo.put("photoUri", bw.photoUri)
            bwArray.put(bwo)
        }
        obj.put("bowlers", bwArray)

        val dArray = JSONArray()
        for (d in inn.deliveries) {
            val do_ = JSONObject()
            do_.put("id", d.id)
            do_.put("overIndex", d.overIndex)
            do_.put("ballInOver", d.ballInOver)
            do_.put("legalBallNumber", d.legalBallNumber)
            do_.put("bowlerId", d.bowlerId)
            do_.put("bowlerName", d.bowlerName)
            do_.put("strikerId", d.strikerId)
            do_.put("strikerName", d.strikerName)
            do_.put("nonStrikerId", d.nonStrikerId)
            do_.put("nonStrikerName", d.nonStrikerName)
            do_.put("runsBat", d.runsBat)
            do_.put("extraRuns", d.extraRuns)
            do_.put("extraType", d.extraType.name)
            do_.put("isWicket", d.isWicket)
            if (d.wicketType != null) do_.put("wicketType", d.wicketType.name)
            do_.put("dismissedPlayerId", d.dismissedPlayerId ?: "")
            do_.put("dismissedPlayerName", d.dismissedPlayerName ?: "")
            do_.put("fielderName", d.fielderName ?: "")
            do_.put("description", d.description)
            do_.put("isLegalDelivery", d.isLegalDelivery)
            do_.put("isFreeHit", d.isFreeHit)
            do_.put("timestamp", d.timestamp)
            dArray.put(do_)
        }
        obj.put("deliveries", dArray)

        val fArray = JSONArray()
        for (f in inn.fallOfWickets) {
            val fo = JSONObject()
            fo.put("wicketNum", f.wicketNum)
            fo.put("teamRuns", f.teamRuns)
            fo.put("overText", f.overText)
            fo.put("batsmanName", f.batsmanName)
            fArray.put(fo)
        }
        obj.put("fallOfWickets", fArray)

        return obj
    }

    fun jsonToMatch(obj: JSONObject): Match {
        val teamA = jsonToTeam(obj.getJSONObject("teamA"))
        val teamB = jsonToTeam(obj.getJSONObject("teamB"))
        val tossObj = obj.getJSONObject("tossResult")
        val tossResult = TossResult(
            winnerTeamId = tossObj.getString("winnerTeamId"),
            choice = TossChoice.valueOf(tossObj.getString("choice"))
        )
        val inn1 = jsonToInnings(obj.getJSONObject("innings1"))
        val inn2 = if (obj.has("innings2")) jsonToInnings(obj.getJSONObject("innings2")) else null

        return Match(
            id = obj.getString("id"),
            date = obj.optString("date", ""),
            teamA = teamA,
            teamB = teamB,
            totalOvers = obj.optInt("totalOvers", 20),
            maxOversPerBowler = obj.optInt("maxOversPerBowler", 4),
            playersPerSide = obj.optInt("playersPerSide", 11),
            wideRuns = obj.optInt("wideRuns", 1),
            noBallRuns = obj.optInt("noBallRuns", 1),
            rebowlWide = obj.optBoolean("rebowlWide", true),
            rebowlNoBall = obj.optBoolean("rebowlNoBall", true),
            freeHitEnabled = obj.optBoolean("freeHitEnabled", true),
            tossResult = tossResult,
            innings1 = inn1,
            innings2 = inn2,
            currentInningsIndex = obj.optInt("currentInningsIndex", 1),
            currentStrikerId = obj.optString("currentStrikerId", ""),
            currentNonStrikerId = obj.optString("currentNonStrikerId", ""),
            currentBowlerId = obj.optString("currentBowlerId", ""),
            lastBowlerId = obj.optString("lastBowlerId").ifEmpty { null },
            isFinished = obj.optBoolean("isFinished", false),
            resultMessage = obj.optString("resultMessage").ifEmpty { null },
            manOfTheMatch = obj.optString("manOfTheMatch").ifEmpty { null }
        )
    }

    fun matchToJsonString(match: Match): String = matchToJson(match).toString()

    fun jsonStringToMatch(json: String): Match? {
        return try {
            jsonToMatch(JSONObject(json))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun jsonToTeam(obj: JSONObject): Team {
        val players = mutableListOf<Player>()
        val pArray = obj.getJSONArray("players")
        for (i in 0 until pArray.length()) {
            val po = pArray.getJSONObject(i)
            players.add(
                Player(
                    id = po.getString("id"),
                    name = po.getString("name"),
                    role = PlayerRole.valueOf(po.optString("role", PlayerRole.ALL_ROUNDER.name)),
                    isCaptain = po.optBoolean("isCaptain", false),
                    isWicketKeeper = po.optBoolean("isWicketKeeper", false),
                    photoUri = po.optString("photoUri").ifEmpty { null }
                )
            )
        }
        return Team(
            id = obj.getString("id"),
            name = obj.getString("name"),
            players = players
        )
    }

    private fun jsonToInnings(obj: JSONObject): Innings {
        val batsmen = mutableListOf<BatsmanScore>()
        val bArray = obj.getJSONArray("batsmen")
        for (i in 0 until bArray.length()) {
            val bo = bArray.getJSONObject(i)
            batsmen.add(
                BatsmanScore(
                    playerId = bo.getString("playerId"),
                    name = bo.getString("name"),
                    runs = bo.optInt("runs", 0),
                    balls = bo.optInt("balls", 0),
                    fours = bo.optInt("fours", 0),
                    sixes = bo.optInt("sixes", 0),
                    dismissalText = bo.optString("dismissalText", "not out"),
                    isOut = bo.optBoolean("isOut", false),
                    isBatting = bo.optBoolean("isBatting", false),
                    photoUri = bo.optString("photoUri").ifEmpty { null }
                )
            )
        }

        val bowlers = mutableListOf<BowlerScore>()
        val bwArray = obj.getJSONArray("bowlers")
        for (i in 0 until bwArray.length()) {
            val bwo = bwArray.getJSONObject(i)
            bowlers.add(
                BowlerScore(
                    playerId = bwo.getString("playerId"),
                    name = bwo.getString("name"),
                    legalBalls = bwo.optInt("legalBalls", 0),
                    maidens = bwo.optInt("maidens", 0),
                    runsConceded = bwo.optInt("runsConceded", 0),
                    wickets = bwo.optInt("wickets", 0),
                    photoUri = bwo.optString("photoUri").ifEmpty { null }
                )
            )
        }

        val deliveries = mutableListOf<Delivery>()
        val dArray = obj.getJSONArray("deliveries")
        for (i in 0 until dArray.length()) {
            val do_ = dArray.getJSONObject(i)
            val wt = if (do_.has("wicketType")) WicketType.valueOf(do_.getString("wicketType")) else null
            val et = ExtraType.valueOf(do_.optString("extraType", ExtraType.NONE.name))
            deliveries.add(
                Delivery(
                    id = do_.getString("id"),
                    overIndex = do_.optInt("overIndex", 0),
                    ballInOver = do_.optInt("ballInOver", 1),
                    legalBallNumber = do_.optInt("legalBallNumber", 0),
                    bowlerId = do_.getString("bowlerId"),
                    bowlerName = do_.getString("bowlerName"),
                    strikerId = do_.getString("strikerId"),
                    strikerName = do_.getString("strikerName"),
                    nonStrikerId = do_.getString("nonStrikerId"),
                    nonStrikerName = do_.getString("nonStrikerName"),
                    runsBat = do_.optInt("runsBat", 0),
                    extraRuns = do_.optInt("extraRuns", 0),
                    extraType = et,
                    isWicket = do_.optBoolean("isWicket", false),
                    wicketType = wt,
                    dismissedPlayerId = do_.optString("dismissedPlayerId").ifEmpty { null },
                    dismissedPlayerName = do_.optString("dismissedPlayerName").ifEmpty { null },
                    fielderName = do_.optString("fielderName").ifEmpty { null },
                    description = do_.optString("description", ""),
                    isLegalDelivery = do_.optBoolean("isLegalDelivery", wt != null || (et != ExtraType.WIDE && et != ExtraType.NO_BALL)),
                    isFreeHit = do_.optBoolean("isFreeHit", false),
                    timestamp = do_.optLong("timestamp", 0L)
                )
            )
        }

        val fows = mutableListOf<FallOfWicket>()
        val fArray = obj.getJSONArray("fallOfWickets")
        for (i in 0 until fArray.length()) {
            val fo = fArray.getJSONObject(i)
            fows.add(
                FallOfWicket(
                    wicketNum = fo.getInt("wicketNum"),
                    teamRuns = fo.getInt("teamRuns"),
                    overText = fo.getString("overText"),
                    batsmanName = fo.getString("batsmanName")
                )
            )
        }

        return Innings(
            inningsNumber = obj.getInt("inningsNumber"),
            battingTeamId = obj.getString("battingTeamId"),
            battingTeamName = obj.getString("battingTeamName"),
            bowlingTeamId = obj.getString("bowlingTeamId"),
            bowlingTeamName = obj.getString("bowlingTeamName"),
            targetScore = if (obj.has("targetScore")) obj.getInt("targetScore") else null,
            totalRuns = obj.optInt("totalRuns", 0),
            wickets = obj.optInt("wickets", 0),
            legalBalls = obj.optInt("legalBalls", 0),
            maxOvers = obj.optInt("maxOvers", 20),
            wides = obj.optInt("wides", 0),
            noBalls = obj.optInt("noBalls", 0),
            legByes = obj.optInt("legByes", 0),
            byes = obj.optInt("byes", 0),
            penaltyRuns = obj.optInt("penaltyRuns", 0),
            batsmen = batsmen,
            bowlers = bowlers,
            deliveries = deliveries,
            fallOfWickets = fows,
            isCompleted = obj.optBoolean("isCompleted", false)
        )
    }
}
