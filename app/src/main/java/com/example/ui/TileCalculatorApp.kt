package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.TrimMaterial
import com.example.ui.screens.AreaEditorScreen
import com.example.ui.screens.JobDetailScreen
import com.example.ui.screens.JobsListScreen
import com.example.ui.screens.QuotationScreen
import com.example.ui.screens.SettingsScreen
import com.example.viewmodel.CurrentScreen
import com.example.viewmodel.TileCalculatorViewModel

@Composable
fun TileCalculatorApp(
    viewModel: TileCalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val jobs by viewModel.jobs.collectAsStateWithLifecycle()
    val activeJob by viewModel.activeJob.collectAsStateWithLifecycle()
    val calculationResult by viewModel.activeJobResult.collectAsStateWithLifecycle()
    val editingArea by viewModel.editingArea.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val savedNotification by viewModel.savedNotification.collectAsStateWithLifecycle()
    val hasSeenHelp by viewModel.hasSeenHelp.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(savedNotification) {
        savedNotification?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSavedNotification()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                CurrentScreen.JOBS_LIST -> {
                    JobsListScreen(
                        jobs = jobs,
                        onOpenJob = { viewModel.openJob(it) },
                        onNewJob = { viewModel.createNewJob() },
                        onRenameJob = { id, name -> viewModel.renameJob(id, name) },
                        onDuplicateJob = { viewModel.duplicateJob(it) },
                        onDeleteJob = { viewModel.deleteJob(it) },
                        onOpenSettings = { viewModel.navigateTo(CurrentScreen.SETTINGS) },
                        onLoadTwoRoomTestCase = { viewModel.loadTwoRoomTestCase() },
                        onOpenHelp = { viewModel.openHelp() },
                        hasSeenHelp = hasSeenHelp,
                        onDismissHelpBanner = { viewModel.markHelpAsSeen() },
                        onLoadExampleJob = { viewModel.loadExampleJob() }
                    )
                }

                CurrentScreen.JOB_DETAIL -> {
                    val currentJob = activeJob
                    if (currentJob != null) {
                        JobDetailScreen(
                            job = currentJob,
                            calculationResult = calculationResult,
                            userProfile = userProfile,
                            savedMessage = savedNotification,
                            onBack = { viewModel.navigateBack() },
                            onSave = { viewModel.saveExplicitly() },
                            onUpdateJob = { viewModel.updateActiveJob(it) },
                            onSetUnit = { viewModel.setJobUnitSystem(it) },
                            onUpdateTileSettings = { viewModel.updateJobTileSettings(it) },
                            onUpdateMaterialSettings = { viewModel.updateJobMaterialSettings(it) },
                            onUpdateTrimSettings = { viewModel.updateJobTrimSettings(it) },
                            onAddArea = { viewModel.addArea(it) },
                            onEditArea = { viewModel.openAreaEditor(it) },
                            onDeleteArea = { viewModel.deleteArea(it) },
                            onAddOuterCorner = { viewModel.addOuterCorner() },
                            onRemoveOuterCorner = { viewModel.removeOuterCorner(it) },
                            onUpdateOuterCorner = { idx, name, len, mat, enabled ->
                                viewModel.updateOuterCorner(idx, name, len, mat, enabled)
                            },
                            onAddCustomRun = { viewModel.addCustomTrimRun() },
                            onRemoveCustomRun = { viewModel.removeCustomTrimRun(it) },
                            onUpdateCustomRun = { idx, name, len, mat, enabled ->
                                viewModel.updateCustomTrimRun(idx, name, len, mat, enabled)
                            },
                            onOpenQuotation = { viewModel.navigateTo(CurrentScreen.QUOTATION) },
                            onOpenHelp = { viewModel.openHelp() }
                        )
                    } else {
                        // Fallback to Jobs List if no active job
                        LaunchedEffect(Unit) {
                            viewModel.navigateTo(CurrentScreen.JOBS_LIST)
                        }
                    }
                }

                CurrentScreen.AREA_EDITOR -> {
                    val area = editingArea
                    val job = activeJob
                    if (area != null && job != null) {
                        AreaEditorScreen(
                            area = area,
                            unit = job.unitSetting,
                            viewModel = viewModel,
                            onDone = { viewModel.closeAreaEditor() }
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            viewModel.navigateBack()
                        }
                    }
                }

                CurrentScreen.QUOTATION -> {
                    val currentJob = activeJob
                    if (currentJob != null) {
                        QuotationScreen(
                            job = currentJob,
                            calculationResult = calculationResult,
                            userProfile = userProfile,
                            onBack = { viewModel.navigateBack() }
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            viewModel.navigateBack()
                        }
                    }
                }

                CurrentScreen.SETTINGS -> {
                    SettingsScreen(
                        userProfile = userProfile,
                        onSaveProfile = { name, phone -> viewModel.updateUserProfile(name, phone) },
                        onExportBackup = { viewModel.exportBackupJson() },
                        onImportBackup = { viewModel.importBackupJson(it) },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                CurrentScreen.HELP -> {
                    com.example.ui.screens.HelpScreen(
                        onBack = { viewModel.navigateBack() }
                    )
                }
            }
        }
    }
}
