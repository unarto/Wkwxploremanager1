// [Jalur Class/Modul]: core-storage-api/src/main/kotlin/com/wakwau/xplore/core/storage/conflict/ConflictChoice.kt
// [Penjelasan]: Pilihan strategi penyelesaian benturan nama berkas atau direktori saat operasi salin/pindah.
package com.wakwau.xplore.core.storage.conflict

enum class ConflictChoice {
    SKIP,
    OVERWRITE,
    RENAME
}
