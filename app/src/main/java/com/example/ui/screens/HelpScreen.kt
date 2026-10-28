package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class HelpSectionData(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val content: List<String>,
    val bullets: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val sections = remember {
        listOf(
            HelpSectionData(
                id = "section_1",
                title = "1. What this app does",
                icon = Icons.Default.HelpOutline,
                content = listOf(
                    "Tile Calculator helps you work out what to buy for a tiling job. You measure the space, pick your tile, and the app tells you how many boxes of tiles, bags of adhesive and grout, spacers, bonding liquid and edge trims you need."
                )
            ),
            HelpSectionData(
                id = "section_2",
                title = "2. How to use it, in 3 steps",
                icon = Icons.Default.FormatListNumbered,
                content = listOf(
                    "1. Rooms and areas: add each room, veranda or bathroom and enter its measurements.",
                    "2. Choose tiles: pick the tile size, how many tiles are in a box, and how much extra to allow for cutting and breakage.",
                    "3. What to buy: see your shopping list. Tap 'How was this worked out?' to see the numbers."
                )
            ),
            HelpSectionData(
                id = "section_3",
                title = "3. How to measure",
                icon = Icons.Default.Straighten,
                content = listOf(
                    "Use a tape measure. Use one unit for the whole job (meters, centimeters or feet) and set it at the top of the job. Heights such as skirting and steps are entered in centimeters."
                ),
                bullets = listOf(
                    "Room floor: measure the length and width at floor level, wall to wall. If the room is L-shaped, split it into two rectangles and add each one as its own area.",
                    "Doors: count the doors and measure the width of each one.",
                    "Veranda: measure the length and width. For each side, say whether it is against a wall or an open edge with a step. For step sides, measure the height of the step. If there is a door, say which side it is on, how wide it is, and how tall the tile is at the door.",
                    "Bathroom: measure the floor, then the width and height of each wall you will tile. Measure each window and door to take off, and the height and width of the bath surround."
                )
            ),
            HelpSectionData(
                id = "section_4",
                title = "4. Skirting, steps and trims",
                icon = Icons.Default.HorizontalRule,
                content = listOf(
                    "Skirting is the tile strip along the bottom of a wall. The app assumes it is cut from your floor tiles and adds those tiles to your order. A step is the open edge of a veranda where the floor drops down. Its face is tiled too. Trims are plastic or metal strips that finish an exposed tile edge. Plastic is common indoors and metal on veranda step edges."
                )
            ),
            HelpSectionData(
                id = "section_5",
                title = "5. The adhesive bed",
                icon = Icons.Default.Layers,
                content = listOf(
                    "On uneven floors the adhesive layer is thicker, so you use more adhesive. Lay a straight board on the floor and look at the gap underneath. A small gap means a level floor (5 mm), a bigger gap means uneven (8 mm) and a very big gap means very uneven (12 mm). Pick the one that fits."
                )
            ),
            HelpSectionData(
                id = "section_6",
                title = "6. Where to find the tile details",
                icon = Icons.Default.GridView,
                content = listOf(
                    "The tile size, the number of tiles per box and the tile thickness are printed on the box or on the shop's label. The joint size is your choice. Most jobs use 2 to 5 mm."
                )
            ),
            HelpSectionData(
                id = "section_7",
                title = "7. Extra tiles and spare tiles",
                icon = Icons.Default.Percent,
                content = listOf(
                    "The extra for waste covers edge cuts and tiles broken on site. 10% is standard; use 15% for diagonal patterns or tricky shapes. Spare tiles are the full tiles left over after rounding up to whole boxes. You keep them for future repairs."
                )
            ),
            HelpSectionData(
                id = "section_8",
                title = "8. Materials and trims settings",
                icon = Icons.Default.Tune,
                content = listOf(
                    "The app comes with standard settings for adhesive, grout, spacers, bonding liquid and trims that work for most jobs. You only need to open that section if your tile shop sells different bag sizes or you want to turn a material off."
                )
            ),
            HelpSectionData(
                id = "section_9",
                title = "9. Quotations: how they work and how to send them",
                icon = Icons.Default.Description,
                content = listOf(
                    "Tap 'Quotation' at the top of the job to see your quote. Fill in your builder details and prices if you want them shown. Tap 'Share' to send the quote by WhatsApp, email or any messaging app, or copy it to the clipboard. The quote can also be printed."
                )
            )
        )
    }

    var expandedSectionIds by remember { mutableStateOf(setOf("section_1")) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("help_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Text(
                        text = "How it works",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tap any topic below for quick answers and practical measuring tips in plain English.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            items(sections.size, key = { sections[it].id }) { index ->
                val section = sections[index]
                val isExpanded = expandedSectionIds.contains(section.id)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("help_section_${section.id}")
                        .clickable {
                            expandedSectionIds = if (isExpanded) {
                                expandedSectionIds - section.id
                            } else {
                                expandedSectionIds + section.id
                            }
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isExpanded) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        }
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = section.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = section.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))

                                section.content.forEach { paragraph ->
                                    Text(
                                        text = paragraph,
                                        style = MaterialTheme.typography.bodyMedium,
                                        lineHeight = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (section.bullets.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    section.bullets.forEach { bullet ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "•",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = bullet,
                                                style = MaterialTheme.typography.bodyMedium,
                                                lineHeight = 19.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
