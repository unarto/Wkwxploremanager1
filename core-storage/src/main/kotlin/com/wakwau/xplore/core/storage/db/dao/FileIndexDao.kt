// [Jalur Class]: com.wakwau.xplore.core.storage.db.dao.FileIndexDao
// [Penjelasan]: Data Access Object (DAO) untuk query Room tabel file_index dengan dukungan batch query dan transaksi atomik (@Transaction) untuk mencegah file index thrashing dan inkonsistensi saat kegagalan operasi berkas masif.
package com.wakwau.xplore.core.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.wakwau.xplore.core.storage.db.entity.FileIndexEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileIndexDao {
    @Query("""
        SELECT * FROM file_index 
        WHERE filePath LIKE :locationPrefix || '%'
        AND LOWER(fileName) LIKE '%' || LOWER(:keyword) || '%'
        AND (:minSize IS NULL OR size >= :minSize)
        AND (:maxSize IS NULL OR size <= :maxSize)
        AND (:extension IS NULL OR LOWER(extension) = LOWER(:extension))
        ORDER BY dateModified DESC
    """)
    fun searchFiles(
        locationPrefix: String,
        keyword: String,
        minSize: Long?,
        maxSize: Long?,
        extension: String?
    ): Flow<List<FileIndexEntity>>

    @Query("SELECT * FROM file_index WHERE category = :category ORDER BY dateModified DESC")
    fun getFilesByCategory(category: String): Flow<List<FileIndexEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: FileIndexEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(entities: List<FileIndexEntity>)

    @Query("DELETE FROM file_index WHERE filePath = :filePath")
    suspend fun deleteByPath(filePath: String)

    @Query("DELETE FROM file_index WHERE filePath IN (:filePaths)")
    suspend fun deleteByPaths(filePaths: List<String>)

    @Query("DELETE FROM file_index WHERE filePath LIKE :locationPrefix || '%'")
    suspend fun deleteByPrefix(locationPrefix: String)

    @Query("DELETE FROM file_index")
    suspend fun clearAll()

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/db/dao/FileIndexDao.kt
    // [Penjelasan]: Menghapus berkas tunggal beserta seluruh sub-entitas hierarkisnya dalam satu transaksi atomik Room.
    @Transaction
    suspend fun deletePathAndChildren(filePath: String) {
        deleteByPath(filePath)
        val prefixWithSlash = if (filePath.endsWith("/")) filePath else "$filePath/"
        deleteByPrefix(prefixWithSlash)
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/db/dao/FileIndexDao.kt
    // [Penjelasan]: Memasukkan atau memperbarui kumpulan entitas secara bulk dengan batching 500 entitas per batch dalam transaksi atomik.
    @Transaction
    suspend fun insertOrUpdateBatch(entities: List<FileIndexEntity>) {
        if (entities.isEmpty()) return
        entities.chunked(500).forEach { chunk ->
            insertOrUpdateAll(chunk)
        }
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/db/dao/FileIndexDao.kt
    // [Penjelasan]: Menghapus kumpulan path berkas secara bulk dengan chunking 500 path dalam transaksi atomik untuk menghindari batasan variabel SQLite.
    @Transaction
    suspend fun deleteBatch(filePaths: List<String>) {
        if (filePaths.isEmpty()) return
        filePaths.chunked(500).forEach { chunk ->
            deleteByPaths(chunk)
        }
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/db/dao/FileIndexDao.kt
    // [Penjelasan]: Menghapus seluruh awalan direktori dan turunannya secara bulk dalam satu transaksi atomik.
    @Transaction
    suspend fun deleteByPrefixes(prefixes: List<String>) {
        if (prefixes.isEmpty()) return
        prefixes.forEach { prefix ->
            deleteByPrefix(prefix)
            val prefixWithSlash = if (prefix.endsWith("/")) prefix else "$prefix/"
            deleteByPrefix(prefixWithSlash)
        }
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/db/dao/FileIndexDao.kt
    // [Penjelasan]: Mengganti seluruh indeks direktori dengan data baru dalam satu transaksi atomik.
    @Transaction
    suspend fun replacePrefixIndex(locationPrefix: String, entities: List<FileIndexEntity>) {
        deleteByPrefix(locationPrefix)
        val prefixWithSlash = if (locationPrefix.endsWith("/")) locationPrefix else "$locationPrefix/"
        deleteByPrefix(prefixWithSlash)
        if (entities.isNotEmpty()) {
            insertOrUpdateBatch(entities)
        }
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/db/dao/FileIndexDao.kt
    // [Penjelasan]: Menghapus path lama beserta sub-entitas dan menyimpan entitas baru hasil rename dalam satu transaksi atomik.
    @Transaction
    suspend fun syncRenameAtomic(oldPath: String, newEntity: FileIndexEntity) {
        deletePathAndChildren(oldPath)
        insertOrUpdate(newEntity)
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/db/dao/FileIndexDao.kt
    // [Penjelasan]: Menghapus path sumber beserta sub-entitas dan menyimpan entitas destinasi hasil pemindahan dalam satu transaksi atomik.
    @Transaction
    suspend fun syncMoveAtomic(sourcePath: String, destinationEntity: FileIndexEntity) {
        deletePathAndChildren(sourcePath)
        insertOrUpdate(destinationEntity)
    }
}
