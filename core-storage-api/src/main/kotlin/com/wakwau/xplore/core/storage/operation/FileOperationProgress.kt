package com.wakwau.xplore.core.storage.operation

// [Jalur Class]: com.wakwau.xplore.core.storage.operation.FileOperationProgress
// [Penjelasan]: Model progress untuk operasi file secara real-time yang mencakup jumlah byte tertulis, total byte, dan nama file yang sedang diproses.
data class FileOperationProgress(
    val bytesWritten: Long,
    val totalBytes: Long,
    val fileName: String
) {
    val percentage: Float
        get() = if (totalBytes > 0) bytesWritten.toFloat() / totalBytes else 0f
}
