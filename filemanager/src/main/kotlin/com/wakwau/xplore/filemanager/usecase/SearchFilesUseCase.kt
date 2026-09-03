// [Jalur Class]: com.wakwau.xplore.filemanager.usecase.SearchFilesUseCase
// [Penjelasan]: Use case untuk memicu pencarian berkas secara asinkron menggunakan FileSearchService.
package com.wakwau.xplore.filemanager.usecase

import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.search.FileSearchQuery
import com.wakwau.xplore.core.storage.search.FileSearchService
import kotlinx.coroutines.flow.Flow

class SearchFilesUseCase(
    private val fileSearchService: FileSearchService
) {
    operator fun invoke(query: FileSearchQuery): Flow<List<FileItem>> {
        return fileSearchService.search(query)
    }
}
