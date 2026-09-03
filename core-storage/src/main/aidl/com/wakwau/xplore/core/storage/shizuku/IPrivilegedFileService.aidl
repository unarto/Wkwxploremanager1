// [Jalur Class]: com.wakwau.xplore.core.storage.shizuku.IPrivilegedFileService
// [Penjelasan]: Antarmuka AIDL untuk IPC dengan Shizuku service, memfasilitasi operasi file privileged seperti membaca root directory.
package com.wakwau.xplore.core.storage.shizuku;

import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import java.util.List;

interface IPrivilegedFileService {
    void destroy() = 16777114; // Wajib untuk Shizuku service (onDestroy)

    List<Bundle> listDirectory(String path) = 1;
    boolean exists(String path) = 2;
    boolean delete(String path) = 3;
    boolean rename(String sourcePath, String destPath) = 4;
    boolean createDirectory(String path) = 5;
    long length(String path) = 6;
    long lastModified(String path) = 7;
    boolean isDirectory(String path) = 8;
    ParcelFileDescriptor openFileForRead(String path) = 9;
    ParcelFileDescriptor openFileForWrite(String path) = 10;
}
