package com.wakwau.xplore.core.storage.repository

import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.operation.FileOperationProgress
import com.wakwau.xplore.core.storage.operation.FileOperationResult
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    suspend fun delete(location: StorageLocation): FileOperationResult<Unit>
    suspend fun rename(location: StorageLocation, newName: String): FileOperationResult<FileItem>
    fun copy(source: StorageLocation, destination: StorageLocation): Flow<FileOperationResult<FileOperationProgress>>
    fun move(source: StorageLocation, destination: StorageLocation): Flow<FileOperationResult<FileOperationProgress>>
}
