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

/**
 * An action card prompting the user to set up Bitwarden as their autofill service.
 *
 * @param onTurnOnClick Invoked when the user clicks the primary "Turn on autofill" button.
 * @param secondaryButton The text button displayed beneath the primary button.
 * @param modifier The [Modifier] to be applied to the card.
 * @param onDismissClick Invoked when the user clicks the dismiss button, or `null` to hide it.
 */
@Composable
fun SetUpAutofillActionCard(
    onTurnOnClick: () -> Unit,
    secondaryButton: BitwardenButtonData,
    modifier: Modifier = Modifier,
    onDismissClick: (() -> Unit)? = null,
) {
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
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SetUpAutofillActionCard_preview() {
    BitwardenTheme {
        SetUpAutofillActionCard(
            onTurnOnClick = {},
            secondaryButton = BitwardenButtonData(
                label = BitwardenString.dont_remind_me_again.asText(),
                onClick = {},
            ),
            onDismissClick = {},
        )
    }
}
