package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalculationResult

@Composable
fun ResultsView(
    result: CalculationResult,
    jobName: String = "",
    clientName: String = "",
    builderName: String = "",
    builderPhone: String = "",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("results_view_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {

            // Header
            // Header: What to buy
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "WHAT TO BUY",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // BIG NUMBERS SECTION - TILES ON TOP (B4)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth < 360.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Boxes To Buy
                            StatBox(
                                label = "BOXES TO BUY",
                                value = result.totalBoxesToBuy.toString(),
                                unit = "boxes",
                                isHighlight = true,
                                modifier = Modifier.weight(1f),
                                testTag = "result_boxes_to_buy"
                            )

                            // Total Tiles
                            StatBox(
                                label = "TOTAL TILES",
                                value = result.totalTilesCount.toString(),
                                unit = "tiles",
                                isHighlight = false,
                                modifier = Modifier.weight(1f),
                                testTag = "result_total_tiles"
                            )
                        }

                        // Spare tiles left over after boxes
                        StatBox(
                            label = "Spare tiles left over after boxes",
                            value = result.floorSpareTiles.toString() + (if (result.hasWallTiles) " (+${result.wallSpareTiles})" else ""),
                            unit = "tiles",
                            isHighlight = false,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "result_spare_tiles"
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Boxes To Buy
                        StatBox(
                            label = "BOXES TO BUY",
                            value = result.totalBoxesToBuy.toString(),
                            unit = "boxes",
                            isHighlight = true,
                            modifier = Modifier.weight(1f),
                            testTag = "result_boxes_to_buy"
                        )

                        // Total Tiles
                        StatBox(
                            label = "TOTAL TILES",
                            value = result.totalTilesCount.toString(),
                            unit = "tiles",
                            isHighlight = false,
                            modifier = Modifier.weight(1f),
                            testTag = "result_total_tiles"
                        )

                        // Spare tiles left over after boxes
                        StatBox(
                            label = "Spare tiles left over after boxes",
                            value = result.floorSpareTiles.toString() + (if (result.hasWallTiles) " (+${result.wallSpareTiles})" else ""),
                            unit = "tiles",
                            isHighlight = false,
                            modifier = Modifier.weight(1.2f),
                            testTag = "result_spare_tiles"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // MAIN SUMMARY: MATERIALS TO BUY SHOPPING LIST
            // ==========================================
            MaterialsToBuyCard(
                result = result,
                jobName = jobName,
                clientName = clientName,
                builderName = builderName,
                builderPhone = builderPhone
            )

            // Warnings in yellow (B4)
            if (result.warnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                result.warnings.forEach { warning ->
                    Surface(
                        color = Color(0xFFFFF3CD),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("result_warning_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = Color(0xFF856404)
                            )
                            Text(
                                text = warning,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF856404),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            var isHowWorkedOutExpanded by remember { mutableStateOf(false) }

            // Collapsible: "How was this worked out?" (B4)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("how_worked_out_card")
                    .clickable { isHowWorkedOutExpanded = !isHowWorkedOutExpanded },
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "How was this worked out?",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = if (isHowWorkedOutExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isHowWorkedOutExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = isHowWorkedOutExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                // Base floor tiles (no waste)
                BreakdownRow(
                    label = "Base Floor Tiles",
                    count = "${result.baseFloorTiles} tiles",
                    detail = "Net Floor Area: %.2f m² (no waste)".format(result.floorAreaM2),
                    testTag = "breakdown_base_floor_tiles"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Waste allowance on its own line: "Waste allowance (10%): 3 tiles, for cuts and breakage"
                val wasteUnit = if (result.floorWasteAllowanceTiles == 1) "tile" else "tiles"
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("breakdown_waste_allowance_line")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Extra for cuts and breakage (${result.wastePercentage}%): ${result.floorWasteAllowanceTiles} $wasteUnit",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Waste is applied only to floor and wall areas. Strip and step-face tiles already count their own cut leftovers, so no waste is added to them.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Floor tiles with waste
                BreakdownRow(
                    label = "Floor Tiles (with waste)",
                    count = "${result.floorTilesRawNeeded} tiles",
                    detail = "Base ${result.baseFloorTiles} + Waste ${result.floorWasteAllowanceTiles}",
                    testTag = "breakdown_floor_tiles"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                BreakdownRow(
                    label = "Skirting Tiles",
                    count = "${result.skirtingTilesNeeded} tiles",
                    detail = if (result.skirtingTilesNeeded > 0) "Cut from floor tiles (no waste added)" else "None",
                    testTag = "breakdown_skirting_tiles"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                BreakdownRow(
                    label = "Step-Face Tiles",
                    count = "${result.stepFaceTilesNeeded} tiles",
                    detail = if (result.stepFaceTilesNeeded > 0) "Cut from floor tiles (no waste added)" else "None",
                    testTag = "breakdown_step_tiles"
                )

                if (result.hasWallTiles) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    BreakdownRow(
                        label = "Base Wall Tiles",
                        count = "${result.baseWallTiles} tiles",
                        detail = "Net Wall Area: %.2f m² (no waste)".format(result.netWallAreaM2),
                        testTag = "breakdown_base_wall_tiles"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    val wallWasteUnit = if (result.wallWasteAllowanceTiles == 1) "tile" else "tiles"
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("breakdown_wall_waste_allowance_line")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Wall waste allowance (${result.wastePercentage}%): ${result.wallWasteAllowanceTiles} $wallWasteUnit, for cuts and breakage",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Waste is applied only to floor and wall areas. Strip and step-face tiles already count their own cut leftovers, so no waste is added to them.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    BreakdownRow(
                        label = "Wall Tiles (Bathroom)",
                        count = "${result.wallTilesNeeded} tiles (${result.wallBoxesToBuy} boxes)",
                        detail = "Base ${result.baseWallTiles} + Waste ${result.wallWasteAllowanceTiles}",
                        testTag = "breakdown_wall_tiles"
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Spare tiles left over after boxes
                BreakdownRow(
                    label = "Spare tiles left over after boxes",
                    count = "${result.floorSpareTiles} tiles" + (if (result.hasWallTiles) " (+${result.wallSpareTiles} wall)" else ""),
                    detail = "Boxes × tiles per box - total tiles",
                    testTag = "breakdown_spare_tiles"
                )

                if (result.runMethodSummaries.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Text(
                        text = "Face & Strip Runs:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    result.runMethodSummaries.forEach { summary ->
                        Text(
                            text = "• $summary",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (result.areaBreakdowns.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "PER-AREA BREAKDOWN",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    result.areaBreakdowns.forEachIndexed { idx, area ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            BreakdownRow(
                                label = area.areaName,
                                count = "${area.totalAreaTiles} tiles",
                                detail = "${area.spaceType.displayName} • ${area.summaryText}",
                                testTag = "breakdown_area_${area.areaId}"
                            )
                            if (area.trimRuns.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Trim runs:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    area.trimRuns.forEach { run ->
                                        Text(
                                            text = "• ${run.displayText.ifBlank { "${run.name}, ${run.material.label.lowercase()}: %.2f m".format(java.util.Locale.US, run.lengthM) }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        if (idx < result.areaBreakdowns.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // MATERIALS SECTION (Under tile results)
            // ==========================================
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "MATERIALS REQUIRED",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Adhesive bags
                val adhesiveDetail = if (result.materials.wallAndSkirtingAreaM2 > 0.0) {
                    val wallLabel = if (result.materials.wallAreaM2 > 0.0 && result.materials.skirtingAndStepAreaM2 > 0.0) "skirting, step and wall"
                        else if (result.materials.wallAreaM2 > 0.0) "wall"
                        else "skirting and step"
                    "Floor %.2f + %s %.2f = %.2f, so %d bags".format(
                        java.util.Locale.US,
                        result.materials.floorAdhesiveBagsRaw,
                        wallLabel,
                        result.materials.wallAdhesiveBagsRaw,
                        result.materials.totalAdhesiveBagsRaw,
                        result.materials.totalAdhesiveBags
                    )
                } else {
                    "Floor %.2f, so %d bags".format(
                        java.util.Locale.US,
                        result.materials.floorAdhesiveBagsRaw,
                        result.materials.totalAdhesiveBags
                    )
                }
                MaterialItemRow(
                    icon = Icons.Default.Layers,
                    title = "Adhesive (20 kg bags)",
                    quantity = "${result.materials.totalAdhesiveBags} bags",
                    details = adhesiveDetail,
                    testTag = "material_adhesive_bags"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Grout bags
                val bagSizeStr = "${result.materials.groutBagSizeKg.toInt()} kg bag"
                MaterialItemRow(
                    icon = Icons.Default.Grain,
                    title = "Grout",
                    quantity = "${result.materials.groutBagsToBuy} ${if (result.materials.groutBagsToBuy == 1) "bag" else "bags"} of $bagSizeStr",
                    details = "Total: %.2f kg (at %.1f mm joint)".format(result.materials.totalGroutKg, result.materials.jointWidthMm),
                    testTag = "material_grout_bags"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Spacers
                MaterialItemRow(
                    icon = Icons.Default.AllInclusive,
                    title = "Spacers (${result.materials.jointWidthMm} mm)",
                    quantity = "${result.materials.spacerPacksToBuy} ${if (result.materials.spacerPacksToBuy == 1) "pack" else "packs"}",
                    details = "${result.materials.totalSpacersNeeded} spacers (${result.materials.spacerPackSize} per pack)",
                    testTag = "material_spacer_packs"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Bonding Liquid
                MaterialItemRow(
                    icon = Icons.Default.WaterDrop,
                    title = "Bonding Liquid",
                    quantity = result.materials.bondingLiquidDisplayText,
                    details = "${result.materials.bondingMode.shortLabel} (Total %.2f L)".format(result.materials.totalBondingLiquidLiters),
                    testTag = "material_bonding_liquid"
                )

                val pieceLenText = if (result.trims.pieceLengthM % 1.0 == 0.0) "${result.trims.pieceLengthM.toInt()}" else "%.1f".format(java.util.Locale.US, result.trims.pieceLengthM)

                // Plastic Trims
                if (result.trims.plasticPiecesNeeded > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    MaterialItemRow(
                        icon = Icons.Default.Straighten,
                        title = "Plastic trims",
                        quantity = "${result.trims.plasticPiecesNeeded} ${if (result.trims.plasticPiecesNeeded == 1) "piece" else "pieces"} ($pieceLenText m each)",
                        details = "%.2f m run (+%d%%) | %s m pieces".format(
                            result.trims.plasticTotalRunLengthM,
                            result.trims.allowancePercent,
                            pieceLenText
                        ),
                        testTag = "material_plastic_trims"
                    )
                }

                // Metal Trims
                if (result.trims.metalPiecesNeeded > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    MaterialItemRow(
                        icon = Icons.Default.Architecture,
                        title = "Metal trims",
                        quantity = "${result.trims.metalPiecesNeeded} ${if (result.trims.metalPiecesNeeded == 1) "piece" else "pieces"} ($pieceLenText m each)",
                        details = "%.2f m run (+%d%%) | %s m pieces".format(
                            result.trims.metalTotalRunLengthM,
                            result.trims.allowancePercent,
                            pieceLenText
                        ),
                        testTag = "material_metal_trims"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // FORMULA AND MATH VERIFICATION (Under each result, show formula and numbers used)
            MathVerificationCard(result = result)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    quantity: String,
    details: String,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
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
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = quantity,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    unit: String,
    isHighlight: Boolean,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Surface(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = if (isHighlight) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = if (isHighlight) null else CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (isHighlight) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                minLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            val valueFontSize = if (value.length > 6) 18.sp else if (value.length > 4) 22.sp else 28.sp
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = valueFontSize),
                fontWeight = FontWeight.Black,
                maxLines = 1,
                color = if (isHighlight) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = if (isHighlight) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    count: String,
    detail: String,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = count,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun MathVerificationCard(
    result: CalculationResult
) {
    var isExpanded by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("math_verification_section"),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Formulas & Numbers Used",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Floor Math
                    FormulaBlock(
                        title = "1. Floor Tiles & Boxing Math",
                        content = result.floorFormulaExplanation
                    )

                    // Skirting Math
                    if (result.floorSkirtingFormulaExplanation.isNotBlank()) {
                        FormulaBlock(
                            title = "2. Skirting Strip Math",
                            content = result.floorSkirtingFormulaExplanation
                        )
                    }

                    // Step Face Math
                    if (result.floorStepFaceFormulaExplanation.isNotBlank()) {
                        FormulaBlock(
                            title = "3. Step-Face Strip Math",
                            content = result.floorStepFaceFormulaExplanation
                        )
                    }

                    // Wall Math
                    if (result.hasWallTiles && result.wallFormulaExplanation.isNotBlank()) {
                        FormulaBlock(
                            title = "4. Wall Tiles Math",
                            content = result.wallFormulaExplanation
                        )
                    }

                    // 5. Adhesive Math
                    FormulaBlock(
                        title = "5. Adhesive (20 kg bags) Math",
                        content = result.materials.adhesiveFormulaExplanation
                    )

                    // 6. Grout Math
                    FormulaBlock(
                        title = "6. Grout Bags Math",
                        content = result.materials.groutFormulaExplanation
                    )

                    // 7. Spacers Math
                    FormulaBlock(
                        title = "7. Spacers Packs Math",
                        content = result.materials.spacerFormulaExplanation
                    )

                    // 8. Bonding Liquid Math
                    FormulaBlock(
                        title = "8. Bonding Liquid Math",
                        content = result.materials.bondingLiquidFormulaExplanation
                    )

                    // 9. Plastic Edge Trims Math
                    if (result.trims.plasticTotalRunLengthM > 0.0) {
                        FormulaBlock(
                            title = "9. Plastic Edge Trims Math",
                            content = result.trims.plasticFormulaExplanation
                        )
                    }

                    // 10. Metal Edge Trims Math
                    if (result.trims.metalTotalRunLengthM > 0.0) {
                        FormulaBlock(
                            title = "10. Metal Edge Trims Math",
                            content = result.trims.metalFormulaExplanation
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormulaBlock(
    title: String,
    content: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
