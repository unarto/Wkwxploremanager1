// [Jalur Class]: com.wakwau.xplore.core.storage.preferences.AppPreferencesRepository
// [Penjelasan]: Antarmuka kontrak repository untuk membaca dan menyimpan preferensi aplikasi via MMKV.
package com.wakwau.xplore.core.storage.preferences

import kotlinx.coroutines.flow.StateFlow
import com.wakwau.xplore.core.storage.preferences.AppLanguage
import com.wakwau.xplore.core.storage.preferences.AppThemeMode
import com.wakwau.xplore.core.storage.preferences.SettingsState

interface AppPreferencesRepository {
    val preferencesState: StateFlow<FilePreferencesState>
    fun getPreferencesState(): FilePreferencesState
    
    val settingsState: StateFlow<SettingsState>
    val searchHistoryState: StateFlow<List<String>>
    
    suspend fun setSortOrder(sortOrder: FileSortOrder)
    suspend fun setSortDirection(sortDirection: FileSortDirection)
    suspend fun setLayoutMode(layoutMode: FileLayoutMode)
    suspend fun setShowHiddenFiles(showHiddenFiles: Boolean)
    suspend fun setLastVisitedPath(lastVisitedPath: String)
    
    suspend fun setThemeMode(mode: AppThemeMode)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setFileSystemAccessMode(mode: FileSystemAccessMode)
    suspend fun setRootReadOnly(isReadOnly: Boolean)
    suspend fun addSearchHistory(keyword: String)
    suspend fun clearSearchHistory()
}
