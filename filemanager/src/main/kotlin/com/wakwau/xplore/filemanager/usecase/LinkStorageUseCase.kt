// [Jalur Class]: com.wakwau.xplore.filemanager.usecase.LinkStorageUseCase
// [Penjelasan]: Use case untuk menautkan dan menghapus tautan penyimpanan (SAF) yang dimiliki aplikasi lain.

package com.wakwau.xplore.filemanager.usecase

import com.wakwau.xplore.core.storage.permission.SafPermissionHandler

class LinkStorageUseCase(private val safPermissionHandler: SafPermissionHandler) {
    fun addLinkedStorage(uriString: String) {
        safPermissionHandler.takePersistableUriPermission(uriString)
    }

    fun removeLinkedStorage(uriString: String) {
        safPermissionHandler.releasePersistableUriPermission(uriString)
    }
}
