// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/provider/StorageSpaceReader.kt
// [Penjelasan]: Pembaca kapasitas ruang penyimpanan file system menggunakan StatFs dengan penanganan error yang aman.
package com.wakwau.xplore.core.storage.provider

import android.os.StatFs
import com.wakwau.xplore.core.storage.model.StorageSpaceInfo
import java.io.File

open class StorageSpaceReader {
    open fun getSpaceInfo(path: String): StorageSpaceInfo? {
        val file = File(path)
        if (!file.exists() || !file.canRead()) {
            return null
        }
        
        return try {
            val statFs = StatFs(path)
            val blockSize = statFs.blockSizeLong
            val totalBlocks = statFs.blockCountLong
            val availableBlocks = statFs.availableBlocksLong
            
            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = totalBytes - freeBytes
            
            val percentageUsed = if (totalBytes > 0) {
                ((usedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt()
            } else {
                0
            }
            
            StorageSpaceInfo(
                totalBytes = totalBytes,
                freeBytes = freeBytes,
                usedBytes = usedBytes,
                percentageUsed = percentageUsed
            )
        } catch (e: Throwable) {
            null
        }
    }
}
