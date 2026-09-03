// [Jalur Class/Modul]: core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/search/SearchQueryFilterMatcher.kt
// [Penjelasan]: Evaluator murni untuk memeriksa apakah FileItem memenuhi kriteria filter pencarian (kata kunci nama, ekstensi, batas ukuran min/max).
package com.wakwau.xplore.core.storage.search

import com.wakwau.xplore.core.storage.model.FileItem
import java.util.Locale

class SearchQueryFilterMatcher {

    fun matches(item: FileItem, query: FileSearchQuery): Boolean {
        val keyword = query.keyword.lowercase(Locale.getDefault())
        val itemNameLower = item.name.lowercase(Locale.getDefault())

        if (keyword.isNotEmpty() && !itemNameLower.contains(keyword)) {
            return false
        }

        val isDir = item.type == com.wakwau.xplore.core.storage.model.FileType.DIRECTORY
        when (query.searchType) {
            SearchTargetType.FILE -> if (isDir) return false
            SearchTargetType.FOLDER -> if (!isDir) return false
            SearchTargetType.ALL -> { /* allow both */ }
        }

        // khusus Zip belum di implementasikan
        // if (query.searchInArchives) { ... }

        val ext = query.extension
        if (ext != null) {
            val extLower = ext.lowercase(Locale.getDefault())
            val suffix = if (extLower.startsWith(".")) extLower else ".$extLower"
            if (!itemNameLower.endsWith(suffix)) return false
        }

        val minS = query.minSize
        if (minS != null && item.metadata.size < minS) {
            return false
        }

        val maxS = query.maxSize
        if (maxS != null && item.metadata.size > maxS) {
            return false
        }

        if (!query.showHidden && item.metadata.isHidden) {
            return false
        }

        return true
    }
}
