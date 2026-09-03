// [Jalur Class]: com.wakwau.xplore.filemanager.ui.action.FileDetailHandler
// [Penjelasan]: Handler dengan tanggung jawab tunggal untuk mengoordinasikan pembacaan rincian metadata berkas dan eksekusi komputasi checksum ke use case secara asinkron.
package com.wakwau.xplore.filemanager.ui.action

import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.filemanager.ui.event.DualPaneEvent
import com.wakwau.xplore.filemanager.usecase.ComputeFileChecksumUseCase
import com.wakwau.xplore.filemanager.usecase.GetFileDetailedMetadataUseCase
import com.wakwau.xplore.filemanager.ui.R
import kotlinx.coroutines.CancellationException

class FileDetailHandler(
    private val getFileDetailedMetadataUseCase: GetFileDetailedMetadataUseCase,
    private val computeFileChecksumUseCase: ComputeFileChecksumUseCase,
    private val dispatch: (DualPaneEvent) -> Unit
) {

    suspend fun loadDetails(item: FileItem) {
        dispatch(DualPaneEvent.FileDetailsLoadingStarted)
        try {
            val metadata = getFileDetailedMetadataUseCase.invoke(item.location)
            dispatch(DualPaneEvent.FileDetailsLoaded(metadata))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            dispatch(DualPaneEvent.FileDetailsFailed(R.string.err_load_file_details, e.message))
        }
    }

    suspend fun computeChecksum(item: FileItem) {
        dispatch(DualPaneEvent.ChecksumCalculationStarted)
        try {
            val checksum = computeFileChecksumUseCase.invoke(item.location)
            dispatch(DualPaneEvent.ChecksumCalculated(checksum))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            dispatch(DualPaneEvent.ChecksumCalculationFailed(R.string.err_calculate_checksum, e.message))
        }
    }
}
