package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UnitSystem

@Composable
fun DoorsSection(
    doorWidths: List<String>,
    unit: UnitSystem,
    onUpdateDoors: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultDoorWidth = if (unit == UnitSystem.FEET) "3.0" else "0.9"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("doors_section_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
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
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Doors",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    HelpTooltipButton(
                        title = "Doors",
                        explanation = "Doors do not reduce the floor tile count because tile runs through the doorway. Total door widths are subtracted from skirting only.",
                        testTag = "doors_help_tooltip"
                    )
                }

                // Stepper: How many doors? (0 or more)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "How many doors?",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Minus button
                    FilledTonalIconButton(
                        onClick = {
                            if (doorWidths.isNotEmpty()) {
                                onUpdateDoors(doorWidths.dropLast(1))
                            }
                        },
                        enabled = doorWidths.isNotEmpty(),
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("doors_count_minus_button")
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease door count", modifier = Modifier.size(18.dp))
                    }

                    Text(
                        text = doorWidths.size.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .testTag("doors_count_display")
                    )

                    // Plus button
                    FilledTonalIconButton(
                        onClick = {
                            onUpdateDoors(doorWidths + defaultDoorWidth)
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("doors_count_plus_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase door count", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Total door width is subtracted from skirting. Floor tiles remain unchanged.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (doorWidths.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                doorWidths.forEachIndexed { index, widthStr ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = widthStr,
                            onValueChange = { newWidth ->
                                val updated = doorWidths.toMutableList()
                                updated[index] = newWidth
                                onUpdateDoors(updated)
                            },
                            label = { Text("Door ${index + 1} width (${unit.symbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("door_width_input_$index"),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                        )

                        IconButton(
                            onClick = {
                                val updated = doorWidths.filterIndexed { i, _ -> i != index }
                                onUpdateDoors(updated)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("remove_door_button_$index")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete door ${index + 1}",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
