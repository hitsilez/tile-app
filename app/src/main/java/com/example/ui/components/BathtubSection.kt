package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*

@Composable
fun BathtubSection(
    bathroomState: BathroomState,
    unit: UnitSystem,
    onUpdate: ((BathroomState) -> BathroomState) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bathtub_section_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Main Switch: "Is there a built-in bathtub to tile?"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bathtub,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Is there a built-in bathtub to tile?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tile the sides (apron) and top edge of the tub",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = bathroomState.hasBathtub,
                    onCheckedChange = { checked -> onUpdate { it.copy(hasBathtub = checked) } },
                    modifier = Modifier.testTag("bathtub_toggle")
                )
            }

            AnimatedVisibility(visible = bathroomState.hasBathtub) {
                Column(modifier = Modifier.padding(top = 14.dp)) {

                    // Dimensions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = bathroomState.bathtubLength,
                            onValueChange = { len -> onUpdate { it.copy(bathtubLength = len) } },
                            label = { Text("Length of bathtub (${unit.symbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bathtub_length_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = bathroomState.bathtubWidth,
                            onValueChange = { wid -> onUpdate { it.copy(bathtubWidth = wid) } },
                            label = { Text("Width of bathtub (${unit.symbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bathtub_width_input"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = bathroomState.bathtubHeight,
                        onValueChange = { h -> onUpdate { it.copy(bathtubHeight = h) } },
                        label = { Text("Height of the tiled side (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bathtub_height_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Shape Selector with Picture Buttons
                    Text(
                        text = "What shape is the bathtub area?",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BathtubShape.values().forEach { shape ->
                            BathtubShapeButton(
                                shape = shape,
                                isSelected = bathroomState.bathtubShape == shape,
                                lEnd = bathroomState.bathtubLEnd,
                                onClick = { onUpdate { it.copy(bathtubShape = shape) } }
                            )
                        }
                    }

                    // L-shape sub-question: "Which end can be seen?"
                    if (bathroomState.bathtubShape == BathtubShape.L_SHAPE) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Which end can be seen?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    BathtubLEnd.values().forEach { end ->
                                        val isSel = bathroomState.bathtubLEnd == end
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { onUpdate { it.copy(bathtubLEnd = end) } },
                                            label = { Text(end.label) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .testTag("bathtub_l_${end.name.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Custom sub-toggles
                    if (bathroomState.bathtubShape == BathtubShape.CUSTOM) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Choose which sides need tiles:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                SideToggleRow("Front side", bathroomState.bathtubFrontTiled, "bathtub_custom_front_toggle") {
                                    onUpdate { it.copy(bathtubFrontTiled = it.bathtubFrontTiled.not()) }
                                }
                                SideToggleRow("Back side", bathroomState.bathtubBackTiled, "bathtub_custom_back_toggle") {
                                    onUpdate { it.copy(bathtubBackTiled = it.bathtubBackTiled.not()) }
                                }
                                SideToggleRow("Left end", bathroomState.bathtubLeftTiled, "bathtub_custom_left_toggle") {
                                    onUpdate { it.copy(bathtubLeftTiled = it.bathtubLeftTiled.not()) }
                                }
                                SideToggleRow("Right end", bathroomState.bathtubRightTiled, "bathtub_custom_right_toggle") {
                                    onUpdate { it.copy(bathtubRightTiled = it.bathtubRightTiled.not()) }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Top Flat Edge Switch & Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tile the top (the flat edge) too?",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Horizontal ledge/shelf around the bathtub rim",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = bathroomState.tileBathtubTop,
                            onCheckedChange = { checked -> onUpdate { it.copy(tileBathtubTop = checked) } },
                            modifier = Modifier.testTag("tile_bathtub_top_toggle")
                        )
                    }

                    if (bathroomState.tileBathtubTop) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bathroomState.bathtubTopWidthCm,
                            onValueChange = { w -> onUpdate { it.copy(bathtubTopWidthCm = w) } },
                            label = { Text("Width of the tiled top (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bathtub_top_width_input"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Top-down diagram of bathtub with labeled sides and highlighted tiled sides
                    Text(
                        text = "TOP-DOWN VIEW",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    BathtubTopDownDiagram(
                        bathroomState = bathroomState,
                        unit = unit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bathtub_top_down_diagram")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Length around the bathtub: shown on its own line
                    val lengthAroundM = bathroomState.bathtubLengthAroundM(unit)
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bathtub_length_around_display")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Length around the bathtub: ${TileCalculatorEngine.formatTrimLen(lengthAroundM)} m",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val cornerCount = bathroomState.bathtubCornerCount()
                            if (cornerCount > 0) {
                                Text(
                                    text = "$cornerCount corner${if (cornerCount == 1) "" else "s"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Optional Trims controls directly in the bathtub section
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "BATHTUB STRIPS (TRIMS)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Top edge strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Strip on top edge of the bathtub",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Run: ${TileCalculatorEngine.formatTrimLen(lengthAroundM)} m",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = bathroomState.bathtubTopEdgeTrimEnabled,
                            onCheckedChange = { checked -> onUpdate { it.copy(bathtubTopEdgeTrimEnabled = checked) } },
                            modifier = Modifier.testTag("bathtub_top_edge_trim_toggle")
                        )
                    }

                    if (bathroomState.bathtubTopEdgeTrimEnabled) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TrimMaterialSelector(
                            selected = bathroomState.bathtubTopEdgeTrimMaterial,
                            onSelect = { mat -> onUpdate { it.copy(bathtubTopEdgeTrimMaterial = mat) } },
                            testTagPrefix = "bath_section_top_edge_mat"
                        )
                    }

                    // Outside corner strips
                    val cornerCount = bathroomState.bathtubCornerCount()
                    if (cornerCount > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val heightM = bathroomState.bathtubHeightM()
                        val cornerRunM = cornerCount * heightM
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Strips on outside corners",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$cornerCount corner${if (cornerCount == 1) "" else "s"} × ${TileCalculatorEngine.formatTrimLen(heightM)} m = ${TileCalculatorEngine.formatTrimLen(cornerRunM)} m",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = bathroomState.bathtubCornerTrimEnabled,
                                onCheckedChange = { checked -> onUpdate { it.copy(bathtubCornerTrimEnabled = checked) } },
                                modifier = Modifier.testTag("bathtub_corner_trim_toggle")
                            )
                        }

                        if (bathroomState.bathtubCornerTrimEnabled) {
                            Spacer(modifier = Modifier.height(4.dp))
                            TrimMaterialSelector(
                                selected = bathroomState.bathtubCornerTrimMaterial,
                                onSelect = { mat -> onUpdate { it.copy(bathtubCornerTrimMaterial = mat) } },
                                testTagPrefix = "bath_section_corners_mat"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SideToggleRow(
    label: String,
    checked: Boolean,
    testTag: String,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun BathtubShapeButton(
    shape: BathtubShape,
    isSelected: Boolean,
    lEnd: BathtubLEnd,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary))
        } else {
            CardDefaults.outlinedCardBorder()
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bathtub_shape_${shape.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Small top-down thumbnail
            BathtubShapeThumbnail(
                shape = shape,
                lEnd = lEnd,
                isSelected = isSelected,
                modifier = Modifier.size(width = 44.dp, height = 28.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shape.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BathtubShapeThumbnail(
    shape: BathtubShape,
    lEnd: BathtubLEnd,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val wallColor = Color(0xFF9E9E9E)
    val tubInteriorColor = if (isSelected) Color(0xFFE0F2FE) else Color(0xFFF1F5F9)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Background / tub interior
        drawRoundRect(
            color = tubInteriorColor,
            topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
            size = Size(w - 4.dp.toPx(), h - 4.dp.toPx()),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )

        // Determine which sides are tiled in this thumbnail
        val isFrontTiled = true
        val isBackTiled = shape == BathtubShape.ALL_SIDES
        val isLeftTiled = shape == BathtubShape.C_SHAPE || shape == BathtubShape.ALL_SIDES || (shape == BathtubShape.L_SHAPE && lEnd == BathtubLEnd.LEFT)
        val isRightTiled = shape == BathtubShape.C_SHAPE || shape == BathtubShape.ALL_SIDES || (shape == BathtubShape.L_SHAPE && lEnd == BathtubLEnd.RIGHT)

        val strokeWidthTiled = 3.dp.toPx()
        val strokeWidthWall = 1.5.dp.toPx()

        // Back (top)
        drawLine(
            color = if (isBackTiled) primaryColor else wallColor,
            start = Offset(4.dp.toPx(), 2.dp.toPx()),
            end = Offset(w - 4.dp.toPx(), 2.dp.toPx()),
            strokeWidth = if (isBackTiled) strokeWidthTiled else strokeWidthWall,
            cap = StrokeCap.Round
        )

        // Front (bottom)
        drawLine(
            color = if (isFrontTiled) primaryColor else wallColor,
            start = Offset(4.dp.toPx(), h - 2.dp.toPx()),
            end = Offset(w - 4.dp.toPx(), h - 2.dp.toPx()),
            strokeWidth = if (isFrontTiled) strokeWidthTiled else strokeWidthWall,
            cap = StrokeCap.Round
        )

        // Left
        drawLine(
            color = if (isLeftTiled) primaryColor else wallColor,
            start = Offset(2.dp.toPx(), 4.dp.toPx()),
            end = Offset(2.dp.toPx(), h - 4.dp.toPx()),
            strokeWidth = if (isLeftTiled) strokeWidthTiled else strokeWidthWall,
            cap = StrokeCap.Round
        )

        // Right
        drawLine(
            color = if (isRightTiled) primaryColor else wallColor,
            start = Offset(w - 2.dp.toPx(), 4.dp.toPx()),
            end = Offset(w - 2.dp.toPx(), h - 4.dp.toPx()),
            strokeWidth = if (isRightTiled) strokeWidthTiled else strokeWidthWall,
            cap = StrokeCap.Round
        )

        // Drain circle
        drawCircle(
            color = Color(0xFF64748B),
            radius = 1.5.dp.toPx(),
            center = Offset(w * 0.75f, h * 0.5f)
        )
    }
}

@Composable
fun BathtubTopDownDiagram(
    bathroomState: BathroomState,
    unit: UnitSystem,
    modifier: Modifier = Modifier
) {
    val frontOn = bathroomState.isBathtubFrontOn()
    val backOn = bathroomState.isBathtubBackOn()
    val leftOn = bathroomState.isBathtubLeftOn()
    val rightOn = bathroomState.isBathtubRightOn()

    val lenStr = bathroomState.bathtubLength.ifBlank { "0" } + " " + unit.symbol
    val widStr = bathroomState.bathtubWidth.ifBlank { "0" } + " " + unit.symbol

    val primaryColor = MaterialTheme.colorScheme.primary
    val wallColor = Color(0xFF9E9E9E)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top: Back side label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Back ($lenStr)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (backOn) primaryColor else wallColor
                )
                Text(
                    text = if (backOn) "• Tiled" else "• Against wall",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (backOn) primaryColor else wallColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Middle: Left label + Bathtub Canvas + Right label
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side label
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(54.dp)
                ) {
                    Text(
                        text = "Left",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (leftOn) primaryColor else wallColor
                    )
                    Text(
                        text = widStr,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (leftOn) "Tiled" else "Wall",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = if (leftOn) primaryColor else wallColor
                    )
                }

                // Center Bathtub Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(90.dp)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        val padX = 8.dp.toPx()
                        val padY = 8.dp.toPx()
                        val tubW = w - (padX * 2)
                        val tubH = h - (padY * 2)

                        // 1. Tub outer rim / flat ledge
                        drawRoundRect(
                            color = Color(0xFFE2E8F0),
                            topLeft = Offset(padX, padY),
                            size = Size(tubW, tubH),
                            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                        )

                        // 2. Tub basin interior (water color)
                        val basinInset = 8.dp.toPx()
                        drawRoundRect(
                            color = Color(0xFFBAE6FD),
                            topLeft = Offset(padX + basinInset, padY + basinInset),
                            size = Size(tubW - (basinInset * 2), tubH - (basinInset * 2)),
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                        )

                        // Inner basin border
                        drawRoundRect(
                            color = Color(0xFF7DD3FC),
                            topLeft = Offset(padX + basinInset, padY + basinInset),
                            size = Size(tubW - (basinInset * 2), tubH - (basinInset * 2)),
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx())
                        )

                        // Drain
                        drawCircle(
                            color = Color(0xFF475569),
                            radius = 3.dp.toPx(),
                            center = Offset(padX + tubW * 0.8f, padY + tubH * 0.5f)
                        )

                        // Faucet on back rim
                        drawCircle(
                            color = Color(0xFF94A3B8),
                            radius = 2.5.dp.toPx(),
                            center = Offset(padX + tubW * 0.8f, padY + 4.dp.toPx())
                        )

                        // 3. Highlighted outer sides
                        val strokeTiled = 5.dp.toPx()
                        val strokeWall = 2.dp.toPx()

                        // Back side (top edge)
                        drawLine(
                            color = if (backOn) primaryColor else wallColor,
                            start = Offset(padX + 6.dp.toPx(), padY),
                            end = Offset(padX + tubW - 6.dp.toPx(), padY),
                            strokeWidth = if (backOn) strokeTiled else strokeWall,
                            cap = StrokeCap.Round
                        )

                        // Front side (bottom edge)
                        drawLine(
                            color = if (frontOn) primaryColor else wallColor,
                            start = Offset(padX + 6.dp.toPx(), padY + tubH),
                            end = Offset(padX + tubW - 6.dp.toPx(), padY + tubH),
                            strokeWidth = if (frontOn) strokeTiled else strokeWall,
                            cap = StrokeCap.Round
                        )

                        // Left side (left edge)
                        drawLine(
                            color = if (leftOn) primaryColor else wallColor,
                            start = Offset(padX, padY + 6.dp.toPx()),
                            end = Offset(padX, padY + tubH - 6.dp.toPx()),
                            strokeWidth = if (leftOn) strokeTiled else strokeWall,
                            cap = StrokeCap.Round
                        )

                        // Right side (right edge)
                        drawLine(
                            color = if (rightOn) primaryColor else wallColor,
                            start = Offset(padX + tubW, padY + 6.dp.toPx()),
                            end = Offset(padX + tubW, padY + tubH - 6.dp.toPx()),
                            strokeWidth = if (rightOn) strokeTiled else strokeWall,
                            cap = StrokeCap.Round
                        )

                        // Corner indicator dots where 2 tiled sides meet
                        val cornerRadiusPx = 4.dp.toPx()
                        val cornerColor = Color(0xFFD97706) // amber corner strip marker

                        if (frontOn && leftOn) {
                            drawCircle(color = cornerColor, radius = cornerRadiusPx, center = Offset(padX, padY + tubH))
                        }
                        if (frontOn && rightOn) {
                            drawCircle(color = cornerColor, radius = cornerRadiusPx, center = Offset(padX + tubW, padY + tubH))
                        }
                        if (backOn && leftOn) {
                            drawCircle(color = cornerColor, radius = cornerRadiusPx, center = Offset(padX, padY))
                        }
                        if (backOn && rightOn) {
                            drawCircle(color = cornerColor, radius = cornerRadiusPx, center = Offset(padX + tubW, padY))
                        }
                    }
                }

                // Right side label
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(54.dp)
                ) {
                    Text(
                        text = "Right",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (rightOn) primaryColor else wallColor
                    )
                    Text(
                        text = widStr,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (rightOn) "Tiled" else "Wall",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = if (rightOn) primaryColor else wallColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom: Front side label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Front ($lenStr)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (frontOn) primaryColor else wallColor
                )
                Text(
                    text = if (frontOn) "• Tiled" else "• Untiled",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (frontOn) primaryColor else wallColor
                )
            }

            // Legend
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 12.dp, height = 4.dp)
                            .background(primaryColor, RoundedCornerShape(2.dp))
                    )
                    Text(text = "Tiled side", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 12.dp, height = 4.dp)
                            .background(wallColor, RoundedCornerShape(2.dp))
                    )
                    Text(text = "Against wall", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val corners = bathroomState.bathtubCornerCount()
                if (corners > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFFD97706), CircleShape)
                        )
                        Text(text = "$corners corner strip${if (corners == 1) "" else "s"}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
