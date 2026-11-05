package com.cricket.scoreboard.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cricket.scoreboard.model.*
import com.cricket.scoreboard.ui.components.PlayerAvatar
import com.cricket.scoreboard.ui.theme.*
import com.cricket.scoreboard.viewmodel.CricketViewModel

@Composable
fun LiveScoreboardScreen(
    viewModel: CricketViewModel,
    onViewScorecardClick: () -> Unit,
    onViewTeamsClick: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val match = uiState.match ?: return
    val inn = match.currentInnings

    val striker = inn.batsmen.find { it.playerId == match.currentStrikerId }
    val nonStriker = inn.batsmen.find { it.playerId == match.currentNonStrikerId }
    val bowler = inn.bowlers.find { it.playerId == match.currentBowlerId } ?: BowlerScore(match.currentBowlerId, "Bowler")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Spectator Mode Banner
        if (uiState.isSpectatorMode) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = WicketRed.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, WicketRed.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(shape = CircleShape, color = WicketRed, modifier = Modifier.size(8.dp)) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🔴 SPECTATING LIVE • ${uiState.spectatorTarget}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = WicketRed
                        )
                    }
                    TextButton(
                        onClick = { viewModel.exitSpectatorMode() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Exit", fontSize = 11.sp, color = WicketRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Top Banner / Scorecard Header
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = TurfGreenDark),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Teams & Innings Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = inn.battingTeamName.uppercase(),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "vs ${inn.bowlingTeamName} • Innings ${match.currentInningsIndex} of 2",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        color = PitchGold,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${match.totalOvers} OVERS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Big Score Display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${inn.totalRuns}",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "/${inn.wickets}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = PitchGold,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Overs: ${inn.oversText} / ${match.totalOvers}.0",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "CRR: ${inn.formattedCrr}",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                // Target & Chase Info (for 2nd innings)
                if (match.currentInningsIndex == 2 && inn.targetScore != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color.Black.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val reqRuns = match.requiredRuns ?: 0
                            val remBalls = match.remainingBalls ?: 0
                            Text(
                                text = "🎯 Target: ${inn.targetScore} (Need $reqRuns from $remBalls b)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PitchGold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "RRR: ${match.formattedRrr ?: "-"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    // Projected score for 1st innings
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Projected Total: ${inn.projectedScore} at current run rate",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }

                // Live Sharing Bar (for Scorer)
                if (!uiState.isSpectatorMode) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (uiState.isBroadcasting) TurfGreenMedium.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, if (uiState.isBroadcasting) PitchGold else Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openLiveSharingDialog() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                if (uiState.isBroadcasting) {
                                    Surface(shape = CircleShape, color = WicketRed, modifier = Modifier.size(8.dp)) {}
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "🔴 BROADCASTING LIVE: ${uiState.broadcastMatchCode}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PitchGold
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Podcasts,
                                        contentDescription = null,
                                        tint = PitchGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "📡 Share Live Scoreboard with Other Teams",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Text(
                                text = if (uiState.isBroadcasting) "Manage ▶" else "Share ▶",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PitchGold
                            )
                        }
                    }
                }
            }
        }

        // Match Completed Banner with Rematch Button
        if (match.isFinished) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PitchGold.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🏆 MATCH COMPLETED 🏆",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = PitchGold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = match.resultMessage ?: "",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.startNewGameWithSameTeams(directToOpening = false) },
                        colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "🔁 Start New Game with Same Teams",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Active Batsmen Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏏 Batsmen",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (!uiState.isSpectatorMode) {
                        OutlinedButton(
                            onClick = { viewModel.rotateStrikeManually() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Swap Strike", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Table Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Batter", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(2f))
                    Text("R", fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                    Text("B", fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                    Text("4s", fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                    Text("6s", fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                    Text("SR", fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                // Striker Row
                striker?.let { s ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(2f), verticalAlignment = Alignment.CenterVertically) {
                            PlayerAvatar(
                                photoUri = s.photoUri,
                                name = s.name,
                                size = 26.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🏏", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${s.name}*",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TurfGreenMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text("${s.runs}", fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                        Text("${s.balls}", fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                        Text("${s.fours}", fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                        Text("${s.sixes}", fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                        Text(s.formattedStrikeRate, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Non-Striker Row
                nonStriker?.let { ns ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(2f), verticalAlignment = Alignment.CenterVertically) {
                            PlayerAvatar(
                                photoUri = ns.photoUri,
                                name = ns.name,
                                size = 26.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = ns.name,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text("${ns.runs}", fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                        Text("${ns.balls}", fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                        Text("${ns.fours}", fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                        Text("${ns.sixes}", fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                        Text(ns.formattedStrikeRate, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Active Bowler Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚾ Current Bowler",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (!uiState.isSpectatorMode) {
                        OutlinedButton(
                            onClick = { viewModel.promptStartMatch() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Change", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Bowler", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(2f))
                    Text("O", fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                    Text("M", fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                    Text("R", fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                    Text("W", fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                    Text("Econ", fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(2f), verticalAlignment = Alignment.CenterVertically) {
                        PlayerAvatar(
                            photoUri = bowler.photoUri,
                            name = bowler.name,
                            size = 26.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = bowler.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(bowler.oversText, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                    Text("${bowler.maidens}", fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                    Text("${bowler.runsConceded}", fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                    Text("${bowler.wickets}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WicketRed, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                    Text(bowler.formattedEconomy, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                }
            }
        }

        // Current Over Balls Strip
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "This Over (Over ${(inn.legalBalls / 6) + 1}):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                val thisOverBalls = inn.currentOverDeliveries
                if (thisOverBalls.isEmpty()) {
                    Text("Starting over...", fontSize = 13.sp, color = Color.Gray)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(thisOverBalls) { delivery ->
                            DeliveryBadge(delivery = delivery)
                        }
                    }
                }
            }
        }

        // SCORING KEYPAD (Hidden when in Spectator Mode)
        if (uiState.isSpectatorMode) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "👁️ Spectator Mode Active",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "You are watching this match live. Score, wickets, and overs update automatically in real-time as the scorer records each ball.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = { viewModel.exitSpectatorMode() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WicketRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exit Spectator Mode", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "⚡ Scoring Keypad",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Main Run Buttons: 0, 1, 2, 3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScoreButton(label = "0", sublabel = "Dot", color = MaterialTheme.colorScheme.surfaceVariant, textColor = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f)) {
                            viewModel.recordBall(runsBat = 0)
                        }
                        ScoreButton(label = "1", sublabel = "Single", color = MaterialTheme.colorScheme.surfaceVariant, textColor = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f)) {
                            viewModel.recordBall(runsBat = 1)
                        }
                        ScoreButton(label = "2", sublabel = "Two", color = MaterialTheme.colorScheme.surfaceVariant, textColor = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f)) {
                            viewModel.recordBall(runsBat = 2)
                        }
                        ScoreButton(label = "3", sublabel = "Three", color = MaterialTheme.colorScheme.surfaceVariant, textColor = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f)) {
                            viewModel.recordBall(runsBat = 3)
                        }
                    }

                    // Boundaries: 4 & 6
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScoreButton(label = "4", sublabel = "FOUR!", color = BoundaryGold, textColor = Color.Black, modifier = Modifier.weight(1f)) {
                            viewModel.recordBall(runsBat = 4)
                        }
                        ScoreButton(label = "6", sublabel = "SIX!", color = MaximumSixGreen, textColor = Color.White, modifier = Modifier.weight(1f)) {
                            viewModel.recordBall(runsBat = 6)
                        }
                    }

                    // Extras Row: WD, NB, LB, BYE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScoreButton(label = "WD", sublabel = "+${match.wideRuns}", color = ExtraOrange.copy(alpha = 0.2f), textColor = ExtraOrange, modifier = Modifier.weight(1f)) {
                            viewModel.recordBall(extraType = ExtraType.WIDE, extraRuns = match.wideRuns)
                        }
                        ScoreButton(label = "NB", sublabel = "+${match.noBallRuns}", color = ExtraOrange.copy(alpha = 0.2f), textColor = ExtraOrange, modifier = Modifier.weight(1f)) {
                            viewModel.recordBall(extraType = ExtraType.NO_BALL, extraRuns = match.noBallRuns)
                        }
                        ScoreButton(label = "LB", sublabel = "1 run", color = MaterialTheme.colorScheme.surfaceVariant, textColor = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f)) {
                            viewModel.recordBall(extraType = ExtraType.LEG_BYE, extraRuns = 1)
                        }
                        ScoreButton(label = "BYE", sublabel = "1 run", color = MaterialTheme.colorScheme.surfaceVariant, textColor = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f)) {
                            viewModel.recordBall(extraType = ExtraType.BYE, extraRuns = 1)
                        }
                    }

                    // Action Controls: WICKET, UNDO, CUSTOM
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.openWicketDialog() },
                            colors = ButtonDefaults.buttonColors(containerColor = WicketRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(50.dp)
                        ) {
                            Text("☝️ WICKET", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }

                        Button(
                            onClick = { viewModel.undoLastDelivery() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            enabled = uiState.undoStack.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text("↩️ Undo", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Button(
                            onClick = { viewModel.openCustomRunsDialog() },
                            colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text("➕ Extra", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // Navigation Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onViewScorecardClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("📋 Scorecard", fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = onViewTeamsClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("👥 Teams", fontSize = 13.sp)
            }
            IconButton(
                onClick = { viewModel.shareScorecard(context) }
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = TurfGreenMedium)
            }
            IconButton(
                onClick = { viewModel.openNewGameDialog() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Match", tint = TurfGreenMedium)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun ScoreButton(
    label: String,
    sublabel: String,
    color: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = modifier
            .height(52.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = textColor
            )
            Text(
                text = sublabel,
                fontSize = 10.sp,
                color = textColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun DeliveryBadge(delivery: Delivery) {
    val bg = when {
        delivery.isWicket -> WicketRed
        delivery.runsBat == 4 -> BoundaryGold
        delivery.runsBat == 6 -> MaximumSixGreen
        delivery.extraType == ExtraType.WIDE || delivery.extraType == ExtraType.NO_BALL -> ExtraOrange
        delivery.runsBat == 0 -> Color.Gray.copy(alpha = 0.3f)
        else -> TurfGreenMedium.copy(alpha = 0.2f)
    }

    val textCol = when {
        delivery.isWicket -> Color.White
        delivery.runsBat == 4 -> Color.Black
        delivery.runsBat == 6 -> Color.White
        delivery.extraType == ExtraType.WIDE || delivery.extraType == ExtraType.NO_BALL -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .background(bg, CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
    ) {
        Text(
            text = delivery.badgeText,
            color = textCol,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}
