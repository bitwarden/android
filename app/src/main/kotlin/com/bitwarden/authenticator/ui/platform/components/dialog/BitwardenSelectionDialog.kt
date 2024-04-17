package com.bitwarden.authenticator.ui.platform.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.authenticator.ui.platform.components.util.maxDialogHeight

/**
 * Displays a dialog with a title and "Cancel" button.
 *
 * @param title Title to display.
 * @param onDismissRequest Invoked when the user dismisses the dialog.
 * @param selectionItems Lambda containing selection items to show to the user. See
 * [BitwardenSelectionRow].
 */
@Suppress("LongMethod")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BitwardenSelectionDialog(
    title: String,
    onDismissRequest: () -> Unit,
    selectionItems: @Composable ColumnScope.() -> Unit = {},
) {
    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        val configuration = LocalConfiguration.current
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .requiredHeightIn(
                    max = configuration.maxDialogHeight,
                )
                // This background is necessary for the dialog to not be transparent.
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(28.dp),
                ),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall,
            )
            if (scrollState.canScrollBackward) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState),
                content = selectionItems,
            )
            if (scrollState.canScrollForward) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
            BitwardenTextButton(
                modifier = Modifier.padding(24.dp),
                label = stringResource(id = R.string.cancel),
                onClick = onDismissRequest,
            )
        }
    }
}
