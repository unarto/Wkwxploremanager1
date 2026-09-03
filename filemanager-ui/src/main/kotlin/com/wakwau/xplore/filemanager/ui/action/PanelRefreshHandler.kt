package com.wakwau.xplore.filemanager.ui.action

import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.operation.FileOperationResult
import com.wakwau.xplore.filemanager.ui.event.DualPaneEvent
import com.wakwau.xplore.filemanager.ui.state.PanelId
import com.wakwau.xplore.filemanager.usecase.ListDirectoryUseCase
import kotlinx.coroutines.CancellationException

class PanelRefreshHandler(
    private val listDirectoryUseCase: ListDirectoryUseCase,
    private val dispatch: (DualPaneEvent) -> Unit
) {
    suspend fun loadDirectory(panelId: PanelId, location: StorageLocation) {
        dispatch(DualPaneEvent.LoadingStarted(panelId))
        try {
            when (val result = listDirectoryUseCase(location)) {
                is FileOperationResult.Success -> {
                    dispatch(DualPaneEvent.DirectoryLoaded(panelId, location, result.data))
                }
                is FileOperationResult.Failure -> {
                    dispatch(DualPaneEvent.DirectoryLoadFailed(panelId, result.error.name))
                }
                is FileOperationResult.Cancelled -> {
                    // Ignored intentionally for pure cancellation semantics
                }
                is FileOperationResult.Completed -> {
                    // No-op
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            dispatch(DualPaneEvent.DirectoryLoadFailed(panelId, e.message ?: "Unknown Error"))
        }
    }
}
