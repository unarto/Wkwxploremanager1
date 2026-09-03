// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/provider/volume/InternalVolumeProvider.kt
// [Penjelasan]: Provider khusus untuk mendeteksi dan menyediakan volume Internal Storage utama beserta kapasitas ruang penyimpanannya.
package com.wakwau.xplore.core.storage.provider.volume

import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.model.StorageVolumeItem
import com.wakwau.xplore.core.storage.model.StorageVolumeType
import com.wakwau.xplore.core.storage.provider.InternalStoragePathResolver
import com.wakwau.xplore.core.storage.provider.StorageSpaceReader

class InternalVolumeProvider(
    private val internalStoragePathResolver: InternalStoragePathResolver,
    private val storageSpaceReader: StorageSpaceReader
) {
    fun getInternalVolume(): StorageVolumeItem {
        val primaryRootPath = internalStoragePathResolver.getPrimaryInternalStoragePath()
        val primarySpaceInfo = storageSpaceReader.getSpaceInfo(primaryRootPath)
        return StorageVolumeItem(
            id = StorageConstants.PRIMARY_INTERNAL_VOLUME_ID,
            name = StorageConstants.DEFAULT_PRIMARY_VOLUME_NAME,
            rootPath = primaryRootPath,
            type = StorageVolumeType.PRIMARY_INTERNAL,
            isReadOnly = false,
            spaceInfo = primarySpaceInfo
        )
    }
}
