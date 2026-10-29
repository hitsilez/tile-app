package com.example.data

import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject

object JobJsonSerializer {

    fun serializeJobs(jobs: List<Job>): String {
        val array = JSONArray()
        for (job in jobs) {
            array.put(serializeJob(job))
        }
        return array.toString()
    }

    fun deserializeJobs(jsonStr: String): List<Job> {
        if (jsonStr.isBlank()) return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<Job>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                deserializeJob(obj)?.let { list.add(it) }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeJob(job: Job): JSONObject {
        val obj = JSONObject()
        obj.put("id", job.id)
        obj.put("name", job.name)
        obj.put("clientName", job.clientName)
        obj.put("addressOrPlot", job.addressOrPlot)
        obj.put("notes", job.notes)
        obj.put("unitSetting", job.unitSetting.name)

        // Tile Settings
        val ts = JSONObject()
        ts.put("preset", job.tileSettings.preset.name)
        ts.put("customLength", job.tileSettings.customLength)
        ts.put("customWidth", job.tileSettings.customWidth)
        ts.put("tilesPerBox", job.tileSettings.tilesPerBox)
        ts.put("wastePercentage", job.tileSettings.wastePercentage)
        ts.put("isCustomWaste", job.tileSettings.isCustomWaste)
        obj.put("tileSettings", ts)

        // Material Settings
        val ms = JSONObject()
        ms.put("baseCoverageAt5mm", job.materialSettings.baseCoverageAt5mm)
        ms.put("floorBedPreset", job.materialSettings.floorBedPreset.name)
        ms.put("customFloorBedMm", job.materialSettings.customFloorBedMm)
        ms.put("wallBedPreset", job.materialSettings.wallBedPreset.name)
        ms.put("customWallBedMm", job.materialSettings.customWallBedMm)
        ms.put("jointWidthMm", job.materialSettings.jointWidthMm)
        ms.put("jointDepthMm", job.materialSettings.jointDepthMm)
        ms.put("groutType", job.materialSettings.groutType.name)
        ms.put("groutBagSize", job.materialSettings.groutBagSize.name)
        ms.put("spacerBuyMode", job.materialSettings.spacerBuyMode.name)
        ms.put("tilingSpeed", job.materialSettings.tilingSpeed.name)
        ms.put("customPacksToBuy", job.materialSettings.customPacksToBuy)
        ms.put("slowPacksDefault", job.materialSettings.slowPacksDefault)
        ms.put("normalPacksDefault", job.materialSettings.normalPacksDefault)
        ms.put("fastPacksDefault", job.materialSettings.fastPacksDefault)
        ms.put("spacersPerTile", job.materialSettings.spacersPerTile)
        ms.put("spacerPackSize", job.materialSettings.spacerPackSize)
        ms.put("spacerLossPercent", job.materialSettings.spacerLossPercent)
        ms.put("bondingMode", job.materialSettings.bondingMode.name)
        ms.put("additiveMlPerBag", job.materialSettings.additiveMlPerBag)
        ms.put("replacementLitersPerBag", job.materialSettings.replacementLitersPerBag)
        obj.put("materialSettings", ms)

        // Trim Settings
        val tr = JSONObject()
        tr.put("pieceLengthM", job.trimSettings.pieceLengthM)
        tr.put("allowancePercent", job.trimSettings.allowancePercent)
        tr.put("vf_en", job.trimSettings.verandaStepFront.isEnabled)
        tr.put("vf_mat", job.trimSettings.verandaStepFront.material.name)
        tr.put("vb_en", job.trimSettings.verandaStepBack.isEnabled)
        tr.put("vb_mat", job.trimSettings.verandaStepBack.material.name)
        tr.put("vl_en", job.trimSettings.verandaStepLeft.isEnabled)
        tr.put("vl_mat", job.trimSettings.verandaStepLeft.material.name)
        tr.put("vr_en", job.trimSettings.verandaStepRight.isEnabled)
        tr.put("vr_mat", job.trimSettings.verandaStepRight.material.name)
        tr.put("hasSkirtingTopTrim", job.trimSettings.hasSkirtingTopTrim)
        tr.put("skirtingTopMaterial", job.trimSettings.skirtingTopMaterial.name)
        tr.put("hasBathroomWallTopTrim", job.trimSettings.hasBathroomWallTopTrim)
        tr.put("bathroomWallTopMaterial", job.trimSettings.bathroomWallTopMaterial.name)

        val cornersArr = JSONArray()
        job.trimSettings.outerCorners.forEach { c ->
            val co = JSONObject()
            co.put("id", c.id)
            co.put("name", c.name)
            co.put("lengthM", c.lengthM)
            co.put("material", c.material.name)
            co.put("isEnabled", c.isEnabled)
            cornersArr.put(co)
        }
        tr.put("outerCorners", cornersArr)

        val customArr = JSONArray()
        job.trimSettings.customRuns.forEach { cu ->
            val co = JSONObject()
            co.put("id", cu.id)
            co.put("name", cu.name)
            co.put("lengthM", cu.lengthM)
            co.put("material", cu.material.name)
            co.put("isEnabled", cu.isEnabled)
            customArr.put(co)
        }
        tr.put("customRuns", customArr)
        obj.put("trimSettings", tr)

        // Areas
        val areasArr = JSONArray()
        for (area in job.areas) {
            areasArr.put(serializeArea(area))
        }
        obj.put("areas", areasArr)

        return obj
    }

    fun deserializeJob(obj: JSONObject): Job? {
        return try {
            val id = obj.optString("id", java.util.UUID.randomUUID().toString())
            val name = obj.optString("name", "Untitled Job")
            val clientName = obj.optString("clientName", "")
            val addressOrPlot = obj.optString("addressOrPlot", "")
            val notes = obj.optString("notes", "")
            val unitSetting = try {
                UnitSystem.valueOf(obj.optString("unitSetting", UnitSystem.METERS.name))
            } catch (e: Exception) {
                UnitSystem.METERS
            }

            // Tile Settings
            val tsObj = obj.optJSONObject("tileSettings")
            val tileSettings = if (tsObj != null) {
                val preset = try { TilePreset.valueOf(tsObj.optString("preset", TilePreset.SIZE_60X60.name)) } catch (e: Exception) { TilePreset.SIZE_60X60 }
                FloorTileSettingsState(
                    preset = preset,
                    customLength = tsObj.optString("customLength", "60"),
                    customWidth = tsObj.optString("customWidth", "60"),
                    tilesPerBox = tsObj.optString("tilesPerBox", "4"),
                    wastePercentage = tsObj.optInt("wastePercentage", 10),
                    isCustomWaste = tsObj.optBoolean("isCustomWaste", false)
                )
            } else FloorTileSettingsState()

            // Material Settings
            val msObj = obj.optJSONObject("materialSettings")
            val materialSettings = if (msObj != null) {
                val floorBedPreset = try { FloorBedThicknessPreset.valueOf(msObj.optString("floorBedPreset", FloorBedThicknessPreset.LEVEL_5MM.name)) } catch (e: Exception) { FloorBedThicknessPreset.LEVEL_5MM }
                val wallBedPreset = try { WallBedThicknessPreset.valueOf(msObj.optString("wallBedPreset", WallBedThicknessPreset.BED_6MM.name)) } catch (e: Exception) { WallBedThicknessPreset.BED_6MM }
                val groutType = try { GroutType.valueOf(msObj.optString("groutType", GroutType.CEMENT.name)) } catch (e: Exception) { GroutType.CEMENT }
                val groutBagSize = try { GroutBagSize.valueOf(msObj.optString("groutBagSize", GroutBagSize.BAG_5KG.name)) } catch (e: Exception) { GroutBagSize.BAG_5KG }
                val bondingMode = try { BondingLiquidMode.valueOf(msObj.optString("bondingMode", BondingLiquidMode.ADDITIVE_SPLASH.name)) } catch (e: Exception) { BondingLiquidMode.ADDITIVE_SPLASH }
                val spacerBuyMode = try { SpacerBuyMode.valueOf(msObj.optString("spacerBuyMode", SpacerBuyMode.REUSE_DAILY.name)) } catch (e: Exception) { SpacerBuyMode.REUSE_DAILY }
                val tilingSpeed = try { TilingSpeed.valueOf(msObj.optString("tilingSpeed", TilingSpeed.NORMAL.name)) } catch (e: Exception) { TilingSpeed.NORMAL }

                MaterialSettingsState(
                    baseCoverageAt5mm = msObj.optString("baseCoverageAt5mm", "3.0"),
                    floorBedPreset = floorBedPreset,
                    customFloorBedMm = msObj.optString("customFloorBedMm", "5"),
                    wallBedPreset = wallBedPreset,
                    customWallBedMm = msObj.optString("customWallBedMm", "6"),
                    jointWidthMm = msObj.optDouble("jointWidthMm", 3.0),
                    jointDepthMm = msObj.optString("jointDepthMm", "10"),
                    groutType = groutType,
                    groutBagSize = groutBagSize,
                    spacerBuyMode = spacerBuyMode,
                    tilingSpeed = tilingSpeed,
                    customPacksToBuy = msObj.optString("customPacksToBuy", "3"),
                    slowPacksDefault = msObj.optString("slowPacksDefault", "1"),
                    normalPacksDefault = msObj.optString("normalPacksDefault", "3"),
                    fastPacksDefault = msObj.optString("fastPacksDefault", "6"),
                    spacersPerTile = msObj.optString("spacersPerTile", "2"),
                    spacerPackSize = msObj.optString("spacerPackSize", "100"),
                    spacerLossPercent = msObj.optString("spacerLossPercent", "10"),
                    bondingMode = bondingMode,
                    additiveMlPerBag = msObj.optString("additiveMlPerBag", "250"),
                    replacementLitersPerBag = msObj.optString("replacementLitersPerBag", "5")
                )
            } else MaterialSettingsState()

            // Trim Settings
            val trObj = obj.optJSONObject("trimSettings")
            val trimSettings = if (trObj != null) {
                fun safeMat(name: String, def: TrimMaterial): TrimMaterial = try { TrimMaterial.valueOf(name) } catch (e: Exception) { def }

                val cornersList = mutableListOf<OuterCornerItem>()
                val cornersArr = trObj.optJSONArray("outerCorners")
                if (cornersArr != null) {
                    for (i in 0 until cornersArr.length()) {
                        val cObj = cornersArr.optJSONObject(i) ?: continue
                        cornersList.add(
                            OuterCornerItem(
                                id = cObj.optString("id", "corner_$i"),
                                name = cObj.optString("name", "Outer Corner"),
                                lengthM = cObj.optString("lengthM", "2.4"),
                                material = safeMat(cObj.optString("material", "PLASTIC"), TrimMaterial.PLASTIC),
                                isEnabled = cObj.optBoolean("isEnabled", true)
                            )
                        )
                    }
                }

                val customList = mutableListOf<CustomTrimRunItem>()
                val customArr = trObj.optJSONArray("customRuns")
                if (customArr != null) {
                    for (i in 0 until customArr.length()) {
                        val cuObj = customArr.optJSONObject(i) ?: continue
                        customList.add(
                            CustomTrimRunItem(
                                id = cuObj.optString("id", "custom_$i"),
                                name = cuObj.optString("name", "Custom Run"),
                                lengthM = cuObj.optString("lengthM", "2.0"),
                                material = safeMat(cuObj.optString("material", "PLASTIC"), TrimMaterial.PLASTIC),
                                isEnabled = cuObj.optBoolean("isEnabled", true)
                            )
                        )
                    }
                }

                TrimSettingsState(
                    pieceLengthM = trObj.optString("pieceLengthM", "2.5"),
                    allowancePercent = trObj.optInt("allowancePercent", 10),
                    verandaStepFront = VerandaStepTrimConfig(
                        isEnabled = trObj.optBoolean("vf_en", true),
                        material = safeMat(trObj.optString("vf_mat", "METAL"), TrimMaterial.METAL)
                    ),
                    verandaStepBack = VerandaStepTrimConfig(
                        isEnabled = trObj.optBoolean("vb_en", true),
                        material = safeMat(trObj.optString("vb_mat", "METAL"), TrimMaterial.METAL)
                    ),
                    verandaStepLeft = VerandaStepTrimConfig(
                        isEnabled = trObj.optBoolean("vl_en", true),
                        material = safeMat(trObj.optString("vl_mat", "METAL"), TrimMaterial.METAL)
                    ),
                    verandaStepRight = VerandaStepTrimConfig(
                        isEnabled = trObj.optBoolean("vr_en", true),
                        material = safeMat(trObj.optString("vr_mat", "METAL"), TrimMaterial.METAL)
                    ),
                    hasSkirtingTopTrim = trObj.optBoolean("hasSkirtingTopTrim", false),
                    skirtingTopMaterial = safeMat(trObj.optString("skirtingTopMaterial", "PLASTIC"), TrimMaterial.PLASTIC),
                    hasBathroomWallTopTrim = trObj.optBoolean("hasBathroomWallTopTrim", false),
                    bathroomWallTopMaterial = safeMat(trObj.optString("bathroomWallTopMaterial", "PLASTIC"), TrimMaterial.PLASTIC),
                    outerCorners = cornersList,
                    customRuns = customList
                )
            } else TrimSettingsState()

            // Areas
            val areasList = mutableListOf<AreaItem>()
            val areasArr = obj.optJSONArray("areas")
            if (areasArr != null) {
                for (i in 0 until areasArr.length()) {
                    val aObj = areasArr.optJSONObject(i) ?: continue
                    deserializeArea(aObj)?.let { areasList.add(it) }
                }
            }

            Job(
                id = id,
                name = name,
                clientName = clientName,
                addressOrPlot = addressOrPlot,
                notes = notes,
                unitSetting = unitSetting,
                tileSettings = tileSettings,
                materialSettings = materialSettings,
                trimSettings = trimSettings,
                areas = areasList
            )
        } catch (e: Exception) {
            null
        }
    }

    fun serializeArea(area: AreaItem): JSONObject {
        val obj = JSONObject()
        obj.put("id", area.id)
        obj.put("name", area.name)
        obj.put("type", area.type.name)

        // Room State
        val rs = JSONObject()
        rs.put("length", area.roomState.length)
        rs.put("width", area.roomState.width)
        val dw = JSONArray()
        area.roomState.doorwayWidths.forEach { dw.put(it) }
        rs.put("doorwayWidths", dw)
        rs.put("hasSkirting", area.roomState.hasSkirting)
        rs.put("skirtingPreset", area.roomState.skirtingPreset.name)
        rs.put("customSkirtingHeight", area.roomState.customSkirtingHeight)
        rs.put("hasAlongSkirtingTrim", area.roomState.hasAlongSkirtingTrim)
        rs.put("alongSkirtingTrimMaterial", area.roomState.alongSkirtingTrimMaterial.name)
        val dte = JSONArray()
        area.roomState.doorTrimEnabled.forEach { dte.put(it) }
        rs.put("doorTrimEnabled", dte)
        val dtm = JSONArray()
        area.roomState.doorTrimMaterials.forEach { dtm.put(it.name) }
        rs.put("doorTrimMaterials", dtm)
        obj.put("roomState", rs)

        // Veranda State
        val vs = JSONObject()
        vs.put("length", area.verandaState.length)
        vs.put("width", area.verandaState.width)
        vs.put("frontType", area.verandaState.frontType.name)
        vs.put("backType", area.verandaState.backType.name)
        vs.put("leftType", area.verandaState.leftType.name)
        vs.put("rightType", area.verandaState.rightType.name)
        vs.put("stepEdgeHeight", area.verandaState.stepEdgeHeight)
        vs.put("hasWallSkirting", area.verandaState.hasWallSkirting)
        vs.put("wallSkirtingPreset", area.verandaState.wallSkirtingPreset.name)
        vs.put("customWallSkirtingHeight", area.verandaState.customWallSkirtingHeight)
        vs.put("hasDoor", area.verandaState.hasDoor)
        vs.put("doorSide", area.verandaState.doorSide.name)
        vs.put("doorWidth", area.verandaState.doorWidth)
        vs.put("doorStripHeight", area.verandaState.doorStripHeight)
        vs.put("frontTrimEnabled", area.verandaState.frontTrimEnabled)
        vs.put("frontTrimMaterial", area.verandaState.frontTrimMaterial?.name ?: "")
        vs.put("backTrimEnabled", area.verandaState.backTrimEnabled)
        vs.put("backTrimMaterial", area.verandaState.backTrimMaterial?.name ?: "")
        vs.put("leftTrimEnabled", area.verandaState.leftTrimEnabled)
        vs.put("leftTrimMaterial", area.verandaState.leftTrimMaterial?.name ?: "")
        vs.put("rightTrimEnabled", area.verandaState.rightTrimEnabled)
        vs.put("rightTrimMaterial", area.verandaState.rightTrimMaterial?.name ?: "")

        val pilArr = JSONArray()
        area.verandaState.pillars.forEach { p ->
            val po = JSONObject()
            po.put("id", p.id)
            po.put("widthCm", p.widthCm)
            po.put("depthCm", p.depthCm)
            po.put("material", p.material.name)
            po.put("isEnabled", p.isEnabled)
            po.put("removeFromEdge", p.removeFromEdge)
            po.put("edgeSide", p.edgeSide.name)
            pilArr.put(po)
        }
        vs.put("pillars", pilArr)
        obj.put("verandaState", vs)

        // Bathroom State
        val bs = JSONObject()
        bs.put("length", area.bathroomState.length)
        bs.put("width", area.bathroomState.width)
        val bdw = JSONArray()
        area.bathroomState.doorwayWidths.forEach { bdw.put(it) }
        bs.put("doorwayWidths", bdw)
        bs.put("hasSkirting", area.bathroomState.hasSkirting)
        bs.put("skirtingPreset", area.bathroomState.skirtingPreset.name)
        bs.put("customSkirtingHeight", area.bathroomState.customSkirtingHeight)
        bs.put("hasAlongSkirtingTrim", area.bathroomState.hasAlongSkirtingTrim)
        bs.put("alongSkirtingTrimMaterial", area.bathroomState.alongSkirtingTrimMaterial.name)
        val bdte = JSONArray()
        area.bathroomState.doorTrimEnabled.forEach { bdte.put(it) }
        bs.put("doorTrimEnabled", bdte)
        val bdtm = JSONArray()
        area.bathroomState.doorTrimMaterials.forEach { bdtm.put(it.name) }
        bs.put("doorTrimMaterials", bdtm)
        bs.put("tileWallsToo", area.bathroomState.tileWallsToo)
        bs.put("wallTilePreset", area.bathroomState.wallTilePreset.name)
        bs.put("customWallTileLength", area.bathroomState.customWallTileLength)
        bs.put("customWallTileWidth", area.bathroomState.customWallTileWidth)
        bs.put("wallTilesPerBox", area.bathroomState.wallTilesPerBox)
        bs.put("hasWallTopTrim", area.bathroomState.hasWallTopTrim)
        bs.put("wallTopTrimMaterial", area.bathroomState.wallTopTrimMaterial.name)

        val wallsArr = JSONArray()
        area.bathroomState.walls.forEach { w ->
            val wo = JSONObject()
            wo.put("id", w.id)
            wo.put("label", w.label)
            wo.put("width", w.width)
            wo.put("height", w.height)
            wallsArr.put(wo)
        }
        bs.put("walls", wallsArr)

        val winArr = JSONArray()
        area.bathroomState.windows.forEach { win ->
            val wio = JSONObject()
            wio.put("id", win.id)
            wio.put("width", win.width)
            wio.put("height", win.height)
            winArr.put(wio)
        }
        bs.put("windows", winArr)

        val doorsArr = JSONArray()
        area.bathroomState.doors.forEach { d ->
            val dio = JSONObject()
            dio.put("id", d.id)
            dio.put("width", d.width)
            dio.put("height", d.height)
            doorsArr.put(dio)
        }
        bs.put("doors", doorsArr)
        bs.put("hasBathtub", area.bathroomState.hasBathtub)
        bs.put("bathtubLength", area.bathroomState.bathtubLength)
        bs.put("bathtubWidth", area.bathroomState.bathtubWidth)
        bs.put("bathtubHeight", area.bathroomState.bathtubHeight)
        bs.put("bathtubHeightCm", area.bathroomState.bathtubHeight)
        bs.put("bathtubShape", area.bathroomState.bathtubShape.name)
        bs.put("bathtubLEnd", area.bathroomState.bathtubLEnd.name)
        bs.put("bathtubFrontTiled", area.bathroomState.bathtubFrontTiled)
        bs.put("bathtubBackTiled", area.bathroomState.bathtubBackTiled)
        bs.put("bathtubLeftTiled", area.bathroomState.bathtubLeftTiled)
        bs.put("bathtubRightTiled", area.bathroomState.bathtubRightTiled)
        bs.put("tileBathtubTop", area.bathroomState.tileBathtubTop)
        bs.put("bathtubTopWidthCm", area.bathroomState.bathtubTopWidthCm)
        bs.put("bathtubTopEdgeTrimEnabled", area.bathroomState.bathtubTopEdgeTrimEnabled)
        bs.put("bathtubTopEdgeTrimMaterial", area.bathroomState.bathtubTopEdgeTrimMaterial.name)
        bs.put("bathtubCornerTrimEnabled", area.bathroomState.bathtubCornerTrimEnabled)
        bs.put("bathtubCornerTrimMaterial", area.bathroomState.bathtubCornerTrimMaterial.name)
        obj.put("bathroomState", bs)

        return obj
    }

    fun deserializeArea(obj: JSONObject): AreaItem? {
        return try {
            val id = obj.optString("id", java.util.UUID.randomUUID().toString())
            val name = obj.optString("name", "Area")
            val type = try {
                SpaceType.valueOf(obj.optString("type", SpaceType.ROOM_FLOOR.name))
            } catch (e: Exception) {
                SpaceType.ROOM_FLOOR
            }

            // Room State
            val rsObj = obj.optJSONObject("roomState")
            val roomState = if (rsObj != null) {
                val dws = mutableListOf<String>()
                val dwsArr = rsObj.optJSONArray("doorwayWidths")
                if (dwsArr != null) {
                    for (i in 0 until dwsArr.length()) {
                        dws.add(dwsArr.optString(i))
                    }
                }
                val skirtingPreset = try {
                    SkirtingPreset.valueOf(rsObj.optString("skirtingPreset", SkirtingPreset.HEIGHT_10CM.name))
                } catch (e: Exception) {
                    SkirtingPreset.HEIGHT_10CM
                }
                val hasAlongSkirtingTrim = rsObj.optBoolean("hasAlongSkirtingTrim", true)
                val alongSkirtingTrimMaterial = try {
                    TrimMaterial.valueOf(rsObj.optString("alongSkirtingTrimMaterial", TrimMaterial.PLASTIC.name))
                } catch (e: Exception) {
                    TrimMaterial.PLASTIC
                }
                val dteList = mutableListOf<Boolean>()
                rsObj.optJSONArray("doorTrimEnabled")?.let { arr ->
                    for (i in 0 until arr.length()) dteList.add(arr.optBoolean(i, false))
                }
                val dtmList = mutableListOf<TrimMaterial>()
                rsObj.optJSONArray("doorTrimMaterials")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val m = try { TrimMaterial.valueOf(arr.optString(i, TrimMaterial.PLASTIC.name)) } catch (e: Exception) { TrimMaterial.PLASTIC }
                        dtmList.add(m)
                    }
                }
                RoomFloorState(
                    length = rsObj.optString("length", "4"),
                    width = rsObj.optString("width", "3"),
                    doorwayWidths = dws,
                    hasSkirting = rsObj.optBoolean("hasSkirting", false),
                    skirtingPreset = skirtingPreset,
                    customSkirtingHeight = rsObj.optString("customSkirtingHeight", "10"),
                    hasAlongSkirtingTrim = hasAlongSkirtingTrim,
                    alongSkirtingTrimMaterial = alongSkirtingTrimMaterial,
                    doorTrimEnabled = dteList,
                    doorTrimMaterials = dtmList
                )
            } else RoomFloorState()

            // Veranda State
            val vsObj = obj.optJSONObject("verandaState")
            val verandaState = if (vsObj != null) {
                fun safeSideType(name: String, def: VerandaSideType): VerandaSideType = try { VerandaSideType.valueOf(name) } catch (e: Exception) { def }
                val skirtingPreset = try {
                    SkirtingPreset.valueOf(vsObj.optString("wallSkirtingPreset", SkirtingPreset.HEIGHT_10CM.name))
                } catch (e: Exception) {
                    SkirtingPreset.HEIGHT_10CM
                }
                val doorSide = try {
                    VerandaSide.valueOf(vsObj.optString("doorSide", VerandaSide.BACK.name))
                } catch (e: Exception) {
                    VerandaSide.BACK
                }
                fun optTrimMat(key: String): TrimMaterial? {
                    val s = vsObj.optString(key, "")
                    return if (s.isNotBlank()) try { TrimMaterial.valueOf(s) } catch (e: Exception) { null } else null
                }
                val pillarsList = mutableListOf<PillarItem>()
                val pilArr = vsObj.optJSONArray("pillars")
                if (pilArr != null) {
                    for (i in 0 until pilArr.length()) {
                        val po = pilArr.optJSONObject(i) ?: continue
                        val pMat = try { TrimMaterial.valueOf(po.optString("material", TrimMaterial.METAL.name)) } catch (e: Exception) { TrimMaterial.METAL }
                        val pSide = try { VerandaSide.valueOf(po.optString("edgeSide", VerandaSide.FRONT.name)) } catch (e: Exception) { VerandaSide.FRONT }
                        pillarsList.add(
                            PillarItem(
                                id = po.optString("id", java.util.UUID.randomUUID().toString()),
                                widthCm = po.optString("widthCm", "30"),
                                depthCm = po.optString("depthCm", "30"),
                                material = pMat,
                                isEnabled = po.optBoolean("isEnabled", true),
                                removeFromEdge = po.optBoolean("removeFromEdge", false),
                                edgeSide = pSide
                            )
                        )
                    }
                }
                VerandaState(
                    length = vsObj.optString("length", "5"),
                    width = vsObj.optString("width", "2.5"),
                    frontType = safeSideType(vsObj.optString("frontType"), VerandaSideType.OPENING_STEP),
                    backType = safeSideType(vsObj.optString("backType"), VerandaSideType.WALL),
                    leftType = safeSideType(vsObj.optString("leftType"), VerandaSideType.OPENING_STEP),
                    rightType = safeSideType(vsObj.optString("rightType"), VerandaSideType.WALL),
                    stepEdgeHeight = vsObj.optString("stepEdgeHeight", "15"),
                    hasWallSkirting = vsObj.optBoolean("hasWallSkirting", true),
                    wallSkirtingPreset = skirtingPreset,
                    customWallSkirtingHeight = vsObj.optString("customWallSkirtingHeight", "10"),
                    hasDoor = vsObj.optBoolean("hasDoor", false),
                    doorSide = doorSide,
                    doorWidth = vsObj.optString("doorWidth", "0.9"),
                    doorStripHeight = vsObj.optString("doorStripHeight", "15"),
                    frontTrimEnabled = vsObj.optBoolean("frontTrimEnabled", true),
                    frontTrimMaterial = optTrimMat("frontTrimMaterial"),
                    backTrimEnabled = vsObj.optBoolean("backTrimEnabled", true),
                    backTrimMaterial = optTrimMat("backTrimMaterial"),
                    leftTrimEnabled = vsObj.optBoolean("leftTrimEnabled", true),
                    leftTrimMaterial = optTrimMat("leftTrimMaterial"),
                    rightTrimEnabled = vsObj.optBoolean("rightTrimEnabled", true),
                    rightTrimMaterial = optTrimMat("rightTrimMaterial"),
                    pillars = pillarsList
                )
            } else VerandaState()

            // Bathroom State
            val bsObj = obj.optJSONObject("bathroomState")
            val bathroomState = if (bsObj != null) {
                val dws = mutableListOf<String>()
                val dwsArr = bsObj.optJSONArray("doorwayWidths")
                if (dwsArr != null) {
                    for (i in 0 until dwsArr.length()) {
                        dws.add(dwsArr.optString(i))
                    }
                }
                val skirtingPreset = try {
                    SkirtingPreset.valueOf(bsObj.optString("skirtingPreset", SkirtingPreset.HEIGHT_7CM.name))
                } catch (e: Exception) {
                    SkirtingPreset.HEIGHT_7CM
                }
                val wallTilePreset = try {
                    TilePreset.valueOf(bsObj.optString("wallTilePreset", TilePreset.SIZE_30X30.name))
                } catch (e: Exception) {
                    TilePreset.SIZE_30X30
                }

                val hasAlongSkirtingTrim = bsObj.optBoolean("hasAlongSkirtingTrim", true)
                val alongSkirtingTrimMaterial = try {
                    TrimMaterial.valueOf(bsObj.optString("alongSkirtingTrimMaterial", TrimMaterial.PLASTIC.name))
                } catch (e: Exception) {
                    TrimMaterial.PLASTIC
                }
                val dteList = mutableListOf<Boolean>()
                bsObj.optJSONArray("doorTrimEnabled")?.let { arr ->
                    for (i in 0 until arr.length()) dteList.add(arr.optBoolean(i, false))
                }
                val dtmList = mutableListOf<TrimMaterial>()
                bsObj.optJSONArray("doorTrimMaterials")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val m = try { TrimMaterial.valueOf(arr.optString(i, TrimMaterial.PLASTIC.name)) } catch (e: Exception) { TrimMaterial.PLASTIC }
                        dtmList.add(m)
                    }
                }
                val hasWallTopTrim = bsObj.optBoolean("hasWallTopTrim", false)
                val wallTopTrimMaterial = try {
                    TrimMaterial.valueOf(bsObj.optString("wallTopTrimMaterial", TrimMaterial.PLASTIC.name))
                } catch (e: Exception) {
                    TrimMaterial.PLASTIC
                }

                val wallsList = mutableListOf<WallDimension>()
                val wallsArr = bsObj.optJSONArray("walls")
                if (wallsArr != null) {
                    for (i in 0 until wallsArr.length()) {
                        val wo = wallsArr.optJSONObject(i) ?: continue
                        wallsList.add(
                            WallDimension(
                                id = wo.optString("id", "w$i"),
                                label = wo.optString("label", "Wall ${i + 1}"),
                                width = wo.optString("width", "2.0"),
                                height = wo.optString("height", "2.4")
                            )
                        )
                    }
                }

                val winList = mutableListOf<OpeningDimension>()
                val winArr = bsObj.optJSONArray("windows")
                if (winArr != null) {
                    for (i in 0 until winArr.length()) {
                        val wio = winArr.optJSONObject(i) ?: continue
                        winList.add(OpeningDimension(wio.optString("id", "win$i"), wio.optString("width", "0.6"), wio.optString("height", "0.6")))
                    }
                }

                val doorsList = mutableListOf<OpeningDimension>()
                val doorsArr = bsObj.optJSONArray("doors")
                if (doorsArr != null) {
                    for (i in 0 until doorsArr.length()) {
                        val dio = doorsArr.optJSONObject(i) ?: continue
                        doorsList.add(OpeningDimension(dio.optString("id", "door$i"), dio.optString("width", "0.8"), dio.optString("height", "2.0")))
                    }
                }

                BathroomState(
                    length = bsObj.optString("length", "2.5"),
                    width = bsObj.optString("width", "2.0"),
                    doorwayWidths = dws,
                    hasSkirting = bsObj.optBoolean("hasSkirting", false),
                    skirtingPreset = skirtingPreset,
                    customSkirtingHeight = bsObj.optString("customSkirtingHeight", "7"),
                    hasAlongSkirtingTrim = hasAlongSkirtingTrim,
                    alongSkirtingTrimMaterial = alongSkirtingTrimMaterial,
                    doorTrimEnabled = dteList,
                    doorTrimMaterials = dtmList,
                    tileWallsToo = bsObj.optBoolean("tileWallsToo", false),
                    wallTilePreset = wallTilePreset,
                    customWallTileLength = bsObj.optString("customWallTileLength", "30"),
                    customWallTileWidth = bsObj.optString("customWallTileWidth", "30"),
                    wallTilesPerBox = bsObj.optString("wallTilesPerBox", "10"),
                    walls = if (wallsList.isEmpty()) BathroomState().walls else wallsList,
                    windows = winList,
                    doors = doorsList,
                    hasBathtub = bsObj.optBoolean("hasBathtub", false),
                    bathtubLength = bsObj.optString("bathtubLength", bsObj.optString("bathtubWidth", "1.7")),
                    bathtubWidth = if (bsObj.has("bathtubLength")) bsObj.optString("bathtubWidth", "0.7") else "0.7",
                    bathtubHeight = if (bsObj.has("bathtubHeightCm")) {
                        bsObj.optString("bathtubHeightCm", "50")
                    } else {
                        val oldH = bsObj.optString("bathtubHeight", "50")
                        val d = oldH.toDoubleOrNull() ?: 50.0
                        if (d in 0.01..3.0) (d * 100).toInt().toString() else oldH
                    },
                    bathtubShape = try {
                        BathtubShape.valueOf(bsObj.optString("bathtubShape", BathtubShape.AGAINST_WALL.name))
                    } catch (e: Exception) {
                        BathtubShape.AGAINST_WALL
                    },
                    bathtubLEnd = try {
                        BathtubLEnd.valueOf(bsObj.optString("bathtubLEnd", BathtubLEnd.RIGHT.name))
                    } catch (e: Exception) {
                        BathtubLEnd.RIGHT
                    },
                    bathtubFrontTiled = bsObj.optBoolean("bathtubFrontTiled", true),
                    bathtubBackTiled = bsObj.optBoolean("bathtubBackTiled", false),
                    bathtubLeftTiled = bsObj.optBoolean("bathtubLeftTiled", false),
                    bathtubRightTiled = bsObj.optBoolean("bathtubRightTiled", false),
                    tileBathtubTop = bsObj.optBoolean("tileBathtubTop", false),
                    bathtubTopWidthCm = bsObj.optString("bathtubTopWidthCm", ""),
                    bathtubTopEdgeTrimEnabled = bsObj.optBoolean("bathtubTopEdgeTrimEnabled", true),
                    bathtubTopEdgeTrimMaterial = try {
                        TrimMaterial.valueOf(bsObj.optString("bathtubTopEdgeTrimMaterial", TrimMaterial.PLASTIC.name))
                    } catch (e: Exception) {
                        TrimMaterial.PLASTIC
                    },
                    bathtubCornerTrimEnabled = bsObj.optBoolean("bathtubCornerTrimEnabled", true),
                    bathtubCornerTrimMaterial = try {
                        TrimMaterial.valueOf(bsObj.optString("bathtubCornerTrimMaterial", TrimMaterial.PLASTIC.name))
                    } catch (e: Exception) {
                        TrimMaterial.PLASTIC
                    },
                    hasWallTopTrim = hasWallTopTrim,
                    wallTopTrimMaterial = wallTopTrimMaterial
                )
            } else BathroomState()

            AreaItem(
                id = id,
                name = name,
                type = type,
                roomState = roomState,
                verandaState = verandaState,
                bathroomState = bathroomState
            )
        } catch (e: Exception) {
            null
        }
    }

    fun serializeUserProfile(profile: UserProfile): String {
        val obj = JSONObject()
        obj.put("builderName", profile.builderName)
        obj.put("builderPhone", profile.builderPhone)
        return obj.toString()
    }

    fun deserializeUserProfile(jsonStr: String): UserProfile {
        if (jsonStr.isBlank()) return UserProfile()
        return try {
            val obj = JSONObject(jsonStr)
            UserProfile(
                builderName = obj.optString("builderName", ""),
                builderPhone = obj.optString("builderPhone", "")
            )
        } catch (e: Exception) {
            UserProfile()
        }
    }

    fun createBackupJson(jobs: List<Job>, activeJobId: String?, profile: UserProfile): String {
        val backup = JSONObject()
        backup.put("version", 1)
        backup.put("timestamp", System.currentTimeMillis())
        backup.put("tile_calc_active_job_id", activeJobId ?: "")
        val jobsArr = JSONArray()
        for (j in jobs) {
            jobsArr.put(serializeJob(j))
        }
        backup.put("tile_calc_jobs", jobsArr)
        val profObj = JSONObject()
        profObj.put("builderName", profile.builderName)
        profObj.put("builderPhone", profile.builderPhone)
        backup.put("tile_calc_user_profile", profObj)
        return backup.toString(2)
    }

    fun parseBackupJson(jsonStr: String): Triple<List<Job>, String?, UserProfile>? {
        if (jsonStr.isBlank()) return null
        return try {
            val obj = JSONObject(jsonStr)
            val activeJobId = obj.optString("tile_calc_active_job_id", "").ifBlank { null }
            val jobsArr = obj.optJSONArray("tile_calc_jobs")
            val jobsList = mutableListOf<Job>()
            if (jobsArr != null) {
                for (i in 0 until jobsArr.length()) {
                    val jObj = jobsArr.optJSONObject(i) ?: continue
                    deserializeJob(jObj)?.let { jobsList.add(it) }
                }
            }
            val profObj = obj.optJSONObject("tile_calc_user_profile")
            val userProfile = if (profObj != null) {
                UserProfile(
                    builderName = profObj.optString("builderName", ""),
                    builderPhone = profObj.optString("builderPhone", "")
                )
            } else UserProfile()
            Triple(jobsList, activeJobId, userProfile)
        } catch (e: Exception) {
            null
        }
    }
}
