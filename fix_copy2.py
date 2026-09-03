import sys

file_path = 'core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/local/LocalFileSystem.kt'
with open(file_path, 'r') as f:
    content = f.read()

target_str = """    fun copy(sourcePath: String, destPath: String): Flow<FileOperationProgress> = flow {
        val sourceFile = File(sourcePath)
        val destFile = File(destPath)

        if (!sourceFile.exists()) {
            throw FileNotFoundException("Source not found: $sourcePath")
        }

        if (sourceFile.absolutePath == destFile.absolutePath) {
            throw IllegalArgumentException("Source and destination are the same")
        }

        if (sourceFile.isDirectory && destFile.absolutePath.startsWith(sourceFile.absolutePath + File.separator)) {
            throw IllegalArgumentException("Cannot copy a directory into itself")
        }

        var totalCopied = 0L
        val totalBytes = if (sourceFile.isDirectory) calculateTotalSize(sourceFile) else sourceFile.length()"""

replacement_str = """    fun copy(sourcePath: String, destPath: String): Flow<FileOperationProgress> = flow {
        val sourceFile = File(sourcePath)
        val destFile = File(destPath)

        if (!sourceFile.exists()) {
            throw FileNotFoundException("Source not found: $sourcePath")
        }

        if (sourceFile.absolutePath == destFile.absolutePath) {
            throw IllegalArgumentException("Source and destination are the same")
        }

        if (sourceFile.isDirectory && destFile.absolutePath.startsWith(sourceFile.absolutePath + File.separator)) {
            throw IllegalArgumentException("Cannot copy a directory into itself")
        }

        emit(FileOperationProgress(0L, 0L, sourceFile.name))

        var totalCopied = 0L
        val totalBytes = if (sourceFile.isDirectory) calculateTotalSize(sourceFile) else sourceFile.length()"""

content = content.replace(target_str, replacement_str)

with open(file_path, 'w') as f:
    f.write(content)

