package com.wakwau.xplore.core.storage.metadata

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.wakwau.xplore.core.storage.model.FileDetailedMetadata
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.permission.FilePermissionFormatter
import com.wakwau.xplore.core.util.MimeTypeDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// [Jalur Class]: com.wakwau.xplore.core.storage.metadata.LocalDetailedMetadataReader
// [Penjelasan]: Membaca rincian metadata lengkap berkas dari sistem berkas lokal maupun SAF Content URI seperti path lengkap, ukuran byte, timestamp, izin POSIX, dan MIME type secara aktual.
class LocalDetailedMetadataReader(
    private val permissionFormatter: FilePermissionFormatter = FilePermissionFormatter(),
    private val context: Context? = null
) : DetailedMetadataReader {

    override suspend fun readDetailedMetadata(location: StorageLocation): FileDetailedMetadata = withContext(Dispatchers.IO) {
        if (location.path.startsWith("content://") && context != null) {
            val uri = Uri.parse(location.path)
            val doc = DocumentFile.fromSingleUri(context, uri) ?: DocumentFile.fromTreeUri(context, uri)
            val isDirectory = doc?.isDirectory ?: false
            val name = doc?.name ?: location.path
            val mimeType = if (isDirectory) "inode/directory" else (doc?.type ?: MimeTypeDetector.getMimeType(name))

            FileDetailedMetadata(
                fileName = name,
                fullPath = location.path,
                parentPath = uri.path ?: "",
                sizeBytes = if (doc?.isFile == true) doc.length() else 0L,
                isDirectory = isDirectory,
                lastModifiedTimestamp = doc?.lastModified() ?: 0L,
                isReadable = doc?.canRead() ?: false,
                isWritable = doc?.canWrite() ?: false,
                isExecutable = false,
                isHidden = name.startsWith("."),
                posixPermissions = if (doc?.canWrite() == true) "rw-" else "r--",
                mimeType = mimeType
            )
        } else {
            val file = File(location.path)
            val isDirectory = file.isDirectory
            val mimeType = if (isDirectory) "inode/directory" else MimeTypeDetector.getMimeType(file.name)
            val name = file.name.ifEmpty { location.path }

            FileDetailedMetadata(
                fileName = name,
                fullPath = file.absolutePath,
                parentPath = file.parent ?: "",
                sizeBytes = if (file.isFile) file.length() else 0L,
                isDirectory = isDirectory,
                lastModifiedTimestamp = file.lastModified(),
                isReadable = file.canRead(),
                isWritable = file.canWrite(),
                isExecutable = file.canExecute(),
                isHidden = file.isHidden,
                posixPermissions = permissionFormatter.formatPosixPermissions(file),
                mimeType = mimeType
            )
        }
    }
}
