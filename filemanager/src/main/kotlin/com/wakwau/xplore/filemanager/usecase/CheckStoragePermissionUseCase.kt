// [Jalur Class/Modul]: filemanager/src/main/kotlin/com/wakwau/xplore/filemanager/usecase/CheckStoragePermissionUseCase.kt
// [Penjelasan]: Use case domain untuk mengecek status perizinan dan tipe izin yang dibutuhkan secara murni tanpa dependensi framework Android.
package com.wakwau.xplore.filemanager.usecase

import com.wakwau.xplore.core.storage.permission.StoragePermissionChecker
import com.wakwau.xplore.core.storage.permission.StoragePermissionType

class CheckStoragePermissionUseCase(private val permissionChecker: StoragePermissionChecker) {
    fun hasPermission(): Boolean {
        return permissionChecker.hasAccess()
    }
    
    fun getRequiredPermission(): StoragePermissionType {
        return permissionChecker.getRequiredPermissionType()
    }
}
