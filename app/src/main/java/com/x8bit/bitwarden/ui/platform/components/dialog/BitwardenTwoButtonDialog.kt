package com.x8bit.bitwarden.ui.platform.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.util.maxDialogHeight
import com.x8bit.bitwarden.ui.platform.components.util.maxDialogWidth

/**
 * Represents a Bitwarden-styled dialog with two buttons.
 *
 * @param title the optional title to show.
 * @param message message to show.
 * @param confirmButtonText text to show on confirm button.
 * @param dismissButtonText text to show on dismiss button.
 * @param onConfirmClick called when the confirm button is clicked.
 * @param onDismissClick called when the dismiss button is clicked.
 * @param onDismissRequest called when the user attempts to dismiss the dialog (for example by
 * tapping outside of it).
 * @param confirmTextColor The color of the confirm text.
 * @param dismissTextColor The color of the dismiss text.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Composable
@Suppress("LongMethod")
fun BitwardenTwoButtonDialog(
    title: String?,
    message: String,
    confirmButtonText: String,
    dismissButtonText: String,
    onConfirmClick: () -> Unit,
    onDismissClick: () -> Unit,
    onDismissRequest: () -> Unit,
    confirmTextColor: Color? = null,
    dismissTextColor: Color? = null,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val configuration = LocalConfiguration.current
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .semantics {
                    testTagsAsResourceId = true
                    testTag = "AlertPopup"
                }
                .requiredHeightIn(
                    max = configuration.maxDialogHeight,
                )
                .requiredWidthIn(
                    max = configuration.maxDialogWidth,
                )
                // This background is necessary for the dialog to not be transparent.
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(28.dp),
                ),
            horizontalAlignment = Alignment.End,
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            title?.let {
                Text(
                    modifier = Modifier
                        .testTag("AlertTitleText")
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth(),
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (scrollState.canScrollBackward) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
            Text(
                modifier = Modifier
                    .testTag("AlertContentText")
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                text = message,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (scrollState.canScrollForward) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                BitwardenTextButton(
                    modifier = Modifier
                        .testTag("AcceptAlertButton")
                        .padding(horizontal = 4.dp),
                    label = confirmButtonText,
                    labelTextColor = confirmTextColor,
                    onClick = onConfirmClick,
                )
                BitwardenTextButton(
                    modifier = Modifier
                        .testTag("DismissAlertButton")
                        .padding(horizontal = 4.dp),
                    label = dismissButtonText,
                    labelTextColor = dismissTextColor,
                    onClick = onDismissClick,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
