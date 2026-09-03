package com.wakwau.xplore.core.storage.filesystem

import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.storage.model.StorageLocation
import org.junit.Assert.assertEquals
import org.junit.Test

class StorageBackendClassifierTest {

    @Test
    fun classify_contentScheme_returnsSaf() {
        val classifier = StorageBackendClassifier(
            isSuAvailable = { false },
            isShizukuAvailable = { false },
            isSafPersisted = { false }
        )
        val location = StorageLocation("content://com.android.externalstorage.documents/tree/primary%3ADownload")
        assertEquals(StorageBackendType.SAF, classifier.classify(location))
    }

    @Test
    fun classify_rootStorageId_withSuAvailable_returnsRoot() {
        val classifier = StorageBackendClassifier(
            isSuAvailable = { true },
            isShizukuAvailable = { false },
            isSafPersisted = { false }
        )
        val location = StorageLocation("/", rootId = StorageConstants.ROOT_STORAGE_ID)
        assertEquals(StorageBackendType.ROOT, classifier.classify(location))
    }

    @Test
    fun classify_rootStorageId_withOnlyShizukuAvailable_returnsShizuku() {
        val classifier = StorageBackendClassifier(
            isSuAvailable = { false },
            isShizukuAvailable = { true },
            isSafPersisted = { false }
        )
        val location = StorageLocation("/", rootId = StorageConstants.ROOT_STORAGE_ID)
        assertEquals(StorageBackendType.SHIZUKU, classifier.classify(location))
    }

    @Test
    fun classify_explicitRootId_withSuAndShizukuUnavailable_returnsLocal() {
        val classifier = StorageBackendClassifier(
            isSuAvailable = { false },
            isShizukuAvailable = { false },
            isSafPersisted = { false }
        )
        val location = StorageLocation("/some/path", rootId = StorageConstants.ROOT_STORAGE_ID)
        assertEquals(StorageBackendType.LOCAL, classifier.classify(location))
    }

    @Test
    fun classify_rootPath_withSuAvailable_returnsRoot() {
        val classifier = StorageBackendClassifier(
            isSuAvailable = { true },
            isShizukuAvailable = { false },
            isSafPersisted = { false }
        )
        val location = StorageLocation("/")
        assertEquals(StorageBackendType.ROOT, classifier.classify(location))
    }

    @Test
    fun classify_rootPath_withSuAndShizukuUnavailable_returnsLocal() {
        val classifier = StorageBackendClassifier(
            isSuAvailable = { false },
            isShizukuAvailable = { false },
            isSafPersisted = { false }
        )
        val location = StorageLocation("/")
        assertEquals(StorageBackendType.LOCAL, classifier.classify(location))
    }

    @Test
    fun classify_androidDataPath_withSuAvailable_returnsRoot() {
        val classifier = StorageBackendClassifier(
            isSuAvailable = { true },
            isShizukuAvailable = { false },
            isSafPersisted = { false }
        )
        val location = StorageLocation("/storage/emulated/0/Android/data/com.example")
        assertEquals(StorageBackendType.ROOT, classifier.classify(location))
    }

    @Test
    fun classify_androidDataPath_withShizukuAvailable_returnsShizuku() {
        val classifier = StorageBackendClassifier(
            isSuAvailable = { false },
            isShizukuAvailable = { true },
            isSafPersisted = { false }
        )
        val location = StorageLocation("/storage/emulated/0/Android/data/com.example")
        assertEquals(StorageBackendType.SHIZUKU, classifier.classify(location))
    }

    @Test
    fun classify_androidDataPath_withSafPersisted_returnsSaf() {
        val targetPath = "/storage/emulated/0/Android/data"
        val classifier = StorageBackendClassifier(
            isSuAvailable = { false },
            isShizukuAvailable = { false },
            isSafPersisted = { it == targetPath }
        )
        val location = StorageLocation(targetPath)
        assertEquals(StorageBackendType.SAF, classifier.classify(location))
    }

    @Test
    fun classify_androidObbPath_withSafPersisted_returnsSaf() {
        val targetPath = "/storage/emulated/0/Android/obb"
        val classifier = StorageBackendClassifier(
            isSuAvailable = { false },
            isShizukuAvailable = { false },
            isSafPersisted = { it == targetPath }
        )
        val location = StorageLocation(targetPath)
        assertEquals(StorageBackendType.SAF, classifier.classify(location))
    }

    @Test
    fun classify_androidDataPath_withNoSpecialAccess_returnsLocal() {
        val classifier = StorageBackendClassifier(
            isSuAvailable = { false },
            isShizukuAvailable = { false },
            isSafPersisted = { false }
        )
        val location = StorageLocation("/storage/emulated/0/Android/data/com.example")
        assertEquals(StorageBackendType.LOCAL, classifier.classify(location))
    }

    @Test
    fun classify_androidDataBackupPath_isNotTreatedAsAndroidData() {
        val classifier = StorageBackendClassifier(
            isSuAvailable = { true },
            isShizukuAvailable = { true },
            isSafPersisted = { false }
        )
        val location = StorageLocation("/storage/emulated/0/Android/dataBackup")
        assertEquals(StorageBackendType.LOCAL, classifier.classify(location))
    }

    @Test
    fun classify_standardLocalPath_returnsLocal() {
        val classifier = StorageBackendClassifier(
            isSuAvailable = { true },
            isShizukuAvailable = { true },
            isSafPersisted = { false }
        )
        val location = StorageLocation("/storage/emulated/0/Download")
        assertEquals(StorageBackendType.LOCAL, classifier.classify(location))
    }

    @Test
    fun classify_allBackendsUnavailable_returnsLocal() {
        val classifier = StorageBackendClassifier(
            isSuAvailable = { false },
            isShizukuAvailable = { false },
            isSafPersisted = { false }
        )
        val location = StorageLocation("/data/data/com.example/files")
        assertEquals(StorageBackendType.LOCAL, classifier.classify(location))
    }
}
