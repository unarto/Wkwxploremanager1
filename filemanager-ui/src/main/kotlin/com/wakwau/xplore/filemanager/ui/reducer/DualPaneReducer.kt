// [Jalur Class]: com.wakwau.xplore.filemanager.ui.reducer.DualPaneReducer
// [Penjelasan]: Reducer murni untuk mengolah transisi status immutable panel ganda, dialog input berkas (buat folder, ganti nama, konfirmasi hapus), rincian metadata, dan pencarian.
package com.wakwau.xplore.filemanager.ui.reducer

import com.wakwau.xplore.filemanager.ui.event.DualPaneEvent
import com.wakwau.xplore.filemanager.ui.state.ChecksumState
import com.wakwau.xplore.filemanager.ui.state.DualPaneState
import com.wakwau.xplore.filemanager.ui.state.FileDetailState
import com.wakwau.xplore.filemanager.ui.state.FileDialogUiState
import com.wakwau.xplore.filemanager.ui.state.OperationUiState
import com.wakwau.xplore.filemanager.ui.state.PanelId
import com.wakwau.xplore.filemanager.ui.state.PanelState

class DualPaneReducer {

    fun reduce(state: DualPaneState, event: DualPaneEvent): DualPaneState {
        return when (event) {
            is DualPaneEvent.SetActivePanel -> {
                state.copy(activePanelId = event.panelId)
            }
            is DualPaneEvent.OpenLocation -> {
                updatePanel(state, event.panelId) {
                    it.copy(currentLocation = event.location, isLoading = true, error = null)
                }
            }
            is DualPaneEvent.LoadingStarted -> {
                updatePanel(state, event.panelId) {
                    it.copy(isLoading = true, error = null)
                }
            }
            is DualPaneEvent.DirectoryLoaded -> {
                updatePanel(state, event.panelId) {
                    it.copy(
                        currentLocation = event.location,
                        items = event.items,
                        isLoading = false,
                        error = null,
                        selectedItemIds = emptySet() // clear selection on load
                    )
                }
            }
            is DualPaneEvent.DirectoryLoadFailed -> {
                updatePanel(state, event.panelId) {
                    it.copy(isLoading = false, error = event.error)
                }
            }
            is DualPaneEvent.ToggleSelection -> {
                updatePanel(state, event.panelId) {
                    val newSelection = if (it.selectedItemIds.contains(event.itemId)) {
                        it.selectedItemIds - event.itemId
                    } else {
                        it.selectedItemIds + event.itemId
                    }
                    it.copy(selectedItemIds = newSelection)
                }
            }
            is DualPaneEvent.SetSelectedItems -> {
                updatePanel(state, event.panelId) {
                    it.copy(selectedItemIds = event.itemIds)
                }
            }
            is DualPaneEvent.ClearSelection -> {
                updatePanel(state, event.panelId) {
                    it.copy(selectedItemIds = emptySet())
                }
            }
            // Operation Results
            is DualPaneEvent.ShowOperationConfirmation -> {
                state.copy(operationState = OperationUiState.Confirming(
                    isMove = event.isMove,
                    items = event.items,
                    targetPath = event.targetPath
                ))
            }
            is DualPaneEvent.ShowConflictResolution -> {
                state.copy(operationState = OperationUiState.ConflictResolution(
                    isMove = event.isMove,
                    pendingConflicts = event.conflicts,
                    currentConflictIndex = 0,
                    destinationDir = event.destinationDir,
                    allSources = event.allSources,
                    resolvedDecisions = emptyMap()
                ))
            }
            is DualPaneEvent.ResolveConflictDecision -> {
                val currentOpState = state.operationState
                if (currentOpState is OperationUiState.ConflictResolution) {
                    val currentConflict = currentOpState.currentConflict
                    if (event.applyToAll) {
                        // Terapkan pilihan ke seluruh benturan yang tersisa
                        val updatedDecisions = currentOpState.resolvedDecisions.toMutableMap()
                        for (i in currentOpState.currentConflictIndex until currentOpState.pendingConflicts.size) {
                            val conflict = currentOpState.pendingConflicts[i]
                            updatedDecisions[conflict.source] = event.choice
                        }
                        state.copy(operationState = currentOpState.copy(
                            currentConflictIndex = currentOpState.pendingConflicts.size,
                            resolvedDecisions = updatedDecisions
                        ))
                    } else if (currentConflict != null) {
                        val updatedDecisions = currentOpState.resolvedDecisions + (currentConflict.source to event.choice)
                        state.copy(operationState = currentOpState.copy(
                            currentConflictIndex = currentOpState.currentConflictIndex + 1,
                            resolvedDecisions = updatedDecisions
                        ))
                    } else {
                        state
                    }
                } else {
                    state
                }
            }
            is DualPaneEvent.CancelOperationRequested -> {
                state.copy(operationState = OperationUiState.Cancelled)
            }
            is DualPaneEvent.OperationStarted -> {
                state.copy(operationState = OperationUiState.Running(event.operationNameRes))
            }
            is DualPaneEvent.OperationProgress -> {
                val currentOpState = state.operationState
                if (currentOpState is OperationUiState.Running) {
                    state.copy(operationState = currentOpState.copy(
                        progress = event.progress
                    ))
                } else {
                    state
                }
            }
            is DualPaneEvent.OperationSuccess -> {
                state.copy(operationState = OperationUiState.Success(event.messageRes))
            }
            is DualPaneEvent.OperationFailed -> {
                state.copy(operationState = OperationUiState.Failure(event.error))
            }
            is DualPaneEvent.OperationCancelled -> {
                state.copy(operationState = OperationUiState.Cancelled)
            }
            is DualPaneEvent.ClearOperationState -> {
                state.copy(operationState = OperationUiState.Idle)
            }

            // File Details & Checksum
            is DualPaneEvent.ShowFileDetails -> {
                state.copy(
                    fileDetailState = FileDetailState(
                        isVisible = true,
                        selectedItem = event.item,
                        isLoadingMetadata = true,
                        checksumState = ChecksumState.Idle
                    )
                )
            }
            is DualPaneEvent.DismissFileDetails -> {
                state.copy(fileDetailState = FileDetailState(isVisible = false))
            }
            is DualPaneEvent.FileDetailsLoadingStarted -> {
                state.copy(fileDetailState = state.fileDetailState.copy(isLoadingMetadata = true, errorMessage = null))
            }
            is DualPaneEvent.FileDetailsLoaded -> {
                state.copy(fileDetailState = state.fileDetailState.copy(isLoadingMetadata = false, metadata = event.metadata, errorMessage = null))
            }
            is DualPaneEvent.FileDetailsFailed -> {
                state.copy(fileDetailState = state.fileDetailState.copy(isLoadingMetadata = false, errorRes = event.errorRes, errorMessage = event.errorMessage))
            }
            is DualPaneEvent.ChecksumCalculationStarted -> {
                state.copy(fileDetailState = state.fileDetailState.copy(checksumState = ChecksumState.Calculating))
            }
            is DualPaneEvent.ChecksumCalculated -> {
                state.copy(fileDetailState = state.fileDetailState.copy(checksumState = ChecksumState.Success(event.checksum)))
            }
            is DualPaneEvent.ChecksumCalculationFailed -> {
                state.copy(fileDetailState = state.fileDetailState.copy(checksumState = ChecksumState.Error(event.errorRes, event.errorMessage)))
            }
            is DualPaneEvent.CalculateChecksum -> {
                state.copy(fileDetailState = state.fileDetailState.copy(checksumState = ChecksumState.Calculating))
            }

            // [Jalur Class/Modul]: com.wakwau.xplore.filemanager.ui.reducer.DualPaneReducer
            // [Penjelasan]: Mengolah status pencarian berkas dan menyimpan daftar hasil pencarian nyata (results) ke SearchUiState saat SearchResultsUpdated diterima.
            is DualPaneEvent.SearchIconClicked -> {
                val currentLocation = if (state.activePanelId == PanelId.LEFT) {
                    state.leftPanel.currentLocation
                } else {
                    state.rightPanel.currentLocation
                }
                state.copy(searchUiState = state.searchUiState.copy(
                    isSearchDialogOpen = true, 
                    searchError = null,
                    searchScope = currentLocation
                ))
            }
            is DualPaneEvent.DismissSearchDialog -> {
                state.copy(searchUiState = state.searchUiState.copy(
                    isSearchDialogOpen = false, 
                    searchError = null, 
                    isSearching = false, 
                    results = emptyList(), 
                    hasSearched = false,
                    searchScope = null
                ))
            }
            is DualPaneEvent.SearchStarted -> {
                state.copy(searchUiState = state.searchUiState.copy(isSearching = true, searchError = null, results = emptyList(), hasSearched = true))
            }
            is DualPaneEvent.SearchResultsUpdated -> {
                state.copy(searchUiState = state.searchUiState.copy(
                    isSearching = true, 
                    results = state.searchUiState.results + event.results, 
                    hasSearched = true, 
                    searchError = null
                ))
            }
            is DualPaneEvent.SearchCompleted -> {
                state.copy(searchUiState = state.searchUiState.copy(isSearching = false))
            }
            is DualPaneEvent.SearchFailed -> {
                state.copy(searchUiState = state.searchUiState.copy(searchError = event.error, isSearching = false, results = emptyList(), hasSearched = true))
            }
            is DualPaneEvent.SearchCancelled -> {
                state.copy(searchUiState = state.searchUiState.copy(isSearching = false))
            }

            // [Jalur Class/Modul]: com.wakwau.xplore.filemanager.ui.reducer.DualPaneReducer
            // [Penjelasan]: Mengolah status izin penyimpanan dan pembaruan storage volumes secara pure immutability.
            is DualPaneEvent.PermissionStatusUpdated -> {
                state.copy(
                    hasPermission = event.hasPermission,
                    requiredPermissionType = event.requiredPermissionType
                )
            }
            is DualPaneEvent.StorageVolumesUpdated -> {
                state.copy(
                    storageVolumes = event.volumes,
                    isVolumesLoading = false
                )
            }
            is DualPaneEvent.StorageVolumesLoading -> {
                state.copy(isVolumesLoading = event.isLoading)
            }

            // Dialog Intents
            is DualPaneEvent.ShowCreateDirectoryDialog -> {
                state.copy(dialogState = FileDialogUiState.CreateDirectory(event.parentLocation))
            }
            is DualPaneEvent.ShowRenameDialog -> {
                state.copy(dialogState = FileDialogUiState.RenameItem(event.item))
            }
            is DualPaneEvent.ShowDeleteConfirmationDialog -> {
                state.copy(dialogState = FileDialogUiState.DeleteConfirmation(event.items))
            }
            is DualPaneEvent.DismissInputDialog -> {
                state.copy(dialogState = FileDialogUiState.None)
            }
            is DualPaneEvent.CreateDirectory,
            is DualPaneEvent.RenameItem,
            is DualPaneEvent.DeleteSelected -> {
                state.copy(dialogState = FileDialogUiState.None)
            }

            // Intents that don't directly modify state synchronously without external result
            is DualPaneEvent.ExecuteSearch,
            is DualPaneEvent.NavigateUp,
            is DualPaneEvent.Refresh,
            is DualPaneEvent.ExecuteConfirmedCopy,
            is DualPaneEvent.ExecuteConfirmedMove,
            is DualPaneEvent.UpdateSortPreferences,
            is DualPaneEvent.ToggleShowHiddenFiles -> {
                state // Reducer does not perform side effects. It returns current state.
            }
        }
    }

    private fun updatePanel(
        state: DualPaneState,
        panelId: PanelId,
        updater: (PanelState) -> PanelState
    ): DualPaneState {
        return if (panelId == PanelId.LEFT) {
            state.copy(leftPanel = updater(state.leftPanel))
        } else {
            state.copy(rightPanel = updater(state.rightPanel))
        }
    }
}
