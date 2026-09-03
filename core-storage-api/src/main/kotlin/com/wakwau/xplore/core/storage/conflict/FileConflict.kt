// [Jalur Class/Modul]: core-storage-api/src/main/kotlin/com/wakwau/xplore/core/storage/conflict/FileConflict.kt
// [Penjelasan]: Model representasi benturan nama antara entitas berkas/direktori sumber dan entitas yang sudah ada di lokasi tujuan.
package com.wakwau.xplore.core.storage.conflict

import com.wakwau.xplore.core.storage.model.StorageLocation

data class FileConflict(
    val source: StorageLocation,
    val sourceName: String,
    val targetName: String,
    val isDirectory: Boolean,
    val destinationDir: StorageLocation
)
