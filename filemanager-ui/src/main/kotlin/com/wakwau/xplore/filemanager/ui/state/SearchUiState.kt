// [Jalur Class/Modul]: com.wakwau.xplore.filemanager.ui.state.SearchUiState
// [Penjelasan]: State UI pencarian berkas yang menampung visibilitas dialog, progres pencarian, pesan galat, riwayat pencarian (hasSearched), dan daftar hasil pencarian (results: List<FileItem>).
package com.wakwau.xplore.filemanager.ui.state

import com.wakwau.xplore.core.storage.model.FileItem

data class SearchUiState(
    val isSearchDialogOpen: Boolean = false,
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val results: List<FileItem> = emptyList(),
    val hasSearched: Boolean = false,
    val searchScope: com.wakwau.xplore.core.storage.model.StorageLocation? = null,
    val searchHistory: List<String> = emptyList()
)

