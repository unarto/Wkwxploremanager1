// [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/presentation/DualPaneViewModel.kt
// [Penjelasan]: Single Source of Truth untuk seluruh state panel ganda, operasi berkas, observasi storage volumes, dan perizinan penyimpanan Android.
package com.wakwau.xplore.filemanager.ui.presentation

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakwau.xplore.core.storage.operation.BackgroundOperationManager
import com.wakwau.xplore.core.storage.operation.BackgroundOperationType
import com.wakwau.xplore.core.storage.operation.FileOperationResult
import com.wakwau.xplore.core.storage.preferences.AppPreferencesRepository
import com.wakwau.xplore.filemanager.ui.action.CopyOperationHandler
import com.wakwau.xplore.filemanager.ui.action.CreateDirectoryOperationHandler
import com.wakwau.xplore.filemanager.ui.action.DeleteOperationHandler
import com.wakwau.xplore.filemanager.ui.action.FileDetailHandler
import com.wakwau.xplore.filemanager.ui.action.MoveOperationHandler
import com.wakwau.xplore.filemanager.ui.action.PanelNavigationHandler
import com.wakwau.xplore.filemanager.ui.action.PanelRefreshHandler
import com.wakwau.xplore.filemanager.ui.action.RenameOperationHandler
import com.wakwau.xplore.filemanager.ui.action.SearchOperationHandler
import com.wakwau.xplore.filemanager.ui.constant.FileOperationConstants
import com.wakwau.xplore.filemanager.ui.event.DualPaneEvent
import com.wakwau.xplore.filemanager.ui.reducer.DualPaneReducer
import com.wakwau.xplore.filemanager.ui.state.DualPaneState
import com.wakwau.xplore.filemanager.ui.state.DualPanelStateController
import com.wakwau.xplore.filemanager.ui.state.OperationUiState
import com.wakwau.xplore.filemanager.ui.state.PanelId
import com.wakwau.xplore.filemanager.ui.tree.TreeNavigationAdapter
import com.wakwau.xplore.filemanager.usecase.CheckStoragePermissionUseCase
import com.wakwau.xplore.filemanager.usecase.GetStorageVolumesUseCase
import com.wakwau.xplore.filemanager.usecase.LinkStorageUseCase
import com.wakwau.xplore.filemanager.usecase.ToggleShowHiddenFilesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DualPaneViewModel(
    private val reducer: DualPaneReducer,
    private val refreshHandler: PanelRefreshHandler,
    private val navigationHandler: PanelNavigationHandler,
    private val copyHandler: CopyOperationHandler,
    private val moveHandler: MoveOperationHandler,
    private val deleteHandler: DeleteOperationHandler,
    private val renameHandler: RenameOperationHandler,
    private val createDirectoryHandler: CreateDirectoryOperationHandler,
    private val fileDetailHandler: FileDetailHandler,
    private val searchOperationHandler: SearchOperationHandler,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val treeNavigationAdapter: TreeNavigationAdapter,
    private val toggleShowHiddenFilesUseCase: ToggleShowHiddenFilesUseCase,
    private val backgroundOperationManager: BackgroundOperationManager,
    private val checkStoragePermissionUseCase: CheckStoragePermissionUseCase,
    private val getStorageVolumesUseCase: GetStorageVolumesUseCase,
    private val linkStorageUseCase: LinkStorageUseCase
) : ViewModel() {

    // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/presentation/DualPaneViewModel.kt
    // [Penjelasan]: Mengontrol pergantian panel aktif melalui callback lambda dispatch MVI.
    val panelStateController = DualPanelStateController { panelId ->
        dispatch(DualPaneEvent.SetActivePanel(panelId))
    }
    private val _state = MutableStateFlow(
        DualPaneState(
            hasPermission = checkStoragePermissionUseCase.hasPermission(),
            requiredPermissionType = checkStoragePermissionUseCase.getRequiredPermission()
        )
    )
    val state: StateFlow<DualPaneState> = _state.asStateFlow()
    
    val preferencesState = appPreferencesRepository.preferencesState

    init {
        // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/presentation/DualPaneViewModel.kt
        // [Penjelasan]: Memulai pengamatan progress operasi berkas di latar belakang melalui backgroundOperationManager Flow.
        viewModelScope.launch {
            backgroundOperationManager.observeProgress().collect { result ->
                when (result) {
                    is FileOperationResult.Success -> {
                        dispatch(DualPaneEvent.OperationProgress(result.data))
                    }
                    is FileOperationResult.Failure -> {
                        dispatch(DualPaneEvent.OperationFailed(result.error.name))
                    }
                    is FileOperationResult.Cancelled -> {
                        dispatch(DualPaneEvent.OperationCancelled)
                    }
                    is FileOperationResult.Completed -> {
                        val messageRes = when (result.operationType) {
                            BackgroundOperationType.COPY -> FileOperationConstants.SUCCESS_COPY
                            BackgroundOperationType.MOVE -> FileOperationConstants.SUCCESS_MOVE
                            BackgroundOperationType.DELETE -> FileOperationConstants.SUCCESS_DELETE
                        }
                        dispatch(DualPaneEvent.OperationSuccess(messageRes))
                        dispatch(DualPaneEvent.Refresh(PanelId.LEFT))
                        dispatch(DualPaneEvent.Refresh(PanelId.RIGHT))
                    }
                }
            }
        }

        // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/presentation/DualPaneViewModel.kt
        // [Penjelasan]: Mengamati storage volume secara realtime dan menyalurkannya ke state panel ganda.
        viewModelScope.launch {
            dispatch(DualPaneEvent.StorageVolumesLoading(true))
            getStorageVolumesUseCase.refresh()
            getStorageVolumesUseCase().collect { volumes ->
                dispatch(DualPaneEvent.StorageVolumesUpdated(volumes))
            }
        }

        checkPermission()
    }

    fun checkPermission() {
        val hasPermission = checkStoragePermissionUseCase.hasPermission()
        val requiredType = checkStoragePermissionUseCase.getRequiredPermission()
        dispatch(DualPaneEvent.PermissionStatusUpdated(hasPermission, requiredType))
        if (hasPermission) {
            viewModelScope.launch {
                getStorageVolumesUseCase.refresh()
            }
        }
    }

    fun addLinkedStorage(uri: Uri) {
        viewModelScope.launch {
            linkStorageUseCase.addLinkedStorage(uri.toString())
            getStorageVolumesUseCase.refresh()
        }
    }

    fun removeLinkedStorage(uri: Uri) {
        viewModelScope.launch {
            linkStorageUseCase.removeLinkedStorage(uri.toString())
            getStorageVolumesUseCase.refresh()
        }
    }

    fun dispatch(event: DualPaneEvent) {
        val newState = reducer.reduce(_state.value, event)
        _state.value = newState
        
        handleSideEffects(event)
    }

    private fun handleSideEffects(event: DualPaneEvent) {
        val stateSnapshot = _state.value
        
        when (event) {
            is DualPaneEvent.OpenLocation -> {
                viewModelScope.launch {
                    refreshHandler.loadDirectory(event.panelId, event.location)
                }
            }
            is DualPaneEvent.Refresh -> {
                val panel = if (event.panelId == PanelId.LEFT) stateSnapshot.leftPanel else stateSnapshot.rightPanel
                viewModelScope.launch {
                    panel.currentLocation?.let { location ->
                        refreshHandler.loadDirectory(event.panelId, location)
                    }
                    treeNavigationAdapter.refreshAllNodes()
                }
            }
            is DualPaneEvent.NavigateUp -> {
                navigationHandler.handleNavigateUp(stateSnapshot, event.panelId)
            }
            is DualPaneEvent.ExecuteConfirmedCopy -> {
                viewModelScope.launch {
                    copyHandler.execute(stateSnapshot, event.items, event.targetPath)
                }
            }
            is DualPaneEvent.ExecuteConfirmedMove -> {
                viewModelScope.launch {
                    moveHandler.execute(stateSnapshot, event.items, event.targetPath)
                }
            }
            is DualPaneEvent.DeleteSelected -> {
                viewModelScope.launch {
                    deleteHandler.execute(stateSnapshot, event.items)
                }
            }
            is DualPaneEvent.RenameItem -> {
                viewModelScope.launch {
                    renameHandler.execute(stateSnapshot, event.item, event.newName)
                }
            }
            is DualPaneEvent.CreateDirectory -> {
                viewModelScope.launch {
                    createDirectoryHandler.execute(stateSnapshot, event.parentLocation, event.name)
                }
            }
            is DualPaneEvent.ShowFileDetails -> {
                viewModelScope.launch {
                    fileDetailHandler.loadDetails(event.item)
                }
            }
            is DualPaneEvent.CalculateChecksum -> {
                viewModelScope.launch {
                    fileDetailHandler.computeChecksum(event.item)
                }
            }
            is DualPaneEvent.UpdateSortPreferences -> {
                viewModelScope.launch {
                    appPreferencesRepository.setSortOrder(event.fileSortOrder)
                    appPreferencesRepository.setSortDirection(event.fileSortDirection)
                    treeNavigationAdapter.reSortNodes()
                }
            }
            is DualPaneEvent.ToggleShowHiddenFiles -> {
                viewModelScope.launch {
                    toggleShowHiddenFilesUseCase()
                    treeNavigationAdapter.refreshAllNodes()
                }
            }
            is DualPaneEvent.ResolveConflictDecision -> {
                val opState = stateSnapshot.operationState
                if (opState is OperationUiState.ConflictResolution) {
                    if (opState.currentConflictIndex >= opState.pendingConflicts.size) {
                        viewModelScope.launch {
                            if (opState.isMove) {
                                moveHandler.executeResolved(
                                    sources = opState.allSources,
                                    destinationDir = opState.destinationDir,
                                    decisions = opState.resolvedDecisions
                                )
                            } else {
                                copyHandler.executeResolved(
                                    sources = opState.allSources,
                                    destinationDir = opState.destinationDir,
                                    decisions = opState.resolvedDecisions
                                )
                            }
                        }
                    }
                }
            }
            is DualPaneEvent.CancelOperationRequested -> {
                backgroundOperationManager.cancelOperation()
            }
            is DualPaneEvent.ExecuteSearch -> {
                viewModelScope.launch {
                    appPreferencesRepository.addSearchHistory(event.query.keyword)
                    searchOperationHandler.executeSearch(event.query)
                }
            }
            is DualPaneEvent.ClearSearchHistory -> {
                viewModelScope.launch {
                    appPreferencesRepository.clearSearchHistory()
                }
            }
            is DualPaneEvent.SearchCancelled -> {
                searchOperationHandler.cancelSearch()
            }
            is DualPaneEvent.SearchResultsUpdated -> {
                treeNavigationAdapter.updateSearchResults(stateSnapshot.activePanelId, event.keyword, stateSnapshot.searchUiState.results + event.results)
            }
            is DualPaneEvent.SearchCompleted -> {
                // Do not dismiss dialog automatically so user can see results, but update UI
            }
            else -> {}
        }
    }
}
