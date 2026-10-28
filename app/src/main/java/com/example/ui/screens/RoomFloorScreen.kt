package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RoomFloorState
import com.example.model.SkirtingPreset
import com.example.model.UnitSystem

@Composable
fun RoomFloorScreen(
    roomState: RoomFloorState,
    unit: UnitSystem,
    onUpdate: ((RoomFloorState) -> RoomFloorState) -> Unit,
    onAddDoorway: () -> Unit,
    onRemoveDoorway: (Int) -> Unit,
    onUpdateDoorway: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("room_inputs_card"),
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
                    imageVector = Icons.Default.MeetingRoom,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "ROOM FLOOR DIMENSIONS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Length and Width inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = roomState.length,
                    onValueChange = { len -> onUpdate { it.copy(length = len) } },
                    label = { Text("Length (${unit.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("room_length_input"),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                )

                OutlinedTextField(
                    value = roomState.width,
                    onValueChange = { wid -> onUpdate { it.copy(width = wid) } },
                    label = { Text("Width (${unit.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("room_width_input"),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Skirting Section
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
                        text = "Skirting",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    com.example.ui.components.HelpTooltipButton(
                        title = "Skirting",
                        explanation = "The tile strip along the bottom of the wall.",
                        testTag = "room_skirting_help"
                    )
                }
                Switch(
                    checked = roomState.hasSkirting,
                    onCheckedChange = { checked -> onUpdate { it.copy(hasSkirting = checked) } },
                    modifier = Modifier.testTag("skirting_toggle")
                )
            }
            Text(
                text = "Cut from your floor tiles along the bottom of the walls.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (roomState.hasSkirting) {
                Spacer(modifier = Modifier.height(12.dp))
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
                        val isSelected = roomState.skirtingPreset == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = { onUpdate { it.copy(skirtingPreset = preset) } },
                            label = { Text(preset.label) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("skirting_preset_${preset.name.lowercase()}")
                        )
                    }
                }

                if (roomState.skirtingPreset == SkirtingPreset.CUSTOM) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = roomState.customSkirtingHeight,
                        onValueChange = { h -> onUpdate { it.copy(customSkirtingHeight = h) } },
                        label = { Text("Custom skirting height ($heightUnit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_skirting_height_input"),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Doors Section (A5, B3)
            com.example.ui.components.DoorsSection(
                doorWidths = roomState.doorwayWidths,
                unit = unit,
                onUpdateDoors = { newDoors ->
                    onUpdate { it.copy(doorwayWidths = newDoors) }
                }
            )
        }
    }
}
