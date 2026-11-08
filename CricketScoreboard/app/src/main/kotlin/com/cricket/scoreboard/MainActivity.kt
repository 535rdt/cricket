package com.cricket.scoreboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cricket.scoreboard.ui.dialogs.*
import com.cricket.scoreboard.ui.screens.*
import com.cricket.scoreboard.ui.theme.CricketScoreboardTheme
import com.cricket.scoreboard.ui.theme.PitchGold
import com.cricket.scoreboard.ui.theme.TurfGreenDark
import com.cricket.scoreboard.ui.theme.TurfGreenLight
import com.cricket.scoreboard.viewmodel.ActiveScreen
import com.cricket.scoreboard.viewmodel.CricketViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: CricketViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CricketScoreboardTheme {
                val context = LocalContext.current
                val uiState by viewModel.uiState.collectAsState()
                val currentScreen = uiState.currentScreen
                val match = uiState.match
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(uiState.statusMessage) {
                    uiState.statusMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearStatusMessage()
                    }
                }

                Scaffold(
                    snackbarHost = {
                        SnackbarHost(snackbarHostState) { data ->
                            Snackbar(
                                containerColor = TurfGreenDark,
                                contentColor = Color.White,
                                shape = RoundedCornerShape(12.dp),
                                snackbarData = data
                            )
                        }
                    },
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = if (uiState.isSpectatorMode) "🟢 Spectating Match" else "🏏 Cricket Scoreboard",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = TurfGreenDark,
                                titleContentColor = Color.White,
                                actionIconContentColor = Color.White
                            ),
                            actions = {
                                if (uiState.isSpectatorMode) {
                                    IconButton(onClick = { viewModel.exitSpectatorMode() }) {
                                        Icon(Icons.Default.ExitToApp, contentDescription = "Exit Spectator Mode", tint = Color.White)
                                    }
                                } else {
                                    // Watch Live button (for joining other matches)
                                    IconButton(onClick = { viewModel.openWatchLiveDialog() }) {
                                        Icon(Icons.Default.Sensors, contentDescription = "Watch Live Match", tint = Color.White)
                                    }
                                    if (match != null) {
                                        // Live Broadcast button
                                        IconButton(onClick = { viewModel.openLiveSharingDialog() }) {
                                            Icon(
                                                Icons.Default.Podcasts,
                                                contentDescription = "Live Sharing",
                                                tint = if (uiState.isBroadcasting) PitchGold else Color.White
                                            )
                                        }
                                        IconButton(onClick = { viewModel.shareScorecard(context) }) {
                                            Icon(Icons.Default.Share, contentDescription = "Share Scorecard")
                                        }
                                        IconButton(onClick = { viewModel.openNewGameDialog() }) {
                                            Icon(Icons.Default.Add, contentDescription = "New Match")
                                        }
                                    }
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == ActiveScreen.SETUP,
                                onClick = { viewModel.setScreen(ActiveScreen.SETUP) },
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                label = { Text("Setup", fontSize = 10.sp) }
                            )

                            NavigationBarItem(
                                selected = currentScreen == ActiveScreen.LIVE_SCORING,
                                onClick = {
                                    if (match != null) viewModel.setScreen(ActiveScreen.LIVE_SCORING)
                                },
                                enabled = match != null,
                                icon = { Icon(Icons.Default.SportsCricket, contentDescription = null) },
                                label = { Text("Live", fontSize = 10.sp) }
                            )

                            NavigationBarItem(
                                selected = currentScreen == ActiveScreen.FULL_SCORECARD,
                                onClick = {
                                    if (match != null) viewModel.setScreen(ActiveScreen.FULL_SCORECARD)
                                },
                                enabled = match != null,
                                icon = { Icon(Icons.Default.TableChart, contentDescription = null) },
                                label = { Text("Scorecard", fontSize = 10.sp) }
                            )

                            NavigationBarItem(
                                selected = currentScreen == ActiveScreen.TEAMS,
                                onClick = { viewModel.setScreen(ActiveScreen.TEAMS) },
                                icon = { Icon(Icons.Default.Groups, contentDescription = null) },
                                label = { Text("Teams", fontSize = 10.sp) }
                            )

                            NavigationBarItem(
                                selected = currentScreen == ActiveScreen.SHARING,
                                onClick = { viewModel.setScreen(ActiveScreen.SHARING) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (uiState.isSpectatorMode) {
                                                Badge(containerColor = TurfGreenLight)
                                            } else if (uiState.isBroadcasting) {
                                                Badge(containerColor = PitchGold)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            if (uiState.isSpectatorMode) Icons.Default.LiveTv else Icons.Default.Podcasts,
                                            contentDescription = "Sharing"
                                        )
                                    }
                                },
                                label = { Text("Sharing", fontSize = 10.sp) }
                            )

                            NavigationBarItem(
                                selected = currentScreen == ActiveScreen.HISTORY,
                                onClick = { viewModel.setScreen(ActiveScreen.HISTORY) },
                                icon = { Icon(Icons.Default.History, contentDescription = null) },
                                label = { Text("History", fontSize = 10.sp) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            ActiveScreen.SETUP -> {
                                MatchSetupScreen(
                                    viewModel = viewModel,
                                    onStartMatchClick = { viewModel.promptStartMatch() }
                                )
                            }
                            ActiveScreen.LIVE_SCORING -> {
                                LiveScoreboardScreen(
                                    viewModel = viewModel,
                                    onViewScorecardClick = { viewModel.setScreen(ActiveScreen.FULL_SCORECARD) },
                                    onViewTeamsClick = { viewModel.setScreen(ActiveScreen.TEAMS) }
                                )
                            }
                            ActiveScreen.FULL_SCORECARD -> {
                                FullScorecardScreen(
                                    viewModel = viewModel,
                                    onBackClick = { viewModel.setScreen(ActiveScreen.LIVE_SCORING) }
                                )
                            }
                            ActiveScreen.TEAMS -> {
                                TeamsScreen(
                                    viewModel = viewModel,
                                    onNavigateToSetup = { viewModel.setScreen(ActiveScreen.SETUP) }
                                )
                            }
                            ActiveScreen.SHARING -> {
                                SharingScreen(
                                    viewModel = viewModel,
                                    onNavigateToSetup = { viewModel.setScreen(ActiveScreen.SETUP) }
                                )
                            }
                            ActiveScreen.HISTORY -> {
                                MatchHistoryScreen(
                                    viewModel = viewModel,
                                    onSelectMatch = { viewModel.loadMatchFromHistory(it) }
                                )
                            }
                        }

                        // Dialogs
                        if (uiState.showOpeningDialog) {
                            val s = uiState.setup
                            val tA = com.cricket.scoreboard.model.Team("tempA", s.teamAName, s.teamAPlayers)
                            val tB = com.cricket.scoreboard.model.Team("tempB", s.teamBName, s.teamBPlayers)
                            val teamABats = if (s.tossWinnerIsTeamA) s.tossChoice == com.cricket.scoreboard.model.TossChoice.BAT else s.tossChoice == com.cricket.scoreboard.model.TossChoice.BOWL
                            val battingTeam = if (teamABats) tA else tB
                            val bowlingTeam = if (teamABats) tB else tA

                            OpeningPlayersDialog(
                                battingTeam = battingTeam,
                                bowlingTeam = bowlingTeam,
                                onConfirm = { sId, nsId, bId -> viewModel.startMatch(sId, nsId, bId) },
                                onDismiss = { viewModel.dismissOpeningDialog() }
                            )
                        }

                        if (uiState.showNextBowlerDialog && match != null) {
                            NextBowlerDialog(
                                bowlingTeam = match.bowlingTeam,
                                currentBowlerId = match.currentBowlerId,
                                lastBowlerId = match.lastBowlerId,
                                innings = match.currentInnings,
                                maxOversPerBowler = match.maxOversPerBowler,
                                onConfirm = { newBowlerId -> viewModel.selectNextBowler(newBowlerId) }
                            )
                        }

                        if (uiState.showNewBatsmanDialog && match != null) {
                            NewBatsmanDialog(
                                battingTeam = match.battingTeam,
                                currentInnings = match.currentInnings,
                                onConfirm = { newBatsmanId -> viewModel.selectIncomingBatsman(newBatsmanId) }
                            )
                        }

                        if (uiState.showWicketDialog && match != null) {
                            val striker = match.currentInnings.batsmen.find { it.playerId == match.currentStrikerId }
                            val nonStriker = match.currentInnings.batsmen.find { it.playerId == match.currentNonStrikerId }
                            WicketDialog(
                                striker = striker,
                                nonStriker = nonStriker,
                                bowlingTeam = match.bowlingTeam,
                                onConfirm = { wType, outId, fName, runsComp ->
                                    viewModel.dismissWicketDialog()
                                    viewModel.recordBall(
                                        runsBat = runsComp,
                                        isWicket = true,
                                        wicketType = wType,
                                        dismissedPlayerId = outId,
                                        fielderName = fName
                                    )
                                },
                                onDismiss = { viewModel.dismissWicketDialog() }
                            )
                        }

                        if (uiState.showInningsBreakDialog && match != null) {
                            InningsBreakDialog(
                                innings1 = match.innings1,
                                teamA = match.teamA,
                                teamB = match.teamB,
                                onStartSecondInnings = { sId, nsId, bId ->
                                    viewModel.startSecondInnings(sId, nsId, bId)
                                }
                            )
                        }

                        if (uiState.showMatchEndDialog && match != null) {
                            MatchEndDialog(
                                match = match,
                                onViewScorecard = {
                                    viewModel.dismissMatchEndDialog()
                                    viewModel.setScreen(ActiveScreen.FULL_SCORECARD)
                                },
                                onShare = { viewModel.shareScorecard(context) },
                                onRematchWithSameTeams = {
                                    viewModel.startNewGameWithSameTeams(directToOpening = false)
                                },
                                onSetupWithSameTeams = {
                                    viewModel.startNewGameWithSameTeams(directToOpening = false)
                                },
                                onNewMatchFresh = { viewModel.resetMatch() },
                                onDismiss = { viewModel.dismissMatchEndDialog() }
                            )
                        }

                        if (uiState.showNewGameDialog) {
                            val nameA = match?.teamA?.name ?: uiState.setup.teamAName
                            val nameB = match?.teamB?.name ?: uiState.setup.teamBName
                            NewGameChoiceDialog(
                                teamAName = nameA,
                                teamBName = nameB,
                                onRematchWithSameTeams = {
                                    viewModel.startNewGameWithSameTeams(directToOpening = false)
                                },
                                onSetupWithSameTeams = {
                                    viewModel.startNewGameWithSameTeams(directToOpening = false)
                                },
                                onFreshMatch = { viewModel.resetMatch() },
                                onDismiss = { viewModel.dismissNewGameDialog() }
                            )
                        }

                        if (uiState.showSavedTeamsDialog) {
                            SavedTeamsDialog(
                                savedTeams = uiState.savedTeams,
                                targetIsTeamA = uiState.savedTeamsTargetIsTeamA,
                                onLoadTeam = { team, isTeamA ->
                                    viewModel.loadTeamIntoSetup(team, isTeamA)
                                },
                                onDeleteTeam = { teamId ->
                                    viewModel.deleteSavedTeam(teamId)
                                },
                                onDismiss = { viewModel.dismissSavedTeamsDialog() }
                            )
                        }

                        if (uiState.showSaveTeamDialog) {
                            val isA = uiState.saveTeamIsTeamA
                            val defName = if (isA) uiState.setup.teamAName else uiState.setup.teamBName
                            val players = if (isA) uiState.setup.teamAPlayers else uiState.setup.teamBPlayers
                            SaveTeamDialog(
                                initialName = defName,
                                players = players,
                                isTeamA = isA,
                                onConfirm = { name ->
                                    viewModel.saveCurrentTeam(isA, name)
                                },
                                onDismiss = { viewModel.dismissSaveTeamDialog() }
                            )
                        }

                        if (uiState.showCustomRunsDialog) {
                            CustomRunsDialog(
                                onConfirm = { rBat, eType, eRuns ->
                                    viewModel.dismissCustomRunsDialog()
                                    viewModel.recordBall(runsBat = rBat, extraType = eType, extraRuns = eRuns)
                                },
                                onDismiss = { viewModel.dismissCustomRunsDialog() }
                            )
                        }

                        if (uiState.showLiveSharingDialog) {
                            LiveSharingDialog(
                                match = match,
                                isBroadcasting = uiState.isBroadcasting,
                                matchCode = uiState.broadcastMatchCode,
                                webUrl = uiState.broadcastWebUrl,
                                onToggleBroadcast = { enable -> viewModel.toggleLiveBroadcast(enable) },
                                onDismiss = { viewModel.dismissLiveSharingDialog() }
                            )
                        }

                        if (uiState.showWatchLiveDialog) {
                            WatchLiveDialog(
                                isLoading = uiState.liveViewerLoading,
                                errorMessage = uiState.liveViewerError,
                                onConnect = { code -> viewModel.connectToLiveMatch(code) },
                                onDismiss = { viewModel.dismissWatchLiveDialog() }
                            )
                        }

                        if (uiState.showShareReportDialog && match != null) {
                            ShareReportDialog(
                                match = match,
                                onShareImage = { format ->
                                    viewModel.shareReportAsImage(context, format)
                                    viewModel.dismissShareReportDialog()
                                },
                                onSaveToGallery = { format ->
                                    viewModel.saveReportToGallery(context, format)
                                },
                                onShareText = {
                                    viewModel.shareScorecardTextOnly(context)
                                    viewModel.dismissShareReportDialog()
                                },
                                onDismiss = { viewModel.dismissShareReportDialog() }
                            )
                        }
                    }
                }
            }
        }
    }
}
