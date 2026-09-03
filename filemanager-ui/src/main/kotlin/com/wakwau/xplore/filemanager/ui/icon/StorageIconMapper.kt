// [Jalur Class]: com.wakwau.xplore.filemanager.ui.icon.StorageIconMapper
// [Penjelasan]: Mengecek apakah item berkas adalah root internal storage secara dinamis menggunakan Environment API dan konstanta StorageConstants tanpa hardcoding.
package com.wakwau.xplore.filemanager.ui.icon

import android.os.Environment
import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.model.FileItem

object StorageIconMapper {
    fun isInternalStorage(item: FileItem): Boolean {
        val primaryPath = try {
            Environment.getExternalStorageDirectory().absolutePath
        } catch (_: Exception) {
            StorageConstants.DEFAULT_PRIMARY_STORAGE_PATH
        }
        return item.location.path.equals(primaryPath, ignoreCase = true) ||
               item.location.path.equals(StorageConstants.DEFAULT_PRIMARY_STORAGE_PATH, ignoreCase = true) ||
               item.name.equals(StorageConstants.DEFAULT_PRIMARY_VOLUME_NAME, ignoreCase = true) ||
               item.location.rootId == StorageConstants.PRIMARY_INTERNAL_VOLUME_ID
    }
}
