// [Jalur Class/Modul]: filemanager/src/main/kotlin/com/wakwau/xplore/filemanager/usecase/MoveFilesUseCase.kt
// [Penjelasan]: UseCase pemindahan berkas dengan dukungan operasi langsung maupun via item hasil resolusi konflik.
package com.wakwau.xplore.filemanager.usecase

import com.wakwau.xplore.core.storage.conflict.ResolvedTransferItem
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.operation.BackgroundOperationManager
import com.wakwau.xplore.core.storage.operation.BackgroundOperationType

class MoveFilesUseCase(private val backgroundOperationManager: BackgroundOperationManager) {
    fun invoke(sources: List<StorageLocation>, destinationDir: StorageLocation) {
        backgroundOperationManager.enqueueOperation(BackgroundOperationType.MOVE, sources, destinationDir)
    }

    fun invoke(resolvedItems: List<ResolvedTransferItem>) {
        backgroundOperationManager.enqueueResolvedOperation(BackgroundOperationType.MOVE, resolvedItems)
    }
}

