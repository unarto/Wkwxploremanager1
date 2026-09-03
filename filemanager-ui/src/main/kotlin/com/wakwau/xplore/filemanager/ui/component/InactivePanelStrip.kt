// [Jalur Class/Modul]: filemanager-ui/src/main/kotlin/com/wakwau/xplore/filemanager/ui/component/InactivePanelStrip.kt
// [Penjelasan]: Menampilkan strip samping panel tidak aktif dengan ikon dan label dinamis berbasis StorageConstants dan string resource tanpa hardcoding.
package com.wakwau.xplore.filemanager.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakwau.xplore.core.storage.constant.StorageConstants
import com.wakwau.xplore.core.ui.theme.XPloreTheme
import com.wakwau.xplore.filemanager.ui.R
import com.wakwau.xplore.filemanager.ui.state.PanelId

@Composable
fun InactivePanelStrip(
    panelId: PanelId,
    locationName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLeft = panelId == PanelId.LEFT
    val colors = XPloreTheme.colors
    val internalStorageLabel = stringResource(R.string.label_internal_shared_storage)

    val isPhoneStorage = locationName.isEmpty() ||
            locationName.equals(internalStorageLabel, ignoreCase = true) ||
            locationName.equals(StorageConstants.DEFAULT_PRIMARY_VOLUME_NAME, ignoreCase = true)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(36.dp)
            .background(colors.sideBarContainer)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon (Phone / Folder)
        Icon(
            imageVector = if (isPhoneStorage) {
                Icons.Default.PhoneAndroid
            } else {
                Icons.Default.Folder
            },
            contentDescription = null,
            tint = if (isLeft) colors.primary else colors.secondary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Small directional arrow
        Icon(
            imageVector = if (isLeft) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = stringResource(R.string.cd_switch_to_panel),
            tint = colors.secondary,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Vertical rotated title
        val storageLoadingLabel = stringResource(R.string.label_storage_loading)
        val displayName = if (locationName.isEmpty()) storageLoadingLabel else locationName
        Text(
            text = displayName,
            color = colors.textSecondary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = -90f
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.height, placeable.width) {
                        placeable.placeRelative(
                            -(placeable.width - placeable.height) / 2,
                            -(placeable.height - placeable.width) / 2
                        )
                    }
                }
        )
    }
}
