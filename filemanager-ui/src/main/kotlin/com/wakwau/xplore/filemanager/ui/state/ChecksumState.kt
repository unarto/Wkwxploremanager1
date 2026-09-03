package com.wakwau.xplore.filemanager.ui.state

import com.wakwau.xplore.core.storage.model.FileChecksum

// [Jalur Class]: com.wakwau.xplore.filemanager.ui.state.ChecksumState
// [Penjelasan]: Status UI tersegel untuk melacak tahapan kalkulasi hash checksum berkas (Idle, Calculating, Success, Error).
sealed interface ChecksumState {
    object Idle : ChecksumState
    object Calculating : ChecksumState
    data class Success(val checksum: FileChecksum) : ChecksumState
    data class Error(val errorRes: Int? = null, val message: String? = null) : ChecksumState
}
