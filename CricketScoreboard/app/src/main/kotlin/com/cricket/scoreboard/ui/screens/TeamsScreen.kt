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
            savedPlayers = savedPlayers,
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
    onSave: (Team) -> Unit,
    onDismiss: () -> Unit
) {
    var teamName by remember { mutableStateOf(initialTeam.name) }
    var players by remember { mutableStateOf(initialTeam.players) }

    var showPoolMultiPicker by remember { mutableStateOf(false) }
    var pickingSlotIndex by remember { mutableStateOf<Int?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(4.dp)
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

                Spacer(modifier = Modifier.height(8.dp))

                // Team Name Input
                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("Team Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Squad Size and Action Bar: "Add from Player Pool" button + squad stepper
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
                        if (savedPlayers.isNotEmpty()) {
                            Button(
                                onClick = { showPoolMultiPicker = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PitchGold),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pick from Pool", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

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

                // Horizontal Quick-Add Chips for available Pool Players (1-tap to add into squad!)
                if (savedPlayers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚡ Quick-Add Player (Tap to add to squad):",
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
                                        val mutable = players.toMutableList()
                                        // Replace placeholder "Player X" if present
                                        val placeholderIdx = mutable.indexOfFirst {
                                            it.name.matches(Regex("Player\\s+\\d+", RegexOption.IGNORE_CASE)) && it.photoUri == null
                                        }
                                        if (placeholderIdx != -1) {
                                            mutable[placeholderIdx] = sp.copy(
                                                id = UUID.randomUUID().toString(),
                                                isCaptain = mutable[placeholderIdx].isCaptain,
                                                isWicketKeeper = mutable[placeholderIdx].isWicketKeeper || sp.isWicketKeeper
                                            )
                                        } else {
                                            mutable.add(sp.copy(id = UUID.randomUUID().toString()))
                                        }
                                        players = mutable
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

                                    // Pick from Pool button for this specific slot
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

    // Modal: Multi-Select from Player Pool into Squad
    if (showPoolMultiPicker) {
        PlayerPoolPickerDialog(
            savedPlayers = savedPlayers,
            alreadySelectedPlayerNames = players.map { it.name }.toSet(),
            isMultiSelect = true,
            onPlayersSelected = { selectedList ->
                val mutable = players.toMutableList()
                selectedList.forEach { sel ->
                    if (!mutable.any { it.name.trim().equals(sel.name.trim(), ignoreCase = true) }) {
                        val placeholderIdx = mutable.indexOfFirst {
                            it.name.matches(Regex("Player\\s+\\d+", RegexOption.IGNORE_CASE)) && it.photoUri == null
                        }
                        if (placeholderIdx != -1) {
                            mutable[placeholderIdx] = sel.copy(
                                id = UUID.randomUUID().toString(),
                                isCaptain = mutable[placeholderIdx].isCaptain,
                                isWicketKeeper = mutable[placeholderIdx].isWicketKeeper || sel.isWicketKeeper
                            )
                        } else {
                            mutable.add(sel.copy(id = UUID.randomUUID().toString()))
                        }
                    }
                }
                players = mutable
                showPoolMultiPicker = false
            },
            onDismiss = { showPoolMultiPicker = false }
        )
    }

    // Modal: Single-select from Player Pool to fill or swap a specific slot
    pickingSlotIndex?.let { slotIndex ->
        PlayerPoolPickerDialog(
            savedPlayers = savedPlayers,
            alreadySelectedPlayerNames = players.map { it.name }.toSet(),
            isMultiSelect = false,
            onPlayersSelected = { selectedList ->
                if (selectedList.isNotEmpty() && slotIndex in players.indices) {
                    val sel = selectedList.first()
                    val mutable = players.toMutableList()
                    mutable[slotIndex] = sel.copy(
                        id = UUID.randomUUID().toString(),
                        isCaptain = mutable[slotIndex].isCaptain,
                        isWicketKeeper = sel.isWicketKeeper || mutable[slotIndex].isWicketKeeper
                    )
                    players = mutable
                }
                pickingSlotIndex = null
            },
            onDismiss = { pickingSlotIndex = null }
        )
    }
}
