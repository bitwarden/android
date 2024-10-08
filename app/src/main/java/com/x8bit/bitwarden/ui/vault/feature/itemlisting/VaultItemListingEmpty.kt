package com.x8bit.bitwarden.ui.vault.feature.itemlisting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultNoItems

/**
 * Empty view for the [VaultItemListingScreen].
 */
@Composable
fun VaultItemListingEmpty(
    state: VaultItemListingState.ViewState.NoItems,
    policyDisablesSend: Boolean,
    addItemClickAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.shouldShowAddButton) {
        VaultNoItems(
            policyDisablesSend = policyDisablesSend,
            vectorRes = state.vectorRes,
            headerText = state.header(),
            message = state.message(),
            buttonText = state.buttonText(),
            modifier = modifier,
            addItemClickAction = addItemClickAction,
        )
    } else {
        GenericNoItems(
            text = state.message(),
            modifier = modifier,
        )
    }
}

@Composable
private fun GenericNoItems(
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            text = text,
            style = BitwardenTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
