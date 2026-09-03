// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/repository/StorageVolumeRepositoryImpl.kt
// [Penjelasan]: Repository orchestrator tipis untuk volume penyimpanan. Mendelegasikan deteksi ke provider-provider granular (Internal, External, Root, SAF) dan merespon event media via BroadcastReceiver terisolasi.
package com.wakwau.xplore.core.storage.repository

import android.content.Context
import com.wakwau.xplore.core.storage.model.StorageVolumeItem
import com.wakwau.xplore.core.storage.model.StorageVolumeType
import com.wakwau.xplore.core.storage.provider.volume.ExternalVolumeProvider
import com.wakwau.xplore.core.storage.provider.volume.InternalVolumeProvider
import com.wakwau.xplore.core.storage.provider.volume.RootVolumeProvider
import com.wakwau.xplore.core.storage.provider.volume.SafVolumeProvider
import com.wakwau.xplore.core.storage.provider.volume.StorageVolumeBroadcastReceiver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StorageVolumeRepositoryImpl(
    private val context: Context,
    private val internalVolumeProvider: InternalVolumeProvider,
    private val externalVolumeProvider: ExternalVolumeProvider,
    private val rootVolumeProvider: RootVolumeProvider,
    private val safVolumeProvider: SafVolumeProvider,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : StorageVolumeRepository {

    private val _volumes = MutableStateFlow<List<StorageVolumeItem>>(emptyList())
    private val scope = CoroutineScope(ioDispatcher)
    private val broadcastReceiver = StorageVolumeBroadcastReceiver {
        scope.launch {
            refreshVolumes()
        }
    }

    init {
        broadcastReceiver.register(context)
        // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/repository/StorageVolumeRepositoryImpl.kt
        // [Penjelasan]: Memuat volume penyimpanan awal saat repository diinisialisasi agar Flow tidak kosong saat awal pengamatan.
        scope.launch {
            refreshVolumes()
        }
    }

    override fun getVolumes(): Flow<List<StorageVolumeItem>> {
        return _volumes.asStateFlow()
    }

    override suspend fun refreshVolumes() {
        val newVolumes = mutableListOf<StorageVolumeItem>()

        // 1. Internal Storage
        val internalVolume = internalVolumeProvider.getInternalVolume()
        newVolumes.add(internalVolume)

        // 2 & 3. Deteksi SD Card & USB OTG
        val externalVolumes = externalVolumeProvider.getExternalVolumes(internalVolume.rootPath)
        newVolumes.addAll(externalVolumes)

        // 4. Root Storage
        val rootVolume = rootVolumeProvider.getRootVolume()
        newVolumes.add(rootVolume)

        // 5. SAF Persisted URIs
        val safVolumes = safVolumeProvider.getSafVolumes()
        newVolumes.addAll(safVolumes)

        // Sort volumes based on priority order and chronological timestamp
        newVolumes.sortWith(Comparator { a, b ->
            val orderA = getOrderForType(a.type)
            val orderB = getOrderForType(b.type)
            if (orderA != orderB) {
                orderA.compareTo(orderB)
            } else {
                a.createdAt.compareTo(b.createdAt)
            }
        })

        _volumes.value = newVolumes
    }

    private fun getOrderForType(type: StorageVolumeType): Int {
        return when (type) {
            StorageVolumeType.PRIMARY_INTERNAL -> 1
            StorageVolumeType.SECONDARY_SDCARD -> 2
            StorageVolumeType.USB_OTG -> 3
            StorageVolumeType.ROOT -> 4
            StorageVolumeType.SAF_PROVIDER -> 5
            StorageVolumeType.UNKNOWN -> 6
        }
    }
}
