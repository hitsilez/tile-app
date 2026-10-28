package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalculationResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ShoppingItemData(
    val title: String,
    val quantityFormatted: String,
    val detailsFormatted: String,
    val plainTextLine: String,
    val icon: ImageVector,
    val testTag: String,
    val wasteAllowanceText: String? = null
)

@Composable
fun MaterialsToBuyCard(
    result: CalculationResult,
    jobName: String = "",
    clientName: String = "",
    builderName: String = "",
    builderPhone: String = "",
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var showCopiedConfirmation by remember { mutableStateOf(false) }

    val shoppingItems = remember(result) {
        val list = mutableListOf<ShoppingItemData>()

        // 1. Floor tiles: boxes (and tiles per box)
        if (result.floorBoxesToBuy > 0) {
            val labelPart = if (result.floorTileLabel.isNotBlank()) " ${result.floorTileLabel}" else ""
            val shortLabel = result.floorTileLabel.replace(" × ", "x").replace(" cm", "").replace(" ", "")
            val sizePart = if (shortLabel.isNotBlank()) " $shortLabel" else ""
            val boxUnit = if (result.floorBoxesToBuy == 1) "box" else "boxes"
            val wasteUnit = if (result.floorWasteAllowanceTiles == 1) "tile" else "tiles"
            val wasteText = if (result.floorWasteAllowanceTiles > 0) {
                "Waste allowance (${result.wastePercentage}%): ${result.floorWasteAllowanceTiles} $wasteUnit"
            } else null

            list.add(
                ShoppingItemData(
                    title = "Floor tiles$labelPart",
                    quantityFormatted = "${result.floorBoxesToBuy} $boxUnit",
                    detailsFormatted = "${result.floorTilesPerBoxCount} per box (${result.totalFloorTilesNeeded} tiles total, incl. ${result.floorWasteAllowanceTiles} waste tiles)",
                    plainTextLine = "Floor tiles$sizePart: ${result.floorBoxesToBuy} $boxUnit (${result.floorTilesPerBoxCount} per box)",
                    icon = Icons.Default.GridView,
                    testTag = "buy_item_floor_tiles",
                    wasteAllowanceText = wasteText
                )
            )
        }

        // 2. Wall tiles: boxes (only if wall tiles are on)
        if (result.hasWallTiles && result.wallBoxesToBuy > 0) {
            val labelPart = if (result.wallTileLabel.isNotBlank()) " ${result.wallTileLabel}" else ""
            val boxUnit = if (result.wallBoxesToBuy == 1) "box" else "boxes"
            val wallWasteUnit = if (result.wallWasteAllowanceTiles == 1) "tile" else "tiles"
            val wallWasteText = if (result.wallWasteAllowanceTiles > 0) {
                "Wall waste allowance (${result.wastePercentage}%): ${result.wallWasteAllowanceTiles} $wallWasteUnit"
            } else null

            list.add(
                ShoppingItemData(
                    title = "Wall tiles$labelPart",
                    quantityFormatted = "${result.wallBoxesToBuy} $boxUnit",
                    detailsFormatted = "${result.wallTilesPerBoxCount} per box (${result.wallTilesNeeded} tiles total, incl. ${result.wallWasteAllowanceTiles} waste tiles)",
                    plainTextLine = "Wall tiles$labelPart: ${result.wallBoxesToBuy} $boxUnit (${result.wallTilesPerBoxCount} per box)",
                    icon = Icons.Default.Window,
                    testTag = "buy_item_wall_tiles",
                    wasteAllowanceText = wallWasteText
                )
            )
        }

        // 3. Tile adhesive: 20 kg bags
        if (result.materials.totalAdhesiveBags > 0) {
            val bagUnit = if (result.materials.totalAdhesiveBags == 1) "bag" else "bags"
            val details = if (result.materials.wallAndSkirtingAreaM2 > 0.0) {
                val wallLabel = if (result.materials.wallAreaM2 > 0.0 && result.materials.skirtingAndStepAreaM2 > 0.0) "skirting, step and wall"
                    else if (result.materials.wallAreaM2 > 0.0) "wall"
                    else "skirting and step"
                "20 kg bags (Floor %.2f + %s %.2f = %.2f)".format(
                    java.util.Locale.US,
                    result.materials.floorAdhesiveBagsRaw,
                    wallLabel,
                    result.materials.wallAdhesiveBagsRaw,
                    result.materials.totalAdhesiveBagsRaw
                )
            } else {
                "20 kg cement-based bags (Floor: %.2f bags)".format(java.util.Locale.US, result.materials.floorAdhesiveBagsRaw)
            }
            list.add(
                ShoppingItemData(
                    title = "Tile adhesive",
                    quantityFormatted = "${result.materials.totalAdhesiveBags} $bagUnit",
                    detailsFormatted = details,
                    plainTextLine = "Tile adhesive: ${result.materials.totalAdhesiveBags} 20 kg bags",
                    icon = Icons.Default.Layers,
                    testTag = "buy_item_adhesive"
                )
            )
        }

        // 4. Tile grout: bags (with bag size)
        if (result.materials.groutBagsToBuy > 0) {
            val bagUnit = if (result.materials.groutBagsToBuy == 1) "bag" else "bags"
            val bagSizeKg = if (result.materials.groutBagSizeKg % 1.0 == 0.0) {
                result.materials.groutBagSizeKg.toInt().toString()
            } else {
                "%.1f".format(result.materials.groutBagSizeKg)
            }
            list.add(
                ShoppingItemData(
                    title = "Tile grout",
                    quantityFormatted = "${result.materials.groutBagsToBuy} $bagUnit",
                    detailsFormatted = "$bagSizeKg kg bags (Total: %.2f kg needed)".format(result.materials.totalGroutKg),
                    plainTextLine = "Tile grout: ${result.materials.groutBagsToBuy} $bagUnit ($bagSizeKg kg bag)",
                    icon = Icons.Default.Grain,
                    testTag = "buy_item_grout"
                )
            )
        }

        // 5. Bonding liquid: amount in bottles or jugs
        if (result.materials.totalBondingLiquidLiters > 0.001) {
            list.add(
                ShoppingItemData(
                    title = "Bonding liquid",
                    quantityFormatted = result.materials.bondingLiquidDisplayText,
                    detailsFormatted = "${result.materials.bondingMode.shortLabel} (%.2f L total)".format(result.materials.totalBondingLiquidLiters),
                    plainTextLine = "Bonding liquid: ${result.materials.bondingLiquidDisplayText}",
                    icon = Icons.Default.WaterDrop,
                    testTag = "buy_item_bonding_liquid"
                )
            )
        }

        // 6. Tile spacers: packs (with joint size)
        if (result.materials.spacerPacksToBuy > 0) {
            val packUnit = if (result.materials.spacerPacksToBuy == 1) "pack" else "packs"
            val jointSizeStr = if (result.materials.jointWidthMm % 1.0 == 0.0) {
                result.materials.jointWidthMm.toInt().toString()
            } else {
                "%.1f".format(result.materials.jointWidthMm)
            }
            list.add(
                ShoppingItemData(
                    title = "Tile spacers",
                    quantityFormatted = "${result.materials.spacerPacksToBuy} $packUnit",
                    detailsFormatted = "$jointSizeStr mm joint size (${result.materials.spacerPackSize} per pack)",
                    plainTextLine = "Tile spacers: ${result.materials.spacerPacksToBuy} $packUnit ($jointSizeStr mm)",
                    icon = Icons.Default.AllInclusive,
                    testTag = "buy_item_spacers"
                )
            )
        }

        // 7. Plastic trims: pieces (with piece length)
        if (result.trims.plasticPiecesNeeded > 0) {
            val pieceUnit = if (result.trims.plasticPiecesNeeded == 1) "piece" else "pieces"
            val pieceLenStr = if (result.trims.pieceLengthM % 1.0 == 0.0) {
                result.trims.pieceLengthM.toInt().toString()
            } else {
                "%.1f".format(result.trims.pieceLengthM)
            }
            list.add(
                ShoppingItemData(
                    title = "Plastic trims",
                    quantityFormatted = "${result.trims.plasticPiecesNeeded} $pieceUnit ($pieceLenStr m each)",
                    detailsFormatted = "$pieceLenStr m piece length (Run: %.2f m + %d%% allowance)".format(
                        result.trims.plasticTotalRunLengthM,
                        result.trims.allowancePercent
                    ),
                    plainTextLine = "Plastic trims: ${result.trims.plasticPiecesNeeded} $pieceUnit ($pieceLenStr m each)",
                    icon = Icons.Default.Straighten,
                    testTag = "buy_item_plastic_trims"
                )
            )
        }

        // 8. Metal trims: pieces (with piece length)
        if (result.trims.metalPiecesNeeded > 0) {
            val pieceUnit = if (result.trims.metalPiecesNeeded == 1) "piece" else "pieces"
            val pieceLenStr = if (result.trims.pieceLengthM % 1.0 == 0.0) {
                result.trims.pieceLengthM.toInt().toString()
            } else {
                "%.1f".format(result.trims.pieceLengthM)
            }
            list.add(
                ShoppingItemData(
                    title = "Metal trims",
                    quantityFormatted = "${result.trims.metalPiecesNeeded} $pieceUnit ($pieceLenStr m each)",
                    detailsFormatted = "$pieceLenStr m piece length (Run: %.2f m + %d%% allowance)".format(
                        result.trims.metalTotalRunLengthM,
                        result.trims.allowancePercent
                    ),
                    plainTextLine = "Metal trims: ${result.trims.metalPiecesNeeded} $pieceUnit ($pieceLenStr m each)",
                    icon = Icons.Default.Architecture,
                    testTag = "buy_item_metal_trims"
                )
            )
        }

        list
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("materials_to_buy_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Header with title and Copy List button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.width(40.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = "MATERIALS TO BUY",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Main Summary Shopping List",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Copy List button
                Button(
                    onClick = {
                        val headerLines = mutableListOf<String>()
                        if (jobName.isNotBlank()) headerLines.add("Job: $jobName")
                        if (clientName.isNotBlank()) headerLines.add("Client: $clientName")
                        if (builderName.isNotBlank() || builderPhone.isNotBlank()) {
                            val phonePart = if (builderPhone.isNotBlank()) " ($builderPhone)" else ""
                            headerLines.add("Contractor: $builderName$phonePart")
                        }
                        if (headerLines.isNotEmpty()) {
                            headerLines.add("----------------------------------------")
                        }
                        val plainText = (headerLines + shoppingItems.map { it.plainTextLine }).joinToString("\n")
                        clipboardManager.setText(AnnotatedString(plainText))
                        coroutineScope.launch {
                            showCopiedConfirmation = true
                            delay(2500)
                            showCopiedConfirmation = false
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("copy_list_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy list",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Copy list",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // Copied Confirmation Banner
            AnimatedVisibility(
                visible = showCopiedConfirmation,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .testTag("copied_confirmation_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Copied confirmation",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Copied! Shopping list copied to clipboard.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Itemized List (0-quantity items are already hidden)
            if (shoppingItems.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No materials needed yet. Enter space dimensions above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    shoppingItems.forEachIndexed { index, item ->
                        ShoppingItemRow(item = item)
                        if (index < shoppingItems.lastIndex) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(modifier = Modifier.width(48.dp))
                                HorizontalDivider(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    thickness = 0.5.dp
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
private fun ShoppingItemRow(
    item: ShoppingItemData
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .testTag(item.testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon on the left in a fixed 40 px column
        Box(
            modifier = Modifier.width(40.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(34.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // The item name and its detail line in the middle
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, end = 8.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = item.detailsFormatted,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (item.wasteAllowanceText != null) {
                Text(
                    text = item.wasteAllowanceText,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // The quantity on the right in its own column, vertically centered
        Box(
            modifier = Modifier.wrapContentWidth(Alignment.End),
            contentAlignment = Alignment.CenterEnd
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = item.quantityFormatted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
