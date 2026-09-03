// [Jalur Class/Modul]: core-storage-api/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/FileSystemContract.kt
// [Penjelasan]: Kontrak interface dasar untuk seluruh backend sistem berkas (Local, SAF, Shizuku, Root) yang mendefinisikan operasi I/O dan manajemen direktori secara konsisten tanpa ketergantungan pada Android OS, libsu, atau Shizuku.

package com.wakwau.xplore.core.storage.filesystem

import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.operation.FileOperationProgress
import kotlinx.coroutines.flow.Flow

interface FileSystemContract {
    // [Jalur Class/Modul]: core-storage-api/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/FileSystemContract.kt
    // [Penjelasan]: Mendaftar seluruh berkas dan folder pada lokasi penyimpanan tertentu.
    suspend fun listFiles(location: StorageLocation, showHidden: Boolean = true): List<FileItem>

    // [Jalur Class/Modul]: core-storage-api/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/FileSystemContract.kt
    // [Penjelasan]: Membuat direktori baru di dalam lokasi penyimpanan induk.
    suspend fun createDirectory(location: StorageLocation, name: String): FileItem

    // [Jalur Class/Modul]: core-storage-api/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/FileSystemContract.kt
    // [Penjelasan]: Menghapus berkas atau direktori pada lokasi penyimpanan target.
    suspend fun delete(location: StorageLocation)

    // [Jalur Class/Modul]: core-storage-api/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/FileSystemContract.kt
    // [Penjelasan]: Mengubah nama berkas atau direktori pada lokasi penyimpanan.
    suspend fun rename(location: StorageLocation, newName: String): FileItem

    // [Jalur Class/Modul]: core-storage-api/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/FileSystemContract.kt
    // [Penjelasan]: Menyalin berkas/direktori dengan streaming progres.
    fun copy(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress>

    // [Jalur Class/Modul]: core-storage-api/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/FileSystemContract.kt
    // [Penjelasan]: Memindahkan berkas/direktori dengan streaming progres dan verifikasi integritas.
    fun move(source: StorageLocation, destination: StorageLocation): Flow<FileOperationProgress>

    // [Jalur Class/Modul]: core-storage-api/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/FileSystemContract.kt
    // [Penjelasan]: Mengambil entitas FileItem beserta metadata dari lokasi berkas.
    suspend fun getFileItem(location: StorageLocation): FileItem?

    // [Jalur Class/Modul]: core-storage-api/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/FileSystemContract.kt
    // [Penjelasan]: Memeriksa keberadaan berkas atau direktori pada lokasi penyimpanan.
    suspend fun exists(location: StorageLocation): Boolean
}
