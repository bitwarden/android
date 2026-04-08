package com.bitwarden.ui.platform.components.dialog.model

import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.util.Text

/**
 * Contains the data for displaying a [BitwardenTwoButtonDialog].
 *
 * @property title The optional title to show.
 * @property message The message to show.
 * @property confirmButtonText The text to show on confirm button.
 * @property dismissButtonText The text to show on dismiss button.
 */
data class BitwardenTwoButtonDialogData(
    val title: Text?,
    val message: Text,
    val confirmButtonText: Text,
    val dismissButtonText: Text,
)
