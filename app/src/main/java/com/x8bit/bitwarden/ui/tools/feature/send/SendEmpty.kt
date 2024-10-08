package com.x8bit.bitwarden.ui.tools.feature.send

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledTonalButton
import com.x8bit.bitwarden.ui.platform.components.card.BitwardenInfoCalloutCard
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Content for the empty state of the [SendScreen].
 */
@Composable
fun SendEmpty(
    policyDisablesSend: Boolean,
    onAddItemClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        if (policyDisablesSend) {
            BitwardenInfoCalloutCard(
                text = stringResource(id = R.string.send_disabled_warning),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.weight(1F))

        Text(
            textAlign = TextAlign.Center,
            text = stringResource(id = R.string.no_sends),
            style = BitwardenTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        BitwardenFilledTonalButton(
            onClick = onAddItemClick,
            label = stringResource(id = R.string.add_a_send),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.weight(1F))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
