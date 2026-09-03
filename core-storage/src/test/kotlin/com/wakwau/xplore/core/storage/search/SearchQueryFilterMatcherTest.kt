// [Jalur Class/Modul]: core-storage/src/test/kotlin/com/wakwau/xplore/core/storage/search/SearchQueryFilterMatcherTest.kt
// [Penjelasan]: Unit test untuk SearchQueryFilterMatcher memverifikasi pencocokan kata kunci, ekstensi file, dan batas ukuran file dengan model domain terkini.
package com.wakwau.xplore.core.storage.search

import com.wakwau.xplore.core.storage.model.FileItem
import com.wakwau.xplore.core.storage.model.FileMetadata
import com.wakwau.xplore.core.storage.model.FileType
import com.wakwau.xplore.core.storage.model.StorageLocation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchQueryFilterMatcherTest {

    private val matcher = SearchQueryFilterMatcher()
    private val baseLocation = StorageLocation(path = "/storage/emulated/0")

    @Test
    fun matches_whenQueryEmpty_returnsTrue() {
        val item = FileItem(
            id = "/storage/emulated/0/document.pdf",
            name = "document.pdf",
            location = StorageLocation(path = "/storage/emulated/0/document.pdf"),
            type = FileType.FILE,
            metadata = FileMetadata.EMPTY.copy(size = 1024L, modifiedTime = 1000L)
        )
        val query = FileSearchQuery(location = baseLocation)

        assertTrue(matcher.matches(item, query))
    }

    @Test
    fun matches_byKeyword_success() {
        val item = FileItem(
            id = "/storage/emulated/0/report_2026.pdf",
            name = "report_2026.pdf",
            location = StorageLocation(path = "/storage/emulated/0/report_2026.pdf"),
            type = FileType.FILE,
            metadata = FileMetadata.EMPTY.copy(size = 1024L, modifiedTime = 1000L)
        )
        val matchingQuery = FileSearchQuery(location = baseLocation, keyword = "report")
        val nonMatchingQuery = FileSearchQuery(location = baseLocation, keyword = "invoice")

        assertTrue(matcher.matches(item, matchingQuery))
        assertFalse(matcher.matches(item, nonMatchingQuery))
    }

    @Test
    fun matches_byExtension_success() {
        val item = FileItem(
            id = "/storage/emulated/0/photo.jpg",
            name = "photo.jpg",
            location = StorageLocation(path = "/storage/emulated/0/photo.jpg"),
            type = FileType.FILE,
            metadata = FileMetadata.EMPTY.copy(size = 2048L, modifiedTime = 1000L)
        )
        val jpgQuery = FileSearchQuery(location = baseLocation, extension = "jpg")
        val dotJpgQuery = FileSearchQuery(location = baseLocation, extension = ".jpg")
        val pngQuery = FileSearchQuery(location = baseLocation, extension = "png")

        assertTrue(matcher.matches(item, jpgQuery))
        assertTrue(matcher.matches(item, dotJpgQuery))
        assertFalse(matcher.matches(item, pngQuery))
    }

    @Test
    fun matches_byMinAndMaxSize_success() {
        val item = FileItem(
            id = "/storage/emulated/0/video.mp4",
            name = "video.mp4",
            location = StorageLocation(path = "/storage/emulated/0/video.mp4"),
            type = FileType.FILE,
            metadata = FileMetadata.EMPTY.copy(size = 5000L, modifiedTime = 1000L)
        )
        val validSizeQuery = FileSearchQuery(location = baseLocation, minSize = 1000L, maxSize = 10000L)
        val tooLargeMinSizeQuery = FileSearchQuery(location = baseLocation, minSize = 6000L)
        val tooSmallMaxSizeQuery = FileSearchQuery(location = baseLocation, maxSize = 4000L)

        assertTrue(matcher.matches(item, validSizeQuery))
        assertFalse(matcher.matches(item, tooLargeMinSizeQuery))
        assertFalse(matcher.matches(item, tooSmallMaxSizeQuery))
    }
}
