// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/operation/FileCopyServiceLocator.kt
// [Penjelasan]: Object singleton ringan untuk memberikan referensi instance FileRepository dari root DI container ke FileCopyService tanpa mengimpor modul app.
package com.wakwau.xplore.core.storage.operation

import com.wakwau.xplore.core.storage.repository.FileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object FileCopyServiceLocator {
    var fileRepository: FileRepository? = null
    
    private val _progressFlow = MutableSharedFlow<FileOperationResult<FileOperationProgress>>(extraBufferCapacity = 64)
    val progressFlow: SharedFlow<FileOperationResult<FileOperationProgress>> = _progressFlow.asSharedFlow()

    suspend fun emitProgress(result: FileOperationResult<FileOperationProgress>) {
        _progressFlow.emit(result)
    }
}
