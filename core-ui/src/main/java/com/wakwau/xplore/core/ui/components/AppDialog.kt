package com.wakwau.xplore.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wakwau.xplore.core.ui.R
import com.wakwau.xplore.core.ui.theme.DarkBorder
import com.wakwau.xplore.core.ui.theme.DarkSurface
import com.wakwau.xplore.core.ui.theme.DarkSurfaceElevated
import com.wakwau.xplore.core.ui.theme.XploreOrange

object AppDialogDefaults {
    const val DIALOG_WIDTH_FRACTION = 0.92f
    val CornerRadius: Dp = 8.dp
    val TonalElevation: Dp = 6.dp
    val ContentPadding: Dp = 16.dp
    val TitleBottomSpacing: Dp = 12.dp
    val ContentBottomSpacing: Dp = 16.dp
    val ButtonSpacing: Dp = 8.dp
}

@Composable
fun AppDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    confirmButtonText: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissButtonText: String? = stringResource(R.string.btn_cancel),
    isConfirmEnabled: Boolean = true,
    confirmButtonColor: Color = XploreOrange,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(AppDialogDefaults.DIALOG_WIDTH_FRACTION)
                .clip(RoundedCornerShape(AppDialogDefaults.CornerRadius)),
            color = DarkSurface,
            tonalElevation = AppDialogDefaults.TonalElevation
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDialogDefaults.ContentPadding)
            ) {
                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(AppDialogDefaults.TitleBottomSpacing))

                // Content
                content()

                Spacer(modifier = Modifier.height(AppDialogDefaults.ContentBottomSpacing))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    if (dismissButtonText != null) {
                        OutlinedButton(
                            onClick = onDismissRequest,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(dismissButtonText)
                        }
                    }

                    if (confirmButtonText != null && onConfirm != null) {
                        Spacer(modifier = Modifier.width(AppDialogDefaults.ButtonSpacing))
                        Button(
                            onClick = onConfirm,
                            enabled = isConfirmEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = confirmButtonColor,
                                contentColor = Color.Black
                            )
                        ) {
                            Text(confirmButtonText)
                        }
                    }
                }
            }
        }
    }
}
