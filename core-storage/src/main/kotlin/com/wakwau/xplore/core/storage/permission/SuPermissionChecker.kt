// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/permission/SuPermissionChecker.kt
// [Penjelasan]: Memeriksa ketersediaan akses SU (Root) menggunakan API libsu Topjohnwu tanpa ketergantungan Shizuku.

package com.wakwau.xplore.core.storage.permission

import com.topjohnwu.superuser.Shell

open class SuPermissionChecker(
    private val rootCheck: () -> Boolean = {
        try {
            Shell.isAppGrantedRoot() == true || (Shell.isAppGrantedRoot() == null && Shell.getShell().isRoot)
        } catch (_: Exception) {
            false
        }
    }
) : StoragePermissionChecker {

    override fun hasAccess(): Boolean = isSuAvailable()

    override fun getRequiredPermissionType(): StoragePermissionType = StoragePermissionType.SHIZUKU_ROOT

    open fun isSuAvailable(): Boolean {
        return try {
            rootCheck()
        } catch (_: Exception) {
            false
        }
    }
}
