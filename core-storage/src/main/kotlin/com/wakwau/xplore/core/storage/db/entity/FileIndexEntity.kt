// [Jalur Class]: com.wakwau.xplore.core.storage.db.entity.FileIndexEntity
// [Penjelasan]: Entity Room untuk indeks metadata berkas dengan index eksplisit guna mempercepat operasi pencarian, filter ekstensi, filter modifikasi, dan pengelompokan kategori.
package com.wakwau.xplore.core.storage.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "file_index",
    indices = [
        Index(value = ["category"]),
        Index(value = ["dateModified"]),
        Index(value = ["extension"]),
        Index(value = ["fileName"])
    ]
)
data class FileIndexEntity(
    @PrimaryKey
    val filePath: String,
    val fileName: String,
    val size: Long,
    val extension: String,
    val category: String,
    val dateModified: Long,
    val isDirectory: Boolean = false
)
