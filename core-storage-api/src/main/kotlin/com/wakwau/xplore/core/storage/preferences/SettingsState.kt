// [Jalur Class]: com.wakwau.xplore.core.storage.preferences.SettingsState
// [Penjelasan]: Menambahkan fileSystemAccessMode dan isRootReadOnly

package com.wakwau.xplore.core.storage.preferences

data class SettingsState(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val fileSystemAccessMode: FileSystemAccessMode = FileSystemAccessMode.NORMAL,
    val isRootReadOnly: Boolean = true
)

