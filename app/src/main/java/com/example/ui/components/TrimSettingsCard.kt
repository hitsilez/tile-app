package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
fun TrimSettingsCard(
    settings: TrimSettingsState,
    spaceType: SpaceType,
    unit: UnitSystem,
    verandaState: VerandaState,
    bathroomState: BathroomState,
    roomHasSkirting: Boolean,
    onUpdate: ((TrimSettingsState) -> TrimSettingsState) -> Unit,
    onAddOuterCorner: () -> Unit,
    onRemoveOuterCorner: (Int) -> Unit,
    onUpdateOuterCorner: (Int, String, String, TrimMaterial, Boolean) -> Unit,
    onAddCustomRun: () -> Unit,
    onRemoveCustomRun: (Int) -> Unit,
    onUpdateCustomRun: (Int, String, String, TrimMaterial, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("trim_settings_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header with toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                            text = "EDGE TRIM SETTINGS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Plastic & Metal finishing profiles",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {

                    // 1. Piece length and Allowance %
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = settings.pieceLengthM,
                            onValueChange = { len -> onUpdate { it.copy(pieceLengthM = len) } },
                            label = { Text("Piece Length (m)") },
                            supportingText = { Text("Default 2.5 m") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("trim_piece_length_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = settings.allowancePercent.toString(),
                            onValueChange = { v ->
                                val pct = v.filter { it.isDigit() }.toIntOrNull() ?: 10
                                onUpdate { it.copy(allowancePercent = pct) }
                            },
                            label = { Text("Allowance %") },
                            supportingText = { Text("Default 10%") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("trim_allowance_input"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Space Specific Auto-Runs
                    Text(
                        text = "EDGE RUNS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // A. Veranda step edge runs
                    if (spaceType == SpaceType.VERANDA) {
                        Text(
                            text = "Veranda Step Edges (Openings)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        if (verandaState.frontType == VerandaSideType.OPENING_STEP) {
                            TrimRunRow(
                                name = "Front Step Edge (${verandaState.length} ${unit.symbol})",
                                isEnabled = settings.verandaStepFront.isEnabled,
                                material = settings.verandaStepFront.material,
                                onToggle = { en -> onUpdate { it.copy(verandaStepFront = it.verandaStepFront.copy(isEnabled = en)) } },
                                onMaterialChange = { mat -> onUpdate { it.copy(verandaStepFront = it.verandaStepFront.copy(material = mat)) } },
                                testTagPrefix = "trim_v_front"
                            )
                        }
                        if (verandaState.backType == VerandaSideType.OPENING_STEP) {
                            TrimRunRow(
                                name = "Back Step Edge (${verandaState.length} ${unit.symbol})",
                                isEnabled = settings.verandaStepBack.isEnabled,
                                material = settings.verandaStepBack.material,
                                onToggle = { en -> onUpdate { it.copy(verandaStepBack = it.verandaStepBack.copy(isEnabled = en)) } },
                                onMaterialChange = { mat -> onUpdate { it.copy(verandaStepBack = it.verandaStepBack.copy(material = mat)) } },
                                testTagPrefix = "trim_v_back"
                            )
                        }
                        if (verandaState.leftType == VerandaSideType.OPENING_STEP) {
                            TrimRunRow(
                                name = "Left Step Edge (${verandaState.width} ${unit.symbol})",
                                isEnabled = settings.verandaStepLeft.isEnabled,
                                material = settings.verandaStepLeft.material,
                                onToggle = { en -> onUpdate { it.copy(verandaStepLeft = it.verandaStepLeft.copy(isEnabled = en)) } },
                                onMaterialChange = { mat -> onUpdate { it.copy(verandaStepLeft = it.verandaStepLeft.copy(material = mat)) } },
                                testTagPrefix = "trim_v_left"
                            )
                        }
                        if (verandaState.rightType == VerandaSideType.OPENING_STEP) {
                            TrimRunRow(
                                name = "Right Step Edge (${verandaState.width} ${unit.symbol})",
                                isEnabled = settings.verandaStepRight.isEnabled,
                                material = settings.verandaStepRight.material,
                                onToggle = { en -> onUpdate { it.copy(verandaStepRight = it.verandaStepRight.copy(isEnabled = en)) } },
                                onMaterialChange = { mat -> onUpdate { it.copy(verandaStepRight = it.verandaStepRight.copy(material = mat)) } },
                                testTagPrefix = "trim_v_right"
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // B. Skirting Top Edge
                    val skirtingAvailable = when (spaceType) {
                        SpaceType.ROOM_FLOOR -> roomHasSkirting
                        SpaceType.VERANDA -> verandaState.hasWallSkirting
                        SpaceType.BATHROOM -> bathroomState.hasSkirting
                    }

                    if (skirtingAvailable) {
                        Text(
                            text = "Skirting Top Edge",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TrimRunRow(
                            name = "Trim on top of skirting",
                            isEnabled = settings.hasSkirtingTopTrim,
                            material = settings.skirtingTopMaterial,
                            onToggle = { en -> onUpdate { it.copy(hasSkirtingTopTrim = en) } },
                            onMaterialChange = { mat -> onUpdate { it.copy(skirtingTopMaterial = mat) } },
                            testTagPrefix = "trim_skirting_top"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // C. Bathroom walls top edge
                    if (spaceType == SpaceType.BATHROOM && bathroomState.tileWallsToo) {
                        Text(
                            text = "Wall Tiles Top Edge",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TrimRunRow(
                            name = "Trim on top of wall tiles",
                            isEnabled = settings.hasBathroomWallTopTrim,
                            material = settings.bathroomWallTopMaterial,
                            onToggle = { en -> onUpdate { it.copy(hasBathroomWallTopTrim = en) } },
                            onMaterialChange = { mat -> onUpdate { it.copy(bathroomWallTopMaterial = mat) } },
                            testTagPrefix = "trim_wall_top"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // D. Outer Corners
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Outer Corners (${settings.outerCorners.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Profiles for exposed external corners",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = onAddOuterCorner,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("add_outer_corner_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Corner")
                        }
                    }

                    settings.outerCorners.forEachIndexed { index, corner ->
                        CustomTrimItemRow(
                            name = corner.name,
                            lengthStr = corner.lengthM,
                            unitSymbol = unit.symbol,
                            material = corner.material,
                            isEnabled = corner.isEnabled,
                            onUpdate = { n, l, m, en -> onUpdateOuterCorner(index, n, l, m, en) },
                            onDelete = { onRemoveOuterCorner(index) },
                            testTagPrefix = "corner_$index"
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // E. Custom Trim Runs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Custom Trim Runs (${settings.customRuns.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Door thresholds, dividers, window reveals, etc.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = onAddCustomRun,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("add_custom_trim_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Trim Run")
                        }
                    }

                    settings.customRuns.forEachIndexed { index, custom ->
                        CustomTrimItemRow(
                            name = custom.name,
                            lengthStr = custom.lengthM,
                            unitSymbol = unit.symbol,
                            material = custom.material,
                            isEnabled = custom.isEnabled,
                            onUpdate = { n, l, m, en -> onUpdateCustomRun(index, n, l, m, en) },
                            onDelete = { onRemoveCustomRun(index) },
                            testTagPrefix = "custom_trim_$index"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrimRunRow(
    name: String,
    isEnabled: Boolean,
    material: TrimMaterial,
    onToggle: (Boolean) -> Unit,
    onMaterialChange: (TrimMaterial) -> Unit,
    testTagPrefix: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Material selector chip (Plastic / Metal)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TrimMaterial.values().forEach { mat ->
                        val isSelected = material == mat
                        FilterChip(
                            selected = isSelected,
                            onClick = { onMaterialChange(mat) },
                            label = { Text(mat.label, fontSize = 11.sp) },
                            modifier = Modifier.testTag("${testTagPrefix}_mat_${mat.name.lowercase()}")
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("${testTagPrefix}_toggle")
                )
            }
        }
    }
}

@Composable
private fun CustomTrimItemRow(
    name: String,
    lengthStr: String,
    unitSymbol: String,
    material: TrimMaterial,
    isEnabled: Boolean,
    onUpdate: (String, String, TrimMaterial, Boolean) -> Unit,
    onDelete: () -> Unit,
    testTagPrefix: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { n -> onUpdate(n, lengthStr, material, isEnabled) },
                    label = { Text("Name / Location") },
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("${testTagPrefix}_name"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = lengthStr,
                    onValueChange = { l -> onUpdate(name, l, material, isEnabled) },
                    label = { Text("Length ($unitSymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${testTagPrefix}_length"),
                    singleLine = true
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("${testTagPrefix}_delete")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TrimMaterial.values().forEach { mat ->
                        val isSelected = material == mat
                        FilterChip(
                            selected = isSelected,
                            onClick = { onUpdate(name, lengthStr, mat, isEnabled) },
                            label = { Text(mat.label, fontSize = 11.sp) },
                            modifier = Modifier.testTag("${testTagPrefix}_mat_${mat.name.lowercase()}")
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isEnabled) "Active" else "Off",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { en -> onUpdate(name, lengthStr, material, en) },
                        modifier = Modifier.testTag("${testTagPrefix}_switch")
                    )
                }
            }
        }
    }
}
