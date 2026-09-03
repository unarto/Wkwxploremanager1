// [Jalur Class]: com.wakwau.xplore.core.ui.components.dialog.CreateDirectoryDialog
// [Penjelasan]: Dialog komponen reusable untuk membuat folder baru dengan text selection otomatis dan validasi nama folder tanpa hardcoding.
package com.wakwau.xplore.core.ui.components.dialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import com.wakwau.xplore.core.ui.R
import com.wakwau.xplore.core.ui.components.AppDialog
import com.wakwau.xplore.core.ui.theme.XploreOrange

@Composable
fun CreateDirectoryDialog(
    initialName: String = "",
    onConfirm: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val defaultInitialName = if (initialName.isEmpty()) stringResource(R.string.dialog_title_create_folder) else initialName
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = defaultInitialName,
                selection = TextRange(0, defaultInitialName.length)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val isNameValid = textFieldValue.text.isNotBlank()

    AppDialog(
        title = stringResource(R.string.dialog_title_create_folder),
        onDismissRequest = onDismissRequest,
        confirmButtonText = stringResource(R.string.btn_create),
        onConfirm = {
            if (isNameValid) {
                onConfirm(textFieldValue.text.trim())
            }
        },
        isConfirmEnabled = isNameValid,
        confirmButtonColor = XploreOrange
    ) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { textFieldValue = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true,
            label = { Text(stringResource(R.string.label_folder_name)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = XploreOrange,
                focusedLabelColor = XploreOrange,
                cursorColor = XploreOrange
            )
        )
    }
}
