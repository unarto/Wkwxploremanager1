// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/provider/InternalStoragePathResolver.kt
// [Penjelasan]: Resolusi path root dan internal storage terstandarisasi menggunakan StorageConstants dan Android Environment API dengan penanganan fallback aman.
package com.wakwau.xplore.core.storage.provider

import android.os.Environment
import com.wakwau.xplore.core.storage.constant.StorageConstants

open class InternalStoragePathResolver {
    open fun getPrimaryInternalStoragePath(): String {
        return try {
            Environment.getExternalStorageDirectory()?.absolutePath ?: StorageConstants.DEFAULT_PRIMARY_STORAGE_PATH
        } catch (e: Throwable) {
            StorageConstants.DEFAULT_PRIMARY_STORAGE_PATH
        }
    }

    open fun getRootPath(): String {
        return StorageConstants.ROOT_PATH
    }
}

