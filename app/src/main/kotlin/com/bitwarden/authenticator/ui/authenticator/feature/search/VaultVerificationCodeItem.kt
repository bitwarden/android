package com.bitwarden.authenticator.ui.authenticator.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.components.icon.BitwardenIcon
import com.bitwarden.authenticator.ui.platform.components.indicator.BitwardenCircularCountdownIndicator
import com.bitwarden.authenticator.ui.platform.components.model.IconData
import com.bitwarden.authenticator.ui.platform.theme.AuthenticatorTheme

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
    modifier: Modifier = Modifier,
    supportingLabel: String? = null,
) {
    Row(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = MaterialTheme.colorScheme.primary),
                onClick = onItemClick,
            )
            .defaultMinSize(minHeight = 72.dp)
            .padding(vertical = 8.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BitwardenIcon(
            iconData = startIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )

        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.weight(1f),
        ) {
            issuer?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            supportingLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        IconButton(
            onClick = onCopyClick,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_copy),
                contentDescription = stringResource(id = R.string.copy),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Suppress("MagicNumber")
@Preview(showBackground = true)
@Composable
private fun VerificationCodeItem_preview() {
    AuthenticatorTheme {
        VaultVerificationCodeItem(
            startIcon = IconData.Local(R.drawable.ic_login_item),
            issuer = "Sample Label",
            supportingLabel = "Supporting Label",
            authCode = "1234567890".chunked(3).joinToString(" "),
            timeLeftSeconds = 15,
            periodSeconds = 30,
            onCopyClick = {},
            onItemClick = {},
            modifier = Modifier.padding(horizontal = 16.dp),
            alertThresholdSeconds = 7,
        )
    }
}
