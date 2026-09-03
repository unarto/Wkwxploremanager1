// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/provider/volume/SafVolumeProvider.kt
// [Penjelasan]: Provider khusus untuk mendeteksi dan menyediakan volume dari URI SAF yang memiliki persisted permission dari ContentResolver.
package com.wakwau.xplore.core.storage.provider.volume

import android.content.Context
import com.wakwau.xplore.core.storage.model.StorageVolumeItem
import com.wakwau.xplore.core.storage.model.StorageVolumeType
import com.wakwau.xplore.core.storage.provider.SafVolumeNameResolver

class SafVolumeProvider(
    private val context: Context,
    private val safVolumeNameResolver: SafVolumeNameResolver
) {
    fun getSafVolumes(): List<StorageVolumeItem> {
        val persistedUris = context.contentResolver.persistedUriPermissions
        return persistedUris.map { uriPermission ->
            val uri = uriPermission.uri
            val name = safVolumeNameResolver.resolveVolumeName(uri)
            StorageVolumeItem(
                id = uri.toString().hashCode().toString(),
                name = name,
                rootPath = uri.toString(),
                type = StorageVolumeType.SAF_PROVIDER,
                isReadOnly = !uriPermission.isWritePermission,
                spaceInfo = null,
                createdAt = uriPermission.persistedTime
            )
        }
    }
}
