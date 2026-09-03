package com.wakwau.xplore.core.storage.permission

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

// [Jalur Class]: com.wakwau.xplore.core.storage.permission.ShizukuPermissionChecker
// [Penjelasan]: Mengimplementasikan StoragePermissionChecker untuk memeriksa apakah izin Shizuku telah diberikan, mengadopsi kemampuan akses root/ADB via ShizukuProvider dan BinderContainer.
class ShizukuPermissionChecker : StoragePermissionChecker {
    override fun hasAccess(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    override fun getRequiredPermissionType(): StoragePermissionType {
        return StoragePermissionType.SHIZUKU_ROOT
    }

    fun isShizukuAvailable(): Boolean {
        return Shizuku.pingBinder()
    }
    
    fun requestPermission(requestCode: Int) {
        if (isShizukuAvailable() && !hasAccess()) {
            Shizuku.requestPermission(requestCode)
        }
    }
}
