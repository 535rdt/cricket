package com.cricket.scoreboard.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cricket.scoreboard.model.Player
import com.cricket.scoreboard.model.PlayerRole
import com.cricket.scoreboard.model.Team
import com.cricket.scoreboard.ui.components.PlayerAvatar
import com.cricket.scoreboard.ui.dialogs.AddPlayerToTeamDialog
import com.cricket.scoreboard.ui.dialogs.IndividualPlayerEditorDialog
import com.cricket.scoreboard.ui.dialogs.PlayerPoolPickerDialog
import com.cricket.scoreboard.ui.theme.PitchGold
import com.cricket.scoreboard.ui.theme.TurfGreenDark
import com.cricket.scoreboard.ui.theme.TurfGreenLight
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
    val savedPlayers = uiState.savedPlayers
    val setup = uiState.setup

    var selectedTab by remember { mutableStateOf(0) } // 0: Saved Teams, 1: Player Pool

    // Dialogs state for Teams
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTeam by remember { mutableStateOf<Team?>(null) }
    var expandedTeamIds by remember { mutableStateOf(setOf<String>()) }

    // Dialogs state for Player Pool
    var showPlayerEditor by remember { mutableStateOf(false) }
    var editingPlayer by remember { mutableStateOf<Player?>(null) }
    var playerForAddToTeam by remember { mutableStateOf<Player?>(null) }
    var poolSearchQuery by remember { mutableStateOf("") }
    var poolRoleFilter by remember { mutableStateOf<PlayerRole?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Top Navigation Tabs: Saved Teams vs Player Library
        PrimaryTabRow(
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
                        Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Teams (${savedTeams.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Player Pool (${savedPlayers.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedTab == 0) {
            // ==================== TAB 0: SAVED TEAMS ====================
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
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
                                    text = "Save custom rosters & build teams directly from Player Pool",
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
                                    textAlign = TextAlign.Center
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
        } else {
            // ==================== TAB 1: PLAYER POOL (LIBRARY) ====================
            val filteredPool = remember(savedPlayers, poolSearchQuery, poolRoleFilter) {
                savedPlayers.filter { p ->
                    val matchesQuery = poolSearchQuery.isBlank() || p.name.contains(poolSearchQuery, ignoreCase = true)
                    val matchesRole = poolRoleFilter == null || p.role == poolRoleFilter
                    matchesQuery && matchesRole
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Player Pool Header Banner
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TurfGreenDark),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "🏏 INDIVIDUAL PLAYER POOL",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Add players once with photo & role. Add to any team without re-typing!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        editingPlayer = null
                                        showPlayerEditor = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PitchGold),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Player", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.importPlayersFromTeams() },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                                    modifier = Modifier.weight(1.2f).height(38.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import from Teams", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Search & Filter
                item {
                    OutlinedTextField(
                        value = poolSearchQuery,
                        onValueChange = { poolSearchQuery = it },
                        placeholder = { Text("Search player by name...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TurfGreenMedium) },
                        trailingIcon = {
                            if (poolSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { poolSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Role filter chips
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = poolRoleFilter == null,
                                onClick = { poolRoleFilter = null },
                                label = { Text("All (${savedPlayers.size})", fontSize = 11.sp) }
                            )
                        }
                        items(
                            listOf(
                                PlayerRole.BATSMAN,
                                PlayerRole.BOWLER,
                                PlayerRole.ALL_ROUNDER,
                                PlayerRole.WICKET_KEEPER
                            )
                        ) { r ->
                            val count = savedPlayers.count { it.role == r }
                            FilterChip(
                                selected = poolRoleFilter == r,
                                onClick = { poolRoleFilter = if (poolRoleFilter == r) null else r },
                                label = { Text("${r.label} ($count)", fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Player Cards List
                if (filteredPool.isEmpty()) {
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
                                    Icons.Default.PersonOff,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (savedPlayers.isEmpty()) "No saved players in pool yet" else "No players match '$poolSearchQuery'",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (savedPlayers.isEmpty()) "Add individual players or tap 'Import from Teams' to pull existing players into this library!" else "Try a different search term or role filter.",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                if (savedPlayers.isEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                editingPlayer = null
                                                showPlayerEditor = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Add First Player")
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.importPlayersFromTeams() }
                                        ) {
                                            Text("Import from Teams")
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredPool, key = { it.id }) { player ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlayerAvatar(
                                    photoUri = player.photoUri,
                                    name = player.name,
                                    size = 46.dp,
                                    isEditable = true,
                                    playerId = player.id,
                                    onPhotoChanged = { newUri ->
                                        viewModel.saveIndividualPlayer(player.copy(photoUri = newUri))
                                    }
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = player.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = TurfGreenMedium.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = player.role.label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TurfGreenMedium,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        if (player.isCaptain) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = PitchGold.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "👑 Capt",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF8B6508),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        if (player.isWicketKeeper) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = "🧤 WK",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // "+ Add to Team" button
                                Button(
                                    onClick = { playerForAddToTeam = player },
                                    colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Team", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Edit Player
                                IconButton(
                                    onClick = {
                                        editingPlayer = player
                                        showPlayerEditor = true
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Player", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                                }

                                // Delete Player
                                IconButton(
                                    onClick = { viewModel.deleteIndividualPlayer(player.id) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Player", tint = Color.Gray, modifier = Modifier.size(17.dp))
                                }
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
                players = emptyList() // Start empty so user can easily tap to pick players from pool!
            ),
            isNewTeam = true,
            savedPlayers = savedPlayers,
            onSaveNewPlayer = { viewModel.saveIndividualPlayer(it) },
            onImportPlayersFromTeams = { viewModel.importPlayersFromTeams() },
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
            savedPlayers = savedPlayers,
            onSaveNewPlayer = { viewModel.saveIndividualPlayer(it) },
            onImportPlayersFromTeams = { viewModel.importPlayersFromTeams() },
            onSave = { updatedTeam ->
                viewModel.updateOrSaveTeam(updatedTeam)
                editingTeam = null
            },
            onDismiss = { editingTeam = null }
        )
    }

    // Individual Player Editor Dialog (Add or Edit)
    if (showPlayerEditor) {
        IndividualPlayerEditorDialog(
            initialPlayer = editingPlayer,
            onSave = { saved ->
                viewModel.saveIndividualPlayer(saved)
                showPlayerEditor = false
                editingPlayer = null
            },
            onDismiss = {
                showPlayerEditor = false
                editingPlayer = null
            }
        )
    }

    // Add Player to Team Dialog
    playerForAddToTeam?.let { player ->
        AddPlayerToTeamDialog(
            player = player,
            savedTeams = savedTeams,
            onTeamSelected = { teamId ->
                viewModel.addPlayerToTeam(teamId, player)
                playerForAddToTeam = null
            },
            onDismiss = { playerForAddToTeam = null }
        )
    }
}

/**
 * Enhanced TeamEditorDialog:
 * - Direct quick-add chips for available saved players from the pool.
 * - "➕ Add from Player Pool" button to multi-select players without typing.
 * - "👤 Pick" icon on each row to swap or select a player from the pool without typing.
 */
@Composable
fun TeamEditorDialog(
    initialTeam: Team,
    isNewTeam: Boolean,
    savedPlayers: List<Player> = emptyList(),
    onSaveNewPlayer: ((Player) -> Unit)? = null,
    onImportPlayersFromTeams: (() -> Unit)? = null,
    onSave: (Team) -> Unit,
    onDismiss: () -> Unit
) {
    var teamName by remember { mutableStateOf(initialTeam.name) }
    var players by remember { mutableStateOf(initialTeam.players) }
    var targetSquadSize by remember {
        mutableStateOf(if (initialTeam.players.isNotEmpty()) initialTeam.players.size else 11)
    }

    var editorMode by remember { mutableStateOf(if (savedPlayers.isNotEmpty()) 0 else 1) }
    var poolSearchQuery by remember { mutableStateOf("") }
    var poolRoleFilter by remember { mutableStateOf<PlayerRole?>(null) }
    var showNewPlayerDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
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

                Spacer(modifier = Modifier.height(6.dp))

                // Team Name Input
                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("Team Name") },
                    placeholder = { Text("e.g. Mumbai Kings, Royal Challengers") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mode Tabs: [⚡ Pick from Pool] vs [✍️ Manual List]
                TabRow(
                    selectedTabIndex = editorMode,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = editorMode == 0,
                        onClick = { editorMode = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = PitchGold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("⚡ Tap to Pick (${players.size}/$targetSquadSize)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = editorMode == 1,
                        onClick = { editorMode = 1 },
                        text = {
                            Text("✍️ Manual (${players.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (editorMode == 0) {
                    // ========================================================
                    // MODE 0: ⚡ 1-Tap Pick from Player Pool
                    // ========================================================

                    // Action Controls: Target Squad Size + Auto-Fill + Clear + New Player
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Target Squad Size Stepper
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Target: $targetSquadSize",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FilledTonalIconButton(
                                onClick = { if (targetSquadSize > 2) targetSquadSize-- },
                                enabled = targetSquadSize > 2,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("—", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            FilledTonalIconButton(
                                onClick = { if (targetSquadSize < 30) targetSquadSize++ },
                                enabled = targetSquadSize < 30,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }

                        // Action Buttons: Auto-Fill, Clear, Add Player
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (savedPlayers.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        val currentList = players.toMutableList()
                                        for (sp in savedPlayers) {
                                            if (currentList.size >= targetSquadSize) break
                                            if (!currentList.any { it.name.trim().equals(sp.name.trim(), ignoreCase = true) }) {
                                                currentList.add(sp.copy(id = UUID.randomUUID().toString()))
                                            }
                                        }
                                        players = currentList
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PitchGold),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("⚡ Auto-Fill $targetSquadSize", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (players.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { players = emptyList() },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Clear", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }

                            FilledTonalButton(
                                onClick = { showNewPlayerDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("+ Player", fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Selected Squad Shelf (Horizontal scrollable cards showing who has been picked)
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Selected Squad (${players.size}/$targetSquadSize):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (players.size >= targetSquadSize) TurfGreenMedium else MaterialTheme.colorScheme.primary
                                )
                                if (players.size >= targetSquadSize) {
                                    Text("✓ Ready", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TurfGreenMedium)
                                }
                            }

                            if (players.isEmpty()) {
                                Text(
                                    text = "No players selected yet. Tap players from the list below to add them directly!",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 2.dp)
                                ) {
                                    itemsIndexed(players) { index, player ->
                                        Card(
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, TurfGreenMedium.copy(alpha = 0.5f)),
                                            modifier = Modifier.width(115.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "#${index + 1}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            players = players.filterIndexed { i, _ -> i != index }
                                                        },
                                                        modifier = Modifier.size(18.dp)
                                                    ) {
                                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(13.dp))
                                                    }
                                                }

                                                PlayerAvatar(photoUri = player.photoUri, name = player.name, size = 32.dp)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = player.name,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = player.role.label,
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Captain Chip
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = if (player.isCaptain) TurfGreenMedium else MaterialTheme.colorScheme.surfaceVariant,
                                                        modifier = Modifier.clickable {
                                                            players = players.mapIndexed { i, p ->
                                                                p.copy(isCaptain = (i == index && !player.isCaptain))
                                                            }
                                                        }
                                                    ) {
                                                        Text(
                                                            text = "C",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (player.isCaptain) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    // Keeper Chip
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = if (player.isWicketKeeper) PitchGold else MaterialTheme.colorScheme.surfaceVariant,
                                                        modifier = Modifier.clickable {
                                                            players = players.mapIndexed { i, p ->
                                                                p.copy(isWicketKeeper = (i == index && !player.isWicketKeeper))
                                                            }
                                                        }
                                                    ) {
                                                        Text(
                                                            text = "WK",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (player.isWicketKeeper) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search & Role Filter for the Pool
                    OutlinedTextField(
                        value = poolSearchQuery,
                        onValueChange = { poolSearchQuery = it },
                        placeholder = { Text("Search pool player by name...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Filter Chips (All, Batsman, Bowler, etc.)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = poolRoleFilter == null,
                                onClick = { poolRoleFilter = null },
                                label = { Text("All (${savedPlayers.size})", fontSize = 11.sp) }
                            )
                        }
                        PlayerRole.values().forEach { role ->
                            val count = savedPlayers.count { it.role == role }
                            item {
                                FilterChip(
                                    selected = poolRoleFilter == role,
                                    onClick = { poolRoleFilter = if (poolRoleFilter == role) null else role },
                                    label = { Text("${role.label} ($count)", fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Available Players List
                    val filteredPool = remember(savedPlayers, poolSearchQuery, poolRoleFilter) {
                        savedPlayers.filter { p ->
                            (poolSearchQuery.isBlank() || p.name.contains(poolSearchQuery, ignoreCase = true)) &&
                            (poolRoleFilter == null || p.role == poolRoleFilter)
                        }
                    }

                    if (savedPlayers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Your player pool is currently empty.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (onImportPlayersFromTeams != null) {
                                    Button(
                                        onClick = { onImportPlayersFromTeams() },
                                        colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("📥 Auto-Import from Existing Teams")
                                    }
                                }
                                OutlinedButton(onClick = { showNewPlayerDialog = true }) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("➕ Add Individual Player")
                                }
                            }
                        }
                    } else if (filteredPool.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No players matching \"$poolSearchQuery\"", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredPool, key = { it.id }) { poolPlayer ->
                                val inSquad = players.any { it.name.trim().equals(poolPlayer.name.trim(), ignoreCase = true) }
                                val isSquadFull = !inSquad && players.size >= targetSquadSize

                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (inSquad)
                                            TurfGreenMedium.copy(alpha = 0.12f)
                                        else
                                            MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (inSquad) TurfGreenMedium else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (inSquad) {
                                                players = players.filterNot { it.name.trim().equals(poolPlayer.name.trim(), ignoreCase = true) }
                                            } else if (!isSquadFull) {
                                                players = players + poolPlayer.copy(id = UUID.randomUUID().toString())
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PlayerAvatar(photoUri = poolPlayer.photoUri, name = poolPlayer.name, size = 38.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = poolPlayer.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                if (poolPlayer.isCaptain) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("(C)", fontSize = 11.sp, color = TurfGreenMedium, fontWeight = FontWeight.Bold)
                                                }
                                                if (poolPlayer.isWicketKeeper) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("(WK)", fontSize = 11.sp, color = PitchGold, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Text(
                                                text = poolPlayer.role.label,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Status action indicator
                                        if (inSquad) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = TurfGreenMedium.copy(alpha = 0.2f)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = TurfGreenMedium, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("In Squad", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TurfGreenMedium)
                                                    }
                                                }
                                                IconButton(
                                                    onClick = {
                                                        players = players.filterNot { it.name.trim().equals(poolPlayer.name.trim(), ignoreCase = true) }
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        } else if (isSquadFull) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                            ) {
                                                Text(
                                                    text = "Full ($targetSquadSize)",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                                )
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    if (players.size < targetSquadSize) {
                                                        players = players + poolPlayer.copy(id = UUID.randomUUID().toString())
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ========================================================
                    // MODE 1: ✍️ Manual List
                    // ========================================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Squad: ${players.size} Players",
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
                                modifier = Modifier.size(34.dp)
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
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add player", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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
                                            modifier = Modifier.width(24.dp)
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

                                        Spacer(modifier = Modifier.width(4.dp))

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
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Dialog Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${players.size} Players in team",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (teamName.isNotBlank() && players.isNotEmpty()) {
                                    onSave(initialTeam.copy(name = teamName.trim(), players = players))
                                }
                            },
                            enabled = teamName.isNotBlank() && players.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium)
                        ) {
                            Text("Save Team (${players.size})", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Modal: Create a new individual player on the fly and immediately add to squad
    if (showNewPlayerDialog) {
        IndividualPlayerEditorDialog(
            initialPlayer = null,
            onSave = { newPlayer ->
                onSaveNewPlayer?.invoke(newPlayer)
                if (!players.any { it.name.trim().equals(newPlayer.name.trim(), ignoreCase = true) }) {
                    players = players + newPlayer.copy(id = UUID.randomUUID().toString())
                }
                showNewPlayerDialog = false
            },
            onDismiss = { showNewPlayerDialog = false }
        )
    }
}
