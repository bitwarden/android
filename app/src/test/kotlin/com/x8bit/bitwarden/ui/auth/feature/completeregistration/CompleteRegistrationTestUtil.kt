package com.x8bit.bitwarden.ui.auth.feature.completeregistration

import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.R

/**
 * Creates a mock [CompleteRegistrationDialog.HaveIBeenPwned].
 */
fun createHaveIBeenPwned(
    title: Text = R.string.weak_and_exposed_master_password.asText(),
    message: Text = R.string.weak_password_identified_and_found_in_a_data_breach_alert_description
        .asText(),
): CompleteRegistrationDialog.HaveIBeenPwned =
    CompleteRegistrationDialog.HaveIBeenPwned(
        title = title,
        message = message,
    )
