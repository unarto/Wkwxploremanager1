// [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/action/DeleteOperationHandler.kt
// [Penjelasan]: Handler operasi penghapusan berkas/direktori terpilih dengan pemisahan eksplisit antara CancellationException (OperationCancelled) dan kegagalan I/O riil (OperationFailed) menggunakan StorageErrorMapper.
package com.wakwau.xplore.filemanager.ui.action

import com.wakwau.xplore.core.storage.error.StorageErrorMapper
import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.filemanager.ui.constant.FileOperationConstants
import com.wakwau.xplore.filemanager.ui.event.DualPaneEvent
import com.wakwau.xplore.filemanager.ui.state.DualPaneState
import com.wakwau.xplore.filemanager.usecase.DeleteFilesUseCase
import kotlinx.coroutines.CancellationException

class DeleteOperationHandler(
    private val deleteFilesUseCase: DeleteFilesUseCase,
    private val storageErrorMapper: StorageErrorMapper = StorageErrorMapper(),
    private val dispatch: (DualPaneEvent) -> Unit
) {
    constructor(
        deleteFilesUseCase: DeleteFilesUseCase,
        dispatch: (DualPaneEvent) -> Unit
    ) : this(deleteFilesUseCase, StorageErrorMapper(), dispatch)

    suspend fun execute(state: DualPaneState, sourceItems: List<FileItem>) {
        val sourcePanel = state.activePanel
        if (sourceItems.isEmpty()) return
        
        dispatch(DualPaneEvent.OperationStarted(FileOperationConstants.OPERATION_DELETE))
        try {
            val locations = sourceItems.map { it.location }
            deleteFilesUseCase.invoke(locations)
            
            // Success is handled by DualPaneViewModel observing BackgroundOperationManager
            dispatch(DualPaneEvent.ClearSelection(sourcePanel.id))
        } catch (e: CancellationException) {
            dispatch(DualPaneEvent.OperationCancelled)
            throw e
        } catch (e: Exception) {
            val domainError = storageErrorMapper.map(e)
            dispatch(DualPaneEvent.OperationFailed(domainError.name))
        }
    }
}
