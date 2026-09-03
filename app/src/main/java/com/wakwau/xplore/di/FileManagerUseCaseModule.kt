// [Jalur Class/Modul]: app/src/main/java/com/wakwau/xplore/di/FileManagerUseCaseModule.kt
// [Penjelasan]: Menginisialisasi use case domain (termasuk GetParentLocationUseCase) dan factory domain FileTreeItemFactory.
package com.wakwau.xplore.di

import com.wakwau.xplore.core.storage.checksum.FileChecksumReader
import com.wakwau.xplore.core.storage.metadata.DetailedMetadataReader
import com.wakwau.xplore.core.storage.permission.StoragePermissionChecker
import com.wakwau.xplore.core.storage.preferences.AppPreferencesRepository
import com.wakwau.xplore.core.storage.repository.DirectoryRepository
import com.wakwau.xplore.core.storage.repository.FileRepository
import com.wakwau.xplore.core.storage.repository.StorageVolumeRepository
import com.wakwau.xplore.core.storage.search.FileSearchService
import com.wakwau.xplore.filemanager.factory.FileTreeItemFactory
import com.wakwau.xplore.core.storage.permission.SafPermissionHandler
import com.wakwau.xplore.filemanager.usecase.CheckStoragePermissionUseCase
import com.wakwau.xplore.filemanager.usecase.ComputeFileChecksumUseCase
import com.wakwau.xplore.filemanager.usecase.CopyFilesUseCase
import com.wakwau.xplore.filemanager.usecase.CreateDirectoryUseCase
import com.wakwau.xplore.filemanager.usecase.DeleteFilesUseCase
import com.wakwau.xplore.filemanager.usecase.GetFileDetailedMetadataUseCase
import com.wakwau.xplore.filemanager.usecase.GetParentLocationUseCase
import com.wakwau.xplore.filemanager.usecase.GetStorageVolumesUseCase
import com.wakwau.xplore.filemanager.usecase.LinkStorageUseCase
import com.wakwau.xplore.filemanager.usecase.ListDirectoryUseCase
import com.wakwau.xplore.filemanager.usecase.MoveFilesUseCase
import com.wakwau.xplore.filemanager.usecase.RenameFileUseCase
import com.wakwau.xplore.filemanager.usecase.SearchFilesUseCase
import com.wakwau.xplore.filemanager.usecase.ToggleShowHiddenFilesUseCase

class FileManagerUseCaseModule(
    private val directoryRepository: DirectoryRepository,
    private val fileRepository: FileRepository,
    private val storageVolumeRepository: StorageVolumeRepository,
    private val storagePermissionChecker: StoragePermissionChecker,
    private val detailedMetadataReader: DetailedMetadataReader,
    private val fileChecksumReader: FileChecksumReader,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val fileSearchService: FileSearchService,
    private val backgroundOperationManager: com.wakwau.xplore.core.storage.operation.BackgroundOperationManager,
    private val conflictDetector: com.wakwau.xplore.core.storage.conflict.ConflictDetector,
    private val conflictResolver: com.wakwau.xplore.core.storage.conflict.ConflictResolver,
    private val safPermissionHandler: SafPermissionHandler
) {
    val getStorageVolumesUseCase: GetStorageVolumesUseCase by lazy { GetStorageVolumesUseCase(storageVolumeRepository) }
    val checkStoragePermissionUseCase: CheckStoragePermissionUseCase by lazy { CheckStoragePermissionUseCase(storagePermissionChecker) }
    val getParentLocationUseCase: GetParentLocationUseCase by lazy { GetParentLocationUseCase() }
    val fileTreeItemFactory: FileTreeItemFactory by lazy { FileTreeItemFactory() }
    
    val listDirectoryUseCase: ListDirectoryUseCase by lazy { ListDirectoryUseCase(directoryRepository, appPreferencesRepository) }
    val copyFilesUseCase: CopyFilesUseCase by lazy { CopyFilesUseCase(backgroundOperationManager) }
    val moveFilesUseCase: MoveFilesUseCase by lazy { MoveFilesUseCase(backgroundOperationManager) }
    val deleteFilesUseCase: DeleteFilesUseCase by lazy { DeleteFilesUseCase(backgroundOperationManager) }
    val renameFileUseCase: RenameFileUseCase by lazy { RenameFileUseCase(fileRepository) }
    val createDirectoryUseCase: CreateDirectoryUseCase by lazy { CreateDirectoryUseCase(directoryRepository) }
    
    val linkStorageUseCase: LinkStorageUseCase by lazy { LinkStorageUseCase(safPermissionHandler) }

    val detectConflictsUseCase: com.wakwau.xplore.filemanager.usecase.DetectConflictsUseCase by lazy {
        com.wakwau.xplore.filemanager.usecase.DetectConflictsUseCase(conflictDetector)
    }

    val resolveTransferUseCase: com.wakwau.xplore.filemanager.usecase.ResolveTransferUseCase by lazy {
        com.wakwau.xplore.filemanager.usecase.ResolveTransferUseCase(conflictDetector, conflictResolver)
    }

    val cancelOperationUseCase: com.wakwau.xplore.filemanager.usecase.CancelOperationUseCase by lazy {
        com.wakwau.xplore.filemanager.usecase.CancelOperationUseCase(backgroundOperationManager)
    }

    val getFileDetailedMetadataUseCase: GetFileDetailedMetadataUseCase by lazy {
        GetFileDetailedMetadataUseCase(detailedMetadataReader)
    }

    val computeFileChecksumUseCase: ComputeFileChecksumUseCase by lazy {
        ComputeFileChecksumUseCase(fileChecksumReader)
    }

    val toggleShowHiddenFilesUseCase: ToggleShowHiddenFilesUseCase by lazy {
        ToggleShowHiddenFilesUseCase(appPreferencesRepository)
    }

    val searchFilesUseCase: SearchFilesUseCase by lazy {
        SearchFilesUseCase(fileSearchService)
    }
}
