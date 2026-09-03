// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/search/FileIndexSynchronizer.kt
// [Penjelasan]: Komponen sinkronisasi data index Room database dengan dukungan batching bulk operation dan transaksi atomik untuk mencegah I/O thrashing saat mass operations.
package com.wakwau.xplore.core.storage.search

import com.wakwau.xplore.core.storage.db.entity.FileIndexEntity
import com.wakwau.xplore.core.storage.db.repository.FileIndexRepository
import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.FileType
import com.wakwau.xplore.core.util.MimeTypeDetector
import java.util.Locale

class FileIndexSynchronizer(
    private val fileIndexRepository: FileIndexRepository
) {

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/search/FileIndexSynchronizer.kt
    // [Penjelasan]: Menyinkronkan daftar berkas secara bulk/batching ke Room DB.
    suspend fun syncBatch(items: List<FileItem>) {
        if (items.isEmpty()) return
        val indexBatch = items.map { createEntity(it) }
        fileIndexRepository.addOrUpdateIndexBatch(indexBatch)
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/search/FileIndexSynchronizer.kt
    // [Penjelasan]: Menyimpan atau memperbarui indeks entitas berkas tunggal ke tabel file_index di Room DB.
    suspend fun syncSingle(item: FileItem) {
        fileIndexRepository.addOrUpdateIndex(createEntity(item))
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/search/FileIndexSynchronizer.kt
    // [Penjelasan]: Menghapus entitas indeks spesifik beserta turunannya secara atomik di Room DB.
    suspend fun removeSingle(filePath: String) {
        fileIndexRepository.removeIndex(filePath)
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/search/FileIndexSynchronizer.kt
    // [Penjelasan]: Menghapus kumpulan entitas indeks spesifik secara bulk dalam satu transaksi.
    suspend fun removeBatch(filePaths: List<String>) {
        if (filePaths.isEmpty()) return
        fileIndexRepository.removeIndexBatch(filePaths)
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/search/FileIndexSynchronizer.kt
    // [Penjelasan]: Menghapus hierarki entitas indeks berdasarkan prefiks direktori induk di Room DB.
    suspend fun removeByPrefix(prefix: String) {
        fileIndexRepository.removeIndexByPrefix(prefix)
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/search/FileIndexSynchronizer.kt
    // [Penjelasan]: Menghapus banyak hierarki prefiks direktori dalam satu transaksi atomik.
    suspend fun removeByPrefixes(prefixes: List<String>) {
        if (prefixes.isEmpty()) return
        fileIndexRepository.removeIndexByPrefixes(prefixes)
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/search/FileIndexSynchronizer.kt
    // [Penjelasan]: Menyinkronkan perubahan rename secara atomik (hapus old path & subpath, lalu insert new entity).
    suspend fun syncRename(oldPath: String, newItem: FileItem) {
        fileIndexRepository.syncRename(oldPath, createEntity(newItem))
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/search/FileIndexSynchronizer.kt
    // [Penjelasan]: Menyinkronkan pemindahan berkas secara atomik (hapus source path & subpath, lalu insert dest entity).
    suspend fun syncMove(sourcePath: String, destinationItem: FileItem) {
        fileIndexRepository.syncMove(sourcePath, createEntity(destinationItem))
    }

    // [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/search/FileIndexSynchronizer.kt
    // [Penjelasan]: Mengganti seluruh indeks direktori dengan data baru dalam satu transaksi.
    suspend fun replacePrefixIndex(prefix: String, items: List<FileItem>) {
        val entities = items.map { createEntity(it) }
        fileIndexRepository.replacePrefixIndex(prefix, entities)
    }

    private fun createEntity(item: FileItem): FileIndexEntity {
        val isDir = item.type == FileType.DIRECTORY
        val extension = item.name.substringAfterLast('.', "").lowercase(Locale.getDefault())
        val category = MimeTypeDetector.getCategory(item.name, isDir).name

        return FileIndexEntity(
            filePath = item.location.path,
            fileName = item.name,
            size = item.metadata.size,
            extension = extension,
            category = category,
            dateModified = item.metadata.modifiedTime,
            isDirectory = isDir
        )
    }
}
