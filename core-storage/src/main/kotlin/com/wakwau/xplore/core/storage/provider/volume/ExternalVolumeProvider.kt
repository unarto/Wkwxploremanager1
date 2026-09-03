// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/provider/volume/ExternalVolumeProvider.kt
// [Penjelasan]: Provider khusus untuk mendeteksi volume eksternal sekunder (SD Card dan USB OTG) melalui ContextCompat dan StorageManager tanpa hardcoding.
package com.wakwau.xplore.core.storage.provider.volume

import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import androidx.core.content.ContextCompat
import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.model.StorageVolumeItem
import com.wakwau.xplore.core.storage.model.StorageVolumeType
import com.wakwau.xplore.core.storage.provider.StorageSpaceReader
import java.io.File

class ExternalVolumeProvider(
    private val context: Context,
    private val storageSpaceReader: StorageSpaceReader
) {
    fun getExternalVolumes(primaryRootPath: String): List<StorageVolumeItem> {
        val volumes = mutableListOf<StorageVolumeItem>()
        val externalDirs = ContextCompat.getExternalFilesDirs(context, null)
        for (dir in externalDirs) {
            if (dir == null) continue
            val absolutePath = dir.absolutePath
            if (absolutePath.contains(StorageConstants.ANDROID_DATA_PATH_SEGMENT)) {
                val rootPath = absolutePath.substringBefore(StorageConstants.ANDROID_DATA_PATH_SEGMENT)

                // Lewati jika ini adalah internal storage
                if (rootPath == primaryRootPath || rootPath.contains(StorageConstants.EMULATED_PATH_SEGMENT)) {
                    continue
                }

                // Cek apakah SD Card atau USB OTG via StorageManager
                var type = StorageVolumeType.UNKNOWN
                var name = StorageConstants.DEFAULT_EXTERNAL_VOLUME_NAME

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
                    val storageVolume = storageManager?.getStorageVolume(File(rootPath))
                    if (storageVolume != null) {
                        name = storageVolume.getDescription(context) ?: name
                        type = if (storageVolume.isRemovable) {
                            if (storageVolume.uuid?.contains("-") == true) StorageVolumeType.SECONDARY_SDCARD else StorageVolumeType.USB_OTG
                        } else {
                            StorageVolumeType.UNKNOWN
                        }
                    } else {
                        type = if (rootPath.contains("-")) StorageVolumeType.SECONDARY_SDCARD else StorageVolumeType.USB_OTG
                    }
                } else {
                    type = if (rootPath.contains("-")) StorageVolumeType.SECONDARY_SDCARD else StorageVolumeType.USB_OTG
                }

                val spaceInfo = storageSpaceReader.getSpaceInfo(rootPath)
                volumes.add(
                    StorageVolumeItem(
                        id = rootPath.hashCode().toString(),
                        name = name,
                        rootPath = rootPath,
                        type = type,
                        isReadOnly = false,
                        spaceInfo = spaceInfo
                    )
                )
            }
        }
        return volumes
    }
}
