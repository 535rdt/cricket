package com.cricket.scoreboard.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cricket.scoreboard.model.*
import com.cricket.scoreboard.network.SupabaseClient
import com.cricket.scoreboard.ui.components.PlayerAvatar
import com.cricket.scoreboard.ui.dialogs.SupabaseConfigDialog
import com.cricket.scoreboard.ui.theme.*
import com.cricket.scoreboard.viewmodel.ActiveScreen
import com.cricket.scoreboard.viewmodel.CricketViewModel

@Composable
fun SharingScreen(
    viewModel: CricketViewModel,
    onNavigateToSetup: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uiState by viewModel.uiState.collectAsState()
    val match = uiState.match

    // Tab state: 0 = Watch Another Scoreboard, 1 = Broadcast My Scoreboard
    var selectedTab by remember { mutableStateOf(if (uiState.isBroadcasting && !uiState.isSpectatorMode) 1 else 0) }
    var inputCode by remember { mutableStateOf("") }
    var selectedScorecardInnings by remember { mutableStateOf(1) }
    var showSupabaseDialog by remember { mutableStateOf(false) }

    fun copyToClipboard(label: String, text: String) {
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun shareViaIntent() {
        val shareText = """
🏏 *FOLLOW LIVE CRICKET SCOREBOARD* 🏏
━━━━━━━━━━━━━━━━━━━━
${match?.teamA?.name ?: "Team 1"} vs ${match?.teamB?.name ?: "Team 2"}

📱 *WATCH IN APP:*
Enter Match Code: *${uiState.broadcastMatchCode}* in the "Sharing" tab.

🌐 *WATCH IN BROWSER:*
${uiState.broadcastWebUrl}
━━━━━━━━━━━━━━━━━━━━
Follow ball-by-ball updates live!
        """.trimIndent()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Live Scoreboard")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.isSpectatorMode) {
                            Surface(
                                shape = CircleShape,
                                color = WicketRed,
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (uiState.isSpectatorMode) "🔴 Live Game Window" else "👀 Watch Scoreboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            )

            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.isBroadcasting) {
                            Surface(
                                shape = CircleShape,
                                color = PitchGold,
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = "📢 Broadcast Scoreboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // TAB 0: WATCH ANOTHER SCOREBOARD
            if (selectedTab == 0) {
                if (uiState.isSpectatorMode && match != null) {
                    // ==========================================
                    // GAME SHARED WINDOW OF ANOTHER SCOREBOARD
                    // ==========================================

                    // 1. Live Spectator Connection Banner
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = WicketRed.copy(alpha = 0.14f)),
                        border = BorderStroke(1.5.dp, WicketRed.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = WicketRed,
                                        modifier = Modifier.size(10.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "🔴 LIVE SPECTATING: ${uiState.spectatorTarget}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = WicketRed
                                        )
                                        Text(
                                            text = "Auto-syncing ball-by-ball from scorer",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilledTonalIconButton(
                                        onClick = { viewModel.connectToLiveMatch(uiState.spectatorTarget) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "Refresh",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.exitSpectatorMode() },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(34.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WicketRed)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Disconnect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 2. Main Live Scoreboard Hero Card
                    val inn = match.currentInnings
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = TurfGreenDark),
                        elevation = CardDefaults.cardElevation(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Match Teams & Innings Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${match.teamA.name}  vs  ${match.teamB.name}",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${inn.battingTeamName} Batting (Innings ${match.currentInningsIndex})",
                                        color = PitchGold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.3f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${match.totalOvers} Ov Match",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Giant Live Score
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "${inn.totalRuns}",
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "/${inn.wickets}",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PitchGold,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${inn.oversText} / ${inn.maxOvers} Ov",
                                        fontSize = 20.sp,
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

                            // Chase Target / Equation Banner (2nd Innings)
                            if (match.currentInningsIndex == 2 && inn.targetScore != null) {
                                val target = inn.targetScore
                                val runsNeeded = (target - inn.totalRuns).coerceAtLeast(0)
                                val maxLegalBalls = inn.maxOvers * 6
                                val ballsLeft = (maxLegalBalls - inn.legalBalls).coerceAtLeast(0)
                                val rrr = if (ballsLeft > 0) String.format(java.util.Locale.US, "%.2f", (runsNeeded.toDouble() * 6.0) / ballsLeft) else "0.00"

                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Target: $target • Need $runsNeeded off $ballsLeft balls",
                                            color = PitchGold,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "RRR: $rrr",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Match Result if finished
                            if (match.isFinished && match.resultMessage != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = PitchGold.copy(alpha = 0.2f)),
                                    border = BorderStroke(1.dp, PitchGold.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "🏆 ${match.resultMessage}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = PitchGold,
                                            textAlign = TextAlign.Center
                                        )
                                        if (match.manOfTheMatch != null) {
                                            Text(
                                                text = "⭐ Player of the Match: ${match.manOfTheMatch}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Active Batsmen & Bowler Section
                    val striker = inn.batsmen.find { it.playerId == match.currentStrikerId }
                    val nonStriker = inn.batsmen.find { it.playerId == match.currentNonStrikerId }
                    val bowler = inn.bowlers.find { it.playerId == match.currentBowlerId } ?: BowlerScore(match.currentBowlerId, "Bowler")

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "🏏 AT THE CREASE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Striker Row
                            striker?.let { s ->
                                SpectatorPlayerRow(
                                    name = s.name,
                                    photoUri = s.photoUri,
                                    isStriker = true,
                                    runs = s.runs,
                                    balls = s.balls,
                                    fours = s.fours,
                                    sixes = s.sixes,
                                    sr = s.formattedStrikeRate
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Non-Striker Row
                            nonStriker?.let { ns ->
                                SpectatorPlayerRow(
                                    name = ns.name,
                                    photoUri = ns.photoUri,
                                    isStriker = false,
                                    runs = ns.runs,
                                    balls = ns.balls,
                                    fours = ns.fours,
                                    sixes = ns.sixes,
                                    sr = ns.formattedStrikeRate
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Current Bowler Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    PlayerAvatar(
                                        photoUri = bowler.photoUri,
                                        name = bowler.name,
                                        size = 32.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "${bowler.name} ⚾",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Bowling this over",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${bowler.wickets}/${bowler.runsConceded}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = TurfGreenMedium
                                        )
                                        Text(text = "${bowler.oversText} Ov", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = bowler.formattedEconomy,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(text = "Econ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    // 4. Current Over Deliveries Strip
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "THIS OVER (${inn.currentOverDeliveries.count { it.isLegalDelivery }}/6 balls)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (inn.currentOverDeliveries.isEmpty()) {
                                Text(
                                    text = "Starting new over...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(inn.currentOverDeliveries) { d ->
                                        val isWicket = d.isWicket
                                        val isBoundary = d.runsBat == 4 || d.runsBat == 6
                                        val bg = when {
                                            isWicket -> WicketRed
                                            d.runsBat == 6 -> TurfGreenMedium
                                            d.runsBat == 4 -> PitchGold
                                            d.extraType != ExtraType.NONE -> ExtraOrange
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                        val textColor = when {
                                            isWicket || d.runsBat == 6 -> Color.White
                                            d.runsBat == 4 -> Color.Black
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(bg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = d.badgeText,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp,
                                                color = textColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Scorecard / Report Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.shareScorecard(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Report (PNG/JPG)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.setScreen(ActiveScreen.FULL_SCORECARD) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Full Scorecard", fontSize = 12.sp)
                        }
                    }

                } else {
                    // ==========================================
                    // NOT CONNECTED: JOIN / CONNECT TO SCOREBOARD
                    // ==========================================
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(TurfGreenMedium.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LiveTv,
                                    contentDescription = null,
                                    tint = TurfGreenMedium,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Watch Another Scoreboard Live",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Enter the 6-character Match Code or Host IP shared by the other team's scorer to follow their match in real time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            OutlinedTextField(
                                value = inputCode,
                                onValueChange = { inputCode = it.uppercase() },
                                label = { Text("Match Code or Host IP") },
                                placeholder = { Text("e.g. CRIC-8492 or 192.168.43.1") },
                                leadingIcon = {
                                    Icon(Icons.Default.Tag, contentDescription = null, tint = TurfGreenMedium)
                                },
                                trailingIcon = {
                                    if (inputCode.isNotEmpty()) {
                                        IconButton(onClick = { inputCode = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (inputCode.isNotBlank()) viewModel.connectToLiveMatch(inputCode.trim())
                                })
                            )

                            if (uiState.liveViewerError != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = WicketRed.copy(alpha = 0.12f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = WicketRed, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = uiState.liveViewerError!!,
                                            color = WicketRed,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (inputCode.isNotBlank()) viewModel.connectToLiveMatch(inputCode.trim())
                                },
                                enabled = inputCode.isNotBlank() && !uiState.liveViewerLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                if (uiState.liveViewerLoading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Connecting to Scoreboard...", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Connect & Watch Live Game", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    // How to Watch Explainer Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "💡 HOW LIVE SHARING WORKS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            GuideItem(
                                step = "1",
                                title = "Get Match Code from Scorer",
                                desc = "Ask the scorer on the other phone to tap 'Sharing' -> 'Broadcast Scoreboard' and give you their 6-letter match code."
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            GuideItem(
                                step = "2",
                                title = "Ground Hotspot / Offline Mode",
                                desc = "If playing in a turf or ground with no internet, connect to the scorer's Mobile Hotspot and enter their local IP."
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            GuideItem(
                                step = "3",
                                title = "Instant Real-Time Stream",
                                desc = "Every ball, run, boundary, wicket, and strike rotation entered by the scorer will automatically update on your screen!"
                            )
                        }
                    }
                }

            } else {
                // ==========================================
                // TAB 1: BROADCAST MY SCOREBOARD
                // ==========================================
                if (match == null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.SportsCricket,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Active Match to Broadcast",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Start a match in Match Setup to broadcast live scores to spectators and other teams.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onNavigateToSetup,
                                colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium)
                            ) {
                                Text("Go to Match Setup ⚙️")
                            }
                        }
                    }
                } else {
                    // Match Broadcast Controls
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.isBroadcasting) TurfGreenDark else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (uiState.isBroadcasting) {
                                            Surface(
                                                shape = CircleShape,
                                                color = PitchGold,
                                                modifier = Modifier.size(8.dp)
                                            ) {}
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = if (uiState.isBroadcasting) "LIVE BROADCAST ACTIVE" else "Live Broadcast Paused",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            color = if (uiState.isBroadcasting) PitchGold else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = if (uiState.isBroadcasting) "Other teams are receiving your live scores" else "Turn on switch to broadcast to other teams",
                                        fontSize = 11.sp,
                                        color = if (uiState.isBroadcasting) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Switch(
                                    checked = uiState.isBroadcasting,
                                    onCheckedChange = { viewModel.toggleLiveBroadcast(it) }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = uiState.isBroadcasting) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Match Code for other App users
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = TurfGreenDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "📱 MATCH CODE FOR APP SPECTATORS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = uiState.broadcastMatchCode,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        color = PitchGold,
                                        letterSpacing = 3.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Other teams can enter this code in their 'Sharing' tab to follow live!",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { copyToClipboard("Match Code", uiState.broadcastMatchCode) },
                                        colors = ButtonDefaults.buttonColors(containerColor = PitchGold),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copy Match Code", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }

                            // Web Browser Link
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "🌐 WEB BROWSER LIVE LINK (No App Needed)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = uiState.broadcastWebUrl,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TurfGreenMedium
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { copyToClipboard("Web Link", uiState.broadcastWebUrl) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copy Link", fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = { shareViaIntent() },
                                            colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Share WhatsApp", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Supabase Cloud Sync Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (SupabaseClient.isConfigured(context)) TurfGreenMedium.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = if (SupabaseClient.isConfigured(context)) TurfGreenMedium else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Supabase Cloud Sync",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (SupabaseClient.isConfigured(context)) TurfGreenMedium else Color.Gray,
                                    modifier = Modifier.size(8.dp)
                                ) {}
                            }
                            Text(
                                text = if (SupabaseClient.isConfigured(context)) "Connected • Real-time DB enabled" else "Connect project for cloud database sync",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showSupabaseDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (SupabaseClient.isConfigured(context)) "Config" else "Setup", fontSize = 11.sp)
                    }
                }
            }
        }

        if (showSupabaseDialog) {
            SupabaseConfigDialog(
                onDismiss = { showSupabaseDialog = false },
                onSaved = {
                    showSupabaseDialog = false
                    Toast.makeText(context, "Supabase cloud settings saved!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
private fun SpectatorPlayerRow(
    name: String,
    photoUri: String?,
    isStriker: Boolean,
    runs: Int,
    balls: Int,
    fours: Int,
    sixes: Int,
    sr: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            PlayerAvatar(
                photoUri = photoUri,
                name = name,
                size = 32.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        fontWeight = if (isStriker) FontWeight.ExtraBold else FontWeight.Medium,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isStriker) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "🏏*", fontSize = 12.sp, color = PitchGold, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = "SR: $sr",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$runs",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = if (isStriker) PitchGold else MaterialTheme.colorScheme.onSurface
                )
                Text(text = "($balls b)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${fours}x4 • ${sixes}x6",
                    fontSize = 11.sp,
                    color = TurfGreenMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun GuideItem(
    step: String,
    title: String,
    desc: String
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(TurfGreenMedium),
            contentAlignment = Alignment.Center
        ) {
            Text(text = step, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
