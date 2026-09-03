package com.wakwau.xplore.filemanager.usecase

import com.wakwau.xplore.core.storage.model.StorageVolumeItem
import com.wakwau.xplore.core.storage.repository.StorageVolumeRepository
import kotlinx.coroutines.flow.Flow

class GetStorageVolumesUseCase(private val storageVolumeRepository: StorageVolumeRepository) {
    operator fun invoke(): Flow<List<StorageVolumeItem>> {
        return storageVolumeRepository.getVolumes()
    }
    
    suspend fun refresh() {
        storageVolumeRepository.refreshVolumes()
    }
}
