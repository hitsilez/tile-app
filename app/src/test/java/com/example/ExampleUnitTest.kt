package com.example

import com.example.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {

    @Test
    fun testUserTestCase() {
        // TEST CASE: Room 4 m x 3 m, 60x60 tiles, 4 tiles per box, 10% waste, no skirting, no doorways
        // Expected: 37 tiles, 10 boxes, 3 spare
        val result = TileCalculatorEngine.calculate(
            spaceType = SpaceType.ROOM_FLOOR,
            unit = UnitSystem.METERS,
            tileSettings = FloorTileSettingsState(
                preset = TilePreset.SIZE_60X60,
                tilesPerBox = "4",
                wastePercentage = 10
            ),
            materialSettings = MaterialSettingsState(),
            roomState = RoomFloorState(
                length = "4",
                width = "3",
                doorwayWidths = emptyList(),
                hasSkirting = false
            ),
            verandaState = VerandaState(),
            bathroomState = BathroomState()
        )

        assertEquals("Total floor tiles must be 37", 37, result.floorTilesRawNeeded)
        assertEquals("Total tiles must be 37", 37, result.totalFloorTilesNeeded)
        assertEquals("Boxes to buy must be 10", 10, result.floorBoxesToBuy)
        assertEquals("Spare tiles must be 3", 3, result.floorSpareTiles)
    }

    @Test
    fun testMaterialsTestCase() {
        // TEST CASE:
        // Room 4 m x 3 m, 60x60 tiles, 4 tiles per box, 10% waste, no skirting, floor bed 5 mm,
        // coverage 3.0 m² per bag, J = 3 mm, D = 10 mm, cement grout, 5 kg bags, 2 spacers per tile,
        // pack size 100, additive splash mode.
        // - Tiles: 37, boxes: 10 (unchanged)
        // - Adhesive: 4 bags
        // - Grout: about 2.1 kg, so 1 bag of 5 kg
        // - Spacers: 74, so 1 pack
        // - Bonding liquid: 4 x 250 ml = 1 L, so 1 bottle of 1 L
        val result = TileCalculatorEngine.calculate(
            spaceType = SpaceType.ROOM_FLOOR,
            unit = UnitSystem.METERS,
            tileSettings = FloorTileSettingsState(
                preset = TilePreset.SIZE_60X60,
                tilesPerBox = "4",
                wastePercentage = 10
            ),
            materialSettings = MaterialSettingsState(
                baseCoverageAt5mm = "3.0",
                floorBedPreset = FloorBedThicknessPreset.LEVEL_5MM,
                customFloorBedMm = "5",
                wallBedPreset = WallBedThicknessPreset.BED_6MM,
                customWallBedMm = "6",
                jointWidthMm = 3.0,
                jointDepthMm = "10",
                groutType = GroutType.CEMENT,
                groutBagSize = GroutBagSize.BAG_5KG,
                spacersPerTile = "2",
                spacerPackSize = "100",
                bondingMode = BondingLiquidMode.ADDITIVE_SPLASH,
                additiveMlPerBag = "250",
                replacementLitersPerBag = "5"
            ),
            roomState = RoomFloorState(
                length = "4",
                width = "3",
                doorwayWidths = emptyList(),
                hasSkirting = false
            ),
            verandaState = VerandaState(),
            bathroomState = BathroomState()
        )

        // Tiles
        assertEquals("Total tiles must be 37", 37, result.totalFloorTilesNeeded)
        assertEquals("Boxes to buy must be 10", 10, result.floorBoxesToBuy)

        // Adhesive: 4 bags
        assertEquals("Adhesive bags must be 4", 4, result.materials.totalAdhesiveBags)

        // Grout: about 2.1 kg, 1 bag of 5 kg
        assertEquals("Grout kg should be approx 2.112 kg", 2.112, result.materials.totalGroutKg, 0.05)
        assertEquals("Grout bags must be 1", 1, result.materials.groutBagsToBuy)

        // Spacers: 74, so 1 pack
        assertEquals("Total spacers must be 74", 74, result.materials.totalSpacersNeeded)
        assertEquals("Spacer packs must be 1", 1, result.materials.spacerPacksToBuy)

        // Bonding liquid: 1 L -> "1 bottle of 1 L"
        assertEquals("Bonding liquid total liters must be 1.0 L", 1.0, result.materials.totalBondingLiquidLiters, 0.001)
        assertEquals("Bonding liquid display must be '1 bottle of 1 L'", "1 bottle of 1 L", result.materials.bondingLiquidDisplayText)
    }

    @Test
    fun testSkirtingCalculation() {
        // Tile 60x60 cm (0.60 m x 0.60 m)
        // Strip height 10 cm (0.10 m)
        // strips per tile = floor(0.60 / 0.10) = 6 strips/tile
        // Run length: 12 m
        // strips needed = ceil(12 / 0.60) = 20 strips
        // tiles needed = ceil(20 / 6) = 4 tiles
        val stripResult = TileFormulas.calculateStrips(
            tileLengthM = 0.60,
            tileWidthM = 0.60,
            stripHeightM = 0.10,
            runLengthM = 12.0
        )

        assertEquals(6, stripResult.stripsPerTile)
        assertEquals(20, stripResult.stripsNeeded)
        assertEquals(4, stripResult.tilesNeeded)
    }

    @Test
    fun testStripHeightBiggerThanTileUsesWholeRowsWithoutWarning() {
        val stripResult = TileFormulas.calculateStrips(
            tileLengthM = 0.30,
            tileWidthM = 0.30,
            stripHeightM = 0.40,
            runLengthM = 10.0,
            label = "Step face",
            areaName = "Veranda",
            sideName = "front step"
        )

        // Under A2, taller faces use whole rows + leftover without warning
        assertEquals("Warning should be null since taller faces use whole rows", null, stripResult.warning)
        assertTrue("Tiles needed should be > 0", stripResult.tilesNeeded > 0)

        // Under A3, warning only when height > 3.0 m (300 cm)
        val excessiveHeightResult = TileFormulas.calculateStrips(
            tileLengthM = 0.30,
            tileWidthM = 0.30,
            stripHeightM = 3.50,
            runLengthM = 10.0,
            label = "Step face",
            areaName = "Veranda",
            sideName = "front step"
        )
        assertNotNull("Warning should be present when height > 3.0 m", excessiveHeightResult.warning)
        assertEquals("Veranda, front step: height is more than 3 m.", excessiveHeightResult.warning)
    }

    @Test
    fun testBondingLiquidDisplayFormatting() {
        assertEquals("750 ml", TileFormulas.formatBondingLiquidDisplay(0.75))
        assertEquals("1 bottle of 1 L", TileFormulas.formatBondingLiquidDisplay(1.0))
        assertEquals("2 bottles of 1 L", TileFormulas.formatBondingLiquidDisplay(2.0))
        assertEquals("1 jug of 5 L + 1 bottle of 1 L", TileFormulas.formatBondingLiquidDisplay(6.0))
        assertEquals("4 jugs of 5 L", TileFormulas.formatBondingLiquidDisplay(20.0))
    }

    @Test
    fun testUnitConversion() {
        val cmValue = 400.0 // 400 cm = 4.0 m
        val metersFromCm = UnitSystem.CENTIMETERS.toMeters(cmValue)
        assertEquals(4.0, metersFromCm, 0.0001)

        val ftValue = 10.0 // 10 ft = 3.048 m
        val metersFromFt = UnitSystem.FEET.toMeters(ftValue)
        assertEquals(3.048, metersFromFt, 0.0001)
    }

    @Test
    fun testVerandaTrimTestCase() {
        // TEST CASE:
        // Veranda length 4 m, width 3 m, 60x60 tiles, 4 per box, 10% waste.
        // Front and right sides are Openings (step), back and left sides are Walls.
        // Skirting on both walls, no door. Metal trim on the openings, plastic trim on the skirting.
        // Piece length 2.5 m, allowance 10%.
        // - Metal trim run = 4 + 3 = 7 m, pieces = ceil( 7 x 1.1 / 2.5 ) = ceil( 3.08 ) = 4
        // - Plastic trim run = 4 + 3 = 7 m, pieces = 4
        val result = TileCalculatorEngine.calculate(
            spaceType = SpaceType.VERANDA,
            unit = UnitSystem.METERS,
            tileSettings = FloorTileSettingsState(
                preset = TilePreset.SIZE_60X60,
                tilesPerBox = "4",
                wastePercentage = 10
            ),
            materialSettings = MaterialSettingsState(),
            trimSettings = TrimSettingsState(
                pieceLengthM = "2.5",
                allowancePercent = 10,
                verandaStepFront = VerandaStepTrimConfig(isEnabled = true, material = TrimMaterial.METAL),
                verandaStepRight = VerandaStepTrimConfig(isEnabled = true, material = TrimMaterial.METAL),
                verandaStepBack = VerandaStepTrimConfig(isEnabled = false, material = TrimMaterial.METAL),
                verandaStepLeft = VerandaStepTrimConfig(isEnabled = false, material = TrimMaterial.METAL),
                hasSkirtingTopTrim = true,
                skirtingTopMaterial = TrimMaterial.PLASTIC,
                outerCorners = emptyList(),
                customRuns = emptyList()
            ),
            roomState = RoomFloorState(),
            verandaState = VerandaState(
                length = "4",
                width = "3",
                frontType = VerandaSideType.OPENING_STEP,
                backType = VerandaSideType.WALL,
                leftType = VerandaSideType.WALL,
                rightType = VerandaSideType.OPENING_STEP,
                stepEdgeHeight = "15",
                hasWallSkirting = true,
                hasDoor = false
            ),
            bathroomState = BathroomState()
        )

        // Metal Trims (Front = 4 m, Right = 3 m -> Total 7 m)
        assertEquals("Metal trim total run should be 7.0 m", 7.0, result.trims.metalTotalRunLengthM, 0.001)
        assertEquals("Metal trim pieces should be 4", 4, result.trims.metalPiecesNeeded)

        // Plastic Trims (Back wall = 4 m, Left wall = 3 m -> Total 7 m)
        assertEquals("Plastic trim total run should be 7.0 m", 7.0, result.trims.plasticTotalRunLengthM, 0.001)
        assertEquals("Plastic trim pieces should be 4", 4, result.trims.plasticPiecesNeeded)
    }

    @Test
    fun testTwoRoomsJobTestCase() {
        // TEST CASE (Prompt 3):
        // Create one job with 60x60 tiles, 4 tiles per box and 10% waste.
        // Add two room floors: Room A is 4 m x 3 m and Room B is 2 m x 2 m. No skirting, no doorways.
        // - Total net floor area = 16 m²
        // - Tiles needed = ceil( 16 x 1.1 / 0.36 ) = ceil( 48.89 ) = 49
        // - Boxes = ceil( 49 / 4 ) = 13
        // - Spare tiles = 3
        val job = Job(
            id = "test_job_1",
            name = "Test Job",
            clientName = "John Doe",
            unitSetting = UnitSystem.METERS,
            tileSettings = FloorTileSettingsState(
                preset = TilePreset.SIZE_60X60,
                tilesPerBox = "4",
                wastePercentage = 10
            ),
            materialSettings = MaterialSettingsState(),
            trimSettings = TrimSettingsState(),
            areas = listOf(
                AreaItem(
                    id = "area_a",
                    name = "Room A",
                    type = SpaceType.ROOM_FLOOR,
                    roomState = RoomFloorState(
                        length = "4",
                        width = "3",
                        doorwayWidths = emptyList(),
                        hasSkirting = false
                    )
                ),
                AreaItem(
                    id = "area_b",
                    name = "Room B",
                    type = SpaceType.ROOM_FLOOR,
                    roomState = RoomFloorState(
                        length = "2",
                        width = "2",
                        doorwayWidths = emptyList(),
                        hasSkirting = false
                    )
                )
            )
        )

        val result = TileCalculatorEngine.calculateJob(job)

        assertEquals("Total net floor area must be 16.0 m²", 16.0, result.floorAreaM2, 0.001)
        assertEquals("Raw floor tiles needed must be 49", 49, result.floorTilesRawNeeded)
        assertEquals("Total floor tiles needed must be 49", 49, result.totalFloorTilesNeeded)
        assertEquals("Boxes to buy must be 13", 13, result.floorBoxesToBuy)
        assertEquals("Spare tiles must be 3", 3, result.floorSpareTiles)
        assertEquals("Area breakdowns size must be 2", 2, result.areaBreakdowns.size)
        assertEquals("Room A net floor area must be 12.0 m²", 12.0, result.areaBreakdowns[0].netFloorAreaM2, 0.001)
        assertEquals("Room B net floor area must be 4.0 m²", 4.0, result.areaBreakdowns[1].netFloorAreaM2, 0.001)
    }

    @Test
    fun testJobPersistenceRoundTrip() {
        val job = Job(
            id = "job_persistence_123",
            name = "House Renovation",
            clientName = "Alice Smith",
            addressOrPlot = "Plot 890, Broadhurst",
            notes = "Kitchen and dining renovation",
            unitSetting = UnitSystem.METERS,
            tileSettings = FloorTileSettingsState(
                preset = TilePreset.SIZE_60X60,
                tilesPerBox = "4",
                wastePercentage = 10
            ),
            areas = listOf(
                AreaItem(
                    id = "area_1",
                    name = "Living Room",
                    type = SpaceType.ROOM_FLOOR,
                    roomState = RoomFloorState(length = "5", width = "4")
                ),
                AreaItem(
                    id = "area_2",
                    name = "Back Veranda",
                    type = SpaceType.VERANDA,
                    verandaState = VerandaState(length = "4", width = "2.5")
                )
            )
        )
        val profile = UserProfile(builderName = "Bob Tiling", builderPhone = "+267 71000000")

        val serialized = com.example.data.JobJsonSerializer.createBackupJson(listOf(job), job.id, profile)
        assertNotNull(serialized)
        assertTrue(serialized.contains("House Renovation"))
        assertTrue(serialized.contains("Bob Tiling"))

        val parsed = com.example.data.JobJsonSerializer.parseBackupJson(serialized)
        assertNotNull("Parsed backup should not be null", parsed)
        val (jobsList, activeId, loadedProfile) = parsed!!
        assertEquals(1, jobsList.size)
        assertEquals("House Renovation", jobsList[0].name)
        assertEquals("Alice Smith", jobsList[0].clientName)
        assertEquals(2, jobsList[0].areas.size)
        assertEquals("Living Room", jobsList[0].areas[0].name)
        assertEquals("Back Veranda", jobsList[0].areas[1].name)
        assertEquals(job.id, activeId)
        assertEquals("Bob Tiling", loadedProfile.builderName)
        assertEquals("+267 71000000", loadedProfile.builderPhone)
    }

    @Test
    fun testQuotationQuickListTestCase() {
        // TEST CASE:
        // Using the job from the last test (Room A 4 m x 3 m and Room B 2 m x 2 m, 60x60 tiles, 4 per box, 10% waste):
        // - The Quick list must contain "Floor tiles 60x60: 13 boxes (4 per box)", plus the adhesive, grout, spacers and bonding liquid lines.
        val job = Job(
            id = "test_job_quick_list",
            name = "Two Rooms Test Job",
            clientName = "Sample Client",
            addressOrPlot = "Plot 1234, Gaborone",
            notes = "Test case: Room A (4x3) + Room B (2x2) with 60x60 tiles",
            unitSetting = UnitSystem.METERS,
            tileSettings = FloorTileSettingsState(
                preset = TilePreset.SIZE_60X60,
                tilesPerBox = "4",
                wastePercentage = 10
            ),
            materialSettings = MaterialSettingsState(),
            trimSettings = TrimSettingsState(),
            areas = listOf(
                AreaItem(
                    id = "area_a",
                    name = "Room A",
                    type = SpaceType.ROOM_FLOOR,
                    roomState = RoomFloorState(length = "4", width = "3", doorwayWidths = emptyList(), hasSkirting = false)
                ),
                AreaItem(
                    id = "area_b",
                    name = "Room B",
                    type = SpaceType.ROOM_FLOOR,
                    roomState = RoomFloorState(length = "2", width = "2", doorwayWidths = emptyList(), hasSkirting = false)
                )
            )
        )
        val result = TileCalculatorEngine.calculateJob(job)
        val quickList = QuotationEngine.generateQuickList(job, result)

        assertTrue(
            "Quick list must contain 'Floor tiles 60x60: 13 boxes (4 per box)'",
            quickList.contains("Floor tiles 60x60: 13 boxes (4 per box)")
        )
        assertTrue("Quick list must contain adhesive line", quickList.contains("Tile adhesive"))
        assertTrue("Quick list must contain grout line", quickList.contains("Tile grout"))
        assertTrue("Quick list must contain spacers line", quickList.contains("Tile spacers"))
        assertTrue("Quick list must contain bonding liquid line", quickList.contains("Bonding liquid"))
        assertTrue("Quick list must start with job name", quickList.startsWith("Two Rooms Test Job"))
    }

    @Test
    fun testDetailedQuotationTestCase() {
        // TEST CASE:
        // - The Detailed quotation must show both areas, a total area of 16 m², and the builder name and phone from Settings.
        val job = Job(
            id = "test_job_detailed",
            name = "Two Rooms Test Job",
            clientName = "Sample Client",
            addressOrPlot = "Plot 1234, Gaborone",
            notes = "Test case: Room A (4x3) + Room B (2x2) with 60x60 tiles",
            unitSetting = UnitSystem.METERS,
            tileSettings = FloorTileSettingsState(
                preset = TilePreset.SIZE_60X60,
                tilesPerBox = "4",
                wastePercentage = 10
            ),
            materialSettings = MaterialSettingsState(),
            trimSettings = TrimSettingsState(),
            areas = listOf(
                AreaItem(
                    id = "area_a",
                    name = "Room A",
                    type = SpaceType.ROOM_FLOOR,
                    roomState = RoomFloorState(length = "4", width = "3", doorwayWidths = emptyList(), hasSkirting = false)
                ),
                AreaItem(
                    id = "area_b",
                    name = "Room B",
                    type = SpaceType.ROOM_FLOOR,
                    roomState = RoomFloorState(length = "2", width = "2", doorwayWidths = emptyList(), hasSkirting = false)
                )
            )
        )
        val userProfile = UserProfile(builderName = "Bob Tiling", builderPhone = "+267 71000000")
        val result = TileCalculatorEngine.calculateJob(job)

        val quotation = QuotationEngine.generateDetailedQuotation(
            job = job,
            result = result,
            userProfile = userProfile,
            prices = QuotationPrices()
        )

        assertTrue("Quotation must show builder name", quotation.contains("Bob Tiling"))
        assertTrue("Quotation must show builder phone", quotation.contains("+267 71000000"))
        assertTrue("Quotation must show Room A", quotation.contains("Room A"))
        assertTrue("Quotation must show Room B", quotation.contains("Room B"))
        assertTrue("Quotation must show total area of 16 m²", quotation.contains("16 m²"))
        assertTrue("Quotation must have end disclaimer", quotation.contains("Quantities are estimates. Final amounts depend on site conditions."))
    }

    @Test
    fun testDetailedQuotationWithPriceTestCase() {
        // TEST CASE:
        // - Enter a price of 150 for floor tiles per box. The line total must be 1,950 and the quotation total must show 1,950.
        val job = Job(
            id = "test_job_pricing",
            name = "Two Rooms Test Job",
            clientName = "Sample Client",
            addressOrPlot = "Plot 1234, Gaborone",
            notes = "Test case: Room A (4x3) + Room B (2x2) with 60x60 tiles",
            unitSetting = UnitSystem.METERS,
            tileSettings = FloorTileSettingsState(
                preset = TilePreset.SIZE_60X60,
                tilesPerBox = "4",
                wastePercentage = 10
            ),
            areas = listOf(
                AreaItem(
                    id = "area_a",
                    name = "Room A",
                    type = SpaceType.ROOM_FLOOR,
                    roomState = RoomFloorState(length = "4", width = "3")
                ),
                AreaItem(
                    id = "area_b",
                    name = "Room B",
                    type = SpaceType.ROOM_FLOOR,
                    roomState = RoomFloorState(length = "2", width = "2")
                )
            )
        )
        val userProfile = UserProfile(builderName = "Bob Tiling", builderPhone = "+267 71000000")
        val result = TileCalculatorEngine.calculateJob(job)

        val prices = QuotationPrices(
            currency = "P",
            floorTilePricePerBox = "150"
        )

        val quotation = QuotationEngine.generateDetailedQuotation(
            job = job,
            result = result,
            userProfile = userProfile,
            prices = prices
        )

        // Line total: 13 boxes * 150 = 1,950
        assertTrue("Quotation must contain line total 1,950", quotation.contains("1,950"))
        assertTrue("Quotation must contain 'Quotation Total: P 1,950'", quotation.contains("Quotation Total: P 1,950"))
    }

    @Test
    fun testTestCaseF() {
        // TEST CASE F: Room 4 m x 3 m, 60x60 tiles, 4 tiles per box, 10% waste, no skirting.
        // - Base floor tiles = 34, floor tiles with waste = 37, waste allowance = 3 tiles
        // - Boxes = 10, spare tiles left over after boxes = 3
        val result = TileCalculatorEngine.calculate(
            spaceType = SpaceType.ROOM_FLOOR,
            unit = UnitSystem.METERS,
            tileSettings = FloorTileSettingsState(
                preset = TilePreset.SIZE_60X60,
                tilesPerBox = "4",
                wastePercentage = 10
            ),
            materialSettings = MaterialSettingsState(),
            roomState = RoomFloorState(
                length = "4",
                width = "3",
                doorwayWidths = emptyList(),
                hasSkirting = false
            ),
            verandaState = VerandaState(),
            bathroomState = BathroomState()
        )

        assertEquals("Base floor tiles must be 34", 34, result.baseFloorTiles)
        assertEquals("Floor tiles with waste must be 37", 37, result.floorTilesRawNeeded)
        assertEquals("Waste allowance must be 3 tiles", 3, result.floorWasteAllowanceTiles)
        assertEquals("Boxes must be 10", 10, result.floorBoxesToBuy)
        assertEquals("Spare tiles left over after boxes must be 3", 3, result.floorSpareTiles)
    }

    @Test
    fun testTestCaseFInJob() {
        // Whole job mode: Room 4 m x 3 m, 60x60 tiles, 4 per box, 10% waste, no skirting
        val job = Job(
            id = "test_job_case_f",
            name = "Test Job F",
            clientName = "Client F",
            unitSetting = UnitSystem.METERS,
            tileSettings = FloorTileSettingsState(
                preset = TilePreset.SIZE_60X60,
                tilesPerBox = "4",
                wastePercentage = 10
            ),
            areas = listOf(
                AreaItem(
                    id = "area_f",
                    name = "Room 4x3",
                    type = SpaceType.ROOM_FLOOR,
                    roomState = RoomFloorState(length = "4", width = "3", doorwayWidths = emptyList(), hasSkirting = false)
                )
            )
        )

        val result = TileCalculatorEngine.calculateJob(job)
        assertEquals("Base floor tiles must be 34", 34, result.baseFloorTiles)
        assertEquals("Floor tiles with waste must be 37", 37, result.floorTilesRawNeeded)
        assertEquals("Waste allowance must be 3 tiles", 3, result.floorWasteAllowanceTiles)
        assertEquals("Boxes must be 10", 10, result.floorBoxesToBuy)
        assertEquals("Spare tiles left over after boxes must be 3", 3, result.floorSpareTiles)
    }

    @Test
    fun testPartA_Test1_TallStepFaceFullRows() {
        // 1) 40x40 tiles, step face 4 m long x 80 cm high: 2 full rows, 10 tiles per row = 20 tiles, no warnings.
        val res = TileFormulas.calculateStrips(
            tileLengthM = 0.4,
            tileWidthM = 0.4,
            stripHeightM = 0.8,
            runLengthM = 4.0,
            label = "Step face, front"
        )
        assertEquals("full rows must be 2", 2, res.fullRows)
        assertEquals("full-row tiles must be 20", 20, res.fullRowTiles)
        assertEquals("leftover tiles must be 0", 0, res.leftoverTiles)
        assertEquals("total tiles must be 20", 20, res.tilesNeeded)
        assertEquals("no warnings", null, res.warning)
    }

    @Test
    fun testPartA_Test2_ShortStepFaceStrips() {
        // 2) 40x40 tiles, step face 3 m x 10 cm: strips per tile = 4, strips needed = 8, tiles = 2.
        val res = TileFormulas.calculateStrips(
            tileLengthM = 0.4,
            tileWidthM = 0.4,
            stripHeightM = 0.10,
            runLengthM = 3.0,
            label = "Step face"
        )
        assertEquals("strips per tile = 4", 4, res.stripsPerTile)
        assertEquals("strips needed = 8", 8, res.stripsNeeded)
        assertEquals("tiles = 2", 2, res.tilesNeeded)
        assertEquals("no warnings", null, res.warning)
    }

    @Test
    fun testPartA_Test3_StepFaceFullRowsPlusLeftover() {
        // 3) 40x40 tiles, step face 3 m x 50 cm: 1 full row (8 tiles) + leftover 10 cm: 8 strips, 4 per tile = 2 tiles, total = 10 tiles.
        val res = TileFormulas.calculateStrips(
            tileLengthM = 0.4,
            tileWidthM = 0.4,
            stripHeightM = 0.50,
            runLengthM = 3.0,
            label = "Step face"
        )
        assertEquals("full rows = 1", 1, res.fullRows)
        assertEquals("full-row tiles = 8", 8, res.fullRowTiles)
        assertEquals("leftover tiles = 2", 2, res.leftoverTiles)
        assertEquals("total tiles = 10", 10, res.tilesNeeded)
        assertEquals("no warnings", null, res.warning)
    }

    @Test
    fun testPartA_Test4_DoorStrip() {
        // 4) Door strip 11 cm over 0.81 m with 40x40 tiles: strips per tile = 3, strips needed = 3, tiles = 1.
        val res = TileFormulas.calculateStrips(
            tileLengthM = 0.4,
            tileWidthM = 0.4,
            stripHeightM = 0.11,
            runLengthM = 0.81,
            label = "Door strip"
        )
        assertEquals("strips per tile = 3", 3, res.stripsPerTile)
        assertEquals("strips needed = 3", 3, res.stripsNeeded)
        assertEquals("tiles = 1", 1, res.tilesNeeded)
        assertEquals("no warnings", null, res.warning)
    }

    @Test
    fun testPartA_Test5_PassageDoorsAndSkirting() {
        // 5) Passage 6 m x 1.2 m, 60x60 tiles, 4 per box, 10% waste, skirting 10 cm, 3 doors of 0.9 m:
        // floor tiles = 22, skirting length = 14.4 - 2.7 = 11.7 m.
        // Changing the door count changes the skirting length but never the floor tile count.
        val job3Doors = Job(
            id = "passage_3_doors",
            name = "Passage",
            unitSetting = UnitSystem.METERS,
            tileSettings = FloorTileSettingsState(
                preset = TilePreset.SIZE_60X60,
                tilesPerBox = "4",
                wastePercentage = 10
            ),
            areas = listOf(
                AreaItem(
                    id = "passage_area",
                    name = "Passage",
                    type = SpaceType.ROOM_FLOOR,
                    roomState = RoomFloorState(
                        length = "6",
                        width = "1.2",
                        hasSkirting = true,
                        skirtingPreset = SkirtingPreset.HEIGHT_10CM,
                        doorwayWidths = listOf("0.9", "0.9", "0.9")
                    )
                )
            )
        )
        val res3Doors = TileCalculatorEngine.calculateJob(job3Doors)
        assertEquals("Floor tiles with waste must be 22", 22, res3Doors.floorTilesRawNeeded)
        // Check skirting explanation contains "Perimeter 14.40 m minus doors 2.70 m = 11.70 m"
        assertTrue(res3Doors.floorSkirtingFormulaExplanation.contains("11.70 m") || res3Doors.floorSkirtingFormulaExplanation.contains("14.40 m minus doors 2.70 m"))

        // Now test with 1 door: floor tiles must stay 22
        val job1Door = job3Doors.copy(
            areas = listOf(
                job3Doors.areas[0].copy(
                    roomState = job3Doors.areas[0].roomState.copy(
                        doorwayWidths = listOf("0.9")
                    )
                )
            )
        )
        val res1Door = TileCalculatorEngine.calculateJob(job1Door)
        assertEquals("Floor tiles with waste must remain 22 regardless of doors", 22, res1Door.floorTilesRawNeeded)
    }

    @Test
    fun testPartA_Test6_Room4x3BaseAndWaste() {
        // 6) Room 4 m x 3 m, 60x60 tiles, 4 per box, 10% waste, no skirting: base 34, with waste 37, waste allowance 3, boxes 10, spare 3.
        val job = Job(
            id = "room_4x3",
            name = "Room 4x3",
            unitSetting = UnitSystem.METERS,
            tileSettings = FloorTileSettingsState(
                preset = TilePreset.SIZE_60X60,
                tilesPerBox = "4",
                wastePercentage = 10
            ),
            areas = listOf(
                AreaItem(
                    id = "room_area",
                    name = "Room",
                    type = SpaceType.ROOM_FLOOR,
                    roomState = RoomFloorState(
                        length = "4",
                        width = "3",
                        hasSkirting = false,
                        doorwayWidths = emptyList()
                    )
                )
            )
        )
        val res = TileCalculatorEngine.calculateJob(job)
        assertEquals("Base floor tiles = 34", 34, res.baseFloorTiles)
        assertEquals("Floor tiles with waste = 37", 37, res.floorTilesRawNeeded)
        assertEquals("Waste allowance = 3", 3, res.floorWasteAllowanceTiles)
        assertEquals("Boxes = 10", 10, res.floorBoxesToBuy)
        assertEquals("Spare tiles = 3", 3, res.floorSpareTiles)
    }

    // ========================================================
    // USER TEST CASES FOR EDGE TRIMS (A, B, C, D, E)
    // ========================================================

    @Test
    fun testTrims_TestCaseA_Passage() {
        // A) Passage 6 m x 1.2 m, skirting on, 3 doors of 0.9 m.
        // Skirting length = 14.4 - 2.7 = 11.7 m.
        // Plastic = ceil( 11.7 x 1.1 / 2.5 ) = 6 pieces.
        // Changing the number of doors must change the length.
        val area3Doors = AreaItem(
            id = "passage_a",
            name = "Passage",
            type = SpaceType.ROOM_FLOOR,
            roomState = RoomFloorState(
                length = "6",
                width = "1.2",
                hasSkirting = true,
                skirtingPreset = SkirtingPreset.HEIGHT_10CM,
                doorwayWidths = listOf("0.9", "0.9", "0.9"),
                hasAlongSkirtingTrim = true,
                alongSkirtingTrimMaterial = TrimMaterial.PLASTIC
            )
        )
        val job3Doors = Job(id = "job_a", name = "Test A", areas = listOf(area3Doors))
        val resA = TileCalculatorEngine.calculateJob(job3Doors)

        assertEquals("Plastic trim run length must be 11.7 m", 11.7, resA.trims.plasticTotalRunLengthM, 0.001)
        assertEquals("Plastic trim pieces must be 6", 6, resA.trims.plasticPiecesNeeded)
        assertEquals("Metal trim pieces must be 0", 0, resA.trims.metalPiecesNeeded)

        // Changing the number of doors must change the length:
        val area2Doors = area3Doors.copy(
            roomState = area3Doors.roomState.copy(
                doorwayWidths = listOf("0.9", "0.9")
            )
        )
        val res2Doors = TileCalculatorEngine.calculateJob(Job(id = "job_a2", name = "Test A 2 doors", areas = listOf(area2Doors)))
        // 14.4 - 1.8 = 12.6 m
        assertEquals("Changing door count must change skirting trim length to 12.6 m", 12.6, res2Doors.trims.plasticTotalRunLengthM, 0.001)
        assertEquals("Plastic trim pieces for 12.6 m = ceil(12.6 * 1.1 / 2.5) = 6", 6, res2Doors.trims.plasticPiecesNeeded)
    }

    @Test
    fun testTrims_TestCaseB_VerandaStepsAndWalls() {
        // B) Veranda 2.4 m x 1.85 m. Front and left are steps (metal).
        // Back and right are walls (plastic).
        // Metal run = 4.25 m = 2 pieces. Plastic run = 4.25 m = 2 pieces.
        val veranda = AreaItem(
            id = "veranda_b",
            name = "Veranda",
            type = SpaceType.VERANDA,
            verandaState = VerandaState(
                length = "2.4",
                width = "1.85",
                frontType = VerandaSideType.OPENING_STEP, // metal default
                leftType = VerandaSideType.OPENING_STEP,  // metal default
                backType = VerandaSideType.WALL,          // plastic default
                rightType = VerandaSideType.WALL,         // plastic default
                hasDoor = false
            )
        )
        val resB = TileCalculatorEngine.calculateJob(Job(id = "job_b", name = "Test B", areas = listOf(veranda)))

        assertEquals("Metal run must be 4.25 m", 4.25, resB.trims.metalTotalRunLengthM, 0.001)
        assertEquals("Metal trim pieces must be 2", 2, resB.trims.metalPiecesNeeded)
        assertEquals("Plastic run must be 4.25 m", 4.25, resB.trims.plasticTotalRunLengthM, 0.001)
        assertEquals("Plastic trim pieces must be 2", 2, resB.trims.plasticPiecesNeeded)
    }

    @Test
    fun testTrims_TestCaseC_VerandaWithDoorOnWall() {
        // C) The same veranda with a 0.81 m door on the back wall:
        // plastic run = 4.25 - 0.81 = 3.44 m = 2 pieces.
        val veranda = AreaItem(
            id = "veranda_c",
            name = "Veranda",
            type = SpaceType.VERANDA,
            verandaState = VerandaState(
                length = "2.4",
                width = "1.85",
                frontType = VerandaSideType.OPENING_STEP,
                leftType = VerandaSideType.OPENING_STEP,
                backType = VerandaSideType.WALL,
                rightType = VerandaSideType.WALL,
                hasDoor = true,
                doorSide = VerandaSide.BACK,
                doorWidth = "0.81"
            )
        )
        val resC = TileCalculatorEngine.calculateJob(Job(id = "job_c", name = "Test C", areas = listOf(veranda)))

        assertEquals("Plastic run must be 3.44 m", 3.44, resC.trims.plasticTotalRunLengthM, 0.001)
        assertEquals("Plastic trim pieces must be 2", 2, resC.trims.plasticPiecesNeeded)
        assertEquals("Metal run must remain 4.25 m", 4.25, resC.trims.metalTotalRunLengthM, 0.001)
        assertEquals("Metal trim pieces must be 2", 2, resC.trims.metalPiecesNeeded)
    }

    @Test
    fun testTrims_TestCaseD_VerandaWithMetalPillars() {
        // D) The same veranda plus 2 pillars of 30 cm x 30 cm, both metal:
        // each pillar run = 2 x (0.3 + 0.3) = 1.2 m.
        // Metal total = 4.25 + 2.4 = 6.65 m = ceil( 6.65 x 1.1 / 2.5 ) = 3 pieces.
        val veranda = AreaItem(
            id = "veranda_d",
            name = "Veranda",
            type = SpaceType.VERANDA,
            verandaState = VerandaState(
                length = "2.4",
                width = "1.85",
                frontType = VerandaSideType.OPENING_STEP,
                leftType = VerandaSideType.OPENING_STEP,
                backType = VerandaSideType.WALL,
                rightType = VerandaSideType.WALL,
                hasDoor = true,
                doorSide = VerandaSide.BACK,
                doorWidth = "0.81",
                pillars = listOf(
                    PillarItem(id = "p1", widthCm = "30", depthCm = "30", material = TrimMaterial.METAL, isEnabled = true),
                    PillarItem(id = "p2", widthCm = "30", depthCm = "30", material = TrimMaterial.METAL, isEnabled = true)
                )
            )
        )
        val resD = TileCalculatorEngine.calculateJob(Job(id = "job_d", name = "Test D", areas = listOf(veranda)))

        assertEquals("Metal run must be 6.65 m", 6.65, resD.trims.metalTotalRunLengthM, 0.001)
        assertEquals("Metal trim pieces must be 3", 3, resD.trims.metalPiecesNeeded)
        assertEquals("Plastic run must be 3.44 m", 3.44, resD.trims.plasticTotalRunLengthM, 0.001)
        assertEquals("Plastic trim pieces must be 2", 2, resD.trims.plasticPiecesNeeded)
    }

    @Test
    fun testTrims_TestCaseE_VerandaWithPlasticPillars() {
        // E) Switch the pillars to plastic:
        // the plastic total goes up, the metal total goes back to 2 pieces.
        val veranda = AreaItem(
            id = "veranda_e",
            name = "Veranda",
            type = SpaceType.VERANDA,
            verandaState = VerandaState(
                length = "2.4",
                width = "1.85",
                frontType = VerandaSideType.OPENING_STEP,
                leftType = VerandaSideType.OPENING_STEP,
                backType = VerandaSideType.WALL,
                rightType = VerandaSideType.WALL,
                hasDoor = true,
                doorSide = VerandaSide.BACK,
                doorWidth = "0.81",
                pillars = listOf(
                    PillarItem(id = "p1", widthCm = "30", depthCm = "30", material = TrimMaterial.PLASTIC, isEnabled = true),
                    PillarItem(id = "p2", widthCm = "30", depthCm = "30", material = TrimMaterial.PLASTIC, isEnabled = true)
                )
            )
        )
        val resE = TileCalculatorEngine.calculateJob(Job(id = "job_e", name = "Test E", areas = listOf(veranda)))

        // Plastic total = 3.44 + 2.4 = 5.84 m -> ceil(5.84 * 1.1 / 2.5) = 3 pieces
        assertEquals("Plastic run must be 5.84 m", 5.84, resE.trims.plasticTotalRunLengthM, 0.001)
        assertEquals("Plastic trim pieces must be 3", 3, resE.trims.plasticPiecesNeeded)
        // Metal total = 4.25 m -> ceil(4.25 * 1.1 / 2.5) = 2 pieces
        assertEquals("Metal run must return to 4.25 m", 4.25, resE.trims.metalTotalRunLengthM, 0.001)
        assertEquals("Metal trim pieces must be 2", 2, resE.trims.metalPiecesNeeded)
    }

    @Test
    fun testAreaSizeWording_MetersAndCentimeters() {
        val verandaMeter = AreaItem(
            id = "v_meter",
            name = "Main Veranda",
            type = SpaceType.VERANDA,
            verandaState = VerandaState(length = "2.05", width = "1.85")
        )
        val meterStr = TileCalculatorEngine.formatAreaSize(verandaMeter, UnitSystem.METERS)
        assertEquals("2.05 m x 1.85 m", meterStr)

        val verandaCm = AreaItem(
            id = "v_cm",
            name = "Main Veranda",
            type = SpaceType.VERANDA,
            verandaState = VerandaState(length = "205", width = "185")
        )
        val cmStr = TileCalculatorEngine.formatAreaSize(verandaCm, UnitSystem.CENTIMETERS)
        assertEquals("205 cm x 185 cm", cmStr)
        assertTrue("Centimeter size string must not contain meter symbol 'm'", !cmStr.contains(" m"))
    }

    @Test
    fun testAdhesiveBagRounding_CombinedUnrounded() {
        // TEST: floor 3.79 m² at an 8 mm bed with 3.0 m² per bag at 5 mm (coverage 1.875 m²),
        // skirting and step area 0.62 m² on a 3 mm bed (coverage 5.0 m²).
        // Floor 2.02 + skirting and step 0.12 = 2.14, so 3 bags. Bonding liquid = 3 x 250 ml = 750 ml.
        val materialsSettings = MaterialSettingsState(
            baseCoverageAt5mm = "3.0",
            floorBedPreset = FloorBedThicknessPreset.CUSTOM,
            customFloorBedMm = "8",
            wallBedPreset = WallBedThicknessPreset.CUSTOM,
            customWallBedMm = "3",
            bondingMode = BondingLiquidMode.ADDITIVE_SPLASH,
            additiveMlPerBag = "250"
        )

        val matResult = TileCalculatorEngine.calculateMaterials(
            floorAreaM2 = 3.79,
            skirtingAndStepAreaM2 = 0.62,
            wallAreaM2 = 0.0,
            floorTileLengthM = 0.6,
            floorTileWidthM = 0.6,
            hasWallTiles = false,
            wallTileLengthM = 0.0,
            wallTileWidthM = 0.0,
            totalTilesCount = 50,
            materialSettings = materialsSettings
        )

        // Floor coverage = 3.0 * (5/8) = 1.875
        assertEquals(1.875, matResult.floorCoverageM2PerBag, 0.001)
        // Wall coverage = 3.0 * (5/3) = 5.0
        assertEquals(5.0, matResult.wallCoverageM2PerBag, 0.001)

        // Unrounded: Floor 3.79 / 1.875 = 2.02133..., Wall/Skirting 0.62 / 5.0 = 0.124
        assertEquals(2.0213, matResult.floorAdhesiveBagsRaw, 0.001)
        assertEquals(0.124, matResult.wallAdhesiveBagsRaw, 0.001)
        assertEquals(2.1453, matResult.totalAdhesiveBagsRaw, 0.001)

        // Total rounded once at the end: ceil(2.1453) = 3 bags (NOT separate 3 + 1 = 4)
        assertEquals(3, matResult.totalAdhesiveBags)

        // Bonding liquid: 3 x 250 ml = 750 ml
        assertEquals(0.75, matResult.totalBondingLiquidLiters, 0.001)
        assertEquals("750 ml", matResult.bondingLiquidDisplayText)

        // Explanation text verification
        assertTrue("Explanation should contain unrounded addition and ceil",
            matResult.adhesiveFormulaExplanation.contains("Floor 2.02") &&
            matResult.adhesiveFormulaExplanation.contains("0.12") &&
            matResult.adhesiveFormulaExplanation.contains("3 bags")
        )
    }
}


