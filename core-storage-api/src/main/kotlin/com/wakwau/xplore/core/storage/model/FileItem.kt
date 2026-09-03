package com.wakwau.xplore.core.storage.model

data class FileItem(
    val id: String,
    val name: String,
    val location: StorageLocation,
    val type: FileType,
    val metadata: FileMetadata
)
