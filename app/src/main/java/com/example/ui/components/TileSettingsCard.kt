package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FloorTileSettingsState
import com.example.model.TilePreset
import com.example.model.UnitSystem

@Composable
fun TileSettingsCard(
    settings: FloorTileSettingsState,
    unit: UnitSystem,
    onUpdate: ((FloorTileSettingsState) -> FloorTileSettingsState) -> Unit,
    title: String = "TILE SETTINGS (FLOOR)",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tile_settings_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Tile Size Picker
            Text(
                text = "Tile Size (cm)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Presets grid/flow
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
                        PresetTileChip(
                            preset = preset,
                            isSelected = settings.preset == preset,
                            modifier = Modifier.weight(1f),
                            onClick = { onUpdate { it.copy(preset = preset) } }
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
                        PresetTileChip(
                            preset = preset,
                            isSelected = settings.preset == preset,
                            modifier = Modifier.weight(1f),
                            onClick = { onUpdate { it.copy(preset = preset) } }
                        )
                    }
                }
            }

            // Custom dimensions inputs if CUSTOM
            if (settings.preset == TilePreset.CUSTOM) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = settings.customLength,
                        onValueChange = { len -> onUpdate { it.copy(customLength = len) } },
                        label = { Text("Tile Length (${unit.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_tile_length_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = settings.customWidth,
                        onValueChange = { wid -> onUpdate { it.copy(customWidth = wid) } },
                        label = { Text("Tile Width (${unit.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_tile_width_input"),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Tiles per box
            OutlinedTextField(
                value = settings.tilesPerBox,
                onValueChange = { value ->
                    onUpdate { it.copy(tilesPerBox = value.filter { c -> c.isDigit() }) }
                },
                label = { Text("Tiles per box") },
                supportingText = { Text("Packaging count from the tile box") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tiles_per_box_input"),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Extra for waste picker (plain language)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Extra for waste",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                com.example.ui.components.HelpTooltipButton(
                    title = "Extra for waste",
                    explanation = "Covers cuts and breakage. 10% is recommended for standard rectangular rooms and verandas.",
                    testTag = "tile_waste_help_tooltip"
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(5, 10, 15, 20).forEach { wastePercent ->
                    val isSelected = settings.wastePercentage == wastePercent
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                onUpdate { it.copy(wastePercentage = wastePercent) }
                            }
                            .testTag("waste_option_$wastePercent"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$wastePercent%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetTileChip(
    preset: TilePreset,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag("preset_${preset.name.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = preset.label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}
