package com.wakwau.xplore.filemanager.usecase

import com.wakwau.xplore.core.storage.metadata.DetailedMetadataReader
import com.wakwau.xplore.core.storage.model.FileDetailedMetadata
import com.wakwau.xplore.core.storage.model.StorageLocation

// [Jalur Class]: com.wakwau.xplore.filemanager.usecase.GetFileDetailedMetadataUseCase
// [Penjelasan]: Use case domain untuk mengekstrak seluruh rincian metadata berkas yang dipilih secara asinkron.
class GetFileDetailedMetadataUseCase(
    private val metadataReader: DetailedMetadataReader
) {
    suspend fun invoke(location: StorageLocation): FileDetailedMetadata {
        return metadataReader.readDetailedMetadata(location)
    }
}
