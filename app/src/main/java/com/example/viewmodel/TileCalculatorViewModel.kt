package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LocalStorageManager
import com.example.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class CurrentScreen {
    JOBS_LIST,
    JOB_DETAIL,
    AREA_EDITOR,
    QUOTATION,
    SETTINGS,
    HELP
}

class TileCalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = LocalStorageManager(application)

    private val _currentScreen = MutableStateFlow(CurrentScreen.JOBS_LIST)
    val currentScreen: StateFlow<CurrentScreen> = _currentScreen.asStateFlow()

    private var previousScreenBeforeHelp: CurrentScreen = CurrentScreen.JOBS_LIST

    private val _hasSeenHelp = MutableStateFlow(false)
    val hasSeenHelp: StateFlow<Boolean> = _hasSeenHelp.asStateFlow()

    private val _jobs = MutableStateFlow<List<Job>>(emptyList())
    val jobs: StateFlow<List<Job>> = _jobs.asStateFlow()

    private val _activeJobId = MutableStateFlow<String?>(null)
    val activeJobId: StateFlow<String?> = _activeJobId.asStateFlow()

    private val _editingAreaId = MutableStateFlow<String?>(null)
    val editingAreaId: StateFlow<String?> = _editingAreaId.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _savedNotification = MutableStateFlow<String?>(null)
    val savedNotification: StateFlow<String?> = _savedNotification.asStateFlow()

    init {
        loadFromStorage()
    }

    private fun loadFromStorage() {
        val loadedJobs = storage.loadJobs()
        val loadedProfile = storage.loadUserProfile()
        val loadedActiveId = storage.loadActiveJobId()
        _hasSeenHelp.value = storage.hasSeenHelp()

        _jobs.value = loadedJobs
        _userProfile.value = loadedProfile
        _activeJobId.value = loadedActiveId

        // If active job exists in loaded jobs, we can remain or go to JOBS_LIST
        // Default to JOBS_LIST as the home screen
        _currentScreen.value = CurrentScreen.JOBS_LIST
    }

    val activeJob: StateFlow<Job?> = combine(_jobs, _activeJobId) { jobsList, activeId ->
        jobsList.find { it.id == activeId }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val activeJobResult: StateFlow<CalculationResult> = activeJob.map { job ->
        if (job != null) {
            TileCalculatorEngine.calculateJob(job)
        } else {
            TileCalculatorEngine.calculate(
                spaceType = SpaceType.ROOM_FLOOR,
                unit = UnitSystem.METERS,
                tileSettings = FloorTileSettingsState(),
                materialSettings = MaterialSettingsState(),
                trimSettings = TrimSettingsState(),
                roomState = RoomFloorState(),
                verandaState = VerandaState(),
                bathroomState = BathroomState()
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TileCalculatorEngine.calculateJob(Job(name = "Initial", areas = emptyList()))
    )

    val editingArea: StateFlow<AreaItem?> = combine(activeJob, _editingAreaId) { job, areaId ->
        job?.areas?.find { it.id == areaId }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private fun autoSave() {
        storage.saveJobs(_jobs.value)
        storage.saveActiveJobId(_activeJobId.value)
        storage.saveUserProfile(_userProfile.value)
    }

    fun saveExplicitly() {
        autoSave()
        _savedNotification.value = "Saved!"
    }

    fun clearSavedNotification() {
        _savedNotification.value = null
    }

    // Navigation
    fun navigateTo(screen: CurrentScreen) {
        if (screen == CurrentScreen.HELP) {
            previousScreenBeforeHelp = _currentScreen.value
        }
        _currentScreen.value = screen
    }

    fun openHelp() {
        previousScreenBeforeHelp = _currentScreen.value
        _currentScreen.value = CurrentScreen.HELP
    }

    fun markHelpAsSeen() {
        storage.setSeenHelp(true)
        _hasSeenHelp.value = true
    }

    fun loadExampleJob(): Job {
        val exampleJob = Job(
            id = UUID.randomUUID().toString(),
            name = "Example: Room 4m x 3m",
            clientName = "Sample Client",
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
                    id = UUID.randomUUID().toString(),
                    name = "Room 4x3",
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
        _jobs.value = _jobs.value + exampleJob
        _activeJobId.value = exampleJob.id
        markHelpAsSeen()
        autoSave()
        _currentScreen.value = CurrentScreen.JOB_DETAIL
        return exampleJob
    }

    fun navigateBack() {
        when (_currentScreen.value) {
            CurrentScreen.HELP -> {
                _currentScreen.value = previousScreenBeforeHelp
            }
            CurrentScreen.AREA_EDITOR -> {
                _editingAreaId.value = null
                _currentScreen.value = CurrentScreen.JOB_DETAIL
            }
            CurrentScreen.QUOTATION -> {
                _currentScreen.value = CurrentScreen.JOB_DETAIL
            }
            CurrentScreen.JOB_DETAIL -> {
                _currentScreen.value = CurrentScreen.JOBS_LIST
            }
            CurrentScreen.SETTINGS -> {
                _currentScreen.value = CurrentScreen.JOBS_LIST
            }
            CurrentScreen.JOBS_LIST -> {
                // Already at root
            }
        }
    }

    // Job Management
    fun createNewJob(name: String = "", client: String = ""): Job {
        val nextIndex = _jobs.value.size + 1
        val jobName = name.ifBlank { "Job $nextIndex" }
        val newJob = Job(
            id = UUID.randomUUID().toString(),
            name = jobName,
            clientName = client,
            unitSetting = UnitSystem.METERS,
            tileSettings = FloorTileSettingsState(preset = TilePreset.SIZE_60X60, tilesPerBox = "4", wastePercentage = 10),
            materialSettings = MaterialSettingsState(),
            trimSettings = TrimSettingsState(),
            areas = emptyList()
        )
        _jobs.value = _jobs.value + newJob
        _activeJobId.value = newJob.id
        autoSave()
        _currentScreen.value = CurrentScreen.JOB_DETAIL
        return newJob
    }

    fun openJob(jobId: String) {
        _activeJobId.value = jobId
        storage.saveActiveJobId(jobId)
        _currentScreen.value = CurrentScreen.JOB_DETAIL
    }

    fun renameJob(jobId: String, newName: String) {
        _jobs.value = _jobs.value.map {
            if (it.id == jobId) it.copy(name = newName) else it
        }
        autoSave()
    }

    fun duplicateJob(jobId: String) {
        val jobToDup = _jobs.value.find { it.id == jobId } ?: return
        val newId = UUID.randomUUID().toString()
        val duplicatedAreas = jobToDup.areas.map { it.copy(id = UUID.randomUUID().toString()) }
        val duplicatedJob = jobToDup.copy(
            id = newId,
            name = "${jobToDup.name} (Copy)",
            areas = duplicatedAreas
        )
        _jobs.value = _jobs.value + duplicatedJob
        autoSave()
    }

    fun deleteJob(jobId: String) {
        _jobs.value = _jobs.value.filter { it.id != jobId }
        if (_activeJobId.value == jobId) {
            _activeJobId.value = _jobs.value.firstOrNull()?.id
        }
        autoSave()
        if (_currentScreen.value == CurrentScreen.JOB_DETAIL && _activeJobId.value == null) {
            _currentScreen.value = CurrentScreen.JOBS_LIST
        }
    }

    fun updateActiveJob(update: (Job) -> Job) {
        val currentId = _activeJobId.value ?: return
        _jobs.value = _jobs.value.map {
            if (it.id == currentId) update(it) else it
        }
        autoSave()
    }

    // Active Job Properties
    fun setJobUnitSystem(unit: UnitSystem) {
        updateActiveJob { it.copy(unitSetting = unit) }
    }

    fun updateJobTileSettings(update: (FloorTileSettingsState) -> FloorTileSettingsState) {
        updateActiveJob { it.copy(tileSettings = update(it.tileSettings)) }
    }

    fun updateJobMaterialSettings(update: (MaterialSettingsState) -> MaterialSettingsState) {
        updateActiveJob { it.copy(materialSettings = update(it.materialSettings)) }
    }

    fun updateJobTrimSettings(update: (TrimSettingsState) -> TrimSettingsState) {
        updateActiveJob { it.copy(trimSettings = update(it.trimSettings)) }
    }

    // Outer corner trim runs helper for Job
    fun addOuterCorner() {
        updateJobTrimSettings {
            val nextNum = it.outerCorners.size + 1
            it.copy(
                outerCorners = it.outerCorners + OuterCornerItem(
                    id = "corner_${System.currentTimeMillis()}",
                    name = "Outer Corner $nextNum",
                    lengthM = "2.4",
                    material = TrimMaterial.PLASTIC,
                    isEnabled = true
                )
            )
        }
    }

    fun removeOuterCorner(index: Int) {
        updateJobTrimSettings {
            val list = it.outerCorners.toMutableList()
            if (index in list.indices) list.removeAt(index)
            it.copy(outerCorners = list)
        }
    }

    fun updateOuterCorner(index: Int, name: String, lengthM: String, material: TrimMaterial, isEnabled: Boolean) {
        updateJobTrimSettings {
            val list = it.outerCorners.toMutableList()
            if (index in list.indices) {
                list[index] = list[index].copy(name = name, lengthM = lengthM, material = material, isEnabled = isEnabled)
            }
            it.copy(outerCorners = list)
        }
    }

    // Custom trim runs helper for Job
    fun addCustomTrimRun() {
        updateJobTrimSettings {
            val nextNum = it.customRuns.size + 1
            it.copy(
                customRuns = it.customRuns + CustomTrimRunItem(
                    id = "custom_${System.currentTimeMillis()}",
                    name = "Custom Run $nextNum",
                    lengthM = "2.0",
                    material = TrimMaterial.PLASTIC,
                    isEnabled = true
                )
            )
        }
    }

    fun removeCustomTrimRun(index: Int) {
        updateJobTrimSettings {
            val list = it.customRuns.toMutableList()
            if (index in list.indices) list.removeAt(index)
            it.copy(customRuns = list)
        }
    }

    fun updateCustomTrimRun(index: Int, name: String, lengthM: String, material: TrimMaterial, isEnabled: Boolean) {
        updateJobTrimSettings {
            val list = it.customRuns.toMutableList()
            if (index in list.indices) {
                list[index] = list[index].copy(name = name, lengthM = lengthM, material = material, isEnabled = isEnabled)
            }
            it.copy(customRuns = list)
        }
    }

    // Area Management within Active Job
    fun addArea(type: SpaceType, customName: String? = null): AreaItem {
        val currentJob = activeJob.value ?: return AreaItem(name = "Area", type = type)
        val typeCount = currentJob.areas.count { it.type == type } + 1
        val areaName = customName ?: when (type) {
            SpaceType.ROOM_FLOOR -> if (typeCount == 1) "Room A" else "Room $typeCount"
            SpaceType.VERANDA -> if (typeCount == 1) "Main Veranda" else "Veranda $typeCount"
            SpaceType.BATHROOM -> if (typeCount == 1) "Bathroom 1" else "Bathroom $typeCount"
        }

        val newArea = AreaItem(
            id = UUID.randomUUID().toString(),
            name = areaName,
            type = type,
            roomState = RoomFloorState(length = "4", width = "3", hasSkirting = false),
            verandaState = VerandaState(length = "4", width = "3"),
            bathroomState = BathroomState(length = "2.5", width = "2.0")
        )

        updateActiveJob { it.copy(areas = it.areas + newArea) }
        _editingAreaId.value = newArea.id
        _currentScreen.value = CurrentScreen.AREA_EDITOR
        return newArea
    }

    fun openAreaEditor(areaId: String) {
        _editingAreaId.value = areaId
        _currentScreen.value = CurrentScreen.AREA_EDITOR
    }

    fun closeAreaEditor() {
        _editingAreaId.value = null
        _currentScreen.value = CurrentScreen.JOB_DETAIL
    }

    fun updateArea(areaId: String, update: (AreaItem) -> AreaItem) {
        updateActiveJob { job ->
            job.copy(
                areas = job.areas.map { if (it.id == areaId) update(it) else it }
            )
        }
    }

    fun deleteArea(areaId: String) {
        updateActiveJob { job ->
            job.copy(areas = job.areas.filter { it.id != areaId })
        }
        if (_editingAreaId.value == areaId) {
            closeAreaEditor()
        }
    }

    // Editing Area specific update helpers
    fun updateEditingArea(update: (AreaItem) -> AreaItem) {
        val areaId = _editingAreaId.value ?: return
        updateArea(areaId, update)
    }

    fun updateEditingAreaName(name: String) {
        updateEditingArea { it.copy(name = name) }
    }

    fun updateEditingRoomState(update: (RoomFloorState) -> RoomFloorState) {
        updateEditingArea { it.copy(roomState = update(it.roomState)) }
    }

    fun updateEditingVerandaState(update: (VerandaState) -> VerandaState) {
        updateEditingArea { it.copy(verandaState = update(it.verandaState)) }
    }

    fun updateEditingBathroomState(update: (BathroomState) -> BathroomState) {
        updateEditingArea { it.copy(bathroomState = update(it.bathroomState)) }
    }

    // Doorway helpers for editing area
    fun addEditingRoomDoorway() {
        updateEditingRoomState { it.copy(doorwayWidths = it.doorwayWidths + listOf("0.9")) }
    }

    fun removeEditingRoomDoorway(index: Int) {
        updateEditingRoomState {
            val list = it.doorwayWidths.toMutableList()
            if (index in list.indices) list.removeAt(index)
            it.copy(doorwayWidths = list)
        }
    }

    fun updateEditingRoomDoorway(index: Int, width: String) {
        updateEditingRoomState {
            val list = it.doorwayWidths.toMutableList()
            if (index in list.indices) list[index] = width
            it.copy(doorwayWidths = list)
        }
    }

    // Bathroom helpers for editing area
    fun addEditingBathroomDoorway() {
        updateEditingBathroomState { it.copy(doorwayWidths = it.doorwayWidths + listOf("0.8")) }
    }

    fun removeEditingBathroomDoorway(index: Int) {
        updateEditingBathroomState {
            val list = it.doorwayWidths.toMutableList()
            if (index in list.indices) list.removeAt(index)
            it.copy(doorwayWidths = list)
        }
    }

    fun updateEditingBathroomDoorway(index: Int, width: String) {
        updateEditingBathroomState {
            val list = it.doorwayWidths.toMutableList()
            if (index in list.indices) list[index] = width
            it.copy(doorwayWidths = list)
        }
    }

    fun addEditingBathroomWall() {
        updateEditingBathroomState {
            val nextNum = it.walls.size + 1
            it.copy(walls = it.walls + WallDimension("w$nextNum", "Wall $nextNum", "2.0", "2.4"))
        }
    }

    fun removeEditingBathroomWall(index: Int) {
        updateEditingBathroomState {
            val list = it.walls.toMutableList()
            if (index in list.indices && list.size > 1) list.removeAt(index)
            it.copy(walls = list)
        }
    }

    fun updateEditingBathroomWall(index: Int, width: String, height: String) {
        updateEditingBathroomState {
            val list = it.walls.toMutableList()
            if (index in list.indices) {
                list[index] = list[index].copy(width = width, height = height)
            }
            it.copy(walls = list)
        }
    }

    fun addEditingBathroomWindow() {
        updateEditingBathroomState {
            val nextNum = it.windows.size + 1
            it.copy(windows = it.windows + OpeningDimension("win$nextNum", "0.6", "0.6"))
        }
    }

    fun removeEditingBathroomWindow(index: Int) {
        updateEditingBathroomState {
            val list = it.windows.toMutableList()
            if (index in list.indices) list.removeAt(index)
            it.copy(windows = list)
        }
    }

    fun updateEditingBathroomWindow(index: Int, width: String, height: String) {
        updateEditingBathroomState {
            val list = it.windows.toMutableList()
            if (index in list.indices) {
                list[index] = list[index].copy(width = width, height = height)
            }
            it.copy(windows = list)
        }
    }

    fun addEditingBathroomDoor() {
        updateEditingBathroomState {
            val nextNum = it.doors.size + 1
            it.copy(doors = it.doors + OpeningDimension("door$nextNum", "0.8", "2.0"))
        }
    }

    fun removeEditingBathroomDoor(index: Int) {
        updateEditingBathroomState {
            val list = it.doors.toMutableList()
            if (index in list.indices) list.removeAt(index)
            it.copy(doors = list)
        }
    }

    fun updateEditingBathroomDoor(index: Int, width: String, height: String) {
        updateEditingBathroomState {
            val list = it.doors.toMutableList()
            if (index in list.indices) {
                list[index] = list[index].copy(width = width, height = height)
            }
            it.copy(doors = list)
        }
    }

    // User Profile / Settings
    fun updateUserProfile(builderName: String, builderPhone: String) {
        val updated = UserProfile(builderName = builderName, builderPhone = builderPhone)
        _userProfile.value = updated
        storage.saveUserProfile(updated)
        _savedNotification.value = "Settings Saved!"
    }

    fun exportBackupJson(): String {
        return storage.exportBackup()
    }

    fun importBackupJson(jsonStr: String): Boolean {
        val success = storage.importBackup(jsonStr)
        if (success) {
            loadFromStorage()
            _savedNotification.value = "Backup restored successfully!"
        }
        return success
    }

    // TEST CASE 3 (JOBS SPECIFICATION):
    // Create one job with 60x60 tiles, 4 tiles per box and 10% waste.
    // Add two room floors: Room A is 4 m x 3 m and Room B is 2 m x 2 m. No skirting, no doorways.
    // - Total net floor area = 16 m²
    // - Tiles needed = ceil( 16 x 1.1 / 0.36 ) = ceil( 48.89 ) = 49
    // - Boxes = ceil( 49 / 4 ) = 13
    // - Spare tiles = 3
    fun loadTwoRoomTestCase() {
        val job = Job(
            id = "test_job_2_rooms_${System.currentTimeMillis()}",
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
                    id = "room_a",
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
                    id = "room_b",
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
        _jobs.value = _jobs.value + job
        _activeJobId.value = job.id
        autoSave()
        _currentScreen.value = CurrentScreen.JOB_DETAIL
    }
}
