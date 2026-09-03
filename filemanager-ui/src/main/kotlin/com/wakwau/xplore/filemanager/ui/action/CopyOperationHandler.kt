// [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/action/CopyOperationHandler.kt
// [Penjelasan]: Handler operasi penyalinan berkas/direktori dengan integrasi deteksi benturan (ConflictDetector) dan eksekusi transfer ter-resolve (ConflictResolver).
package com.wakwau.xplore.filemanager.ui.action

import com.wakwau.xplore.core.storage.conflict.ConflictChoice
import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.error.StorageErrorMapper
import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.filemanager.ui.constant.FileOperationConstants
import com.wakwau.xplore.filemanager.ui.event.DualPaneEvent
import com.wakwau.xplore.filemanager.ui.state.DualPaneState
import com.wakwau.xplore.filemanager.usecase.CopyFilesUseCase
import com.wakwau.xplore.filemanager.usecase.DetectConflictsUseCase
import com.wakwau.xplore.filemanager.usecase.ResolveTransferUseCase
import kotlinx.coroutines.CancellationException

class CopyOperationHandler(
    private val copyFilesUseCase: CopyFilesUseCase,
    private val detectConflictsUseCase: DetectConflictsUseCase,
    private val resolveTransferUseCase: ResolveTransferUseCase,
    private val storageErrorMapper: StorageErrorMapper = StorageErrorMapper(),
    private val dispatch: (DualPaneEvent) -> Unit
) {
    suspend fun execute(state: DualPaneState, itemsToCopy: List<FileItem>, targetPath: String) {
        val sourcePanel = state.activePanel
        val destPanel = state.inactivePanel
        
        val destLocation = StorageLocation(path = targetPath, rootId = destPanel.currentLocation?.rootId ?: StorageConstants.UNKNOWN_ROOT_ID)
        
        if (itemsToCopy.isEmpty()) return
        
        try {
            val sources = itemsToCopy.map { it.location }
            val conflicts = detectConflictsUseCase.invoke(sources, destLocation)
            
            if (conflicts.isNotEmpty()) {
                // Tampilkan dialog resolusi benturan ke pengguna
                dispatch(
                    DualPaneEvent.ShowConflictResolution(
                        isMove = false,
                        conflicts = conflicts,
                        destinationDir = destLocation,
                        allSources = sources
                    )
                )
            } else {
                dispatch(DualPaneEvent.OperationStarted(FileOperationConstants.OPERATION_COPY))
                copyFilesUseCase.invoke(sources, destLocation)
                dispatch(DualPaneEvent.ClearSelection(sourcePanel.id))
            }
        } catch (e: CancellationException) {
            dispatch(DualPaneEvent.OperationCancelled)
            throw e
        } catch (e: Exception) {
            val domainError = storageErrorMapper.map(e)
            dispatch(DualPaneEvent.OperationFailed(domainError.name))
        }
    }

    suspend fun executeResolved(
        sources: List<StorageLocation>,
        destinationDir: StorageLocation,
        decisions: Map<StorageLocation, ConflictChoice>
    ) {
        dispatch(DualPaneEvent.OperationStarted(FileOperationConstants.OPERATION_COPY))
        try {
            val resolvedItems = resolveTransferUseCase.invoke(
                sources = sources,
                destinationDir = destinationDir,
                conflictDecisions = decisions
            )
            copyFilesUseCase.invoke(resolvedItems)
        } catch (e: CancellationException) {
            dispatch(DualPaneEvent.OperationCancelled)
            throw e
        } catch (e: Exception) {
            val domainError = storageErrorMapper.map(e)
            dispatch(DualPaneEvent.OperationFailed(domainError.name))
        }
    }
}

