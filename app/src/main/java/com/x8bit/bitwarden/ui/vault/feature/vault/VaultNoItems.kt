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
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.card.BitwardenInfoCalloutCard
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * No items view for the [VaultScreen].
 */
@Composable
fun VaultNoItems(
    addItemClickAction: () -> Unit,
    policyDisablesSend: Boolean,
    modifier: Modifier = Modifier,
    @DrawableRes vectorRes: Int = R.drawable.img_vault_items,
    headerText: String = stringResource(id = R.string.save_and_protect_your_data),
    message: String = stringResource(R.string.the_vault_protects_more_than_just_passwords),
    buttonText: String = stringResource(R.string.new_login),
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (policyDisablesSend) {
            BitwardenInfoCalloutCard(
                text = stringResource(id = R.string.send_disabled_warning),
                modifier = Modifier
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.weight(1F))

        Image(
            painter = rememberVectorPainter(id = vectorRes),
            contentDescription = null,
            modifier = Modifier
                .standardHorizontalMargin()
                .size(100.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
            text = headerText,
            style = BitwardenTheme.typography.titleMedium,
            color = BitwardenTheme.colorScheme.text.primary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
            text = message,
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
        )

        Spacer(modifier = Modifier.height(24.dp))

        BitwardenFilledButton(
            icon = rememberVectorPainter(R.drawable.ic_plus_small),
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
                addItemClickAction = {},
                policyDisablesSend = true,
            )
        }
    }
}
