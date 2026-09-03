import sys

file_path = 'core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/local/LocalFileSystem.kt'
with open(file_path, 'r') as f:
    content = f.read()

target_str = """    fun move(sourcePath: String, destPath: String): Flow<FileOperationProgress> = flow {
        val sourceFile = File(sourcePath)
        val destFile = File(destPath)

        if (!sourceFile.exists()) {
            throw FileNotFoundException("Source not found: $sourcePath")
        }

        val sourceCanonical = sourceFile.canonicalPath
        val destCanonical = destFile.canonicalPath

        if (sourceCanonical == destCanonical) {
            throw IOException("Source and destination are the same")
        }

        if (sourceFile.isDirectory && destCanonical.startsWith(sourceCanonical + File.separator)) {
            throw IOException("Cannot move a directory into itself")
        }

        val sourceLength = if (sourceFile.isFile) sourceFile.length() else 0L
        val isSourceDir = sourceFile.isDirectory"""

replacement_str = """    fun move(sourcePath: String, destPath: String): Flow<FileOperationProgress> = flow {
        val sourceFile = File(sourcePath)
        val destFile = File(destPath)

        if (!sourceFile.exists()) {
            throw FileNotFoundException("Source not found: $sourcePath")
        }

        val sourceCanonical = sourceFile.canonicalPath
        val destCanonical = destFile.canonicalPath

        if (sourceCanonical == destCanonical) {
            throw IOException("Source and destination are the same")
        }

        if (sourceFile.isDirectory && destCanonical.startsWith(sourceCanonical + File.separator)) {
            throw IOException("Cannot move a directory into itself")
        }

        emit(FileOperationProgress(0L, 0L, sourceFile.name))

        val sourceLength = if (sourceFile.isFile) sourceFile.length() else calculateTotalSize(sourceFile)
        val isSourceDir = sourceFile.isDirectory"""

content = content.replace(target_str, replacement_str)

with open(file_path, 'w') as f:
    f.write(content)

