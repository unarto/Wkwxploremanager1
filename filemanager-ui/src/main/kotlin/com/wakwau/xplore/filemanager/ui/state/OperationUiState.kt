// [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/state/OperationUiState.kt
// [Penjelasan]: Status UI untuk operasi berkas termasuk konfirmasi awal, dialog resolusi konflik berkas/folder ganda, progres berjalan, sukses, gagal, dan dibatalkan.
package com.wakwau.xplore.filemanager.ui.state

import com.wakwau.xplore.core.storage.conflict.ConflictChoice
import com.wakwau.xplore.core.storage.conflict.FileConflict
import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.StorageLocation
import com.wakwau.xplore.core.storage.operation.FileOperationProgress

sealed class OperationUiState {
    object Idle : OperationUiState()
    data class Confirming(
        val isMove: Boolean,
        val items: List<FileItem>,
        val targetPath: String
    ) : OperationUiState()
    data class ConflictResolution(
        val isMove: Boolean,
        val pendingConflicts: List<FileConflict>,
        val currentConflictIndex: Int = 0,
        val destinationDir: StorageLocation,
        val allSources: List<StorageLocation>,
        val resolvedDecisions: Map<StorageLocation, ConflictChoice> = emptyMap()
    ) : OperationUiState() {
        val currentConflict: FileConflict?
            get() = pendingConflicts.getOrNull(currentConflictIndex)
        val remainingConflictsCount: Int
            get() = (pendingConflicts.size - currentConflictIndex).coerceAtLeast(0)
    }
    data class Running(val operationNameRes: Int, val progress: FileOperationProgress? = null) : OperationUiState()
    data class Success(val messageRes: Int) : OperationUiState()
    data class Failure(val errorMessage: String) : OperationUiState()
    object Cancelled : OperationUiState()
}

