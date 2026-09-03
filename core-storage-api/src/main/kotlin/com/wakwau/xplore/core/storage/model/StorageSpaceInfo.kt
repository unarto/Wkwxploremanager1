package com.wakwau.xplore.core.storage.model

data class StorageSpaceInfo(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val percentageUsed: Int
)
