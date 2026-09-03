# Global Source Code Audit - Bug & Issue Report

## 1. CRITICAL: Potensi Memory Leak & Background Resource Exhaustion pada Compose UI
- **Severity**: CRITICAL
- **File + Lokasi**: `MainActivity.kt`, `XploreRoot.kt`, `FileManagerScreen.kt`, `FileManagerContent.kt`, `DirectoryTreeView.kt`
- **Masalah**: Penggunaan `collectAsState()` alih-alih `collectAsStateWithLifecycle()` untuk mengobservasi StateFlow dari ViewModel di dalam komponen Compose.
- **Dampak**: Flow akan terus mengumpulkan event dan data (termasuk list files, background file operation progress, polling state) meskipun aplikasi berada di background (layar mati / user pindah aplikasi). Ini menyebabkan pemborosan baterai ekstrim, resource exhaustion, serta memory leak akibat UI me-recompose di background.
- **Penyebab**: Kesalahan penggunaan API observasi Flow pada framework Jetpack Compose modern.
- **Rekomendasi Perbaikan**: Ganti semua pemanggilan `collectAsState()` dengan `collectAsStateWithLifecycle()` (dari dependensi `androidx.lifecycle:lifecycle-runtime-compose`).

## 2. HIGH: Mass File Operation / Thrashing pada Room DB Indexer
- **Severity**: HIGH
- **File + Lokasi**: `DirectoryRepositoryImpl.kt` (fungsi `create`), `FileRepositoryImpl.kt` (fungsi `rename`, `copy`, `move`, `delete`)
- **Masalah**: Pemanggilan `fileIndexSynchronizer?.syncSingle(...)` dan `removeSingle(...)` dipanggil satu per satu setiap kali sebuah file selesai di-copy/move/delete. 
- **Dampak**: Jika user menyalin atau menghapus 10.000 file, akan ada 10.000 transaksi DB terpisah yang dieksekusi secara berurutan. Ini akan membekukan (ANR) background queue, menyebabkan overhead tinggi, dan menurunkan kecepatan file manager secara signifikan.
- **Penyebab**: Sinkronisasi indeks lokal ke Room DB tidak di-batch (bulk update) untuk operasi jamak.
- **Rekomendasi Perbaikan**: Gunakan buffer/batching saat mengirim status ke `FileIndexSynchronizer` atau buat fungsi khusus `syncMultiple(items)` dengan anotasi `@Transaction` di Room DAO.

## 3. HIGH: Silent Partial Failures (Race Condition di Transfer Filesystem)
- **Severity**: HIGH
- **File + Lokasi**: `CrossFilesystemTransferBridge.kt` (Logika rekursif delete/copy file jamak)
- **Masalah**: Jika suatu folder gagal di-copy/move di pertengahan proses karena permission / kapasitas penuh, proses tidak meng-rollback atau memberitahu resume-state.
- **Dampak**: File akan terpotong (corrupt) atau menjadi file yatim (orphaned) jika operasi `Move` dibatalkan (Cancel) saat proses berlangsung.
- **Penyebab**: State flow tidak menahan status atomic transactional. File terlanjur dihapus dari `source` saat `move` dijalankan, namun destinasinya corrupt/belum lengkap.
- **Rekomendasi Perbaikan**: Terapkan logika commit/rollback: Copy semua file ke temp/destination. Jika sukses semua, baru panggil *batch delete* di sumber.

## 4. MEDIUM: Deprecated Material Icons & RTL Incompatibility
- **Severity**: MEDIUM
- **File + Lokasi**: `SettingsTreeScreen.kt` (baris 112), `BottomBar.kt`
- **Masalah**: Menggunakan hardcoded icon `Icons.Filled.ArrowBack` dan `Icons.Filled.InsertDriveFile`.
- **Dampak**: Panah tidak akan membalik secara otomatis saat aplikasi digunakan pada perangkat dengan bahasa Right-To-Left (seperti Arab/Ibrani).
- **Penyebab**: Penggunaan API Material Icons lama yang sudah deprecated di versi Jetpack Compose terbaru.
- **Rekomendasi Perbaikan**: Gunakan `Icons.AutoMirrored.Filled.ArrowBack` dan `InsertDriveFile`.

## 5. MEDIUM: Crash Runtime pada Injeksi Ketergantungan (Nullability)
- **Severity**: MEDIUM
- **File + Lokasi**: `FileRepositoryImpl.kt` dan `DirectoryRepositoryImpl.kt`
- **Masalah**: Provider (Root, Shizuku, SAF) didefinisikan sebagai parameter _nullable_ (`? = null`) di konstruktor repository. Namun, jika ada panggilan routing oleh `StorageBackendClassifier` namun dependensi null, ia akan melempar `SecurityException` di runtime.
- **Dampak**: Crash spontan saat navigasi file system jika injeksi DI dari `StorageModule` suatu hari terputus atau tidak menginisialisasi parameter.
- **Penyebab**: Penggunaan parameter nullable tanpa jaminan compile-time safety.
- **Rekomendasi Perbaikan**: Buat parameter konstruktor _non-null_ (wajib disediakan) oleh DI, atau gunakan `sealed class` StorageBackend untuk memastikan semua jalur tersedia.
