// [Jalur Class]: com.wakwau.xplore.core.ui.components.dialog.RenameDialog
// [Penjelasan]: Dialog komponen reusable untuk operasi penggantian nama (rename) file atau folder. Menggunakan AppDialog sebagai basis.
package com.wakwau.xplore.core.ui.components.dialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
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
fun RenameDialog(
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    // Menyiapkan initial value dengan ekstensi (jika ada) terpilih tapi siap dimodifikasi.
    var textFieldValue by remember { 
        val dotIndex = initialName.lastIndexOf('.')
        val selectionEnd = if (dotIndex > 0) dotIndex else initialName.length
        mutableStateOf(
            TextFieldValue(
                text = initialName,
                selection = TextRange(0, selectionEnd)
            )
        )
    }
    
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val isNameValid = textFieldValue.text.isNotBlank() && textFieldValue.text != initialName

    AppDialog(
        title = stringResource(R.string.dialog_title_rename),
        onDismissRequest = onDismissRequest,
        confirmButtonText = stringResource(R.string.btn_rename),
        onConfirm = {
            if (isNameValid) {
                onConfirm(textFieldValue.text)
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
            label = { Text(stringResource(R.string.label_new_name)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = XploreOrange,
                focusedLabelColor = XploreOrange,
                cursorColor = XploreOrange
            )
        )
    }
}
