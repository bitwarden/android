package com.x8bit.bitwarden.ui.platform.components

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.parcelize.Parcelize

/**
 * Represents a Bitwarden-styled loading dialog that shows text and a circular progress indicator.
 *
 * @param visibilityState the [LoadingDialogState] used to populate the dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitwardenLoadingDialog(
    visibilityState: LoadingDialogState,
) {
    when (visibilityState) {
        is LoadingDialogState.Hidden -> Unit
        is LoadingDialogState.Shown -> {
            AlertDialog(
                onDismissRequest = {},
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = visibilityState.text(),
                            modifier = Modifier.padding(
                                top = 24.dp,
                                bottom = 8.dp,
                            ),
                        )
                        CircularProgressIndicator(
                            modifier = Modifier.padding(
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
