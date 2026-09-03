// [Jalur Class/Modul]: core-storage-api/src/main/kotlin/com/wakwau/xplore/core/storage/conflict/ConflictDecision.kt
// [Penjelasan]: Keputusan pilihan resolusi benturan nama berkas/direktori dengan opsi applyToAll untuk menerapkan ke semua benturan berikutnya.
package com.wakwau.xplore.core.storage.conflict

data class ConflictDecision(
    val choice: ConflictChoice,
    val applyToAll: Boolean = false
)
