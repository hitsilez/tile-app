package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotationScreen(
    job: Job,
    calculationResult: CalculationResult,
    userProfile: UserProfile,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedFormat by remember { mutableStateOf(QuotationFormat.DETAILED_CLIENT) }
    var prices by remember { mutableStateOf(QuotationPrices()) }
    var isPricesExpanded by remember { mutableStateOf(false) }

    // Track user custom edits
    var hasUserEditedText by remember { mutableStateOf(false) }
    var quotationText by remember {
        mutableStateOf(
            QuotationEngine.generateDetailedQuotation(
                job = job,
                result = calculationResult,
                userProfile = userProfile,
                prices = prices
            )
        )
    }

    var showRegenerateConfirmDialog by remember { mutableStateOf(false) }
    var showCopiedBanner by remember { mutableStateOf(false) }

    // Helper to rebuild text
    fun rebuildText(format: QuotationFormat, currentPrices: QuotationPrices) {
        quotationText = when (format) {
            QuotationFormat.DETAILED_CLIENT -> QuotationEngine.generateDetailedQuotation(
                job = job,
                result = calculationResult,
                userProfile = userProfile,
                prices = currentPrices
            )
            QuotationFormat.QUICK_SHOP -> QuotationEngine.generateQuickList(
                job = job,
                result = calculationResult
            )
        }
        hasUserEditedText = false
    }

    // When format or prices change, if user hasn't typed manual edits, keep in sync
    LaunchedEffect(selectedFormat) {
        if (!hasUserEditedText) {
            rebuildText(selectedFormat, prices)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("quotation_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Job"
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Quotation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = job.name.ifBlank { "Untitled Job" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (hasUserEditedText) {
                                showRegenerateConfirmDialog = true
                            } else {
                                rebuildText(selectedFormat, prices)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Quotation regenerated from job")
                                }
                            }
                        },
                        modifier = Modifier.testTag("regenerate_quotation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Regenerate",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Regenerate")
                    }
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp)
        ) {

            // Copied Banner
            if (showCopiedBanner) {
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quotation_copied_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Copied! Quotation text copied to clipboard.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // 1. FORMAT SELECTOR
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quotation_format_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "FORMAT SELECTOR",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedFormat == QuotationFormat.DETAILED_CLIENT,
                                onClick = {
                                    selectedFormat = QuotationFormat.DETAILED_CLIENT
                                    if (!hasUserEditedText) {
                                        rebuildText(QuotationFormat.DETAILED_CLIENT, prices)
                                    }
                                },
                                label = {
                                    Text(
                                        text = "Detailed quotation (for client)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (selectedFormat == QuotationFormat.DETAILED_CLIENT) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (selectedFormat == QuotationFormat.DETAILED_CLIENT) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quotation_format_detailed")
                            )

                            FilterChip(
                                selected = selectedFormat == QuotationFormat.QUICK_SHOP,
                                onClick = {
                                    selectedFormat = QuotationFormat.QUICK_SHOP
                                    if (!hasUserEditedText) {
                                        rebuildText(QuotationFormat.QUICK_SHOP, prices)
                                    }
                                },
                                label = {
                                    Text(
                                        text = "Quick list (for shop)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (selectedFormat == QuotationFormat.QUICK_SHOP) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (selectedFormat == QuotationFormat.QUICK_SHOP) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quotation_format_quick")
                            )
                        }
                    }
                }
            }

            // 2. OPTIONAL PRICES SECTION (Only relevant for Detailed Quotation)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("prices_collapsible_card"),
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
                                    imageVector = Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "Prices (optional)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (QuotationEngine.hasAnyPrices(prices)) "Prices entered" else "All prices start empty",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            TextButton(
                                onClick = { isPricesExpanded = !isPricesExpanded },
                                modifier = Modifier.testTag("prices_expand_button")
                            ) {
                                Text(if (isPricesExpanded) "Collapse" else "Edit Prices")
                                Icon(
                                    imageVector = if (isPricesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        AnimatedVisibility(visible = isPricesExpanded) {
                            Column(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                // Currency selector
                                OutlinedTextField(
                                    value = prices.currency,
                                    onValueChange = { newCur ->
                                        prices = prices.copy(currency = newCur)
                                        if (!hasUserEditedText && selectedFormat == QuotationFormat.DETAILED_CLIENT) {
                                            rebuildText(selectedFormat, prices)
                                        }
                                    },
                                    label = { Text("Currency Symbol") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("price_currency_input"),
                                    singleLine = true,
                                    supportingText = { Text("Default 'P' (Botswana Pula), editable to $, €, £, R, etc.") }
                                )

                                // Floor tiles price
                                if (calculationResult.floorBoxesToBuy > 0) {
                                    PriceInputRow(
                                        title = "Floor tiles (${calculationResult.floorBoxesToBuy} boxes)",
                                        unitLabel = "per box",
                                        currency = prices.currency,
                                        quantity = calculationResult.floorBoxesToBuy,
                                        priceValue = prices.floorTilePricePerBox,
                                        testTag = "price_floor_tiles_input",
                                        onPriceChange = { newPrice ->
                                            prices = prices.copy(floorTilePricePerBox = newPrice)
                                            if (!hasUserEditedText && selectedFormat == QuotationFormat.DETAILED_CLIENT) {
                                                rebuildText(selectedFormat, prices)
                                            }
                                        }
                                    )
                                }

                                // Wall tiles price
                                if (calculationResult.hasWallTiles && calculationResult.wallBoxesToBuy > 0) {
                                    PriceInputRow(
                                        title = "Wall tiles (${calculationResult.wallBoxesToBuy} boxes)",
                                        unitLabel = "per box",
                                        currency = prices.currency,
                                        quantity = calculationResult.wallBoxesToBuy,
                                        testTag = "price_wall_tiles_input",
                                        priceValue = prices.wallTilePricePerBox,
                                        onPriceChange = { newPrice ->
                                            prices = prices.copy(wallTilePricePerBox = newPrice)
                                            if (!hasUserEditedText && selectedFormat == QuotationFormat.DETAILED_CLIENT) {
                                                rebuildText(selectedFormat, prices)
                                            }
                                        }
                                    )
                                }

                                // Tile adhesive price
                                if (calculationResult.materials.totalAdhesiveBags > 0) {
                                    PriceInputRow(
                                        title = "Tile adhesive (${calculationResult.materials.totalAdhesiveBags} bags)",
                                        unitLabel = "per 20 kg bag",
                                        currency = prices.currency,
                                        quantity = calculationResult.materials.totalAdhesiveBags,
                                        testTag = "price_adhesive_input",
                                        priceValue = prices.adhesivePricePerBag,
                                        onPriceChange = { newPrice ->
                                            prices = prices.copy(adhesivePricePerBag = newPrice)
                                            if (!hasUserEditedText && selectedFormat == QuotationFormat.DETAILED_CLIENT) {
                                                rebuildText(selectedFormat, prices)
                                            }
                                        }
                                    )
                                }

                                // Tile grout price
                                if (calculationResult.materials.groutBagsToBuy > 0) {
                                    PriceInputRow(
                                        title = "Tile grout (${calculationResult.materials.groutBagsToBuy} bags)",
                                        unitLabel = "per bag",
                                        currency = prices.currency,
                                        quantity = calculationResult.materials.groutBagsToBuy,
                                        testTag = "price_grout_input",
                                        priceValue = prices.groutPricePerBag,
                                        onPriceChange = { newPrice ->
                                            prices = prices.copy(groutPricePerBag = newPrice)
                                            if (!hasUserEditedText && selectedFormat == QuotationFormat.DETAILED_CLIENT) {
                                                rebuildText(selectedFormat, prices)
                                            }
                                        }
                                    )
                                }

                                // Bonding liquid price
                                if (calculationResult.materials.totalBondingLiquidLiters > 0.001) {
                                    val units = QuotationEngine.getBondingLiquidBottlesOrJugs(calculationResult.materials.totalBondingLiquidLiters)
                                    PriceInputRow(
                                        title = "Bonding liquid ($units units / ${calculationResult.materials.bondingLiquidDisplayText})",
                                        unitLabel = "per bottle/jug",
                                        currency = prices.currency,
                                        quantity = units,
                                        testTag = "price_bonding_liquid_input",
                                        priceValue = prices.bondingLiquidPrice,
                                        onPriceChange = { newPrice ->
                                            prices = prices.copy(bondingLiquidPrice = newPrice)
                                            if (!hasUserEditedText && selectedFormat == QuotationFormat.DETAILED_CLIENT) {
                                                rebuildText(selectedFormat, prices)
                                            }
                                        }
                                    )
                                }

                                // Tile spacers price
                                if (calculationResult.materials.spacerPacksToBuy > 0) {
                                    PriceInputRow(
                                        title = "Tile spacers (${calculationResult.materials.spacerPacksToBuy} packs)",
                                        unitLabel = "per pack",
                                        currency = prices.currency,
                                        quantity = calculationResult.materials.spacerPacksToBuy,
                                        testTag = "price_spacers_input",
                                        priceValue = prices.spacersPricePerPack,
                                        onPriceChange = { newPrice ->
                                            prices = prices.copy(spacersPricePerPack = newPrice)
                                            if (!hasUserEditedText && selectedFormat == QuotationFormat.DETAILED_CLIENT) {
                                                rebuildText(selectedFormat, prices)
                                            }
                                        }
                                    )
                                }

                                // Plastic trims price
                                if (calculationResult.trims.plasticPiecesNeeded > 0) {
                                    PriceInputRow(
                                        title = "Plastic trims (${calculationResult.trims.plasticPiecesNeeded} pieces)",
                                        unitLabel = "per piece",
                                        currency = prices.currency,
                                        quantity = calculationResult.trims.plasticPiecesNeeded,
                                        testTag = "price_plastic_trims_input",
                                        priceValue = prices.plasticTrimPricePerPiece,
                                        onPriceChange = { newPrice ->
                                            prices = prices.copy(plasticTrimPricePerPiece = newPrice)
                                            if (!hasUserEditedText && selectedFormat == QuotationFormat.DETAILED_CLIENT) {
                                                rebuildText(selectedFormat, prices)
                                            }
                                        }
                                    )
                                }

                                // Metal trims price
                                if (calculationResult.trims.metalPiecesNeeded > 0) {
                                    PriceInputRow(
                                        title = "Metal trims (${calculationResult.trims.metalPiecesNeeded} pieces)",
                                        unitLabel = "per piece",
                                        currency = prices.currency,
                                        quantity = calculationResult.trims.metalPiecesNeeded,
                                        testTag = "price_metal_trims_input",
                                        priceValue = prices.metalTrimPricePerPiece,
                                        onPriceChange = { newPrice ->
                                            prices = prices.copy(metalTrimPricePerPiece = newPrice)
                                            if (!hasUserEditedText && selectedFormat == QuotationFormat.DETAILED_CLIENT) {
                                                rebuildText(selectedFormat, prices)
                                            }
                                        }
                                    )
                                }

                                // Optional Labour line
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Text(
                                    text = "Optional Labour",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = prices.labourLabel,
                                        onValueChange = {
                                            prices = prices.copy(labourLabel = it)
                                            if (!hasUserEditedText && selectedFormat == QuotationFormat.DETAILED_CLIENT) {
                                                rebuildText(selectedFormat, prices)
                                            }
                                        },
                                        label = { Text("Labour Description") },
                                        placeholder = { Text("Labour") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("labour_label_input"),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = prices.labourAmount,
                                        onValueChange = {
                                            prices = prices.copy(labourAmount = it)
                                            if (!hasUserEditedText && selectedFormat == QuotationFormat.DETAILED_CLIENT) {
                                                rebuildText(selectedFormat, prices)
                                            }
                                        },
                                        label = { Text("Amount (${prices.currency})") },
                                        placeholder = { Text("0") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("labour_amount_input"),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. EDITABLE MONOSPACE TEXT BOX
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                            Text(
                                text = "QUOTATION TEXT (EDITABLE)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (hasUserEditedText) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        text = "Edited",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = quotationText,
                            onValueChange = {
                                quotationText = it
                                hasUserEditedText = true
                            },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 220.dp, max = 380.dp)
                                .testTag("quotation_text_editor"),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // 4. SHARING BUTTONS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "SHARE QUOTATION",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Row 1: Copy & WhatsApp
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(quotationText))
                                    coroutineScope.launch {
                                        showCopiedBanner = true
                                        delay(2500)
                                        showCopiedBanner = false
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quotation_copy_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy text",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy", fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = {
                                    try {
                                        val encoded = URLEncoder.encode(quotationText, "UTF-8").replace("+", "%20")
                                        val uri = Uri.parse("https://wa.me/?text=$encoded")
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Could not open WhatsApp. Please use Copy instead.")
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quotation_whatsapp_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "WhatsApp",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Row 2: SMS & Email
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val encoded = URLEncoder.encode(quotationText, "UTF-8").replace("+", "%20")
                                        val uri = Uri.parse("sms:?body=$encoded")
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Could not open SMS. Please use Copy instead.")
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quotation_sms_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = "SMS",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SMS", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        val subject = job.name.ifBlank { "Quotation" }
                                        val encSubject = URLEncoder.encode(subject, "UTF-8").replace("+", "%20")
                                        val encBody = URLEncoder.encode(quotationText, "UTF-8").replace("+", "%20")
                                        val uri = Uri.parse("mailto:?subject=$encSubject&body=$encBody")
                                        val intent = Intent(Intent.ACTION_SENDTO, uri)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Could not open Email. Please use Copy instead.")
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quotation_email_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Email", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Regenerate Confirmation Dialog
    if (showRegenerateConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRegenerateConfirmDialog = false },
            title = { Text("Regenerate Quotation?") },
            text = { Text("Rebuilding the quotation will overwrite your manual edits in the text box. Do you want to continue?") },
            confirmButton = {
                Button(
                    onClick = {
                        rebuildText(selectedFormat, prices)
                        showRegenerateConfirmDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Quotation rebuilt from job")
                        }
                    }
                ) {
                    Text("Overwrite & Regenerate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PriceInputRow(
    title: String,
    unitLabel: String,
    currency: String,
    quantity: Int,
    priceValue: String,
    testTag: String,
    onPriceChange: (String) -> Unit
) {
    val unitPriceNum = priceValue.toDoubleOrNull()
    val lineTotal = if (unitPriceNum != null) quantity * unitPriceNum else null

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (lineTotal != null) {
                Text(
                    text = "Total: $currency ${QuotationEngine.formatNumber(lineTotal)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        OutlinedTextField(
            value = priceValue,
            onValueChange = onPriceChange,
            label = { Text("Price $unitLabel ($currency)") },
            placeholder = { Text("e.g. 150") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
    }
}
