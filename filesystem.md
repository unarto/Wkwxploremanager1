# Hasil Audit Filesystem Xplore

## 1. Wiring yang Putus & Kode Duplikat (Dead Code)
* **File Terkait:** 
  * `app/src/main/java/com/wakwau/xplore/saf/GetTreeUriActivity.kt`
  * `app/src/main/AndroidManifest.xml`
  * `app/src/main/java/com/wakwau/xplore/XploreRoot.kt`
* **Temuan:** 
  * `GetTreeUriActivity` adalah *dead code* (kode mati). Activity transparan ini didesain sebagai *trampoline* untuk meluncurkan `ACTION_OPEN_DOCUMENT_TREE` dan mengambil *persistable URI permission* secara langsung (mengadopsi kemampuan dari X-plore). Meskipun sudah didaftarkan di `AndroidManifest.xml`, activity ini **TIDAK PERNAH DIPANGGIL**.
  * Sebagai gantinya, UI utama di `XploreRoot.kt` mengimplementasikan peluncurannya sendiri menggunakan Jetpack Compose (`rememberLauncherForActivityResult`).
* **Akar Masalah:** 
  * Terdapat duplikasi logika (antara Activity *trampoline* dan UI Compose). Wiring terputus karena `GetTreeUriActivity` yang seharusnya dipanggil beserta `EXTRA_INITIAL_URI` diabaikan oleh UI Compose.
* **Prioritas Perbaikan:** Tinggi. Hapus salah satu logika, atau integrasikan `GetTreeUriActivity` ke dalam UI untuk mendukung peluncuran awal berbasis URI (sangat krusial untuk eksploitasi SAF pada Android/data).

## 2. Tautkan Penyimpanan Aplikasi Lain yang Tidak Bekerja (Permission/URI Salah)
* **File Terkait:** 
  * `app/src/main/java/com/wakwau/xplore/XploreRoot.kt`
  * `core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/permission/SafPermissionHandlerImpl.kt`
* **Temuan:** 
  * `XploreRoot.kt` hanya memanggil `OpenDocumentTree()` secara polos tanpa intent/flag kustom.
  * Ketika `SafPermissionHandlerImpl` mencoba mengambil hak akses permanen (`takePersistableUriPermission(uri, takeFlags)`), jika Intent pemanggil tidak mendukung atau framework URI menolak, ia hanya akan melakukan catch `Exception` dan mencetak `Log.e` secara diam-diam.
  * Hasilnya: Storage volume baru yang ditambahkan tidak pernah disimpan di `persistedUriPermissions` ContentResolver Android, sehingga tautan penyimpanan tidak bekerja/hilang setelah restart.
* **Akar Masalah:** 
  * Pemanggilan dari Compose (`ActivityResultContracts.OpenDocumentTree()`) mungkin gagal menyimpan persistable rights karena kurangnya penanganan intent khusus, sementara `GetTreeUriActivity` yang dirancang untuk hal itu dibiarkan mati.
* **Prioritas Perbaikan:** Sedang - Tinggi. 

## 3. Routing SAF/Root/Shizuku yang Keliru & Fallback yang Salah
* **File Terkait:** 
  * `core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/StorageBackendClassifier.kt`
  * `core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/shizuku/SafShizukuFileSystem.kt`
* **Temuan:** 
  * Logika klasifikasi di `StorageBackendClassifier` terlalu naif/primitif:
    - Jika dimulai dengan `content://` -> SAF
    - Jika rootId adalah `root_storage` -> SHIZUKU
    - Sisanya -> LOCAL
  * Jika pengguna berada di direktori Root (`/`), aplikasi secara buta mengarahkannya ke `StorageBackendType.SHIZUKU`.
  * **Fallback Salah:** Tidak ada sama sekali fallback ke backend akses *Native Root* (`su` shell / `libsu`). Jika Shizuku tidak aktif atau belum diizinkan, akses Root akan gagal total, berbeda dengan X-plore yang memiliki *graceful fallback* dari API khusus ke *native shell root*.
* **Akar Masalah:** 
  * Tidak adanya implementasi shell root sebagai fallback. Arsitektur memaksa Shizuku sebagai satu-satunya *backend* eksekusi level root.
* **Prioritas Perbaikan:** Sedang. Buat backend Native Root Shell sebagai fallback jika Shizuku gagal.

## 4. Android/data & Android/obb yang Tidak Bisa Dibuka
* **File Terkait:** 
  * `core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/StorageBackendClassifier.kt`
  * `core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/local/LocalFileSystem.kt`
* **Temuan:** 
  * Path untuk `Android/data` dan `Android/obb` (contoh: `/storage/emulated/0/Android/data`) diklasifikasikan sebagai `LOCAL` karena tidak dimulai dengan `content://` dan tidak memiliki ID root.
  * Karena diklasifikasikan sebagai `LOCAL`, operasi pendaftaran dan pembacaan dilempar ke `LocalFileSystem` yang menggunakan Java `File` API standar.
  * Pada Android 11+ (Scoped Storage), `java.io.File` memblokir akses ke dalam `Android/data` dan `Android/obb`. Panggilan `listFiles()` di `LocalFileSystem` secara diam-diam akan mengembalikan *empty array* / *list* kosong.
  * Berbeda dengan X-plore, tidak ada intersep atau peralihan (fallback) paksa ke antarmuka SAF atau Shizuku saat membedah masuk ke dalam path `Android/data`.
* **Akar Masalah:** 
  * Pengklasifikasi storage gagal mendeteksi `Android/data` dan `Android/obb` sebagai path khusus yang membutuhkan bypass SAF/Shizuku, sehingga tetap diurai menggunakan `java.io.File` yang diblokir oleh Scoped Storage OS.
* **Prioritas Perbaikan:** Tinggi (Kritis untuk fungsionalitas File Manager). Tambahkan intersep path di `StorageBackendClassifier` agar rute `Android/data` dialihkan ke `SAF` atau `SHIZUKU`. Mengingat `GetTreeUriActivity` juga menerima `EXTRA_INITIAL_URI`, activity ini harus digunakan untuk meminta akses khusus ke folder tersebut.
