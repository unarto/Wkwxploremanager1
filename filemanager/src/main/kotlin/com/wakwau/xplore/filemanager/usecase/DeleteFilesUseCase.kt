package com.wakwau.xplore.filemanager.usecase

import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.operation.BackgroundOperationManager
import com.wakwau.xplore.core.storage.operation.BackgroundOperationType

class DeleteFilesUseCase(
    private val backgroundOperationManager: BackgroundOperationManager
) {
    fun invoke(locations: List<StorageLocation>) {
        backgroundOperationManager.enqueueOperation(BackgroundOperationType.DELETE, locations)
    }
}
