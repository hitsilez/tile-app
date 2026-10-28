package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

enum class QuotationFormat(val label: String) {
    DETAILED_CLIENT("Detailed quotation (for client)"),
    QUICK_SHOP("Quick list (for shop)")
}

data class QuotationPrices(
    val currency: String = "P",
    val floorTilePricePerBox: String = "",
    val wallTilePricePerBox: String = "",
    val adhesivePricePerBag: String = "",
    val groutPricePerBag: String = "",
    val bondingLiquidPrice: String = "",
    val spacersPricePerPack: String = "",
    val plasticTrimPricePerPiece: String = "",
    val metalTrimPricePerPiece: String = "",
    val labourLabel: String = "Labour",
    val labourAmount: String = ""
)

object QuotationEngine {

    fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            String.format(Locale.US, "%,d", value.toLong())
        } else {
            String.format(Locale.US, "%,.2f", value)
        }
    }

    fun formatArea(value: Double): String {
        return if (value % 1.0 == 0.0) {
            "${value.toInt()} m²"
        } else {
            String.format(Locale.US, "%.2f m²", value)
        }
    }

    fun formatTileSizeShort(
        preset: TilePreset,
        customL: String,
        customW: String,
        label: String
    ): String {
        return when (preset) {
            TilePreset.SIZE_30X30 -> "30x30"
            TilePreset.SIZE_40X40 -> "40x40"
            TilePreset.SIZE_60X60 -> "60x60"
            TilePreset.SIZE_60X120 -> "60x120"
            TilePreset.SIZE_80X80 -> "80x80"
            TilePreset.CUSTOM -> {
                val cleanL = customL.trim().replace(".0", "")
                val cleanW = customW.trim().replace(".0", "")
                if (cleanL.isNotBlank() && cleanW.isNotBlank()) {
                    "${cleanL}x${cleanW}"
                } else if (label.isNotBlank()) {
                    label.replace(" × ", "x").replace(" cm", "").replace(" ", "")
                } else {
                    "Custom"
                }
            }
        }
    }

    fun getBondingLiquidBottlesOrJugs(liters: Double): Int {
        if (liters <= 0.001) return 0
        if (liters < 1.0) return 1
        if (liters <= 5.001) return ceil(liters - 1e-9).toInt()
        val jugs = floor((liters + 1e-9) / 5.0).toInt()
        val remainderL = liters - (jugs * 5.0)
        val bottles = ceil(remainderL - 1e-9).toInt()
        return jugs + bottles
    }

    fun hasAnyPrices(prices: QuotationPrices): Boolean {
        return prices.floorTilePricePerBox.toDoubleOrNull() != null ||
                prices.wallTilePricePerBox.toDoubleOrNull() != null ||
                prices.adhesivePricePerBag.toDoubleOrNull() != null ||
                prices.groutPricePerBag.toDoubleOrNull() != null ||
                prices.bondingLiquidPrice.toDoubleOrNull() != null ||
                prices.spacersPricePerPack.toDoubleOrNull() != null ||
                prices.plasticTrimPricePerPiece.toDoubleOrNull() != null ||
                prices.metalTrimPricePerPiece.toDoubleOrNull() != null ||
                prices.labourAmount.toDoubleOrNull() != null
    }

    /**
     * QUICK LIST (for shop):
     * Contains only: job name, then one line per item with its quantity,
     * for example "Floor tiles 60x60: 13 boxes (4 per box)".
     * No client details, no areas, no prices.
     * Hide any item whose quantity is zero.
     */
    fun generateQuickList(job: Job, result: CalculationResult): String {
        val lines = mutableListOf<String>()

        // 1. Job Name only
        lines.add(job.name.ifBlank { "Untitled Job" })

        // 2. Floor tiles: boxes (and tiles per box)
        if (result.floorBoxesToBuy > 0) {
            val shortSize = formatTileSizeShort(
                preset = job.tileSettings.preset,
                customL = job.tileSettings.customLength,
                customW = job.tileSettings.customWidth,
                label = result.floorTileLabel
            )
            val boxWord = if (result.floorBoxesToBuy == 1) "box" else "boxes"
            val sizePart = if (shortSize.isNotBlank()) " $shortSize" else ""
            lines.add("Floor tiles$sizePart: ${result.floorBoxesToBuy} $boxWord (${result.floorTilesPerBoxCount} per box)")
        }

        // 3. Wall tiles: boxes (only if wall tiles are on)
        if (result.hasWallTiles && result.wallBoxesToBuy > 0) {
            val wallTileShort = result.wallTileLabel.replace(" × ", "x").replace(" cm", "").replace(" ", "")
            val boxWord = if (result.wallBoxesToBuy == 1) "box" else "boxes"
            val sizePart = if (wallTileShort.isNotBlank()) " $wallTileShort" else ""
            lines.add("Wall tiles$sizePart: ${result.wallBoxesToBuy} $boxWord (${result.wallTilesPerBoxCount} per box)")
        }

        // 4. Tile adhesive: 20 kg bags
        if (result.materials.totalAdhesiveBags > 0) {
            val bagWord = if (result.materials.totalAdhesiveBags == 1) "bag" else "bags"
            lines.add("Tile adhesive: ${result.materials.totalAdhesiveBags} $bagWord (20 kg)")
        }

        // 5. Tile grout: bags (with bag size)
        if (result.materials.groutBagsToBuy > 0) {
            val bagWord = if (result.materials.groutBagsToBuy == 1) "bag" else "bags"
            val bagSizeKg = if (result.materials.groutBagSizeKg % 1.0 == 0.0) {
                result.materials.groutBagSizeKg.toInt().toString()
            } else {
                "%.1f".format(result.materials.groutBagSizeKg)
            }
            lines.add("Tile grout: ${result.materials.groutBagsToBuy} $bagWord ($bagSizeKg kg)")
        }

        // 6. Bonding liquid: amount in bottles or jugs
        if (result.materials.totalBondingLiquidLiters > 0.001) {
            lines.add("Bonding liquid: ${result.materials.bondingLiquidDisplayText}")
        }

        // 7. Tile spacers: packs (with joint size)
        if (result.materials.spacerPacksToBuy > 0) {
            val packWord = if (result.materials.spacerPacksToBuy == 1) "pack" else "packs"
            val jointSizeStr = if (result.materials.jointWidthMm % 1.0 == 0.0) {
                result.materials.jointWidthMm.toInt().toString()
            } else {
                "%.1f".format(result.materials.jointWidthMm)
            }
            lines.add("Tile spacers: ${result.materials.spacerPacksToBuy} $packWord ($jointSizeStr mm)")
        }

        // 8. Plastic trims: pieces (with piece length)
        if (result.trims.plasticPiecesNeeded > 0) {
            val pieceWord = if (result.trims.plasticPiecesNeeded == 1) "piece" else "pieces"
            val pieceLenStr = if (result.trims.pieceLengthM % 1.0 == 0.0) {
                result.trims.pieceLengthM.toInt().toString()
            } else {
                "%.1f".format(result.trims.pieceLengthM)
            }
            lines.add("Plastic trims: ${result.trims.plasticPiecesNeeded} $pieceWord ($pieceLenStr m each)")
        }

        // 9. Metal trims: pieces (with piece length)
        if (result.trims.metalPiecesNeeded > 0) {
            val pieceWord = if (result.trims.metalPiecesNeeded == 1) "piece" else "pieces"
            val pieceLenStr = if (result.trims.pieceLengthM % 1.0 == 0.0) {
                result.trims.pieceLengthM.toInt().toString()
            } else {
                "%.1f".format(result.trims.pieceLengthM)
            }
            lines.add("Metal trims: ${result.trims.metalPiecesNeeded} $pieceWord ($pieceLenStr m each)")
        }

        return lines.joinToString("\n")
    }

    /**
     * DETAILED QUOTATION (for client):
     * In this exact order:
     * - Builder name and phone number from Settings (skip any that are empty)
     * - Job name, client name, address/plot, today's date
     * - Areas: each area's type, size and tiled area
     * - Total tiled area
     * - Materials list with quantities: floor tiles (size, boxes), wall tiles (size, boxes, only if used),
     *   adhesive bags, grout bags, bonding liquid, spacer packs, plastic trims, metal trims (hide zero items)
     * - If the user entered prices: a price for each item, and a total
     * - Notes from the job
     * - A line at the end: "Quantities are estimates. Final amounts depend on site conditions."
     */
    fun generateDetailedQuotation(
        job: Job,
        result: CalculationResult,
        userProfile: UserProfile,
        prices: QuotationPrices = QuotationPrices(),
        currentDateStr: String? = null
    ): String {
        val lines = mutableListOf<String>()

        // 1. Builder name and phone number from Settings (skip any that are empty)
        val hasBuilderName = userProfile.builderName.isNotBlank()
        val hasBuilderPhone = userProfile.builderPhone.isNotBlank()
        if (hasBuilderName) {
            lines.add(userProfile.builderName)
        }
        if (hasBuilderPhone) {
            lines.add("Phone: ${userProfile.builderPhone}")
        }
        if (hasBuilderName || hasBuilderPhone) {
            lines.add("")
        }

        // 2. Job name, client name, address/plot, today's date
        lines.add("Job: ${job.name.ifBlank { "Untitled Job" }}")
        if (job.clientName.isNotBlank()) {
            lines.add("Client: ${job.clientName}")
        }
        if (job.addressOrPlot.isNotBlank()) {
            lines.add("Address/Plot: ${job.addressOrPlot}")
        }
        val dateText = currentDateStr ?: SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
        lines.add("Date: $dateText")
        lines.add("")

        // 3. Areas: each area's type, size and tiled area
        lines.add("Areas:")
        if (job.areas.isEmpty()) {
            lines.add("No areas added.")
        } else {
            job.areas.forEach { area ->
                val breakdown = result.areaBreakdowns.find { it.areaId == area.id }
                val areaTiledM2 = if (breakdown != null) {
                    breakdown.netFloorAreaM2 + breakdown.skirtingAndStepAreaM2 + breakdown.netWallAreaM2
                } else 0.0

                val sizeStr = TileCalculatorEngine.formatAreaSize(area, job.unitSetting)
                lines.add("- ${area.name} (${area.type.displayName}): $sizeStr, Tiled Area: ${formatArea(areaTiledM2)}")
            }
        }
        lines.add("")

        // 4. Total tiled area
        val totalTiledAreaM2 = result.floorAreaM2 + result.netWallAreaM2 +
                result.areaBreakdowns.sumOf { it.skirtingAndStepAreaM2 }
        lines.add("Total Tiled Area: ${formatArea(totalTiledAreaM2)}")
        lines.add("")

        // 5 & 6. Materials list with quantities & prices
        val withPrices = hasAnyPrices(prices)
        val cur = prices.currency.trim().ifBlank { "P" }
        var quotationTotal = 0.0

        lines.add("Materials:")

        // Floor tiles: size, boxes
        if (result.floorBoxesToBuy > 0) {
            val shortSize = formatTileSizeShort(
                preset = job.tileSettings.preset,
                customL = job.tileSettings.customLength,
                customW = job.tileSettings.customWidth,
                label = result.floorTileLabel
            )
            val boxWord = if (result.floorBoxesToBuy == 1) "box" else "boxes"
            val sizePart = if (shortSize.isNotBlank()) " $shortSize" else ""
            val baseLine = "- Floor tiles$sizePart: ${result.floorBoxesToBuy} $boxWord (${result.floorTilesPerBoxCount} per box)"

            val unitPrice = prices.floorTilePricePerBox.toDoubleOrNull()
            if (withPrices && unitPrice != null) {
                val itemTotal = result.floorBoxesToBuy * unitPrice
                quotationTotal += itemTotal
                lines.add("$baseLine @ $cur ${formatNumber(unitPrice)} = $cur ${formatNumber(itemTotal)}")
            } else {
                lines.add(baseLine)
            }
        }

        // Wall tiles: size, boxes (only if used)
        if (result.hasWallTiles && result.wallBoxesToBuy > 0) {
            val wallTileShort = result.wallTileLabel.replace(" × ", "x").replace(" cm", "").replace(" ", "")
            val boxWord = if (result.wallBoxesToBuy == 1) "box" else "boxes"
            val sizePart = if (wallTileShort.isNotBlank()) " $wallTileShort" else ""
            val baseLine = "- Wall tiles$sizePart: ${result.wallBoxesToBuy} $boxWord (${result.wallTilesPerBoxCount} per box)"

            val unitPrice = prices.wallTilePricePerBox.toDoubleOrNull()
            if (withPrices && unitPrice != null) {
                val itemTotal = result.wallBoxesToBuy * unitPrice
                quotationTotal += itemTotal
                lines.add("$baseLine @ $cur ${formatNumber(unitPrice)} = $cur ${formatNumber(itemTotal)}")
            } else {
                lines.add(baseLine)
            }
        }

        // Tile adhesive: 20 kg bags
        if (result.materials.totalAdhesiveBags > 0) {
            val bagWord = if (result.materials.totalAdhesiveBags == 1) "bag" else "bags"
            val baseLine = "- Tile adhesive: ${result.materials.totalAdhesiveBags} $bagWord (20 kg)"

            val unitPrice = prices.adhesivePricePerBag.toDoubleOrNull()
            if (withPrices && unitPrice != null) {
                val itemTotal = result.materials.totalAdhesiveBags * unitPrice
                quotationTotal += itemTotal
                lines.add("$baseLine @ $cur ${formatNumber(unitPrice)} = $cur ${formatNumber(itemTotal)}")
            } else {
                lines.add(baseLine)
            }
        }

        // Tile grout: bags (with bag size)
        if (result.materials.groutBagsToBuy > 0) {
            val bagWord = if (result.materials.groutBagsToBuy == 1) "bag" else "bags"
            val bagSizeKg = if (result.materials.groutBagSizeKg % 1.0 == 0.0) {
                result.materials.groutBagSizeKg.toInt().toString()
            } else {
                "%.1f".format(result.materials.groutBagSizeKg)
            }
            val baseLine = "- Tile grout: ${result.materials.groutBagsToBuy} $bagWord ($bagSizeKg kg)"

            val unitPrice = prices.groutPricePerBag.toDoubleOrNull()
            if (withPrices && unitPrice != null) {
                val itemTotal = result.materials.groutBagsToBuy * unitPrice
                quotationTotal += itemTotal
                lines.add("$baseLine @ $cur ${formatNumber(unitPrice)} = $cur ${formatNumber(itemTotal)}")
            } else {
                lines.add(baseLine)
            }
        }

        // Bonding liquid: amount in bottles or jugs
        if (result.materials.totalBondingLiquidLiters > 0.001) {
            val baseLine = "- Bonding liquid: ${result.materials.bondingLiquidDisplayText}"

            val unitPrice = prices.bondingLiquidPrice.toDoubleOrNull()
            if (withPrices && unitPrice != null) {
                val units = getBondingLiquidBottlesOrJugs(result.materials.totalBondingLiquidLiters)
                val itemTotal = units * unitPrice
                quotationTotal += itemTotal
                lines.add("$baseLine @ $cur ${formatNumber(unitPrice)} = $cur ${formatNumber(itemTotal)}")
            } else {
                lines.add(baseLine)
            }
        }

        // Tile spacers: packs (with joint size)
        if (result.materials.spacerPacksToBuy > 0) {
            val packWord = if (result.materials.spacerPacksToBuy == 1) "pack" else "packs"
            val jointSizeStr = if (result.materials.jointWidthMm % 1.0 == 0.0) {
                result.materials.jointWidthMm.toInt().toString()
            } else {
                "%.1f".format(result.materials.jointWidthMm)
            }
            val baseLine = "- Tile spacers: ${result.materials.spacerPacksToBuy} $packWord ($jointSizeStr mm)"

            val unitPrice = prices.spacersPricePerPack.toDoubleOrNull()
            if (withPrices && unitPrice != null) {
                val itemTotal = result.materials.spacerPacksToBuy * unitPrice
                quotationTotal += itemTotal
                lines.add("$baseLine @ $cur ${formatNumber(unitPrice)} = $cur ${formatNumber(itemTotal)}")
            } else {
                lines.add(baseLine)
            }
        }

        // Plastic trims: pieces (with piece length)
        if (result.trims.plasticPiecesNeeded > 0) {
            val pieceWord = if (result.trims.plasticPiecesNeeded == 1) "piece" else "pieces"
            val pieceLenStr = if (result.trims.pieceLengthM % 1.0 == 0.0) {
                result.trims.pieceLengthM.toInt().toString()
            } else {
                "%.1f".format(result.trims.pieceLengthM)
            }
            val baseLine = "- Plastic trims: ${result.trims.plasticPiecesNeeded} $pieceWord ($pieceLenStr m each)"

            val unitPrice = prices.plasticTrimPricePerPiece.toDoubleOrNull()
            if (withPrices && unitPrice != null) {
                val itemTotal = result.trims.plasticPiecesNeeded * unitPrice
                quotationTotal += itemTotal
                lines.add("$baseLine @ $cur ${formatNumber(unitPrice)} = $cur ${formatNumber(itemTotal)}")
            } else {
                lines.add(baseLine)
            }
        }

        // Metal trims: pieces (with piece length)
        if (result.trims.metalPiecesNeeded > 0) {
            val pieceWord = if (result.trims.metalPiecesNeeded == 1) "piece" else "pieces"
            val pieceLenStr = if (result.trims.pieceLengthM % 1.0 == 0.0) {
                result.trims.pieceLengthM.toInt().toString()
            } else {
                "%.1f".format(result.trims.pieceLengthM)
            }
            val baseLine = "- Metal trims: ${result.trims.metalPiecesNeeded} $pieceWord ($pieceLenStr m each)"

            val unitPrice = prices.metalTrimPricePerPiece.toDoubleOrNull()
            if (withPrices && unitPrice != null) {
                val itemTotal = result.trims.metalPiecesNeeded * unitPrice
                quotationTotal += itemTotal
                lines.add("$baseLine @ $cur ${formatNumber(unitPrice)} = $cur ${formatNumber(itemTotal)}")
            } else {
                lines.add(baseLine)
            }
        }

        // Optional Labour line (if priced)
        val labourVal = prices.labourAmount.toDoubleOrNull()
        if (withPrices && labourVal != null && labourVal > 0.0) {
            val labourLabel = prices.labourLabel.ifBlank { "Labour" }
            quotationTotal += labourVal
            lines.add("- $labourLabel: $cur ${formatNumber(labourVal)}")
        }

        // Total if prices entered
        if (withPrices) {
            lines.add("")
            lines.add("Quotation Total: $cur ${formatNumber(quotationTotal)}")
        }

        // 7. Notes from the job
        if (job.notes.isNotBlank()) {
            lines.add("")
            lines.add("Notes:")
            lines.add(job.notes.trim())
        }

        // 8. Line at the end: "Quantities are estimates. Final amounts depend on site conditions."
        lines.add("")
        lines.add("Quantities are estimates. Final amounts depend on site conditions.")

        return lines.joinToString("\n")
    }
}
