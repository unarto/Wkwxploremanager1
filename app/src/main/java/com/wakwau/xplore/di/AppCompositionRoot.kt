// [Jalur Class/Modul]: app/src/main/java/com/wakwau/xplore/di/AppCompositionRoot.kt
// [Penjelasan]: Menghubungkan modul storage (termasuk SettingsRepositoryImpl dari core-storage) dan fileManagerUseCaseModule ke composition root.
package com.wakwau.xplore.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.wakwau.xplore.core.storage.operation.FileCopyServiceManager
import com.wakwau.xplore.settings.SettingsViewModel

class AppCompositionRoot(applicationContext: Context) {
    val storageModule = StorageModule(applicationContext)

    val backgroundOperationManager by lazy {
        FileCopyServiceManager(applicationContext)
    }

    val fileManagerUseCaseModule = FileManagerUseCaseModule(
        directoryRepository = storageModule.directoryRepository,
        fileRepository = storageModule.fileRepository,
        storageVolumeRepository = storageModule.storageVolumeRepository,
        storagePermissionChecker = storageModule.storagePermissionChecker,
        detailedMetadataReader = storageModule.detailedMetadataReader,
        fileChecksumReader = storageModule.fileChecksumReader,
        appPreferencesRepository = storageModule.appPreferencesRepository,
        fileSearchService = storageModule.fileSearchService,
        backgroundOperationManager = backgroundOperationManager,
        conflictDetector = storageModule.conflictDetector,
        conflictResolver = storageModule.conflictResolver,
        safPermissionHandler = storageModule.safPermissionHandler
    )

    val fileManagerPresentationModule = FileManagerPresentationModule(
        useCaseModule = fileManagerUseCaseModule,
        appPreferencesRepository = storageModule.appPreferencesRepository,
        backgroundOperationManager = backgroundOperationManager
    )

    val dualPaneViewModelFactory: ViewModelProvider.Factory by lazy {
        fileManagerPresentationModule.createViewModelFactory()
    }

    val settingsViewModelFactory: ViewModelProvider.Factory by lazy {
        SettingsViewModel.provideFactory(storageModule.appPreferencesRepository)
    }
}
