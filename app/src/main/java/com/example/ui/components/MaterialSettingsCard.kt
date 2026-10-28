package com.example.ui.components

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MaterialSettingsCard(
    settings: MaterialSettingsState,
    trimSettings: TrimSettingsState,
    onUpdateMaterialSettings: ((MaterialSettingsState) -> MaterialSettingsState) -> Unit,
    onUpdateTrimSettings: ((TrimSettingsState) -> TrimSettingsState) -> Unit,
    onResetToStandard: () -> Unit,
    onAddOuterCorner: () -> Unit,
    onRemoveOuterCorner: (Int) -> Unit,
    onUpdateOuterCorner: (Int, String, String, TrimMaterial, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBondingMore by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // "Use standard settings" reset button
        OutlinedButton(
            onClick = onResetToStandard,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("reset_standard_materials_button")
        ) {
            Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Use standard settings",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // ==========================================
        // 1. TILE CEMENT (ADHESIVE)
        // ==========================================
        SectionTitle(title = "TILE CEMENT (ADHESIVE)")

        // "How flat is the floor?"
        FieldLabel(
            label = "How flat is the floor?",
            note = "Uneven floors need a thicker layer of cement, so you need more bags."
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val isFlat = settings.floorBedPreset == FloorBedThicknessPreset.LEVEL_5MM
            FilterChip(
                selected = isFlat,
                onClick = { onUpdateMaterialSettings { it.copy(floorBedPreset = FloorBedThicknessPreset.LEVEL_5MM) } },
                label = { Text("Flat", fontSize = 14.sp) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("floor_bed_flat")
            )

            val isBitUneven = settings.floorBedPreset == FloorBedThicknessPreset.UNEVEN_8MM
            FilterChip(
                selected = isBitUneven,
                onClick = { onUpdateMaterialSettings { it.copy(floorBedPreset = FloorBedThicknessPreset.UNEVEN_8MM) } },
                label = { Text("A bit uneven", fontSize = 14.sp) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("floor_bed_a_bit_uneven")
            )

            val isVeryUneven = settings.floorBedPreset == FloorBedThicknessPreset.VERY_UNEVEN_12MM
            FilterChip(
                selected = isVeryUneven,
                onClick = { onUpdateMaterialSettings { it.copy(floorBedPreset = FloorBedThicknessPreset.VERY_UNEVEN_12MM) } },
                label = { Text("Very uneven", fontSize = 14.sp) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("floor_bed_very_uneven")
            )

            val isCustomFloor = settings.floorBedPreset == FloorBedThicknessPreset.CUSTOM
            FilterChip(
                selected = isCustomFloor,
                onClick = { onUpdateMaterialSettings { it.copy(floorBedPreset = FloorBedThicknessPreset.CUSTOM) } },
                label = { Text("Other", fontSize = 14.sp) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("floor_bed_custom")
            )
        }

        if (settings.floorBedPreset == FloorBedThicknessPreset.CUSTOM) {
            OutlinedTextField(
                value = settings.customFloorBedMm,
                onValueChange = { t -> onUpdateMaterialSettings { it.copy(customFloorBedMm = t) } },
                label = { Text("Thickness of cement layer (mm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_floor_bed_input"),
                singleLine = true
            )
        }

        // For walls
        Spacer(modifier = Modifier.height(4.dp))
        FieldLabel(
            label = "How thick is the cement layer on walls?",
            note = "Normal walls use 6 mm, thicker for uneven brickwork."
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val isThin = settings.wallBedPreset == WallBedThicknessPreset.BED_3MM
            FilterChip(
                selected = isThin,
                onClick = { onUpdateMaterialSettings { it.copy(wallBedPreset = WallBedThicknessPreset.BED_3MM) } },
                label = { Text("Thin", fontSize = 14.sp) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("wall_bed_thin")
            )

            val isNormal = settings.wallBedPreset == WallBedThicknessPreset.BED_6MM
            FilterChip(
                selected = isNormal,
                onClick = { onUpdateMaterialSettings { it.copy(wallBedPreset = WallBedThicknessPreset.BED_6MM) } },
                label = { Text("Normal", fontSize = 14.sp) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("wall_bed_normal")
            )

            val isThick = settings.wallBedPreset == WallBedThicknessPreset.BED_10MM
            FilterChip(
                selected = isThick,
                onClick = { onUpdateMaterialSettings { it.copy(wallBedPreset = WallBedThicknessPreset.BED_10MM) } },
                label = { Text("Thick", fontSize = 14.sp) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("wall_bed_thick")
            )

            val isCustomWall = settings.wallBedPreset == WallBedThicknessPreset.CUSTOM
            FilterChip(
                selected = isCustomWall,
                onClick = { onUpdateMaterialSettings { it.copy(wallBedPreset = WallBedThicknessPreset.CUSTOM) } },
                label = { Text("Other", fontSize = 14.sp) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("wall_bed_custom")
            )
        }

        if (settings.wallBedPreset == WallBedThicknessPreset.CUSTOM) {
            OutlinedTextField(
                value = settings.customWallBedMm,
                onValueChange = { t -> onUpdateMaterialSettings { it.copy(customWallBedMm = t) } },
                label = { Text("Thickness of cement layer on walls (mm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_wall_bed_input"),
                singleLine = true
            )
        }

        // Coverage per bag
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = settings.baseCoverageAt5mm,
            onValueChange = { cov -> onUpdateMaterialSettings { it.copy(baseCoverageAt5mm = cov) } },
            label = { Text("How much floor does one 20 kg bag cover? (m²)") },
            supportingText = { Text("Check the bag. If your brand says something different, type it here.", fontSize = 13.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("adhesive_base_coverage_input"),
            singleLine = true
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ==========================================
        // 2. GROUT (THE FILLER BETWEEN TILES)
        // ==========================================
        SectionTitle(title = "GROUT (THE FILLER BETWEEN TILES)")

        // Gap between tiles (mm)
        FieldLabel(
            label = "Gap between tiles (mm)",
            note = "This is the size of your spacers."
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val commonGaps = listOf(2.0, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0)
            commonGaps.forEach { gap ->
                val isSelected = Math.abs(settings.jointWidthMm - gap) < 0.05
                val label = if (gap % 1.0 == 0.0) "${gap.toInt()} mm" else "$gap mm"
                FilterChip(
                    selected = isSelected,
                    onClick = { onUpdateMaterialSettings { it.copy(jointWidthMm = gap) } },
                    label = { Text(label, fontSize = 14.sp) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("joint_width_${gap.toInt()}")
                )
            }
        }

        // Grout depth (mm)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = settings.jointDepthMm,
            onValueChange = { d -> onUpdateMaterialSettings { it.copy(jointDepthMm = d) } },
            label = { Text("Grout depth (mm)") },
            supportingText = { Text("Usually the same as the tile thickness.", fontSize = 13.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("joint_depth_input"),
            singleLine = true
        )

        // Grout type
        Spacer(modifier = Modifier.height(4.dp))
        FieldLabel(
            label = "Grout type",
            note = "Cement grout is standard. Epoxy is waterproof and chemical resistant."
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val isCement = settings.groutType == GroutType.CEMENT
            FilterChip(
                selected = isCement,
                onClick = { onUpdateMaterialSettings { it.copy(groutType = GroutType.CEMENT) } },
                label = { Text("Normal (cement)", fontSize = 14.sp) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("grout_type_cement")
            )

            val isEpoxy = settings.groutType == GroutType.EPOXY
            FilterChip(
                selected = isEpoxy,
                onClick = { onUpdateMaterialSettings { it.copy(groutType = GroutType.EPOXY) } },
                label = { Text("Epoxy (special, costs more)", fontSize = 14.sp) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("grout_type_epoxy")
            )
        }

        // Bag size
        Spacer(modifier = Modifier.height(4.dp))
        FieldLabel(
            label = "Bag size",
            note = "Choose the size sold in your store."
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val is5kg = settings.groutBagSize == GroutBagSize.BAG_5KG
            FilterChip(
                selected = is5kg,
                onClick = { onUpdateMaterialSettings { it.copy(groutBagSize = GroutBagSize.BAG_5KG) } },
                label = { Text("5 kg", fontSize = 14.sp) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("grout_bag_size_bag_5kg")
            )

            val is2kg = settings.groutBagSize == GroutBagSize.BAG_2KG
            FilterChip(
                selected = is2kg,
                onClick = { onUpdateMaterialSettings { it.copy(groutBagSize = GroutBagSize.BAG_2KG) } },
                label = { Text("2 kg", fontSize = 14.sp) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("grout_bag_size_bag_2kg")
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ==========================================
        // 3. SPACERS (THE LITTLE PLASTIC CROSSES)
        // ==========================================
        SectionTitle(title = "SPACERS (THE LITTLE PLASTIC CROSSES)")

        OutlinedTextField(
            value = settings.spacersPerTile,
            onValueChange = { s -> onUpdateMaterialSettings { it.copy(spacersPerTile = s.filter { c -> c.isDigit() }) } },
            label = { Text("Spacers per tile") },
            supportingText = { Text("2 is normal.", fontSize = 13.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("spacers_per_tile_input"),
            singleLine = true
        )

        OutlinedTextField(
            value = settings.spacerPackSize,
            onValueChange = { p -> onUpdateMaterialSettings { it.copy(spacerPackSize = p.filter { c -> c.isDigit() }) } },
            label = { Text("Spacers in one pack") },
            supportingText = { Text("Standard packs usually contain 100 or 250 spacers.", fontSize = 13.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("spacer_pack_size_input"),
            singleLine = true
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ==========================================
        // 4. BONDING LIQUID
        // ==========================================
        SectionTitle(title = "BONDING LIQUID")

        FieldLabel(
            label = "Where is it used?",
            note = "Improves cement adhesion and water resistance."
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val isNormal = settings.bondingMode == BondingLiquidMode.ADDITIVE_SPLASH
            FilterChip(
                selected = isNormal,
                onClick = { onUpdateMaterialSettings { it.copy(bondingMode = BondingLiquidMode.ADDITIVE_SPLASH) } },
                label = { Text("Normal rooms", fontSize = 14.sp) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("bonding_mode_additive_splash")
            )

            val isWet = settings.bondingMode == BondingLiquidMode.FULL_REPLACEMENT
            FilterChip(
                selected = isWet,
                onClick = { onUpdateMaterialSettings { it.copy(bondingMode = BondingLiquidMode.FULL_REPLACEMENT) } },
                label = { Text("Wet areas (bathroom, shower)", fontSize = 14.sp) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("bonding_mode_full_replacement")
            )
        }

        // Small "More" link for exact ml / litre amounts
        Row(
            modifier = Modifier
                .clickable { showBondingMore = !showBondingMore }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showBondingMore) "Hide amounts" else "More (change mixing ratio)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = if (showBondingMore) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(visible = showBondingMore) {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                if (settings.bondingMode == BondingLiquidMode.ADDITIVE_SPLASH) {
                    OutlinedTextField(
                        value = settings.additiveMlPerBag,
                        onValueChange = { ml -> onUpdateMaterialSettings { it.copy(additiveMlPerBag = ml) } },
                        label = { Text("ml per 20 kg adhesive bag") },
                        supportingText = { Text("Default 250 ml splash per bag.", fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("additive_ml_input"),
                        singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = settings.replacementLitersPerBag,
                        onValueChange = { l -> onUpdateMaterialSettings { it.copy(replacementLitersPerBag = l) } },
                        label = { Text("Litres per 20 kg adhesive bag") },
                        supportingText = { Text("Default 5 litres per bag.", fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("replacement_liters_input"),
                        singleLine = true
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ==========================================
        // 5. TRIMS
        // ==========================================
        SectionTitle(title = "TRIMS")

        OutlinedTextField(
            value = trimSettings.pieceLengthM,
            onValueChange = { len -> onUpdateTrimSettings { it.copy(pieceLengthM = len) } },
            label = { Text("Length of one strip (m)") },
            supportingText = { Text("Standard trims are usually 2.5 meters long.", fontSize = 13.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("trim_piece_length_input"),
            singleLine = true
        )

        OutlinedTextField(
            value = trimSettings.allowancePercent.toString(),
            onValueChange = { v ->
                val pct = v.filter { it.isDigit() }.toIntOrNull() ?: 10
                onUpdateTrimSettings { it.copy(allowancePercent = pct) }
            },
            label = { Text("Extra for cutting (%)") },
            supportingText = { Text("Extra percentage to cover offcuts and corners (default 10%).", fontSize = 13.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("trim_allowance_input"),
            singleLine = true
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ==========================================
        // 6. OUTSIDE CORNERS
        // ==========================================
        SectionTitle(title = "OUTSIDE CORNERS")

        Text(
            text = "Outside corners (where a tiled edge sticks out)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "An outside corner is where two tiled sides meet and stick out, like the corner of a wall, a step edge or a tiled counter. A strip covers the raw tile edge so it does not chip. Add one for each outside corner. You can ignore this on a plain room floor.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )

        Button(
            onClick = onAddOuterCorner,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("add_outer_corner_button")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add a corner strip", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        if (trimSettings.outerCorners.isNotEmpty()) {
            trimSettings.outerCorners.forEachIndexed { index, corner ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = corner.name.ifBlank { "Corner ${index + 1}" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { onRemoveOuterCorner(index) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("delete_corner_$index")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        OutlinedTextField(
                            value = corner.lengthM,
                            onValueChange = { len -> onUpdateOuterCorner(index, corner.name, len, corner.material, corner.isEnabled) },
                            label = { Text("Length (m)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Material:", style = MaterialTheme.typography.bodySmall)
                            TrimMaterialSelector(
                                selected = corner.material,
                                onSelect = { mat -> onUpdateOuterCorner(index, corner.name, corner.lengthM, mat, corner.isEnabled) },
                                testTagPrefix = "corner_$index"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 0.8.sp
    )
}

@Composable
private fun FieldLabel(label: String, note: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
    }
}
