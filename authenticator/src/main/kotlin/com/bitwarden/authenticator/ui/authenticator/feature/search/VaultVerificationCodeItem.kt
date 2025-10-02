package com.bitwarden.authenticator.ui.authenticator.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.components.icon.BitwardenIcon
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.indicator.BitwardenCircularCountdownIndicator
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * The verification code item displayed to the user.
 *
 * @param authCode The code for the item.
 * @param issuer The label for the item.
 * @param periodSeconds The times span where the code is valid.
 * @param timeLeftSeconds The seconds remaining until a new code is needed.
 * @param startIcon The leading icon for the item.
 * @param onCopyClick The lambda function to be invoked when the copy button is clicked.
 * @param onItemClick The lambda function to be invoked when the item is clicked.
 * @param modifier The modifier for the item.
 * @param supportingLabel The supporting label for the item.
 */
@Suppress("LongMethod", "MagicNumber")
@Composable
fun VaultVerificationCodeItem(
    authCode: String,
    issuer: String?,
    periodSeconds: Int,
    timeLeftSeconds: Int,
    alertThresholdSeconds: Int,
    startIcon: IconData,
    onCopyClick: () -> Unit,
    onItemClick: () -> Unit,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
    supportingLabel: String? = null,
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 60.dp)
            .cardStyle(
                cardStyle = cardStyle,
                onClick = onItemClick,
                paddingStart = 16.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BitwardenIcon(
            iconData = startIcon,
            tint = BitwardenTheme.colorScheme.icon.primary,
            modifier = Modifier.size(24.dp),
        )

        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.weight(1f),
        ) {
            if (!issuer.isNullOrEmpty()) {
                Text(
                    text = issuer,
                    style = BitwardenTheme.typography.bodyLarge,
                    color = BitwardenTheme.colorScheme.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!supportingLabel.isNullOrEmpty()) {
                Text(
                    text = supportingLabel,
                    style = BitwardenTheme.typography.bodyMedium,
                    color = BitwardenTheme.colorScheme.text.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        BitwardenCircularCountdownIndicator(
            timeLeftSeconds = timeLeftSeconds,
            periodSeconds = periodSeconds,
            alertThresholdSeconds = alertThresholdSeconds,
        )

        Text(
            text = authCode.chunked(3).joinToString(" "),
            style = BitwardenTheme.typography.sensitiveInfoSmall,
            color = BitwardenTheme.colorScheme.text.primary,
        )

        IconButton(
            onClick = onCopyClick,
        ) {
            Icon(
                painter = painterResource(id = BitwardenDrawable.ic_copy),
                contentDescription = stringResource(id = BitwardenString.copy),
                tint = BitwardenTheme.colorScheme.icon.primary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Suppress("MagicNumber")
@Preview(showBackground = true)
@Composable
private fun VerificationCodeItem_preview() {
    BitwardenTheme {
        VaultVerificationCodeItem(
            startIcon = IconData.Local(BitwardenDrawable.ic_login_item),
            issuer = "Sample Label",
            supportingLabel = "Supporting Label",
            authCode = "1234567890".chunked(3).joinToString(" "),
            timeLeftSeconds = 15,
            periodSeconds = 30,
            onCopyClick = {},
            onItemClick = {},
            modifier = Modifier.padding(horizontal = 16.dp),
            alertThresholdSeconds = 7,
            cardStyle = CardStyle.Full,
        )
    }
}
