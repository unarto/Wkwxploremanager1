package com.wakwau.xplore.core.storage.metadata

import com.wakwau.xplore.core.storage.model.FileDetailedMetadata
import com.wakwau.xplore.core.storage.model.StorageLocation

// [Jalur Class]: com.wakwau.xplore.core.storage.metadata.DetailedMetadataReader
// [Penjelasan]: Kontrak interface domain untuk mengekstrak informasi metadata berkas secara lengkap dan mendalam.
interface DetailedMetadataReader {
    suspend fun readDetailedMetadata(location: StorageLocation): FileDetailedMetadata
}
