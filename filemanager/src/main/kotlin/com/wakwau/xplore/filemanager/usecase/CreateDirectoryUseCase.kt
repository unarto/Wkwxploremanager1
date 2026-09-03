package com.wakwau.xplore.filemanager.usecase

import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.operation.FileOperationResult
import com.wakwau.xplore.core.storage.repository.DirectoryRepository

class CreateDirectoryUseCase(private val directoryRepository: DirectoryRepository) {
    suspend operator fun invoke(location: StorageLocation, name: String): FileOperationResult<FileItem> {
        return directoryRepository.create(location, name)
    }
}
