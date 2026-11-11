package com.cricket.scoreboard.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cricket.scoreboard.model.Player
import com.cricket.scoreboard.model.PlayerRole
import com.cricket.scoreboard.model.Team
import com.cricket.scoreboard.ui.components.PlayerAvatar
import com.cricket.scoreboard.ui.theme.PitchGold
import com.cricket.scoreboard.ui.theme.TurfGreenDark
import com.cricket.scoreboard.ui.theme.TurfGreenLight
import com.cricket.scoreboard.ui.theme.TurfGreenMedium
import java.util.UUID

/**
 * Dialog to create or edit an individual player in the Player Pool.
 */
@Composable
fun IndividualPlayerEditorDialog(
    initialPlayer: Player? = null,
    onSave: (Player) -> Unit,
    onDismiss: () -> Unit
) {
    val playerId = remember { initialPlayer?.id ?: UUID.randomUUID().toString() }
    var name by remember { mutableStateOf(initialPlayer?.name ?: "") }
    var role by remember { mutableStateOf(initialPlayer?.role ?: PlayerRole.ALL_ROUNDER) }
    var photoUri by remember { mutableStateOf(initialPlayer?.photoUri) }
    var isCaptain by remember { mutableStateOf(initialPlayer?.isCaptain ?: false) }
    var isWicketKeeper by remember { mutableStateOf(initialPlayer?.isWicketKeeper ?: false) }

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
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialPlayer == null) "➕ Add Individual Player" else "✏️ Edit Player",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Avatar with camera icon for adding/changing photo
                PlayerAvatar(
                    photoUri = photoUri,
                    name = name.ifEmpty { "New Player" },
                    size = 72.dp,
                    isEditable = true,
                    playerId = playerId,
                    onPhotoChanged = { newUri -> photoUri = newUri }
                )

                Text(
                    text = "Tap avatar to add/change player photo",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Player Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Player Name") },
                    placeholder = { Text("e.g. Virat Kohli or Rinash") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Player Role Selection
                Text(
                    text = "Player Role",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        PlayerRole.BATSMAN,
                        PlayerRole.BOWLER,
                        PlayerRole.ALL_ROUNDER,
                        PlayerRole.WICKET_KEEPER
                    ).forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = {
                                role = r
                                if (r == PlayerRole.WICKET_KEEPER) isWicketKeeper = true
                            },
                            label = { Text(r.label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
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
                            if (name.isNotBlank()) {
                                onSave(
                                    Player(
                                        id = playerId,
                                        name = name.trim(),
                                        role = role,
                                        isCaptain = isCaptain,
                                        isWicketKeeper = isWicketKeeper || role == PlayerRole.WICKET_KEEPER,
                                        photoUri = photoUri
                                    )
                                )
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (initialPlayer == null) "Add to Pool" else "Save Changes")
                    }
                }
            }
        }
    }
}

/**
 * Dialog to pick one or more players from the saved Player Pool.
 * Used when creating/editing a team or during match setup so users never have to type names!
 */
@Composable
fun PlayerPoolPickerDialog(
    savedPlayers: List<Player>,
    alreadySelectedPlayerNames: Set<String> = emptySet(),
    isMultiSelect: Boolean = true,
    onPlayersSelected: (List<Player>) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf<PlayerRole?>(null) }
    val chosenPlayerIds = remember { mutableStateListOf<String>() }

    val filteredPlayers = remember(savedPlayers, searchQuery, selectedRoleFilter) {
        savedPlayers.filter { p ->
            val matchesQuery = searchQuery.isBlank() || p.name.contains(searchQuery, ignoreCase = true)
            val matchesRole = selectedRoleFilter == null || p.role == selectedRoleFilter
            matchesQuery && matchesRole
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isMultiSelect) "🏏 Select Players from Pool" else "🏏 Select a Player",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isMultiSelect) "Tap players to add to team without typing" else "Choose player to fill this slot",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by player name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TurfGreenMedium) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Role Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = selectedRoleFilter == null,
                        onClick = { selectedRoleFilter = null },
                        label = { Text("All", fontSize = 10.sp) }
                    )
                    listOf(
                        PlayerRole.BATSMAN,
                        PlayerRole.BOWLER,
                        PlayerRole.ALL_ROUNDER,
                        PlayerRole.WICKET_KEEPER
                    ).forEach { r ->
                        FilterChip(
                            selected = selectedRoleFilter == r,
                            onClick = { selectedRoleFilter = if (selectedRoleFilter == r) null else r },
                            label = { Text(r.label, fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredPlayers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PersonOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (savedPlayers.isEmpty()) "No saved players in pool yet.\nAdd players individually first!" else "No players match your search.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredPlayers, key = { it.id }) { player ->
                            val isChosen = chosenPlayerIds.contains(player.id)
                            val isAlreadyInTeam = alreadySelectedPlayerNames.any { it.trim().equals(player.name.trim(), ignoreCase = true) }

                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        isChosen -> TurfGreenMedium.copy(alpha = 0.2f)
                                        isAlreadyInTeam -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    }
                                ),
                                border = if (isChosen) BorderStroke(1.5.dp, TurfGreenMedium) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (!isMultiSelect) {
                                            onPlayersSelected(listOf(player))
                                        } else {
                                            if (isChosen) chosenPlayerIds.remove(player.id)
                                            else chosenPlayerIds.add(player.id)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PlayerAvatar(
                                        photoUri = player.photoUri,
                                        name = player.name,
                                        size = 36.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = player.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            if (isAlreadyInTeam) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "(Already in team)",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Text(
                                            text = player.role.label,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    if (isMultiSelect) {
                                        Checkbox(
                                            checked = isChosen,
                                            onCheckedChange = { checked ->
                                                if (checked) chosenPlayerIds.add(player.id)
                                                else chosenPlayerIds.remove(player.id)
                                            }
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.AddCircle,
                                            contentDescription = "Select",
                                            tint = TurfGreenMedium,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Footer actions
                if (isMultiSelect) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${chosenPlayerIds.size} selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TurfGreenMedium
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onDismiss) { Text("Cancel") }
                            Button(
                                onClick = {
                                    val selectedList = savedPlayers.filter { chosenPlayerIds.contains(it.id) }
                                    onPlayersSelected(selectedList)
                                },
                                enabled = chosenPlayerIds.isNotEmpty(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add to Squad (${chosenPlayerIds.size})")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog to add a specific individual player to one of the user's saved teams.
 */
@Composable
fun AddPlayerToTeamDialog(
    player: Player,
    savedTeams: List<Team>,
    onTeamSelected: (teamId: String) -> Unit,
    onDismiss: () -> Unit
) {
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
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Add to Team",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Choose team to add '${player.name}':",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (savedTeams.isEmpty()) {
                    Text(
                        text = "No saved teams found. Please create a team first in the Teams tab.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 14.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedTeams, key = { it.id }) { team ->
                            val alreadyInTeam = team.players.any { it.name.trim().equals(player.name.trim(), ignoreCase = true) }
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (alreadyInTeam) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !alreadyInTeam) {
                                        onTeamSelected(team.id)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = team.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${team.players.size} players in squad",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (alreadyInTeam) {
                                        Text(
                                            text = "Already in squad",
                                            fontSize = 11.sp,
                                            color = TurfGreenLight,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Button(
                                            onClick = { onTeamSelected(team.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text("+ Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
