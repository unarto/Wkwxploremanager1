// [Jalur Class/Modul]: core-storage-api/src/main/kotlin/com/wakwau/xplore/core/storage/permission/StoragePermissionChecker.kt
// [Penjelasan]: Kontrak tunggal untuk pemeriksaan status perizinan penyimpanan secara murni tanpa dependensi Android OS.
package com.wakwau.xplore.core.storage.permission

interface StoragePermissionChecker {
    fun hasAccess(): Boolean
    fun getRequiredPermissionType(): StoragePermissionType
}

