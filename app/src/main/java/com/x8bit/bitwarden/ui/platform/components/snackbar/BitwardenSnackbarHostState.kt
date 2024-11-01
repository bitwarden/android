package com.x8bit.bitwarden.ui.platform.components.snackbar

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.x8bit.bitwarden.ui.platform.base.util.Text

/**
 * A custom state holder for [BitwardenSnackbarData] and manging a snackbar host with the
 * passed in [SnackbarHostState].
 */
@Stable
class BitwardenSnackbarHostState(
    val snackbarHostState: SnackbarHostState,
) {
    /**
     * The current snackbar data to be displayed.
     */
    var currentSnackbarData: BitwardenSnackbarData? by mutableStateOf(null)
        private set

    /**
     * Shows a snackbar with the given [snackbarData]. Passes the [BitwardenSnackbarData.key]
     * through the message parameter of the [SnackbarHostState.showSnackbar] method. This key
     * can be used to identify the correct snackbar data to show in the host.
     */
    suspend fun showSnackbar(
        snackbarData: BitwardenSnackbarData,
        duration: SnackbarDuration = SnackbarDuration.Short,
    ): SnackbarResult {
        currentSnackbarData = snackbarData
        return snackbarHostState
            .showSnackbar(message = snackbarData.key, duration = duration)
            .also { currentSnackbarData = null }
    }
}

/**
 * Models possible data to show in a custom bitwarden snackbar.
 * @property message The text to show in the snackbar.
 * @property messageHeader The optional title text to show.
 * @property actionLabel The optional text to show in the action button.
 * @property withDismissAction Whether to show the dismiss action.
 * @property key The unique key for the [BitwardenSnackbarData].
 */
@Immutable
data class BitwardenSnackbarData(
    val message: Text,
    val messageHeader: Text? = null,
    val actionLabel: Text? = null,
    val withDismissAction: Boolean = false,
) {
    val key: String = this.hashCode().toString()
}

/**
 * Creates a [BitwardenSnackbarHostState] that is remembered across compositions.
 */
@Composable
fun rememberBitwardenSnackbarHostState(
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) = remember {
    BitwardenSnackbarHostState(snackbarHostState)
}
