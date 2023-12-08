package com.x8bit.bitwarden.ui.platform.base.util

import android.content.Context
import android.widget.Toast
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * Shows a [Toast] with a message indicating something is not yet implemented.
 */
@OmitFromCoverage
fun showNotYetImplementedToast(context: Context) {
    Toast
        .makeText(
            context,
            "Not yet implemented",
            Toast.LENGTH_SHORT,
        )
        .show()
}
