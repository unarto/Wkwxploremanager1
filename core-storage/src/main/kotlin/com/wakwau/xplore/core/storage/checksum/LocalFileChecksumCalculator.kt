package com.wakwau.xplore.core.storage.checksum

import android.content.Context
import android.net.Uri
import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.model.FileChecksum
import com.wakwau.xplore.core.storage.model.StorageLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

// [Jalur Class]: com.wakwau.xplore.core.storage.checksum.LocalFileChecksumCalculator
// [Penjelasan]: Menghitung hash kriptografis nyata (MD5, SHA-1, SHA-256) langsung dari berkas lokal atau SAF content URI menggunakan MessageDigest dan stream I/O nyata.
class LocalFileChecksumCalculator(
    private val context: Context? = null
) : FileChecksumReader {

    override suspend fun calculateChecksum(location: StorageLocation): FileChecksum = withContext(Dispatchers.IO) {
        val md5Digest = MessageDigest.getInstance(StorageConstants.ChecksumAlgorithm.MD5)
        val sha1Digest = MessageDigest.getInstance(StorageConstants.ChecksumAlgorithm.SHA1)
        val sha256Digest = MessageDigest.getInstance(StorageConstants.ChecksumAlgorithm.SHA256)

        val buffer = ByteArray(StorageConstants.Buffer.DEFAULT_I_O_BUFFER_SIZE_BYTES)

        val stream: InputStream = if (location.path.startsWith("content://")) {
            val ctx = context ?: throw IllegalArgumentException("Context required for SAF stream")
            ctx.contentResolver.openInputStream(Uri.parse(location.path))
                ?: throw IllegalArgumentException("Cannot open stream: ${location.path}")
        } else {
            val file = File(location.path)
            if (!file.exists()) {
                throw IllegalArgumentException("Berkas tidak ditemukan: ${location.path}")
            }
            if (!file.isFile) {
                throw IllegalArgumentException("Target bukan berkas biasa: ${location.path}")
            }
            FileInputStream(file)
        }

        stream.use { fis ->
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                md5Digest.update(buffer, 0, bytesRead)
                sha1Digest.update(buffer, 0, bytesRead)
                sha256Digest.update(buffer, 0, bytesRead)
            }
        }

        FileChecksum(
            md5 = md5Digest.digest().toHex(),
            sha1 = sha1Digest.digest().toHex(),
            sha256 = sha256Digest.digest().toHex()
        )
    }

    private fun ByteArray.toHex(): String {
        val hexChars = StorageConstants.ChecksumAlgorithm.HEX_CHARS
        val result = StringBuilder(size * 2)
        for (b in this) {
            val i = b.toInt() and 0xFF
            result.append(hexChars[i ushr 4])
            result.append(hexChars[i and 0x0F])
        }
        return result.toString()
    }
}
