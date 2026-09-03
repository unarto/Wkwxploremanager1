package com.wakwau.xplore.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wakwau.xplore.core.storage.preferences.AppLanguage
import com.wakwau.xplore.core.storage.preferences.AppThemeMode
import com.wakwau.xplore.core.storage.preferences.AppPreferencesRepository
import com.wakwau.xplore.core.storage.preferences.SettingsState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: AppPreferencesRepository
) : ViewModel() {
    val settingsState: StateFlow<SettingsState> = settingsRepository.settingsState

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
        }
    }

    fun setFileSystemAccessMode(mode: com.wakwau.xplore.core.storage.preferences.FileSystemAccessMode) {
        viewModelScope.launch {
            settingsRepository.setFileSystemAccessMode(mode)
        }
    }

    fun setRootReadOnly(isReadOnly: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRootReadOnly(isReadOnly)
        }
    }

    companion object {
        fun provideFactory(repository: AppPreferencesRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(repository) as T
                }
            }
    }
}
