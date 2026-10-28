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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.HelpTooltipButton

@Composable
fun VerandaScreen(
    verandaState: VerandaState,
    unit: UnitSystem,
    onUpdate: ((VerandaState) -> VerandaState) -> Unit,
    modifier: Modifier = Modifier
) {
    val heightUnit = if (unit == UnitSystem.FEET) "inches" else "cm"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("veranda_inputs_card"),
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
                    imageVector = Icons.Default.Balcony,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "VERANDA DIMENSIONS & SIDES",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Length and Width
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = verandaState.length,
                    onValueChange = { len -> onUpdate { it.copy(length = len) } },
                    label = { Text("Length (${unit.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("veranda_length_input"),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                )

                OutlinedTextField(
                    value = verandaState.width,
                    onValueChange = { wid -> onUpdate { it.copy(width = wid) } },
                    label = { Text("Width (${unit.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("veranda_width_input"),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Interactive 4-Sided Veranda Diagram
            Text(
                text = "Interactive Diagram (tap a side to toggle)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))

            VerandaInteractiveDiagram(
                verandaState = verandaState,
                unit = unit,
                onToggleSide = { side ->
                    onUpdate { current ->
                        when (side) {
                            VerandaSide.FRONT -> current.copy(
                                frontType = if (current.frontType == VerandaSideType.OPENING_STEP) VerandaSideType.WALL else VerandaSideType.OPENING_STEP
                            )
                            VerandaSide.BACK -> current.copy(
                                backType = if (current.backType == VerandaSideType.OPENING_STEP) VerandaSideType.WALL else VerandaSideType.OPENING_STEP
                            )
                            VerandaSide.LEFT -> current.copy(
                                leftType = if (current.leftType == VerandaSideType.OPENING_STEP) VerandaSideType.WALL else VerandaSideType.OPENING_STEP
                            )
                            VerandaSide.RIGHT -> current.copy(
                                rightType = if (current.rightType == VerandaSideType.OPENING_STEP) VerandaSideType.WALL else VerandaSideType.OPENING_STEP
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4 Sides Marking Buttons (Plain Language)
            Text(
                text = "Side Configuration",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Front
            VerandaSideRow(
                sideName = "Front Side",
                dimLabel = "${verandaState.length.ifBlank { "0" }} ${unit.symbol}",
                currentType = verandaState.frontType,
                onTypeChange = { onUpdate { s -> s.copy(frontType = it) } },
                testTagPrefix = "front"
            )

            // Back
            VerandaSideRow(
                sideName = "Back Side",
                dimLabel = "${verandaState.length.ifBlank { "0" }} ${unit.symbol}",
                currentType = verandaState.backType,
                onTypeChange = { onUpdate { s -> s.copy(backType = it) } },
                testTagPrefix = "back"
            )

            // Left
            VerandaSideRow(
                sideName = "Left Side",
                dimLabel = "${verandaState.width.ifBlank { "0" }} ${unit.symbol}",
                currentType = verandaState.leftType,
                onTypeChange = { onUpdate { s -> s.copy(leftType = it) } },
                testTagPrefix = "left"
            )

            // Right
            VerandaSideRow(
                sideName = "Right Side",
                dimLabel = "${verandaState.width.ifBlank { "0" }} ${unit.symbol}",
                currentType = verandaState.rightType,
                onTypeChange = { onUpdate { s -> s.copy(rightType = it) } },
                testTagPrefix = "right"
            )

            val hasAnyOpening = verandaState.frontType == VerandaSideType.OPENING_STEP ||
                    verandaState.backType == VerandaSideType.OPENING_STEP ||
                    verandaState.leftType == VerandaSideType.OPENING_STEP ||
                    verandaState.rightType == VerandaSideType.OPENING_STEP

            if (hasAnyOpening) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Open edge with a step",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    HelpTooltipButton(
                        title = "Open edge with a step",
                        explanation = "Vertical step edge dropping down. Tiled with cut strips from your floor tiles.",
                        testTag = "veranda_step_help"
                    )
                }
                Text(
                    text = "Cut from your floor tiles to cover the step riser.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = verandaState.stepEdgeHeight,
                    onValueChange = { h -> onUpdate { it.copy(stepEdgeHeight = h) } },
                    label = { Text("Height of the step ($heightUnit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("step_edge_height_input"),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                )

                // Inline yellow warning if step height is invalid (A3, B4)
                val stepH = verandaState.stepEdgeHeight.toDoubleOrNull() ?: 0.0
                val stepM = if (stepH > 3.0) stepH / 100.0 else stepH
                if (verandaState.stepEdgeHeight.isNotBlank() && (stepM <= 0.0 || stepM > 3.0)) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = Color(0xFFFFF3CD),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().testTag("veranda_step_inline_warning")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFF856404), modifier = Modifier.size(16.dp))
                            Text(
                                text = if (stepM <= 0.0) "Veranda, step face: height must be greater than 0." else "Veranda, step face: height is more than 3 m.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF856404)
                            )
                        }
                    }
                }
            }

            // Wall sides configuration
            val hasAnyWall = verandaState.frontType == VerandaSideType.WALL ||
                    verandaState.backType == VerandaSideType.WALL ||
                    verandaState.leftType == VerandaSideType.WALL ||
                    verandaState.rightType == VerandaSideType.WALL

            if (hasAnyWall) {
                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                // Wall Skirting Toggle
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
                            text = "Skirting on wall sides",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        HelpTooltipButton(
                            title = "Skirting",
                            explanation = "The tile strip along the bottom of the wall.",
                            testTag = "veranda_skirting_help"
                        )
                    }
                    Switch(
                        checked = verandaState.hasWallSkirting,
                        onCheckedChange = { checked -> onUpdate { it.copy(hasWallSkirting = checked) } },
                        modifier = Modifier.testTag("veranda_skirting_toggle")
                    )
                }
                Text(
                    text = "Cut from your floor tiles along the bottom of the wall.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (verandaState.hasWallSkirting) {
                    Spacer(modifier = Modifier.height(8.dp))
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
                            val isSelected = verandaState.wallSkirtingPreset == preset
                            FilterChip(
                                selected = isSelected,
                                onClick = { onUpdate { it.copy(wallSkirtingPreset = preset) } },
                                label = { Text(preset.label) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("veranda_skirting_preset_${preset.name.lowercase()}")
                            )
                        }
                    }

                    if (verandaState.wallSkirtingPreset == SkirtingPreset.CUSTOM) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = verandaState.customWallSkirtingHeight,
                            onValueChange = { h -> onUpdate { it.copy(customWallSkirtingHeight = h) } },
                            label = { Text("Custom skirting height ($heightUnit)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("veranda_custom_skirting_height_input"),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Door on Veranda Wall
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoorFront,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Door on wall side",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = "Reduces skirting length and applies tile height at the door.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = verandaState.hasDoor,
                        onCheckedChange = { checked -> onUpdate { it.copy(hasDoor = checked) } },
                        modifier = Modifier.testTag("veranda_door_toggle")
                    )
                }

                if (verandaState.hasDoor) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Door Wall Location",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val wallSides = mutableListOf<VerandaSide>()
                    if (verandaState.frontType == VerandaSideType.WALL) wallSides.add(VerandaSide.FRONT)
                    if (verandaState.backType == VerandaSideType.WALL) wallSides.add(VerandaSide.BACK)
                    if (verandaState.leftType == VerandaSideType.WALL) wallSides.add(VerandaSide.LEFT)
                    if (verandaState.rightType == VerandaSideType.WALL) wallSides.add(VerandaSide.RIGHT)

                    if (wallSides.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            wallSides.forEach { side ->
                                val isSelected = verandaState.doorSide == side
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onUpdate { it.copy(doorSide = side) } },
                                    label = { Text(side.label) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("veranda_door_side_${side.name.lowercase()}")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = verandaState.doorWidth,
                            onValueChange = { w -> onUpdate { it.copy(doorWidth = w) } },
                            label = { Text("Door width (${unit.symbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("veranda_door_width_input"),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                        )

                        OutlinedTextField(
                            value = verandaState.doorStripHeight,
                            onValueChange = { h -> onUpdate { it.copy(doorStripHeight = h) } },
                            label = { Text("Tile height at the door ($heightUnit)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("veranda_door_strip_height_input"),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerandaInteractiveDiagram(
    verandaState: VerandaState,
    unit: UnitSystem,
    onToggleSide: (VerandaSide) -> Unit
) {
    val stepColor = Color(0xFFE65100) // Deep Orange for Step
    val wallColor = MaterialTheme.colorScheme.primary // Primary for Wall

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .testTag("veranda_diagram_interactive"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Back side
            DiagramSideButton(
                sideName = "Back",
                lengthText = "${verandaState.length.ifBlank { "0" }} ${unit.symbol}",
                type = verandaState.backType,
                stepColor = stepColor,
                wallColor = wallColor,
                onClick = { onToggleSide(VerandaSide.BACK) },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(38.dp)
                    .testTag("diagram_toggle_back")
            )

            // Middle: Left, Center Slab, Right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side
                DiagramSideButton(
                    sideName = "Left",
                    lengthText = "${verandaState.width.ifBlank { "0" }} ${unit.symbol}",
                    type = verandaState.leftType,
                    stepColor = stepColor,
                    wallColor = wallColor,
                    onClick = { onToggleSide(VerandaSide.LEFT) },
                    modifier = Modifier
                        .width(76.dp)
                        .fillMaxHeight()
                        .testTag("diagram_toggle_left")
                )

                // Floor slab representation
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Veranda Floor",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tap any edge",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Right side
                DiagramSideButton(
                    sideName = "Right",
                    lengthText = "${verandaState.width.ifBlank { "0" }} ${unit.symbol}",
                    type = verandaState.rightType,
                    stepColor = stepColor,
                    wallColor = wallColor,
                    onClick = { onToggleSide(VerandaSide.RIGHT) },
                    modifier = Modifier
                        .width(76.dp)
                        .fillMaxHeight()
                        .testTag("diagram_toggle_right")
                )
            }

            // Bottom: Front side
            DiagramSideButton(
                sideName = "Front",
                lengthText = "${verandaState.length.ifBlank { "0" }} ${unit.symbol}",
                type = verandaState.frontType,
                stepColor = stepColor,
                wallColor = wallColor,
                onClick = { onToggleSide(VerandaSide.FRONT) },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(38.dp)
                    .testTag("diagram_toggle_front")
            )
        }
    }
}

@Composable
private fun DiagramSideButton(
    sideName: String,
    lengthText: String,
    type: VerandaSideType,
    stepColor: Color,
    wallColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isStep = type == VerandaSideType.OPENING_STEP
    val bgColor = if (isStep) stepColor.copy(alpha = 0.18f) else wallColor.copy(alpha = 0.18f)
    val borderColor = if (isStep) stepColor else wallColor

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$sideName: ${if (isStep) "Step" else "Wall"}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = borderColor,
                maxLines = 1
            )
            Text(
                text = lengthText,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VerandaSideRow(
    sideName: String,
    dimLabel: String,
    currentType: VerandaSideType,
    onTypeChange: (VerandaSideType) -> Unit,
    testTagPrefix: String
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sideName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dimLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VerandaSideType.values().forEach { type ->
                    val isSelected = currentType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { onTypeChange(type) },
                        label = {
                            Text(
                                type.label,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("${testTagPrefix}_${type.name.lowercase()}")
                    )
                }
            }
        }
    }
}
