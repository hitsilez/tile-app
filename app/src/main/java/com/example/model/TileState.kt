package com.example.model

data class OpeningDimension(
    val id: String,
    val width: String,
    val height: String
)

data class WallDimension(
    val id: String,
    val label: String,
    val width: String,
    val height: String
)

data class FloorTileSettingsState(
    val preset: TilePreset = TilePreset.SIZE_60X60,
    val customLength: String = "60",
    val customWidth: String = "60",
    val tilesPerBox: String = "4",
    val wastePercentage: Int = 10
)

data class MaterialSettingsState(
    // ADHESIVE (cement-based, 20 kg bags)
    val baseCoverageAt5mm: String = "3.0", // m² covered per 20 kg bag at 5 mm bed (default 3.0)
    val floorBedPreset: FloorBedThicknessPreset = FloorBedThicknessPreset.LEVEL_5MM,
    val customFloorBedMm: String = "5",
    val wallBedPreset: WallBedThicknessPreset = WallBedThicknessPreset.BED_6MM,
    val customWallBedMm: String = "6",

    // GROUT & SPACERS
    val jointWidthMm: Double = 3.0, // Shared joint size J (1.5, 2, 3, 4, 5, 6, 8, 10 mm)
    val jointDepthMm: String = "10", // Joint depth D in mm (default 10)
    val groutType: GroutType = GroutType.CEMENT, // Cement (1.6 kg/L) or Epoxy (1.5 kg/L)
    val groutBagSize: GroutBagSize = GroutBagSize.BAG_5KG, // 5 kg or 2 kg

    // SPACERS
    val spacersPerTile: String = "2", // default 2
    val spacerPackSize: String = "100", // default 100

    // BONDING LIQUID
    val bondingMode: BondingLiquidMode = BondingLiquidMode.ADDITIVE_SPLASH,
    val additiveMlPerBag: String = "250", // ml per 20 kg bag for additive (default 250)
    val replacementLitersPerBag: String = "5" // L per 20 kg bag for full replacement (default 5)
)

data class OuterCornerItem(
    val id: String,
    val name: String,
    val lengthM: String,
    val material: TrimMaterial = TrimMaterial.PLASTIC,
    val isEnabled: Boolean = true
)

data class CustomTrimRunItem(
    val id: String,
    val name: String,
    val lengthM: String,
    val material: TrimMaterial = TrimMaterial.PLASTIC,
    val isEnabled: Boolean = true
)

data class VerandaStepTrimConfig(
    val isEnabled: Boolean = true,
    val material: TrimMaterial = TrimMaterial.METAL
)

data class TrimSettingsState(
    val pieceLengthM: String = "2.5", // default 2.5 m
    val allowancePercent: Int = 10,   // default 10%

    // Veranda opening step edge runs (each side marked Opening (step))
    val verandaStepFront: VerandaStepTrimConfig = VerandaStepTrimConfig(isEnabled = true, material = TrimMaterial.METAL),
    val verandaStepBack: VerandaStepTrimConfig = VerandaStepTrimConfig(isEnabled = true, material = TrimMaterial.METAL),
    val verandaStepLeft: VerandaStepTrimConfig = VerandaStepTrimConfig(isEnabled = true, material = TrimMaterial.METAL),
    val verandaStepRight: VerandaStepTrimConfig = VerandaStepTrimConfig(isEnabled = true, material = TrimMaterial.METAL),

    // Skirting top edge (room, bathroom, veranda wall sides)
    val hasSkirtingTopTrim: Boolean = false, // Show a switch: "Trim on top of skirting"
    val skirtingTopMaterial: TrimMaterial = TrimMaterial.PLASTIC, // Default Plastic

    // Bathroom walls top edge
    val hasBathroomWallTopTrim: Boolean = false, // Show a switch: "Trim on top of wall tiles"
    val bathroomWallTopMaterial: TrimMaterial = TrimMaterial.PLASTIC, // Default Plastic

    // Outer corners
    val outerCorners: List<OuterCornerItem> = emptyList(),

    // Custom trim runs
    val customRuns: List<CustomTrimRunItem> = emptyList()
)

data class RoomFloorState(
    val length: String = "4",
    val width: String = "3",
    val doorwayWidths: List<String> = emptyList(),
    val hasSkirting: Boolean = false,
    val skirtingPreset: SkirtingPreset = SkirtingPreset.HEIGHT_10CM,
    val customSkirtingHeight: String = "10",
    // Trims for Room Floor / Passage
    val hasAlongSkirtingTrim: Boolean = true,
    val alongSkirtingTrimMaterial: TrimMaterial = TrimMaterial.PLASTIC,
    val doorTrimEnabled: List<Boolean> = emptyList(),
    val doorTrimMaterials: List<TrimMaterial> = emptyList()
) {
    fun isDoorTrimEnabled(index: Int): Boolean = doorTrimEnabled.getOrNull(index) ?: false
    fun getDoorTrimMaterial(index: Int): TrimMaterial = doorTrimMaterials.getOrNull(index) ?: TrimMaterial.PLASTIC

    fun withDoorTrimToggled(index: Int, enabled: Boolean): RoomFloorState {
        val newEnabled = doorTrimEnabled.toMutableList()
        while (newEnabled.size <= index) newEnabled.add(false)
        newEnabled[index] = enabled
        return copy(doorTrimEnabled = newEnabled)
    }

    fun withDoorTrimMaterial(index: Int, mat: TrimMaterial): RoomFloorState {
        val newMats = doorTrimMaterials.toMutableList()
        while (newMats.size <= index) newMats.add(TrimMaterial.PLASTIC)
        newMats[index] = mat
        return copy(doorTrimMaterials = newMats)
    }
}

data class VerandaState(
    val length: String = "5",
    val width: String = "2.5",
    val frontType: VerandaSideType = VerandaSideType.OPENING_STEP,
    val backType: VerandaSideType = VerandaSideType.WALL,
    val leftType: VerandaSideType = VerandaSideType.OPENING_STEP,
    val rightType: VerandaSideType = VerandaSideType.WALL,
    val stepEdgeHeight: String = "15", // usually in cm if cm, or current unit
    val hasWallSkirting: Boolean = true,
    val wallSkirtingPreset: SkirtingPreset = SkirtingPreset.HEIGHT_10CM,
    val customWallSkirtingHeight: String = "10",
    val hasDoor: Boolean = false,
    val doorSide: VerandaSide = VerandaSide.BACK,
    val doorWidth: String = "0.9",
    val doorStripHeight: String = "15",
    // Trims for 4 sides
    val frontTrimEnabled: Boolean = true,
    val frontTrimMaterial: TrimMaterial? = null,
    val backTrimEnabled: Boolean = true,
    val backTrimMaterial: TrimMaterial? = null,
    val leftTrimEnabled: Boolean = true,
    val leftTrimMaterial: TrimMaterial? = null,
    val rightTrimEnabled: Boolean = true,
    val rightTrimMaterial: TrimMaterial? = null,
    // Pillars
    val pillars: List<PillarItem> = emptyList()
) {
    fun getSideTrimMaterial(side: VerandaSide): TrimMaterial {
        val configured = when (side) {
            VerandaSide.FRONT -> frontTrimMaterial
            VerandaSide.BACK -> backTrimMaterial
            VerandaSide.LEFT -> leftTrimMaterial
            VerandaSide.RIGHT -> rightTrimMaterial
        }
        if (configured != null) return configured
        val type = when (side) {
            VerandaSide.FRONT -> frontType
            VerandaSide.BACK -> backType
            VerandaSide.LEFT -> leftType
            VerandaSide.RIGHT -> rightType
        }
        return if (type == VerandaSideType.OPENING_STEP) TrimMaterial.METAL else TrimMaterial.PLASTIC
    }

    fun isSideTrimEnabled(side: VerandaSide): Boolean = when (side) {
        VerandaSide.FRONT -> frontTrimEnabled
        VerandaSide.BACK -> backTrimEnabled
        VerandaSide.LEFT -> leftTrimEnabled
        VerandaSide.RIGHT -> rightTrimEnabled
    }

    fun withSideTrimToggled(side: VerandaSide, enabled: Boolean): VerandaState = when (side) {
        VerandaSide.FRONT -> copy(frontTrimEnabled = enabled)
        VerandaSide.BACK -> copy(backTrimEnabled = enabled)
        VerandaSide.LEFT -> copy(leftTrimEnabled = enabled)
        VerandaSide.RIGHT -> copy(rightTrimEnabled = enabled)
    }

    fun withSideTrimMaterial(side: VerandaSide, mat: TrimMaterial): VerandaState = when (side) {
        VerandaSide.FRONT -> copy(frontTrimMaterial = mat)
        VerandaSide.BACK -> copy(backTrimMaterial = mat)
        VerandaSide.LEFT -> copy(leftTrimMaterial = mat)
        VerandaSide.RIGHT -> copy(rightTrimMaterial = mat)
    }

    fun withPillarCount(count: Int): VerandaState {
        val current = pillars.toMutableList()
        val target = count.coerceAtLeast(0)
        if (target > current.size) {
            for (i in current.size until target) {
                current.add(
                    PillarItem(
                        id = java.util.UUID.randomUUID().toString(),
                        widthCm = "30",
                        depthCm = "30",
                        material = TrimMaterial.METAL,
                        isEnabled = true,
                        removeFromEdge = false,
                        edgeSide = VerandaSide.FRONT
                    )
                )
            }
        } else if (target < current.size) {
            while (current.size > target) {
                current.removeAt(current.size - 1)
            }
        }
        return copy(pillars = current)
    }
}

data class BathroomState(
    // Floor
    val length: String = "2.5",
    val width: String = "2.0",
    val doorwayWidths: List<String> = listOf("0.8"),
    val hasSkirting: Boolean = false,
    val skirtingPreset: SkirtingPreset = SkirtingPreset.HEIGHT_7CM,
    val customSkirtingHeight: String = "7",
    // Floor Trims
    val hasAlongSkirtingTrim: Boolean = true,
    val alongSkirtingTrimMaterial: TrimMaterial = TrimMaterial.PLASTIC,
    val doorTrimEnabled: List<Boolean> = emptyList(),
    val doorTrimMaterials: List<TrimMaterial> = emptyList(),

    // Walls
    val tileWallsToo: Boolean = false,
    val wallTilePreset: TilePreset = TilePreset.SIZE_30X30,
    val customWallTileLength: String = "30",
    val customWallTileWidth: String = "30",
    val wallTilesPerBox: String = "10",
    val walls: List<WallDimension> = listOf(
        WallDimension("w1", "Wall 1 (Front)", "2.5", "2.4"),
        WallDimension("w2", "Wall 2 (Back)", "2.5", "2.4"),
        WallDimension("w3", "Wall 3 (Left)", "2.0", "2.4"),
        WallDimension("w4", "Wall 4 (Right)", "2.0", "2.4")
    ),
    val windows: List<OpeningDimension> = listOf(
        OpeningDimension("win1", "0.6", "0.6")
    ),
    val doors: List<OpeningDimension> = listOf(
        OpeningDimension("door1", "0.8", "2.0")
    ),
    val hasBathtub: Boolean = false,
    val bathtubHeight: String = "0.6",
    val bathtubWidth: String = "1.7",
    // Wall Top Trim
    val hasWallTopTrim: Boolean = false,
    val wallTopTrimMaterial: TrimMaterial = TrimMaterial.PLASTIC
) {
    fun isDoorTrimEnabled(index: Int): Boolean = doorTrimEnabled.getOrNull(index) ?: false
    fun getDoorTrimMaterial(index: Int): TrimMaterial = doorTrimMaterials.getOrNull(index) ?: TrimMaterial.PLASTIC

    fun withDoorTrimToggled(index: Int, enabled: Boolean): BathroomState {
        val newEnabled = doorTrimEnabled.toMutableList()
        while (newEnabled.size <= index) newEnabled.add(false)
        newEnabled[index] = enabled
        return copy(doorTrimEnabled = newEnabled)
    }

    fun withDoorTrimMaterial(index: Int, mat: TrimMaterial): BathroomState {
        val newMats = doorTrimMaterials.toMutableList()
        while (newMats.size <= index) newMats.add(TrimMaterial.PLASTIC)
        newMats[index] = mat
        return copy(doorTrimMaterials = newMats)
    }
}
