package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*

@Composable
fun BathroomScreen(
    bathroomState: BathroomState,
    unit: UnitSystem,
    onUpdate: ((BathroomState) -> BathroomState) -> Unit,
    onAddDoorway: () -> Unit,
    onRemoveDoorway: (Int) -> Unit,
    onUpdateDoorway: (Int, String) -> Unit,
    onAddWall: () -> Unit,
    onRemoveWall: (Int) -> Unit,
    onUpdateWall: (Int, String, String) -> Unit,
    onAddWindow: () -> Unit,
    onRemoveWindow: (Int) -> Unit,
    onUpdateWindow: (Int, String, String) -> Unit,
    onAddDoor: () -> Unit,
    onRemoveDoor: (Int) -> Unit,
    onUpdateDoor: (Int, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bathroom_inputs_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bathtub,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "BATHROOM SPECIFICATIONS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. FLOOR SECTION
            Text(
                text = "1. BATHROOM FLOOR",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = bathroomState.length,
                    onValueChange = { len -> onUpdate { it.copy(length = len) } },
                    label = { Text("Length (${unit.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bathroom_floor_length_input"),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                )

                OutlinedTextField(
                    value = bathroomState.width,
                    onValueChange = { wid -> onUpdate { it.copy(width = wid) } },
                    label = { Text("Width (${unit.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bathroom_floor_width_input"),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Skirting Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Floor Skirting",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    com.example.ui.components.HelpTooltipButton(
                        title = "Floor Skirting",
                        explanation = "The tile strip along the bottom of the wall. Usually skipped if walls are tiled.",
                        testTag = "bathroom_skirting_help"
                    )
                }
                Switch(
                    checked = bathroomState.hasSkirting,
                    onCheckedChange = { checked -> onUpdate { it.copy(hasSkirting = checked) } },
                    modifier = Modifier.testTag("bathroom_skirting_toggle")
                )
            }
            Text(
                text = "Cut from your floor tiles along the bottom of the walls.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (bathroomState.hasSkirting) {
                Spacer(modifier = Modifier.height(10.dp))
                val heightUnit = if (unit == UnitSystem.FEET) "inches" else "cm"
                Text(
                    text = "Height of the skirting ($heightUnit)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SkirtingPreset.values().forEach { preset ->
                        val isSelected = bathroomState.skirtingPreset == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = { onUpdate { it.copy(skirtingPreset = preset) } },
                            label = { Text(preset.label) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("bathroom_skirting_preset_${preset.name.lowercase()}")
                        )
                    }
                }

                if (bathroomState.skirtingPreset == SkirtingPreset.CUSTOM) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bathroomState.customSkirtingHeight,
                        onValueChange = { h -> onUpdate { it.copy(customSkirtingHeight = h) } },
                        label = { Text("Custom skirting height ($heightUnit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bathroom_custom_skirting_height_input"),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Doors Section (A5, B3)
            com.example.ui.components.DoorsSection(
                doorWidths = bathroomState.doorwayWidths,
                unit = unit,
                onUpdateDoors = { newDoors ->
                    onUpdate { it.copy(doorwayWidths = newDoors) }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // 2. WALLS SECTION
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tile the walls too?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Separate wall tile size, box calculation, minus windows & doors, plus bathtub",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = bathroomState.tileWallsToo,
                    onCheckedChange = { checked -> onUpdate { it.copy(tileWallsToo = checked) } },
                    modifier = Modifier.testTag("tile_walls_too_toggle")
                )
            }

            AnimatedVisibility(visible = bathroomState.tileWallsToo) {
                Column(modifier = Modifier.padding(top = 16.dp)) {

                    // Wall Tile Size Picker
                    Text(
                        text = "WALL TILE SIZE (cm)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                TilePreset.SIZE_30X30,
                                TilePreset.SIZE_40X40,
                                TilePreset.SIZE_60X60
                            ).forEach { preset ->
                                FilterChip(
                                    selected = bathroomState.wallTilePreset == preset,
                                    onClick = { onUpdate { it.copy(wallTilePreset = preset) } },
                                    label = { Text(preset.label, fontSize = 11.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("wall_preset_${preset.name.lowercase()}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                TilePreset.SIZE_60X120,
                                TilePreset.SIZE_80X80,
                                TilePreset.CUSTOM
                            ).forEach { preset ->
                                FilterChip(
                                    selected = bathroomState.wallTilePreset == preset,
                                    onClick = { onUpdate { it.copy(wallTilePreset = preset) } },
                                    label = { Text(preset.label, fontSize = 11.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("wall_preset_${preset.name.lowercase()}")
                                )
                            }
                        }
                    }

                    if (bathroomState.wallTilePreset == TilePreset.CUSTOM) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = bathroomState.customWallTileLength,
                                onValueChange = { len -> onUpdate { it.copy(customWallTileLength = len) } },
                                label = { Text("Wall Tile Length (${unit.symbol})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_wall_tile_length_input"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = bathroomState.customWallTileWidth,
                                onValueChange = { wid -> onUpdate { it.copy(customWallTileWidth = wid) } },
                                label = { Text("Wall Tile Width (${unit.symbol})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_wall_tile_width_input"),
                                singleLine = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = bathroomState.wallTilesPerBox,
                        onValueChange = { v -> onUpdate { it.copy(wallTilesPerBox = v.filter { c -> c.isDigit() }) } },
                        label = { Text("Wall Tiles Per Box") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wall_tiles_per_box_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Wall dimensions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WALLS (${bathroomState.walls.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = onAddWall,
                            modifier = Modifier.testTag("add_wall_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Wall")
                        }
                    }

                    bathroomState.walls.forEachIndexed { index, wall ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = wall.width,
                                onValueChange = { w -> onUpdateWall(index, w, wall.height) },
                                label = { Text("${wall.label} W (${unit.symbol})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("wall_${index}_width"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = wall.height,
                                onValueChange = { h -> onUpdateWall(index, wall.width, h) },
                                label = { Text("H (${unit.symbol})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("wall_${index}_height"),
                                singleLine = true
                            )
                            if (bathroomState.walls.size > 1) {
                                IconButton(
                                    onClick = { onRemoveWall(index) },
                                    modifier = Modifier.testTag("remove_wall_$index")
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Windows (Subtracted)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WINDOWS (SUBTRACTED)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = onAddWindow,
                            modifier = Modifier.testTag("add_window_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Window")
                        }
                    }

                    bathroomState.windows.forEachIndexed { index, win ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = win.width,
                                onValueChange = { w -> onUpdateWindow(index, w, win.height) },
                                label = { Text("Window #${index + 1} W (${unit.symbol})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("window_${index}_width"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = win.height,
                                onValueChange = { h -> onUpdateWindow(index, win.width, h) },
                                label = { Text("H (${unit.symbol})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("window_${index}_height"),
                                singleLine = true
                            )
                            IconButton(
                                onClick = { onRemoveWindow(index) },
                                modifier = Modifier.testTag("remove_window_$index")
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Doors (Subtracted)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DOORS (SUBTRACTED)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = onAddDoor,
                            modifier = Modifier.testTag("add_wall_door_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Door")
                        }
                    }

                    bathroomState.doors.forEachIndexed { index, door ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = door.width,
                                onValueChange = { w -> onUpdateDoor(index, w, door.height) },
                                label = { Text("Door #${index + 1} W (${unit.symbol})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("wall_door_${index}_width"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = door.height,
                                onValueChange = { h -> onUpdateDoor(index, door.width, h) },
                                label = { Text("H (${unit.symbol})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("wall_door_${index}_height"),
                                singleLine = true
                            )
                            IconButton(
                                onClick = { onRemoveDoor(index) },
                                modifier = Modifier.testTag("remove_wall_door_$index")
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bathtub Surround
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bathtub Surround",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tiled bathtub apron/surround (added to wall area)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = bathroomState.hasBathtub,
                            onCheckedChange = { checked -> onUpdate { it.copy(hasBathtub = checked) } },
                            modifier = Modifier.testTag("bathtub_toggle")
                        )
                    }

                    if (bathroomState.hasBathtub) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = bathroomState.bathtubWidth,
                                onValueChange = { w -> onUpdate { it.copy(bathtubWidth = w) } },
                                label = { Text("Bathtub Width (${unit.symbol})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("bathtub_width_input"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = bathroomState.bathtubHeight,
                                onValueChange = { h -> onUpdate { it.copy(bathtubHeight = h) } },
                                label = { Text("Bathtub Height (${unit.symbol})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("bathtub_height_input"),
                                singleLine = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Trim on top of the wall tiles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Trim on top of the wall tiles",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Finishing trim along top edge of wall tiles",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = bathroomState.hasWallTopTrim,
                            onCheckedChange = { checked -> onUpdate { it.copy(hasWallTopTrim = checked) } },
                            modifier = Modifier.testTag("bathroom_wall_top_trim_toggle")
                        )
                    }

                    if (bathroomState.hasWallTopTrim) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Trim Material:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            com.example.ui.components.TrimMaterialSelector(
                                selected = bathroomState.wallTopTrimMaterial,
                                onSelect = { mat -> onUpdate { it.copy(wallTopTrimMaterial = mat) } },
                                testTagPrefix = "bathroom_wall_top_mat"
                            )
                        }
                    }
                }
            }
        }
    }
}
