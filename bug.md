# Laporan Audit Kode Sumber (WKW Xplore)

**Auditor:** Senior Android Developer & Code Auditor  
**Tanggal:** 2 September 2026  
**Status Proyek:** Build PASS, Unit Test PASS (141 actionable tasks, 0 failing tests)  
**Tujuan:** Audit menyeluruh terhadap seluruh modul dan arsitektur tanpa mengubah kode, UI, atau behavior aplikasi.

---

## Ringkasan Eksekutif Hasil Audit

Audit mendalam telah dilakukan terhadap seluruh modul proyek:
1. `:app` (Aplikasi utama, DI Container `AppCompositionRoot`, Navigasi, Lifecycle)
2. `:core-storage-api` (Kontrak filesystem, enum, model, conflict resolution)
3. `:core-storage` (Implementasi Local, SAF, Root, Shizuku, Cross-Bridge, Room DB, MMKV, Indexing)
4. `:core-worker` (Jetpack WorkManager, background sync, notifikasi progress)
5. `:core` (Utilitas format byte, tanggal, deteksi MIME)
6. `:core-ui` (Tema M3, dialog dasar, komponen UI, permission state)
7. `:filemanager` (Domain Use Cases & Factory)
8. `:filemanager-ui` (Presentation MVI, Reducer, Dual Pane ViewModel, Handlers, Tree Navigation)
9. `:treeview` (Komponen TreeView, State, Algoritma Hirarki)
10. Konfigurasi Proyek & Direktori Root (`build.gradle.kts`, `settings.gradle.kts`, `XFiles-1.3.1`)

Total temuan: **20 temuan** yang dikelompokkan ke dalam tingkat keparahan:
- **CRITICAL**: 1 temuan
- **HIGH**: 4 temuan
- **MEDIUM**: 8 temuan
- **LOW**: 7 temuan

---

## Daftar Temuan Lengkap

---

### Temuan 1 (CRITICAL): Search Results Overwriting & Dialog Premature Dismissal
- **Lokasi File + Class/Method:**  
  - `core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/search/FileSystemSearchTraversal.kt` (`traverse`)  
  - `filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/action/SearchOperationHandler.kt` (`execute`)  
  - `filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/reducer/DualPaneReducer.kt` (`DualPaneEvent.SearchResultsUpdated`)  
  - `filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/presentation/DualPaneViewModel.kt` (`DualPaneEvent.SearchResultsUpdated`)
- **Masalah:**  
  `FileSystemSearchTraversal` memancarkan hasil pencarian per batch berukuran 10 (`EMIT_BATCH_SIZE = 10`). Ketika batch pertama tiba, `SearchOperationHandler` mengirim event `SearchResultsUpdated(query.keyword, results)`.  
  Pada `DualPaneReducer`:
  ```kotlin
  is DualPaneEvent.SearchResultsUpdated -> {
      state.copy(
          searchUiState = state.searchUiState.copy(
              isSearching = false,
              results = event.results, // Mereplace total, tidak mengakumulasi!
              hasSearched = true
          )
      )
  }
  ```
  Dan pada `DualPaneViewModel`:
  ```kotlin
  is DualPaneEvent.SearchResultsUpdated -> {
      treeNavigationAdapter.updateSearchResults(stateSnapshot.activePanelId, event.keyword, event.results)
      dispatch(DualPaneEvent.DismissSearchDialog) // Menutup dialog seketika pada batch ke-1!
  }
  ```
  Ketika batch ke-2, ke-3, dst. tiba:
  1. `treeNavigationAdapter.updateSearchResults` menimpa root node pencarian dengan batch baru (bukan akumulasi).
  2. Dialog pencarian telah tertutup pada batch pertama, dan `DismissSearchDialog` di `DualPaneReducer` mereset `results = emptyList()`.
- **Dampak:**  
  Hasil pencarian terpotong (hanya 10 item dari batch terakhir yang tersisa), data pencarian sebelumnya hilang, dan pengguna mengalami penutupan dialog pencarian secara tiba-tiba di tengah proses traversal.
- **Tingkat Severity:** CRITICAL
- **Rekomendasi Perbaikan:**  
  - Akumulasikan hasil pencarian di state (`state.searchUiState.results + event.results`) atau emisi hasil secara kumulatif dari traversal.
  - Jangan mendispatch `DismissSearchDialog` pada setiap emisi batch; pisahkan event antara `SearchResultsBatchEmitted` dan `SearchCompleted`. Dialog hanya ditutup atau status `isSearching = false` diset saat traversal benar-benar selesai.

---

### Temuan 2 (HIGH): Swallowing `CancellationException` pada Operation Handlers
- **Lokasi File + Class/Method:**  
  - `filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/action/DeleteOperationHandler.kt` (`execute`, baris 34-36)  
  - `filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/action/CopyOperationHandler.kt` (`execute` baris 52-54, `executeResolved` baris 73-75)  
  - `filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/action/MoveOperationHandler.kt` (`execute` baris 50-52, `executeResolved` baris 71-73)  
  - `filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/action/FileDetailHandler.kt` (`loadDetails` baris 22-24, `computeChecksum` baris 32-34)
- **Masalah:**  
  Pada handler operasi berkas (`DeleteOperationHandler`, `CopyOperationHandler`, `MoveOperationHandler`), blok penanganan exception menangkap `CancellationException` dan mendispatch event `OperationCancelled`, namun **TIDAK melempar kembali (`rethrow`)** exception tersebut (`throw e` hilang).  
  Pada `FileDetailHandler`, digunakan `catch (e: Throwable)` yang menangkap segala jenis throwables termasuk `CancellationException` dan `VirtualMachineError`, lalu mengonversinya menjadi `FileDetailsFailed` / `ChecksumCalculationFailed`.
- **Dampak:**  
  Menelan `CancellationException` melanggar prinsip *Structured Concurrency* Kotlin Coroutines. Coroutine scope induk tidak dapat mendeteksi bahwa job telah dibatalkan, menyebabkan coroutine tetap menggantung (hanging/leaking coroutines) dan lifecycle cancellation tidak terpropagasi dengan benar.
- **Tingkat Severity:** HIGH
- **Rekomendasi Perbaikan:**  
  - Pada `DeleteOperationHandler`, `CopyOperationHandler`, dan `MoveOperationHandler`: tambahkan `throw e` setelah `dispatch(DualPaneEvent.OperationCancelled)`.
  - Pada `FileDetailHandler`: pisahkan penanganan `catch (e: CancellationException) { throw e }` sebelum `catch (e: Exception)`.

---

### Temuan 3 (HIGH): Inkonsistensi State Ekspansi Tree pada Auto-Expand Selection
- **Lokasi File + Class/Method:**  
  `filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/selection/TreeSelectionHandler.kt` (`nextSelection`, baris 85 & 128)
- **Masalah:**  
  `TreeSelectionHandler` melakukan mutasi langsung ke properti `node.isExpanded = true` pada node storage (baris 85) dan folder (baris 128). Namun, perubahan ini tidak memanggil `TreeState.expand(node)` atau `TreeState.forceRefresh()`.
- **Dampak:**  
  `TreeState` memegang `visibleNodes: StateFlow`, yang hanya diperbarui jika fungsi di `TreeState` dipanggil untuk meregenerasi flattened list (`updateVisibleNodes()`). Karena `node.isExpanded` diubah secara in-memory di luar `TreeState`, StateFlow tidak memancarkan daftar node terbaru, sehingga UI tidak menampilkan anak folder yang seharusnya otomatis terbuka (desinkronisasi antara internal model dan UI view).
- **Tingkat Severity:** HIGH
- **Rekomendasi Perbaikan:**  
  Delegasikan aksi ekspansi melalui `TreeNavigationAdapter` atau panggil `treeState.expand(node)` / `treeState.forceRefresh()` saat siklus auto-expand terjadi agar `visibleNodes` dihitung ulang secara reaktif.

---

### Temuan 4 (HIGH): Potensi `ConcurrentModificationException` pada `TreeNode` saat Refresh / Sorting Bersamaan dengan Interaksi UI
- **Lokasi File + Class/Method:**  
  - `treeview/src/main/java/com/wakwau/xplore/treeview/model/TreeNode.kt` (baris 13, properti `_children`)  
  - `filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/tree/FileTreeEngine.kt` (`refreshExpandedNodes`, `loadChildren`, `reSortCurrentNodes`)
- **Masalah:**  
  Koleksi `_children` di dalam `TreeNode` menggunakan `ArrayList` biasa (`mutableListOf<TreeNode<T>>()`). Operasi penambahan (`addChild`), penghapusan (`clearChildren`), dan pengurutan (`sortChildren`) dipanggil dari coroutine background/IO pada `FileTreeEngine`. Di saat yang sama, Compose UI mentraverse hirarki `_children` pada Main Thread melalui `TreeState.updateVisibleNodes()` dan `TreeScopeCalculator`.
- **Dampak:**  
  Jika user menggeser layar (scroll) atau mengklik node saat proses refresh/sorting direktori besar sedang berlangsung, JVM akan melempar `java.util.ConcurrentModificationException`, menyebabkan crash aplikasi.
- **Tingkat Severity:** HIGH
- **Rekomendasi Perbaikan:**  
  Gunakan thread-safe collection (seperti `CopyOnWriteArrayList`), atau pastikan seluruh mutasi dan traversal struktur `TreeNode` disinkronkan menggunakan thread lock / `Mutex`, atau lakukan snapshot copy (`toList()`) sebelum iterasi.

---

### Temuan 5 (MEDIUM): Global State Singleton pada `OperationProgressTracker`
- **Lokasi File + Class/Method:**  
  `core-worker/src/main/kotlin/com/wakwau/xplore/core/worker/OperationProgressTracker.kt` (baris 11-18)
- **Masalah:**  
  `OperationProgressTracker` dideklarasikan sebagai `object` singleton global:
  ```kotlin
  object OperationProgressTracker {
      private val _progressFlow = MutableSharedFlow<FileOperationResult<FileOperationProgress>>(extraBufferCapacity = 64)
      val progressFlow: SharedFlow<FileOperationResult<FileOperationProgress>> = _progressFlow.asSharedFlow()
  }
  ```
  Hal ini melanggar batasan eksplisit pada instruksi proyek: *"DILARANG MEMBUAT: Global State"*.
- **Dampak:**  
  1. Pelanggaran batas arsitektur & Single Responsibility Principle.
  2. State in-memory rentan hilang saat proses aplikasi dimatikan oleh sistem Android di background (process death).
  3. Rentan terhadap kebocoran progres jika terjadi beberapa operasi transfer sekuensial atau paralel.
- **Tingkat Severity:** MEDIUM
- **Rekomendasi Perbaikan:**  
  Gunakan mekanisme resmi Jetpack WorkManager: pantau progres via `WorkManager.getWorkInfoByIdFlow()` / `setProgressAsync()`, atau injeksikan event broker / tracker melalui Dependency Injection (`AppCompositionRoot`) ke dalam `FileOperationWorkerFactory` dan `FileOperationWorkManager` tanpa `object` singleton.

---

### Temuan 6 (MEDIUM): Inisialisasi Navigasi Flash Screen & Kondisi Terjebak di Permission Screen
- **Lokasi File + Class/Method:**  
  - `app/src/main/java/com/wakwau/xplore/XploreRoot.kt` (baris 89-100)  
  - `filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/presentation/FileManagerViewModel.kt` (baris 17-22, baris 29)
- **Masalah:**  
  `FileManagerUiState` memiliki nilai awal `hasPermission = false`. Pada `XploreRoot.kt`:
  ```kotlin
  val startDest = remember {
      if (uiState.hasPermission) AppRoute.DualPane.route else AppRoute.Permission.route
  }
  ```
  Karena `remember` tanpa parameter key dieksekusi pada komposisi pertama saat `uiState.hasPermission` masih bernilai *default* (`false`), `startDestination` NavHost selalu disetel ke `Permission.route`. Navigasi ke `DualPane` baru dipicu setelah `LaunchedEffect(uiState.hasPermission)` dijalankan.
- **Dampak:**  
  Pengguna yang sudah memberikan izin penyimpanan akan selalu melihat layar izin berkedip (flicker/flash) sesaat sebelum dialihkan ke layar Dual Pane setiap kali aplikasi dibuka.
- **Tingkat Severity:** MEDIUM
- **Rekomendasi Perbaikan:**  
  Evaluasi izin secara sinkron sebelum inisialisasi state awal `FileManagerUiState` atau tahan penampilan `NavHost` dengan splash/loading state hingga status izin terverifikasi.

---

### Temuan 7 (MEDIUM): Duplikasi Logika Pemeriksaan Izin Antara `core-ui` dan `core-storage`
- **Lokasi File + Class/Method:**  
  - `core-ui/src/main/java/com/wakwau/xplore/core/ui/permission/StoragePermissionHelper.kt`  
  - `core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/permission/StoragePermissionCheckerImpl.kt`
- **Masalah:**  
  Dua modul mengimplementasikan logika deteksi izin yang identik secara redundan:
  - Keduanya mengecek `Environment.isExternalStorageManager()` untuk Android 11+ (API 30+).
  - Keduanya mengecek `Manifest.permission.READ_EXTERNAL_STORAGE` dan `WRITE_EXTERNAL_STORAGE` untuk Android 10 ke bawah.
  - `core-ui` memiliki `StoragePermissionStatus`, sedangkan `core-storage-api` memiliki `StoragePermissionType`.
- **Dampak:**  
  Pelanggaran aturan *"tidak boleh ada duplikat code"*. Jika terjadi perubahan kebijakan izin Android di masa mendatang, developer harus memperbarui dua tempat berbeda, meningkatkan risiko desinkronisasi logika.
- **Tingkat Severity:** MEDIUM
- **Rekomendasi Perbaikan:**  
  Hapus duplikasi logika di `StoragePermissionHelper` dan arahkan implementasi untuk menggunakan kontrak `StoragePermissionChecker` dari `:core-storage-api` yang diimplementasikan di `:core-storage`.

---

### Temuan 8 (MEDIUM): Redundant ViewModel Architecture (`FileManagerViewModel` vs `DualPaneViewModel`)
- **Lokasi File + Class/Method:**  
  - `filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/presentation/FileManagerViewModel.kt`  
  - `filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/presentation/DualPaneViewModel.kt`  
  - `app/src/main/java/com/wakwau/xplore/XploreRoot.kt`
- **Masalah:**  
  Terdapat dua ViewModel terpisah yang di-instantiate di layar utama: `FileManagerViewModel` (hanya bertugas memeriksa permission, memuat volume, dan link storage) serta `DualPaneViewModel` (mengatur state dual pane, operasi berkas, dan juga memiliki method `loadVolumes`).
- **Dampak:**  
  Dua source of truth untuk `storageVolumes`. State storage volume terfragmentasi di antara dua ViewModel, melanggar prinsip *"Satu source of truth untuk setiap state"* dan *"Satu tanggung jawab = satu komponen"*.
- **Tingkat Severity:** MEDIUM
- **Rekomendasi Perbaikan:**  
  Pindahkan tanggung jawab observasi volume dan izin ke dalam alur `DualPaneViewModel` (atau buat UseCase terkoordinasi), lalu hapus `FileManagerViewModel` untuk menyederhanakan arsitektur presentation.

---

### Temuan 9 (MEDIUM): Unused / Orphaned Directory: `XFiles-1.3.1` pada Root Project
- **Lokasi File + Class/Method:**  
  Direktori root `/XFiles-1.3.1/` (beserta seluruh subdirektori di dalamnya: `app`, `fastlane`, `vendor`, `gradlew`, dll.)
- **Masalah:**  
  Terdapat artefak sisa ekstraksi proyek referensi luar (`XFiles-1.3.1`) yang berukuran puluhan megabyte di direktori utama. Folder ini tidak terdaftar di `settings.gradle.kts` proyek utama dan memuat puluhan file source code serta skrip build duplikat yang tidak terpakai.
- **Dampak:**  
  Membengkakkan repositori proyek, memperlambat proses pencarian kode/grep, dan dapat membingungkan auditor/developer lain terkait file mana yang aktif digunakan dalam build.
- **Tingkat Severity:** MEDIUM
- **Rekomendasi Perbaikan:**  
  Hapus direktori `/XFiles-1.3.1/` secara menyeluruh setelah memverifikasi bahwa tidak ada aset atau referensi aktif yang tertinggal.

---

### Temuan 10 (MEDIUM): Bypass Abstraction & Kerancuan ID pada Virtual Search Results Node
- **Lokasi File + Class/Method:**  
  - `filemanager/src/main/kotlin/com/wakwau/xplore/filemanager/factory/FileTreeItemFactory.kt` (`createSearchResultsRoot`)  
  - `filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/tree/FileTreeEngine.kt` (baris 294 & 300)
- **Masalah:**  
  Pada `FileTreeEngine`, ID node hasil pencarian di-generate dengan prefix khusus:  
  `id = "${StorageConstants.SEARCH_RESULT_ID_PREFIX}${item.location.path}"`.  
  Namun, pada operasi berkas lain (`findNodeByPath`, `reSortCurrentNodes`, `loadChildren`), sistem memperlakukan `node.id` setara dengan `item.location.path`. Hal ini menciptakan inkonsistensi identitas antara lokasi berkas riil dan ID node tampilan tree.
- **Dampak:**  
  Jika user memilih berkas dari hasil pencarian lalu melakukan aksi (rename, delete, rincian), fungsi lookup node dapat gagal menemukan node induk atau referensi berkas di tree, menyebabkan operasi gagal atau UI tidak ter-refresh.
- **Tingkat Severity:** MEDIUM
- **Rekomendasi Perbaikan:**  
  Pertahankan `node.data.location.path` sebagai satu-satunya rujukan identitas path berkas, dan pisahkan secara eksplisit antara Tree Node Identifier untuk keperluan Compose LazyColumn rendering dan StorageLocation path untuk operasi filesystem.

---

### Temuan 11 (MEDIUM): Potensi Uncaught Runtime Exceptions pada `PanelRefreshHandler.loadDirectory`
- **Lokasi File + Class/Method:**  
  `filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/action/PanelRefreshHandler.kt` (baris 14-34)
- **Masalah:**  
  Fungsi `loadDirectory` hanya menangani pembungkusan `FileOperationResult`, tetapi blok `try-catch` luar hanya menangkap `CancellationException`:
  ```kotlin
  try {
      when (val result = listDirectoryUseCase(location)) { ... }
  } catch (e: CancellationException) {
      throw e
  }
  ```
  Jika terjadi exception di luar `FileOperationResult` (misalnya `SecurityException`, `IllegalArgumentException`, atau `NullPointerException` tak terduga dari interaksi platform Android), exception tersebut tidak tertangkap.
- **Dampak:**  
  Aplikasi akan crash seketika pada saat user membuka folder yang memicu unhandled runtime exception di dalam coroutine.
- **Tingkat Severity:** MEDIUM
- **Rekomendasi Perbaikan:**  
  Tambahkan blok `catch (e: Exception)` untuk menangkap sisa runtime exception dan mendispatch event kegagalan: `dispatch(DualPaneEvent.DirectoryLoadFailed(panelId, e.message ?: "Unknown error"))`.

---

### Temuan 12 (MEDIUM): Potensi Non-Unique Key Crash di `ComposeTreeView`
- **Lokasi File + Class/Method:**  
  `treeview/src/main/java/com/wakwau/xplore/treeview/component/ComposeTreeView.kt` (baris 36)
- **Masalah:**  
  Pada `LazyColumn`, kunci item didefinisikan sebagai:
  ```kotlin
  itemsIndexed(
      items = visibleNodes,
      key = { _, it -> it.node.id }
  )
  ```
  Jika terdapat dua node dengan ID yang sama (misalnya root virtual, dua linked storage yang merujuk pada direktori serupa, atau item hasil pencarian ganda), Compose runtime akan melempar `IllegalArgumentException: Key was already used`.
- **Dampak:**  
  Crash fatal seketika pada layar tree view Jetpack Compose.
- **Tingkat Severity:** MEDIUM
- **Rekomendasi Perbaikan:**  
  Gunakan composite key yang menyertakan kedalaman atau indeks:
  `key = { index, it -> "${it.node.id}_${it.depth}_$index" }` atau pastikan ID dijamin unik secara global di tingkat generator node.

---

### Temuan 13 (LOW): Missing Logging pada Silent Catch Blocks Cleanup di Driver Filesystem
- **Lokasi File + Class/Method:**  
  - `core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/local/LocalFileSystem.kt` (baris 325, 375)  
  - `core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/saf/SafFileSystem.kt` (baris 375)  
  - `core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/root/RootFileSystem.kt` (baris 368)  
  - `core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/bridge/CrossFilesystemTransferBridge.kt` (baris 443, 447, 461)
- **Masalah:**  
  Terdapat blok kosong `catch (_: Exception) {}` saat menghapus target file yang parsial (setelah gagal copy/move).
- **Dampak:**  
  Jika file parsial gagal dihapus (misalnya izin write terblokir), developer tidak mendapatkan informasi apa pun di logcat untuk menganalisis sisa berkas sampah.
- **Tingkat Severity:** LOW
- **Rekomendasi Perbaikan:**  
  Tambahkan logging peringatan non-fatal (`Log.w("FileSystem", "Failed to clean partial file: ...", e)`).

---

### Temuan 14 (LOW): Pelanggaran Hardcoded Strings pada `FileOperationWorkerConstants`
- **Lokasi File + Class/Method:**  
  `core-worker/src/main/kotlin/com/wakwau/xplore/core/worker/FileOperationWorkerConstants.kt`
- **Masalah:**  
  Konstanta teks notifikasi:  
  - `CHANNEL_NAME = "Operasi Berkas"`  
  - `CHANNEL_DESCRIPTION = "Menampilkan progres operasi berkas di latar belakang"`  
  - `NOTIFICATION_TITLE = "Memproses Berkas..."`  
  ditulis hardcoded dalam bahasa Indonesia tanpa integrasi Android string resource (`@string/`).
- **Dampak:**  
  Teks notifikasi latar belakang tidak dapat dilokalisasi saat pengguna mengubah bahasa aplikasi di pengaturan (`AppLanguage`). Melanggar aturan *"tidak boleh pakai Hardcoded"*.
- **Tingkat Severity:** LOW
- **Rekomendasi Perbaikan:**  
  Pindahkan string ke `res/values/strings.xml` dan baca melalui `context.getString(R.string.xxx)`.

---

### Temuan 15 (LOW): Deklarasi `requestLegacyExternalStorage` Usang pada AndroidManifest.xml
- **Lokasi File + Class/Method:**  
  `app/src/main/AndroidManifest.xml` (baris 33)
- **Masalah:**  
  Tag `<application>` memuat atribut `android:requestLegacyExternalStorage="true"`. Pada targetSdk 36 (Android 15+), atribut ini sudah diabaikan secara total oleh sistem operasi.
- **Dampak:**  
  Konfigurasi usang (*dead configuration*) yang dapat memicu catatan audit kebijakan Google Play Store.
- **Tingkat Severity:** LOW
- **Rekomendasi Perbaikan:**  
  Hapus atribut `android:requestLegacyExternalStorage="true"`.

---

### Temuan 16 (LOW): Warning Manifest Merge pada WorkManager Initialization Tag
- **Lokasi File + Class/Method:**  
  `app/src/main/AndroidManifest.xml` (baris 66-70)
- **Masalah:**  
  Log build memunculkan peringatan:  
  `Warning: meta-data#androidx.work.WorkManagerInitializer was tagged at AndroidManifest.xml:66 to remove other declarations but no other declaration present`.
- **Dampak:**  
  Peringatan berulang pada setiap build/test log.
- **Tingkat Severity:** LOW
- **Rekomendasi Perbaikan:**  
  Hapus tag `meta-data` dengan `tools:node="remove"` jika dependensi WorkManager versi terkini tidak lagi menginjeksi initializer default tersebut.

---

### Temuan 17 (LOW): Navigasi Compose Menggunakan String Route Alih-alih `@Serializable` Type-Safe Keys
- **Lokasi File + Class/Method:**  
  `app/src/main/java/com/wakwau/xplore/navigation/AppRoute.kt`
- **Masalah:**  
  Rute navigasi didefinisikan menggunakan format string lama:  
  `sealed class AppRoute(val route: String) { object Permission : AppRoute("permission_screen") ... }`  
  daripada kontrak tipe aman `@Serializable` yang direkomendasikan pada pedoman framework Android.
- **Dampak:**  
  Tidak type-safe jika di masa mendatang perlu melewatkan argumen kompleks antar layar.
- **Tingkat Severity:** LOW
- **Rekomendasi Perbaikan:**  
  Migrasi `AppRoute` menjadi serializable data class / object sesuai Navigation Compose 2.8+.

---

### Temuan 18 (LOW): Beban Komputasi Checksum pada Berkas Raksasa Tanpa Peringatan Ukuran
- **Lokasi File + Class/Method:**  
  `filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/detail/FileDetailChecksumTab.kt`
- **Masalah:**  
  Tab checksum langsung menghitung 4 hash (MD5, SHA-1, SHA-256, CRC-32) tanpa memeriksa ukuran berkas terlebih dahulu.
- **Dampak:**  
  Jika pengguna membuka rincian berkas berukuran sangat besar (misalnya 10 GB - 50 GB), pemrosesan akan mengonsumsi CPU dan baterai secara masif.
- **Tingkat Severity:** LOW
- **Rekomendasi Perbaikan:**  
  Tampilkan tombol konfirmasi "Hitung Checksum" jika ukuran berkas melebihi 500 MB.

---

### Temuan 19 (LOW): Potensi Deadlock/Hang pada `LocalFileSystem.copySingleFile` Saat Zero-Byte TransferTo Berulang
- **Lokasi File + Class/Method:**  
  `core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/filesystem/local/LocalFileSystem.kt` (baris 300-315)
- **Masalah:**  
  Loop pemindahan data menggunakan `inChannel.transferTo`. Pada beberapa filesystem virtual/FUSE, `transferTo` dapat mengembalikan `0` bytes terus-menerus jika kernel buffer penuh atau terjadi socket stalling.
- **Dampak:**  
  Potensi loop tak berujung (infinite loop) yang memakan CPU jika `transferTo` mengembalikan 0 tanpa melempar exception.
- **Tingkat Severity:** LOW
- **Rekomendasi Perbaikan:**  
  Tambahkan pengecekan counter jika `transferTo` mengembalikan `0` berkali-kali secara berurutan, lalu alihkan ke buffer streaming manual `inChannel.read()`.

---

### Temuan 20 (LOW): Komentar Wajib Instruksi Pemrograman Belum Lengkap pada Modul Utilitas Lama
- **Lokasi File + Class/Method:**  
  - `core/src/main/java/com/wakwau/xplore/core/util/ByteFormatter.kt`  
  - `core/src/main/java/com/wakwau/xplore/core/util/DateFormatter.kt`  
  - `core-ui/src/main/java/com/wakwau/xplore/core/ui/components/AppDialog.kt`  
  - `core-ui/src/main/java/com/wakwau/xplore/core/ui/components/BreadcrumbBar.kt`
- **Masalah:**  
  Beberapa file lama di modul `:core` dan `:core-ui` belum memiliki format komentar wajib:
  `// [Jalur Class/Modul]: ...`
  `// [Penjelasan]: ...`
  sesuai Aturan Pemrograman Instruksi Khusus nomor 1.
- **Dampak:**  
  Inkonsistensi format dokumentasi antar modul proyek.
- **Tingkat Severity:** LOW
- **Rekomendasi Perbaikan:**  
  Sematkan tag komentar standar pada seluruh file pendukung yang belum memilikinya.

---

## Tabel Matriks Temuan Audit

| No | Lokasi Komponen | Kategori Temuan | Tingkat Severity | Status Rekomendasi |
|:---|:---|:---|:---:|:---|
| 1 | `FileSystemSearchTraversal` & `DualPaneReducer` | Logic Bug (Search Data Overwrite & Premature Dismiss) | **CRITICAL** | Akumulasikan batch & pisahkan event selesai |
| 2 | `Delete/Copy/Move/FileDetailHandler` | Coroutine Bug (Swallowing `CancellationException`) | **HIGH** | Selalu rethrow `CancellationException` |
| 3 | `TreeSelectionHandler` | State Sync (Auto-expand tanpa notifikasi `TreeState`) | **HIGH** | Sinkronkan ekspansi via `TreeState` |
| 4 | `TreeNode` & `FileTreeEngine` | Concurrency Bug (Thread-unsafe `_children` modification) | **HIGH** | Amankan mutasi koleksi `_children` |
| 5 | `OperationProgressTracker` | Architecture Violation (Global State Singleton `object`) | **MEDIUM** | Gunakan WorkManager progress / DI |
| 6 | `XploreRoot` & `FileManagerViewModel` | UI State Bug (Flicker Permission Screen pada start) | **MEDIUM** | Sinkronkan inisialisasi state izin awal |
| 7 | `StoragePermissionHelper` & `CheckerImpl` | Duplicate Code (Logika perizinan terduplikasi) | **MEDIUM** | Satukan ke `:core-storage` via contract |
| 8 | `FileManagerViewModel` & `DualPaneViewModel` | Architecture / SRP (Dual ViewModel untuk volume) | **MEDIUM** | Konsolidasi ke `DualPaneViewModel` |
| 9 | Direktori root `/XFiles-1.3.1` | Dead Code / Project Bloat (Folder proyek sisa referensi) | **MEDIUM** | Hapus direktori tidak terpakai |
| 10 | `FileTreeItemFactory` & `FileTreeEngine` | Abstraction Bug (Kerancuan Node ID vs File Path) | **MEDIUM** | Standarisasi pemisahan ID dan Path |
| 11 | `PanelRefreshHandler` | Crash Hazard (Uncaught runtime exceptions) | **MEDIUM** | Tangkap `Exception` generik |
| 12 | `ComposeTreeView` | Crash Hazard (Potensi duplicate key pada LazyColumn) | **MEDIUM** | Gunakan composite key unik |
| 13 | Filesystem Drivers Cleanup | Error Handling (Silent catch blocks tanpa logging) | **LOW** | Tambahkan logcat warning |
| 14 | `FileOperationWorkerConstants` | Hardcoded Violation (String notifikasi tanpa strings.xml) | **LOW** | Pindahkan string ke `strings.xml` |
| 15 | `AndroidManifest.xml` | Play Policy / Legacy (Atribut `requestLegacyExternalStorage`) | **LOW** | Hapus atribut usang |
| 16 | `AndroidManifest.xml` | Build Warning (Tag metadata WorkManager redundant) | **LOW** | Bersihkan tag usang |
| 17 | `AppRoute.kt` | Framework Guideline (String route alih-alih `@Serializable`) | **LOW** | Migrasi ke `@Serializable` |
| 18 | `FileDetailChecksumTab` | Performance / UX (Komputasi file masif tanpa konfirmasi) | **LOW** | Beri batas/konfirmasi ukuran |
| 19 | `LocalFileSystem.kt` | I/O Stalling (Potensi hang pada zero-byte `transferTo`) | **LOW** | Tambahkan timeout/fallback buffer |
| 20 | Utility Modul `:core` & `:core-ui` | Coding Rules (Format komentar wajib belum lengkap) | **LOW** | Lengkapi format komentar |

---

**Hasil Validasi Lingkungan:**
- Kompilasi (`compile_applet`): **PASS**
- Unit Tests (`gradle test`): **PASS** (141 actionable tasks, seluruh test suite hijau)
- Status Kode Saat Ini: **TIDAK ADA KODE YANG DIUBAH** sesuai instruksi pengguna.
