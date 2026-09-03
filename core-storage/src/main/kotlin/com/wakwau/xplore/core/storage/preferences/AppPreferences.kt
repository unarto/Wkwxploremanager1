// [Jalur Class]: com.wakwau.xplore.core.storage.preferences.AppPreferences
// [Penjelasan]: Helper/Class pengelolaan MMKV untuk menyimpan dan membaca preferensi sort order, sort direction, layout mode, flag hidden files, dan last visited path.
package com.wakwau.xplore.core.storage.preferences

import android.content.Context
import com.tencent.mmkv.MMKV
import com.wakwau.xplore.core.storage.constant.StorageConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.wakwau.xplore.core.storage.preferences.AppLanguage
import com.wakwau.xplore.core.storage.preferences.AppThemeMode
import com.wakwau.xplore.core.storage.preferences.SettingsState
import com.wakwau.xplore.core.storage.preferences.FileSystemAccessMode

class AppPreferences private constructor(
    private val mmkv: MMKV
) : AppPreferencesRepository {

    companion object {
        fun create(context: Context): AppPreferences {
            MMKV.initialize(context)
            return AppPreferences(MMKV.defaultMMKV())
        }
    }

    private val _preferencesState = MutableStateFlow(loadInitialPreferences())
    override val preferencesState: StateFlow<FilePreferencesState> = _preferencesState.asStateFlow()

    private val _settingsState = MutableStateFlow(loadSettings())
    override val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()
    
    private val _searchHistoryState = MutableStateFlow(loadSearchHistory())
    override val searchHistoryState: StateFlow<List<String>> = _searchHistoryState.asStateFlow()

    override fun getPreferencesState(): FilePreferencesState {
        return _preferencesState.value
    }

    private fun loadSearchHistory(): List<String> {
        val json = mmkv.decodeString(StorageConstants.Preferences.KEY_SEARCH_HISTORY, "[]") ?: "[]"
        return try {
            val jsonArray = org.json.JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun loadSettings(): SettingsState {
        val themeString = mmkv.decodeString(StorageConstants.Preferences.KEY_THEME_MODE, AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name
        val themeMode = runCatching { AppThemeMode.valueOf(themeString) }.getOrDefault(AppThemeMode.DARK)

        val langString = mmkv.decodeString(StorageConstants.Preferences.KEY_LANGUAGE, AppLanguage.SYSTEM.name) ?: AppLanguage.SYSTEM.name
        val language = runCatching { AppLanguage.valueOf(langString) }.getOrDefault(AppLanguage.SYSTEM)

        val accessModeString = mmkv.decodeString(StorageConstants.Preferences.KEY_FILE_SYSTEM_ACCESS_MODE, FileSystemAccessMode.NORMAL.name) ?: FileSystemAccessMode.NORMAL.name
        val fileSystemAccessMode = runCatching { FileSystemAccessMode.valueOf(accessModeString) }.getOrDefault(FileSystemAccessMode.NORMAL)

        val isRootReadOnly = mmkv.decodeBool(StorageConstants.Preferences.KEY_ROOT_READ_ONLY, true)

        return SettingsState(
            themeMode = themeMode,
            language = language,
            fileSystemAccessMode = fileSystemAccessMode,
            isRootReadOnly = isRootReadOnly
        )
    }

    private fun loadInitialPreferences(): FilePreferencesState {
        val sortOrderName = mmkv.decodeString(StorageConstants.Preferences.KEY_SORT_ORDER, FileSortOrder.NAME.name) ?: FileSortOrder.NAME.name
        val sortOrder = runCatching { FileSortOrder.valueOf(sortOrderName) }.getOrDefault(FileSortOrder.NAME)

        val sortDirName = mmkv.decodeString(StorageConstants.Preferences.KEY_SORT_DIRECTION, FileSortDirection.ASCENDING.name) ?: FileSortDirection.ASCENDING.name
        val sortDirection = runCatching { FileSortDirection.valueOf(sortDirName) }.getOrDefault(FileSortDirection.ASCENDING)

        val layoutModeName = mmkv.decodeString(StorageConstants.Preferences.KEY_LAYOUT_MODE, FileLayoutMode.LIST.name) ?: FileLayoutMode.LIST.name
        val layoutMode = runCatching { FileLayoutMode.valueOf(layoutModeName) }.getOrDefault(FileLayoutMode.LIST)

        val showHiddenFiles = mmkv.decodeBool(StorageConstants.Preferences.KEY_SHOW_HIDDEN_FILES, false)
        val lastVisitedPath = mmkv.decodeString(StorageConstants.Preferences.KEY_LAST_VISITED_PATH, FilePreferencesState.DEFAULT_PATH) ?: FilePreferencesState.DEFAULT_PATH

        return FilePreferencesState(
            sortOrder = sortOrder,
            sortDirection = sortDirection,
            layoutMode = layoutMode,
            showHiddenFiles = showHiddenFiles,
            lastVisitedPath = lastVisitedPath
        )
    }

    override suspend fun setSortOrder(sortOrder: FileSortOrder) {
        mmkv.encode(StorageConstants.Preferences.KEY_SORT_ORDER, sortOrder.name)
        _preferencesState.value = _preferencesState.value.copy(sortOrder = sortOrder)
    }

    override suspend fun setSortDirection(sortDirection: FileSortDirection) {
        mmkv.encode(StorageConstants.Preferences.KEY_SORT_DIRECTION, sortDirection.name)
        _preferencesState.value = _preferencesState.value.copy(sortDirection = sortDirection)
    }

    override suspend fun setLayoutMode(layoutMode: FileLayoutMode) {
        mmkv.encode(StorageConstants.Preferences.KEY_LAYOUT_MODE, layoutMode.name)
        _preferencesState.value = _preferencesState.value.copy(layoutMode = layoutMode)
    }

    override suspend fun setShowHiddenFiles(showHiddenFiles: Boolean) {
        mmkv.encode(StorageConstants.Preferences.KEY_SHOW_HIDDEN_FILES, showHiddenFiles)
        _preferencesState.value = _preferencesState.value.copy(showHiddenFiles = showHiddenFiles)
    }

    override suspend fun setLastVisitedPath(lastVisitedPath: String) {
        mmkv.encode(StorageConstants.Preferences.KEY_LAST_VISITED_PATH, lastVisitedPath)
        _preferencesState.value = _preferencesState.value.copy(lastVisitedPath = lastVisitedPath)
    }

    override suspend fun setThemeMode(mode: AppThemeMode) {
        mmkv.encode(StorageConstants.Preferences.KEY_THEME_MODE, mode.name)
        _settingsState.value = _settingsState.value.copy(themeMode = mode)
    }

    override suspend fun setLanguage(language: AppLanguage) {
        mmkv.encode(StorageConstants.Preferences.KEY_LANGUAGE, language.name)
        _settingsState.value = _settingsState.value.copy(language = language)
    }

    override suspend fun setFileSystemAccessMode(mode: FileSystemAccessMode) {
        mmkv.encode(StorageConstants.Preferences.KEY_FILE_SYSTEM_ACCESS_MODE, mode.name)
        _settingsState.value = _settingsState.value.copy(fileSystemAccessMode = mode)
    }

    override suspend fun setRootReadOnly(isReadOnly: Boolean) {
        mmkv.encode(StorageConstants.Preferences.KEY_ROOT_READ_ONLY, isReadOnly)
        _settingsState.value = _settingsState.value.copy(isRootReadOnly = isReadOnly)
    }

    override suspend fun addSearchHistory(keyword: String) {
        if (keyword.isBlank()) return
        val currentList = _searchHistoryState.value.toMutableList()
        currentList.remove(keyword)
        currentList.add(0, keyword)
        if (currentList.size > 20) { // Limit history size
            currentList.removeLast()
        }
        val jsonArray = org.json.JSONArray(currentList)
        mmkv.encode(StorageConstants.Preferences.KEY_SEARCH_HISTORY, jsonArray.toString())
        _searchHistoryState.value = currentList
    }

    override suspend fun clearSearchHistory() {
        mmkv.encode(StorageConstants.Preferences.KEY_SEARCH_HISTORY, "[]")
        _searchHistoryState.value = emptyList()
    }
}
