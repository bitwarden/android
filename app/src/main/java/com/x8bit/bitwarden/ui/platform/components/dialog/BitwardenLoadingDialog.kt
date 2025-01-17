package com.x8bit.bitwarden.ui.platform.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a Bitwarden-styled loading dialog that shows text and a circular progress indicator.
 *
 * @param text The text to display in the dialog.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BitwardenLoadingDialog(
    text: String,
) {
    Popup(
        properties = PopupProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        BitwardenLoadingContent(
            text = text,
            modifier = Modifier
                .semantics {
                    testTagsAsResourceId = true
                    testTag = "AlertPopup"
                }
                .fillMaxSize()
                .background(
                    color = BitwardenTheme.colorScheme.background.secondary.copy(alpha = 0.90f),
                ),
        )
    }
}

@Preview
@Composable
private fun BitwardenLoadingDialog_preview() {
    BitwardenTheme {
        BitwardenLoadingDialog(text = "Loading...")
    }
}
