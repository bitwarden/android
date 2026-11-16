package com.x8bit.bitwarden.ui.platform.components.card

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.bitwarden.ui.platform.components.button.model.BitwardenButtonData
import com.bitwarden.ui.platform.components.card.BitwardenActionCard
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage
import com.x8bit.bitwarden.ui.platform.util.displayLabel

/**
 * An action card prompting the user to set up autofill, or to turn on third party autofill in
 * their default browser.
 *
 * @param browserPackage The default browser that needs third party autofill turned on, or `null`
 * to prompt the user to set up autofill.
 * @param onTurnOnClick Invoked when the user clicks the primary "Turn on" button.
 * @param secondaryButton The text button displayed beneath the primary button.
 * @param modifier The [Modifier] to be applied to the card.
 * @param onDismissClick Invoked when the user clicks the dismiss button, or `null` to hide it.
 */
@Composable
fun AutofillPromptActionCard(
    browserPackage: BrowserPackage?,
    onTurnOnClick: () -> Unit,
    secondaryButton: BitwardenButtonData,
    modifier: Modifier = Modifier,
    onDismissClick: (() -> Unit)? = null,
) {
    if (browserPackage == null) {
        BitwardenActionCard(
            cardTitle = stringResource(id = BitwardenString.set_up_autofill),
            cardSubtitle = stringResource(
                id = BitwardenString.bitwarden_can_suggest_your_passwords_and_other_credentials,
            ),
            actionButton = BitwardenButtonData(
                label = BitwardenString.turn_on_autofill.asText(),
                onClick = onTurnOnClick,
            ),
            secondaryButton = secondaryButton,
            onDismissClick = onDismissClick,
            modifier = modifier,
        )
    } else {
        val browserName = browserPackage.displayLabel
        BitwardenActionCard(
            cardTitle = BitwardenString.turn_on_autofill_in_x.asText(browserName)(),
            cardSubtitle = BitwardenString
                .x_needs_one_more_setting_before_bitwarden_can_fill_your_passwords
                .asText(browserName)(),
            actionButton = BitwardenButtonData(
                label = BitwardenString.turn_on_in_x.asText(browserName),
                onClick = onTurnOnClick,
            ),
            secondaryButton = secondaryButton,
            onDismissClick = onDismissClick,
            modifier = modifier,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AutofillPromptActionCardSetUpAutofill_preview() {
    BitwardenTheme {
        AutofillPromptActionCard(
            browserPackage = null,
            onTurnOnClick = {},
            secondaryButton = BitwardenButtonData(
                label = BitwardenString.dont_remind_me_again.asText(),
                onClick = {},
            ),
            onDismissClick = {},
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AutofillPromptActionCardBrowser_preview() {
    BitwardenTheme {
        AutofillPromptActionCard(
            browserPackage = BrowserPackage.CHROME_STABLE,
            onTurnOnClick = {},
            secondaryButton = BitwardenButtonData(
                label = BitwardenString.having_trouble_get_help.asText(),
                onClick = {},
            ),
        )
    }
}
