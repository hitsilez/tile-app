package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(
    job: Job,
    calculationResult: CalculationResult,
    userProfile: UserProfile,
    savedMessage: String?,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onUpdateJob: ((Job) -> Job) -> Unit,
    onSetUnit: (UnitSystem) -> Unit,
    onUpdateTileSettings: ((FloorTileSettingsState) -> FloorTileSettingsState) -> Unit,
    onUpdateMaterialSettings: ((MaterialSettingsState) -> MaterialSettingsState) -> Unit,
    onUpdateTrimSettings: ((TrimSettingsState) -> TrimSettingsState) -> Unit,
    onAddArea: (SpaceType) -> Unit,
    onEditArea: (String) -> Unit,
    onDeleteArea: (String) -> Unit,
    onAddOuterCorner: () -> Unit,
    onRemoveOuterCorner: (Int) -> Unit,
    onUpdateOuterCorner: (Int, String, String, TrimMaterial, Boolean) -> Unit,
    onAddCustomRun: () -> Unit,
    onRemoveCustomRun: (Int) -> Unit,
    onUpdateCustomRun: (Int, String, String, TrimMaterial, Boolean) -> Unit,
    onOpenQuotation: () -> Unit,
    onOpenHelp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var showDeleteConfirmAreaId by remember { mutableStateOf<String?>(null) }
    var isJobInfoExpanded by remember { mutableStateOf(false) }
    var isMaterialsSettingsExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("job_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Jobs List"
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = job.name.ifBlank { "Untitled Job" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1
                        )
                        if (job.clientName.isNotBlank()) {
                            Text(
                                text = "Client: ${job.clientName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenHelp,
                        modifier = Modifier.testTag("job_detail_help_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "How it works",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    FilledTonalButton(
                        onClick = onOpenQuotation,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("job_quotation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Quotation",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Quotation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = onSave,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("job_save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Save",
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
        },
        bottomBar = {
            // Transparent floating bar with side margins 16 px clearing navigation bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("job_sticky_bottom_bar")
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            // Smoothly scroll to the "What to buy" section in the list
                            listState.animateScrollToItem(index = maxOf(0, job.areas.size + 4))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("sticky_what_to_buy_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "What to buy",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp + innerPadding.calculateBottomPadding())
        ) {

            // Saved Confirmation Banner
            if (savedMessage != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("saved_confirmation_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = savedMessage,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // 1. JOB INFORMATION CARD
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("job_info_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "JOB INFORMATION",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            TextButton(onClick = { isJobInfoExpanded = !isJobInfoExpanded }) {
                                Text(if (isJobInfoExpanded) "Collapse" else "Edit Details")
                                Icon(
                                    imageVector = if (isJobInfoExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Always visible: Unit switcher
                        Spacer(modifier = Modifier.height(8.dp))
                        UnitSelector(
                            selectedUnit = job.unitSetting,
                            onUnitSelected = onSetUnit
                        )

                        AnimatedVisibility(visible = isJobInfoExpanded) {
                            Column(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = job.name,
                                    onValueChange = { newName -> onUpdateJob { it.copy(name = newName) } },
                                    label = { Text("Job Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("job_name_input")
                                )

                                OutlinedTextField(
                                    value = job.clientName,
                                    onValueChange = { newClient -> onUpdateJob { it.copy(clientName = newClient) } },
                                    label = { Text("Client Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("job_client_input")
                                )

                                OutlinedTextField(
                                    value = job.addressOrPlot,
                                    onValueChange = { newAddr -> onUpdateJob { it.copy(addressOrPlot = newAddr) } },
                                    label = { Text("Address / Plot Number") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("job_address_input")
                                )

                                OutlinedTextField(
                                    value = job.notes,
                                    onValueChange = { newNotes -> onUpdateJob { it.copy(notes = newNotes) } },
                                    label = { Text("Job Notes / Instructions") },
                                    minLines = 2,
                                    maxLines = 4,
                                    modifier = Modifier.fillMaxWidth().testTag("job_notes_input")
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================
            // 1. "Rooms and areas": add and edit areas
            // =========================================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "1. Rooms and areas",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Add and edit areas (${job.areas.size} added)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (job.areas.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No areas added yet. Tap a button below to add a Room, Veranda or Bathroom.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(job.areas, key = { it.id }) { area ->
                    val breakdown = calculationResult.areaBreakdowns.find { it.areaId == area.id }
                    AreaCard(
                        area = area,
                        unit = job.unitSetting,
                        tileCount = breakdown?.totalAreaTiles ?: 0,
                        onEdit = { onEditArea(area.id) },
                        onDelete = { showDeleteConfirmAreaId = area.id }
                    )
                }
            }

            // Buttons: + Room, + Veranda, + Bathroom (Buttons at least 48dp tall)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_area_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Add an area",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onAddArea(SpaceType.ROOM_FLOOR) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("add_room_area_button"),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Text(
                                    text = "+ Room",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { onAddArea(SpaceType.VERANDA) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("add_veranda_area_button"),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Text(
                                    text = "+ Veranda",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { onAddArea(SpaceType.BATHROOM) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("add_bathroom_area_button"),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Text(
                                    text = "+ Bathroom",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================
            // 2. "Choose tiles": tile size, tiles per box, waste %
            // =========================================================
            item {
                Column {
                    Text(
                        text = "2. Choose tiles",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Tile size, tiles per box, extra for waste (5, 10, 15 or 20%)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                TileSettingsCard(
                    settings = job.tileSettings,
                    unit = job.unitSetting,
                    onUpdate = onUpdateTileSettings
                )
            }

            // Collapsed Materials settings (2. SIMPLE MATERIALS SETTINGS: "Adjust materials (optional)", closed by default)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("materials_settings_collapsible_card")
                        .clickable { isMaterialsSettingsExpanded = !isMaterialsSettingsExpanded },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "Adjust materials (optional)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Defaults work for most jobs",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (isMaterialsSettingsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isMaterialsSettingsExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(visible = isMaterialsSettingsExpanded) {
                            Column(
                                modifier = Modifier.padding(top = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                val hasSmallWallTiles = remember(job.areas, job.unitSetting) {
                                    job.areas.any { area ->
                                        if (area.type == SpaceType.BATHROOM && (area.bathroomState.tileWallsToo || area.bathroomState.hasBathtub)) {
                                            val (wM, hM) = TileCalculatorEngine.resolveTileDimensionsMeters(
                                                area.bathroomState.wallTilePreset,
                                                area.bathroomState.customWallTileLength,
                                                area.bathroomState.customWallTileWidth,
                                                job.unitSetting
                                            )
                                            val wCm = wM * 100.0
                                            val hCm = hM * 100.0
                                            (wCm > 0.0 && wCm < 39.99) || (hCm > 0.0 && hCm < 39.99)
                                        } else false
                                    }
                                }

                                MaterialSettingsCard(
                                    settings = job.materialSettings,
                                    trimSettings = job.trimSettings,
                                    onUpdateMaterialSettings = onUpdateMaterialSettings,
                                    onUpdateTrimSettings = onUpdateTrimSettings,
                                    onResetToStandard = {
                                        onUpdateMaterialSettings { MaterialSettingsState() }
                                        onUpdateTrimSettings { TrimSettingsState() }
                                    },
                                    onAddOuterCorner = onAddOuterCorner,
                                    onRemoveOuterCorner = onRemoveOuterCorner,
                                    onUpdateOuterCorner = onUpdateOuterCorner,
                                    hasSmallWallTiles = hasSmallWallTiles
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================
            // 3. "What to buy": the result
            // =========================================================
            item {
                Column {
                    Text(
                        text = "3. What to buy",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Your results and shopping list",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 7. WHOLE JOB RESULTS VIEW
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_quotation_banner_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "Ready to share?",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Generate client quotation or shop quick list",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Button(
                            onClick = onOpenQuotation,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("generate_quotation_action_button")
                        ) {
                            Text("Quotation", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                ResultsView(
                    result = calculationResult,
                    jobName = job.name,
                    clientName = job.clientName,
                    builderName = userProfile.builderName,
                    builderPhone = userProfile.builderPhone
                )
            }
        }
    }

    // Delete Area Dialog
    showDeleteConfirmAreaId?.let { areaId ->
        val area = job.areas.find { it.id == areaId }
        AlertDialog(
            onDismissRequest = { showDeleteConfirmAreaId = null },
            title = { Text("Delete Area") },
            text = { Text("Are you sure you want to remove \"${area?.name ?: "this area"}\" from the job?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteArea(areaId)
                        showDeleteConfirmAreaId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmAreaId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AreaCard(
    area: AreaItem,
    unit: UnitSystem,
    tileCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("area_card_${area.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (area.type) {
                                SpaceType.ROOM_FLOOR -> Icons.Default.SquareFoot
                                SpaceType.VERANDA -> Icons.Default.Balcony
                                SpaceType.BATHROOM -> Icons.Default.Bathtub
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    // One-line summary: for example "Main Veranda (Veranda): 2.05 m x 1.85 m"
                    Text(
                        text = "${area.name} (${area.type.displayName}): ${TileCalculatorEngine.formatAreaSize(area, unit)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$tileCount tiles",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("edit_area_${area.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Area",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("delete_area_${area.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Area",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
