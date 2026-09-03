fun getPhysicalPath(path: String): String {
    return path.replaceFirst("^/storage/emulated/([0-9]+)".toRegex(), "/data/media/$1")
}
