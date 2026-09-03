// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/permission/CompositeStoragePermissionChecker.kt
// [Penjelasan]: Menggabungkan pemeriksaan izin Android standar, Shizuku, dan SU (Root) untuk akses penyimpanan menyeluruh.
package com.wakwau.xplore.core.storage.permission

class CompositeStoragePermissionChecker(
    private val androidPermissionChecker: StoragePermissionChecker,
    private val shizukuPermissionChecker: ShizukuPermissionChecker,
    private val suPermissionChecker: SuPermissionChecker? = null
) : StoragePermissionChecker {

    override fun hasAccess(): Boolean {
        return androidPermissionChecker.hasAccess() ||
                shizukuPermissionChecker.hasAccess() ||
                (suPermissionChecker?.hasAccess() == true)
    }

    override fun getRequiredPermissionType(): StoragePermissionType {
        return androidPermissionChecker.getRequiredPermissionType()
    }
}

