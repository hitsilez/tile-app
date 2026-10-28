package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.viewmodel.TileCalculatorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaEditorScreen(
    area: AreaItem,
    unit: UnitSystem,
    viewModel: TileCalculatorViewModel,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onDone() }

    val activeJob by viewModel.activeJob.collectAsState()
    val trimSettings = activeJob?.trimSettings ?: TrimSettingsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onDone,
                        modifier = Modifier.testTag("area_editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Edit ${area.type.displayName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = area.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = onDone,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("area_done_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Done",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Done",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp)
        ) {
            // Area Name Input Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "AREA LABEL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = area.name,
                            onValueChange = { viewModel.updateEditingAreaName(it) },
                            label = { Text("Area Name (e.g. Master Bedroom, Front Veranda)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("area_name_input")
                        )
                    }
                }
            }

            // Space Type Specific Inputs
            item {
                when (area.type) {
                    SpaceType.ROOM_FLOOR -> {
                        RoomFloorScreen(
                            roomState = area.roomState,
                            unit = unit,
                            onUpdate = { transform -> viewModel.updateEditingRoomState(transform) },
                            onAddDoorway = { viewModel.addEditingRoomDoorway() },
                            onRemoveDoorway = { viewModel.removeEditingRoomDoorway(it) },
                            onUpdateDoorway = { idx, w -> viewModel.updateEditingRoomDoorway(idx, w) }
                        )
                    }
                    SpaceType.VERANDA -> {
                        VerandaScreen(
                            verandaState = area.verandaState,
                            unit = unit,
                            onUpdate = { transform -> viewModel.updateEditingVerandaState(transform) }
                        )
                    }
                    SpaceType.BATHROOM -> {
                        BathroomScreen(
                            bathroomState = area.bathroomState,
                            unit = unit,
                            onUpdate = { transform -> viewModel.updateEditingBathroomState(transform) },
                            onAddDoorway = { viewModel.addEditingBathroomDoorway() },
                            onRemoveDoorway = { viewModel.removeEditingBathroomDoorway(it) },
                            onUpdateDoorway = { idx, w -> viewModel.updateEditingBathroomDoorway(idx, w) },
                            onAddWall = { viewModel.addEditingBathroomWall() },
                            onRemoveWall = { viewModel.removeEditingBathroomWall(it) },
                            onUpdateWall = { idx, w, h -> viewModel.updateEditingBathroomWall(idx, w, h) },
                            onAddWindow = { viewModel.addEditingBathroomWindow() },
                            onRemoveWindow = { viewModel.removeEditingBathroomWindow(it) },
                            onUpdateWindow = { idx, w, h -> viewModel.updateEditingBathroomWindow(idx, w, h) },
                            onAddDoor = { viewModel.addEditingBathroomDoor() },
                            onRemoveDoor = { viewModel.removeEditingBathroomDoor(it) },
                            onUpdateDoor = { idx, w, h -> viewModel.updateEditingBathroomDoor(idx, w, h) }
                        )
                    }
                }
            }

            // Trims Card (comes after the skirting card for EVERY area type: Room floor, Passage, Veranda, Bathroom)
            item {
                com.example.ui.components.AreaTrimsCard(
                    area = area,
                    unit = unit,
                    trimSettings = trimSettings,
                    onUpdateTrimSettings = { update -> viewModel.updateJobTrimSettings(update) },
                    onUpdateRoomState = { transform -> viewModel.updateEditingRoomState(transform) },
                    onUpdateVerandaState = { transform -> viewModel.updateEditingVerandaState(transform) },
                    onUpdateBathroomState = { transform -> viewModel.updateEditingBathroomState(transform) }
                )
            }

            // Bottom "Done" button
            item {
                Button(
                    onClick = onDone,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("area_bottom_done_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Done Editing Area",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
