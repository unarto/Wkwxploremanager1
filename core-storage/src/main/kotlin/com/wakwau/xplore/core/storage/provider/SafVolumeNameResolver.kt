// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/provider/SafVolumeNameResolver.kt
// [Penjelasan]: Mengurai nama volume dinamis dari URI SAF berdasarkan nama aplikasi provider dan label document folder tanpa hardcoded string.
package com.wakwau.xplore.core.storage.provider

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.wakwau.xplore.core.storage.constant.StorageConstants

class SafVolumeNameResolver(private val context: Context) {
    fun resolveVolumeName(uri: Uri): String {
        val authority = uri.authority
        var providerAppName: String? = null

        if (authority != null) {
            try {
                val providerInfo = context.packageManager.resolveContentProvider(authority, 0)
                if (providerInfo != null) {
                    val appInfo = providerInfo.applicationInfo
                    providerAppName = context.packageManager.getApplicationLabel(appInfo).toString()
                }
            } catch (e: Exception) {
                // Abaikan jika gagal me-resolve provider
            }
        }

        var documentName: String? = null
        try {
            val documentFile = DocumentFile.fromTreeUri(context, uri)
            documentName = documentFile?.name
        } catch (e: Exception) {
            // Abaikan jika gagal membaca dokumen
        }

        return when {
            providerAppName != null && documentName != null && documentName.isNotBlank() -> {
                if (providerAppName.equals(documentName, ignoreCase = true)) {
                    providerAppName
                } else {
                    "$providerAppName ($documentName)"
                }
            }
            providerAppName != null -> providerAppName
            documentName != null && documentName.isNotBlank() -> documentName
            else -> StorageConstants.DEFAULT_SAF_VOLUME_NAME
        }
    }
}
