// [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/detail/AppIntentResolver.kt
// [Penjelasan]: Utilitas penyelesai intent Android di modul filemanager-ui untuk mendeteksi dan membuka daftar aplikasi kompatibel di perangkat bagi berkas atau tipe MIME tertentu secara aman dan kompatibel dengan Android 11+ (API 30+) serta Android 13+ (API 33+).
package com.wakwau.xplore.filemanager.ui.detail

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.wakwau.xplore.core.util.MimeTypeDetector
import java.io.File

data class CompatibleAppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: Drawable?,
    val isDefault: Boolean = false
)

object AppIntentResolver {

    // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/detail/AppIntentResolver.kt
    // [Penjelasan]: Mendapatkan content URI aman melalui FileProvider atau URI parse dengan penanganan exception jika berkas belum ada di disk atau bukan jalur file lokal.
    private fun getSafeUri(context: Context, filePath: String): Uri? {
        return try {
            if (filePath.startsWith("content://")) {
                filePath.toUri()
            } else {
                val file = File(filePath)
                if (file.exists()) {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                } else {
                    null
                }
            }
        } catch (_: Throwable) {
            null
        }
    }

    // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/detail/AppIntentResolver.kt
    // [Penjelasan]: Mencari daftar aplikasi yang kompatibel dengan intent ACTION_VIEW menggunakan PackageManager dengan dukungan flag API 33+ dan fallback match-all query.
    fun queryCompatibleApps(context: Context, filePath: String, mimeType: String): List<CompatibleAppInfo> {
        val packageManager = context.packageManager
        val fallbackName = if (filePath.startsWith("content://")) "" else File(filePath).name
        val effectiveMimeType = if (mimeType.isNotBlank()) mimeType else MimeTypeDetector.getMimeType(fallbackName)
        val safeUri = getSafeUri(context, filePath)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            if (safeUri != null) {
                setDataAndType(safeUri, effectiveMimeType)
            } else {
                type = effectiveMimeType
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val resolveInfoList: List<ResolveInfo> = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val defaultFlags = PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                val list = packageManager.queryIntentActivities(intent, defaultFlags)
                if (list.isEmpty()) {
                    val allFlags = PackageManager.ResolveInfoFlags.of(0L)
                    packageManager.queryIntentActivities(intent, allFlags)
                } else {
                    list
                }
            } else {
                @Suppress("DEPRECATION")
                val list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                if (list.isEmpty()) {
                    @Suppress("DEPRECATION")
                    packageManager.queryIntentActivities(intent, 0)
                } else {
                    list
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }

        val resultList = mutableListOf<CompatibleAppInfo>()
        for (resolveInfo in resolveInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            val activityName = resolveInfo.activityInfo.name
            val label = resolveInfo.loadLabel(packageManager).toString()
            val icon = resolveInfo.loadIcon(packageManager)

            resultList.add(
                CompatibleAppInfo(
                    packageName = packageName,
                    activityName = activityName,
                    label = label,
                    icon = icon,
                    isDefault = false
                )
            )
        }

        return resultList.distinctBy { it.packageName }
    }

    // [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/detail/AppIntentResolver.kt
    // [Penjelasan]: Membuka berkas dengan aplikasi pilihan menggunakan intent ACTION_VIEW terarah ke komponen spesifik dengan fallback intent broadcast.
    fun openWithApp(context: Context, filePath: String, mimeType: String, packageName: String, activityName: String) {
        val fallbackName = if (filePath.startsWith("content://")) "" else File(filePath).name
        val effectiveMimeType = if (mimeType.isNotBlank()) mimeType else MimeTypeDetector.getMimeType(fallbackName)
        val safeUri = getSafeUri(context, filePath)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            if (safeUri != null) {
                setDataAndType(safeUri, effectiveMimeType)
            } else {
                type = effectiveMimeType
            }
            setClassName(packageName, activityName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(intent)
        } catch (_: Throwable) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                if (safeUri != null) {
                    setDataAndType(safeUri, effectiveMimeType)
                } else {
                    type = effectiveMimeType
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                context.startActivity(fallbackIntent)
            } catch (_: Throwable) {}
        }
    }
}
