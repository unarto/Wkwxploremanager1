// [Jalur Class]: com.wakwau.xplore.core.storage.db.repository.FileIndexRepositoryImpl
// [Penjelasan]: Implementasi FileIndexRepository yang mengeksekusi query pencarian dan transaksi indeks berkas via FileIndexDao dengan dukungan batching dan atomisitas.
package com.wakwau.xplore.core.storage.db.repository

import com.wakwau.xplore.core.storage.db.dao.FileIndexDao
import com.wakwau.xplore.core.storage.db.entity.FileIndexEntity
import kotlinx.coroutines.flow.Flow

class FileIndexRepositoryImpl(
    private val fileIndexDao: FileIndexDao
) : FileIndexRepository {

    override fun searchFiles(
        locationPrefix: String,
        keyword: String,
        minSize: Long?,
        maxSize: Long?,
        extension: String?
    ): Flow<List<FileIndexEntity>> {
        return fileIndexDao.searchFiles(
            locationPrefix = locationPrefix,
            keyword = keyword,
            minSize = minSize,
            maxSize = maxSize,
            extension = extension
        )
    }

    override fun getFilesByCategory(category: String): Flow<List<FileIndexEntity>> {
        return fileIndexDao.getFilesByCategory(category)
    }

    override suspend fun addOrUpdateIndex(entity: FileIndexEntity) {
        fileIndexDao.insertOrUpdate(entity)
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/db/repository/FileIndexRepositoryImpl.kt
    // [Penjelasan]: Menambahkan atau memperbarui indeks dalam batch chunked via transaksi DAO.
    override suspend fun addOrUpdateIndexBatch(entities: List<FileIndexEntity>) {
        fileIndexDao.insertOrUpdateBatch(entities)
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/db/repository/FileIndexRepositoryImpl.kt
    // [Penjelasan]: Menghapus indeks berkas dan seluruh turunan hierarkisnya secara atomik.
    override suspend fun removeIndex(filePath: String) {
        fileIndexDao.deletePathAndChildren(filePath)
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/db/repository/FileIndexRepositoryImpl.kt
    // [Penjelasan]: Menghapus kumpulan path berkas secara bulk dalam satu transaksi.
    override suspend fun removeIndexBatch(filePaths: List<String>) {
        fileIndexDao.deleteBatch(filePaths)
    }

    override suspend fun removeIndexByPrefix(locationPrefix: String) {
        fileIndexDao.deleteByPrefix(locationPrefix)
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/db/repository/FileIndexRepositoryImpl.kt
    // [Penjelasan]: Menghapus kumpulan awalan direktori secara bulk dalam satu transaksi.
    override suspend fun removeIndexByPrefixes(locationPrefixes: List<String>) {
        fileIndexDao.deleteByPrefixes(locationPrefixes)
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/db/repository/FileIndexRepositoryImpl.kt
    // [Penjelasan]: Mengganti indeks direktori secara atomik.
    override suspend fun replacePrefixIndex(locationPrefix: String, entities: List<FileIndexEntity>) {
        fileIndexDao.replacePrefixIndex(locationPrefix, entities)
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/db/repository/FileIndexRepositoryImpl.kt
    // [Penjelasan]: Menyinkronkan perubahan nama berkas/direktori secara atomik.
    override suspend fun syncRename(oldPath: String, newEntity: FileIndexEntity) {
        fileIndexDao.syncRenameAtomic(oldPath, newEntity)
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/db/repository/FileIndexRepositoryImpl.kt
    // [Penjelasan]: Menyinkronkan pemindahan berkas/direktori secara atomik.
    override suspend fun syncMove(sourcePath: String, destinationEntity: FileIndexEntity) {
        fileIndexDao.syncMoveAtomic(sourcePath, destinationEntity)
    }

    override suspend fun clearIndex() {
        fileIndexDao.clearAll()
    }
}
