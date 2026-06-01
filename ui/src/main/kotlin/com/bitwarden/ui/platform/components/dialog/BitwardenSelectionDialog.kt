package com.bitwarden.ui.platform.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.dialog.row.BitwardenSelectionRow
import com.bitwarden.ui.platform.components.dialog.util.maxDialogHeight
import com.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Displays a dialog with a title and "Cancel" button.
 *
 * @param title Title to display.
 * @param subTitle The subtitle to display
 * @param onDismissRequest Invoked when the user dismisses the dialog.
 * @param selectionItems Lambda containing selection items to show to the user. See
 * [BitwardenSelectionRow].
 */
@Suppress("LongMethod")
@Composable
fun BitwardenSelectionDialog(
    title: String,
    subTitle: String? = null,
    onDismissRequest: () -> Unit,
    selectionItems: @Composable ColumnScope.() -> Unit = {},
) {
    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        val configuration = LocalConfiguration.current
        val scrollState = rememberScrollState()
        var canScrollForward by remember { mutableStateOf(value = false) }
        var canScrollBackward by remember { mutableStateOf(value = false) }
        LaunchedEffect(scrollState.canScrollBackward, scrollState.canScrollForward) {
            canScrollForward = scrollState.canScrollForward
            canScrollBackward = scrollState.canScrollBackward
        }
        Column(
            modifier = Modifier
                .semantics {
                    testTagsAsResourceId = true
                    testTag = "AlertPopup"
                }
                .requiredHeightIn(
                    max = configuration.maxDialogHeight,
                )
                // This background is necessary for the dialog to not be transparent.
                .background(
                    color = BitwardenTheme.colorScheme.background.primary,
                    shape = BitwardenTheme.shapes.dialog,
                ),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                modifier = Modifier
                    .testTag("AlertTitleText")
                    .padding(24.dp)
                    .fillMaxWidth(),
                text = title,
                color = BitwardenTheme.colorScheme.text.primary,
                style = BitwardenTheme.typography.headlineSmall,
            )
            subTitle?.let {
                Text(
                    modifier = Modifier
                        .testTag("AlertSubTitleText")
                        .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                        .fillMaxWidth(),
                    text = it,
                    color = BitwardenTheme.colorScheme.text.secondary,
                    style = BitwardenTheme.typography.bodyMedium,
                )
            }
            if (canScrollBackward) {
                BitwardenHorizontalDivider()
            }
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState),
                content = selectionItems,
            )
            if (canScrollForward) {
                BitwardenHorizontalDivider()
            }
            BitwardenTextButton(
                modifier = Modifier
                    .testTag("DismissAlertButton")
                    .padding(24.dp),
                label = stringResource(id = BitwardenString.cancel),
                onClick = onDismissRequest,
            )
        }
    }
}
