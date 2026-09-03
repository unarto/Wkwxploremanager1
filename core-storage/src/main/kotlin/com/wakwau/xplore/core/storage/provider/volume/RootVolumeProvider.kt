// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/provider/volume/RootVolumeProvider.kt
// [Penjelasan]: Provider khusus untuk mendeteksi ketersediaan akses Root melalui Shizuku IPC binder dan menyediakan item root volume.
package com.wakwau.xplore.core.storage.provider.volume

import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.model.StorageVolumeItem
import com.wakwau.xplore.core.storage.model.StorageVolumeType
import com.wakwau.xplore.core.storage.provider.StorageSpaceReader

class RootVolumeProvider(
    private val storageSpaceReader: StorageSpaceReader
) {
    fun getRootVolume(): StorageVolumeItem {
        val rootPath = StorageConstants.ROOT_PATH
        val rootSpaceInfo = storageSpaceReader.getSpaceInfo(rootPath)
        return StorageVolumeItem(
            id = StorageConstants.ROOT_STORAGE_ID,
            name = StorageConstants.ROOT_STORAGE_NAME,
            rootPath = rootPath,
            type = StorageVolumeType.ROOT,
            isReadOnly = false,
            spaceInfo = rootSpaceInfo
        )
    }
}
