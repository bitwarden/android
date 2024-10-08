package com.x8bit.bitwarden.ui.platform.components.dialog

import android.os.Parcelable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.indicator.BitwardenCircularProgressIndicator
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.parcelize.Parcelize

/**
 * Represents a Bitwarden-styled loading dialog that shows text and a circular progress indicator.
 *
 * @param visibilityState the [LoadingDialogState] used to populate the dialog.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BitwardenLoadingDialog(
    visibilityState: LoadingDialogState,
) {
    when (visibilityState) {
        is LoadingDialogState.Hidden -> Unit
        is LoadingDialogState.Shown -> {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                ),
            ) {
                Card(
                    shape = BitwardenTheme.shapes.dialog,
                    colors = CardDefaults.cardColors(
                        containerColor = BitwardenTheme.colorScheme.background.primary,
                        contentColor = BitwardenTheme.colorScheme.text.primary,
                    ),
                    modifier = Modifier
                        .semantics {
                            testTagsAsResourceId = true
                            testTag = "AlertPopup"
                        }
                        .fillMaxWidth()
                        .wrapContentHeight(),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = visibilityState.text(),
                            modifier = Modifier
                                .testTag("AlertTitleText")
                                .padding(
                                    top = 24.dp,
                                    bottom = 8.dp,
                                ),
                        )
                        BitwardenCircularProgressIndicator(
                            modifier = Modifier
                                .testTag("AlertProgressIndicator")
                                .padding(
                                    top = 8.dp,
                                    bottom = 24.dp,
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun BitwardenLoadingDialog_preview() {
    BitwardenTheme {
        BitwardenLoadingDialog(
            visibilityState = LoadingDialogState.Shown(
                text = "Loading...".asText(),
            ),
        )
    }
}

/**
 * Models display of a [BitwardenLoadingDialog].
 */
sealed class LoadingDialogState : Parcelable {
    /**
     * Hide the dialog.
     */
    @Parcelize
    data object Hidden : LoadingDialogState()

    /**
     * Show the dialog with the given values.
     */
    @Parcelize
    data class Shown(val text: Text) : LoadingDialogState()
}
