package com.wakwau.xplore.core.storage.mapper

import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.FileMetadata
import com.wakwau.xplore.core.storage.model.FileType
import com.wakwau.xplore.core.storage.model.StorageLocation

class FileItemMapper {
    fun map(
        id: String,
        name: String,
        location: StorageLocation,
        type: FileType,
        metadata: FileMetadata
    ): FileItem {
        return FileItem(
            id = id,
            name = name,
            location = location,
            type = type,
            metadata = metadata
        )
    }
}
