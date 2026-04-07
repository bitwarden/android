package com.x8bit.bitwarden.ui.tools.feature.send

import android.content.res.Configuration
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toAnnotatedString
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.card.BitwardenInfoCalloutCard
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Content for the empty state of the [SendScreen].
 */
@Suppress("LongMethod")
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
            Spacer(modifier = Modifier.height(12.dp))
            BitwardenInfoCalloutCard(
                text = stringResource(id = BitwardenString.send_disabled_warning),
                modifier = Modifier
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.weight(1F))
        Image(
            painter = rememberVectorPainter(BitwardenDrawable.ill_send),
            contentDescription = null,
            modifier = Modifier
                .standardHorizontalMargin()
                .size(100.dp),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            textAlign = TextAlign.Center,
            text = stringResource(BitwardenString.send_sensitive_information_safely),
            style = BitwardenTheme.typography.titleMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            textAlign = TextAlign.Center,
            text = stringResource(
                BitwardenString.share_files_and_data_securely_with_anyone_on_any_platform,
            ),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .testTag("EmptySendListText"),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // This button is hidden from accessibility to avoid duplicate voice over with the FAB
        val newSendLabel = stringResource(id = BitwardenString.add_a_send)
        BitwardenFilledButton(
            onClick = onAddItemClick,
            label = stringResource(id = BitwardenString.add_a_send),
            icon = rememberVectorPainter(BitwardenDrawable.ic_plus_small),
            modifier = Modifier
                .clearAndSetSemantics {
                    text = newSendLabel.toAnnotatedString()
                    hideFromAccessibility()
                }
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.weight(1F))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SendEmpty_preview() {
    BitwardenTheme {
        Column(
            modifier = Modifier.background(BitwardenTheme.colorScheme.background.primary),
        ) {
            SendEmpty(
                policyDisablesSend = false,
                onAddItemClick = {},
            )
        }
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SendEmptyPolicyDisabled_preview() {
    BitwardenTheme {
        Column(
            modifier = Modifier.background(BitwardenTheme.colorScheme.background.primary),
        ) {
            SendEmpty(
                policyDisablesSend = true,
                onAddItemClick = {},
            )
        }
    }
}
