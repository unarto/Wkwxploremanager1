// [Jalur Class/Modul]: core-storage-api/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/StorageBackendType.kt
// [Penjelasan]: Enum tipe backend penyimpanan (LOCAL, SAF, SHIZUKU, ROOT) untuk klasifikasi driver filesystem secara murni.

package com.wakwau.xplore.core.storage.filesystem

enum class StorageBackendType {
    LOCAL,
    SAF,
    SHIZUKU,
    ROOT
}
