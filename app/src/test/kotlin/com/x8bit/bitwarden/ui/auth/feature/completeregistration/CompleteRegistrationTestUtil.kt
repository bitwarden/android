package com.x8bit.bitwarden.ui.auth.feature.completeregistration

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText

/**
 * Creates a mock [CompleteRegistrationDialog.HaveIBeenPwned].
 */
fun createHaveIBeenPwned(
    title: Text = BitwardenString.weak_and_exposed_master_password.asText(),
    message: Text = BitwardenString
        .weak_password_identified_and_found_in_a_data_breach_alert_description
        .asText(),
): CompleteRegistrationDialog.HaveIBeenPwned =
    CompleteRegistrationDialog.HaveIBeenPwned(
        title = title,
        message = message,
    )
