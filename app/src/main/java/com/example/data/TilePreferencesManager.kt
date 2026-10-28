package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.*

class TilePreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("tile_calculator_storage", Context.MODE_PRIVATE)

    fun saveUnit(unit: UnitSystem) {
        prefs.edit().putString("key_unit", unit.name).apply()
    }

    fun loadUnit(): UnitSystem {
        val name = prefs.getString("key_unit", UnitSystem.METERS.name)
        return try {
            UnitSystem.valueOf(name ?: UnitSystem.METERS.name)
        } catch (e: Exception) {
            UnitSystem.METERS
        }
    }

    fun saveSpaceType(spaceType: SpaceType) {
        prefs.edit().putString("key_space_type", spaceType.name).apply()
    }

    fun loadSpaceType(): SpaceType {
        val name = prefs.getString("key_space_type", SpaceType.ROOM_FLOOR.name)
        return try {
            SpaceType.valueOf(name ?: SpaceType.ROOM_FLOOR.name)
        } catch (e: Exception) {
            SpaceType.ROOM_FLOOR
        }
    }

    fun saveFloorTileSettings(settings: FloorTileSettingsState) {
        prefs.edit()
            .putString("key_floor_preset", settings.preset.name)
            .putString("key_floor_custom_len", settings.customLength)
            .putString("key_floor_custom_wid", settings.customWidth)
            .putString("key_floor_tiles_per_box", settings.tilesPerBox)
            .putInt("key_floor_waste", settings.wastePercentage)
            .apply()
    }

    fun loadFloorTileSettings(): FloorTileSettingsState {
        val presetName = prefs.getString("key_floor_preset", TilePreset.SIZE_60X60.name)
        val preset = try {
            TilePreset.valueOf(presetName ?: TilePreset.SIZE_60X60.name)
        } catch (e: Exception) {
            TilePreset.SIZE_60X60
        }
        return FloorTileSettingsState(
            preset = preset,
            customLength = prefs.getString("key_floor_custom_len", "60") ?: "60",
            customWidth = prefs.getString("key_floor_custom_wid", "60") ?: "60",
            tilesPerBox = prefs.getString("key_floor_tiles_per_box", "4") ?: "4",
            wastePercentage = prefs.getInt("key_floor_waste", 10)
        )
    }

    fun saveRoomState(state: RoomFloorState) {
        val doorwayStr = state.doorwayWidths.joinToString(",")
        prefs.edit()
            .putString("key_room_len", state.length)
            .putString("key_room_wid", state.width)
            .putString("key_room_doorways", doorwayStr)
            .putBoolean("key_room_has_skirting", state.hasSkirting)
            .putString("key_room_skirting_preset", state.skirtingPreset.name)
            .putString("key_room_custom_skirting", state.customSkirtingHeight)
            .apply()
    }

    fun loadRoomState(): RoomFloorState {
        val doorwayStr = prefs.getString("key_room_doorways", "") ?: ""
        val doorways = if (doorwayStr.isBlank()) emptyList() else doorwayStr.split(",")
        val skirtingPresetName = prefs.getString("key_room_skirting_preset", SkirtingPreset.HEIGHT_10CM.name)
        val skirtingPreset = try {
            SkirtingPreset.valueOf(skirtingPresetName ?: SkirtingPreset.HEIGHT_10CM.name)
        } catch (e: Exception) {
            SkirtingPreset.HEIGHT_10CM
        }
        return RoomFloorState(
            length = prefs.getString("key_room_len", "4") ?: "4",
            width = prefs.getString("key_room_wid", "3") ?: "3",
            doorwayWidths = doorways,
            hasSkirting = prefs.getBoolean("key_room_has_skirting", false),
            skirtingPreset = skirtingPreset,
            customSkirtingHeight = prefs.getString("key_room_custom_skirting", "10") ?: "10"
        )
    }

    fun saveVerandaState(state: VerandaState) {
        prefs.edit()
            .putString("key_veranda_len", state.length)
            .putString("key_veranda_wid", state.width)
            .putString("key_veranda_front_type", state.frontType.name)
            .putString("key_veranda_back_type", state.backType.name)
            .putString("key_veranda_left_type", state.leftType.name)
            .putString("key_veranda_right_type", state.rightType.name)
            .putString("key_veranda_step_height", state.stepEdgeHeight)
            .putBoolean("key_veranda_wall_skirting", state.hasWallSkirting)
            .putString("key_veranda_skirting_preset", state.wallSkirtingPreset.name)
            .putString("key_veranda_custom_skirting", state.customWallSkirtingHeight)
            .putBoolean("key_veranda_has_door", state.hasDoor)
            .putString("key_veranda_door_side", state.doorSide.name)
            .putString("key_veranda_door_width", state.doorWidth)
            .putString("key_veranda_door_strip_height", state.doorStripHeight)
            .apply()
    }

    fun loadVerandaState(): VerandaState {
        val frontType = safeVerandaSideType(prefs.getString("key_veranda_front_type", null), VerandaSideType.OPENING_STEP)
        val backType = safeVerandaSideType(prefs.getString("key_veranda_back_type", null), VerandaSideType.WALL)
        val leftType = safeVerandaSideType(prefs.getString("key_veranda_left_type", null), VerandaSideType.OPENING_STEP)
        val rightType = safeVerandaSideType(prefs.getString("key_veranda_right_type", null), VerandaSideType.WALL)

        val skirtingPreset = try {
            val name = prefs.getString("key_veranda_skirting_preset", SkirtingPreset.HEIGHT_10CM.name)
            SkirtingPreset.valueOf(name ?: SkirtingPreset.HEIGHT_10CM.name)
        } catch (e: Exception) {
            SkirtingPreset.HEIGHT_10CM
        }

        val doorSide = try {
            val name = prefs.getString("key_veranda_door_side", VerandaSide.BACK.name)
            VerandaSide.valueOf(name ?: VerandaSide.BACK.name)
        } catch (e: Exception) {
            VerandaSide.BACK
        }

        return VerandaState(
            length = prefs.getString("key_veranda_len", "5") ?: "5",
            width = prefs.getString("key_veranda_wid", "2.5") ?: "2.5",
            frontType = frontType,
            backType = backType,
            leftType = leftType,
            rightType = rightType,
            stepEdgeHeight = prefs.getString("key_veranda_step_height", "15") ?: "15",
            hasWallSkirting = prefs.getBoolean("key_veranda_wall_skirting", true),
            wallSkirtingPreset = skirtingPreset,
            customWallSkirtingHeight = prefs.getString("key_veranda_custom_skirting", "10") ?: "10",
            hasDoor = prefs.getBoolean("key_veranda_has_door", false),
            doorSide = doorSide,
            doorWidth = prefs.getString("key_veranda_door_width", "0.9") ?: "0.9",
            doorStripHeight = prefs.getString("key_veranda_door_strip_height", "15") ?: "15"
        )
    }

    private fun safeVerandaSideType(value: String?, default: VerandaSideType): VerandaSideType {
        return try {
            if (value != null) VerandaSideType.valueOf(value) else default
        } catch (e: Exception) {
            default
        }
    }

    fun saveMaterialSettings(state: MaterialSettingsState) {
        prefs.edit()
            .putString("key_mat_base_cov", state.baseCoverageAt5mm)
            .putString("key_mat_floor_bed_preset", state.floorBedPreset.name)
            .putString("key_mat_custom_floor_bed", state.customFloorBedMm)
            .putString("key_mat_wall_bed_preset", state.wallBedPreset.name)
            .putString("key_mat_custom_wall_bed", state.customWallBedMm)
            .putString("key_mat_joint_width", state.jointWidthMm.toString())
            .putString("key_mat_joint_depth", state.jointDepthMm)
            .putString("key_mat_grout_type", state.groutType.name)
            .putString("key_mat_grout_bag_size", state.groutBagSize.name)
            .putString("key_mat_spacers_per_tile", state.spacersPerTile)
            .putString("key_mat_spacer_pack_size", state.spacerPackSize)
            .putString("key_mat_bonding_mode", state.bondingMode.name)
            .putString("key_mat_additive_ml", state.additiveMlPerBag)
            .putString("key_mat_replacement_l", state.replacementLitersPerBag)
            .apply()
    }

    fun loadMaterialSettings(): MaterialSettingsState {
        val floorBedPreset = try {
            val name = prefs.getString("key_mat_floor_bed_preset", FloorBedThicknessPreset.LEVEL_5MM.name)
            FloorBedThicknessPreset.valueOf(name ?: FloorBedThicknessPreset.LEVEL_5MM.name)
        } catch (e: Exception) {
            FloorBedThicknessPreset.LEVEL_5MM
        }

        val wallBedPreset = try {
            val name = prefs.getString("key_mat_wall_bed_preset", WallBedThicknessPreset.BED_6MM.name)
            WallBedThicknessPreset.valueOf(name ?: WallBedThicknessPreset.BED_6MM.name)
        } catch (e: Exception) {
            WallBedThicknessPreset.BED_6MM
        }

        val groutType = try {
            val name = prefs.getString("key_mat_grout_type", GroutType.CEMENT.name)
            GroutType.valueOf(name ?: GroutType.CEMENT.name)
        } catch (e: Exception) {
            GroutType.CEMENT
        }

        val groutBagSize = try {
            val name = prefs.getString("key_mat_grout_bag_size", GroutBagSize.BAG_5KG.name)
            GroutBagSize.valueOf(name ?: GroutBagSize.BAG_5KG.name)
        } catch (e: Exception) {
            GroutBagSize.BAG_5KG
        }

        val bondingMode = try {
            val name = prefs.getString("key_mat_bonding_mode", BondingLiquidMode.ADDITIVE_SPLASH.name)
            BondingLiquidMode.valueOf(name ?: BondingLiquidMode.ADDITIVE_SPLASH.name)
        } catch (e: Exception) {
            BondingLiquidMode.ADDITIVE_SPLASH
        }

        val jointWidth = prefs.getString("key_mat_joint_width", "3.0")?.toDoubleOrNull() ?: 3.0

        return MaterialSettingsState(
            baseCoverageAt5mm = prefs.getString("key_mat_base_cov", "3.0") ?: "3.0",
            floorBedPreset = floorBedPreset,
            customFloorBedMm = prefs.getString("key_mat_custom_floor_bed", "5") ?: "5",
            wallBedPreset = wallBedPreset,
            customWallBedMm = prefs.getString("key_mat_custom_wall_bed", "6") ?: "6",
            jointWidthMm = jointWidth,
            jointDepthMm = prefs.getString("key_mat_joint_depth", "10") ?: "10",
            groutType = groutType,
            groutBagSize = groutBagSize,
            spacersPerTile = prefs.getString("key_mat_spacers_per_tile", "2") ?: "2",
            spacerPackSize = prefs.getString("key_mat_spacer_pack_size", "100") ?: "100",
            bondingMode = bondingMode,
            additiveMlPerBag = prefs.getString("key_mat_additive_ml", "250") ?: "250",
            replacementLitersPerBag = prefs.getString("key_mat_replacement_l", "5") ?: "5"
        )
    }

    fun saveTrimSettings(state: TrimSettingsState) {
        val cornersStr = state.outerCorners.joinToString(";;") { "${it.id}::${it.name}::${it.lengthM}::${it.material.name}::${it.isEnabled}" }
        val customStr = state.customRuns.joinToString(";;") { "${it.id}::${it.name}::${it.lengthM}::${it.material.name}::${it.isEnabled}" }
        prefs.edit()
            .putString("key_trim_piece_len", state.pieceLengthM)
            .putInt("key_trim_allowance", state.allowancePercent)
            .putBoolean("key_trim_vf_en", state.verandaStepFront.isEnabled)
            .putString("key_trim_vf_mat", state.verandaStepFront.material.name)
            .putBoolean("key_trim_vb_en", state.verandaStepBack.isEnabled)
            .putString("key_trim_vb_mat", state.verandaStepBack.material.name)
            .putBoolean("key_trim_vl_en", state.verandaStepLeft.isEnabled)
            .putString("key_trim_vl_mat", state.verandaStepLeft.material.name)
            .putBoolean("key_trim_vr_en", state.verandaStepRight.isEnabled)
            .putString("key_trim_vr_mat", state.verandaStepRight.material.name)
            .putBoolean("key_trim_skirting_en", state.hasSkirtingTopTrim)
            .putString("key_trim_skirting_mat", state.skirtingTopMaterial.name)
            .putBoolean("key_trim_wall_en", state.hasBathroomWallTopTrim)
            .putString("key_trim_wall_mat", state.bathroomWallTopMaterial.name)
            .putString("key_trim_corners", cornersStr)
            .putString("key_trim_custom", customStr)
            .apply()
    }

    fun loadTrimSettings(): TrimSettingsState {
        val cornersStr = prefs.getString("key_trim_corners", "") ?: ""
        val corners = if (cornersStr.isBlank()) emptyList() else {
            cornersStr.split(";;").mapNotNull { item ->
                val parts = item.split("::")
                if (parts.size >= 5) {
                    val mat = try { TrimMaterial.valueOf(parts[3]) } catch (e: Exception) { TrimMaterial.PLASTIC }
                    OuterCornerItem(parts[0], parts[1], parts[2], mat, parts[4].toBoolean())
                } else null
            }
        }

        val customStr = prefs.getString("key_trim_custom", "") ?: ""
        val custom = if (customStr.isBlank()) emptyList() else {
            customStr.split(";;").mapNotNull { item ->
                val parts = item.split("::")
                if (parts.size >= 5) {
                    val mat = try { TrimMaterial.valueOf(parts[3]) } catch (e: Exception) { TrimMaterial.PLASTIC }
                    CustomTrimRunItem(parts[0], parts[1], parts[2], mat, parts[4].toBoolean())
                } else null
            }
        }

        fun safeTrimMat(key: String, default: TrimMaterial): TrimMaterial {
            val name = prefs.getString(key, default.name)
            return try { if (name != null) TrimMaterial.valueOf(name) else default } catch (e: Exception) { default }
        }

        return TrimSettingsState(
            pieceLengthM = prefs.getString("key_trim_piece_len", "2.5") ?: "2.5",
            allowancePercent = prefs.getInt("key_trim_allowance", 10),
            verandaStepFront = VerandaStepTrimConfig(
                isEnabled = prefs.getBoolean("key_trim_vf_en", true),
                material = safeTrimMat("key_trim_vf_mat", TrimMaterial.METAL)
            ),
            verandaStepBack = VerandaStepTrimConfig(
                isEnabled = prefs.getBoolean("key_trim_vb_en", true),
                material = safeTrimMat("key_trim_vb_mat", TrimMaterial.METAL)
            ),
            verandaStepLeft = VerandaStepTrimConfig(
                isEnabled = prefs.getBoolean("key_trim_vl_en", true),
                material = safeTrimMat("key_trim_vl_mat", TrimMaterial.METAL)
            ),
            verandaStepRight = VerandaStepTrimConfig(
                isEnabled = prefs.getBoolean("key_trim_vr_en", true),
                material = safeTrimMat("key_trim_vr_mat", TrimMaterial.METAL)
            ),
            hasSkirtingTopTrim = prefs.getBoolean("key_trim_skirting_en", false),
            skirtingTopMaterial = safeTrimMat("key_trim_skirting_mat", TrimMaterial.PLASTIC),
            hasBathroomWallTopTrim = prefs.getBoolean("key_trim_wall_en", false),
            bathroomWallTopMaterial = safeTrimMat("key_trim_wall_mat", TrimMaterial.PLASTIC),
            outerCorners = corners,
            customRuns = custom
        )
    }
}
