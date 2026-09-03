// [Jalur Class]: com.wakwau.xplore.filemanager.ui.component.FileManagerTopBar
// [Penjelasan]: Komponen TopAppBar Material 3 untuk menampilkan judul aplikasi, indikator jumlah item terpilih, tombol kembali, dan tombol navigasi pengaturan.
package com.wakwau.xplore.filemanager.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakwau.xplore.core.ui.theme.XPloreTheme
import com.wakwau.xplore.filemanager.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerTopBar(
    title: String = stringResource(R.string.app_title),
    selectedCount: Int = 0,
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLinkStorageClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = XPloreTheme.colors

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    fontSize = 20.sp
                )
                if (selectedCount > 0) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        color = colors.badgeBackground,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.badgeBorder)
                    ) {
                        Text(
                            text = "✓ $selectedCount",
                            color = colors.badgeText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = colors.textPrimary
                )
            }
        },
        actions = {
            IconButton(onClick = onLinkStorageClick) {
                Icon(
                    imageVector = Icons.Default.AddLink,
                    contentDescription = stringResource(R.string.cd_link_storage),
                    tint = colors.textPrimary
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.cd_app_settings),
                    tint = colors.textPrimary
                )
            }
        },
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.topBarContainer
        )
    )
}
