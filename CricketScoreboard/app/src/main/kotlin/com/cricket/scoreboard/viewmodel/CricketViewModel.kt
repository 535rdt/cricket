package com.cricket.scoreboard.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cricket.scoreboard.data.CricketStorage
import com.cricket.scoreboard.model.*
import com.cricket.scoreboard.network.LiveSharingManager
import com.cricket.scoreboard.network.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ActiveScreen {
    SETUP,
    LIVE_SCORING,
    FULL_SCORECARD,
    TEAMS,
    SHARING,
    HISTORY
}

data class SetupUiState(
    val teamAName: String = "Team 1",
    val teamBName: String = "Team 2",
    val totalOvers: Int = 10,
    val playersPerSide: Int = 11,
    val wideRuns: Int = 1,
    val noBallRuns: Int = 1,
    val maxOversPerBowler: Int = 2,
    val isMaxBowlerOversAuto: Boolean = true,
    val rebowlWide: Boolean = true,
    val rebowlNoBall: Boolean = true,
    val freeHitEnabled: Boolean = true,
    val tossWinnerIsTeamA: Boolean = true,
    val tossChoice: TossChoice = TossChoice.BAT,
    val teamAPlayers: List<Player> = emptyList(),
    val teamBPlayers: List<Player> = emptyList()
)

data class ScoreboardUiState(
    val currentScreen: ActiveScreen = ActiveScreen.SETUP,
    val setup: SetupUiState = SetupUiState(),
    val match: Match? = null,
    val undoStack: List<Match> = emptyList(),
    val showOpeningDialog: Boolean = false,
    val showNextBowlerDialog: Boolean = false,
    val showNewBatsmanDialog: Boolean = false,
    val showWicketDialog: Boolean = false,
    val showInningsBreakDialog: Boolean = false,
    val showMatchEndDialog: Boolean = false,
    val showCustomRunsDialog: Boolean = false,
    val showSavedTeamsDialog: Boolean = false,
    val savedTeamsTargetIsTeamA: Boolean = true,
    val showSaveTeamDialog: Boolean = false,
    val saveTeamIsTeamA: Boolean = true,
    val showNewGameDialog: Boolean = false,
    val savedTeams: List<Team> = emptyList(),
    val lastMatchTeams: Pair<Team, Team>? = null,
    val pendingWicketType: WicketType? = null,
    val pendingDismissedPlayerId: String? = null,
    val isBroadcasting: Boolean = false,
    val broadcastMatchCode: String = "",
    val broadcastWebUrl: String = "",
    val isSpectatorMode: Boolean = false,
    val spectatorTarget: String = "",
    val showLiveSharingDialog: Boolean = false,
    val showWatchLiveDialog: Boolean = false,
    val showShareReportDialog: Boolean = false,
    val liveViewerLoading: Boolean = false,
    val liveViewerError: String? = null,
    val statusMessage: String? = null
)

class CricketViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = CricketStorage(application)

    private val _uiState = MutableStateFlow(ScoreboardUiState())
    val uiState: StateFlow<ScoreboardUiState> = _uiState.asStateFlow()

    init {
        storage.purgeAllDemoData()
        loadDefaultPlayers()
        restoreActiveMatch()
        loadSavedTeams()
        loadLastMatchTeams()
    }

    private fun loadDefaultPlayers() {
        val teamAPlayers = generateDefaultPlayers(11)
        val teamBPlayers = generateDefaultPlayers(11)
        _uiState.update {
            it.copy(
                setup = it.setup.copy(
                    teamAPlayers = teamAPlayers,
                    teamBPlayers = teamBPlayers
                )
            )
        }
    }

    private fun generateDefaultPlayers(count: Int): List<Player> {
        return (1..count).map { i ->
            val role = when (i) {
                1, 2, 3, 4 -> PlayerRole.BATSMAN
                5 -> PlayerRole.WICKET_KEEPER
                6, 7 -> PlayerRole.ALL_ROUNDER
                else -> PlayerRole.BOWLER
            }
            Player(
                id = UUID.randomUUID().toString(),
                name = "Player $i",
                role = role,
                isCaptain = i == 1,
                isWicketKeeper = i == 5
            )
        }
    }

    private fun restoreActiveMatch() {
        val saved = storage.getActiveMatch()
        if (saved != null) {
            _uiState.update {
                it.copy(
                    match = saved,
                    lastMatchTeams = Pair(saved.teamA, saved.teamB),
                    currentScreen = if (saved.isFinished) ActiveScreen.FULL_SCORECARD else ActiveScreen.LIVE_SCORING
                )
            }
        }
    }

    private fun saveAndBroadcastMatch(match: Match?) {
        storage.saveActiveMatch(match)
        if (match != null && LiveSharingManager.isBroadcasting) {
            LiveSharingManager.onMatchUpdated(getApplication(), match)
        }
    }

    // Live Scoreboard Sharing & Spectator Mode
    fun openLiveSharingDialog() {
        val match = _uiState.value.match
        if (match != null) {
            val isBroadcasting = LiveSharingManager.isBroadcasting
            val code = if (isBroadcasting) LiveSharingManager.currentMatchCode else "CRIC-${match.id.replace("-", "").takeLast(4).uppercase()}"
            val url = if (isBroadcasting) LiveSharingManager.currentWebUrl else "http://${LiveSharingManager.getLocalIpAddress()}:8080"
            _uiState.update {
                it.copy(
                    showLiveSharingDialog = true,
                    isBroadcasting = isBroadcasting,
                    broadcastMatchCode = code,
                    broadcastWebUrl = url
                )
            }
        } else {
            _uiState.update { it.copy(statusMessage = "Start a match first to share live score!") }
        }
    }

    fun dismissLiveSharingDialog() {
        _uiState.update { it.copy(showLiveSharingDialog = false) }
    }

    fun toggleLiveBroadcast(enable: Boolean) {
        val match = _uiState.value.match ?: return
        if (enable) {
            val info = LiveSharingManager.startBroadcasting(getApplication(), match)
            _uiState.update {
                it.copy(
                    isBroadcasting = true,
                    broadcastMatchCode = info.matchCode,
                    broadcastWebUrl = info.localWebUrl,
                    statusMessage = "🟢 Live broadcast started! Match Code: ${info.matchCode}"
                )
            }
        } else {
            LiveSharingManager.stopBroadcasting()
            _uiState.update {
                it.copy(
                    isBroadcasting = false,
                    statusMessage = "Live broadcast paused."
                )
            }
        }
    }

    fun openWatchLiveDialog() {
        _uiState.update { it.copy(showWatchLiveDialog = true, liveViewerError = null, liveViewerLoading = false) }
    }

    fun dismissWatchLiveDialog() {
        _uiState.update { it.copy(showWatchLiveDialog = false, liveViewerError = null, liveViewerLoading = false) }
    }

    fun connectToLiveMatch(inputCode: String) {
        _uiState.update { it.copy(liveViewerLoading = true, liveViewerError = null) }
        LiveSharingManager.startWatching(
            context = getApplication(),
            input = inputCode,
            onMatchUpdate = { liveMatch ->
                _uiState.update {
                    it.copy(
                        match = liveMatch,
                        isSpectatorMode = true,
                        spectatorTarget = inputCode,
                        showWatchLiveDialog = false,
                        liveViewerLoading = false,
                        liveViewerError = null,
                        currentScreen = ActiveScreen.SHARING,
                        statusMessage = "🔴 Connected to live match!"
                    )
                }
            },
            onError = { err ->
                _uiState.update { it.copy(liveViewerLoading = false, liveViewerError = err) }
            }
        )
    }

    fun exitSpectatorMode() {
        LiveSharingManager.stopWatching()
        val local = storage.getActiveMatch()
        _uiState.update {
            it.copy(
                isSpectatorMode = false,
                spectatorTarget = "",
                match = local,
                currentScreen = ActiveScreen.SHARING,
                statusMessage = "Exited spectator mode."
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        LiveSharingManager.stopBroadcasting()
        LiveSharingManager.stopWatching()
    }

    // Setup actions
    fun setScreen(screen: ActiveScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun updateTeamAName(name: String) {
        _uiState.update { it.copy(setup = it.setup.copy(teamAName = name)) }
    }

    fun updateTeamBName(name: String) {
        _uiState.update { it.copy(setup = it.setup.copy(teamBName = name)) }
    }

    fun updateTotalOvers(overs: Int) {
        val clamped = overs.coerceIn(1, 200)
        _uiState.update {
            val autoMax = if (it.setup.isMaxBowlerOversAuto) ((clamped + 4) / 5).coerceAtLeast(1) else it.setup.maxOversPerBowler.coerceAtMost(clamped)
            it.copy(
                setup = it.setup.copy(
                    totalOvers = clamped,
                    maxOversPerBowler = autoMax
                )
            )
        }
    }

    fun updatePlayersPerSide(count: Int) {
        val clamped = count.coerceIn(2, 30)
        val teamA = if (clamped > _uiState.value.setup.teamAPlayers.size) {
            _uiState.value.setup.teamAPlayers + (_uiState.value.setup.teamAPlayers.size until clamped).map {
                Player(UUID.randomUUID().toString(), "Player ${it + 1}")
            }
        } else {
            _uiState.value.setup.teamAPlayers.take(clamped)
        }

        val teamB = if (clamped > _uiState.value.setup.teamBPlayers.size) {
            _uiState.value.setup.teamBPlayers + (_uiState.value.setup.teamBPlayers.size until clamped).map {
                Player(UUID.randomUUID().toString(), "Player ${it + 1}")
            }
        } else {
            _uiState.value.setup.teamBPlayers.take(clamped)
        }

        _uiState.update {
            it.copy(
                setup = it.setup.copy(
                    playersPerSide = clamped,
                    teamAPlayers = teamA,
                    teamBPlayers = teamB
                )
            )
        }
    }

    fun setTossWinner(isTeamA: Boolean) {
        _uiState.update { it.copy(setup = it.setup.copy(tossWinnerIsTeamA = isTeamA)) }
    }

    fun setTossChoice(choice: TossChoice) {
        _uiState.update { it.copy(setup = it.setup.copy(tossChoice = choice)) }
    }

    fun setWideRuns(runs: Int) {
        _uiState.update { it.copy(setup = it.setup.copy(wideRuns = runs.coerceIn(0, 10))) }
    }

    fun setNoBallRuns(runs: Int) {
        _uiState.update { it.copy(setup = it.setup.copy(noBallRuns = runs.coerceIn(0, 10))) }
    }

    fun setMaxOversPerBowler(overs: Int) {
        _uiState.update {
            it.copy(
                setup = it.setup.copy(
                    maxOversPerBowler = overs.coerceIn(1, it.setup.totalOvers),
                    isMaxBowlerOversAuto = false
                )
            )
        }
    }

    fun setAutoMaxBowlerOvers() {
        _uiState.update {
            val autoMax = ((it.setup.totalOvers + 4) / 5).coerceAtLeast(1)
            it.copy(
                setup = it.setup.copy(
                    maxOversPerBowler = autoMax,
                    isMaxBowlerOversAuto = true
                )
            )
        }
    }

    fun setRebowlWide(enabled: Boolean) {
        _uiState.update { it.copy(setup = it.setup.copy(rebowlWide = enabled)) }
    }

    fun setRebowlNoBall(enabled: Boolean) {
        _uiState.update { it.copy(setup = it.setup.copy(rebowlNoBall = enabled)) }
    }

    fun setFreeHitEnabled(enabled: Boolean) {
        _uiState.update { it.copy(setup = it.setup.copy(freeHitEnabled = enabled)) }
    }

    fun updatePlayerName(teamIsA: Boolean, index: Int, newName: String) {
        _uiState.update { state ->
            val list = if (teamIsA) state.setup.teamAPlayers.toMutableList() else state.setup.teamBPlayers.toMutableList()
            if (index in list.indices) {
                list[index] = list[index].copy(name = newName)
            }
            if (teamIsA) {
                state.copy(setup = state.setup.copy(teamAPlayers = list))
            } else {
                state.copy(setup = state.setup.copy(teamBPlayers = list))
            }
        }
    }

    fun setPlayerCaptain(teamIsA: Boolean, index: Int) {
        _uiState.update { state ->
            val list = (if (teamIsA) state.setup.teamAPlayers else state.setup.teamBPlayers).mapIndexed { i, p ->
                p.copy(isCaptain = i == index)
            }
            if (teamIsA) state.copy(setup = state.setup.copy(teamAPlayers = list))
            else state.copy(setup = state.setup.copy(teamBPlayers = list))
        }
    }

    fun setPlayerWicketKeeper(teamIsA: Boolean, index: Int) {
        _uiState.update { state ->
            val list = (if (teamIsA) state.setup.teamAPlayers else state.setup.teamBPlayers).mapIndexed { i, p ->
                p.copy(isWicketKeeper = i == index)
            }
            if (teamIsA) state.copy(setup = state.setup.copy(teamAPlayers = list))
            else state.copy(setup = state.setup.copy(teamBPlayers = list))
        }
    }

    fun updatePlayerPhoto(teamIsA: Boolean, index: Int, photoPath: String?) {
        _uiState.update { state ->
            val list = (if (teamIsA) state.setup.teamAPlayers else state.setup.teamBPlayers).toMutableList()
            if (index in list.indices) {
                list[index] = list[index].copy(photoUri = photoPath)
            }
            if (teamIsA) state.copy(setup = state.setup.copy(teamAPlayers = list))
            else state.copy(setup = state.setup.copy(teamBPlayers = list))
        }
    }

    fun promptStartMatch() {
        _uiState.update { it.copy(showOpeningDialog = true) }
    }

    fun dismissOpeningDialog() {
        _uiState.update { it.copy(showOpeningDialog = false) }
    }

    fun startMatch(strikerId: String, nonStrikerId: String, openingBowlerId: String) {
        val s = _uiState.value.setup
        val teamA = Team(UUID.randomUUID().toString(), s.teamAName, s.teamAPlayers)
        val teamB = Team(UUID.randomUUID().toString(), s.teamBName, s.teamBPlayers)

        val tossWinnerId = if (s.tossWinnerIsTeamA) teamA.id else teamB.id
        val tossResult = TossResult(tossWinnerId, s.tossChoice)

        val teamABatsFirst = if (s.tossWinnerIsTeamA) {
            s.tossChoice == TossChoice.BAT
        } else {
            s.tossChoice == TossChoice.BOWL
        }

        val battingTeam = if (teamABatsFirst) teamA else teamB
        val bowlingTeam = if (teamABatsFirst) teamB else teamA

        val strikerPlayer = battingTeam.players.find { it.id == strikerId } ?: battingTeam.players[0]
        val nonStrikerPlayer = battingTeam.players.find { it.id == nonStrikerId } ?: battingTeam.players[1]
        val bowlerPlayer = bowlingTeam.players.find { it.id == openingBowlerId } ?: bowlingTeam.players[0]

        val initialBatsmen = battingTeam.players.map { p ->
            BatsmanScore(
                playerId = p.id,
                name = p.name,
                photoUri = p.photoUri,
                isBatting = (p.id == strikerPlayer.id || p.id == nonStrikerPlayer.id)
            )
        }

        val initialBowlers = listOf(
            BowlerScore(
                playerId = bowlerPlayer.id,
                name = bowlerPlayer.name,
                photoUri = bowlerPlayer.photoUri
            )
        )

        val innings1 = Innings(
            inningsNumber = 1,
            battingTeamId = battingTeam.id,
            battingTeamName = battingTeam.name,
            bowlingTeamId = bowlingTeam.id,
            bowlingTeamName = bowlingTeam.name,
            maxOvers = s.totalOvers,
            batsmen = initialBatsmen,
            bowlers = initialBowlers
        )

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val newMatch = Match(
            date = dateFormat.format(Date()),
            teamA = teamA,
            teamB = teamB,
            totalOvers = s.totalOvers,
            maxOversPerBowler = s.maxOversPerBowler,
            playersPerSide = s.playersPerSide,
            wideRuns = s.wideRuns,
            noBallRuns = s.noBallRuns,
            rebowlWide = s.rebowlWide,
            rebowlNoBall = s.rebowlNoBall,
            freeHitEnabled = s.freeHitEnabled,
            tossResult = tossResult,
            innings1 = innings1,
            currentInningsIndex = 1,
            currentStrikerId = strikerPlayer.id,
            currentNonStrikerId = nonStrikerPlayer.id,
            currentBowlerId = bowlerPlayer.id
        )

        _uiState.update {
            it.copy(
                match = newMatch,
                lastMatchTeams = Pair(teamA, teamB),
                undoStack = emptyList(),
                currentScreen = ActiveScreen.LIVE_SCORING,
                showOpeningDialog = false
            )
        }
        saveAndBroadcastMatch(newMatch)
    }

    // Live Scoring Actions
    fun recordBall(
        runsBat: Int = 0,
        extraType: ExtraType = ExtraType.NONE,
        extraRuns: Int = 0,
        isWicket: Boolean = false,
        wicketType: WicketType? = null,
        dismissedPlayerId: String? = null,
        fielderName: String? = null
    ) {
        val currentMatch = _uiState.value.match ?: return
        if (currentMatch.isFinished) return

        // Push snapshot to undo stack
        val newUndoStack = _uiState.value.undoStack + currentMatch

        val inn = currentMatch.currentInnings
        val isLegal = when (extraType) {
            ExtraType.WIDE -> !currentMatch.rebowlWide
            ExtraType.NO_BALL -> !currentMatch.rebowlNoBall
            else -> true
        }
        val ballRunsTotal = runsBat + extraRuns

        // 1. Update legal ball count & over calculations
        val newLegalBalls = if (isLegal) inn.legalBalls + 1 else inn.legalBalls
        val overIndex = inn.legalBalls / 6
        val ballInOver = (inn.legalBalls % 6) + 1

        // 2. Extras counters
        val newWides = if (extraType == ExtraType.WIDE) inn.wides + extraRuns else inn.wides
        val newNoBalls = if (extraType == ExtraType.NO_BALL) inn.noBalls + extraRuns else inn.noBalls
        val newLegByes = if (extraType == ExtraType.LEG_BYE) inn.legByes + extraRuns else inn.legByes
        val newByes = if (extraType == ExtraType.BYE) inn.byes + extraRuns else inn.byes

        // 3. Update Striker & Non-Striker stats
        val strikerId = currentMatch.currentStrikerId
        val nonStrikerId = currentMatch.currentNonStrikerId
        val bowlerId = currentMatch.currentBowlerId

        val strikerScore = inn.batsmen.find { it.playerId == strikerId }
        val bowlerScore = inn.bowlers.find { it.playerId == bowlerId } ?: BowlerScore(bowlerId, currentMatch.bowlingTeam.players.find { it.id == bowlerId }?.name ?: "Bowler")

        // Batsman runs: counts off bat (not for wide, legbye, bye)
        val batRunsToBatsman = if (extraType == ExtraType.WIDE || extraType == ExtraType.LEG_BYE || extraType == ExtraType.BYE) 0 else runsBat
        val ballsFacedIncrement = if (extraType == ExtraType.WIDE) 0 else 1

        val updatedBatsmen = inn.batsmen.map { b ->
            if (b.playerId == strikerId) {
                b.copy(
                    runs = b.runs + batRunsToBatsman,
                    balls = b.balls + ballsFacedIncrement,
                    fours = if (batRunsToBatsman == 4) b.fours + 1 else b.fours,
                    sixes = if (batRunsToBatsman == 6) b.sixes + 1 else b.sixes,
                    isBatting = true
                )
            } else b
        }.toMutableList()

        // 4. Update Bowler stats
        // Bowler runs conceded: runsBat + wides + noBalls (Leg Byes & Byes don't count against bowler)
        val runsConcededIncrement = when (extraType) {
            ExtraType.WIDE -> extraRuns
            ExtraType.NO_BALL -> extraRuns + runsBat
            ExtraType.LEG_BYE, ExtraType.BYE -> 0
            ExtraType.NONE -> runsBat
        }
        val bowlerLegalBallsIncrement = if (isLegal) 1 else 0
        val bowlerWicketsIncrement = if (isWicket && wicketType != WicketType.RUN_OUT && wicketType != WicketType.RETIRED_HURT) 1 else 0

        val updatedBowlers = inn.bowlers.map { bw ->
            if (bw.playerId == bowlerId) {
                bw.copy(
                    legalBalls = bw.legalBalls + bowlerLegalBallsIncrement,
                    runsConceded = bw.runsConceded + runsConcededIncrement,
                    wickets = bw.wickets + bowlerWicketsIncrement
                )
            } else bw
        }.toMutableList()

        if (updatedBowlers.none { it.playerId == bowlerId }) {
            updatedBowlers.add(
                BowlerScore(
                    playerId = bowlerId,
                    name = bowlerScore.name,
                    legalBalls = bowlerLegalBallsIncrement,
                    runsConceded = runsConcededIncrement,
                    wickets = bowlerWicketsIncrement
                )
            )
        }

        // 5. Build Delivery Description
        val desc = buildDeliveryDescription(
            strikerName = strikerScore?.name ?: "Striker",
            bowlerName = bowlerScore.name,
            runsBat = runsBat,
            extraType = extraType,
            extraRuns = extraRuns,
            isWicket = isWicket,
            wicketType = wicketType,
            dismissedPlayerName = inn.batsmen.find { it.playerId == dismissedPlayerId }?.name,
            fielderName = fielderName
        )

        val newDelivery = Delivery(
            id = UUID.randomUUID().toString(),
            overIndex = overIndex,
            ballInOver = ballInOver,
            legalBallNumber = newLegalBalls,
            bowlerId = bowlerId,
            bowlerName = bowlerScore.name,
            strikerId = strikerId,
            strikerName = strikerScore?.name ?: "Striker",
            nonStrikerId = nonStrikerId,
            nonStrikerName = inn.batsmen.find { it.playerId == nonStrikerId }?.name ?: "Non-Striker",
            runsBat = runsBat,
            extraRuns = extraRuns,
            extraType = extraType,
            isWicket = isWicket,
            wicketType = wicketType,
            dismissedPlayerId = dismissedPlayerId,
            dismissedPlayerName = inn.batsmen.find { it.playerId == dismissedPlayerId }?.name,
            fielderName = fielderName,
            description = desc,
            isLegalDelivery = isLegal
        )

        // 6. Handle Wicket
        val newWicketsCount = if (isWicket) inn.wickets + 1 else inn.wickets
        val updatedFows = inn.fallOfWickets.toMutableList()
        var outPlayerId = dismissedPlayerId

        if (isWicket) {
            if (outPlayerId == null) {
                outPlayerId = strikerId
            }
            val dismissalText = buildDismissalText(wicketType ?: WicketType.BOWLED, bowlerScore.name, fielderName)
            val outIndex = updatedBatsmen.indexOfFirst { it.playerId == outPlayerId }
            if (outIndex >= 0) {
                val outBatsman = updatedBatsmen[outIndex]
                updatedBatsmen[outIndex] = outBatsman.copy(
                    dismissalText = dismissalText,
                    isOut = true,
                    isBatting = false
                )
                updatedFows.add(
                    FallOfWicket(
                        wicketNum = newWicketsCount,
                        teamRuns = inn.totalRuns + ballRunsTotal,
                        overText = "${newLegalBalls / 6}.${newLegalBalls % 6}",
                        batsmanName = outBatsman.name
                    )
                )
            }
        }

        // 7. Strike Rotation Logic
        var newStrikerId = strikerId
        var newNonStrikerId = nonStrikerId

        // Did batsmen cross on physical runs (odd number of runs 1, 3, 5)?
        val physicalRuns = if (extraType == ExtraType.WIDE) (extraRuns - currentMatch.wideRuns).coerceAtLeast(0) else runsBat
        val runsForStrikeRotation = if (extraType == ExtraType.LEG_BYE || extraType == ExtraType.BYE) extraRuns else physicalRuns

        if (runsForStrikeRotation % 2 == 1) {
            val temp = newStrikerId
            newStrikerId = newNonStrikerId
            newNonStrikerId = temp
        }

        // Did the over end on this delivery?
        val overFinished = isLegal && (newLegalBalls % 6 == 0)
        var maidenOverIncrement = 0
        if (overFinished) {
            // End of over: batsmen switch ends!
            val temp = newStrikerId
            newStrikerId = newNonStrikerId
            newNonStrikerId = temp

            // Check if maiden over (0 runs conceded by bowler in the 6 balls of this over)
            val thisOverDeliveries = inn.deliveries.filter { it.overIndex == overIndex } + newDelivery
            val overRunsConceded = thisOverDeliveries.sumOf { d ->
                when (d.extraType) {
                    ExtraType.WIDE -> d.extraRuns
                    ExtraType.NO_BALL -> d.extraRuns + d.runsBat
                    ExtraType.LEG_BYE, ExtraType.BYE -> 0
                    ExtraType.NONE -> d.runsBat
                }
            }
            if (overRunsConceded == 0) {
                maidenOverIncrement = 1
                val bwIdx = updatedBowlers.indexOfFirst { it.playerId == bowlerId }
                if (bwIdx >= 0) {
                    updatedBowlers[bwIdx] = updatedBowlers[bwIdx].copy(
                        maidens = updatedBowlers[bwIdx].maidens + 1
                    )
                }
            }
        }

        val newTotalRuns = inn.totalRuns + ballRunsTotal
        val isAllOut = newWicketsCount >= currentMatch.playersPerSide - 1
        val isOversDone = newLegalBalls >= currentMatch.totalOvers * 6

        // Check if 2nd innings target achieved
        var is2ndInningsWon = false
        if (currentMatch.currentInningsIndex == 2 && inn.targetScore != null) {
            if (newTotalRuns >= inn.targetScore) {
                is2ndInningsWon = true
            }
        }

        val inningsFinished = isAllOut || isOversDone || is2ndInningsWon

        val updatedInnings = inn.copy(
            totalRuns = newTotalRuns,
            wickets = newWicketsCount,
            legalBalls = newLegalBalls,
            wides = newWides,
            noBalls = newNoBalls,
            legByes = newLegByes,
            byes = newByes,
            batsmen = updatedBatsmen,
            bowlers = updatedBowlers,
            deliveries = inn.deliveries + newDelivery,
            fallOfWickets = updatedFows,
            isCompleted = inningsFinished
        )

        // Check Match Status
        var updatedMatch = if (currentMatch.currentInningsIndex == 1) {
            currentMatch.copy(
                innings1 = updatedInnings,
                currentStrikerId = newStrikerId,
                currentNonStrikerId = newNonStrikerId
            )
        } else {
            currentMatch.copy(
                innings2 = updatedInnings,
                currentStrikerId = newStrikerId,
                currentNonStrikerId = newNonStrikerId
            )
        }

        var showNextBowler = false
        var showNewBatsman = false
        var showInningsBreak = false
        var showMatchEnd = false

        if (inningsFinished) {
            if (updatedMatch.currentInningsIndex == 1) {
                showInningsBreak = true
            } else {
                // Match Ended! Determine winner
                val inn1 = updatedMatch.innings1
                val inn2 = updatedInnings
                val result = calculateMatchResult(updatedMatch, inn1, inn2)
                val mom = calculateManOfTheMatch(updatedMatch)
                updatedMatch = updatedMatch.copy(
                    isFinished = true,
                    resultMessage = result,
                    manOfTheMatch = mom
                )
                showMatchEnd = true
                storage.saveCompletedMatch(updatedMatch)
                viewModelScope.launch {
                    SupabaseClient.uploadMatchHistory(getApplication(), updatedMatch)
                }
            }
        } else {
            if (isWicket) {
                showNewBatsman = true
            }
            if (overFinished) {
                showNextBowler = true
                updatedMatch = updatedMatch.copy(lastBowlerId = bowlerId)
            }
        }

        _uiState.update {
            it.copy(
                match = updatedMatch,
                undoStack = newUndoStack,
                showNextBowlerDialog = showNextBowler,
                showNewBatsmanDialog = showNewBatsman,
                showInningsBreakDialog = showInningsBreak,
                showMatchEndDialog = showMatchEnd
            )
        }
        saveAndBroadcastMatch(updatedMatch)
    }

    private fun calculateMatchResult(match: Match, inn1: Innings, inn2: Innings): String {
        return when {
            inn2.totalRuns >= (inn2.targetScore ?: (inn1.totalRuns + 1)) -> {
                val wicketsLeft = (match.playersPerSide - 1) - inn2.wickets
                val ballsLeft = (match.totalOvers * 6) - inn2.legalBalls
                "${inn2.battingTeamName} won by $wicketsLeft wicket${if (wicketsLeft != 1) "s" else ""} ($ballsLeft balls remaining)!"
            }
            inn2.totalRuns == inn1.totalRuns -> {
                "Match Tied!"
            }
            else -> {
                val runDiff = inn1.totalRuns - inn2.totalRuns
                "${inn1.battingTeamName} won by $runDiff run${if (runDiff != 1) "s" else ""}!"
            }
        }
    }

    private fun calculateManOfTheMatch(match: Match): String {
        // Impact points = runs * 1 + wickets * 25 + maidens * 10
        val pointsMap = mutableMapOf<String, Pair<String, Int>>() // playerId -> (name, points)

        val allInnings = listOfNotNull(match.innings1, match.innings2)
        for (inn in allInnings) {
            for (b in inn.batsmen) {
                val current = pointsMap[b.playerId]?.second ?: 0
                pointsMap[b.playerId] = Pair(b.name, current + b.runs)
            }
            for (bw in inn.bowlers) {
                val current = pointsMap[bw.playerId]?.second ?: 0
                pointsMap[bw.playerId] = Pair(bw.name, current + (bw.wickets * 25) + (bw.maidens * 10))
            }
        }

        val top = pointsMap.maxByOrNull { it.value.second }
        return top?.value?.first ?: "Captain"
    }

    private fun buildDeliveryDescription(
        strikerName: String,
        bowlerName: String,
        runsBat: Int,
        extraType: ExtraType,
        extraRuns: Int,
        isWicket: Boolean,
        wicketType: WicketType?,
        dismissedPlayerName: String?,
        fielderName: String?
    ): String {
        return when {
            isWicket -> {
                when (wicketType) {
                    WicketType.BOWLED -> "$bowlerName to $strikerName: OUT! Bowled clean through the gate!"
                    WicketType.CAUGHT -> "$bowlerName to $strikerName: OUT! Caught by ${fielderName ?: "fielder"}!"
                    WicketType.RUN_OUT -> "OUT! Run out! ${dismissedPlayerName ?: strikerName} is short of the crease!"
                    WicketType.LBW -> "$bowlerName to $strikerName: OUT! Trapped LBW right in front!"
                    WicketType.STUMPED -> "$bowlerName to $strikerName: OUT! Stumped by wicketkeeper!"
                    WicketType.HIT_WICKET -> "$bowlerName to $strikerName: OUT! Hit wicket, dislodged the bails!"
                    else -> "OUT! ${dismissedPlayerName ?: strikerName} departs!"
                }
            }
            extraType == ExtraType.WIDE -> {
                if (extraRuns > 1) "$bowlerName to $strikerName: Wide ball + ${extraRuns - 1} extra overthrows!"
                else "$bowlerName to $strikerName: Wide ball outside off!"
            }
            extraType == ExtraType.NO_BALL -> {
                if (runsBat > 0) "$bowlerName to $strikerName: No ball! Smashed for $runsBat! Free Hit next!"
                else "$bowlerName to $strikerName: No ball overstepping the popping crease!"
            }
            runsBat == 4 -> "$bowlerName to $strikerName: FOUR! Beautifully timed to the boundary ropes!"
            runsBat == 6 -> "$bowlerName to $strikerName: SIX! High and handsome over the ropes!"
            runsBat == 0 -> "$bowlerName to $strikerName: Dot ball, solid defense."
            runsBat == 1 -> "$bowlerName to $strikerName: 1 run, pushed into gaps for a single."
            else -> "$bowlerName to $strikerName: $runsBat runs taken."
        }
    }

    private fun buildDismissalText(type: WicketType, bowler: String, fielder: String?): String {
        return when (type) {
            WicketType.BOWLED -> "b $bowler"
            WicketType.CAUGHT -> if (fielder != null) "c $fielder b $bowler" else "c & b $bowler"
            WicketType.RUN_OUT -> if (fielder != null) "run out ($fielder)" else "run out"
            WicketType.LBW -> "lbw b $bowler"
            WicketType.STUMPED -> if (fielder != null) "st $fielder b $bowler" else "stumped b $bowler"
            WicketType.HIT_WICKET -> "hit wicket b $bowler"
            WicketType.RETIRED_HURT -> "retired hurt"
            WicketType.RETIRED_OUT -> "retired out"
        }
    }

    // Modal resolution handlers
    fun selectNextBowler(newBowlerId: String) {
        val currentMatch = _uiState.value.match ?: return
        val bowlerPlayer = currentMatch.bowlingTeam.players.find { it.id == newBowlerId } ?: return

        // Ensure bowler is present in inn.bowlers list
        val inn = currentMatch.currentInnings
        val updatedBowlers = inn.bowlers.toMutableList()
        if (updatedBowlers.none { it.playerId == newBowlerId }) {
            updatedBowlers.add(
                BowlerScore(playerId = newBowlerId, name = bowlerPlayer.name, photoUri = bowlerPlayer.photoUri)
            )
        }

        val updatedInn = inn.copy(bowlers = updatedBowlers)
        val updatedMatch = if (currentMatch.currentInningsIndex == 1) {
            currentMatch.copy(
                innings1 = updatedInn,
                currentBowlerId = newBowlerId
            )
        } else {
            currentMatch.copy(
                innings2 = updatedInn,
                currentBowlerId = newBowlerId
            )
        }

        _uiState.update {
            it.copy(
                match = updatedMatch,
                showNextBowlerDialog = false
            )
        }
        saveAndBroadcastMatch(updatedMatch)
    }

    fun selectIncomingBatsman(newBatsmanId: String) {
        val currentMatch = _uiState.value.match ?: return
        val inn = currentMatch.currentInnings

        // Find which batting spot is vacant (either current striker or non striker was out)
        val strikerIsOut = inn.batsmen.find { it.playerId == currentMatch.currentStrikerId }?.isOut == true

        val updatedBatsmen = inn.batsmen.map { b ->
            if (b.playerId == newBatsmanId) {
                b.copy(isBatting = true)
            } else b
        }

        val updatedInn = inn.copy(batsmen = updatedBatsmen)
        val updatedMatch = if (currentMatch.currentInningsIndex == 1) {
            currentMatch.copy(
                innings1 = updatedInn,
                currentStrikerId = if (strikerIsOut) newBatsmanId else currentMatch.currentStrikerId,
                currentNonStrikerId = if (!strikerIsOut) newBatsmanId else currentMatch.currentNonStrikerId
            )
        } else {
            currentMatch.copy(
                innings2 = updatedInn,
                currentStrikerId = if (strikerIsOut) newBatsmanId else currentMatch.currentStrikerId,
                currentNonStrikerId = if (!strikerIsOut) newBatsmanId else currentMatch.currentNonStrikerId
            )
        }

        _uiState.update {
            it.copy(
                match = updatedMatch,
                showNewBatsmanDialog = false
            )
        }
        saveAndBroadcastMatch(updatedMatch)
    }

    fun startSecondInnings(strikerId: String, nonStrikerId: String, openingBowlerId: String) {
        val currentMatch = _uiState.value.match ?: return
        val battingTeam = currentMatch.bowlingTeam // Team that bowled 1st bats 2nd
        val bowlingTeam = currentMatch.battingTeam

        val strikerPlayer = battingTeam.players.find { it.id == strikerId } ?: battingTeam.players[0]
        val nonStrikerPlayer = battingTeam.players.find { it.id == nonStrikerId } ?: battingTeam.players[1]
        val bowlerPlayer = bowlingTeam.players.find { it.id == openingBowlerId } ?: bowlingTeam.players[0]

        val initialBatsmen = battingTeam.players.map { p ->
            BatsmanScore(
                playerId = p.id,
                name = p.name,
                photoUri = p.photoUri,
                isBatting = (p.id == strikerPlayer.id || p.id == nonStrikerPlayer.id)
            )
        }

        val initialBowlers = listOf(
            BowlerScore(
                playerId = bowlerPlayer.id,
                name = bowlerPlayer.name,
                photoUri = bowlerPlayer.photoUri
            )
        )

        val target = currentMatch.innings1.totalRuns + 1

        val innings2 = Innings(
            inningsNumber = 2,
            battingTeamId = battingTeam.id,
            battingTeamName = battingTeam.name,
            bowlingTeamId = bowlingTeam.id,
            bowlingTeamName = bowlingTeam.name,
            targetScore = target,
            maxOvers = currentMatch.totalOvers,
            batsmen = initialBatsmen,
            bowlers = initialBowlers
        )

        val updatedMatch = currentMatch.copy(
            innings2 = innings2,
            currentInningsIndex = 2,
            currentStrikerId = strikerPlayer.id,
            currentNonStrikerId = nonStrikerPlayer.id,
            currentBowlerId = bowlerPlayer.id,
            lastBowlerId = null
        )

        _uiState.update {
            it.copy(
                match = updatedMatch,
                showInningsBreakDialog = false
            )
        }
        saveAndBroadcastMatch(updatedMatch)
    }

    fun rotateStrikeManually() {
        val currentMatch = _uiState.value.match ?: return
        val temp = currentMatch.currentStrikerId
        val updated = currentMatch.copy(
            currentStrikerId = currentMatch.currentNonStrikerId,
            currentNonStrikerId = temp
        )
        _uiState.update { it.copy(match = updated) }
        saveAndBroadcastMatch(updated)
    }

    fun undoLastDelivery() {
        val stack = _uiState.value.undoStack
        if (stack.isEmpty()) return
        val previousMatch = stack.last()
        val newStack = stack.dropLast(1)
        _uiState.update {
            it.copy(
                match = previousMatch,
                undoStack = newStack,
                showNextBowlerDialog = false,
                showNewBatsmanDialog = false,
                showInningsBreakDialog = false,
                showMatchEndDialog = false
            )
        }
        saveAndBroadcastMatch(previousMatch)
    }

    fun resetMatch() {
        storage.saveActiveMatch(null)
        _uiState.update {
            it.copy(
                match = null,
                undoStack = emptyList(),
                currentScreen = ActiveScreen.SETUP,
                showMatchEndDialog = false,
                showInningsBreakDialog = false,
                showNewGameDialog = false
            )
        }
    }

    // --- Saved Teams & Team Management ---
    fun loadSavedTeams() {
        val teams = storage.getSavedTeams()
        _uiState.update { it.copy(savedTeams = teams) }
    }

    fun loadLastMatchTeams() {
        val lastTeams = storage.getLastMatchTeams()
        _uiState.update { it.copy(lastMatchTeams = lastTeams) }
    }

    fun openSavedTeamsDialog(targetIsTeamA: Boolean) {
        loadSavedTeams()
        _uiState.update {
            it.copy(
                showSavedTeamsDialog = true,
                savedTeamsTargetIsTeamA = targetIsTeamA
            )
        }
    }

    fun dismissSavedTeamsDialog() {
        _uiState.update { it.copy(showSavedTeamsDialog = false) }
    }

    fun openSaveTeamDialog(isTeamA: Boolean) {
        _uiState.update {
            it.copy(
                showSaveTeamDialog = true,
                saveTeamIsTeamA = isTeamA
            )
        }
    }

    fun dismissSaveTeamDialog() {
        _uiState.update { it.copy(showSaveTeamDialog = false) }
    }

    fun openNewGameDialog() {
        _uiState.update { it.copy(showNewGameDialog = true) }
    }

    fun dismissNewGameDialog() {
        _uiState.update { it.copy(showNewGameDialog = false) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun saveTeam(name: String, players: List<Player>) {
        val cleanName = name.trim().ifEmpty { "My Team" }
        val teamToSave = Team(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            players = players.map { it.copy(id = UUID.randomUUID().toString()) }
        )
        storage.saveTeam(teamToSave)
        viewModelScope.launch {
            SupabaseClient.uploadTeam(getApplication(), teamToSave)
        }
        loadSavedTeams()
        _uiState.update {
            it.copy(
                showSaveTeamDialog = false,
                statusMessage = "Team '$cleanName' saved to My Teams!"
            )
        }
    }

    fun saveCurrentTeam(isTeamA: Boolean, customName: String? = null) {
        val s = _uiState.value.setup
        val defaultName = if (isTeamA) s.teamAName else s.teamBName
        val targetName = customName?.trim()?.ifEmpty { defaultName } ?: defaultName
        val players = if (isTeamA) s.teamAPlayers else s.teamBPlayers
        saveTeam(targetName, players)
    }

    fun deleteSavedTeam(teamId: String) {
        storage.deleteSavedTeam(teamId)
        loadSavedTeams()
        _uiState.update { it.copy(statusMessage = "Team deleted") }
    }

    fun updateOrSaveTeam(team: Team) {
        val cleanName = team.name.trim().ifEmpty { "My Team" }
        val teamToSave = team.copy(name = cleanName)
        storage.saveTeam(teamToSave)
        viewModelScope.launch {
            SupabaseClient.uploadTeam(getApplication(), teamToSave)
        }
        loadSavedTeams()
        _uiState.update {
            it.copy(
                statusMessage = "Team '$cleanName' saved!"
            )
        }
    }

    fun loadTeamIntoSetup(team: Team, isTeamA: Boolean) {
        val clonedPlayers = team.players.map { it.copy(id = UUID.randomUUID().toString()) }
        _uiState.update { state ->
            val curSetup = state.setup
            val newPlayersPerSide = if (team.players.isNotEmpty()) {
                val otherTeamSize = if (isTeamA) curSetup.teamBPlayers.size else curSetup.teamAPlayers.size
                maxOf(clonedPlayers.size, otherTeamSize).coerceIn(2, 30)
            } else {
                curSetup.playersPerSide
            }
            if (isTeamA) {
                state.copy(
                    setup = curSetup.copy(
                        teamAName = team.name,
                        teamAPlayers = clonedPlayers,
                        playersPerSide = newPlayersPerSide
                    ),
                    showSavedTeamsDialog = false,
                    statusMessage = "Loaded ${team.name} as Team 1"
                )
            } else {
                state.copy(
                    setup = curSetup.copy(
                        teamBName = team.name,
                        teamBPlayers = clonedPlayers,
                        playersPerSide = newPlayersPerSide
                    ),
                    showSavedTeamsDialog = false,
                    statusMessage = "Loaded ${team.name} as Team 2"
                )
            }
        }
    }

    fun loadMatchTeamsIntoSetup(teamA: Team, teamB: Team) {
        val clonedA = teamA.players.map { it.copy(id = UUID.randomUUID().toString()) }
        val clonedB = teamB.players.map { it.copy(id = UUID.randomUUID().toString()) }
        val pCount = maxOf(clonedA.size, clonedB.size).coerceIn(2, 30)

        _uiState.update { state ->
            state.copy(
                setup = state.setup.copy(
                    teamAName = teamA.name,
                    teamBName = teamB.name,
                    teamAPlayers = clonedA,
                    teamBPlayers = clonedB,
                    playersPerSide = pCount
                ),
                currentScreen = ActiveScreen.SETUP,
                statusMessage = "Loaded teams: ${teamA.name} vs ${teamB.name}"
            )
        }
    }

    fun swapTeams() {
        _uiState.update { state ->
            val s = state.setup
            state.copy(
                setup = s.copy(
                    teamAName = s.teamBName,
                    teamBName = s.teamAName,
                    teamAPlayers = s.teamBPlayers,
                    teamBPlayers = s.teamAPlayers,
                    tossWinnerIsTeamA = !s.tossWinnerIsTeamA
                ),
                statusMessage = "Teams swapped!"
            )
        }
    }

    fun startNewGameWithSameTeams(directToOpening: Boolean = false) {
        val currentMatch = _uiState.value.match
        val lastTeams = storage.getLastMatchTeams()
        val s = _uiState.value.setup

        // If match was completed, ensure it's saved in history
        if (currentMatch != null && currentMatch.isFinished) {
            storage.saveCompletedMatch(currentMatch)
        }

        // Determine teams to carry forward
        val teamA = when {
            currentMatch != null -> currentMatch.teamA
            lastTeams != null -> lastTeams.first
            else -> Team(UUID.randomUUID().toString(), s.teamAName, s.teamAPlayers)
        }
        val teamB = when {
            currentMatch != null -> currentMatch.teamB
            lastTeams != null -> lastTeams.second
            else -> Team(UUID.randomUUID().toString(), s.teamBName, s.teamBPlayers)
        }

        // Store as last match teams
        storage.saveLastMatchTeams(teamA, teamB)
        storage.saveActiveMatch(null)

        val clonedA = teamA.players.map { it.copy(id = UUID.randomUUID().toString()) }
        val clonedB = teamB.players.map { it.copy(id = UUID.randomUUID().toString()) }
        val pCount = maxOf(clonedA.size, clonedB.size).coerceIn(2, 30)

        _uiState.update { state ->
            val updatedSetup = state.setup.copy(
                teamAName = teamA.name,
                teamBName = teamB.name,
                teamAPlayers = clonedA,
                teamBPlayers = clonedB,
                playersPerSide = pCount
            )
            state.copy(
                match = null,
                undoStack = emptyList(),
                setup = updatedSetup,
                lastMatchTeams = Pair(teamA, teamB),
                showMatchEndDialog = false,
                showNewGameDialog = false,
                showInningsBreakDialog = false,
                currentScreen = if (directToOpening) ActiveScreen.LIVE_SCORING else ActiveScreen.SETUP,
                showOpeningDialog = directToOpening,
                statusMessage = "Ready for new match with ${teamA.name} and ${teamB.name}!"
            )
        }
    }

    fun openWicketDialog() {
        _uiState.update { it.copy(showWicketDialog = true) }
    }

    fun dismissWicketDialog() {
        _uiState.update { it.copy(showWicketDialog = false) }
    }

    fun openCustomRunsDialog() {
        _uiState.update { it.copy(showCustomRunsDialog = true) }
    }

    fun dismissCustomRunsDialog() {
        _uiState.update { it.copy(showCustomRunsDialog = false) }
    }

    fun dismissMatchEndDialog() {
        _uiState.update { it.copy(showMatchEndDialog = false) }
    }

    // Share scorecard / match report (opens PNG/JPG/Text share dialog)
    fun shareScorecard(context: Context) {
        val match = _uiState.value.match
        if (match == null) {
            _uiState.update { it.copy(statusMessage = "No match available to share!") }
            return
        }
        _uiState.update { it.copy(showShareReportDialog = true) }
    }

    fun promptShareReport() {
        val match = _uiState.value.match
        if (match == null) {
            _uiState.update { it.copy(statusMessage = "No match available to share!") }
            return
        }
        _uiState.update { it.copy(showShareReportDialog = true) }
    }

    fun shareSpecificMatch(match: Match) {
        _uiState.update {
            it.copy(
                match = match,
                showShareReportDialog = true
            )
        }
    }

    fun dismissShareReportDialog() {
        _uiState.update { it.copy(showShareReportDialog = false) }
    }

    fun shareReportAsImage(context: Context, format: com.cricket.scoreboard.export.ReportImageFormat) {
        val match = _uiState.value.match ?: return
        val summaryText = buildScorecardShareText(match)
        com.cricket.scoreboard.export.MatchReportGenerator.shareReportImage(
            context = context,
            match = match,
            format = format,
            textCaption = summaryText
        )
    }

    fun saveReportToGallery(context: Context, format: com.cricket.scoreboard.export.ReportImageFormat) {
        val match = _uiState.value.match ?: return
        com.cricket.scoreboard.export.MatchReportGenerator.saveReportToGallery(
            context = context,
            match = match,
            format = format
        )
    }

    fun shareScorecardTextOnly(context: Context) {
        val match = _uiState.value.match ?: return
        val text = buildScorecardShareText(match)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Cricket Scorecard")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    fun getHistory(): List<Match> {
        return storage.getMatchHistory()
    }

    fun loadMatchFromHistory(match: Match) {
        _uiState.update {
            it.copy(
                match = match,
                currentScreen = ActiveScreen.FULL_SCORECARD
            )
        }
    }

    fun deleteMatchFromHistory(matchId: String) {
        storage.deleteMatchFromHistory(matchId)
        _uiState.update { it.copy() } // triggers recomposition
    }

    private fun buildScorecardShareText(m: Match): String {
        val sb = StringBuilder()
        sb.append("🏏 *CRICKET MATCH SCORECARD* 🏏\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("${m.teamA.name} vs ${m.teamB.name}\n")
        sb.append("Date: ${m.date}\n")
        sb.append("Toss: ${if (m.tossResult.winnerTeamId == m.teamA.id) m.teamA.name else m.teamB.name} elected to ${m.tossResult.choice.label}\n\n")

        // Innings 1
        val inn1 = m.innings1
        sb.append("📊 *1st Innings: ${inn1.battingTeamName}*\n")
        sb.append("Score: *${inn1.totalRuns}/${inn1.wickets}* in ${inn1.oversText} ov (CRR: ${inn1.formattedCrr})\n")
        val topBat1 = inn1.batsmen.filter { it.runs > 0 || it.balls > 0 }.sortedByDescending { it.runs }.take(3)
        if (topBat1.isNotEmpty()) {
            sb.append("Top Batsmen:\n")
            topBat1.forEach { sb.append("• ${it.name}: ${it.runs} (${it.balls}) [4s:${it.fours}, 6s:${it.sixes}]\n") }
        }
        val topBw1 = inn1.bowlers.filter { it.legalBalls > 0 }.sortedByDescending { it.wickets }.take(2)
        if (topBw1.isNotEmpty()) {
            sb.append("Best Bowling:\n")
            topBw1.forEach { sb.append("• ${it.name}: ${it.wickets}/${it.runsConceded} (${it.oversText} ov)\n") }
        }
        sb.append("\n")

        // Innings 2
        val inn2 = m.innings2
        if (inn2 != null) {
            sb.append("📊 *2nd Innings: ${inn2.battingTeamName}*\n")
            sb.append("Score: *${inn2.totalRuns}/${inn2.wickets}* in ${inn2.oversText} ov (Target: ${inn2.targetScore})\n")
            val topBat2 = inn2.batsmen.filter { it.runs > 0 || it.balls > 0 }.sortedByDescending { it.runs }.take(3)
            if (topBat2.isNotEmpty()) {
                sb.append("Top Batsmen:\n")
                topBat2.forEach { sb.append("• ${it.name}: ${it.runs} (${it.balls}) [4s:${it.fours}, 6s:${it.sixes}]\n") }
            }
            val topBw2 = inn2.bowlers.filter { it.legalBalls > 0 }.sortedByDescending { it.wickets }.take(2)
            if (topBw2.isNotEmpty()) {
                sb.append("Best Bowling:\n")
                topBw2.forEach { sb.append("• ${it.name}: ${it.wickets}/${it.runsConceded} (${it.oversText} ov)\n") }
            }
            sb.append("\n")
        }

        if (m.resultMessage != null) {
            sb.append("🏆 *Result:* ${m.resultMessage}\n")
        }
        if (m.manOfTheMatch != null) {
            sb.append("🌟 *Player of the Match:* ${m.manOfTheMatch}\n")
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("Scored on Cricket Scoreboard App 📱")
        return sb.toString()
    }
}
