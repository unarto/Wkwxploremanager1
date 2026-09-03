// [Jalur Class]: com.wakwau.xplore.core.storage.permission.SafPermissionHandlerImpl
// [Penjelasan]: Implementasi dari SafPermissionHandler untuk memanggil API ContentResolver serta memeriksa izin URI persistable.

package com.wakwau.xplore.core.storage.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

class SafPermissionHandlerImpl(private val context: Context) : SafPermissionHandler {
    override fun takePersistableUriPermission(uriString: String) {
        val uri = Uri.parse(uriString)
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            Log.d("SafPermissionHandler", "Successfully taken persistable URI permission for $uri")
        } catch (e: Exception) {
            Log.e("SafPermissionHandler", "Failed to take persistable URI permission for $uri", e)
        }
    }

    override fun releasePersistableUriPermission(uriString: String) {
        val uri = Uri.parse(uriString)
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.releasePersistableUriPermission(uri, takeFlags)
            Log.d("SafPermissionHandler", "Successfully released persistable URI permission for $uri")
        } catch (e: Exception) {
            Log.e("SafPermissionHandler", "Failed to release persistable URI permission for $uri", e)
        }
    }

    override fun hasPersistedPermission(uriOrPath: String): Boolean {
        return try {
            val persistedUris = context.contentResolver.persistedUriPermissions
            persistedUris.any { perm ->
                val permUri = perm.uri.toString()
                permUri == uriOrPath || uriOrPath.startsWith(permUri) ||
                        (perm.uri.path != null && uriOrPath.contains(perm.uri.path.orEmpty()))
            }
        } catch (e: Exception) {
            false
        }
    }
}

