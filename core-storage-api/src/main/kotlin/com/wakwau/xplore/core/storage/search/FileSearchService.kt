package com.wakwau.xplore.core.storage.search

import com.wakwau.xplore.core.storage.model.FileItem
import kotlinx.coroutines.flow.Flow

interface FileSearchService {
    fun search(query: FileSearchQuery): Flow<List<FileItem>>
    suspend fun removeIndexByPrefix(locationPrefix: String)
    suspend fun addOrUpdateIndexBatch(items: List<FileItem>)
}
