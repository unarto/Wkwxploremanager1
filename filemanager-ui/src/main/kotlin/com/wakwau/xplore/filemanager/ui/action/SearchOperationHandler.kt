// [Jalur Class]: com.wakwau.xplore.filemanager.ui.action.SearchOperationHandler
// [Penjelasan]: Handler operasi pencarian berkas asinkron dengan penanganan error menggunakan StorageConstants.DEFAULT_UNKNOWN_ERROR.
package com.wakwau.xplore.filemanager.ui.action

import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.search.FileSearchQuery
import com.wakwau.xplore.filemanager.ui.event.DualPaneEvent
import com.wakwau.xplore.filemanager.usecase.SearchFilesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.CancellationException

class SearchOperationHandler(
    private val searchFilesUseCase: SearchFilesUseCase,
    private val dispatch: (DualPaneEvent) -> Unit
) {
    private var searchJob: Job? = null

    suspend fun executeSearch(query: FileSearchQuery) = coroutineScope {
        searchJob?.cancel()
        dispatch(DualPaneEvent.SearchStarted(query.keyword, query.searchType, query.searchInArchives))
        
        searchJob = searchFilesUseCase(query)
            .onEach { results ->
                dispatch(DualPaneEvent.SearchResultsUpdated(query.keyword, results))
            }
            .onCompletion { cause ->
                if (cause == null || cause is CancellationException) {
                    dispatch(DualPaneEvent.SearchCompleted)
                }
            }
            .catch { e ->
                dispatch(DualPaneEvent.SearchFailed(e.message ?: StorageConstants.DEFAULT_UNKNOWN_ERROR))
            }
            .launchIn(this)
    }

    fun cancelSearch() {
        searchJob?.cancel()
        searchJob = null
        dispatch(DualPaneEvent.SearchCancelled)
    }
}
