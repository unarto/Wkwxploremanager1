// [Jalur Class/Modul]: filemanager/src/main/kotlin/com/wakwau/xplore/filemanager/usecase/GetParentLocationUseCase.kt
// [Penjelasan]: Use case domain untuk menentukan direktori induk (parent) dari StorageLocation dengan validasi batas root volume storage tanpa manipulasi path java.io langsung di presentation layer.
package com.wakwau.xplore.filemanager.usecase

import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.model.StorageLocation

class GetParentLocationUseCase {
    operator fun invoke(currentLocation: StorageLocation): StorageLocation? {
        val path = currentLocation.path
        if (path.isEmpty() || path == StorageConstants.ROOT_PATH || path == StorageConstants.DEFAULT_PRIMARY_STORAGE_PATH) {
            return null
        }
        val trimmed = path.trimEnd('/')
        val lastSlashIndex = trimmed.lastIndexOf('/')
        if (lastSlashIndex < 0) {
            return null
        }
        val parentPath = if (lastSlashIndex == 0) {
            StorageConstants.ROOT_PATH
        } else {
            trimmed.substring(0, lastSlashIndex)
        }
        return StorageLocation(parentPath, currentLocation.rootId)
    }
}
