package com.wakwau.xplore.core.storage.search

import com.wakwau.xplore.core.storage.model.StorageLocation

data class FileSearchQuery(
    val location: StorageLocation,
    val keyword: String = "",
    val searchType: SearchTargetType = SearchTargetType.ALL,
    val searchInArchives: Boolean = false,
    val showHidden: Boolean = false,
    val extension: String? = null,
    val minSize: Long? = null,
    val maxSize: Long? = null
)

enum class SearchTargetType {
    ALL, FILE, FOLDER
}
