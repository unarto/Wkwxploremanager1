import java.io.File
import java.nio.file.Files

fun deleteDirectoryIteratively(dir: File) {
    if (!dir.exists()) return
    val stack = ArrayDeque<File>()
    stack.addLast(dir)
    val filesToDelete = ArrayDeque<File>()
    
    while (stack.isNotEmpty()) {
        val current = stack.removeLast()
        filesToDelete.addFirst(current) // So we process children before parent
        val children = current.listFiles() ?: continue
        for (child in children) {
            val isSymlink = Files.isSymbolicLink(child.toPath())
            if (isSymlink) {
                filesToDelete.addFirst(child)
            } else if (child.isDirectory) {
                stack.addLast(child)
            } else {
                filesToDelete.addFirst(child)
            }
        }
    }
    
    for (file in filesToDelete) {
        if (!file.delete() && file.exists()) {
            throw java.io.IOException("Failed to delete ${file.absolutePath}")
        }
    }
}
