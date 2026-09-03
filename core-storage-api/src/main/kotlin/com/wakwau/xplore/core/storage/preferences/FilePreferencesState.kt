// [Jalur Class]: com.wakwau.xplore.core.storage.preferences.FilePreferencesState
// [Penjelasan]: Data class immutable yang merepresentasikan state preferensi aplikasi file manager.
package com.wakwau.xplore.core.storage.preferences

import com.wakwau.xplore.core.storage.constant.StorageConstants

data class FilePreferencesState(
    val sortOrder: FileSortOrder = FileSortOrder.NAME,
    val sortDirection: FileSortDirection = FileSortDirection.ASCENDING,
    val layoutMode: FileLayoutMode = FileLayoutMode.LIST,
    val showHiddenFiles: Boolean = false,
    val lastVisitedPath: String = DEFAULT_PATH
) {
    companion object {
        val DEFAULT_PATH: String = StorageConstants.DEFAULT_PRIMARY_STORAGE_PATH
    }
}
