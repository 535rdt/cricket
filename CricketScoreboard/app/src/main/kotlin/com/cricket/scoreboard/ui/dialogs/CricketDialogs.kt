package com.cricket.scoreboard.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cricket.scoreboard.model.*
import com.cricket.scoreboard.ui.components.PlayerAvatar
import com.cricket.scoreboard.ui.theme.PitchGold
import com.cricket.scoreboard.ui.theme.TurfGreenDark
import com.cricket.scoreboard.ui.theme.TurfGreenMedium
import com.cricket.scoreboard.ui.theme.WicketRed

@Composable
fun OpeningPlayersDialog(
    battingTeam: Team,
    bowlingTeam: Team,
    onConfirm: (strikerId: String, nonStrikerId: String, openingBowlerId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStrikerId by remember { mutableStateOf(battingTeam.players.getOrNull(0)?.id ?: "") }
    var selectedNonStrikerId by remember { mutableStateOf(battingTeam.players.getOrNull(1)?.id ?: "") }
    var selectedBowlerId by remember { mutableStateOf(bowlingTeam.players.getOrNull(0)?.id ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "🏏 Select Opening Players",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${battingTeam.name} is Batting first vs ${bowlingTeam.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Striker
                Text(
                    text = "Opening Striker (Facing First Ball)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                battingTeam.players.forEach { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedStrikerId = player.id
                                if (selectedNonStrikerId == player.id) {
                                    selectedNonStrikerId = battingTeam.players.firstOrNull { it.id != player.id }?.id ?: ""
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedStrikerId == player.id,
                            onClick = {
                                selectedStrikerId = player.id
                                if (selectedNonStrikerId == player.id) {
                                    selectedNonStrikerId = battingTeam.players.firstOrNull { it.id != player.id }?.id ?: ""
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        PlayerAvatar(photoUri = player.photoUri, name = player.name, size = 26.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = player.name, fontWeight = if (selectedStrikerId == player.id) FontWeight.Bold else FontWeight.Normal)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Non-Striker
                Text(
                    text = "Opening Non-Striker (Runner End)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                battingTeam.players.forEach { player ->
                    if (player.id != selectedStrikerId) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedNonStrikerId = player.id }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedNonStrikerId == player.id,
                                onClick = { selectedNonStrikerId = player.id }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            PlayerAvatar(photoUri = player.photoUri, name = player.name, size = 26.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = player.name, fontWeight = if (selectedNonStrikerId == player.id) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Opening Bowler
                Text(
                    text = "Opening Bowler (${bowlingTeam.name})",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                bowlingTeam.players.forEach { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedBowlerId = player.id }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedBowlerId == player.id,
                            onClick = { selectedBowlerId = player.id }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        PlayerAvatar(photoUri = player.photoUri, name = player.name, size = 26.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = player.name, fontWeight = if (selectedBowlerId == player.id) FontWeight.Bold else FontWeight.Normal)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedStrikerId.isNotEmpty() && selectedNonStrikerId.isNotEmpty() && selectedBowlerId.isNotEmpty()) {
                                onConfirm(selectedStrikerId, selectedNonStrikerId, selectedBowlerId)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium)
                    ) {
                        Text("Start Match 🚀", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun NextBowlerDialog(
    bowlingTeam: Team,
    currentBowlerId: String,
    lastBowlerId: String?,
    innings: Innings? = null,
    maxOversPerBowler: Int = 0,
    onConfirm: (newBowlerId: String) -> Unit
) {
    var selectedBowlerId by remember {
        val eligible = bowlingTeam.players.firstOrNull { player ->
            val bScore = innings?.bowlers?.find { it.playerId == player.id }
            val overs = (bScore?.legalBalls ?: 0) / 6
            val quotaDone = maxOversPerBowler > 0 && overs >= maxOversPerBowler
            player.id != lastBowlerId && player.id != currentBowlerId && !quotaDone
        }
        mutableStateOf(eligible?.id ?: bowlingTeam.players.firstOrNull()?.id ?: "")
    }

    Dialog(onDismissRequest = { /* Must select next bowler */ }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "🎯 Over Completed!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select the bowler for the next over. (Consecutive overs by the same bowler are not allowed in cricket)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                bowlingTeam.players.forEach { player ->
                    val isLastBowler = player.id == lastBowlerId
                    val bowlerScore = innings?.bowlers?.find { it.playerId == player.id }
                    val oversBowled = (bowlerScore?.legalBalls ?: 0) / 6
                    val hasCompletedQuota = maxOversPerBowler > 0 && oversBowled >= maxOversPerBowler
                    val isSelectable = !isLastBowler && !hasCompletedQuota

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isSelectable) {
                                selectedBowlerId = player.id
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedBowlerId == player.id,
                            onClick = { if (isSelectable) selectedBowlerId = player.id },
                            enabled = isSelectable
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        PlayerAvatar(photoUri = player.photoUri, name = player.name, size = 26.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = player.name,
                                fontWeight = if (selectedBowlerId == player.id) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isSelectable) Color.Gray else MaterialTheme.colorScheme.onSurface
                            )
                            val statusNote = when {
                                isLastBowler -> "Just bowled previous over"
                                hasCompletedQuota -> "Quota completed (${oversBowled}/$maxOversPerBowler overs)"
                                bowlerScore != null -> "${bowlerScore.oversText} ov, ${bowlerScore.runsConceded}R, ${bowlerScore.wickets}W"
                                else -> "Yet to bowl"
                            }
                            Text(
                                text = statusNote,
                                fontSize = 11.sp,
                                color = if (!isSelectable) Color.Red.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (selectedBowlerId.isNotEmpty()) {
                            onConfirm(selectedBowlerId)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium)
                ) {
                    Text("Start Over", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun NewBatsmanDialog(
    battingTeam: Team,
    currentInnings: Innings,
    onConfirm: (newBatsmanId: String) -> Unit
) {
    val availablePlayers = battingTeam.players.filter { player ->
        val score = currentInnings.batsmen.find { it.playerId == player.id }
        score == null || (!score.isBatting && !score.isOut)
    }

    var selectedBatsmanId by remember {
        mutableStateOf(availablePlayers.firstOrNull()?.id ?: "")
    }

    Dialog(onDismissRequest = { /* Must select incoming batsman */ }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "☝️ Wicket Fell!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = WicketRed
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select incoming batsman to the crease:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (availablePlayers.isEmpty()) {
                    Text(
                        text = "All batsmen have batted!",
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    availablePlayers.forEach { player ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedBatsmanId = player.id }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedBatsmanId == player.id,
                                onClick = { selectedBatsmanId = player.id }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            PlayerAvatar(photoUri = player.photoUri, name = player.name, size = 26.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = player.name,
                                fontWeight = if (selectedBatsmanId == player.id) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (selectedBatsmanId.isNotEmpty()) {
                            onConfirm(selectedBatsmanId)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedBatsmanId.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium)
                ) {
                    Text("Send Batsman to Crease", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WicketDialog(
    striker: BatsmanScore?,
    nonStriker: BatsmanScore?,
    bowlingTeam: Team,
    onConfirm: (
        wicketType: WicketType,
        dismissedPlayerId: String,
        fielderName: String?,
        runsCompleted: Int
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedWicketType by remember { mutableStateOf(WicketType.BOWLED) }
    var selectedDismissedId by remember { mutableStateOf(striker?.playerId ?: "") }
    var fielderName by remember { mutableStateOf("") }
    var runsCompleted by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "🔴 Record Wicket",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = WicketRed
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Dismissal Type with FlowRow
                Text(text = "Dismissal Method:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))

                val types = listOf(
                    WicketType.BOWLED,
                    WicketType.CAUGHT,
                    WicketType.RUN_OUT,
                    WicketType.LBW,
                    WicketType.STUMPED,
                    WicketType.HIT_WICKET,
                    WicketType.RETIRED_HURT
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    types.forEach { wt ->
                        FilterChip(
                            selected = selectedWicketType == wt,
                            onClick = { selectedWicketType = wt },
                            label = { Text(wt.label, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Who got out?
                if (selectedWicketType == WicketType.RUN_OUT) {
                    Text(text = "Who is Run Out?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        striker?.let { s ->
                            FilterChip(
                                selected = selectedDismissedId == s.playerId,
                                onClick = { selectedDismissedId = s.playerId },
                                label = { Text("Striker (${s.name})", fontSize = 12.sp) }
                            )
                        }
                        nonStriker?.let { ns ->
                            FilterChip(
                                selected = selectedDismissedId == ns.playerId,
                                onClick = { selectedDismissedId = ns.playerId },
                                label = { Text("Non-Striker (${ns.name})", fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Runs completed before Run Out:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0, 1, 2, 3).forEach { r ->
                            FilterChip(
                                selected = runsCompleted == r,
                                onClick = { runsCompleted = r },
                                label = { Text("$r run${if (r != 1) "s" else ""}") }
                            )
                        }
                    }
                }

                // Fielder name
                if (selectedWicketType == WicketType.CAUGHT || selectedWicketType == WicketType.RUN_OUT || selectedWicketType == WicketType.STUMPED) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = fielderName,
                        onValueChange = { fielderName = it },
                        label = { Text("Fielder Name (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val outId = if (selectedWicketType == WicketType.RUN_OUT) selectedDismissedId else (striker?.playerId ?: "")
                            onConfirm(selectedWicketType, outId, fielderName.ifBlank { null }, runsCompleted)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WicketRed)
                    ) {
                        Text("Confirm Wicket", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun InningsBreakDialog(
    innings1: Innings,
    teamA: Team,
    teamB: Team,
    onStartSecondInnings: (strikerId: String, nonStrikerId: String, openingBowlerId: String) -> Unit
) {
    val batting2ndTeam = if (innings1.battingTeamId == teamA.id) teamB else teamA
    val bowling2ndTeam = if (innings1.battingTeamId == teamA.id) teamA else teamB

    var selectedStrikerId by remember { mutableStateOf(batting2ndTeam.players.getOrNull(0)?.id ?: "") }
    var selectedNonStrikerId by remember { mutableStateOf(batting2ndTeam.players.getOrNull(1)?.id ?: "") }
    var selectedBowlerId by remember { mutableStateOf(bowling2ndTeam.players.getOrNull(0)?.id ?: "") }

    Dialog(onDismissRequest = { /* Must proceed to 2nd innings */ }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "🏆 Innings Break!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PitchGold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "${innings1.battingTeamName}: ${innings1.totalRuns}/${innings1.wickets}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Overs: ${innings1.oversText} | Run Rate: ${innings1.formattedCrr}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "🎯 Target for ${batting2ndTeam.name}: ${innings1.totalRuns + 1} runs",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PitchGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Select 2nd Innings Openers (${batting2ndTeam.name})",
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Striker:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                batting2ndTeam.players.forEach { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedStrikerId = player.id
                                if (selectedNonStrikerId == player.id) {
                                    selectedNonStrikerId = batting2ndTeam.players.firstOrNull { it.id != player.id }?.id ?: ""
                                }
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedStrikerId == player.id,
                            onClick = {
                                selectedStrikerId = player.id
                                if (selectedNonStrikerId == player.id) {
                                    selectedNonStrikerId = batting2ndTeam.players.firstOrNull { it.id != player.id }?.id ?: ""
                                }
                            }
                        )
                        Text(text = player.name, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Non-Striker:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                batting2ndTeam.players.forEach { player ->
                    if (player.id != selectedStrikerId) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedNonStrikerId = player.id }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedNonStrikerId == player.id,
                                onClick = { selectedNonStrikerId = player.id }
                            )
                            Text(text = player.name, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Opening Bowler (${bowling2ndTeam.name}):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                bowling2ndTeam.players.forEach { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedBowlerId = player.id }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedBowlerId == player.id,
                            onClick = { selectedBowlerId = player.id }
                        )
                        Text(text = player.name, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (selectedStrikerId.isNotEmpty() && selectedNonStrikerId.isNotEmpty() && selectedBowlerId.isNotEmpty()) {
                            onStartSecondInnings(selectedStrikerId, selectedNonStrikerId, selectedBowlerId)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium)
                ) {
                    Text("Start 2nd Innings Chase 🏏", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MatchEndDialog(
    match: Match,
    onViewScorecard: () -> Unit,
    onShare: () -> Unit,
    onRematchWithSameTeams: () -> Unit,
    onSetupWithSameTeams: () -> Unit,
    onNewMatchFresh: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉 MATCH FINISHED! 🏆",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PitchGold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = TurfGreenMedium.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = match.resultMessage ?: "Match Completed",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (match.manOfTheMatch != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "⭐ Player of the Match: ${match.manOfTheMatch}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PitchGold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Summary
                Text(
                    text = "${match.innings1.battingTeamName}: ${match.innings1.totalRuns}/${match.innings1.wickets} (${match.innings1.oversText} ov)",
                    fontWeight = FontWeight.Medium
                )
                match.innings2?.let { inn2 ->
                    Text(
                        text = "${inn2.battingTeamName}: ${inn2.totalRuns}/${inn2.wickets} (${inn2.oversText} ov)",
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Action: Rematch with Same Teams
                Button(
                    onClick = onRematchWithSameTeams,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "🔁 Start New Game with Same Teams",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onSetupWithSameTeams,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("⚙️ Setup with Same Teams")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onViewScorecard,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TurfGreenDark)
                ) {
                    Text("📋 View Full Scorecard", color = Color.White)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📤 Share Match Scorecard")
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(
                    onClick = onNewMatchFresh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Fresh Match (New Teams)")
                }
            }
        }
    }
}

@Composable
fun SavedTeamsDialog(
    savedTeams: List<Team>,
    targetIsTeamA: Boolean,
    onLoadTeam: (Team, Boolean) -> Unit,
    onDeleteTeam: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📂 Saved Teams Library",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Target: ${if (targetIsTeamA) "Team 1 (Team A)" else "Team 2 (Team B)"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                if (savedTeams.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved teams found.\nConfigure a team and tap 'Save Team' in Setup!",
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(savedTeams) { team ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = team.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = TurfGreenMedium.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "${team.players.size} Players",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TurfGreenMedium,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = { onDeleteTeam(team.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete Team",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    val captain = team.players.find { it.isCaptain }?.name
                                    val keeper = team.players.find { it.isWicketKeeper }?.name
                                    if (captain != null || keeper != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (captain != null) {
                                                Text(
                                                    text = "👑 (C) $captain",
                                                    fontSize = 11.sp,
                                                    color = PitchGold,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            if (keeper != null) {
                                                Text(
                                                    text = "🧤 (WK) $keeper",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    val playersSample = team.players.take(4).joinToString(", ") { it.name }
                                    val extraCount = team.players.size - 4
                                    Text(
                                        text = if (extraCount > 0) "$playersSample +$extraCount more" else playersSample,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onLoadTeam(team, true) },
                                            modifier = Modifier.weight(1f).height(38.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = if (targetIsTeamA) ButtonDefaults.buttonColors(containerColor = TurfGreenMedium) else ButtonDefaults.filledTonalButtonColors()
                                        ) {
                                            Text("Load as Team 1", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Button(
                                            onClick = { onLoadTeam(team, false) },
                                            modifier = Modifier.weight(1f).height(38.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = if (!targetIsTeamA) ButtonDefaults.buttonColors(containerColor = TurfGreenMedium) else ButtonDefaults.filledTonalButtonColors()
                                        ) {
                                            Text("Load as Team 2", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun SaveTeamDialog(
    initialName: String,
    players: List<Player>,
    isTeamA: Boolean,
    onConfirm: (name: String) -> Unit,
    onDismiss: () -> Unit
) {
    var teamName by remember { mutableStateOf(initialName) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "💾 Save ${if (isTeamA) "Team 1" else "Team 2"} to My Teams",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Saving roster with ${players.size} players, roles, captain & keeper badges.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("Team Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Squad Preview (${players.size} Players):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = players.take(6).joinToString(", ") { p ->
                                val badges = mutableListOf<String>()
                                if (p.isCaptain) badges.add("C")
                                if (p.isWicketKeeper) badges.add("WK")
                                if (badges.isNotEmpty()) "${p.name} (${badges.joinToString(",")})" else p.name
                            } + (if (players.size > 6) " +${players.size - 6} more" else ""),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (teamName.isNotBlank()) onConfirm(teamName.trim())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium)
                    ) {
                        Text("Save Team", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun NewGameChoiceDialog(
    teamAName: String,
    teamBName: String,
    onRematchWithSameTeams: () -> Unit,
    onSetupWithSameTeams: () -> Unit,
    onFreshMatch: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🏏 Start New Match",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Choose how you want to start the next game:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Option 1: Direct Rematch
                Button(
                    onClick = onRematchWithSameTeams,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🔁 Rematch (Same Teams)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                text = "$teamAName vs $teamBName",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Option 2: Setup with Same Teams
                OutlinedButton(
                    onClick = onSetupWithSameTeams,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "⚙️ Match Setup (Keep Teams)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Option 3: Fresh Setup
                OutlinedButton(
                    onClick = onFreshMatch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🆕 Fresh Match (Reset All)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomRunsDialog(
    onConfirm: (runsBat: Int, extraType: ExtraType, extraRuns: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedExtraType by remember { mutableStateOf(ExtraType.WIDE) }
    var extraRuns by remember { mutableStateOf(1) }
    var runsBat by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "➕ Custom / Extra Runs",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "Delivery Type:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(ExtraType.WIDE, ExtraType.NO_BALL, ExtraType.LEG_BYE, ExtraType.BYE).forEach { et ->
                        FilterChip(
                            selected = selectedExtraType == et,
                            onClick = {
                                selectedExtraType = et
                                extraRuns = 1
                                runsBat = 0
                            },
                            label = { Text(et.displayName, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedExtraType == ExtraType.WIDE) {
                    Text(text = "Total Wide Runs (including overthrows):", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 3, 4, 5).forEach { r ->
                            FilterChip(
                                selected = extraRuns == r,
                                onClick = { extraRuns = r },
                                label = { Text("$r") }
                            )
                        }
                    }
                } else if (selectedExtraType == ExtraType.NO_BALL) {
                    Text(text = "Runs off the Bat (on this No Ball):", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0, 1, 2, 3, 4, 6).forEach { r ->
                            FilterChip(
                                selected = runsBat == r,
                                onClick = { runsBat = r },
                                label = { Text("$r") }
                            )
                        }
                    }
                } else {
                    Text(text = "Runs by Bye/Leg Bye:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 3, 4).forEach { r ->
                            FilterChip(
                                selected = extraRuns == r,
                                onClick = { extraRuns = r },
                                label = { Text("$r") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(runsBat, selectedExtraType, extraRuns)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium)
                    ) {
                        Text("Add Ball", color = Color.White)
                    }
                }
            }
        }
    }
}
