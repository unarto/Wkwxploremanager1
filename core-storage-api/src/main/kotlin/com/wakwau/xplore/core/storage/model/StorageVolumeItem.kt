package com.wakwau.xplore.core.storage.model

data class StorageVolumeItem(
    val id: String,
    val name: String,
    val rootPath: String,
    val type: StorageVolumeType,
    val isReadOnly: Boolean,
    val spaceInfo: StorageSpaceInfo?,
    val createdAt: Long = 0L
)
