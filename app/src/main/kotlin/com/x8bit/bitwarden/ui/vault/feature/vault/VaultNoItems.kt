package com.x8bit.bitwarden.ui.vault.feature.vault

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.card.BitwardenInfoCalloutCard
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * No items view for the [VaultScreen].
 */
@Composable
fun VaultNoItems(
    addItemClickAction: () -> Unit,
    policyDisablesSend: Boolean,
    message: String,
    buttonText: String,
    modifier: Modifier = Modifier,
    @DrawableRes vectorRes: Int? = null,
    headerText: String? = null,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (policyDisablesSend) {
            BitwardenInfoCalloutCard(
                text = stringResource(id = BitwardenString.send_disabled_warning),
                modifier = Modifier
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.weight(1F))

        vectorRes?.let {
            Image(
                painter = rememberVectorPainter(id = it),
                contentDescription = null,
                modifier = Modifier
                    .standardHorizontalMargin()
                    .size(100.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
        headerText?.let {
            Text(
                textAlign = TextAlign.Center,
                text = it,
                style = BitwardenTheme.typography.titleMedium,
                color = BitwardenTheme.colorScheme.text.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
            Spacer(Modifier.height(12.dp))
        }
        Text(
            textAlign = TextAlign.Center,
            text = message,
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        BitwardenFilledButton(
            icon = rememberVectorPainter(BitwardenDrawable.ic_plus_small),
            modifier = Modifier.standardHorizontalMargin(),
            onClick = addItemClickAction,
            label = buttonText,
        )

        Spacer(modifier = Modifier.weight(1F))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Preview(name = "Light theme")
@Preview(name = "Dark theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VaultNoItems_preview() {
    BitwardenTheme {
        Column(
            modifier = Modifier.background(BitwardenTheme.colorScheme.background.primary),
        ) {
            VaultNoItems(
                vectorRes = BitwardenDrawable.ill_vault_items,
                headerText = stringResource(id = BitwardenString.save_and_protect_your_data),
                message = stringResource(
                    BitwardenString.the_vault_protects_more_than_just_passwords,
                ),
                buttonText = stringResource(BitwardenString.new_login),
                addItemClickAction = {},
                policyDisablesSend = false,
            )
        }
    }
}

@Preview(name = "Light theme")
@Preview(name = "Dark theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VaultNoItemsPolicyDisabled_preview() {
    BitwardenTheme {
        Column(
            modifier = Modifier.background(BitwardenTheme.colorScheme.background.primary),
        ) {
            VaultNoItems(
                message = stringResource(
                    BitwardenString.the_vault_protects_more_than_just_passwords,
                ),
                buttonText = stringResource(BitwardenString.new_login),
                addItemClickAction = {},
                policyDisablesSend = true,
            )
        }
    }
}
