package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.blockautofill

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.util.maxDialogHeight
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A dialog for adding a blocked URI.
 */
@Suppress("LongMethod")
@Composable
fun AddEditBlockedUriDialog(
    uri: String,
    isEdit: Boolean,
    errorMessage: String?,
    onUriChange: (String) -> Unit,
    onCancelClick: () -> Unit,
    onSaveClick: (String) -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        val configuration = LocalConfiguration.current
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .requiredHeightIn(
                    max = configuration.maxDialogHeight,
                )
                // This background is necessary for the dialog to not be transparent.
                .background(
                    color = BitwardenTheme.colorScheme.background.primary,
                    shape = RoundedCornerShape(28.dp),
                ),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                modifier = Modifier
                    .padding(top = 24.dp, start = 24.dp, end = 24.dp)
                    .fillMaxWidth(),
                text = stringResource(id = R.string.new_uri),
                color = BitwardenTheme.colorScheme.text.primary,
                style = BitwardenTheme.typography.headlineSmall,
            )
            if (scrollState.canScrollBackward) {
                BitwardenHorizontalDivider()
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState),
            ) {
                BitwardenTextField(
                    label = stringResource(id = R.string.enter_uri),
                    isError = errorMessage != null,
                    hint = errorMessage ?: stringResource(
                        id = R.string.format_x_separate_multiple_ur_is_with_a_comma,
                        "http://domain.com",
                    ),
                    value = uri,
                    onValueChange = onUriChange,
                    keyboardType = KeyboardType.Uri,
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                )
            }
            if (scrollState.canScrollForward) {
                BitwardenHorizontalDivider()
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp, top = 24.dp, bottom = 24.dp, end = 24.dp),
            ) {
                if (isEdit && onDeleteClick != null) {
                    BitwardenTextButton(
                        label = stringResource(id = R.string.remove),
                        onClick = onDeleteClick,
                    )
                }

                BitwardenTextButton(
                    label = stringResource(id = R.string.cancel),
                    onClick = onCancelClick,
                )

                BitwardenFilledButton(
                    label = stringResource(id = R.string.save),
                    onClick = { onSaveClick(uri) },
                )
            }
        }
    }
}
