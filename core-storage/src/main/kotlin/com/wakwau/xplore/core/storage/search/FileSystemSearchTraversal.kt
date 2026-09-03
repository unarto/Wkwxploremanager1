// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/search/FileSystemSearchTraversal.kt
// [Penjelasan]: Traversal BFS direktori filesystem secara asinkron untuk pencarian berkas mendalam menggunakan Flow dan batch callback berkala guna sinkronisasi indeks Room yang hemat memori.
package com.wakwau.xplore.core.storage.search

import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.FileType
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.operation.FileOperationResult
import com.wakwau.xplore.core.storage.repository.DirectoryRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

class FileSystemSearchTraversal(
    private val directoryRepository: DirectoryRepository,
    private val filterMatcher: SearchQueryFilterMatcher = SearchQueryFilterMatcher()
) {
    companion object {
        private const val EMIT_BATCH_SIZE = 10
        private const val INDEX_SYNC_BATCH_SIZE = 50
    }

    fun traverse(
        query: FileSearchQuery,
        onBatchFound: (suspend (List<FileItem>) -> Unit)? = null
    ): Flow<List<FileItem>> = flow {
        val queue = ArrayDeque<StorageLocation>()
        queue.add(query.location)
        val emitBatch = mutableListOf<FileItem>()
        val indexSyncBatch = mutableListOf<FileItem>()

        while (queue.isNotEmpty() && currentCoroutineContext().isActive) {
            val currentLocation = queue.removeFirst()
            val listResult = directoryRepository.list(currentLocation, query.showHidden)

            if (listResult is FileOperationResult.Success) {
                for (item in listResult.data) {
                    if (!currentCoroutineContext().isActive) break

                    if (item.type == FileType.DIRECTORY) {
                        queue.addLast(item.location)
                    }

                    if (filterMatcher.matches(item, query)) {
                        emitBatch.add(item)
                        indexSyncBatch.add(item)

                        if (emitBatch.size >= EMIT_BATCH_SIZE) {
                            emit(emitBatch.toList())
                            emitBatch.clear()
                        }

                        if (indexSyncBatch.size >= INDEX_SYNC_BATCH_SIZE) {
                            onBatchFound?.invoke(indexSyncBatch.toList())
                            indexSyncBatch.clear()
                        }
                    }
                }
            }
        }

        if (emitBatch.isNotEmpty()) {
            emit(emitBatch.toList())
        }

        if (indexSyncBatch.isNotEmpty()) {
            onBatchFound?.invoke(indexSyncBatch.toList())
            indexSyncBatch.clear()
        }
    }
}
