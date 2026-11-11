package com.cricket.scoreboard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cricket.scoreboard.model.PlayerRole
import com.cricket.scoreboard.model.TossChoice
import com.cricket.scoreboard.ui.components.PlayerAvatar
import com.cricket.scoreboard.ui.dialogs.PlayerPoolPickerDialog
import com.cricket.scoreboard.ui.theme.PitchGold
import com.cricket.scoreboard.ui.theme.TurfGreenDark
import com.cricket.scoreboard.ui.theme.TurfGreenMedium
import com.cricket.scoreboard.viewmodel.CricketViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MatchSetupScreen(
    viewModel: CricketViewModel,
    onStartMatchClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val setup = uiState.setup
    val savedPlayers = uiState.savedPlayers
    var selectedTab by remember { mutableStateOf(0) } // 0: Match Info, 1: Team A Squad, 2: Team B Squad
    var showPoolMultiPickerForSquad by remember { mutableStateOf(false) }
    var pickingSlotIndex by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        // App Title Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TurfGreenDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🏏 CRICKET MATCH SETUP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Configure teams, squads, overs & toss",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    Button(
                        onClick = { viewModel.openWatchLiveDialog() },
                        colors = ButtonDefaults.buttonColors(containerColor = PitchGold),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Sensors, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Watch Live", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // Rematch with Teams from Last Match Banner (if previous match was played)
        if (uiState.lastMatchTeams != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TurfGreenMedium.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.loadMatchTeamsIntoSetup(uiState.lastMatchTeams!!.first, uiState.lastMatchTeams!!.second)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = TurfGreenMedium)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🔁 Use Teams from Last Match",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TurfGreenMedium
                            )
                            Text(
                                text = "${uiState.lastMatchTeams!!.first.name} vs ${uiState.lastMatchTeams!!.second.name}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("Load", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TurfGreenMedium)
                    }
                }
            }
        }

        // Section Tabs
        item {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("⚙️ Settings", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🅰️ ${setup.teamAName}", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("🅱️ ${setup.teamBName}", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        }

        if (selectedTab == 0) {
            // Team Names Card
            item {
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
                                text = "Team 1 (Team A)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilledTonalButton(
                                    onClick = { viewModel.openSavedTeamsDialog(true) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Load", fontSize = 11.sp)
                                }
                                FilledTonalButton(
                                    onClick = { viewModel.openSaveTeamDialog(true) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save", fontSize = 11.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = setup.teamAName,
                            onValueChange = { viewModel.updateTeamAName(it) },
                            label = { Text("Team 1 Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Swap button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.swapTeams() },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("⇄ Swap Team 1 & 2", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Team 2 (Team B)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilledTonalButton(
                                    onClick = { viewModel.openSavedTeamsDialog(false) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Load", fontSize = 11.sp)
                                }
                                FilledTonalButton(
                                    onClick = { viewModel.openSaveTeamDialog(false) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save", fontSize = 11.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = setup.teamBName,
                            onValueChange = { viewModel.updateTeamBName(it) },
                            label = { Text("Team 2 Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // 1. Overs per Innings Card (Fully Editable)
            item {
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
                                text = "⏱️ Overs per Innings",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = "${setup.totalOvers} Overs",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap + / - or type any custom number of overs directly:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Stepper + Direct Editable Number Field
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    val next = (setup.totalOvers - 1).coerceAtLeast(1)
                                    viewModel.updateTotalOvers(next)
                                },
                                enabled = setup.totalOvers > 1,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Text("—", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }

                            var oversText by remember(setup.totalOvers) { mutableStateOf(setup.totalOvers.toString()) }

                            OutlinedTextField(
                                value = oversText,
                                onValueChange = { input ->
                                    val digitsOnly = input.filter { it.isDigit() }.take(3)
                                    oversText = digitsOnly
                                    val parsed = digitsOnly.toIntOrNull()
                                    if (parsed != null && parsed >= 1) {
                                        viewModel.updateTotalOvers(parsed)
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                label = { Text("Overs (1-200)") },
                                suffix = { Text("Ov", fontWeight = FontWeight.SemiBold) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            FilledTonalIconButton(
                                onClick = {
                                    val next = (setup.totalOvers + 1).coerceAtMost(200)
                                    viewModel.updateTotalOvers(next)
                                },
                                enabled = setup.totalOvers < 200,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Over")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Common Presets FlowRow (Wrapping so chips NEVER get crushed)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(5, 8, 10, 15, 20, 50).forEach { ov ->
                                FilterChip(
                                    selected = setup.totalOvers == ov,
                                    onClick = { viewModel.updateTotalOvers(ov) },
                                    label = { Text("$ov Ov", fontWeight = if (setup.totalOvers == ov) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }
                    }
                }
            }

            // 2. Players Per Side Card (Fully Editable)
            item {
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
                                text = "👥 Players Per Side",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = "${setup.playersPerSide} Players",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Gully 2v2 to standard 11v11. Squad adjusts dynamically:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Stepper + Direct Editable Number Field
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    val next = (setup.playersPerSide - 1).coerceAtLeast(2)
                                    viewModel.updatePlayersPerSide(next)
                                },
                                enabled = setup.playersPerSide > 2,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Text("—", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }

                            var playersText by remember(setup.playersPerSide) { mutableStateOf(setup.playersPerSide.toString()) }

                            OutlinedTextField(
                                value = playersText,
                                onValueChange = { input ->
                                    val digitsOnly = input.filter { it.isDigit() }.take(2)
                                    playersText = digitsOnly
                                    val parsed = digitsOnly.toIntOrNull()
                                    if (parsed != null && parsed >= 2) {
                                        viewModel.updatePlayersPerSide(parsed)
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                label = { Text("Players (2-30)") },
                                suffix = { Text("Plyrs", fontWeight = FontWeight.SemiBold) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            FilledTonalIconButton(
                                onClick = {
                                    val next = (setup.playersPerSide + 1).coerceAtMost(30)
                                    viewModel.updatePlayersPerSide(next)
                                },
                                enabled = setup.playersPerSide < 30,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Player")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Common Presets FlowRow (Wrapping so chips NEVER get crushed)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(2, 4, 6, 8, 10, 11).forEach { pCount ->
                                FilterChip(
                                    selected = setup.playersPerSide == pCount,
                                    onClick = { viewModel.updatePlayersPerSide(pCount) },
                                    label = { Text("$pCount Players", fontWeight = if (setup.playersPerSide == pCount) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }
                    }
                }
            }

            // Toss Settings
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "🪙 Toss Result",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = "Who won the toss?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = setup.tossWinnerIsTeamA,
                                onClick = { viewModel.setTossWinner(true) },
                                label = { Text(setup.teamAName, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                            FilterChip(
                                selected = !setup.tossWinnerIsTeamA,
                                onClick = { viewModel.setTossWinner(false) },
                                label = { Text(setup.teamBName, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(text = "Elected to:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = setup.tossChoice == TossChoice.BAT,
                                onClick = { viewModel.setTossChoice(TossChoice.BAT) },
                                label = { Text("🏏 Bat First") }
                            )
                            FilterChip(
                                selected = setup.tossChoice == TossChoice.BOWL,
                                onClick = { viewModel.setTossChoice(TossChoice.BOWL) },
                                label = { Text("⚾ Bowl First") }
                            )
                        }
                    }
                }
            }

            // 3. Extra & Bowling Rules Card (Fully Editable)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "⚖️ Extra Rules & Regulations (Editable)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Configure penalty runs, bowler quota & re-bowl rules",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Wide Ball Runs Stepper & Input
                        Text("Wide Ball Extra Runs", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = { viewModel.setWideRuns((setup.wideRuns - 1).coerceAtLeast(0)) },
                                enabled = setup.wideRuns > 0,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Text("—", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }

                            var wideText by remember(setup.wideRuns) { mutableStateOf(setup.wideRuns.toString()) }
                            OutlinedTextField(
                                value = wideText,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }.take(2)
                                    wideText = digits
                                    val n = digits.toIntOrNull()
                                    if (n != null) viewModel.setWideRuns(n)
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                                suffix = { Text("Runs") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            FilledTonalIconButton(
                                onClick = { viewModel.setWideRuns((setup.wideRuns + 1).coerceAtMost(10)) },
                                enabled = setup.wideRuns < 10,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase Wide Runs")
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0, 1, 2, 3).forEach { r ->
                                FilterChip(
                                    selected = setup.wideRuns == r,
                                    onClick = { viewModel.setWideRuns(r) },
                                    label = { Text("$r ${if (r == 1) "Run" else "Runs"}") }
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // No-Ball Runs Stepper & Input
                        Text("No Ball Extra Runs", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = { viewModel.setNoBallRuns((setup.noBallRuns - 1).coerceAtLeast(0)) },
                                enabled = setup.noBallRuns > 0,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Text("—", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }

                            var noBallText by remember(setup.noBallRuns) { mutableStateOf(setup.noBallRuns.toString()) }
                            OutlinedTextField(
                                value = noBallText,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }.take(2)
                                    noBallText = digits
                                    val n = digits.toIntOrNull()
                                    if (n != null) viewModel.setNoBallRuns(n)
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                                suffix = { Text("Runs") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            FilledTonalIconButton(
                                onClick = { viewModel.setNoBallRuns((setup.noBallRuns + 1).coerceAtMost(10)) },
                                enabled = setup.noBallRuns < 10,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase No Ball Runs")
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1, 2, 3, 4).forEach { r ->
                                FilterChip(
                                    selected = setup.noBallRuns == r,
                                    onClick = { viewModel.setNoBallRuns(r) },
                                    label = { Text("$r ${if (r == 1) "Run" else "Runs"}") }
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // Max Overs Per Bowler Stepper & Input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Max Overs per Bowler", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            if (setup.isMaxBowlerOversAuto) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = TurfGreenMedium.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        "Auto: ${setup.maxOversPerBowler} Ov",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TurfGreenMedium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = { viewModel.setMaxOversPerBowler((setup.maxOversPerBowler - 1).coerceAtLeast(1)) },
                                enabled = setup.maxOversPerBowler > 1,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Text("—", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }

                            var maxBowlerText by remember(setup.maxOversPerBowler) { mutableStateOf(setup.maxOversPerBowler.toString()) }
                            OutlinedTextField(
                                value = maxBowlerText,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }.take(3)
                                    maxBowlerText = digits
                                    val n = digits.toIntOrNull()
                                    if (n != null && n >= 1) viewModel.setMaxOversPerBowler(n)
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                                suffix = { Text("Ov") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            FilledTonalIconButton(
                                onClick = { viewModel.setMaxOversPerBowler((setup.maxOversPerBowler + 1).coerceAtMost(setup.totalOvers)) },
                                enabled = setup.maxOversPerBowler < setup.totalOvers,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase Bowler Overs")
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = setup.isMaxBowlerOversAuto,
                                onClick = { viewModel.setAutoMaxBowlerOvers() },
                                label = { Text("Auto (${((setup.totalOvers + 4) / 5).coerceAtLeast(1)} Ov)") }
                            )
                            listOf(1, 2, 4, 10).filter { it <= setup.totalOvers }.forEach { ov ->
                                FilterChip(
                                    selected = !setup.isMaxBowlerOversAuto && setup.maxOversPerBowler == ov,
                                    onClick = { viewModel.setMaxOversPerBowler(ov) },
                                    label = { Text("$ov Ov") }
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // Match Toggles: Re-bowl Wide, Re-bowl No Ball, Free Hit
                        Text("Regulations & Conditions", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))

                        // Re-bowl Wide
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setRebowlWide(!setup.rebowlWide) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Re-bowl Wide Ball", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    if (setup.rebowlWide) "Standard rule: Extra ball is bowled" else "Box cricket rule: Wide gives runs but counts as ball",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = setup.rebowlWide,
                                onCheckedChange = { viewModel.setRebowlWide(it) }
                            )
                        }

                        // Re-bowl No Ball
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setRebowlNoBall(!setup.rebowlNoBall) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Re-bowl No Ball", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    if (setup.rebowlNoBall) "Standard: Extra ball is bowled" else "No ball gives runs but counts as ball",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = setup.rebowlNoBall,
                                onCheckedChange = { viewModel.setRebowlNoBall(it) }
                            )
                        }

                        // Free Hit on No-Ball
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setFreeHitEnabled(!setup.freeHitEnabled) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Free Hit on No Ball", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    if (setup.freeHitEnabled) "Next delivery is Free Hit (striker safe from bowler dismissal)" else "No free hit awarded",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = setup.freeHitEnabled,
                                onCheckedChange = { viewModel.setFreeHitEnabled(it) }
                            )
                        }
                    }
                }
            }
        } else {
            // Squad Editing Tab (Team A or Team B)
            val isTeamA = selectedTab == 1
            val teamName = if (isTeamA) setup.teamAName else setup.teamBName
            val players = if (isTeamA) setup.teamAPlayers else setup.teamBPlayers

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$teamName Squad (${players.size} Players)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tap to edit names. Tap (C) for Captain, (WK) for Keeper.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.openSaveTeamDialog(isTeamA) },
                        colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Team", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { viewModel.openSavedTeamsDialog(isTeamA) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Load Team", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    if (savedPlayers.isNotEmpty()) {
                        Button(
                            onClick = { showPoolMultiPickerForSquad = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PitchGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.1f).height(38.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("From Pool", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Quick-Add Horizontal Chips from Pool (1-tap add/fill without typing!)
                if (savedPlayers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚡ Quick-Add from Pool (Tap to add):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TurfGreenMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(savedPlayers) { sp ->
                            val inSquad = players.any { it.name.trim().equals(sp.name.trim(), ignoreCase = true) }
                            FilterChip(
                                selected = inSquad,
                                onClick = {
                                    if (!inSquad) {
                                        val placeholderIdx = players.indexOfFirst {
                                            it.name.matches(Regex("Player\\s+\\d+", RegexOption.IGNORE_CASE)) && it.photoUri == null
                                        }
                                        if (placeholderIdx != -1) {
                                            viewModel.replacePlayerInSetup(isTeamA, placeholderIdx, sp)
                                        } else {
                                            viewModel.addPlayerToCurrentSetup(isTeamA, sp)
                                        }
                                    }
                                },
                                leadingIcon = {
                                    PlayerAvatar(photoUri = sp.photoUri, name = sp.name, size = 18.dp)
                                },
                                label = {
                                    Text(
                                        if (inSquad) "✓ ${sp.name}" else "+ ${sp.name}",
                                        fontSize = 11.sp,
                                        fontWeight = if (inSquad) FontWeight.Normal else FontWeight.Bold
                                    )
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            itemsIndexed(players) { index, player ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.width(24.dp)
                        )

                        PlayerAvatar(
                            photoUri = player.photoUri,
                            name = player.name,
                            size = 36.dp,
                            isEditable = true,
                            playerId = player.id,
                            onPhotoChanged = { path ->
                                viewModel.updatePlayerPhoto(isTeamA, index, path)
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = player.name,
                            onValueChange = { viewModel.updatePlayerName(isTeamA, index, it) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )

                        // Pick from pool icon button for this specific slot
                        if (savedPlayers.isNotEmpty()) {
                            IconButton(
                                onClick = { pickingSlotIndex = index },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Default.PersonSearch,
                                    contentDescription = "Pick player from pool",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Captain button
                        FilterChip(
                            selected = player.isCaptain,
                            onClick = { viewModel.setPlayerCaptain(isTeamA, index) },
                            label = { Text("C", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // Wicketkeeper button
                        FilterChip(
                            selected = player.isWicketKeeper,
                            onClick = { viewModel.setPlayerWicketKeeper(isTeamA, index) },
                            label = { Text("WK", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Start Match Button
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onStartMatchClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Match 🏏",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Modal: Multi-Select Pool Picker Dialog for Squad
    if (showPoolMultiPickerForSquad) {
        val isTeamA = selectedTab == 1
        val currentSquad = if (isTeamA) setup.teamAPlayers else setup.teamBPlayers
        PlayerPoolPickerDialog(
            savedPlayers = savedPlayers,
            alreadySelectedPlayerNames = currentSquad.map { it.name }.toSet(),
            isMultiSelect = true,
            onPlayersSelected = { selectedList ->
                selectedList.forEach { sel ->
                    val squad = if (isTeamA) viewModel.uiState.value.setup.teamAPlayers else viewModel.uiState.value.setup.teamBPlayers
                    if (!squad.any { it.name.trim().equals(sel.name.trim(), ignoreCase = true) }) {
                        val placeholderIdx = squad.indexOfFirst {
                            it.name.matches(Regex("Player\\s+\\d+", RegexOption.IGNORE_CASE)) && it.photoUri == null
                        }
                        if (placeholderIdx != -1) {
                            viewModel.replacePlayerInSetup(isTeamA, placeholderIdx, sel)
                        } else {
                            viewModel.addPlayerToCurrentSetup(isTeamA, sel)
                        }
                    }
                }
                showPoolMultiPickerForSquad = false
            },
            onDismiss = { showPoolMultiPickerForSquad = false }
        )
    }

    // Modal: Single-select Pool Picker Dialog to swap or fill a specific slot
    pickingSlotIndex?.let { slotIndex ->
        val isTeamA = selectedTab == 1
        val currentSquad = if (isTeamA) setup.teamAPlayers else setup.teamBPlayers
        PlayerPoolPickerDialog(
            savedPlayers = savedPlayers,
            alreadySelectedPlayerNames = currentSquad.map { it.name }.toSet(),
            isMultiSelect = false,
            onPlayersSelected = { selectedList ->
                if (selectedList.isNotEmpty()) {
                    viewModel.replacePlayerInSetup(isTeamA, slotIndex, selectedList.first())
                }
                pickingSlotIndex = null
            },
            onDismiss = { pickingSlotIndex = null }
        )
    }
}
