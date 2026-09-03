# Laporan Audit Fitur Copy, Move, dan Delete

Dokumen ini berisi hasil audit komprehensif terhadap subsistem operasi berkas (**Copy**, **Move**, **Delete**) pada aplikasi WKW Xplore (`com.wakwau.xplore`), mencakup call chain dari UI Presentation layer (`filemanager-ui`), Domain UseCase (`filemanager`), Background Worker (`core-worker`), Repository & Database Synchronization (`core-storage`), hingga Filesystem Driver (`LocalFileSystem`, `SafFileSystem`, `SafShizukuFileSystem`).

---

## 1. Temuan Bug & Masalah Logika (Critical Logic Bugs)

### 1.1 Bug Penggabungan Path (Path Concatenation & Trailing Slash)
- **Lokasi**: `core-worker/src/main/kotlin/com/wakwau/xplore/core/worker/FileOperationWorker.kt` (baris 53–56 dan 65–68)
- **Penjelasan**: 
  - Path tujuan dibentuk menggunakan format: `destination.path + "/" + source.path.substringAfterLast('/')`.
  - **Kasus 1 (Double Slash)**: Jika `destination.path` berakhir dengan garis miring `/` (misalnya root path `/`), terbentuk double slash (`//filename`).
  - **Kasus 2 (Trailing Slash Folder)**: Jika path folder sumber berakhir dengan `/`, `substringAfterLast('/')` mengembalikan string kosong `""`, sehingga nama folder hilang dan menimpa path induk.
  - **Kasus 3 (SAF Content URI Incompatibility)**: Untuk `SafFileSystem` yang menggunakan URI bertipe `content://`, penambahan `+ "/" + ...` menghasilkan URI malformed/rusak, sehingga `resolveTreeDocumentFile` atau `resolveDocumentFile` gagal menemukan direktori tujuan (`Destination SAF folder not found`).

### 1.2 Ketiadaan Bridging Antar-Filesystem (Cross-Filesystem Copy/Move)
- **Lokasi**: `core-storage/src/main/kotlin/com/wakwau/xplore/core/storage/repository/FileRepositoryImpl.kt` (baris 85–95 & 112–122)
- **Penjelasan**:
  - `FileRepositoryImpl` mendelegasikan operasi secara biner: jika salah satu path berawalan `content://`, seluruh operasi dialihkan ke `safFileSystem.copy(source, destination)`.
  - Jika sumber adalah berkas lokal (`/storage/...`) dan tujuan adalah folder SAF (`content://...`) atau sebaliknya, `SafFileSystem` mencoba mem-parse path lokal sebagai Uri SAF via `resolveDocumentFile`, yang menghasilkan `null` dan melempar `FileNotFoundException: Source SAF file not found`.
  - Belum ada adapter/stream bridge antar-driver filesystem (Local <-> SAF <-> Shizuku).

### 1.3 Target Root ID Fallback Tidak Akurat
- **Lokasi**: 
  - `filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/action/CopyOperationHandler.kt` (baris 29)
  - `filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/action/MoveOperationHandler.kt` (baris 29)
- **Penjelasan**:
  - `val destLocation = StorageLocation(path = targetPath, rootId = destPanel.currentLocation?.rootId ?: StorageConstants.UNKNOWN_ROOT_ID)`
  - Jika panel tujuan (`destPanel.currentLocation`) belum terisi saat navigasi pohon (misal hanya level root atau tree selection), `rootId` di-fallback ke `UNKNOWN_ROOT_ID`. Hal ini dapat mengacaukan identifikasi volume pada sinkronisasi Room DB dan filesystem resolver.

### 1.4 Ambiguitas Target Berkas vs Folder pada SideActionBar
- **Lokasi**: `filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/screen/FileManagerContent.kt` (baris 118–130)
- **Penjelasan**:
  - `targetPath` diambil dari `inactivePanel.currentLocation?.path ?: treeAdapter.getSelectedPath(inactivePanel.id).value ...`
  - Jika di panel inaktif pengguna sedang memilih sebuah **berkas** (bukan folder), `targetPath` menjadi path berkas tersebut. Operasi Copy/Move kemudian mencoba memperlakukan berkas tersebut sebagai direktori kontainer tujuan.

### 1.5 Tombol Batal pada Progress Dialog Tidak Menghentikan Background Worker
- **Lokasi**: 
  - `filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/screen/FileManagerContent.kt` (baris 213)
  - `core-storage-api/src/main/kotlin/com/wakwau/xplore/core/storage/operation/BackgroundOperationManager.kt`
- **Penjelasan**:
  - Saat dialog progres ditekan "Batal", UI mengirimkan `DualPaneEvent.ClearOperationState` yang hanya mengubah status UI menjadi `Idle`.
  - Antarmuka `BackgroundOperationManager` tidak memiliki fungsi `cancelOperation()` sehingga `WorkManager` dan `FileOperationWorker` tetap berjalan di latar belakang hingga selesai tanpa terbatalkan.

### 1.6 Redundant Refresh Tree Nodes pada Siklus Penyelesaian Operasi
- **Lokasi**: `filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/presentation/DualPaneViewModel.kt` (baris 80–81 & 105–111)
- **Penjelasan**:
  - Ketika operasi berhasil, ViewModel mendispatch `Refresh(LEFT)` dan `Refresh(RIGHT)`.
  - Di dalam handler `Refresh`, masing-masing memanggil `treeNavigationAdapter.refreshAllNodes()`, menyebabkan pemindaian ulang rekursif pada seluruh node tree sebanyak 2 kali berturut-turut.

---

## 2. Temuan Kode Mati & Tidak Terpakai (Dead / Unused Code)
- **`FileOperationResult.Completed` pada Delete**:
  - Di `FileOperationWorker.kt` (baris 90–92), blok penanganan `FileOperationResult.Completed` bertindak sebagai no-op karena `fileRepository.delete()` hanya mengembalikan `Success`, `Failure`, atau `Cancelled`.
- **Secondary Constructor Tanpa Dependensi Injeksi**:
  - `CopyOperationHandler`, `MoveOperationHandler`, dan `DeleteOperationHandler` memiliki secondary constructor dengan default mapper yang redundan terhadap DI/Factory.

---

## 3. Temuan Hardcoded Strings & Nilai Konstan
- **Pesan Galat Exception Internal**:
  - Pesan teks seperti `"SAF file system is not available"`, `"Root file system is not available"`, `"Source and destination are the same"` masih berupa hardcoded string di tingkat repository, meskipun di tingkat UI sudah ditangani via `StorageErrorMapper` dan `stringResource`.

---

## 4. Status Implementasi (Mock / Fake / Simulasi)
- **Hasil Audit**: **BERSIH (Tidak Ada Mock/Fake/Placeholder)**
  - Tidak ditemukan data tiruan, dummy list, atau simulasi waktu (`Thread.sleep`/fake progress) pada subsistem Copy, Move, dan Delete.
  - Seluruh I/O menggunakan stream nyata (`FileInputStream`/`FileOutputStream`), SAF `ContentResolver`, IPC Shizuku dengan `ParcelFileDescriptor`, dan WorkManager Foreground Service yang terintegrasi dengan sinkronisasi Room DB.

---

## 5. Ringkasan Call Chain
```
[User Action: SideActionBar (Copy/Move/Delete)]
   ↓
[FileManagerContent: Confirmation Dialog]
   ↓
[DualPaneViewModel: Dispatch Event (ExecuteConfirmedCopy/Move, DeleteSelected)]
   ↓
[OperationHandler: Copy/Move/DeleteOperationHandler]
   ↓
[UseCase: CopyFilesUseCase / MoveFilesUseCase / DeleteFilesUseCase]
   ↓
[Manager: FileOperationWorkManager (enqueue OneTimeWorkRequest)]
   ↓
[Background Service: FileOperationWorker (Foreground Notification & Progress Emission)]
   ↓
[Repository: FileRepositoryImpl (LocalFileSystem / SafFileSystem / SafShizukuFileSystem)]
   ↓
[Database Sync: FileIndexSynchronizer (Sinkronisasi indeks pencarian Room DB)]
   ↓
[UI Observation: DualPaneViewModel -> OperationProgressTracker -> ProgressDialog / Reducer State Update]
```
