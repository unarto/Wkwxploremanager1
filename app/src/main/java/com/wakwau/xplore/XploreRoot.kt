// [Jalur Class/Modul]: app/src/main/java/com/wakwau/xplore/XploreRoot.kt
// [Penjelasan]: Root composable aplikasi yang mengamati DualPaneViewModel sebagai Single Source of Truth, mengeliminasi flicker cold-start izin dan mengelola alur navigasi aplikasi.
package com.wakwau.xplore

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wakwau.xplore.filemanager.ui.R
import com.wakwau.xplore.filemanager.ui.presentation.DualPaneViewModel
import com.wakwau.xplore.filemanager.ui.screen.DualPaneFileManagerScreen
import com.wakwau.xplore.filemanager.ui.screen.PermissionScreen
import com.wakwau.xplore.filemanager.ui.settings.SettingsTreeScreen
import com.wakwau.xplore.navigation.AppRoute
import com.wakwau.xplore.settings.SettingsViewModel

@Composable
fun XploreRoot(
    settingsViewModel: SettingsViewModel = viewModel(
        factory = (LocalContext.current.applicationContext as XploreApplication).appCompositionRoot.settingsViewModelFactory
    )
) {
    // [Jalur Class/Modul]: app/src/main/java/com/wakwau/xplore/XploreRoot.kt
    // [Penjelasan]: Menghubungkan DualPaneViewModel sebagai satu-satunya presentation orchestrator ke seluruh tampilan.
    val context = LocalContext.current
    val app = context.applicationContext as XploreApplication

    val dualPaneViewModel: DualPaneViewModel = viewModel(
        factory = app.appCompositionRoot.dualPaneViewModelFactory
    )

    val dualPaneState by dualPaneViewModel.state.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.settingsState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    
    var showLinkDialog by remember { mutableStateOf(false) }

    val linkStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            dualPaneViewModel.addLinkedStorage(it)
        }
    }

    val manageStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        dualPaneViewModel.checkPermission()
    }
    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        dualPaneViewModel.checkPermission()
    }

    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text(stringResource(R.string.title_link_storage)) },
            text = { Text(stringResource(R.string.msg_link_storage_explanation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLinkDialog = false
                        linkStorageLauncher.launch(null)
                    }
                ) {
                    Text(stringResource(R.string.btn_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) {
                    Text(stringResource(R.string.btn_cancel_operation))
                }
            }
        )
    }

    // [Jalur Class/Modul]: app/src/main/java/com/wakwau/xplore/XploreRoot.kt
    // [Penjelasan]: Inisialisasi start destination sinkron pada cold start melalui usecase langsung untuk mencegah flicker layar izin jika izin sudah diberikan.
    val initialHasPermission = remember { app.appCompositionRoot.fileManagerUseCaseModule.checkStoragePermissionUseCase.hasPermission() }
    val startDest = remember {
        if (initialHasPermission) AppRoute.DualPane.route else AppRoute.Permission.route
    }

    // Navigasi otomatis jika permission baru saja diberikan.
    LaunchedEffect(dualPaneState.hasPermission) {
        if (dualPaneState.hasPermission && navController.currentDestination?.route == AppRoute.Permission.route) {
            navController.navigate(AppRoute.DualPane.route) {
                popUpTo(AppRoute.Permission.route) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDest) {
        composable(AppRoute.Permission.route) {
            PermissionScreen(
                onRequestPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        manageStorageLauncher.launch(com.wakwau.xplore.filemanager.ui.permission.PermissionIntentHelper.createManageStorageIntent(context))
                    } else {
                        com.wakwau.xplore.filemanager.ui.permission.PermissionIntentHelper.requestStorageAccess(context) { permissions ->
                            legacyPermissionLauncher.launch(permissions)
                        }
                    }
                }
            )
        }
        composable(AppRoute.DualPane.route) {
            DualPaneFileManagerScreen(
                viewModel = dualPaneViewModel,
                treeAdapter = app.appCompositionRoot.fileManagerPresentationModule.treeNavigationAdapter,
                storageVolumes = dualPaneState.storageVolumes,
                onSettingsClick = { navController.navigate(AppRoute.Settings.route) },
                onLinkStorageClick = { showLinkDialog = true },
                onRemoveLinkClick = { uri -> dualPaneViewModel.removeLinkedStorage(uri) }
            )
        }
        composable(AppRoute.Settings.route) {
            SettingsTreeScreen(
                settingsState = settingsState,
                onThemeSelected = { mode -> settingsViewModel.setThemeMode(mode) },
                onLanguageSelected = { lang -> settingsViewModel.setLanguage(lang) },
                onFileSystemAccessModeSelected = { mode -> settingsViewModel.setFileSystemAccessMode(mode) },
                onRootReadOnlyChanged = { isReadOnly -> settingsViewModel.setRootReadOnly(isReadOnly) },
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
