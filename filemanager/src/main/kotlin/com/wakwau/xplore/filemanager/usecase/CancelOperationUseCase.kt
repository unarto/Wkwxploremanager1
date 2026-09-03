// [Jalur Class/Modul]: filemanager/src/main/kotlin/com/wakwau/xplore/filemanager/usecase/CancelOperationUseCase.kt
// [Penjelasan]: UseCase untuk menghentikan operasi berkas yang sedang berjalan di background service secara aman.
package com.wakwau.xplore.filemanager.usecase

import com.wakwau.xplore.core.storage.operation.BackgroundOperationManager

class CancelOperationUseCase(
    private val backgroundOperationManager: BackgroundOperationManager
) {
    fun invoke() {
        backgroundOperationManager.cancelOperation()
    }
}
