// [Jalur Class/Modul]: app/src/main/java/com/wakwau/xplore/di/StorageModule.kt
// [Penjelasan]: Menginisialisasi komponen LocalFileSystem, SafFileSystem, SafShizukuFileSystem, RootFileSystem, SuPermissionChecker, volume providers granular, search traversal/synchronizer, Room Database, AppPreferences (MMKV), dan repository data terstruktur.
package com.wakwau.xplore.di

import android.content.Context
import com.wakwau.xplore.core.storage.checksum.FileChecksumReader
import com.wakwau.xplore.core.storage.checksum.LocalFileChecksumCalculator
import com.wakwau.xplore.core.storage.db.DatabaseProvider
import com.wakwau.xplore.core.storage.db.repository.FileIndexRepository
import com.wakwau.xplore.core.storage.db.repository.FileIndexRepositoryImpl
import com.wakwau.xplore.core.storage.error.StorageErrorMapper
import com.wakwau.xplore.core.storage.filesystem.LocalFileSystemContract
import com.wakwau.xplore.core.storage.filesystem.RootFileSystemContract
import com.wakwau.xplore.core.storage.filesystem.SafFileSystemContract
import com.wakwau.xplore.core.storage.filesystem.ShizukuFileSystemContract
import com.wakwau.xplore.core.storage.filesystem.StorageBackendClassifier
import com.wakwau.xplore.core.storage.filesystem.bridge.CrossFilesystemTransferBridge
import com.wakwau.xplore.core.storage.filesystem.local.LocalFileSystem
import com.wakwau.xplore.core.storage.filesystem.root.RootFileSystem
import com.wakwau.xplore.core.storage.filesystem.saf.SafFileSystem
import com.wakwau.xplore.core.storage.filesystem.shizuku.SafShizukuFileSystem
import com.wakwau.xplore.core.storage.mapper.FileItemMapper
import com.wakwau.xplore.core.storage.metadata.DetailedMetadataReader
import com.wakwau.xplore.core.storage.metadata.FileMetadataReader
import com.wakwau.xplore.core.storage.metadata.LocalDetailedMetadataReader
import com.wakwau.xplore.core.storage.permission.AndroidStoragePermissionChecker
import com.wakwau.xplore.core.storage.permission.CompositeStoragePermissionChecker
import com.wakwau.xplore.core.storage.permission.FilePermissionFormatter
import com.wakwau.xplore.core.storage.permission.ShizukuPermissionChecker
import com.wakwau.xplore.core.storage.permission.StoragePermissionChecker
import com.wakwau.xplore.core.storage.permission.SuPermissionChecker
import com.wakwau.xplore.core.storage.preferences.AppPreferences
import com.wakwau.xplore.core.storage.preferences.AppPreferencesRepository
import com.wakwau.xplore.core.storage.provider.InternalStoragePathResolver
import com.wakwau.xplore.core.storage.provider.SafVolumeNameResolver
import com.wakwau.xplore.core.storage.provider.StorageSpaceReader
import com.wakwau.xplore.core.storage.provider.volume.ExternalVolumeProvider
import com.wakwau.xplore.core.storage.provider.volume.InternalVolumeProvider
import com.wakwau.xplore.core.storage.provider.volume.RootVolumeProvider
import com.wakwau.xplore.core.storage.provider.volume.SafVolumeProvider
import com.wakwau.xplore.core.storage.repository.DirectoryRepository
import com.wakwau.xplore.core.storage.repository.DirectoryRepositoryImpl
import com.wakwau.xplore.core.storage.repository.FileRepository
import com.wakwau.xplore.core.storage.repository.FileRepositoryImpl
import com.wakwau.xplore.core.storage.repository.StorageVolumeRepository
import com.wakwau.xplore.core.storage.repository.StorageVolumeRepositoryImpl
import com.wakwau.xplore.core.storage.search.FileIndexSynchronizer
import com.wakwau.xplore.core.storage.search.FileSearchService
import com.wakwau.xplore.core.storage.search.FileSearchServiceImpl
import com.wakwau.xplore.core.storage.search.FileSystemSearchTraversal

class StorageModule(private val applicationContext: Context) {
    private val internalStoragePathResolver = InternalStoragePathResolver()
    private val storageSpaceReader = StorageSpaceReader()
    private val safVolumeNameResolver = SafVolumeNameResolver(applicationContext)
    
    private val fileMetadataReader = FileMetadataReader()
    private val fileItemMapper = FileItemMapper()
    private val storageErrorMapper = StorageErrorMapper()
    private val filePermissionFormatter = FilePermissionFormatter()

    val localFileSystem: LocalFileSystemContract by lazy {
        LocalFileSystem(
            fileMetadataReader = fileMetadataReader,
            fileItemMapper = fileItemMapper
        )
    }

    val safFileSystem: SafFileSystemContract by lazy {
        SafFileSystem(applicationContext)
    }

    val safShizukuFileSystem: ShizukuFileSystemContract by lazy {
        SafShizukuFileSystem(applicationContext)
    }

    val rootFileSystem: RootFileSystemContract by lazy {
        RootFileSystem()
    }

    val internalVolumeProvider: InternalVolumeProvider by lazy {
        InternalVolumeProvider(
            internalStoragePathResolver = internalStoragePathResolver,
            storageSpaceReader = storageSpaceReader
        )
    }

    val externalVolumeProvider: ExternalVolumeProvider by lazy {
        ExternalVolumeProvider(
            context = applicationContext,
            storageSpaceReader = storageSpaceReader
        )
    }

    val rootVolumeProvider: RootVolumeProvider by lazy {
        RootVolumeProvider(
            storageSpaceReader = storageSpaceReader
        )
    }

    val safVolumeProvider: SafVolumeProvider by lazy {
        SafVolumeProvider(
            context = applicationContext,
            safVolumeNameResolver = safVolumeNameResolver
        )
    }

    val appPreferencesRepository: AppPreferencesRepository by lazy {
        AppPreferences.create(applicationContext)
    }

    val fileIndexRepository: FileIndexRepository by lazy {
        FileIndexRepositoryImpl(DatabaseProvider.provideFileIndexDao(applicationContext))
    }

    val fileChecksumReader: FileChecksumReader by lazy {
        LocalFileChecksumCalculator(applicationContext)
    }

    val detailedMetadataReader: DetailedMetadataReader by lazy {
        LocalDetailedMetadataReader(
            permissionFormatter = filePermissionFormatter,
            context = applicationContext
        )
    }

    val safPermissionHandler: com.wakwau.xplore.core.storage.permission.SafPermissionHandler by lazy {
        com.wakwau.xplore.core.storage.permission.SafPermissionHandlerImpl(applicationContext)
    }

    val suPermissionChecker: SuPermissionChecker by lazy {
        SuPermissionChecker()
    }

    val shizukuPermissionChecker: ShizukuPermissionChecker by lazy {
        ShizukuPermissionChecker()
    }

    val storagePermissionChecker: StoragePermissionChecker by lazy {
        CompositeStoragePermissionChecker(
            AndroidStoragePermissionChecker(applicationContext),
            shizukuPermissionChecker,
            suPermissionChecker
        )
    }

    val storageVolumeRepository: StorageVolumeRepository by lazy {
        StorageVolumeRepositoryImpl(
            context = applicationContext,
            internalVolumeProvider = internalVolumeProvider,
            externalVolumeProvider = externalVolumeProvider,
            rootVolumeProvider = rootVolumeProvider,
            safVolumeProvider = safVolumeProvider
        )
    }

    val fileIndexSynchronizer: FileIndexSynchronizer by lazy {
        FileIndexSynchronizer(
            fileIndexRepository = fileIndexRepository
        )
    }

    val storageBackendClassifier: StorageBackendClassifier by lazy {
        StorageBackendClassifier(
            isSuAvailable = { suPermissionChecker.hasAccess() },
            isShizukuAvailable = { shizukuPermissionChecker.hasAccess() },
            isSafPersisted = { path -> safPermissionHandler.hasPersistedPermission(path) }
        )
    }

    val directoryRepository: DirectoryRepository by lazy {
        DirectoryRepositoryImpl(
            localFileSystem = localFileSystem,
            safFileSystem = safFileSystem,
            safShizukuFileSystem = safShizukuFileSystem,
            rootFileSystem = rootFileSystem,
            backendClassifier = storageBackendClassifier,
            storageErrorMapper = storageErrorMapper,
            fileIndexSynchronizer = fileIndexSynchronizer
        )
    }

    val crossFilesystemTransferBridge: CrossFilesystemTransferBridge by lazy {
        CrossFilesystemTransferBridge(
            context = applicationContext,
            localFileSystem = localFileSystem,
            safFileSystem = safFileSystem,
            safShizukuFileSystem = safShizukuFileSystem,
            rootFileSystem = rootFileSystem
        )
    }

    val fileRepository: FileRepository by lazy {
        FileRepositoryImpl(
            localFileSystem = localFileSystem,
            safFileSystem = safFileSystem,
            safShizukuFileSystem = safShizukuFileSystem,
            rootFileSystem = rootFileSystem,
            crossFilesystemTransferBridge = crossFilesystemTransferBridge,
            backendClassifier = storageBackendClassifier,
            storageErrorMapper = storageErrorMapper,
            fileIndexSynchronizer = fileIndexSynchronizer
        )
    }

    val fileSearchTraversal: FileSystemSearchTraversal by lazy {
        FileSystemSearchTraversal(
            directoryRepository = directoryRepository
        )
    }

    val fileSearchService: FileSearchService by lazy {
        FileSearchServiceImpl(
            traversal = fileSearchTraversal,
            indexSynchronizer = fileIndexSynchronizer
        )
    }

    val conflictDetector: com.wakwau.xplore.core.storage.conflict.ConflictDetector by lazy {
        com.wakwau.xplore.core.storage.conflict.DefaultConflictDetector(
            localFileSystem = localFileSystem,
            safFileSystem = safFileSystem,
            safShizukuFileSystem = safShizukuFileSystem,
            rootFileSystem = rootFileSystem,
            backendClassifier = storageBackendClassifier
        )
    }

    val conflictResolver: com.wakwau.xplore.core.storage.conflict.ConflictResolver by lazy {
        com.wakwau.xplore.core.storage.conflict.DefaultConflictResolver()
    }

}




