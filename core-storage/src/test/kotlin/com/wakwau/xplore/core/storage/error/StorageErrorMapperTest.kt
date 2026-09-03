// [Jalur Class/Modul]: core-storage/src/test/kotlin/com/wakwau/xplore/core/storage/error/StorageErrorMapperTest.kt
// [Penjelasan]: Unit test untuk StorageErrorMapper yang memvalidasi pemetaan exception IO, Security, dan IllegalArgument.
package com.wakwau.xplore.core.storage.error

import com.wakwau.xplore.core.storage.operation.FileOperationError
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException

class StorageErrorMapperTest {

    private val mapper = StorageErrorMapper()

    @Test
    fun map_fileNotFoundException_returnsNotFound() {
        val e = FileNotFoundException("test")
        assertEquals(FileOperationError.NOT_FOUND, mapper.map(e))
    }

    @Test
    fun map_securityException_returnsAccessDenied() {
        val e = SecurityException("test")
        assertEquals(FileOperationError.ACCESS_DENIED, mapper.map(e))
    }

    @Test
    fun map_illegalArgumentException_returnsInvalidLocation() {
        val e = IllegalArgumentException("Invalid location path")
        assertEquals(FileOperationError.INVALID_LOCATION, mapper.map(e))
    }

    @Test
    fun map_illegalArgumentExceptionWithName_returnsInvalidName() {
        val e = IllegalArgumentException("Invalid directory name: bad/name")
        assertEquals(FileOperationError.INVALID_NAME, mapper.map(e))
    }

    @Test
    fun map_unsupportedOperationException_returnsNotSupported() {
        val e = UnsupportedOperationException("test")
        assertEquals(FileOperationError.NOT_SUPPORTED, mapper.map(e))
    }

    @Test
    fun map_ioExceptionWithEnospc_returnsIoError() {
        val e = IOException("Some error ENOSPC here")
        assertEquals(FileOperationError.IO_ERROR, mapper.map(e))
    }

    @Test
    fun map_ioExceptionWithEacces_returnsAccessDenied() {
        val e = IOException("Some error EACCES here")
        assertEquals(FileOperationError.ACCESS_DENIED, mapper.map(e))
    }

    @Test
    fun map_ioExceptionWithEexist_returnsAlreadyExists() {
        val e = IOException("Some error EEXIST here")
        assertEquals(FileOperationError.ALREADY_EXISTS, mapper.map(e))
    }

    @Test
    fun map_ioExceptionWithAlreadyExists_returnsAlreadyExists() {
        val e = IOException("Directory already exists: newfolder")
        assertEquals(FileOperationError.ALREADY_EXISTS, mapper.map(e))
    }

    @Test
    fun map_ioExceptionWithNotFound_returnsNotFound() {
        val e = IOException("Parent directory not found: /storage/path")
        assertEquals(FileOperationError.NOT_FOUND, mapper.map(e))
    }

    @Test
    fun map_ioExceptionGeneric_returnsIoError() {
        val e = IOException("Some generic message")
        assertEquals(FileOperationError.IO_ERROR, mapper.map(e))
    }

    @Test
    fun map_unknownException_returnsUnknown() {
        val e = RuntimeException("test")
        assertEquals(FileOperationError.UNKNOWN, mapper.map(e))
    }
}
