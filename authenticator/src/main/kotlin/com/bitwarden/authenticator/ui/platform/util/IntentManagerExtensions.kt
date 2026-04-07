@file:OmitFromCoverage

package com.bitwarden.authenticator.ui.platform.util

import android.content.Intent
import androidx.core.net.toUri
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.manager.IntentManager

/**
 * Launches the Bitwarden account settings.
 */
fun IntentManager.startBitwardenAccountSettings() {
    startActivity(
        Intent(
            Intent.ACTION_VIEW,
            "bitwarden://settings/account_security".toUri(),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )
}
