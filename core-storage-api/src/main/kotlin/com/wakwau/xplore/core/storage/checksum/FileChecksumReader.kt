package com.wakwau.xplore.core.storage.checksum

import com.wakwau.xplore.core.storage.model.FileChecksum
import com.wakwau.xplore.core.storage.model.StorageLocation

// [Jalur Class]: com.wakwau.xplore.core.storage.checksum.FileChecksumReader
// [Penjelasan]: Kontrak interface domain untuk membaca atau menghitung checksum hash kriptografis berkas.
interface FileChecksumReader {
    suspend fun calculateChecksum(location: StorageLocation): FileChecksum
}
