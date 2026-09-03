// [Jalur Class]: com.wakwau.xplore.core.storage.db.repository.FileIndexRepository
// [Penjelasan]: Antarmuka kontrak repository pengelolaan indeks pencarian cepat dan pengelompokan berkas dengan dukungan operasi batch dan sinkronisasi atomik.
package com.wakwau.xplore.core.storage.db.repository

import com.wakwau.xplore.core.storage.db.entity.FileIndexEntity
import kotlinx.coroutines.flow.Flow

interface FileIndexRepository {
    fun searchFiles(
        locationPrefix: String,
        keyword: String,
        minSize: Long?,
        maxSize: Long?,
        extension: String?
    ): Flow<List<FileIndexEntity>>

    fun getFilesByCategory(category: String): Flow<List<FileIndexEntity>>

    suspend fun addOrUpdateIndex(entity: FileIndexEntity)

    suspend fun addOrUpdateIndexBatch(entities: List<FileIndexEntity>)

    suspend fun removeIndex(filePath: String)

    suspend fun removeIndexBatch(filePaths: List<String>)

    suspend fun removeIndexByPrefix(locationPrefix: String)

    suspend fun removeIndexByPrefixes(locationPrefixes: List<String>)

    suspend fun replacePrefixIndex(locationPrefix: String, entities: List<FileIndexEntity>)

    suspend fun syncRename(oldPath: String, newEntity: FileIndexEntity)

    suspend fun syncMove(sourcePath: String, destinationEntity: FileIndexEntity)

    suspend fun clearIndex()
}
