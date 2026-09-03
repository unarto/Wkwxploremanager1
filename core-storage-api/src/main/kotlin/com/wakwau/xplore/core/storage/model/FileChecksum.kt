package com.wakwau.xplore.core.storage.model

// [Jalur Class]: com.wakwau.xplore.core.storage.model.FileChecksum
// [Penjelasan]: Model data immutable untuk menyimpan hash kriptografis berkas (MD5, SHA-1, SHA-256).
data class FileChecksum(
    val md5: String,
    val sha1: String,
    val sha256: String
)
