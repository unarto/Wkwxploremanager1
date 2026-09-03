package com.wakwau.xplore.core.storage.repository

import com.wakwau.xplore.core.storage.model.StorageVolumeItem
import kotlinx.coroutines.flow.Flow

interface StorageVolumeRepository {
    fun getVolumes(): Flow<List<StorageVolumeItem>>
    suspend fun refreshVolumes()
}
