package com.x8bit.bitwarden.ui.auth.feature.createaccount

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText

/**
 * Creates a mock [CreateAccountDialog.HaveIBeenPwned].
 */
fun createHaveIBeenPwned(
    title: Text = R.string.weak_and_exposed_master_password.asText(),
    message: Text = R.string.weak_password_identified_and_found_in_a_data_breach_alert_description
        .asText(),
): CreateAccountDialog.HaveIBeenPwned =
    CreateAccountDialog.HaveIBeenPwned(
        title = title,
        message = message,
    )
