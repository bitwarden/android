package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.browser

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage
import com.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.browser.model.BrowserAutofillSettingsOption
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Card for displaying a list of [BrowserAutofillSettingsOption]s and whether they are currently
 * enabled.
 *
 * @param options List of data to display in the card, if the list is empty nothing will be drawn.
 * @param onOptionClicked Lambda that is invoked when an option row is clicked and passes back the
 * [BrowserPackage] for that option.
 * @param enabled Whether to show the switches for each option as enabled.
 */
@Composable
fun BrowserAutofillSettingsCard(
    options: ImmutableList<BrowserAutofillSettingsOption>,
    onOptionClicked: (BrowserPackage) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) return
    Column(modifier = modifier) {
        options.forEachIndexed { index, option ->
            BitwardenSwitch(
                label = option.optionText(),
                isChecked = option.isEnabled,
                onCheckedChange = {
                    onOptionClicked(option.browserPackage)
                },
                cardStyle = if (index == 0) {
                    CardStyle.Top(
                        dividerPadding = 16.dp,
                    )
                } else {
                    CardStyle.Middle(
                        dividerPadding = 16.dp,
                    )
                },
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
        }
        Text(
            text = stringResource(
                id = R.string.improves_login_filling_for_supported_websites_on_selected_browsers,
            ),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.secondary,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .cardStyle(
                    cardStyle = CardStyle.Bottom,
                    paddingHorizontal = 16.dp,
                )
                .defaultMinSize(minHeight = 48.dp),
        )
    }
}

@Preview
@Composable
private fun ChromeAutofillSettingsCard_preview() {
    BitwardenTheme {
        BrowserAutofillSettingsCard(
            options = persistentListOf(
                BrowserAutofillSettingsOption.BraveStable(enabled = true),
                BrowserAutofillSettingsOption.ChromeStable(enabled = false),
                BrowserAutofillSettingsOption.ChromeBeta(enabled = true),
            ),
            enabled = true,
            onOptionClicked = {},
        )
    }
}
