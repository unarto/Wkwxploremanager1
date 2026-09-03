// [Jalur Class/Modul]: filemanager/src/test/kotlin/com/wakwau/xplore/filemanager/usecase/LinkStorageUseCaseTest.kt
// [Penjelasan]: Unit test untuk LinkStorageUseCase dengan implementasi mock SafPermissionHandler lengkap.
package com.wakwau.xplore.filemanager.usecase

import com.wakwau.xplore.core.storage.permission.SafPermissionHandler
import org.junit.Test
import org.junit.Assert.*

class LinkStorageUseCaseTest {

    @Test
    fun testAddLinkedStorage() {
        var takenUri: String? = null
        val fakeHandler = object : SafPermissionHandler {
            override fun takePersistableUriPermission(uriString: String) {
                takenUri = uriString
            }
            override fun releasePersistableUriPermission(uriString: String) { }
            override fun hasPersistedPermission(uriOrPath: String): Boolean = true
        }
        val useCase = LinkStorageUseCase(fakeHandler)
        useCase.addLinkedStorage("content://test")
        assertEquals("content://test", takenUri)
    }

    @Test
    fun testRemoveLinkedStorage() {
        var releasedUri: String? = null
        val fakeHandler = object : SafPermissionHandler {
            override fun takePersistableUriPermission(uriString: String) { }
            override fun releasePersistableUriPermission(uriString: String) {
                releasedUri = uriString
            }
            override fun hasPersistedPermission(uriOrPath: String): Boolean = true
        }
        val useCase = LinkStorageUseCase(fakeHandler)
        useCase.removeLinkedStorage("content://test2")
        assertEquals("content://test2", releasedUri)
    }
}
