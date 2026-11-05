package com.cricket.scoreboard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cricket.scoreboard.model.Player
import com.cricket.scoreboard.model.PlayerRole
import com.cricket.scoreboard.model.Team
import com.cricket.scoreboard.ui.components.PlayerAvatar
import com.cricket.scoreboard.ui.theme.PitchGold
import com.cricket.scoreboard.ui.theme.TurfGreenDark
import com.cricket.scoreboard.ui.theme.TurfGreenMedium
import com.cricket.scoreboard.viewmodel.ActiveScreen
import com.cricket.scoreboard.viewmodel.CricketViewModel
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeamsScreen(
    viewModel: CricketViewModel,
    onNavigateToSetup: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val savedTeams = uiState.savedTeams
    val setup = uiState.setup

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTeam by remember { mutableStateOf<Team?>(null) }
    var expandedTeamIds by remember { mutableStateOf(setOf<String>()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        // Top Title Banner
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
                            text = "👥 SAVED TEAMS & SQUADS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Save custom rosters & start new games with any team",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PitchGold),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Team", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Current Active Setup Teams Card
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
                            text = "⚡ Teams in Current Setup",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = onNavigateToSetup,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Go to Setup ⚙️", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Team 1 Quick Save
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Team 1: ${setup.teamAName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${setup.teamAPlayers.size} Players in squad",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { viewModel.saveCurrentTeam(isTeamA = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Team 2 Quick Save
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Team 2: ${setup.teamBName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${setup.teamBPlayers.size} Players in squad",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { viewModel.saveCurrentTeam(isTeamA = false) },
                            colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Saved Teams Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📁 Saved Teams Library (${savedTeams.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Empty state
        if (savedTeams.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Groups,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No saved teams yet.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Create custom teams to easily load squads into matches.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Team")
                        }
                    }
                }
            }
        } else {
            // List of Saved Teams
            items(savedTeams, key = { it.id }) { team ->
                val isExpanded = expandedTeamIds.contains(team.id)

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Title row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = team.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface,
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
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Captain & Keeper Badges
                        val captain = team.players.find { it.isCaptain }?.name
                        val keeper = team.players.find { it.isWicketKeeper }?.name
                        if (captain != null || keeper != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (captain != null) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = PitchGold.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "👑 (C) $captain",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF8B6508),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (keeper != null) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "🧤 (WK) $keeper",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Player names preview or full roster
                        if (!isExpanded) {
                            val sample = team.players.take(5).joinToString(", ") { it.name }
                            val remainder = team.players.size - 5
                            Text(
                                text = if (remainder > 0) "$sample +$remainder more" else sample,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            // Full expanded squad view
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                team.players.forEachIndexed { i, p ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (i % 2 == 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f) else Color.Transparent,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${i + 1}.",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(24.dp)
                                        )
                                        PlayerAvatar(
                                            photoUri = p.photoUri,
                                            name = p.name,
                                            size = 26.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = p.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (p.isCaptain) {
                                            Text(
                                                text = "(C)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PitchGold,
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                        }
                                        if (p.isWicketKeeper) {
                                            Text(
                                                text = "(WK)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = p.role.label,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Toggle Expand / Collapse
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedTeamIds = if (isExpanded) expandedTeamIds - team.id else expandedTeamIds + team.id
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isExpanded) "Hide Squad ▲" else "View Full Squad (${team.players.size}) ▼",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Action Buttons: Load as Team 1, Load as Team 2, Edit, Delete
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    viewModel.loadTeamIntoSetup(team, isTeamA = true)
                                    onNavigateToSetup()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium)
                            ) {
                                Text("Load Team 1", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.loadTeamIntoSetup(team, isTeamA = false)
                                    onNavigateToSetup()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TurfGreenDark)
                            ) {
                                Text("Load Team 2", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = { editingTeam = team },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Team", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = { viewModel.deleteSavedTeam(team.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Team", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Create New Team Dialog
    if (showCreateDialog) {
        TeamEditorDialog(
            initialTeam = Team(
                id = UUID.randomUUID().toString(),
                name = "New Team",
                players = (1..11).map { i ->
                    Player(
                        id = UUID.randomUUID().toString(),
                        name = "Player $i",
                        role = when (i) {
                            1, 2, 3, 4 -> PlayerRole.BATSMAN
                            5 -> PlayerRole.WICKET_KEEPER
                            6, 7 -> PlayerRole.ALL_ROUNDER
                            else -> PlayerRole.BOWLER
                        },
                        isCaptain = i == 1,
                        isWicketKeeper = i == 5
                    )
                }
            ),
            isNewTeam = true,
            onSave = { newTeam ->
                viewModel.updateOrSaveTeam(newTeam)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    // Edit Existing Team Dialog
    editingTeam?.let { teamToEdit ->
        TeamEditorDialog(
            initialTeam = teamToEdit,
            isNewTeam = false,
            onSave = { updatedTeam ->
                viewModel.updateOrSaveTeam(updatedTeam)
                editingTeam = null
            },
            onDismiss = { editingTeam = null }
        )
    }
}

@Composable
fun TeamEditorDialog(
    initialTeam: Team,
    isNewTeam: Boolean,
    onSave: (Team) -> Unit,
    onDismiss: () -> Unit
) {
    var teamName by remember { mutableStateOf(initialTeam.name) }
    var players by remember { mutableStateOf(initialTeam.players) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isNewTeam) "➕ Create Team" else "✏️ Edit Team",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Team Name Input
                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("Team Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Player count stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Squad Size: ${players.size} Players",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalIconButton(
                            onClick = {
                                if (players.size > 2) {
                                    players = players.dropLast(1)
                                }
                            },
                            enabled = players.size > 2,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("—", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalIconButton(
                            onClick = {
                                if (players.size < 30) {
                                    val nextNum = players.size + 1
                                    players = players + Player(
                                        id = UUID.randomUUID().toString(),
                                        name = "Player $nextNum",
                                        role = PlayerRole.ALL_ROUNDER
                                    )
                                }
                            },
                            enabled = players.size < 30,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add player", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Editable Players List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(players.size) { index ->
                        val player = players[index]

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}.",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.width(26.dp)
                                    )

                                    PlayerAvatar(
                                        photoUri = player.photoUri,
                                        name = player.name,
                                        size = 36.dp,
                                        isEditable = true,
                                        playerId = player.id,
                                        onPhotoChanged = { path ->
                                            val updated = players.toMutableList()
                                            updated[index] = player.copy(photoUri = path)
                                            players = updated
                                        }
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    OutlinedTextField(
                                        value = player.name,
                                        onValueChange = { newName ->
                                            val updated = players.toMutableList()
                                            updated[index] = player.copy(name = newName)
                                            players = updated
                                        },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        textStyle = MaterialTheme.typography.bodyMedium
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Captain Chip
                                    FilterChip(
                                        selected = player.isCaptain,
                                        onClick = {
                                            players = players.mapIndexed { i, p ->
                                                p.copy(isCaptain = (i == index && !player.isCaptain))
                                            }
                                        },
                                        label = { Text("C", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Keeper Chip
                                    FilterChip(
                                        selected = player.isWicketKeeper,
                                        onClick = {
                                            players = players.mapIndexed { i, p ->
                                                p.copy(isWicketKeeper = (i == index && !player.isWicketKeeper))
                                            }
                                        },
                                        label = { Text("WK", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                                    )
                                }

                                // Role selection row
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(PlayerRole.BATSMAN, PlayerRole.BOWLER, PlayerRole.ALL_ROUNDER, PlayerRole.WICKET_KEEPER).forEach { r ->
                                        FilterChip(
                                            selected = player.role == r,
                                            onClick = {
                                                val updated = players.toMutableList()
                                                updated[index] = player.copy(role = r)
                                                players = updated
                                            },
                                            label = { Text(r.label, fontSize = 10.sp) },
                                            modifier = Modifier.height(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Dialog Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (teamName.isNotBlank() && players.isNotEmpty()) {
                                onSave(initialTeam.copy(name = teamName.trim(), players = players))
                            }
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
