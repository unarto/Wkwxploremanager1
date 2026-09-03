// [Jalur Class/Modul]: core-storage-api/src/main/kotlin/com/wakwau/xplore/core/storage/operation/BackgroundOperationManager.kt
// [Penjelasan]: Antarmuka untuk menangani operasi I/O latar belakang (WorkManager) dengan dukungan transfer ter-resolve (Conflict Resolution) dan pembatalan operasi.
package com.wakwau.xplore.core.storage.operation

import com.wakwau.xplore.core.storage.conflict.ResolvedTransferItem
import com.wakwau.xplore.core.storage.model.StorageLocation
import kotlinx.coroutines.flow.Flow

enum class BackgroundOperationType { COPY, MOVE, DELETE }

interface BackgroundOperationManager {
    fun enqueueOperation(type: BackgroundOperationType, sources: List<StorageLocation>, destination: StorageLocation? = null)
    fun enqueueResolvedOperation(type: BackgroundOperationType, resolvedItems: List<ResolvedTransferItem>)
    fun cancelOperation()
    fun observeProgress(): Flow<FileOperationResult<FileOperationProgress>>
}

