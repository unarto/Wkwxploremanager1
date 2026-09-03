package com.wakwau.xplore.filemanager.usecase

import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.operation.FileOperationResult
import com.wakwau.xplore.core.storage.preferences.AppPreferencesRepository
import com.wakwau.xplore.core.storage.repository.DirectoryRepository

class ListDirectoryUseCase(
    private val directoryRepository: DirectoryRepository,
    private val appPreferencesRepository: AppPreferencesRepository
) {
    suspend operator fun invoke(location: StorageLocation): FileOperationResult<List<FileItem>> {
        val showHidden = appPreferencesRepository.getPreferencesState().showHiddenFiles
        return directoryRepository.list(location, showHidden)
    }
}
