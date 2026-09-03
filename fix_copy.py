import sys

file_path = 'core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/local/LocalFileSystem.kt'
with open(file_path, 'r') as f:
    content = f.read()

target_str = """    private suspend fun copySingleFile(
        source: File,
        dest: File,
        totalBytes: Long,
        onProgress: suspend (Long, String) -> Unit
    ) {
        val buffer = ByteArray(StorageConstants.Buffer.DEFAULT_I_O_BUFFER_SIZE_BYTES)
        try {
            FileInputStream(source).use { input ->
                FileOutputStream(dest).use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } >= 0) {
                        if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                            throw CancellationException("Local copy cancelled")
                        }
                        output.write(buffer, 0, bytesRead)
                        onProgress(bytesRead.toLong(), source.name)
                    }
                    output.flush()
                }
            }
            if (dest.length() != source.length()) {
                throw IOException("Partial copy detected: destination size (${dest.length()}) does not match source size (${source.length()})")
            }
        } catch (e: Throwable) {
            try {
                if (dest.exists()) {
                    dest.delete()
                }
            } catch (e: Exception) { android.util.Log.w("FileSystem", "Failed to clean partial file", e) }
            throw e
        }
    }"""

replacement_str = """    private suspend fun copySingleFile(
        source: File,
        dest: File,
        totalBytes: Long,
        onProgress: suspend (Long, String) -> Unit
    ) {
        try {
            FileInputStream(source).use { input ->
                FileOutputStream(dest).use { output ->
                    val inputChannel = input.channel
                    val outputChannel = output.channel
                    val size = inputChannel.size()
                    var position = 0L
                    val chunkSize = 131072L // 128 KB
                    
                    while (position < size) {
                        if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                            throw CancellationException("Local copy cancelled")
                        }
                        val transferred = inputChannel.transferTo(position, chunkSize, outputChannel)
                        if (transferred > 0L) {
                            position += transferred
                            onProgress(transferred, source.name)
                        } else {
                            // Fallback to stream buffer if transferTo stalls
                            val buffer = ByteArray(StorageConstants.Buffer.DEFAULT_I_O_BUFFER_SIZE_BYTES)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } >= 0) {
                                if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                                    throw CancellationException("Local copy cancelled")
                                }
                                output.write(buffer, 0, bytesRead)
                                onProgress(bytesRead.toLong(), source.name)
                            }
                            break
                        }
                    }
                    output.flush()
                }
            }
            if (dest.length() != source.length()) {
                throw IOException("Partial copy detected: destination size (${dest.length()}) does not match source size (${source.length()})")
            }
        } catch (e: Throwable) {
            try {
                if (dest.exists()) {
                    dest.delete()
                }
            } catch (e: Exception) { android.util.Log.w("FileSystem", "Failed to clean partial file", e) }
            throw e
        }
    }"""

content = content.replace(target_str, replacement_str)

with open(file_path, 'w') as f:
    f.write(content)

