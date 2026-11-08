package com.cricket.scoreboard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
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
import com.cricket.scoreboard.model.Innings
import com.cricket.scoreboard.model.Match
import com.cricket.scoreboard.ui.components.PlayerAvatar
import com.cricket.scoreboard.ui.theme.PitchGold
import com.cricket.scoreboard.ui.theme.TurfGreenDark
import com.cricket.scoreboard.ui.theme.TurfGreenMedium
import com.cricket.scoreboard.ui.theme.WicketRed
import com.cricket.scoreboard.viewmodel.CricketViewModel

@Composable
fun FullScorecardScreen(
    viewModel: CricketViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val match = uiState.match ?: return
    var selectedInningsTab by remember { mutableStateOf(if (match.currentInningsIndex == 2) 1 else 0) }

    val displayInnings = if (selectedInningsTab == 0) match.innings1 else (match.innings2 ?: match.innings1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        // Innings Selector Tabs
        if (match.innings2 != null) {
            PrimaryTabRow(selectedTabIndex = selectedInningsTab, modifier = Modifier.padding(vertical = 8.dp)) {
                Tab(
                    selected = selectedInningsTab == 0,
                    onClick = { selectedInningsTab = 0 },
                    text = { Text("1st Inn: ${match.innings1.battingTeamName}", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                Tab(
                    selected = selectedInningsTab == 1,
                    onClick = { selectedInningsTab = 1 },
                    text = { Text("2nd Inn: ${match.innings2.battingTeamName}", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            // Match Result & Status Banner
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TurfGreenDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${displayInnings.battingTeamName.uppercase()} INNINGS",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color.White,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(onClick = { viewModel.shareScorecard(context) }) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = PitchGold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${displayInnings.totalRuns}",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "/${displayInnings.wickets}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PitchGold,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                            Text(
                                text = "Overs: ${displayInnings.oversText} (RR: ${displayInnings.formattedCrr})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        if (match.resultMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = PitchGold,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "🏆 ${match.resultMessage}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Batting Scorecard Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "🏏 Batting Card",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Table Header
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Batter", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(2.2f))
                            Text("R", fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                            Text("B", fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                            Text("4s", fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                            Text("6s", fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                            Text("SR", fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Batsmen Rows
                        val battedList = displayInnings.batsmen.filter { it.isBatting || it.isOut || it.balls > 0 }
                        battedList.forEach { b ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(2.2f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PlayerAvatar(
                                        photoUri = b.photoUri,
                                        name = b.name,
                                        size = 22.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = b.name + if (b.isBatting) "*" else "",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (b.isBatting) TurfGreenMedium else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = b.dismissalText,
                                            fontSize = 10.sp,
                                            color = if (b.isOut) Color.Gray else TurfGreenMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Text("${b.runs}", fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                Text("${b.balls}", fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                Text("${b.fours}", fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                Text("${b.sixes}", fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                Text(b.formattedStrikeRate, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Extras Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Extras: ${displayInnings.totalExtras}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "(w ${displayInnings.wides}, nb ${displayInnings.noBalls}, lb ${displayInnings.legByes}, b ${displayInnings.byes})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Total Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Score:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${displayInnings.totalRuns}/${displayInnings.wickets} (${displayInnings.oversText} Ov, RR: ${displayInnings.formattedCrr})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TurfGreenMedium
                            )
                        }

                        // Did not bat
                        val didNotBat = displayInnings.batsmen.filter { !it.isBatting && !it.isOut && it.balls == 0 }
                        if (didNotBat.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Yet to Bat: " + didNotBat.joinToString(", ") { it.name },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Fall of Wickets Card
            if (displayInnings.fallOfWickets.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Fall of Wickets",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            displayInnings.fallOfWickets.forEach { fow ->
                                Text(
                                    text = "${fow.wicketNum}-${fow.teamRuns} (${fow.batsmanName}, ${fow.overText} ov)",
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bowling Scorecard Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "⚾ Bowling Card",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Table Header
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Bowler", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(2.2f))
                            Text("O", fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                            Text("M", fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                            Text("R", fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                            Text("W", fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                            Text("Econ", fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Bowler Rows
                        val bowledList = displayInnings.bowlers.filter { it.legalBalls > 0 || it.runsConceded > 0 }
                        bowledList.forEach { bw ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(2.2f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PlayerAvatar(
                                        photoUri = bw.photoUri,
                                        name = bw.name,
                                        size = 22.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = bw.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(bw.oversText, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                Text("${bw.maidens}", fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                Text("${bw.runsConceded}", fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                Text("${bw.wickets}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WicketRed, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                Text(bw.formattedEconomy, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Share & Back Buttons
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (match.isFinished) {
                        Button(
                            onClick = { viewModel.startNewGameWithSameTeams(directToOpening = false) },
                            colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("🔁 Start New Game with Same Teams", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBackClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back to Live Scoring", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { viewModel.shareScorecard(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = TurfGreenDark),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Share Report (PNG/JPG) 📤", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
