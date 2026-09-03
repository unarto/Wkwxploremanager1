package com.wakwau.xplore.core.storage.permission

import java.io.File

object FilePermissionConstants {
    const val POSIX_READ = "r"
    const val POSIX_WRITE = "w"
    const val POSIX_EXECUTE = "x"
    const val POSIX_NONE = "-"
}

// [Jalur Class]: com.wakwau.xplore.core.storage.permission.FilePermissionFormatter
// [Penjelasan]: Membaca dan memformat status perizinan berkas (read, write, execute) ke format standar POSIX 3-karakter (misalnya rwx atau rw-).
class FilePermissionFormatter {
    fun formatPosixPermissions(file: File): String {
        val r = if (file.canRead()) FilePermissionConstants.POSIX_READ else FilePermissionConstants.POSIX_NONE
        val w = if (file.canWrite()) FilePermissionConstants.POSIX_WRITE else FilePermissionConstants.POSIX_NONE
        val x = if (file.canExecute()) FilePermissionConstants.POSIX_EXECUTE else FilePermissionConstants.POSIX_NONE
        return "$r$w$x"
    }
}
