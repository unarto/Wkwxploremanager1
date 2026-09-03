// [Jalur Class/Modul]: filemanager/src/main/kotlin/com/wakwau/xplore/filemanager/usecase/CopyFilesUseCase.kt
// [Penjelasan]: UseCase penyalinan berkas dengan dukungan operasi langsung maupun via item hasil resolusi konflik.
package com.wakwau.xplore.filemanager.usecase

import com.wakwau.xplore.core.storage.conflict.ResolvedTransferItem
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.operation.BackgroundOperationManager
import com.wakwau.xplore.core.storage.operation.BackgroundOperationType

class CopyFilesUseCase(private val backgroundOperationManager: BackgroundOperationManager) {
    fun invoke(sources: List<StorageLocation>, destinationDir: StorageLocation) {
        backgroundOperationManager.enqueueOperation(BackgroundOperationType.COPY, sources, destinationDir)
    }

    fun invoke(resolvedItems: List<ResolvedTransferItem>) {
        backgroundOperationManager.enqueueResolvedOperation(BackgroundOperationType.COPY, resolvedItems)
    }
}

