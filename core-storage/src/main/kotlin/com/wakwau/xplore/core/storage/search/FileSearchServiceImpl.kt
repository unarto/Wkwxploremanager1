// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/search/FileSearchServiceImpl.kt
// [Penjelasan]: Implementasi FileSearchService terkoordinasi yang mendelegasikan traversal ke FileSystemSearchTraversal dan sinkronisasi indeks ke FileIndexSynchronizer tanpa unconfined background scope.
package com.wakwau.xplore.core.storage.search

import com.wakwau.xplore.core.storage.model.FileItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

class FileSearchServiceImpl(
    private val traversal: FileSystemSearchTraversal,
    private val indexSynchronizer: FileIndexSynchronizer,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FileSearchService {

    override fun search(query: FileSearchQuery): Flow<List<FileItem>> {
        return traversal.traverse(query) { items ->
            indexSynchronizer.syncBatch(items)
        }.flowOn(defaultDispatcher)
    }

    override suspend fun removeIndexByPrefix(locationPrefix: String) {
        indexSynchronizer.removeByPrefix(locationPrefix)
    }

    override suspend fun addOrUpdateIndexBatch(items: List<FileItem>) {
        indexSynchronizer.syncBatch(items)
    }
}
