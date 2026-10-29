package com.x8bit.bitwarden.data.pam.util

import android.content.Context
import android.content.Intent
import com.bitwarden.ui.platform.util.getSafeParcelableExtra
import com.x8bit.bitwarden.MainActivity
import com.x8bit.bitwarden.data.platform.manager.model.PamAlertData

private const val PAM_ALERT_DATA: String = "pamAlertData"

/**
 * Creates an [Intent] that opens the PAM access request an alert notification is about.
 */
fun createPamAlertIntent(
    context: Context,
    data: PamAlertData,
): Intent =
    Intent(context, MainActivity::class.java)
        .putExtra(PAM_ALERT_DATA, data)

/**
 * Checks if the given [Intent] contains data for a PAM alert, the [PamAlertData] is returned when
 * present.
 */
fun Intent.getPamAlertDataIntentOrNull(): PamAlertData? =
    this.getSafeParcelableExtra(PAM_ALERT_DATA)
