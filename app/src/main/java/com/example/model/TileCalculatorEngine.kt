package com.example.model

import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

object TileCalculatorEngine {

    private fun safeCeil(value: Double): Int = ceil(value - 1e-9).toInt()
    private fun safeFloor(value: Double): Int = floor(value + 1e-9).toInt()

    fun resolveTileDimensionsMeters(
        preset: TilePreset,
        customLengthStr: String,
        customWidthStr: String,
        unit: UnitSystem
    ): Pair<Double, Double> {
        return if (preset != TilePreset.CUSTOM) {
            Pair(preset.lengthCm / 100.0, preset.widthCm / 100.0)
        } else {
            val l = customLengthStr.toDoubleOrNull() ?: 0.0
            val w = customWidthStr.toDoubleOrNull() ?: 0.0
            Pair(unit.toMeters(l), unit.toMeters(w))
        }
    }

    fun resolveHeightMeters(heightStr: String, unit: UnitSystem): Double {
        val v = heightStr.toDoubleOrNull() ?: 0.0
        if (v <= 0.0) return 0.0
        return if (v > 3.0) {
            if (unit == UnitSystem.FEET) v * 0.0254 else v / 100.0
        } else {
            v
        }
    }

    fun resolveSkirtingHeightMeters(
        preset: SkirtingPreset,
        customHeightStr: String,
        unit: UnitSystem
    ): Double {
        return when (preset) {
            SkirtingPreset.HEIGHT_7CM -> 0.07
            SkirtingPreset.HEIGHT_10CM -> 0.10
            SkirtingPreset.CUSTOM -> resolveHeightMeters(customHeightStr, unit)
        }
    }

    fun calculate(
        spaceType: SpaceType,
        unit: UnitSystem,
        tileSettings: FloorTileSettingsState,
        materialSettings: MaterialSettingsState = MaterialSettingsState(),
        trimSettings: TrimSettingsState = TrimSettingsState(),
        roomState: RoomFloorState,
        verandaState: VerandaState,
        bathroomState: BathroomState
    ): CalculationResult {
        val warnings = mutableListOf<String>()
        val runMethodSummaries = mutableListOf<String>()

        // 1. Floor Tile Dimensions & Area
        val (floorTileLengthM, floorTileWidthM) = resolveTileDimensionsMeters(
            tileSettings.preset,
            tileSettings.customLength,
            tileSettings.customWidth,
            unit
        )
        val floorTileAreaM2 = floorTileLengthM * floorTileWidthM
        val wasteFraction = tileSettings.wastePercentage / 100.0
        val tilesPerBox = tileSettings.tilesPerBox.toIntOrNull()?.coerceAtLeast(1) ?: 1

        val floorTileLabel = if (tileSettings.preset != TilePreset.CUSTOM) {
            tileSettings.preset.label
        } else {
            "${tileSettings.customLength} × ${tileSettings.customWidth} ${unit.symbol}"
        }

        when (spaceType) {
            SpaceType.ROOM_FLOOR -> {
                val roomLengthM = unit.toMeters(roomState.length.toDoubleOrNull() ?: 0.0)
                val roomWidthM = unit.toMeters(roomState.width.toDoubleOrNull() ?: 0.0)
                val floorAreaM2 = roomLengthM * roomWidthM

                val baseFloorTiles = if (floorTileAreaM2 > 0.0) {
                    safeCeil(floorAreaM2 / floorTileAreaM2)
                } else 0

                val rawFloorTiles = if (floorTileAreaM2 > 0.0) {
                    safeCeil((floorAreaM2 * (1.0 + wasteFraction)) / floorTileAreaM2)
                } else 0

                val floorWasteAllowanceTiles = max(0, rawFloorTiles - baseFloorTiles)

                // Doorways & Skirting
                var skirtingTiles = 0
                var skirtingAreaM2 = 0.0
                var skirtingLengthM = 0.0
                val skirtingFormulaDetails = StringBuilder()

                if (roomState.hasSkirting) {
                    val skirtingHeightM = resolveSkirtingHeightMeters(
                        roomState.skirtingPreset,
                        roomState.customSkirtingHeight,
                        unit
                    )
                    val perimeterM = 2.0 * (roomLengthM + roomWidthM)
                    val totalDoorwayWidthM = roomState.doorwayWidths.sumOf {
                        unit.toMeters(it.toDoubleOrNull() ?: 0.0)
                    }
                    skirtingLengthM = max(0.0, perimeterM - totalDoorwayWidthM)
                    skirtingAreaM2 = skirtingLengthM * skirtingHeightM

                    val stripResult = TileFormulas.calculateStrips(
                        tileLengthM = floorTileLengthM,
                        tileWidthM = floorTileWidthM,
                        stripHeightM = skirtingHeightM,
                        runLengthM = skirtingLengthM,
                        label = "Room Skirting",
                        areaName = "Room",
                        sideName = "skirting"
                    )

                    stripResult.warning?.let { if (!warnings.contains(it)) warnings.add(it) }
                    skirtingTiles = stripResult.tilesNeeded
                    runMethodSummaries.add(stripResult.methodSummary)

                    skirtingFormulaDetails.append("• Perimeter = 2 × (%.2f m + %.2f m) = %.2f m\n".format(roomLengthM, roomWidthM, perimeterM))
                    skirtingFormulaDetails.append("• Total Door Widths = %.2f m (subtracted)\n".format(totalDoorwayWidthM))
                    skirtingFormulaDetails.append("• Perimeter %.2f m minus doors %.2f m = %.2f m\n".format(perimeterM, totalDoorwayWidthM, skirtingLengthM))
                    skirtingFormulaDetails.append("• Skirting Height = %.3f m (Area = %.3f m²)\n".format(skirtingHeightM, skirtingAreaM2))
                    skirtingFormulaDetails.append(stripResult.formulaText)
                }

                val totalFloorTilesNeeded = rawFloorTiles + skirtingTiles
                val boxes = safeCeil(totalFloorTilesNeeded.toDouble() / tilesPerBox.toDouble())
                val spareTiles = (boxes * tilesPerBox) - totalFloorTilesNeeded

                val floorFormulaExplanation = buildString {
                    append("Floor Area = Length × Width = %.2f m × %.2f m = %.3f m²\n".format(roomLengthM, roomWidthM, floorAreaM2))
                    append("Tile Area = Tile Length × Tile Width = %.3f m × %.3f m = %.4f m²\n".format(floorTileLengthM, floorTileWidthM, floorTileAreaM2))
                    append("Base floor tiles (no waste) = ceil(Floor Area / Tile Area) = ceil(%.3f m² / %.4f m²) = %d tiles\n".format(floorAreaM2, floorTileAreaM2, baseFloorTiles))
                    append("Floor tiles with waste = ceil((Floor Area × (1 + Waste%%)) / Tile Area)\n")
                    append("= ceil((%.3f m² × (1 + %.2f)) / %.4f m²)\n".format(floorAreaM2, wasteFraction, floorTileAreaM2))
                    append("= ceil(%.3f m² / %.4f m²) = %d tiles\n".format(floorAreaM2 * (1.0 + wasteFraction), floorTileAreaM2, rawFloorTiles))
                    append("Extra for cuts and breakage (%d%%): %d tiles\n".format(tileSettings.wastePercentage, floorWasteAllowanceTiles))
                    if (skirtingTiles > 0) {
                        append("Total tiles before boxing = %d floor tiles + %d skirting tiles = %d tiles\n".format(rawFloorTiles, skirtingTiles, totalFloorTilesNeeded))
                        append("(Waste applies only to floor and wall areas. Strip and step-face tiles already count their own cut leftovers, so no waste is added to them.)\n")
                    }
                    append("Boxes = ceil(Total Tiles / Tiles per Box) = ceil(%d / %d) = %d boxes\n".format(totalFloorTilesNeeded, tilesPerBox, boxes))
                    append("Spare tiles left over after boxes = (Boxes × Tiles per Box) - Total Tiles = (%d × %d) - %d = %d spare".format(boxes, tilesPerBox, totalFloorTilesNeeded, spareTiles))
                }

                val materials = calculateMaterials(
                    floorAreaM2 = floorAreaM2,
                    skirtingAndStepAreaM2 = skirtingAreaM2,
                    wallAreaM2 = 0.0,
                    floorTileLengthM = floorTileLengthM,
                    floorTileWidthM = floorTileWidthM,
                    hasWallTiles = false,
                    wallTileLengthM = 0.0,
                    wallTileWidthM = 0.0,
                    totalTilesCount = totalFloorTilesNeeded,
                    materialSettings = materialSettings
                )

                val trims = calculateTrims(
                    spaceType = spaceType,
                    unit = unit,
                    roomState = roomState,
                    verandaState = verandaState,
                    bathroomState = bathroomState,
                    roomSkirtingLengthM = skirtingLengthM,
                    verandaWallSkirtingLengthM = 0.0,
                    bathroomSkirtingLengthM = 0.0,
                    trimSettings = trimSettings
                )

                return CalculationResult(
                    spaceType = spaceType,
                    floorTileLabel = floorTileLabel,
                    floorTilesPerBoxCount = tilesPerBox,
                    floorAreaM2 = floorAreaM2,
                    floorTileAreaM2 = floorTileAreaM2,
                    baseFloorTiles = baseFloorTiles,
                    floorTilesRawNeeded = rawFloorTiles,
                    floorWasteAllowanceTiles = floorWasteAllowanceTiles,
                    wastePercentage = tileSettings.wastePercentage,
                    skirtingTilesNeeded = skirtingTiles,
                    stepFaceTilesNeeded = 0,
                    totalFloorTilesNeeded = totalFloorTilesNeeded,
                    floorBoxesToBuy = boxes,
                    floorSpareTiles = spareTiles,
                    floorFormulaExplanation = floorFormulaExplanation,
                    floorSkirtingFormulaExplanation = skirtingFormulaDetails.toString(),
                    warnings = warnings,
                    materials = materials,
                    trims = trims,
                    runMethodSummaries = runMethodSummaries
                )
            }

            SpaceType.VERANDA -> {
                val verandaLengthM = unit.toMeters(verandaState.length.toDoubleOrNull() ?: 0.0)
                val verandaWidthM = unit.toMeters(verandaState.width.toDoubleOrNull() ?: 0.0)
                val floorAreaM2 = verandaLengthM * verandaWidthM

                val baseFloorTiles = if (floorTileAreaM2 > 0.0) {
                    safeCeil(floorAreaM2 / floorTileAreaM2)
                } else 0

                val rawFloorTiles = if (floorTileAreaM2 > 0.0) {
                    safeCeil((floorAreaM2 * (1.0 + wasteFraction)) / floorTileAreaM2)
                } else 0

                val floorWasteAllowanceTiles = max(0, rawFloorTiles - baseFloorTiles)

                val stepEdgeHeightM = resolveHeightMeters(verandaState.stepEdgeHeight, unit)
                val wallSkirtingHeightM = resolveSkirtingHeightMeters(
                    verandaState.wallSkirtingPreset,
                    verandaState.customWallSkirtingHeight,
                    unit
                )

                var totalStepFaceTiles = 0
                var stepFaceAreaM2 = 0.0
                val stepFaceDetails = StringBuilder()

                // Sides: Front (length), Back (length), Left (width), Right (width)
                val sideConfigs = listOf(
                    Triple(VerandaSide.FRONT, verandaLengthM, verandaState.frontType),
                    Triple(VerandaSide.BACK, verandaLengthM, verandaState.backType),
                    Triple(VerandaSide.LEFT, verandaWidthM, verandaState.leftType),
                    Triple(VerandaSide.RIGHT, verandaWidthM, verandaState.rightType)
                )

                // 1. Calculate Opening (Step) sides
                for ((side, lengthM, type) in sideConfigs) {
                    if (type == VerandaSideType.OPENING_STEP) {
                        stepFaceAreaM2 += (lengthM * stepEdgeHeightM)
                        val stripRes = TileFormulas.calculateStrips(
                            tileLengthM = floorTileLengthM,
                            tileWidthM = floorTileWidthM,
                            stripHeightM = stepEdgeHeightM,
                            runLengthM = lengthM,
                            label = "Step face, ${side.label.lowercase()}",
                            areaName = "Veranda",
                            sideName = "${side.label.lowercase()} step"
                        )
                        stripRes.warning?.let { if (!warnings.contains(it)) warnings.add(it) }
                        totalStepFaceTiles += stripRes.tilesNeeded
                        runMethodSummaries.add(stripRes.methodSummary)
                        stepFaceDetails.append(stripRes.methodSummary).append("\n")
                        stepFaceDetails.append(stripRes.formulaText).append("\n\n")
                    }
                }

                // 2. Calculate Wall sides & Door
                var totalWallSkirtingTiles = 0
                var wallSkirtingAreaM2 = 0.0
                var verandaWallSkirtingRunSumM = 0.0
                val wallSkirtingDetails = StringBuilder()

                val doorSide = if (verandaState.hasDoor) verandaState.doorSide else null
                val doorWidthM = unit.toMeters(verandaState.doorWidth.toDoubleOrNull() ?: 0.0)
                val doorStripHeightM = resolveHeightMeters(verandaState.doorStripHeight, unit)

                for ((side, lengthM, type) in sideConfigs) {
                    if (type == VerandaSideType.WALL) {
                        val hasDoorOnThisSide = (doorSide == side)

                        if (hasDoorOnThisSide) {
                            val effectiveDoorWidthM = doorWidthM.coerceAtMost(lengthM)
                            val remainingWallRunM = max(0.0, lengthM - effectiveDoorWidthM)

                            wallSkirtingAreaM2 += (effectiveDoorWidthM * doorStripHeightM)

                            val doorStripRes = TileFormulas.calculateStrips(
                                tileLengthM = floorTileLengthM,
                                tileWidthM = floorTileWidthM,
                                stripHeightM = doorStripHeightM,
                                runLengthM = effectiveDoorWidthM,
                                label = "Tile height at the door",
                                areaName = "Veranda",
                                sideName = "door strip"
                            )
                            doorStripRes.warning?.let { if (!warnings.contains(it)) warnings.add(it) }
                            totalWallSkirtingTiles += doorStripRes.tilesNeeded
                            runMethodSummaries.add(doorStripRes.methodSummary)

                            wallSkirtingDetails.append(doorStripRes.methodSummary).append("\n")
                            wallSkirtingDetails.append(doorStripRes.formulaText).append("\n")

                            if (verandaState.hasWallSkirting && remainingWallRunM > 0.0) {
                                wallSkirtingAreaM2 += (remainingWallRunM * wallSkirtingHeightM)
                                verandaWallSkirtingRunSumM += remainingWallRunM
                                val restStripRes = TileFormulas.calculateStrips(
                                    tileLengthM = floorTileLengthM,
                                    tileWidthM = floorTileWidthM,
                                    stripHeightM = wallSkirtingHeightM,
                                    runLengthM = remainingWallRunM,
                                    label = "${side.label} wall skirting",
                                    areaName = "Veranda",
                                    sideName = "${side.label.lowercase()} wall skirting"
                                )
                                restStripRes.warning?.let { if (!warnings.contains(it)) warnings.add(it) }
                                totalWallSkirtingTiles += restStripRes.tilesNeeded
                                runMethodSummaries.add(restStripRes.methodSummary)

                                wallSkirtingDetails.append(restStripRes.methodSummary).append("\n")
                                wallSkirtingDetails.append(restStripRes.formulaText).append("\n")
                            }
                        } else {
                            if (verandaState.hasWallSkirting) {
                                wallSkirtingAreaM2 += (lengthM * wallSkirtingHeightM)
                                verandaWallSkirtingRunSumM += lengthM
                                val stripRes = TileFormulas.calculateStrips(
                                    tileLengthM = floorTileLengthM,
                                    tileWidthM = floorTileWidthM,
                                    stripHeightM = wallSkirtingHeightM,
                                    runLengthM = lengthM,
                                    label = "${side.label} wall skirting",
                                    areaName = "Veranda",
                                    sideName = "${side.label.lowercase()} wall skirting"
                                )
                                stripRes.warning?.let { if (!warnings.contains(it)) warnings.add(it) }
                                totalWallSkirtingTiles += stripRes.tilesNeeded
                                runMethodSummaries.add(stripRes.methodSummary)

                                wallSkirtingDetails.append(stripRes.methodSummary).append("\n")
                                wallSkirtingDetails.append(stripRes.formulaText).append("\n")
                            }
                        }
                    }
                }

                val totalFloorTilesNeeded = rawFloorTiles + totalStepFaceTiles + totalWallSkirtingTiles
                val boxes = safeCeil(totalFloorTilesNeeded.toDouble() / tilesPerBox.toDouble())
                val spareTiles = (boxes * tilesPerBox) - totalFloorTilesNeeded

                val floorFormulaExplanation = buildString {
                    append("Veranda Floor Area = Length × Width = %.2f m × %.2f m = %.3f m²\n".format(verandaLengthM, verandaWidthM, floorAreaM2))
                    append("Tile Area = Tile Length × Tile Width = %.3f m × %.3f m = %.4f m²\n".format(floorTileLengthM, floorTileWidthM, floorTileAreaM2))
                    append("Base floor tiles (no waste) = ceil(Floor Area / Tile Area) = ceil(%.3f m² / %.4f m²) = %d tiles\n".format(floorAreaM2, floorTileAreaM2, baseFloorTiles))
                    append("Floor tiles with waste = ceil((Floor Area × (1 + Waste%%)) / Tile Area)\n")
                    append("= ceil((%.3f m² × (1 + %.2f)) / %.4f m²)\n".format(floorAreaM2, wasteFraction, floorTileAreaM2))
                    append("= ceil(%.3f m² / %.4f m²) = %d tiles\n".format(floorAreaM2 * (1.0 + wasteFraction), floorTileAreaM2, rawFloorTiles))
                    append("Extra for cuts and breakage (%d%%): %d tiles\n".format(tileSettings.wastePercentage, floorWasteAllowanceTiles))
                    append("Total tiles before boxing = %d floor + %d step-face + %d wall skirting = %d tiles\n"
                        .format(rawFloorTiles, totalStepFaceTiles, totalWallSkirtingTiles, totalFloorTilesNeeded))
                    append("(Waste applies only to floor and wall areas. Strip and step-face tiles already count their own cut leftovers, so no waste is added to them.)\n")
                    append("Boxes = ceil(Total Tiles / Tiles per Box) = ceil(%d / %d) = %d boxes\n".format(totalFloorTilesNeeded, tilesPerBox, boxes))
                    append("Spare tiles left over after boxes = (Boxes × Tiles per Box) - Total Tiles = (%d × %d) - %d = %d spare".format(boxes, tilesPerBox, totalFloorTilesNeeded, spareTiles))
                }

                val materials = calculateMaterials(
                    floorAreaM2 = floorAreaM2,
                    skirtingAndStepAreaM2 = stepFaceAreaM2 + wallSkirtingAreaM2,
                    wallAreaM2 = 0.0,
                    floorTileLengthM = floorTileLengthM,
                    floorTileWidthM = floorTileWidthM,
                    hasWallTiles = false,
                    wallTileLengthM = 0.0,
                    wallTileWidthM = 0.0,
                    totalTilesCount = totalFloorTilesNeeded,
                    materialSettings = materialSettings
                )

                val trims = calculateTrims(
                    spaceType = spaceType,
                    unit = unit,
                    roomState = roomState,
                    verandaState = verandaState,
                    bathroomState = bathroomState,
                    roomSkirtingLengthM = 0.0,
                    verandaWallSkirtingLengthM = verandaWallSkirtingRunSumM,
                    bathroomSkirtingLengthM = 0.0,
                    trimSettings = trimSettings
                )

                return CalculationResult(
                    spaceType = spaceType,
                    floorTileLabel = floorTileLabel,
                    floorTilesPerBoxCount = tilesPerBox,
                    floorAreaM2 = floorAreaM2,
                    floorTileAreaM2 = floorTileAreaM2,
                    baseFloorTiles = baseFloorTiles,
                    floorTilesRawNeeded = rawFloorTiles,
                    floorWasteAllowanceTiles = floorWasteAllowanceTiles,
                    wastePercentage = tileSettings.wastePercentage,
                    skirtingTilesNeeded = totalWallSkirtingTiles,
                    stepFaceTilesNeeded = totalStepFaceTiles,
                    totalFloorTilesNeeded = totalFloorTilesNeeded,
                    floorBoxesToBuy = boxes,
                    floorSpareTiles = spareTiles,
                    floorFormulaExplanation = floorFormulaExplanation,
                    floorSkirtingFormulaExplanation = wallSkirtingDetails.toString(),
                    floorStepFaceFormulaExplanation = stepFaceDetails.toString(),
                    warnings = warnings,
                    materials = materials,
                    trims = trims,
                    runMethodSummaries = runMethodSummaries
                )
            }

            SpaceType.BATHROOM -> {
                // 1. Bathroom Floor
                val floorLengthM = unit.toMeters(bathroomState.length.toDoubleOrNull() ?: 0.0)
                val floorWidthM = unit.toMeters(bathroomState.width.toDoubleOrNull() ?: 0.0)
                val floorAreaM2 = floorLengthM * floorWidthM

                val baseFloorTiles = if (floorTileAreaM2 > 0.0) {
                    safeCeil(floorAreaM2 / floorTileAreaM2)
                } else 0

                val rawFloorTiles = if (floorTileAreaM2 > 0.0) {
                    safeCeil((floorAreaM2 * (1.0 + wasteFraction)) / floorTileAreaM2)
                } else 0

                val floorWasteAllowanceTiles = max(0, rawFloorTiles - baseFloorTiles)

                var skirtingTiles = 0
                var skirtingAreaM2 = 0.0
                var bathroomSkirtingLengthM = 0.0
                val skirtingFormulaDetails = StringBuilder()

                if (bathroomState.hasSkirting) {
                    val skirtingHeightM = resolveSkirtingHeightMeters(
                        bathroomState.skirtingPreset,
                        bathroomState.customSkirtingHeight,
                        unit
                    )
                    val perimeterM = 2.0 * (floorLengthM + floorWidthM)
                    val totalDoorwayWidthM = bathroomState.doorwayWidths.sumOf {
                        unit.toMeters(it.toDoubleOrNull() ?: 0.0)
                    }
                    bathroomSkirtingLengthM = max(0.0, perimeterM - totalDoorwayWidthM)
                    skirtingAreaM2 = bathroomSkirtingLengthM * skirtingHeightM

                    val stripResult = TileFormulas.calculateStrips(
                        tileLengthM = floorTileLengthM,
                        tileWidthM = floorTileWidthM,
                        stripHeightM = skirtingHeightM,
                        runLengthM = bathroomSkirtingLengthM,
                        label = "Bathroom skirting",
                        areaName = "Bathroom",
                        sideName = "skirting"
                    )

                    stripResult.warning?.let { if (!warnings.contains(it)) warnings.add(it) }
                    skirtingTiles = stripResult.tilesNeeded
                    runMethodSummaries.add(stripResult.methodSummary)

                    skirtingFormulaDetails.append("• Floor Perimeter = 2 × (%.2f m + %.2f m) = %.2f m\n".format(floorLengthM, floorWidthM, perimeterM))
                    skirtingFormulaDetails.append("• Total Door Widths = %.2f m (subtracted)\n".format(totalDoorwayWidthM))
                    skirtingFormulaDetails.append("• Perimeter %.2f m minus doors %.2f m = %.2f m\n".format(perimeterM, totalDoorwayWidthM, bathroomSkirtingLengthM))
                    skirtingFormulaDetails.append("• Skirting Height = %.3f m (Area = %.3f m²)\n".format(skirtingHeightM, skirtingAreaM2))
                    skirtingFormulaDetails.append(stripResult.formulaText)
                }

                val totalFloorTilesNeeded = rawFloorTiles + skirtingTiles
                val floorBoxes = safeCeil(totalFloorTilesNeeded.toDouble() / tilesPerBox.toDouble())
                val floorSpare = (floorBoxes * tilesPerBox) - totalFloorTilesNeeded

                val floorFormulaExplanation = buildString {
                    append("Bathroom Floor Area = Length × Width = %.2f m × %.2f m = %.3f m²\n".format(floorLengthM, floorWidthM, floorAreaM2))
                    append("Tile Area = Tile Length × Tile Width = %.3f m × %.3f m = %.4f m²\n".format(floorTileLengthM, floorTileWidthM, floorTileAreaM2))
                    append("Base floor tiles (no waste) = ceil(Floor Area / Tile Area) = ceil(%.3f m² / %.4f m²) = %d tiles\n".format(floorAreaM2, floorTileAreaM2, baseFloorTiles))
                    append("Floor tiles with waste = ceil((Floor Area × (1 + Waste%%)) / Tile Area)\n")
                    append("= ceil((%.3f m² × (1 + %.2f)) / %.4f m²)\n".format(floorAreaM2, wasteFraction, floorTileAreaM2))
                    append("= ceil(%.3f m² / %.4f m²) = %d tiles\n".format(floorAreaM2 * (1.0 + wasteFraction), floorTileAreaM2, rawFloorTiles))
                    append("Extra for cuts and breakage (%d%%): %d tiles\n".format(tileSettings.wastePercentage, floorWasteAllowanceTiles))
                    if (skirtingTiles > 0) {
                        append("Total floor tiles before boxing = %d floor + %d skirting = %d tiles\n".format(rawFloorTiles, skirtingTiles, totalFloorTilesNeeded))
                        append("(Waste applies only to floor and wall areas. Strip and step-face tiles already count their own cut leftovers, so no waste is added to them.)\n")
                    }
                    append("Floor Boxes = ceil(Total Floor Tiles / Tiles per Box) = ceil(%d / %d) = %d boxes\n".format(totalFloorTilesNeeded, tilesPerBox, floorBoxes))
                    append("Spare tiles left over after boxes = (%d × %d) - %d = %d spare".format(floorBoxes, tilesPerBox, totalFloorTilesNeeded, floorSpare))
                }

                // 2. Bathroom Walls (Optional)
                var hasWallTiles = false
                var netWallAreaM2 = 0.0
                var wallTileAreaM2 = 0.0
                var baseWallTiles = 0
                var wallTilesNeeded = 0
                var wallWasteAllowanceTiles = 0
                var wallBoxesToBuy = 0
                var wallSpareTiles = 0
                var wallTileLengthM = 0.0
                var wallTileWidthM = 0.0
                var wallTilesPerBox = 10
                var wallTileLabel = ""
                val wallFormulaExplanation = StringBuilder()

                if (bathroomState.tileWallsToo) {
                    hasWallTiles = true
                    val resolvedWallDims = resolveTileDimensionsMeters(
                        bathroomState.wallTilePreset,
                        bathroomState.customWallTileLength,
                        bathroomState.customWallTileWidth,
                        unit
                    )
                    wallTileLengthM = resolvedWallDims.first
                    wallTileWidthM = resolvedWallDims.second
                    wallTileAreaM2 = wallTileLengthM * wallTileWidthM
                    wallTilesPerBox = bathroomState.wallTilesPerBox.toIntOrNull()?.coerceAtLeast(1) ?: 1

                    wallTileLabel = if (bathroomState.wallTilePreset != TilePreset.CUSTOM) {
                        bathroomState.wallTilePreset.label
                    } else {
                        "${bathroomState.customWallTileLength} × ${bathroomState.customWallTileWidth} ${unit.symbol}"
                    }

                    // Wall areas
                    val grossWallAreaM2 = bathroomState.walls.sumOf { wall ->
                        val w = unit.toMeters(wall.width.toDoubleOrNull() ?: 0.0)
                        val h = unit.toMeters(wall.height.toDoubleOrNull() ?: 0.0)
                        w * h
                    }

                    // Windows (subtracted)
                    val windowsAreaM2 = bathroomState.windows.sumOf { win ->
                        val w = unit.toMeters(win.width.toDoubleOrNull() ?: 0.0)
                        val h = unit.toMeters(win.height.toDoubleOrNull() ?: 0.0)
                        w * h
                    }

                    // Doors (subtracted)
                    val doorsAreaM2 = bathroomState.doors.sumOf { door ->
                        val w = unit.toMeters(door.width.toDoubleOrNull() ?: 0.0)
                        val h = unit.toMeters(door.height.toDoubleOrNull() ?: 0.0)
                        w * h
                    }

                    // Bathtub surround (added)
                    val bathtubAreaM2 = if (bathroomState.hasBathtub) {
                        val h = unit.toMeters(bathroomState.bathtubHeight.toDoubleOrNull() ?: 0.0)
                        val w = unit.toMeters(bathroomState.bathtubWidth.toDoubleOrNull() ?: 0.0)
                        h * w
                    } else 0.0

                    netWallAreaM2 = max(0.0, grossWallAreaM2 - windowsAreaM2 - doorsAreaM2 + bathtubAreaM2)

                    if (wallTileAreaM2 > 0.0) {
                        baseWallTiles = safeCeil(netWallAreaM2 / wallTileAreaM2)
                        wallTilesNeeded = safeCeil((netWallAreaM2 * (1.0 + wasteFraction)) / wallTileAreaM2)
                        wallWasteAllowanceTiles = max(0, wallTilesNeeded - baseWallTiles)
                    }
                    wallBoxesToBuy = safeCeil(wallTilesNeeded.toDouble() / wallTilesPerBox.toDouble())
                    wallSpareTiles = (wallBoxesToBuy * wallTilesPerBox) - wallTilesNeeded

                    wallFormulaExplanation.append("Wall Area Calculation:\n")
                    wallFormulaExplanation.append("• Gross Wall Area (%d walls) = %.3f m²\n".format(bathroomState.walls.size, grossWallAreaM2))
                    if (windowsAreaM2 > 0.0) {
                        wallFormulaExplanation.append("• Windows Area = -%.3f m² (subtracted)\n".format(windowsAreaM2))
                    }
                    if (doorsAreaM2 > 0.0) {
                        wallFormulaExplanation.append("• Doors Area = -%.3f m² (subtracted)\n".format(doorsAreaM2))
                    }
                    if (bathtubAreaM2 > 0.0) {
                        wallFormulaExplanation.append("• Bathtub Surround Area = +%.3f m² (added to wall area)\n".format(bathtubAreaM2))
                    }
                    wallFormulaExplanation.append("• Net Wall Area = %.3f m²\n".format(netWallAreaM2))
                    wallFormulaExplanation.append("• Wall Tile Area = %.3f m × %.3f m = %.4f m²\n".format(wallTileLengthM, wallTileWidthM, wallTileAreaM2))
                    wallFormulaExplanation.append("• Base wall tiles (no waste) = ceil(Net Wall Area / Wall Tile Area) = ceil(%.3f m² / %.4f m²) = %d wall tiles\n".format(netWallAreaM2, wallTileAreaM2, baseWallTiles))
                    wallFormulaExplanation.append("• Wall tiles needed = ceil((Net Wall Area × (1 + Waste%%)) / Wall Tile Area)\n")
                    wallFormulaExplanation.append("  = ceil((%.3f m² × (1 + %.2f)) / %.4f m²)\n".format(netWallAreaM2, wasteFraction, wallTileAreaM2))
                    wallFormulaExplanation.append("  = ceil(%.3f m² / %.4f m²) = %d wall tiles\n".format(netWallAreaM2 * (1.0 + wasteFraction), wallTileAreaM2, wallTilesNeeded))
                    wallFormulaExplanation.append("• Extra for cuts and breakage (%d%%): %d tiles\n".format(tileSettings.wastePercentage, wallWasteAllowanceTiles))
                    wallFormulaExplanation.append("  (Waste applies only to floor and wall areas. Strip and step-face tiles already count their own cut leftovers, so no waste is added to them.)\n")
                    wallFormulaExplanation.append("• Wall Boxes = ceil(Wall Tiles / Tiles per Box) = ceil(%d / %d) = %d boxes\n".format(wallTilesNeeded, wallTilesPerBox, wallBoxesToBuy))
                    wallFormulaExplanation.append("• Spare tiles left over after boxes = (%d × %d) - %d = %d spare".format(wallBoxesToBuy, wallTilesPerBox, wallTilesNeeded, wallSpareTiles))
                }

                val totalBoxes = floorBoxes + (if (hasWallTiles) wallBoxesToBuy else 0)
                val totalTiles = totalFloorTilesNeeded + (if (hasWallTiles) wallTilesNeeded else 0)

                val materials = calculateMaterials(
                    floorAreaM2 = floorAreaM2,
                    skirtingAndStepAreaM2 = skirtingAreaM2,
                    wallAreaM2 = if (hasWallTiles) netWallAreaM2 else 0.0,
                    floorTileLengthM = floorTileLengthM,
                    floorTileWidthM = floorTileWidthM,
                    hasWallTiles = hasWallTiles,
                    wallTileLengthM = wallTileLengthM,
                    wallTileWidthM = wallTileWidthM,
                    totalTilesCount = totalTiles,
                    materialSettings = materialSettings
                )

                val trims = calculateTrims(
                    spaceType = spaceType,
                    unit = unit,
                    roomState = roomState,
                    verandaState = verandaState,
                    bathroomState = bathroomState,
                    roomSkirtingLengthM = 0.0,
                    verandaWallSkirtingLengthM = 0.0,
                    bathroomSkirtingLengthM = bathroomSkirtingLengthM,
                    trimSettings = trimSettings
                )

                return CalculationResult(
                    spaceType = spaceType,
                    floorTileLabel = floorTileLabel,
                    floorTilesPerBoxCount = tilesPerBox,
                    floorAreaM2 = floorAreaM2,
                    floorTileAreaM2 = floorTileAreaM2,
                    baseFloorTiles = baseFloorTiles,
                    floorTilesRawNeeded = rawFloorTiles,
                    floorWasteAllowanceTiles = floorWasteAllowanceTiles,
                    wastePercentage = tileSettings.wastePercentage,
                    skirtingTilesNeeded = skirtingTiles,
                    stepFaceTilesNeeded = 0,
                    totalFloorTilesNeeded = totalFloorTilesNeeded,
                    floorBoxesToBuy = floorBoxes,
                    floorSpareTiles = floorSpare,
                    floorFormulaExplanation = floorFormulaExplanation,
                    floorSkirtingFormulaExplanation = skirtingFormulaDetails.toString(),
                    warnings = warnings,
                    hasWallTiles = hasWallTiles,
                    wallTileLabel = wallTileLabel,
                    wallTilesPerBoxCount = wallTilesPerBox,
                    netWallAreaM2 = netWallAreaM2,
                    wallTileAreaM2 = wallTileAreaM2,
                    baseWallTiles = baseWallTiles,
                    wallTilesNeeded = wallTilesNeeded,
                    wallWasteAllowanceTiles = wallWasteAllowanceTiles,
                    wallBoxesToBuy = wallBoxesToBuy,
                    wallSpareTiles = wallSpareTiles,
                    wallFormulaExplanation = wallFormulaExplanation.toString(),
                    totalBoxesToBuy = totalBoxes,
                    totalTilesCount = totalTiles,
                    materials = materials,
                    trims = trims,
                    runMethodSummaries = runMethodSummaries
                )
            }
        }
    }

    /**
     * MATERIALS CALCULATION
     */
    fun calculateMaterials(
        floorAreaM2: Double,
        skirtingAndStepAreaM2: Double,
        wallAreaM2: Double,
        floorTileLengthM: Double,
        floorTileWidthM: Double,
        hasWallTiles: Boolean,
        wallTileLengthM: Double,
        wallTileWidthM: Double,
        totalTilesCount: Int,
        materialSettings: MaterialSettingsState
    ): MaterialCalculationResult {

        // 1. ADHESIVE (cement-based, 20 kg bags)
        val baseCoverageAt5mm = (materialSettings.baseCoverageAt5mm.toDoubleOrNull() ?: 3.0).coerceAtLeast(0.1)

        val floorBedMm = if (materialSettings.floorBedPreset != FloorBedThicknessPreset.CUSTOM) {
            materialSettings.floorBedPreset.thicknessMm
        } else {
            (materialSettings.customFloorBedMm.toDoubleOrNull() ?: 5.0).coerceAtLeast(1.0)
        }

        val wallBedMm = if (materialSettings.wallBedPreset != WallBedThicknessPreset.CUSTOM) {
            materialSettings.wallBedPreset.thicknessMm
        } else {
            (materialSettings.customWallBedMm.toDoubleOrNull() ?: 6.0).coerceAtLeast(1.0)
        }

        val floorCoverageM2PerBag = baseCoverageAt5mm * 5.0 / floorBedMm
        val wallCoverageM2PerBag = baseCoverageAt5mm * 5.0 / wallBedMm

        // Unrounded bags:
        // Floor bags (unrounded) = floor area / floor coverage
        // Wall, skirting and step bags (unrounded) = (wall area + skirting area + step-face area) / wall coverage
        // Total adhesive bags = ceil( floor bags + wall/skirting/step bags ). Round up once, at the end.
        val floorAdhesiveBagsRaw = if (floorAreaM2 > 0.0) floorAreaM2 / floorCoverageM2PerBag else 0.0
        val wallAndSkirtingAreaM2 = wallAreaM2 + skirtingAndStepAreaM2
        val wallAdhesiveBagsRaw = if (wallAndSkirtingAreaM2 > 0.0) wallAndSkirtingAreaM2 / wallCoverageM2PerBag else 0.0

        val totalAdhesiveBagsRaw = floorAdhesiveBagsRaw + wallAdhesiveBagsRaw
        val totalAdhesiveBags = if (totalAdhesiveBagsRaw > 0.0) safeCeil(totalAdhesiveBagsRaw) else 0

        val floorAdhesiveBags = if (floorAdhesiveBagsRaw > 0.0) safeCeil(floorAdhesiveBagsRaw) else 0
        val wallAdhesiveBags = if (wallAdhesiveBagsRaw > 0.0) safeCeil(wallAdhesiveBagsRaw) else 0

        val adhesiveFormulaExplanation = buildString {
            append("• Base coverage @ 5 mm bed = %.2f m²/bag (20 kg)\n".format(Locale.US, baseCoverageAt5mm))
            append("• Floor coverage = base × 5 / bed thickness = %.2f × 5 / %.1f mm = %.3f m²/bag\n"
                .format(Locale.US, baseCoverageAt5mm, floorBedMm, floorCoverageM2PerBag))
            append("• Floor bags (unrounded) = Floor Area / Floor Coverage = %.2f m² / %.3f m² = %.2f bags\n"
                .format(Locale.US, floorAreaM2, floorCoverageM2PerBag, floorAdhesiveBagsRaw))
            if (wallAndSkirtingAreaM2 > 0.0) {
                val wallLabel = if (wallAreaM2 > 0.0 && skirtingAndStepAreaM2 > 0.0) "skirting, step and wall"
                    else if (wallAreaM2 > 0.0) "wall"
                    else "skirting and step"
                append("• %s area = %.2f m²\n".format(Locale.US, wallLabel.replaceFirstChar { it.uppercase() }, wallAndSkirtingAreaM2))
                append("• %s coverage = base × 5 / bed thickness = %.2f × 5 / %.1f mm = %.3f m²/bag\n"
                    .format(Locale.US, wallLabel.replaceFirstChar { it.uppercase() }, baseCoverageAt5mm, wallBedMm, wallCoverageM2PerBag))
                append("• %s bags (unrounded) = %.2f m² / %.3f m² = %.2f bags\n"
                    .format(Locale.US, wallLabel.replaceFirstChar { it.uppercase() }, wallAndSkirtingAreaM2, wallCoverageM2PerBag, wallAdhesiveBagsRaw))
                append("• Total adhesive bags = ceil(Floor %.2f + %s %.2f = %.2f) = %d bags (20 kg)\n"
                    .format(Locale.US, floorAdhesiveBagsRaw, wallLabel, wallAdhesiveBagsRaw, totalAdhesiveBagsRaw, totalAdhesiveBags))
                append("  (Floor %.2f + %s %.2f = %.2f, so %d bags)"
                    .format(Locale.US, floorAdhesiveBagsRaw, wallLabel, wallAdhesiveBagsRaw, totalAdhesiveBagsRaw, totalAdhesiveBags))
            } else {
                append("• Total adhesive bags = ceil(Floor %.2f) = %d bags (20 kg)"
                    .format(Locale.US, floorAdhesiveBagsRaw, totalAdhesiveBags))
            }
        }

        // 2. GROUT
        val jointWidthMm = materialSettings.jointWidthMm.coerceAtLeast(0.5)
        val jointDepthMm = (materialSettings.jointDepthMm.toDoubleOrNull() ?: 10.0).coerceAtLeast(1.0)
        val groutDensity = materialSettings.groutType.densityKgL
        val groutBagSizeKg = materialSettings.groutBagSize.kg

        // Floor tile size in mm
        val floorTileLMm = (floorTileLengthM * 1000.0).coerceAtLeast(10.0)
        val floorTileWMm = (floorTileWidthM * 1000.0).coerceAtLeast(10.0)
        val floorKgPerM2 = ((floorTileLMm + floorTileWMm) / (floorTileLMm * floorTileWMm)) * jointWidthMm * jointDepthMm * groutDensity

        val floorGroutAreaM2 = floorAreaM2 + skirtingAndStepAreaM2
        val floorGroutKg = floorKgPerM2 * floorGroutAreaM2 * 1.10

        // Wall tile size in mm (if applicable)
        val (wallKgPerM2, wallGroutKg) = if (hasWallTiles && wallAreaM2 > 0.0) {
            val wallTileLMm = (wallTileLengthM * 1000.0).coerceAtLeast(10.0)
            val wallTileWMm = (wallTileWidthM * 1000.0).coerceAtLeast(10.0)
            val wKgM2 = ((wallTileLMm + wallTileWMm) / (wallTileLMm * wallTileWMm)) * jointWidthMm * jointDepthMm * groutDensity
            val wGrout = wKgM2 * wallAreaM2 * 1.10
            Pair(wKgM2, wGrout)
        } else {
            Pair(0.0, 0.0)
        }

        val totalGroutKg = floorGroutKg + wallGroutKg
        val groutBagsToBuy = if (totalGroutKg > 0.0) safeCeil(totalGroutKg / groutBagSizeKg) else 0

        val groutFormulaExplanation = buildString {
            append("• Formula: kg/m² = ((L + W) / (L × W)) × J × D × density\n")
            append("  where L, W, J, D are in mm, density = %.2f kg/L (%s)\n"
                .format(groutDensity, materialSettings.groutType.label))
            append("• Floor kg/m² = ((%.0f + %.0f) / (%.0f × %.0f)) × %.1f mm × %.1f mm × %.2f = %.4f kg/m²\n"
                .format(floorTileLMm, floorTileWMm, floorTileLMm, floorTileWMm, jointWidthMm, jointDepthMm, groutDensity, floorKgPerM2))
            append("• Floor grout = %.4f kg/m² × (%.2f m² floor + %.2f m² skirting/step) × 1.10 = %.2f kg\n"
                .format(floorKgPerM2, floorAreaM2, skirtingAndStepAreaM2, floorGroutKg))
            if (hasWallTiles && wallAreaM2 > 0.0) {
                append("• Wall grout = %.4f kg/m² × %.2f m² wall × 1.10 = %.2f kg\n"
                    .format(wallKgPerM2, wallAreaM2, wallGroutKg))
            }
            append("• Total grout kg = %.2f kg\n".format(totalGroutKg))
            append("• Bags to buy = ceil(%.2f kg / %.1f kg bag) = %d bags (%s)"
                .format(totalGroutKg, groutBagSizeKg, groutBagsToBuy, materialSettings.groutBagSize.label))
        }

        // 3. SPACERS
        val spacersPerTile = (materialSettings.spacersPerTile.toIntOrNull() ?: 2).coerceAtLeast(0)
        val spacerPackSize = (materialSettings.spacerPackSize.toIntOrNull() ?: 100).coerceAtLeast(1)
        val totalSpacersNeeded = totalTilesCount * spacersPerTile
        val spacerPacksToBuy = if (totalSpacersNeeded > 0) safeCeil(totalSpacersNeeded.toDouble() / spacerPackSize.toDouble()) else 0

        val spacerFormulaExplanation = buildString {
            append("• Joint size J = %.1f mm (same as grout)\n".format(jointWidthMm))
            append("• Spacers needed = Total tiles × Spacers per tile = %d tiles × %d = %d spacers\n"
                .format(totalTilesCount, spacersPerTile, totalSpacersNeeded))
            append("• Packs to buy = ceil(Spacers needed / Pack size) = ceil(%d / %d) = %d packs"
                .format(totalSpacersNeeded, spacerPackSize, spacerPacksToBuy))
        }

        // 4. BONDING LIQUID
        val totalBondingLiquidLiters = when (materialSettings.bondingMode) {
            BondingLiquidMode.ADDITIVE_SPLASH -> {
                val ml = (materialSettings.additiveMlPerBag.toDoubleOrNull() ?: 250.0).coerceAtLeast(0.0)
                totalAdhesiveBags * (ml / 1000.0)
            }
            BondingLiquidMode.FULL_REPLACEMENT -> {
                val l = (materialSettings.replacementLitersPerBag.toDoubleOrNull() ?: 5.0).coerceAtLeast(0.0)
                totalAdhesiveBags * l
            }
        }

        val bondingDisplayText = TileFormulas.formatBondingLiquidDisplay(totalBondingLiquidLiters)

        val bondingLiquidFormulaExplanation = buildString {
            append("• Mode: %s\n".format(materialSettings.bondingMode.label))
            when (materialSettings.bondingMode) {
                BondingLiquidMode.ADDITIVE_SPLASH -> {
                    val ml = materialSettings.additiveMlPerBag.toDoubleOrNull() ?: 250.0
                    append("• Amount: %.0f ml per 20 kg adhesive bag\n".format(ml))
                    append("• Total volume = %d adhesive bags × %.0f ml = %.0f ml (%.2f L)\n"
                        .format(totalAdhesiveBags, ml, totalAdhesiveBags * ml, totalBondingLiquidLiters))
                }
                BondingLiquidMode.FULL_REPLACEMENT -> {
                    val l = materialSettings.replacementLitersPerBag.toDoubleOrNull() ?: 5.0
                    append("• Amount: %.1f L per 20 kg adhesive bag\n".format(l))
                    append("• Total volume = %d adhesive bags × %.1f L = %.1f L\n"
                        .format(totalAdhesiveBags, l, totalBondingLiquidLiters))
                }
            }
            append("• Packaging required: %s".format(bondingDisplayText))
        }

        return MaterialCalculationResult(
            floorAreaM2 = floorAreaM2,
            skirtingAndStepAreaM2 = skirtingAndStepAreaM2,
            wallAreaM2 = wallAreaM2,
            baseCoverageAt5mm = baseCoverageAt5mm,
            floorBedThicknessMm = floorBedMm,
            wallBedThicknessMm = wallBedMm,
            floorCoverageM2PerBag = floorCoverageM2PerBag,
            wallCoverageM2PerBag = wallCoverageM2PerBag,
            floorAdhesiveBags = floorAdhesiveBags,
            wallAdhesiveBags = wallAdhesiveBags,
            totalAdhesiveBags = totalAdhesiveBags,
            floorAdhesiveBagsRaw = floorAdhesiveBagsRaw,
            wallAdhesiveBagsRaw = wallAdhesiveBagsRaw,
            totalAdhesiveBagsRaw = totalAdhesiveBagsRaw,
            wallAndSkirtingAreaM2 = wallAndSkirtingAreaM2,
            adhesiveFormulaExplanation = adhesiveFormulaExplanation,
            jointWidthMm = jointWidthMm,
            jointDepthMm = jointDepthMm,
            groutDensityKgL = groutDensity,
            floorGroutKgPerM2 = floorKgPerM2,
            wallGroutKgPerM2 = wallKgPerM2,
            floorGroutKg = floorGroutKg,
            wallGroutKg = wallGroutKg,
            totalGroutKg = totalGroutKg,
            groutBagsToBuy = groutBagsToBuy,
            groutBagSizeKg = groutBagSizeKg,
            groutFormulaExplanation = groutFormulaExplanation,
            totalTilesCount = totalTilesCount,
            spacersPerTile = spacersPerTile,
            totalSpacersNeeded = totalSpacersNeeded,
            spacerPackSize = spacerPackSize,
            spacerPacksToBuy = spacerPacksToBuy,
            spacerFormulaExplanation = spacerFormulaExplanation,
            bondingMode = materialSettings.bondingMode,
            totalBondingLiquidLiters = totalBondingLiquidLiters,
            bondingLiquidDisplayText = bondingDisplayText,
            bondingLiquidFormulaExplanation = bondingLiquidFormulaExplanation
        )
    }

    fun formatTrimLen(m: Double): String {
        val s = String.format(Locale.US, "%.2f", m)
        return if (s.endsWith(".00")) {
            String.format(Locale.US, "%.1f", m)
        } else if (s.endsWith("0")) {
            s.dropLast(1)
        } else {
            s
        }
    }

    /**
     * Calculates edge trim runs for an area based on user selections.
     * Material defaults: Metal for veranda step edges and pillars, Plastic everywhere else.
     * Every run can have its material picked and be switched on or off.
     */
    fun calculateAreaTrims(
        area: AreaItem,
        unit: UnitSystem
    ): List<TrimRunItem> {
        val runs = mutableListOf<TrimRunItem>()
        when (area.type) {
            SpaceType.ROOM_FLOOR -> {
                val rLenM = unit.toMeters(area.roomState.length.toDoubleOrNull() ?: 0.0)
                val rWidM = unit.toMeters(area.roomState.width.toDoubleOrNull() ?: 0.0)
                val perimeterM = 2.0 * (rLenM + rWidM)
                val totalDoorwayWidthM = area.roomState.doorwayWidths.sumOf {
                    unit.toMeters(it.toDoubleOrNull() ?: 0.0)
                }
                // "Along the skirting". Length = skirting length = perimeter minus total width of all doors. Stops at every door.
                // If skirting is off, along-skirting run is 0.
                val skirtingRunM = if (area.roomState.hasSkirting) max(0.0, perimeterM - totalDoorwayWidthM) else 0.0
                if (area.roomState.hasAlongSkirtingTrim && area.roomState.hasSkirting && skirtingRunM > 0.0) {
                    val mat = area.roomState.alongSkirtingTrimMaterial
                    runs.add(
                        TrimRunItem(
                            id = "along_skirting_${area.id}",
                            name = "Along skirting",
                            lengthM = skirtingRunM,
                            material = mat,
                            isEnabled = true,
                            areaId = area.id,
                            areaName = area.name,
                            displayText = "Along skirting, ${mat.label.lowercase()}: ${formatTrimLen(skirtingRunM)} m"
                        )
                    )
                }

                // Optional run per door: "Across door opening", length = that door's width. Off by default.
                area.roomState.doorwayWidths.forEachIndexed { dIdx, dStr ->
                    val doorWM = unit.toMeters(dStr.toDoubleOrNull() ?: 0.0)
                    if (doorWM > 0.0 && area.roomState.isDoorTrimEnabled(dIdx)) {
                        val mat = area.roomState.getDoorTrimMaterial(dIdx)
                        runs.add(
                            TrimRunItem(
                                id = "door_trim_${area.id}_$dIdx",
                                name = "Across door opening ${dIdx + 1}",
                                lengthM = doorWM,
                                material = mat,
                                isEnabled = true,
                                areaId = area.id,
                                areaName = area.name,
                                displayText = "Door ${dIdx + 1} opening, ${mat.label.lowercase()}: ${formatTrimLen(doorWM)} m"
                            )
                        )
                    }
                }
            }

            SpaceType.VERANDA -> {
                val vLenM = unit.toMeters(area.verandaState.length.toDoubleOrNull() ?: 0.0)
                val vWidM = unit.toMeters(area.verandaState.width.toDoubleOrNull() ?: 0.0)
                val doorWidthM = unit.toMeters(area.verandaState.doorWidth.toDoubleOrNull() ?: 0.0)
                val hasDoor = area.verandaState.hasDoor
                val doorSide = area.verandaState.doorSide

                // Pillars
                val pillarRuns = mutableListOf<TrimRunItem>()
                val sidePillarDeductions = mutableMapOf<VerandaSide, Double>()

                area.verandaState.pillars.forEachIndexed { pIdx, pillar ->
                    if (pillar.isEnabled) {
                        val pWidthM = (pillar.widthCm.toDoubleOrNull() ?: 30.0) / 100.0
                        val pDepthM = (pillar.depthCm.toDoubleOrNull() ?: 30.0) / 100.0
                        val pRunM = 2.0 * (pWidthM + pDepthM)
                        if (pRunM > 0.0) {
                            pillarRuns.add(
                                TrimRunItem(
                                    id = "pillar_${area.id}_${pillar.id}",
                                    name = "Pillar ${pIdx + 1}",
                                    lengthM = pRunM,
                                    material = pillar.material,
                                    isEnabled = true,
                                    areaId = area.id,
                                    areaName = area.name,
                                    displayText = "Pillar ${pIdx + 1}, ${pillar.material.label.lowercase()}: ${formatTrimLen(pRunM)} m"
                                )
                            )
                        }
                        if (pillar.removeFromEdge) {
                            sidePillarDeductions[pillar.edgeSide] = (sidePillarDeductions[pillar.edgeSide] ?: 0.0) + pWidthM
                        }
                    }
                }

                // Sides: Front & Back length = vLenM, Left & Right length = vWidM
                val sides = listOf(
                    Triple(VerandaSide.FRONT, vLenM, area.verandaState.frontType),
                    Triple(VerandaSide.BACK, vLenM, area.verandaState.backType),
                    Triple(VerandaSide.LEFT, vWidM, area.verandaState.leftType),
                    Triple(VerandaSide.RIGHT, vWidM, area.verandaState.rightType)
                )

                for ((side, sideLenM, sideType) in sides) {
                    if (!area.verandaState.isSideTrimEnabled(side)) continue
                    val mat = area.verandaState.getSideTrimMaterial(side)
                    val pillarDedM = sidePillarDeductions[side] ?: 0.0

                    if (sideType == VerandaSideType.OPENING_STEP) {
                        val netLenM = max(0.0, sideLenM - pillarDedM)
                        val dispText = if (pillarDedM > 0.0) {
                            "${side.label} step, ${mat.label.lowercase()}: ${formatTrimLen(sideLenM)} m minus pillar ${formatTrimLen(pillarDedM)} m = ${formatTrimLen(netLenM)} m"
                        } else {
                            "${side.label} step, ${mat.label.lowercase()}: ${formatTrimLen(netLenM)} m"
                        }
                        if (netLenM > 0.0) {
                            runs.add(
                                TrimRunItem(
                                    id = "v_side_${area.id}_${side.name}",
                                    name = "${side.label} step",
                                    lengthM = netLenM,
                                    material = mat,
                                    isEnabled = true,
                                    areaId = area.id,
                                    areaName = area.name,
                                    displayText = dispText
                                )
                            )
                        }
                    } else {
                        // Against a wall: subtract door width if door is on that side
                        val hasDoorOnThisSide = hasDoor && (doorSide == side)
                        val doorDeduction = if (hasDoorOnThisSide) doorWidthM.coerceAtMost(sideLenM) else 0.0
                        val netLenM = max(0.0, sideLenM - doorDeduction - pillarDedM)
                        val dispText = if (doorDeduction > 0.0 && pillarDedM > 0.0) {
                            "${side.label} wall, ${mat.label.lowercase()}: ${formatTrimLen(sideLenM)} m minus door ${formatTrimLen(doorDeduction)} m minus pillar ${formatTrimLen(pillarDedM)} m = ${formatTrimLen(netLenM)} m"
                        } else if (doorDeduction > 0.0) {
                            "${side.label} wall, ${mat.label.lowercase()}: ${formatTrimLen(sideLenM)} m minus door ${formatTrimLen(doorDeduction)} m = ${formatTrimLen(netLenM)} m"
                        } else if (pillarDedM > 0.0) {
                            "${side.label} wall, ${mat.label.lowercase()}: ${formatTrimLen(sideLenM)} m minus pillar ${formatTrimLen(pillarDedM)} m = ${formatTrimLen(netLenM)} m"
                        } else {
                            "${side.label} wall, ${mat.label.lowercase()}: ${formatTrimLen(netLenM)} m"
                        }
                        if (netLenM > 0.0) {
                            runs.add(
                                TrimRunItem(
                                    id = "v_side_${area.id}_${side.name}",
                                    name = "${side.label} wall",
                                    lengthM = netLenM,
                                    material = mat,
                                    isEnabled = true,
                                    areaId = area.id,
                                    areaName = area.name,
                                    displayText = dispText
                                )
                            )
                        }
                    }
                }

                runs.addAll(pillarRuns)
            }

            SpaceType.BATHROOM -> {
                val bLenM = unit.toMeters(area.bathroomState.length.toDoubleOrNull() ?: 0.0)
                val bWidM = unit.toMeters(area.bathroomState.width.toDoubleOrNull() ?: 0.0)
                val perimeterM = 2.0 * (bLenM + bWidM)
                val totalDoorwayWidthM = area.bathroomState.doorwayWidths.sumOf {
                    unit.toMeters(it.toDoubleOrNull() ?: 0.0)
                }
                // Floor: same as Room floor
                val skirtingRunM = if (area.bathroomState.hasSkirting) max(0.0, perimeterM - totalDoorwayWidthM) else 0.0
                if (area.bathroomState.hasAlongSkirtingTrim && area.bathroomState.hasSkirting && skirtingRunM > 0.0) {
                    val mat = area.bathroomState.alongSkirtingTrimMaterial
                    runs.add(
                        TrimRunItem(
                            id = "along_skirting_${area.id}",
                            name = "Along skirting",
                            lengthM = skirtingRunM,
                            material = mat,
                            isEnabled = true,
                            areaId = area.id,
                            areaName = area.name,
                            displayText = "Along skirting, ${mat.label.lowercase()}: ${formatTrimLen(skirtingRunM)} m"
                        )
                    )
                }

                // Optional run per door
                area.bathroomState.doorwayWidths.forEachIndexed { dIdx, dStr ->
                    val doorWM = unit.toMeters(dStr.toDoubleOrNull() ?: 0.0)
                    if (doorWM > 0.0 && area.bathroomState.isDoorTrimEnabled(dIdx)) {
                        val mat = area.bathroomState.getDoorTrimMaterial(dIdx)
                        runs.add(
                            TrimRunItem(
                                id = "door_trim_${area.id}_$dIdx",
                                name = "Across door opening ${dIdx + 1}",
                                lengthM = doorWM,
                                material = mat,
                                isEnabled = true,
                                areaId = area.id,
                                areaName = area.name,
                                displayText = "Door ${dIdx + 1} opening, ${mat.label.lowercase()}: ${formatTrimLen(doorWM)} m"
                            )
                        )
                    }
                }

                // Walls: existing "Trim on top of the wall tiles" switch
                if (area.bathroomState.tileWallsToo && area.bathroomState.hasWallTopTrim) {
                    val totalWallWidthM = area.bathroomState.walls.sumOf {
                        unit.toMeters(it.width.toDoubleOrNull() ?: 0.0)
                    }
                    if (totalWallWidthM > 0.0) {
                        val mat = area.bathroomState.wallTopTrimMaterial
                        runs.add(
                            TrimRunItem(
                                id = "bath_wall_top_${area.id}",
                                name = "Wall tiles top edge",
                                lengthM = totalWallWidthM,
                                material = mat,
                                isEnabled = true,
                                areaId = area.id,
                                areaName = area.name,
                                displayText = "Wall tiles top edge, ${mat.label.lowercase()}: ${formatTrimLen(totalWallWidthM)} m"
                            )
                        )
                    }
                }
            }
        }
        return runs
    }

    /**
     * Format area dimensions with correct job unit (e.g. "2.05 m x 1.85 m" or "205 cm x 185 cm")
     */
    fun formatAreaSize(area: AreaItem, unit: UnitSystem): String {
        val (rawL, rawW) = when (area.type) {
            SpaceType.ROOM_FLOOR -> area.roomState.length to area.roomState.width
            SpaceType.VERANDA -> area.verandaState.length to area.verandaState.width
            SpaceType.BATHROOM -> area.bathroomState.length to area.bathroomState.width
        }
        val cleanL = rawL.trim().ifBlank { "0" }
        val cleanW = rawW.trim().ifBlank { "0" }
        return "$cleanL ${unit.symbol} x $cleanW ${unit.symbol}"
    }

    /**
     * EDGE TRIMS CALCULATION
     * Piece length (default 2.5 m), allowance % (default 10%)
     * Trim pieces = ceil( total run length of that material x (1 + allowance%) / piece length )
     * Add up all areas first, then round up once per material.
     */
    fun calculateTrimsForResult(
        runs: List<TrimRunItem>,
        trimSettings: TrimSettingsState
    ): TrimCalculationResult {
        val pieceLengthM = (trimSettings.pieceLengthM.toDoubleOrNull() ?: 2.5).coerceAtLeast(0.1)
        val allowanceFraction = trimSettings.allowancePercent / 100.0

        val plasticRuns = runs.filter { it.isEnabled && it.material == TrimMaterial.PLASTIC }
        val metalRuns = runs.filter { it.isEnabled && it.material == TrimMaterial.METAL }

        val plasticTotalRunM = plasticRuns.sumOf { it.lengthM }
        val metalTotalRunM = metalRuns.sumOf { it.lengthM }

        val plasticPieces = if (plasticTotalRunM > 0.0) {
            safeCeil((plasticTotalRunM * (1.0 + allowanceFraction)) / pieceLengthM)
        } else 0

        val metalPieces = if (metalTotalRunM > 0.0) {
            safeCeil((metalTotalRunM * (1.0 + allowanceFraction)) / pieceLengthM)
        } else 0

        val plasticFormula = buildString {
            append("• Plastic trim run = ")
            if (plasticRuns.isEmpty()) {
                append("0.00 m\n")
            } else {
                append(plasticRuns.joinToString(" + ") { "%.2f m (%s)".format(it.lengthM, it.displayText.ifBlank { it.name }) })
                append(" = %.2f m\n".format(plasticTotalRunM))
            }
            append("• Formula: ceil(total run × (1 + extra%%) / piece length)\n")
            append("  = ceil(%.2f m × (1 + %.2f) / %.2f m)\n".format(plasticTotalRunM, allowanceFraction, pieceLengthM))
            append("  = ceil(%.2f / %.2f) = %d pieces (%.1f m)".format(plasticTotalRunM * (1.0 + allowanceFraction), pieceLengthM, plasticPieces, pieceLengthM))
        }

        val metalFormula = buildString {
            append("• Metal trim run = ")
            if (metalRuns.isEmpty()) {
                append("0.00 m\n")
            } else {
                append(metalRuns.joinToString(" + ") { "%.2f m (%s)".format(it.lengthM, it.displayText.ifBlank { it.name }) })
                append(" = %.2f m\n".format(metalTotalRunM))
            }
            append("• Formula: ceil(total run × (1 + extra%%) / piece length)\n")
            append("  = ceil(%.2f m × (1 + %.2f) / %.2f m)\n".format(metalTotalRunM, allowanceFraction, pieceLengthM))
            append("  = ceil(%.2f / %.2f) = %d pieces (%.1f m)".format(metalTotalRunM * (1.0 + allowanceFraction), pieceLengthM, metalPieces, pieceLengthM))
        }

        return TrimCalculationResult(
            pieceLengthM = pieceLengthM,
            allowancePercent = trimSettings.allowancePercent,
            plasticTotalRunLengthM = plasticTotalRunM,
            plasticPiecesNeeded = plasticPieces,
            plasticFormulaExplanation = plasticFormula,
            metalTotalRunLengthM = metalTotalRunM,
            metalPiecesNeeded = metalPieces,
            metalFormulaExplanation = metalFormula,
            activeRuns = runs.filter { it.isEnabled }
        )
    }

    fun calculateTrims(
        spaceType: SpaceType,
        unit: UnitSystem,
        roomState: RoomFloorState,
        verandaState: VerandaState,
        bathroomState: BathroomState,
        roomSkirtingLengthM: Double,
        verandaWallSkirtingLengthM: Double,
        bathroomSkirtingLengthM: Double,
        trimSettings: TrimSettingsState
    ): TrimCalculationResult {
        val dummyArea = AreaItem(
            id = "single_preview",
            name = spaceType.displayName,
            type = spaceType,
            roomState = roomState,
            verandaState = verandaState,
            bathroomState = bathroomState
        )
        val areaRuns = calculateAreaTrims(dummyArea, unit).toMutableList()

        trimSettings.outerCorners.forEach { corner ->
            val lenM = unit.toMeters(corner.lengthM.toDoubleOrNull() ?: 0.0)
            if (lenM > 0.0 && corner.isEnabled) {
                areaRuns.add(
                    TrimRunItem(
                        id = corner.id,
                        name = corner.name.ifBlank { "Outer Corner" },
                        lengthM = lenM,
                        material = corner.material,
                        isEnabled = true,
                        displayText = "${corner.name}: ${formatTrimLen(lenM)} m"
                    )
                )
            }
        }
        trimSettings.customRuns.forEach { custom ->
            val lenM = unit.toMeters(custom.lengthM.toDoubleOrNull() ?: 0.0)
            if (lenM > 0.0 && custom.isEnabled) {
                areaRuns.add(
                    TrimRunItem(
                        id = custom.id,
                        name = custom.name.ifBlank { "Custom Trim Run" },
                        lengthM = lenM,
                        material = custom.material,
                        isEnabled = true,
                        displayText = "${custom.name}: ${formatTrimLen(lenM)} m"
                    )
                )
            }
        }
        return calculateTrimsForResult(areaRuns, trimSettings)
    }

    /**
     * WHOLE JOB CALCULATION:
     * - Add up the net floor area of all areas first
     * - Apply the waste % ONCE to that total
     * - Add the skirting, step-face and wall tiles
     * - Convert to boxes only ONCE, at the end. Never round up to boxes per area.
     * - Materials & trims calculated for the whole job.
     * - Provide per-area breakdown.
     */
    fun calculateJob(job: Job): CalculationResult {
        val warnings = mutableListOf<String>()
        val unit = job.unitSetting

        // 1. Floor Tile Dimensions & Area
        val (floorTileLengthM, floorTileWidthM) = resolveTileDimensionsMeters(
            job.tileSettings.preset,
            job.tileSettings.customLength,
            job.tileSettings.customWidth,
            unit
        )
        val floorTileAreaM2 = floorTileLengthM * floorTileWidthM
        val wasteFraction = job.tileSettings.wastePercentage / 100.0
        val tilesPerBox = job.tileSettings.tilesPerBox.toIntOrNull()?.coerceAtLeast(1) ?: 1

        val floorTileLabel = if (job.tileSettings.preset != TilePreset.CUSTOM) {
            job.tileSettings.preset.label
        } else {
            "${job.tileSettings.customLength} × ${job.tileSettings.customWidth} ${unit.symbol}"
        }

        if (job.areas.isEmpty()) {
            val emptyMat = calculateMaterials(
                floorAreaM2 = 0.0,
                skirtingAndStepAreaM2 = 0.0,
                wallAreaM2 = 0.0,
                floorTileLengthM = floorTileLengthM,
                floorTileWidthM = floorTileWidthM,
                hasWallTiles = false,
                wallTileLengthM = 0.0,
                wallTileWidthM = 0.0,
                totalTilesCount = 0,
                materialSettings = job.materialSettings
            )
            val emptyTrims = calculateTrims(
                spaceType = SpaceType.ROOM_FLOOR,
                unit = unit,
                roomState = RoomFloorState(),
                verandaState = VerandaState(),
                bathroomState = BathroomState(),
                roomSkirtingLengthM = 0.0,
                verandaWallSkirtingLengthM = 0.0,
                bathroomSkirtingLengthM = 0.0,
                trimSettings = job.trimSettings
            )
            return CalculationResult(
                spaceType = SpaceType.ROOM_FLOOR,
                floorTileLabel = floorTileLabel,
                floorTilesPerBoxCount = tilesPerBox,
                floorAreaM2 = 0.0,
                floorTileAreaM2 = floorTileAreaM2,
                floorTilesRawNeeded = 0,
                skirtingTilesNeeded = 0,
                stepFaceTilesNeeded = 0,
                totalFloorTilesNeeded = 0,
                floorBoxesToBuy = 0,
                floorSpareTiles = 0,
                floorFormulaExplanation = "No areas added yet. Add an area below to calculate.",
                warnings = emptyList(),
                materials = emptyMat,
                trims = emptyTrims,
                areaBreakdowns = emptyList()
            )
        }

        var totalNetFloorAreaM2 = 0.0
        var totalSkirtingTilesSum = 0
        var totalStepFaceTilesSum = 0
        var totalSkirtingAndStepAreaM2 = 0.0

        var hasWallTiles = false
        var totalNetWallAreaM2 = 0.0
        var totalWallTilesSum = 0
        var wallTilesPerBox = 10
        var wallTileLengthM = 0.0
        var wallTileWidthM = 0.0
        var wallTileLabel = ""
        val wallFormulaDetails = StringBuilder()

        val allTrimRuns = mutableListOf<TrimRunItem>()
        val areaBreakdowns = mutableListOf<AreaBreakdownItem>()

        val skirtingFormulaDetails = StringBuilder()
        val stepFaceFormulaDetails = StringBuilder()
        val jobRunSummaries = mutableListOf<String>()

        // Iterate through all areas in the job
        job.areas.forEachIndexed { index, area ->
            val areaRunSummaries = mutableListOf<String>()
            when (area.type) {
                SpaceType.ROOM_FLOOR -> {
                    val rLenM = unit.toMeters(area.roomState.length.toDoubleOrNull() ?: 0.0)
                    val rWidM = unit.toMeters(area.roomState.width.toDoubleOrNull() ?: 0.0)
                    val areaFloorM2 = rLenM * rWidM
                    totalNetFloorAreaM2 += areaFloorM2

                    var areaSkirtingTiles = 0
                    var areaSkirtingAreaM2 = 0.0
                    var areaSkirtingRunM = 0.0

                    if (area.roomState.hasSkirting) {
                        val skirtingHeightM = resolveSkirtingHeightMeters(
                            area.roomState.skirtingPreset,
                            area.roomState.customSkirtingHeight,
                            unit
                        )
                        val perimeterM = 2.0 * (rLenM + rWidM)
                        val totalDoorwayWidthM = area.roomState.doorwayWidths.sumOf {
                            unit.toMeters(it.toDoubleOrNull() ?: 0.0)
                        }
                        areaSkirtingRunM = max(0.0, perimeterM - totalDoorwayWidthM)
                        areaSkirtingAreaM2 = areaSkirtingRunM * skirtingHeightM

                        val stripRes = TileFormulas.calculateStrips(
                            tileLengthM = floorTileLengthM,
                            tileWidthM = floorTileWidthM,
                            stripHeightM = skirtingHeightM,
                            runLengthM = areaSkirtingRunM,
                            label = "${area.name} skirting",
                            areaName = area.name,
                            sideName = "skirting"
                        )
                        stripRes.warning?.let { if (!warnings.contains(it)) warnings.add(it) }
                        areaSkirtingTiles = stripRes.tilesNeeded
                        totalSkirtingTilesSum += areaSkirtingTiles
                        totalSkirtingAndStepAreaM2 += areaSkirtingAreaM2
                        areaRunSummaries.add(stripRes.methodSummary)
                        jobRunSummaries.add(stripRes.methodSummary)

                        skirtingFormulaDetails.append("• ${area.name} Skirting:\n")
                        skirtingFormulaDetails.append("  Perimeter %.2f m minus doors %.2f m = %.2f m\n".format(perimeterM, totalDoorwayWidthM, areaSkirtingRunM))
                        skirtingFormulaDetails.append("  ").append(stripRes.formulaText.replace("\n", "\n  ")).append("\n\n")

                    }

                    val areaTrimRuns = calculateAreaTrims(area, unit)
                    allTrimRuns.addAll(areaTrimRuns)

                    val baseTilesForArea = if (floorTileAreaM2 > 0.0) {
                        safeCeil(areaFloorM2 / floorTileAreaM2)
                    } else 0
                    val rawTilesForArea = if (floorTileAreaM2 > 0.0) {
                        safeCeil((areaFloorM2 * (1.0 + wasteFraction)) / floorTileAreaM2)
                    } else 0
                    val wasteTilesForArea = max(0, rawTilesForArea - baseTilesForArea)

                    areaBreakdowns.add(
                        AreaBreakdownItem(
                            areaId = area.id,
                            areaName = area.name,
                            spaceType = SpaceType.ROOM_FLOOR,
                            netFloorAreaM2 = areaFloorM2,
                            rawFloorTiles = rawTilesForArea,
                            skirtingTiles = areaSkirtingTiles,
                            stepFaceTiles = 0,
                            wallTiles = 0,
                            totalAreaTiles = rawTilesForArea + areaSkirtingTiles,
                            skirtingAndStepAreaM2 = areaSkirtingAreaM2,
                            netWallAreaM2 = 0.0,
                            summaryText = "${formatAreaSize(area, unit)} • %.2f m²".format(java.util.Locale.US, areaFloorM2),
                            baseFloorTiles = baseTilesForArea,
                            floorWasteAllowanceTiles = wasteTilesForArea,
                            runMethodSummaries = areaRunSummaries,
                            trimRuns = areaTrimRuns
                        )
                    )
                }

                SpaceType.VERANDA -> {
                    val vLenM = unit.toMeters(area.verandaState.length.toDoubleOrNull() ?: 0.0)
                    val vWidM = unit.toMeters(area.verandaState.width.toDoubleOrNull() ?: 0.0)
                    val areaFloorM2 = vLenM * vWidM
                    totalNetFloorAreaM2 += areaFloorM2

                    val stepEdgeHeightM = resolveHeightMeters(area.verandaState.stepEdgeHeight, unit)
                    val wallSkirtingHeightM = resolveSkirtingHeightMeters(
                        area.verandaState.wallSkirtingPreset,
                        area.verandaState.customWallSkirtingHeight,
                        unit
                    )

                    var areaStepFaceTiles = 0
                    var areaStepFaceAreaM2 = 0.0
                    var areaWallSkirtingTiles = 0
                    var areaWallSkirtingAreaM2 = 0.0
                    var verandaWallSkirtingRunSumM = 0.0

                    val sideConfigs = listOf(
                        Triple(VerandaSide.FRONT, vLenM, area.verandaState.frontType),
                        Triple(VerandaSide.BACK, vLenM, area.verandaState.backType),
                        Triple(VerandaSide.LEFT, vWidM, area.verandaState.leftType),
                        Triple(VerandaSide.RIGHT, vWidM, area.verandaState.rightType)
                    )

                    // 1. Opening (Step) sides
                    for ((side, lengthM, type) in sideConfigs) {
                        if (type == VerandaSideType.OPENING_STEP) {
                            areaStepFaceAreaM2 += (lengthM * stepEdgeHeightM)
                            val stripRes = TileFormulas.calculateStrips(
                                tileLengthM = floorTileLengthM,
                                tileWidthM = floorTileWidthM,
                                stripHeightM = stepEdgeHeightM,
                                runLengthM = lengthM,
                                label = "Step face, ${side.label.lowercase()}",
                                areaName = area.name,
                                sideName = "${side.label.lowercase()} step"
                            )
                            stripRes.warning?.let { if (!warnings.contains(it)) warnings.add(it) }
                            areaStepFaceTiles += stripRes.tilesNeeded
                            areaRunSummaries.add(stripRes.methodSummary)
                            jobRunSummaries.add(stripRes.methodSummary)

                            stepFaceFormulaDetails.append("• ${area.name} ${side.label} Step Face (%.2f m):\n".format(lengthM))
                            stepFaceFormulaDetails.append("  ").append(stripRes.methodSummary).append("\n\n")
                        }
                    }

                    // 2. Wall sides & Door
                    val doorSide = if (area.verandaState.hasDoor) area.verandaState.doorSide else null
                    val doorWidthM = unit.toMeters(area.verandaState.doorWidth.toDoubleOrNull() ?: 0.0)
                    val doorStripHeightM = resolveHeightMeters(area.verandaState.doorStripHeight, unit)

                    for ((side, lengthM, type) in sideConfigs) {
                        if (type == VerandaSideType.WALL) {
                            val hasDoorOnThisSide = (doorSide == side)
                            if (hasDoorOnThisSide) {
                                val effectiveDoorWidthM = doorWidthM.coerceAtMost(lengthM)
                                val remainingWallRunM = max(0.0, lengthM - effectiveDoorWidthM)

                                areaWallSkirtingAreaM2 += (effectiveDoorWidthM * doorStripHeightM)
                                val doorStripRes = TileFormulas.calculateStrips(
                                    tileLengthM = floorTileLengthM,
                                    tileWidthM = floorTileWidthM,
                                    stripHeightM = doorStripHeightM,
                                    runLengthM = effectiveDoorWidthM,
                                    label = "Tile height at the door",
                                    areaName = area.name,
                                    sideName = "door strip"
                                )
                                doorStripRes.warning?.let { if (!warnings.contains(it)) warnings.add(it) }
                                areaWallSkirtingTiles += doorStripRes.tilesNeeded
                                areaRunSummaries.add(doorStripRes.methodSummary)
                                jobRunSummaries.add(doorStripRes.methodSummary)

                                if (area.verandaState.hasWallSkirting && remainingWallRunM > 0.0) {
                                    areaWallSkirtingAreaM2 += (remainingWallRunM * wallSkirtingHeightM)
                                    verandaWallSkirtingRunSumM += remainingWallRunM
                                    val restStripRes = TileFormulas.calculateStrips(
                                        tileLengthM = floorTileLengthM,
                                        tileWidthM = floorTileWidthM,
                                        stripHeightM = wallSkirtingHeightM,
                                        runLengthM = remainingWallRunM,
                                        label = "${side.label} wall skirting",
                                        areaName = area.name,
                                        sideName = "${side.label.lowercase()} wall skirting"
                                    )
                                    restStripRes.warning?.let { if (!warnings.contains(it)) warnings.add(it) }
                                    areaWallSkirtingTiles += restStripRes.tilesNeeded
                                    areaRunSummaries.add(restStripRes.methodSummary)
                                    jobRunSummaries.add(restStripRes.methodSummary)
                                }
                            } else {
                                if (area.verandaState.hasWallSkirting) {
                                    areaWallSkirtingAreaM2 += (lengthM * wallSkirtingHeightM)
                                    verandaWallSkirtingRunSumM += lengthM
                                    val stripRes = TileFormulas.calculateStrips(
                                        tileLengthM = floorTileLengthM,
                                        tileWidthM = floorTileWidthM,
                                        stripHeightM = wallSkirtingHeightM,
                                        runLengthM = lengthM,
                                        label = "${side.label} wall skirting",
                                        areaName = area.name,
                                        sideName = "${side.label.lowercase()} wall skirting"
                                    )
                                    stripRes.warning?.let { if (!warnings.contains(it)) warnings.add(it) }
                                    areaWallSkirtingTiles += stripRes.tilesNeeded
                                    areaRunSummaries.add(stripRes.methodSummary)
                                    jobRunSummaries.add(stripRes.methodSummary)
                                }
                            }
                        }
                    }

                    val areaTrimRuns = calculateAreaTrims(area, unit)
                    allTrimRuns.addAll(areaTrimRuns)

                    totalStepFaceTilesSum += areaStepFaceTiles
                    totalSkirtingTilesSum += areaWallSkirtingTiles
                    totalSkirtingAndStepAreaM2 += (areaStepFaceAreaM2 + areaWallSkirtingAreaM2)

                    val baseTilesForArea = if (floorTileAreaM2 > 0.0) {
                        safeCeil(areaFloorM2 / floorTileAreaM2)
                    } else 0
                    val rawTilesForArea = if (floorTileAreaM2 > 0.0) {
                        safeCeil((areaFloorM2 * (1.0 + wasteFraction)) / floorTileAreaM2)
                    } else 0
                    val wasteTilesForArea = max(0, rawTilesForArea - baseTilesForArea)

                    areaBreakdowns.add(
                        AreaBreakdownItem(
                            areaId = area.id,
                            areaName = area.name,
                            spaceType = SpaceType.VERANDA,
                            netFloorAreaM2 = areaFloorM2,
                            rawFloorTiles = rawTilesForArea,
                            skirtingTiles = areaWallSkirtingTiles,
                            stepFaceTiles = areaStepFaceTiles,
                            wallTiles = 0,
                            totalAreaTiles = rawTilesForArea + areaWallSkirtingTiles + areaStepFaceTiles,
                            skirtingAndStepAreaM2 = areaStepFaceAreaM2 + areaWallSkirtingAreaM2,
                            netWallAreaM2 = 0.0,
                            summaryText = "${formatAreaSize(area, unit)} • %.2f m²".format(java.util.Locale.US, areaFloorM2),
                            baseFloorTiles = baseTilesForArea,
                            floorWasteAllowanceTiles = wasteTilesForArea,
                            runMethodSummaries = areaRunSummaries,
                            trimRuns = areaTrimRuns
                        )
                    )
                }

                SpaceType.BATHROOM -> {
                    val bLenM = unit.toMeters(area.bathroomState.length.toDoubleOrNull() ?: 0.0)
                    val bWidM = unit.toMeters(area.bathroomState.width.toDoubleOrNull() ?: 0.0)
                    val areaFloorM2 = bLenM * bWidM
                    totalNetFloorAreaM2 += areaFloorM2

                    var areaSkirtingTiles = 0
                    var areaSkirtingAreaM2 = 0.0
                    var areaSkirtingRunM = 0.0

                    if (area.bathroomState.hasSkirting) {
                        val skirtingHeightM = resolveSkirtingHeightMeters(
                            area.bathroomState.skirtingPreset,
                            area.bathroomState.customSkirtingHeight,
                            unit
                        )
                        val perimeterM = 2.0 * (bLenM + bWidM)
                        val totalDoorwayWidthM = area.bathroomState.doorwayWidths.sumOf {
                            unit.toMeters(it.toDoubleOrNull() ?: 0.0)
                        }
                        areaSkirtingRunM = max(0.0, perimeterM - totalDoorwayWidthM)
                        areaSkirtingAreaM2 = areaSkirtingRunM * skirtingHeightM

                        val stripRes = TileFormulas.calculateStrips(
                            tileLengthM = floorTileLengthM,
                            tileWidthM = floorTileWidthM,
                            stripHeightM = skirtingHeightM,
                            runLengthM = areaSkirtingRunM,
                            label = "${area.name} skirting",
                            areaName = area.name,
                            sideName = "skirting"
                        )
                        stripRes.warning?.let { if (!warnings.contains(it)) warnings.add(it) }
                        areaSkirtingTiles = stripRes.tilesNeeded
                        totalSkirtingTilesSum += areaSkirtingTiles
                        totalSkirtingAndStepAreaM2 += areaSkirtingAreaM2
                        areaRunSummaries.add(stripRes.methodSummary)
                        jobRunSummaries.add(stripRes.methodSummary)

                        skirtingFormulaDetails.append("• ${area.name} Skirting:\n")
                        skirtingFormulaDetails.append("  Perimeter %.2f m minus doors %.2f m = %.2f m\n".format(perimeterM, totalDoorwayWidthM, areaSkirtingRunM))
                        skirtingFormulaDetails.append("  ").append(stripRes.formulaText.replace("\n", "\n  ")).append("\n\n")
                    }

                    // Wall Tiling
                    var areaWallTiles = 0
                    var areaNetWallM2 = 0.0

                    if (area.bathroomState.tileWallsToo) {
                        hasWallTiles = true
                        val resolvedWallDims = resolveTileDimensionsMeters(
                            area.bathroomState.wallTilePreset,
                            area.bathroomState.customWallTileLength,
                            area.bathroomState.customWallTileWidth,
                            unit
                        )
                        wallTileLengthM = resolvedWallDims.first
                        wallTileWidthM = resolvedWallDims.second
                        val areaWallTileAreaM2 = wallTileLengthM * wallTileWidthM
                        wallTilesPerBox = area.bathroomState.wallTilesPerBox.toIntOrNull()?.coerceAtLeast(1) ?: 1

                        wallTileLabel = if (area.bathroomState.wallTilePreset != TilePreset.CUSTOM) {
                            area.bathroomState.wallTilePreset.label
                        } else {
                            "${area.bathroomState.customWallTileLength} × ${area.bathroomState.customWallTileWidth} ${unit.symbol}"
                        }

                        val grossWallAreaM2 = area.bathroomState.walls.sumOf { wall ->
                            val w = unit.toMeters(wall.width.toDoubleOrNull() ?: 0.0)
                            val h = unit.toMeters(wall.height.toDoubleOrNull() ?: 0.0)
                            w * h
                        }
                        val windowsAreaM2 = area.bathroomState.windows.sumOf { win ->
                            val w = unit.toMeters(win.width.toDoubleOrNull() ?: 0.0)
                            val h = unit.toMeters(win.height.toDoubleOrNull() ?: 0.0)
                            w * h
                        }
                        val doorsAreaM2 = area.bathroomState.doors.sumOf { door ->
                            val w = unit.toMeters(door.width.toDoubleOrNull() ?: 0.0)
                            val h = unit.toMeters(door.height.toDoubleOrNull() ?: 0.0)
                            w * h
                        }
                        val bathtubAreaM2 = if (area.bathroomState.hasBathtub) {
                            val h = unit.toMeters(area.bathroomState.bathtubHeight.toDoubleOrNull() ?: 0.0)
                            val w = unit.toMeters(area.bathroomState.bathtubWidth.toDoubleOrNull() ?: 0.0)
                            h * w
                        } else 0.0

                        areaNetWallM2 = max(0.0, grossWallAreaM2 - windowsAreaM2 - doorsAreaM2 + bathtubAreaM2)
                        totalNetWallAreaM2 += areaNetWallM2

                        if (areaWallTileAreaM2 > 0.0) {
                            areaWallTiles = safeCeil((areaNetWallM2 * (1.0 + wasteFraction)) / areaWallTileAreaM2)
                        }
                        totalWallTilesSum += areaWallTiles

                        wallFormulaDetails.append("• ${area.name} Wall Area: Net = %.2f m² (%d walls)\n".format(areaNetWallM2, area.bathroomState.walls.size))
                    }

                    val areaTrimRuns = calculateAreaTrims(area, unit)
                    allTrimRuns.addAll(areaTrimRuns)

                    val baseTilesForArea = if (floorTileAreaM2 > 0.0) {
                        safeCeil(areaFloorM2 / floorTileAreaM2)
                    } else 0
                    val rawTilesForArea = if (floorTileAreaM2 > 0.0) {
                        safeCeil((areaFloorM2 * (1.0 + wasteFraction)) / floorTileAreaM2)
                    } else 0
                    val wasteTilesForArea = max(0, rawTilesForArea - baseTilesForArea)

                    areaBreakdowns.add(
                        AreaBreakdownItem(
                            areaId = area.id,
                            areaName = area.name,
                            spaceType = SpaceType.BATHROOM,
                            netFloorAreaM2 = areaFloorM2,
                            rawFloorTiles = rawTilesForArea,
                            skirtingTiles = areaSkirtingTiles,
                            stepFaceTiles = 0,
                            wallTiles = areaWallTiles,
                            totalAreaTiles = rawTilesForArea + areaSkirtingTiles + areaWallTiles,
                            skirtingAndStepAreaM2 = areaSkirtingAreaM2,
                            netWallAreaM2 = areaNetWallM2,
                            summaryText = "${formatAreaSize(area, unit)} • %.2f m²".format(java.util.Locale.US, areaFloorM2),
                            baseFloorTiles = baseTilesForArea,
                            floorWasteAllowanceTiles = wasteTilesForArea,
                            runMethodSummaries = areaRunSummaries,
                            trimRuns = areaTrimRuns
                        )
                    )
                }
            }
        }

        // Apply waste % ONCE to the total net floor area
        val baseFloorTiles = if (floorTileAreaM2 > 0.0) {
            safeCeil(totalNetFloorAreaM2 / floorTileAreaM2)
        } else 0

        val floorTilesRawNeeded = if (floorTileAreaM2 > 0.0) {
            safeCeil((totalNetFloorAreaM2 * (1.0 + wasteFraction)) / floorTileAreaM2)
        } else 0

        val floorWasteAllowanceTiles = max(0, floorTilesRawNeeded - baseFloorTiles)

        val totalFloorTilesNeeded = floorTilesRawNeeded + totalSkirtingTilesSum + totalStepFaceTilesSum
        // Convert to boxes only ONCE, at the end. Never round up to boxes per area.
        val floorBoxesToBuy = safeCeil(totalFloorTilesNeeded.toDouble() / tilesPerBox.toDouble())
        val floorSpareTiles = (floorBoxesToBuy * tilesPerBox) - totalFloorTilesNeeded

        // Wall boxes (only if wall tiles are on)
        val wallTileAreaM2 = wallTileLengthM * wallTileWidthM
        val baseWallTiles = if (hasWallTiles && wallTileAreaM2 > 0.0) {
            safeCeil(totalNetWallAreaM2 / wallTileAreaM2)
        } else 0
        val wallWasteAllowanceTiles = max(0, totalWallTilesSum - baseWallTiles)

        val wallBoxesToBuy = if (hasWallTiles && wallTilesPerBox > 0) {
            safeCeil(totalWallTilesSum.toDouble() / wallTilesPerBox.toDouble())
        } else 0
        val wallSpareTiles = if (hasWallTiles) (wallBoxesToBuy * wallTilesPerBox) - totalWallTilesSum else 0

        if (hasWallTiles) {
            wallFormulaDetails.append("• Total Net Wall Area = %.2f m²\n".format(totalNetWallAreaM2))
            wallFormulaDetails.append("• Wall Tile Area = %.3f m × %.3f m = %.4f m²\n".format(wallTileLengthM, wallTileWidthM, wallTileAreaM2))
            wallFormulaDetails.append("• Base wall tiles (no waste) = ceil(%.2f m² / %.4f m²) = %d tiles\n".format(totalNetWallAreaM2, wallTileAreaM2, baseWallTiles))
            wallFormulaDetails.append("• Wall tiles needed with waste = %d tiles\n".format(totalWallTilesSum))
            wallFormulaDetails.append("• Extra for cuts and breakage (%d%%): %d tiles\n".format(job.tileSettings.wastePercentage, wallWasteAllowanceTiles))
            wallFormulaDetails.append("• Boxes to buy (whole job converted once) = ceil(%d / %d) = %d boxes\n".format(totalWallTilesSum, wallTilesPerBox, wallBoxesToBuy))
            wallFormulaDetails.append("• Spare tiles left over after boxes = (%d × %d) - %d = %d spare".format(wallBoxesToBuy, wallTilesPerBox, totalWallTilesSum, wallSpareTiles))
        }

        val totalBoxesToBuy = floorBoxesToBuy + wallBoxesToBuy
        val totalTilesCount = totalFloorTilesNeeded + totalWallTilesSum

        // Build floor formula explanation
        val floorFormulaExplanation = buildString {
            append("• Total Net Floor Area = %s = %.3f m²\n".format(
                areaBreakdowns.joinToString(" + ") { "%.2f m² (%s)".format(it.netFloorAreaM2, it.areaName) },
                totalNetFloorAreaM2
            ))
            append("• Floor Tile Area = Tile Length × Tile Width = %.3f m × %.3f m = %.4f m²\n".format(floorTileLengthM, floorTileWidthM, floorTileAreaM2))
            append("• Base floor tiles (no waste) = ceil(Total Net Floor Area / Tile Area) = ceil(%.3f m² / %.4f m²) = %d tiles\n".format(totalNetFloorAreaM2, floorTileAreaM2, baseFloorTiles))
            append("• Floor tiles needed with waste = ceil((Total Net Floor Area × (1 + Waste%%)) / Tile Area)\n")
            append("  = ceil((%.3f m² × (1 + %.2f)) / %.4f m²)\n".format(totalNetFloorAreaM2, wasteFraction, floorTileAreaM2))
            append("  = ceil(%.3f m² / %.4f m²) = %d tiles\n".format(totalNetFloorAreaM2 * (1.0 + wasteFraction), floorTileAreaM2, floorTilesRawNeeded))
            append("• Extra for cuts and breakage (%d%%): %d tiles\n".format(job.tileSettings.wastePercentage, floorWasteAllowanceTiles))
            if (totalSkirtingTilesSum > 0 || totalStepFaceTilesSum > 0) {
                append("• Total floor tiles before boxing = %d floor + %d skirting + %d step-face = %d tiles\n"
                    .format(floorTilesRawNeeded, totalSkirtingTilesSum, totalStepFaceTilesSum, totalFloorTilesNeeded))
                append("  (Note: Waste applies only to floor and wall areas. Strip and step-face tiles already count their own cut leftovers, so no waste is added to them.)\n")
            }
            append("• Boxes to buy (whole job converted once) = ceil(%d / %d) = %d boxes\n".format(totalFloorTilesNeeded, tilesPerBox, floorBoxesToBuy))
            append("• Spare tiles left over after boxes = (%d × %d) - %d = %d spare".format(floorBoxesToBuy, tilesPerBox, totalFloorTilesNeeded, floorSpareTiles))
        }

        // Materials: whole job totals
        val materials = calculateMaterials(
            floorAreaM2 = totalNetFloorAreaM2,
            skirtingAndStepAreaM2 = totalSkirtingAndStepAreaM2,
            wallAreaM2 = totalNetWallAreaM2,
            floorTileLengthM = floorTileLengthM,
            floorTileWidthM = floorTileWidthM,
            hasWallTiles = hasWallTiles,
            wallTileLengthM = wallTileLengthM,
            wallTileWidthM = wallTileWidthM,
            totalTilesCount = totalTilesCount,
            materialSettings = job.materialSettings
        )

        // Outer corners & Custom trim runs from Job
        job.trimSettings.outerCorners.forEach { corner ->
            val lenM = unit.toMeters(corner.lengthM.toDoubleOrNull() ?: 0.0)
            if (lenM > 0.0) {
                allTrimRuns.add(
                    TrimRunItem(
                        id = corner.id,
                        name = corner.name.ifBlank { "Outer Corner" },
                        lengthM = lenM,
                        material = corner.material,
                        isEnabled = corner.isEnabled
                    )
                )
            }
        }
        job.trimSettings.customRuns.forEach { custom ->
            val lenM = unit.toMeters(custom.lengthM.toDoubleOrNull() ?: 0.0)
            if (lenM > 0.0) {
                allTrimRuns.add(
                    TrimRunItem(
                        id = custom.id,
                        name = custom.name.ifBlank { "Custom Trim Run" },
                        lengthM = lenM,
                        material = custom.material,
                        isEnabled = custom.isEnabled
                    )
                )
            }
        }

        val pieceLengthM = (job.trimSettings.pieceLengthM.toDoubleOrNull() ?: 2.5).coerceAtLeast(0.1)
        val allowanceFraction = job.trimSettings.allowancePercent / 100.0

        val plasticRuns = allTrimRuns.filter { it.isEnabled && it.material == TrimMaterial.PLASTIC }
        val metalRuns = allTrimRuns.filter { it.isEnabled && it.material == TrimMaterial.METAL }

        val plasticTotalRunM = plasticRuns.sumOf { it.lengthM }
        val metalTotalRunM = metalRuns.sumOf { it.lengthM }

        val plasticPieces = if (plasticTotalRunM > 0.0) {
            safeCeil((plasticTotalRunM * (1.0 + allowanceFraction)) / pieceLengthM)
        } else 0
        val metalPieces = if (metalTotalRunM > 0.0) {
            safeCeil((metalTotalRunM * (1.0 + allowanceFraction)) / pieceLengthM)
        } else 0

        val plasticFormula = buildString {
            append("• Plastic trim run = ")
            if (plasticRuns.isEmpty()) {
                append("0.00 m\n")
            } else {
                append(plasticRuns.joinToString(" + ") { "%.2f m (%s)".format(it.lengthM, it.displayText.ifBlank { it.name }) })
                append(" = %.2f m\n".format(plasticTotalRunM))
            }
            append("• Formula: ceil(total run × (1 + allowance%%) / piece length)\n")
            append("  = ceil(%.2f m × (1 + %.2f) / %.2f m)\n".format(plasticTotalRunM, allowanceFraction, pieceLengthM))
            append("  = ceil(%.2f / %.2f) = %d pieces (%.1f m)".format(plasticTotalRunM * (1.0 + allowanceFraction), pieceLengthM, plasticPieces, pieceLengthM))
        }

        val metalFormula = buildString {
            append("• Metal trim run = ")
            if (metalRuns.isEmpty()) {
                append("0.00 m\n")
            } else {
                append(metalRuns.joinToString(" + ") { "%.2f m (%s)".format(it.lengthM, it.displayText.ifBlank { it.name }) })
                append(" = %.2f m\n".format(metalTotalRunM))
            }
            append("• Formula: ceil(total run × (1 + allowance%%) / piece length)\n")
            append("  = ceil(%.2f m × (1 + %.2f) / %.2f m)\n".format(metalTotalRunM, allowanceFraction, pieceLengthM))
            append("  = ceil(%.2f / %.2f) = %d pieces (%.1f m)".format(metalTotalRunM * (1.0 + allowanceFraction), pieceLengthM, metalPieces, pieceLengthM))
        }

        val trims = TrimCalculationResult(
            pieceLengthM = pieceLengthM,
            allowancePercent = job.trimSettings.allowancePercent,
            plasticTotalRunLengthM = plasticTotalRunM,
            plasticPiecesNeeded = plasticPieces,
            plasticFormulaExplanation = plasticFormula,
            metalTotalRunLengthM = metalTotalRunM,
            metalPiecesNeeded = metalPieces,
            metalFormulaExplanation = metalFormula,
            activeRuns = allTrimRuns.filter { it.isEnabled }
        )

        return CalculationResult(
            spaceType = SpaceType.ROOM_FLOOR,
            floorTileLabel = floorTileLabel,
            floorTilesPerBoxCount = tilesPerBox,
            floorAreaM2 = totalNetFloorAreaM2,
            floorTileAreaM2 = floorTileAreaM2,
            baseFloorTiles = baseFloorTiles,
            floorTilesRawNeeded = floorTilesRawNeeded,
            floorWasteAllowanceTiles = floorWasteAllowanceTiles,
            wastePercentage = job.tileSettings.wastePercentage,
            skirtingTilesNeeded = totalSkirtingTilesSum,
            stepFaceTilesNeeded = totalStepFaceTilesSum,
            totalFloorTilesNeeded = totalFloorTilesNeeded,
            floorBoxesToBuy = floorBoxesToBuy,
            floorSpareTiles = floorSpareTiles,
            floorFormulaExplanation = floorFormulaExplanation,
            floorSkirtingFormulaExplanation = skirtingFormulaDetails.toString(),
            floorStepFaceFormulaExplanation = stepFaceFormulaDetails.toString(),
            warnings = warnings,
            hasWallTiles = hasWallTiles,
            wallTileLabel = wallTileLabel,
            wallTilesPerBoxCount = wallTilesPerBox,
            netWallAreaM2 = totalNetWallAreaM2,
            wallTileAreaM2 = wallTileAreaM2,
            baseWallTiles = baseWallTiles,
            wallTilesNeeded = totalWallTilesSum,
            wallWasteAllowanceTiles = wallWasteAllowanceTiles,
            wallBoxesToBuy = wallBoxesToBuy,
            wallSpareTiles = wallSpareTiles,
            wallFormulaExplanation = wallFormulaDetails.toString(),
            totalBoxesToBuy = totalBoxesToBuy,
            totalTilesCount = totalTilesCount,
            materials = materials,
            trims = trims,
            areaBreakdowns = areaBreakdowns,
            runMethodSummaries = jobRunSummaries
        )
    }
}

