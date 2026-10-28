package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import kotlin.math.max

@Composable
fun TrimMaterialSelector(
    selected: TrimMaterial,
    onSelect: (TrimMaterial) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "trim_mat"
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TrimMaterial.values().forEach { mat ->
            val isSelected = selected == mat
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(mat) },
                label = {
                    Text(
                        text = mat.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                modifier = Modifier
                    .height(44.dp)
                    .testTag("${testTagPrefix}_${mat.name.lowercase()}")
            )
        }
    }
}

@Composable
fun AreaTrimsCard(
    area: AreaItem,
    unit: UnitSystem,
    trimSettings: TrimSettingsState,
    onUpdateTrimSettings: ((TrimSettingsState) -> TrimSettingsState) -> Unit,
    onUpdateRoomState: ((RoomFloorState) -> RoomFloorState) -> Unit,
    onUpdateVerandaState: ((VerandaState) -> VerandaState) -> Unit,
    onUpdateBathroomState: ((BathroomState) -> BathroomState) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("trims_card"),
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
                    imageVector = Icons.Default.Straighten,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "TRIMS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Finishing edge profiles for ${area.name.ifBlank { area.type.displayName }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. TRIM SETTINGS (Editable)
            Text(
                text = "TRIM SPECIFICATIONS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = trimSettings.pieceLengthM,
                    onValueChange = { len -> onUpdateTrimSettings { it.copy(pieceLengthM = len) } },
                    label = { Text("Piece length (m)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("trim_piece_length_input"),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
                )

                OutlinedTextField(
                    value = trimSettings.allowancePercent.toString(),
                    onValueChange = { pct ->
                        val intVal = pct.filter { it.isDigit() }.toIntOrNull() ?: 0
                        onUpdateTrimSettings { it.copy(allowancePercent = intVal) }
                    },
                    label = { Text("Extra % (wastage)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("trim_extra_percent_input"),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(14.dp))

            // 2. AREA-SPECIFIC RUNS
            when (area.type) {
                SpaceType.ROOM_FLOOR -> {
                    RoomFloorTrimsContent(
                        roomState = area.roomState,
                        unit = unit,
                        onUpdateRoomState = onUpdateRoomState
                    )
                }
                SpaceType.VERANDA -> {
                    VerandaTrimsContent(
                        verandaState = area.verandaState,
                        unit = unit,
                        onUpdateVerandaState = onUpdateVerandaState
                    )
                }
                SpaceType.BATHROOM -> {
                    BathroomTrimsContent(
                        bathroomState = area.bathroomState,
                        unit = unit,
                        onUpdateBathroomState = onUpdateBathroomState
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomFloorTrimsContent(
    roomState: RoomFloorState,
    unit: UnitSystem,
    onUpdateRoomState: ((RoomFloorState) -> RoomFloorState) -> Unit
) {
    val lenM = unit.toMeters(roomState.length.toDoubleOrNull() ?: 0.0)
    val widM = unit.toMeters(roomState.width.toDoubleOrNull() ?: 0.0)
    val perimeterM = 2.0 * (lenM + widM)
    val totalDoorsM = roomState.doorwayWidths.sumOf { unit.toMeters(it.toDoubleOrNull() ?: 0.0) }
    val skirtingRunM = if (roomState.hasSkirting) max(0.0, perimeterM - totalDoorsM) else 0.0

    // Along the skirting
    TrimRunRow(
        title = "Along the skirting",
        lengthSubtitle = if (!roomState.hasSkirting) {
            "0.0 m (Skirting is OFF)"
        } else {
            "${formatTrimLen(skirtingRunM)} m (perimeter ${formatTrimLen(perimeterM)} m minus doors ${formatTrimLen(totalDoorsM)} m)"
        },
        isEnabled = roomState.hasAlongSkirtingTrim && roomState.hasSkirting,
        onToggle = { enabled -> onUpdateRoomState { it.copy(hasAlongSkirtingTrim = enabled) } },
        material = roomState.alongSkirtingTrimMaterial,
        onMaterialChange = { mat -> onUpdateRoomState { it.copy(alongSkirtingTrimMaterial = mat) } },
        toggleTag = "along_skirting_trim_toggle",
        materialTagPrefix = "along_skirting_mat"
    )

    // Optional run per door
    if (roomState.doorwayWidths.isNotEmpty()) {
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "DOOR OPENINGS (ACROSS THRESHOLD)",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Optional trim across each door opening (off by default)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        roomState.doorwayWidths.forEachIndexed { index, doorStr ->
            val doorWM = unit.toMeters(doorStr.toDoubleOrNull() ?: 0.0)
            TrimRunRow(
                title = "Across door ${index + 1} opening",
                lengthSubtitle = "${formatTrimLen(doorWM)} m",
                isEnabled = roomState.isDoorTrimEnabled(index),
                onToggle = { enabled -> onUpdateRoomState { it.withDoorTrimToggled(index, enabled) } },
                material = roomState.getDoorTrimMaterial(index),
                onMaterialChange = { mat -> onUpdateRoomState { it.withDoorTrimMaterial(index, mat) } },
                toggleTag = "door_trim_${index}_toggle",
                materialTagPrefix = "door_trim_${index}_mat"
            )
            if (index < roomState.doorwayWidths.size - 1) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun VerandaTrimsContent(
    verandaState: VerandaState,
    unit: UnitSystem,
    onUpdateVerandaState: ((VerandaState) -> VerandaState) -> Unit
) {
    val vLenM = unit.toMeters(verandaState.length.toDoubleOrNull() ?: 0.0)
    val vWidM = unit.toMeters(verandaState.width.toDoubleOrNull() ?: 0.0)
    val doorWidthM = unit.toMeters(verandaState.doorWidth.toDoubleOrNull() ?: 0.0)
    val hasDoor = verandaState.hasDoor
    val doorSide = verandaState.doorSide

    // Compute pillar deductions per side
    val sidePillarDeductions = mutableMapOf<VerandaSide, Double>()
    verandaState.pillars.forEach { pillar ->
        if (pillar.isEnabled && pillar.removeFromEdge) {
            val pWidthM = (pillar.widthCm.toDoubleOrNull() ?: 30.0) / 100.0
            sidePillarDeductions[pillar.edgeSide] = (sidePillarDeductions[pillar.edgeSide] ?: 0.0) + pWidthM
        }
    }

    Text(
        text = "VERANDA SIDES (EDGES & WALLS)",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(6.dp))

    val sides = listOf(
        Triple(VerandaSide.FRONT, vLenM, verandaState.frontType),
        Triple(VerandaSide.BACK, vLenM, verandaState.backType),
        Triple(VerandaSide.LEFT, vWidM, verandaState.leftType),
        Triple(VerandaSide.RIGHT, vWidM, verandaState.rightType)
    )

    sides.forEachIndexed { index, triple ->
        val (side, sideLenM, sideType) = triple
        val pillarDedM = sidePillarDeductions[side] ?: 0.0
        val isStep = sideType == VerandaSideType.OPENING_STEP

        val lengthSubtitle = if (isStep) {
            if (pillarDedM > 0.0) {
                "${formatTrimLen(sideLenM)} m minus pillar ${formatTrimLen(pillarDedM)} m = ${formatTrimLen(max(0.0, sideLenM - pillarDedM))} m"
            } else {
                "${formatTrimLen(sideLenM)} m (Step edge)"
            }
        } else {
            val hasDoorOnSide = hasDoor && (doorSide == side)
            val doorDed = if (hasDoorOnSide) doorWidthM.coerceAtMost(sideLenM) else 0.0
            val netM = max(0.0, sideLenM - doorDed - pillarDedM)
            if (doorDed > 0.0 && pillarDedM > 0.0) {
                "${formatTrimLen(sideLenM)} m minus door ${formatTrimLen(doorDed)} m minus pillar ${formatTrimLen(pillarDedM)} m = ${formatTrimLen(netM)} m"
            } else if (doorDed > 0.0) {
                "${formatTrimLen(sideLenM)} m minus door ${formatTrimLen(doorDed)} m = ${formatTrimLen(netM)} m"
            } else if (pillarDedM > 0.0) {
                "${formatTrimLen(sideLenM)} m minus pillar ${formatTrimLen(pillarDedM)} m = ${formatTrimLen(netM)} m"
            } else {
                "${formatTrimLen(sideLenM)} m (Wall side)"
            }
        }

        TrimRunRow(
            title = "${side.label} side (${if (isStep) "Open step edge" else "Against wall"})",
            lengthSubtitle = lengthSubtitle,
            isEnabled = verandaState.isSideTrimEnabled(side),
            onToggle = { enabled -> onUpdateVerandaState { it.withSideTrimToggled(side, enabled) } },
            material = verandaState.getSideTrimMaterial(side),
            onMaterialChange = { mat -> onUpdateVerandaState { it.withSideTrimMaterial(side, mat) } },
            toggleTag = "veranda_trim_${side.name.lowercase()}_toggle",
            materialTagPrefix = "veranda_trim_${side.name.lowercase()}_mat"
        )
        if (index < sides.size - 1) {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(12.dp))

    // 5. PILLARS (Veranda)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Are there pillars on the edge?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Pillar run = 2 × (width + depth)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Counter (- and +)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalIconButton(
                onClick = {
                    val newCount = (verandaState.pillars.size - 1).coerceAtLeast(0)
                    onUpdateVerandaState { it.withPillarCount(newCount) }
                },
                enabled = verandaState.pillars.isNotEmpty(),
                modifier = Modifier
                    .size(40.dp)
                    .testTag("pillar_decrement_button")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease pillars")
            }

            Text(
                text = "${verandaState.pillars.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("veranda_pillar_count")
            )

            FilledTonalIconButton(
                onClick = {
                    val newCount = verandaState.pillars.size + 1
                    onUpdateVerandaState { it.withPillarCount(newCount) }
                },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("pillar_increment_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase pillars")
            }
        }
    }

    if (verandaState.pillars.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))

        verandaState.pillars.forEachIndexed { pIdx, pillar ->
            val wCm = pillar.widthCm.toDoubleOrNull() ?: 30.0
            val dCm = pillar.depthCm.toDoubleOrNull() ?: 30.0
            val runM = 2.0 * (wCm + dCm) / 100.0

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pillar ${pIdx + 1}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Run: ${formatTrimLen(runM)} m",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = pillar.isEnabled,
                                onCheckedChange = { en ->
                                    val updated = verandaState.pillars.toMutableList()
                                    updated[pIdx] = pillar.copy(isEnabled = en)
                                    onUpdateVerandaState { it.copy(pillars = updated) }
                                },
                                modifier = Modifier.testTag("pillar_${pIdx}_toggle")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dimensions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = pillar.widthCm,
                            onValueChange = { w ->
                                val updated = verandaState.pillars.toMutableList()
                                updated[pIdx] = pillar.copy(widthCm = w)
                                onUpdateVerandaState { it.copy(pillars = updated) }
                            },
                            label = { Text("Width (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pillar_${pIdx}_width"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = pillar.depthCm,
                            onValueChange = { d ->
                                val updated = verandaState.pillars.toMutableList()
                                updated[pIdx] = pillar.copy(depthCm = d)
                                onUpdateVerandaState { it.copy(pillars = updated) }
                            },
                            label = { Text("Depth (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pillar_${pIdx}_depth"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Material choice
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pillar Material:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        TrimMaterialSelector(
                            selected = pillar.material,
                            onSelect = { mat ->
                                val updated = verandaState.pillars.toMutableList()
                                updated[pIdx] = pillar.copy(material = mat)
                                onUpdateVerandaState { it.copy(pillars = updated) }
                            },
                            testTagPrefix = "pillar_${pIdx}_mat"
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Remove pillar width from edge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Remove pillar width from the edge",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = pillar.removeFromEdge,
                            onCheckedChange = { rm ->
                                val updated = verandaState.pillars.toMutableList()
                                updated[pIdx] = pillar.copy(removeFromEdge = rm)
                                onUpdateVerandaState { it.copy(pillars = updated) }
                            },
                            modifier = Modifier.testTag("pillar_${pIdx}_remove_edge_toggle")
                        )
                    }

                    if (pillar.removeFromEdge) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Stands on side:",
                                style = MaterialTheme.typography.labelSmall
                            )
                            VerandaSide.values().forEach { side ->
                                val isSelected = pillar.edgeSide == side
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val updated = verandaState.pillars.toMutableList()
                                        updated[pIdx] = pillar.copy(edgeSide = side)
                                        onUpdateVerandaState { it.copy(pillars = updated) }
                                    },
                                    label = { Text(side.label, fontSize = 11.sp) },
                                    modifier = Modifier.height(38.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BathroomTrimsContent(
    bathroomState: BathroomState,
    unit: UnitSystem,
    onUpdateBathroomState: ((BathroomState) -> BathroomState) -> Unit
) {
    val lenM = unit.toMeters(bathroomState.length.toDoubleOrNull() ?: 0.0)
    val widM = unit.toMeters(bathroomState.width.toDoubleOrNull() ?: 0.0)
    val perimeterM = 2.0 * (lenM + widM)
    val totalDoorsM = bathroomState.doorwayWidths.sumOf { unit.toMeters(it.toDoubleOrNull() ?: 0.0) }
    val skirtingRunM = if (bathroomState.hasSkirting) max(0.0, perimeterM - totalDoorsM) else 0.0

    Text(
        text = "BATHROOM FLOOR TRIMS",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(6.dp))

    // Along the skirting
    TrimRunRow(
        title = "Along the skirting",
        lengthSubtitle = if (!bathroomState.hasSkirting) {
            "0.0 m (Floor skirting is OFF)"
        } else {
            "${formatTrimLen(skirtingRunM)} m (perimeter ${formatTrimLen(perimeterM)} m minus doors ${formatTrimLen(totalDoorsM)} m)"
        },
        isEnabled = bathroomState.hasAlongSkirtingTrim && bathroomState.hasSkirting,
        onToggle = { enabled -> onUpdateBathroomState { it.copy(hasAlongSkirtingTrim = enabled) } },
        material = bathroomState.alongSkirtingTrimMaterial,
        onMaterialChange = { mat -> onUpdateBathroomState { it.copy(alongSkirtingTrimMaterial = mat) } },
        toggleTag = "bath_along_skirting_trim_toggle",
        materialTagPrefix = "bath_along_skirting_mat"
    )

    // Optional run per door
    if (bathroomState.doorwayWidths.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        bathroomState.doorwayWidths.forEachIndexed { index, doorStr ->
            val doorWM = unit.toMeters(doorStr.toDoubleOrNull() ?: 0.0)
            TrimRunRow(
                title = "Across door ${index + 1} opening",
                lengthSubtitle = "${formatTrimLen(doorWM)} m",
                isEnabled = bathroomState.isDoorTrimEnabled(index),
                onToggle = { enabled -> onUpdateBathroomState { it.withDoorTrimToggled(index, enabled) } },
                material = bathroomState.getDoorTrimMaterial(index),
                onMaterialChange = { mat -> onUpdateBathroomState { it.withDoorTrimMaterial(index, mat) } },
                toggleTag = "bath_door_trim_${index}_toggle",
                materialTagPrefix = "bath_door_trim_${index}_mat"
            )
            if (index < bathroomState.doorwayWidths.size - 1) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }

    // Walls top trim
    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "BATHROOM WALL TRIMS",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(6.dp))

    val totalWallWidthM = bathroomState.walls.sumOf { unit.toMeters(it.width.toDoubleOrNull() ?: 0.0) }
    TrimRunRow(
        title = "Trim on top of the wall tiles",
        lengthSubtitle = if (!bathroomState.tileWallsToo) {
            "0.0 m (Wall tiling is OFF)"
        } else {
            "${formatTrimLen(totalWallWidthM)} m across all tiled walls"
        },
        isEnabled = bathroomState.hasWallTopTrim && bathroomState.tileWallsToo,
        onToggle = { enabled -> onUpdateBathroomState { it.copy(hasWallTopTrim = enabled) } },
        material = bathroomState.wallTopTrimMaterial,
        onMaterialChange = { mat -> onUpdateBathroomState { it.copy(wallTopTrimMaterial = mat) } },
        toggleTag = "bath_wall_top_trim_toggle",
        materialTagPrefix = "bath_wall_top_mat"
    )
}

@Composable
private fun TrimRunRow(
    title: String,
    lengthSubtitle: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    material: TrimMaterial,
    onMaterialChange: (TrimMaterial) -> Unit,
    toggleTag: String,
    materialTagPrefix: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = lengthSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag(toggleTag)
                )
            }

            if (isEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Material:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TrimMaterialSelector(
                        selected = material,
                        onSelect = onMaterialChange,
                        testTagPrefix = materialTagPrefix
                    )
                }
            }
        }
    }
}

private fun formatTrimLen(m: Double): String {
    return TileCalculatorEngine.formatTrimLen(m)
}
