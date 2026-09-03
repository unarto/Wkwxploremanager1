import re

with open('core-storage-api/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/StorageBackendClassifier.kt', 'r') as f:
    content = f.read()

replacement = """
    private fun isRootSystemPath(path: String): Boolean {
        val clean = path.trim()
        if (clean == "/" || clean == StorageConstants.ROOT_PATH) return true
        
        val tmpDir = System.getProperty("java.io.tmpdir") ?: "/tmp"
        
        // Cek jika path absolut (dimulai dengan '/')
        if (clean.startsWith("/")) {
            val isStandardStorage = clean.startsWith("/storage") || 
                                    clean.startsWith("/sdcard") || 
                                    clean.startsWith("/mnt") ||
                                    clean.startsWith(tmpDir)
            return !isStandardStorage
        }
        return false
    }
"""

content = re.sub(r'\s+private fun isRootSystemPath\(path: String\): Boolean \{.*?\n\s+\}', replacement, content, flags=re.DOTALL)

with open('core-storage-api/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/StorageBackendClassifier.kt', 'w') as f:
    f.write(content)
