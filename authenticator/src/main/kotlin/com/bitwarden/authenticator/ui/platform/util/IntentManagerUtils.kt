@file:OmitFromCoverage

package com.bitwarden.authenticator.ui.platform.util

import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.authenticator.ui.platform.manager.intent.IntentManager

/**
 * Launches the authenticator app settings.
 */
fun IntentManager.startAuthenticatorAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData("package:$packageName".toUri()),
    )
}

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
