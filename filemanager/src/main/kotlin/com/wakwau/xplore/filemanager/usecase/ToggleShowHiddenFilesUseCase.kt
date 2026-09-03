// [Jalur Class]: com.wakwau.xplore.filemanager.usecase.ToggleShowHiddenFilesUseCase
// [Penjelasan]: Use case untuk membaca dan membalik (toggle) status preferensi tampilkan berkas tersembunyi (hidden files) via AppPreferencesRepository.
package com.wakwau.xplore.filemanager.usecase

import com.wakwau.xplore.core.storage.preferences.AppPreferencesRepository

class ToggleShowHiddenFilesUseCase(
    private val appPreferencesRepository: AppPreferencesRepository
) {
    suspend operator fun invoke(): Boolean {
        val currentState = appPreferencesRepository.getPreferencesState().showHiddenFiles
        val newState = !currentState
        appPreferencesRepository.setShowHiddenFiles(newState)
        return newState
    }
}
