package com.wakwau.xplore.filemanager.usecase

import com.wakwau.xplore.core.storage.checksum.FileChecksumReader
import com.wakwau.xplore.core.storage.model.FileChecksum
import com.wakwau.xplore.core.storage.model.StorageLocation

// [Jalur Class]: com.wakwau.xplore.filemanager.usecase.ComputeFileChecksumUseCase
// [Penjelasan]: Use case domain untuk menghitung hash checksum kriptografis (MD5, SHA-1, SHA-256) dari berkas yang dipilih.
class ComputeFileChecksumUseCase(
    private val checksumReader: FileChecksumReader
) {
    suspend fun invoke(location: StorageLocation): FileChecksum {
        return checksumReader.calculateChecksum(location)
    }
}
