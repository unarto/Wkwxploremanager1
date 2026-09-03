// [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/screen/PermissionScreen.kt
// [Penjelasan]: Layar Composable untuk meminta izin akses penyimpanan pengguna secara konsolidasi tanpa duplikasi modul UI.
package com.wakwau.xplore.filemanager.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wakwau.xplore.filemanager.ui.R

object PermissionScreenDefaults {
    val ScreenPadding: Dp = 32.dp
    val TitleSpacing: Dp = 16.dp
    val ContentSpacing: Dp = 32.dp
}

@Composable
fun PermissionScreen(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(PermissionScreenDefaults.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.title_storage_permission),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(PermissionScreenDefaults.TitleSpacing))
            Text(
                text = stringResource(R.string.msg_storage_permission_required),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(PermissionScreenDefaults.ContentSpacing))
            Button(onClick = onRequestPermission) {
                Text(stringResource(R.string.btn_grant_permission))
            }
        }
    }
}

