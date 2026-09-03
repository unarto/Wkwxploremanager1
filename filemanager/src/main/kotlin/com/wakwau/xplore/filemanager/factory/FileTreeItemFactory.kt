// [Jalur Class/Modul]: filemanager/src/main/kotlin/com/wakwau/xplore/filemanager/factory/FileTreeItemFactory.kt
// [Penjelasan]: Factory domain untuk menciptakan entitas FileItem, root volume, dan virtual search node dengan format nama hasil pencarian berbasis StorageConstants.
package com.wakwau.xplore.filemanager.factory

import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.FileMetadata
import com.wakwau.xplore.core.storage.model.FileType
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.model.StorageVolumeItem

class FileTreeItemFactory {

    fun createVolumeRoot(volume: StorageVolumeItem): FileItem {
        return FileItem(
            id = volume.rootPath,
            name = volume.name,
            location = StorageLocation(path = volume.rootPath, rootId = volume.id),
            type = FileType.DIRECTORY,
            metadata = FileMetadata(
                size = volume.spaceInfo?.totalBytes ?: 0L,
                modifiedTime = 0L,
                createdTime = 0L,
                isHidden = false,
                isReadable = true,
                isWritable = true,
                isExecutable = true
            )
        )
    }

    fun createSearchResultsRoot(keyword: String): FileItem {
        val displayName = if (keyword.isBlank()) {
            StorageConstants.DEFAULT_SEARCH_RESULTS_NAME
        } else {
            "${StorageConstants.SEARCH_RESULTS_PREFIX}'$keyword'"
        }
        return FileItem(
            id = StorageConstants.VIRTUAL_SEARCH_ROOT_ID,
            name = displayName,
            location = StorageLocation(
                path = StorageConstants.VIRTUAL_SEARCH_ROOT_ID,
                rootId = StorageConstants.VIRTUAL_SEARCH_LOCATION_ID
            ),
            type = FileType.DIRECTORY,
            metadata = FileMetadata.EMPTY
        )
    }
}
