package com.example.model

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

enum class UnitSystem(val label: String, val symbol: String, val factorToMeters: Double) {
    METERS("Meters", "m", 1.0),
    CENTIMETERS("Centimeters", "cm", 0.01),
    FEET("Feet", "ft", 0.3048);

    fun toMeters(value: Double): Double = value * factorToMeters
    fun fromMeters(meters: Double): Double = if (factorToMeters > 0) meters / factorToMeters else meters
}

enum class SpaceType(val displayName: String) {
    ROOM_FLOOR("Room Floor"),
    VERANDA("Veranda"),
    BATHROOM("Bathroom")
}

enum class TilePreset(val label: String, val lengthCm: Double, val widthCm: Double) {
    SIZE_30X30("30 × 30 cm", 30.0, 30.0),
    SIZE_40X40("40 × 40 cm", 40.0, 40.0),
    SIZE_60X60("60 × 60 cm", 60.0, 60.0),
    SIZE_60X120("60 × 120 cm", 120.0, 60.0),
    SIZE_80X80("80 × 80 cm", 80.0, 80.0),
    CUSTOM("Custom", 0.0, 0.0)
}

enum class SkirtingPreset(val label: String, val heightCm: Double) {
    HEIGHT_7CM("7 cm", 7.0),
    HEIGHT_10CM("10 cm", 10.0),
    CUSTOM("Custom", 0.0)
}

enum class VerandaSide(val label: String, val sideName: String) {
    FRONT("Front", "Front"),
    BACK("Back", "Back"),
    LEFT("Left", "Left"),
    RIGHT("Right", "Right")
}

enum class VerandaSideType(val label: String) {
    OPENING_STEP("Open edge with a step"),
    WALL("Against a wall")
}

// MATERIALS ENUMS

enum class FloorBedThicknessPreset(val label: String, val thicknessMm: Double) {
    LEVEL_5MM("Level 5 mm", 5.0),
    UNEVEN_8MM("Uneven 8 mm", 8.0),
    VERY_UNEVEN_12MM("Very uneven 12 mm", 12.0),
    CUSTOM("Custom", 0.0)
}

enum class WallBedThicknessPreset(val label: String, val thicknessMm: Double) {
    BED_3MM("3 mm", 3.0),
    BED_6MM("6 mm", 6.0),
    BED_10MM("10 mm", 10.0),
    CUSTOM("Custom", 0.0)
}

enum class GroutType(val label: String, val densityKgL: Double) {
    CEMENT("Cement (1.6 kg/L)", 1.6),
    EPOXY("Epoxy (1.5 kg/L)", 1.5)
}

enum class GroutBagSize(val label: String, val kg: Double) {
    BAG_5KG("5 kg", 5.0),
    BAG_2KG("2 kg", 2.0)
}

enum class JointWidthPreset(val label: String, val widthMm: Double) {
    MM_1_5("1.5 mm", 1.5),
    MM_2("2 mm", 2.0),
    MM_3("3 mm", 3.0),
    MM_4("4 mm", 4.0),
    MM_5("5 mm", 5.0),
    MM_6("6 mm", 6.0),
    MM_8("8 mm", 8.0),
    MM_10("10 mm", 10.0)
}

enum class BondingLiquidMode(val label: String, val shortLabel: String) {
    ADDITIVE_SPLASH("Additive splash (normal rooms)", "Additive splash"),
    FULL_REPLACEMENT("Full water replacement (wet areas such as bathrooms and showers)", "Full water replacement")
}

// EDGE TRIMS ENUMS & MODELS

enum class TrimMaterial(val label: String) {
    PLASTIC("Plastic"),
    METAL("Metal")
}

data class PillarItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val widthCm: String = "30",
    val depthCm: String = "30",
    val material: TrimMaterial = TrimMaterial.METAL,
    val isEnabled: Boolean = true,
    val removeFromEdge: Boolean = false,
    val edgeSide: VerandaSide = VerandaSide.FRONT
)

data class TrimRunItem(
    val id: String,
    val name: String,
    val lengthM: Double,
    val material: TrimMaterial,
    val isEnabled: Boolean = true,
    val areaId: String = "",
    val areaName: String = "",
    val displayText: String = ""
)

data class TrimCalculationResult(
    val pieceLengthM: Double,
    val allowancePercent: Int,
    val plasticTotalRunLengthM: Double,
    val plasticPiecesNeeded: Int,
    val plasticFormulaExplanation: String,
    val metalTotalRunLengthM: Double,
    val metalPiecesNeeded: Int,
    val metalFormulaExplanation: String,
    val activeRuns: List<TrimRunItem> = emptyList()
)

data class StripCalculationResult(
    val stripsPerTile: Int,
    val stripsNeeded: Int,
    val tilesNeeded: Int,
    val warning: String? = null,
    val formulaText: String = "",
    val methodSummary: String = "",
    val fullRows: Int = 0,
    val fullRowTiles: Int = 0,
    val leftoverTiles: Int = 0
)

/**
 * Calculates skirting / step strips and face tiles cut from floor tiles:
 * - Lt = longer side, Wt = shorter side
 * - If h <= Lt: cut strips
 *     strips per tile = floor( Lt / h )
 *     strips needed = ceil( r / Wt )
 *     tiles = ceil( strips needed / strips per tile )
 * - If h > Lt: whole tile rows plus leftover strip
 *     full rows = floor( h / Wt )
 *     full-row tiles = full rows x ceil( r / Lt )
 *     leftover height = h - full rows x Wt
 *     If leftover height > 0.005 m, cut leftover as strips
 *     tiles = full-row tiles + leftover strip tiles
 * - Never return 0 tiles for r > 0 and h > 0.
 * - Warnings only if h <= 0, r <= 0, or h > 3.0 m (300 cm), naming area and side.
 */
object TileFormulas {

    fun calculateStrips(
        tileLengthM: Double,
        tileWidthM: Double,
        stripHeightM: Double,
        runLengthM: Double,
        label: String = "Strip",
        areaName: String = "",
        sideName: String = ""
    ): StripCalculationResult {
        val Lt = max(tileLengthM, tileWidthM)
        val Wt = min(tileLengthM, tileWidthM)
        val h = stripHeightM
        val r = runLengthM

        val locationName = if (areaName.isNotBlank() && sideName.isNotBlank()) {
            "$areaName, $sideName"
        } else if (areaName.isNotBlank()) {
            "$areaName, $label"
        } else {
            label
        }

        var warning: String? = null
        if (h <= 0.0) {
            warning = "$locationName: height must be greater than 0."
        } else if (r <= 0.0) {
            warning = "$locationName: length must be greater than 0."
        } else if (h > 3.0) {
            warning = "$locationName: height is more than 3 m."
        }

        if (Lt <= 0.0 || Wt <= 0.0 || r <= 0.0 || h <= 0.0) {
            return StripCalculationResult(
                stripsPerTile = 0,
                stripsNeeded = 0,
                tilesNeeded = 0,
                warning = warning,
                formulaText = "$label: Run length or strip height is 0 (0 tiles)",
                methodSummary = "$label: 0 tiles"
            )
        }

        val formatDim: (Double) -> String = { meters ->
            val cm = meters * 100.0
            if (kotlin.math.abs(cm - kotlin.math.round(cm)) < 0.01) {
                "${kotlin.math.round(cm).toInt()} cm"
            } else {
                "%.1f cm".format(java.util.Locale.US, cm)
            }
        }
        val formatLen: (Double) -> String = { meters ->
            if (kotlin.math.abs(meters - kotlin.math.round(meters)) < 0.01) {
                "${kotlin.math.round(meters).toInt()} m"
            } else {
                "%.2f m".format(java.util.Locale.US, meters)
            }
        }

        if (h <= Lt + 1e-9) {
            val stripsPerTile = floor((Lt / h) + 1e-9).toInt().coerceAtLeast(1)
            val stripsNeeded = ceil((r / Wt) - 1e-9).toInt().coerceAtLeast(1)
            val calculatedTiles = ceil((stripsNeeded.toDouble() / stripsPerTile.toDouble()) - 1e-9).toInt()
            val tilesNeeded = max(1, calculatedTiles)

            val tileWord = if (tilesNeeded == 1) "tile" else "tiles"
            val methodSummary = "$label: ${formatLen(r)} x ${formatDim(h)} = $stripsPerTile strips per tile, $stripsNeeded strips needed = $tilesNeeded $tileWord"

            val formulaText = buildString {
                append("$label Calculation:\n")
                append("• strips per tile = floor(Lt / h) = floor(${formatDim(Lt)} / ${formatDim(h)}) = $stripsPerTile strips/tile\n")
                append("• strips needed = ceil(r / Wt) = ceil(${formatLen(r)} / ${formatDim(Wt)}) = $stripsNeeded strips\n")
                append("• tiles needed = ceil(strips needed / strips per tile) = ceil($stripsNeeded / $stripsPerTile) = $tilesNeeded $tileWord")
            }

            return StripCalculationResult(
                stripsPerTile = stripsPerTile,
                stripsNeeded = stripsNeeded,
                tilesNeeded = tilesNeeded,
                warning = warning,
                formulaText = formulaText,
                methodSummary = methodSummary
            )
        } else {
            val fullRows = floor((h / Wt) + 1e-9).toInt()
            val tilesPerRow = ceil((r / Lt) - 1e-9).toInt().coerceAtLeast(1)
            val fullRowTiles = fullRows * tilesPerRow
            val leftoverHeight = h - (fullRows * Wt)

            val rowWord = if (fullRows == 1) "row" else "rows"
            val fullRowTileWord = if (fullRowTiles == 1) "tile" else "tiles"

            if (leftoverHeight > 0.005) {
                val stripsPerTile = floor((Lt / leftoverHeight) + 1e-9).toInt().coerceAtLeast(1)
                val stripsNeeded = ceil((r / Wt) - 1e-9).toInt().coerceAtLeast(1)
                val leftoverStripTiles = ceil((stripsNeeded.toDouble() / stripsPerTile.toDouble()) - 1e-9).toInt().coerceAtLeast(1)
                val totalTiles = max(1, fullRowTiles + leftoverStripTiles)

                val totalTileWord = if (totalTiles == 1) "tile" else "tiles"
                val leftoverTileWord = if (leftoverStripTiles == 1) "tile" else "tiles"
                val methodSummary = "$label: ${formatLen(r)} x ${formatDim(h)} = $fullRows $rowWord of ${formatDim(Wt)} ($fullRowTiles $fullRowTileWord) + leftover ${formatDim(leftoverHeight)}: $stripsNeeded strips, $stripsPerTile per tile = $leftoverStripTiles $leftoverTileWord, total = $totalTiles $totalTileWord"

                val formulaText = buildString {
                    append("$label Calculation (Full rows + leftover strip):\n")
                    append("• full rows = floor(h / Wt) = floor(${formatDim(h)} / ${formatDim(Wt)}) = $fullRows $rowWord\n")
                    append("• full-row tiles = full rows × ceil(r / Lt) = $fullRows × ceil(${formatLen(r)} / ${formatDim(Lt)}) = $fullRowTiles $fullRowTileWord\n")
                    append("• leftover height = h - (full rows × Wt) = ${formatDim(h)} - (${fullRows} × ${formatDim(Wt)}) = ${formatDim(leftoverHeight)}\n")
                    append("• leftover strips per tile = floor(Lt / leftover height) = floor(${formatDim(Lt)} / ${formatDim(leftoverHeight)}) = $stripsPerTile\n")
                    append("• leftover strips needed = ceil(r / Wt) = ceil(${formatLen(r)} / ${formatDim(Wt)}) = $stripsNeeded\n")
                    append("• leftover strip tiles = ceil($stripsNeeded / $stripsPerTile) = $leftoverStripTiles\n")
                    append("• total tiles = $fullRowTiles + $leftoverStripTiles = $totalTiles $totalTileWord")
                }

                return StripCalculationResult(
                    stripsPerTile = stripsPerTile,
                    stripsNeeded = stripsNeeded,
                    tilesNeeded = totalTiles,
                    warning = warning,
                    formulaText = formulaText,
                    methodSummary = methodSummary,
                    fullRows = fullRows,
                    fullRowTiles = fullRowTiles,
                    leftoverTiles = leftoverStripTiles
                )
            } else {
                val totalTiles = max(1, fullRowTiles)
                val totalTileWord = if (totalTiles == 1) "tile" else "tiles"
                val methodSummary = "$label: ${formatLen(r)} x ${formatDim(h)} = $fullRows $rowWord of ${formatDim(Wt)}, $tilesPerRow tiles per row = $totalTiles $totalTileWord"

                val formulaText = buildString {
                    append("$label Calculation (Full rows):\n")
                    append("• full rows = floor(h / Wt) = floor(${formatDim(h)} / ${formatDim(Wt)}) = $fullRows $rowWord\n")
                    append("• full-row tiles = full rows × ceil(r / Lt) = $fullRows × ceil(${formatLen(r)} / ${formatDim(Lt)}) = $fullRowTiles $fullRowTileWord\n")
                    append("• leftover height = ${formatDim(leftoverHeight)} (<= 5 mm, ignored)\n")
                    append("• total tiles = $totalTiles $totalTileWord")
                }

                return StripCalculationResult(
                    stripsPerTile = 0,
                    stripsNeeded = 0,
                    tilesNeeded = totalTiles,
                    warning = warning,
                    formulaText = formulaText,
                    methodSummary = methodSummary,
                    fullRows = fullRows,
                    fullRowTiles = fullRowTiles,
                    leftoverTiles = 0
                )
            }
        }
    }

    /**
     * Bonding Liquid Display:
     * - under 1 L in ml
     * - from 1 L to 5 L in 1 L bottles
     * - over 5 L as 5 L jugs plus 1 L bottles
     */
    fun formatBondingLiquidDisplay(totalLiters: Double): String {
        if (totalLiters <= 0.0) return "0 ml"

        // under 1 L in ml
        if (totalLiters < 0.9999) {
            val ml = kotlin.math.round(totalLiters * 1000.0).toInt()
            return "$ml ml"
        }

        // from 1 L to 5 L in 1 L bottles
        if (totalLiters <= 5.0001) {
            val bottles = ceil(totalLiters - 1e-9).toInt()
            val bottleLabel = if (bottles == 1) "1 bottle of 1 L" else "$bottles bottles of 1 L"
            val exact = Math.abs(bottles - totalLiters) < 0.001
            return if (exact) bottleLabel else "%.2f L (%s)".format(totalLiters, bottleLabel)
        }

        // over 5 L as 5 L jugs plus 1 L bottles
        val jugs = floor((totalLiters + 1e-9) / 5.0).toInt()
        val remainderL = totalLiters - (jugs * 5.0)
        val bottles = ceil(remainderL - 1e-9).toInt()

        val jugPart = if (jugs == 1) "1 jug of 5 L" else "$jugs jugs of 5 L"
        val bottlePart = when (bottles) {
            0 -> ""
            1 -> " + 1 bottle of 1 L"
            else -> " + $bottles bottles of 1 L"
        }

        val exact = Math.abs(remainderL - bottles) < 0.001
        return if (exact) "$jugPart$bottlePart" else "%.2f L (%s%s)".format(totalLiters, jugPart, bottlePart)
    }
}

data class MaterialCalculationResult(
    // Net Areas (no waste added)
    val floorAreaM2: Double,
    val skirtingAndStepAreaM2: Double,
    val wallAreaM2: Double,

    // Adhesive
    val baseCoverageAt5mm: Double,
    val floorBedThicknessMm: Double,
    val wallBedThicknessMm: Double,
    val floorCoverageM2PerBag: Double,
    val wallCoverageM2PerBag: Double,
    val floorAdhesiveBags: Int,
    val wallAdhesiveBags: Int,
    val totalAdhesiveBags: Int,
    val floorAdhesiveBagsRaw: Double = 0.0,
    val wallAdhesiveBagsRaw: Double = 0.0,
    val totalAdhesiveBagsRaw: Double = 0.0,
    val wallAndSkirtingAreaM2: Double = 0.0,
    val adhesiveFormulaExplanation: String,

    // Grout
    val jointWidthMm: Double,
    val jointDepthMm: Double,
    val groutDensityKgL: Double,
    val floorGroutKgPerM2: Double,
    val wallGroutKgPerM2: Double,
    val floorGroutKg: Double,
    val wallGroutKg: Double,
    val totalGroutKg: Double,
    val groutBagsToBuy: Int,
    val groutBagSizeKg: Double,
    val groutFormulaExplanation: String,

    // Spacers
    val totalTilesCount: Int,
    val spacersPerTile: Int,
    val totalSpacersNeeded: Int,
    val spacerPackSize: Int,
    val spacerPacksToBuy: Int,
    val spacerFormulaExplanation: String,
    val wholeJobSpacerPacks: Int = 0,
    val spacerBuyMode: SpacerBuyMode = SpacerBuyMode.REUSE_DAILY,

    // Bonding Liquid
    val bondingMode: BondingLiquidMode,
    val totalBondingLiquidLiters: Double,
    val bondingLiquidDisplayText: String,
    val bondingLiquidFormulaExplanation: String
)

data class CalculationResult(
    val spaceType: SpaceType,
    // Floor Tile Results
    val floorTileLabel: String,
    val floorTilesPerBoxCount: Int,
    val floorAreaM2: Double,
    val floorTileAreaM2: Double,
    val baseFloorTiles: Int = 0,               // ceil( net floor area / tile area ) with no waste
    val floorTilesRawNeeded: Int,              // floor tiles with waste
    val floorWasteAllowanceTiles: Int = 0,     // floor tiles with waste - base floor tiles
    val wastePercentage: Int = 0,              // chosen waste %
    val skirtingTilesNeeded: Int,
    val stepFaceTilesNeeded: Int,
    val totalFloorTilesNeeded: Int, // floor + skirting + step face
    val floorBoxesToBuy: Int,
    val floorSpareTiles: Int,
    val floorFormulaExplanation: String,
    val floorSkirtingFormulaExplanation: String = "",
    val floorStepFaceFormulaExplanation: String = "",
    val warnings: List<String> = emptyList(),

    // Wall Tile Results (for Bathroom when wall tiling is enabled)
    val hasWallTiles: Boolean = false,
    val wallTileLabel: String = "",
    val wallTilesPerBoxCount: Int = 0,
    val netWallAreaM2: Double = 0.0,
    val wallTileAreaM2: Double = 0.0,
    val baseWallTiles: Int = 0,                // ceil( net wall area / wall tile area ) with no waste
    val wallTilesNeeded: Int = 0,              // wall tiles with waste
    val wallWasteAllowanceTiles: Int = 0,      // wall tiles with waste - base wall tiles
    val wallBoxesToBuy: Int = 0,
    val wallSpareTiles: Int = 0,
    val wallFormulaExplanation: String = "",

    // Overall summary
    val totalBoxesToBuy: Int = floorBoxesToBuy,
    val totalTilesCount: Int = totalFloorTilesNeeded,

    // Materials Calculation Result
    val materials: MaterialCalculationResult,

    // Edge Trims Calculation Result
    val trims: TrimCalculationResult,

    // Bathtub breakdown
    val bathtubSidesAreaM2: Double = 0.0,
    val bathtubTopAreaM2: Double = 0.0,
    val hasBathtub: Boolean = false,

    // Per-area breakdown
    val areaBreakdowns: List<AreaBreakdownItem> = emptyList(),

    // Working for each run (skirting, step face, door strip)
    val runMethodSummaries: List<String> = emptyList()
)

data class AreaBreakdownItem(
    val areaId: String,
    val areaName: String,
    val spaceType: SpaceType,
    val netFloorAreaM2: Double,
    val rawFloorTiles: Int,
    val skirtingTiles: Int,
    val stepFaceTiles: Int,
    val wallTiles: Int,
    val totalAreaTiles: Int,
    val skirtingAndStepAreaM2: Double,
    val netWallAreaM2: Double,
    val summaryText: String,
    val baseFloorTiles: Int = 0,
    val floorWasteAllowanceTiles: Int = 0,
    val runMethodSummaries: List<String> = emptyList(),
    val trimRuns: List<TrimRunItem> = emptyList(),
    val bathtubSidesAreaM2: Double = 0.0,
    val bathtubTopAreaM2: Double = 0.0
)

data class UserProfile(
    val builderName: String = "",
    val builderPhone: String = ""
)

data class AreaItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val type: SpaceType,
    val roomState: RoomFloorState = RoomFloorState(),
    val verandaState: VerandaState = VerandaState(),
    val bathroomState: BathroomState = BathroomState()
)

data class Job(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val clientName: String = "",
    val addressOrPlot: String = "",
    val notes: String = "",
    val unitSetting: UnitSystem = UnitSystem.METERS,
    val tileSettings: FloorTileSettingsState = FloorTileSettingsState(),
    val materialSettings: MaterialSettingsState = MaterialSettingsState(),
    val trimSettings: TrimSettingsState = TrimSettingsState(),
    val areas: List<AreaItem> = emptyList()
)

