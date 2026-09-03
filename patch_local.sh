cat << 'INNER_EOF' > replacement.kt
    private suspend fun deleteDirectoryRecursivelySafe(dir: File) {
        val stack = ArrayDeque<File>()
        stack.addLast(dir)
        val filesToDelete = ArrayDeque<File>()

        while (stack.isNotEmpty()) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            val current = stack.removeLast()
            filesToDelete.addFirst(current)

            val isSymlink = java.nio.file.Files.isSymbolicLink(current.toPath())
            if (!isSymlink && current.isDirectory) {
                val children = current.listFiles() ?: continue
                for (child in children) {
                    stack.addLast(child)
                }
            }
        }

        for (file in filesToDelete) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            if (!file.delete() && file.exists()) {
                throw IOException("Failed to delete: ${file.absolutePath}")
            }
        }
    }
INNER_EOF
awk '/private suspend fun deleteDirectoryRecursivelySafe/{flag=1; print; next} /isRootOrProtectedPath/{flag=0} flag{next} {print}' core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/local/LocalFileSystem.kt > temp.kt
# We need a proper awk replacement. Let's do it safely.
