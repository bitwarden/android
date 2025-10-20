package com.bitwarden.authenticator.ui.platform.components.listitem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.authenticator.ui.platform.components.listitem.model.VaultDropdownMenuAction
import com.bitwarden.authenticator.ui.platform.components.listitem.model.VerificationCodeDisplayItem
import com.bitwarden.core.util.persistentListOfNotNull
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.components.appbar.action.BitwardenOverflowActionItem
import com.bitwarden.ui.platform.components.appbar.model.OverflowMenuItemData
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
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
 * @param displayItem he model containing all relevant data to be displayed.
 * @param onItemClick The lambda function to be invoked when the item is clicked.
 * @param onDropdownMenuClick A lambda function invoked when a dropdown menu action is clicked.
 * @param cardStyle The card style to be applied to this item.
 * @param modifier The modifier for the item.
 */
@Composable
fun VaultVerificationCodeItem(
    displayItem: VerificationCodeDisplayItem,
    onItemClick: () -> Unit,
    onDropdownMenuClick: (VaultDropdownMenuAction) -> Unit,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
) {
    VaultVerificationCodeItem(
        authCode = displayItem.authCode,
        primaryLabel = displayItem.title,
        secondaryLabel = displayItem.subtitle,
        periodSeconds = displayItem.periodSeconds,
        timeLeftSeconds = displayItem.timeLeftSeconds,
        alertThresholdSeconds = displayItem.alertThresholdSeconds,
        startIcon = displayItem.startIcon,
        onItemClick = onItemClick,
        onDropdownMenuClick = onDropdownMenuClick,
        showOverflow = displayItem.showOverflow,
        showMoveToBitwarden = displayItem.showMoveToBitwarden,
        cardStyle = cardStyle,
        modifier = modifier,
    )
}

/**
 * The verification code item displayed to the user.
 *
 * @param authCode The code for the item.
 * @param primaryLabel The label for the item. Represents the OTP issuer.
 * @param secondaryLabel The supporting label for the item. Represents the OTP account name.
 * @param periodSeconds The times span where the code is valid.
 * @param timeLeftSeconds The seconds remaining until a new code is needed.
 * @param alertThresholdSeconds The time threshold in seconds to display an expiration warning.
 * @param startIcon The leading icon for the item.
 * @param onItemClick The lambda function to be invoked when the item is clicked.
 * @param onDropdownMenuClick A lambda function invoked when a dropdown menu action is clicked.
 * @param showOverflow Whether overflow menu should be available or not.
 * @param showMoveToBitwarden Whether the option to move the item to Bitwarden is displayed.
 * @param cardStyle The card style to be applied to this item.
 * @param modifier The modifier for the item.
 */
@Suppress("LongMethod", "MagicNumber")
@Composable
fun VaultVerificationCodeItem(
    authCode: String,
    primaryLabel: String?,
    secondaryLabel: String?,
    periodSeconds: Int,
    timeLeftSeconds: Int,
    alertThresholdSeconds: Int,
    startIcon: IconData,
    onItemClick: () -> Unit,
    onDropdownMenuClick: (VaultDropdownMenuAction) -> Unit,
    showOverflow: Boolean,
    showMoveToBitwarden: Boolean,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .testTag(tag = "Item")
            .defaultMinSize(minHeight = 60.dp)
            .cardStyle(
                cardStyle = cardStyle,
                onClick = onItemClick,
                paddingStart = 16.dp,
                paddingEnd = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        BitwardenIcon(
            iconData = startIcon,
            tint = BitwardenTheme.colorScheme.icon.primary,
            modifier = Modifier.size(size = 24.dp),
        )

        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.weight(weight = 1f),
        ) {
            if (!primaryLabel.isNullOrEmpty()) {
                Text(
                    modifier = Modifier.testTag(tag = "Name"),
                    text = primaryLabel,
                    style = BitwardenTheme.typography.bodyLarge,
                    color = BitwardenTheme.colorScheme.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (!secondaryLabel.isNullOrEmpty()) {
                Text(
                    modifier = Modifier.testTag(tag = "Username"),
                    text = secondaryLabel,
                    style = BitwardenTheme.typography.bodyMedium,
                    color = BitwardenTheme.colorScheme.text.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        BitwardenCircularCountdownIndicator(
            modifier = Modifier.testTag(tag = "CircularCountDown"),
            timeLeftSeconds = timeLeftSeconds,
            periodSeconds = periodSeconds,
            alertThresholdSeconds = alertThresholdSeconds,
        )

        Text(
            modifier = Modifier.testTag(tag = "AuthCode"),
            text = authCode.chunked(size = 3).joinToString(separator = " "),
            style = BitwardenTheme.typography.sensitiveInfoSmall,
            color = BitwardenTheme.colorScheme.text.primary,
        )

        if (showOverflow) {
            BitwardenOverflowActionItem(
                contentDescription = stringResource(id = BitwardenString.more),
                menuItemDataList = persistentListOfNotNull(
                    OverflowMenuItemData(
                        text = stringResource(id = BitwardenString.copy),
                        onClick = { onDropdownMenuClick(VaultDropdownMenuAction.COPY_CODE) },
                    ),
                    OverflowMenuItemData(
                        text = stringResource(id = BitwardenString.edit),
                        onClick = { onDropdownMenuClick(VaultDropdownMenuAction.EDIT) },
                    ),
                    if (showMoveToBitwarden) {
                        OverflowMenuItemData(
                            text = stringResource(id = BitwardenString.copy_to_bitwarden_vault),
                            onClick = {
                                onDropdownMenuClick(VaultDropdownMenuAction.COPY_TO_BITWARDEN)
                            },
                        )
                    } else {
                        null
                    },
                    OverflowMenuItemData(
                        text = stringResource(id = BitwardenString.delete_item),
                        onClick = { onDropdownMenuClick(VaultDropdownMenuAction.DELETE) },
                    ),
                ),
                vectorIconRes = BitwardenDrawable.ic_ellipsis_horizontal,
                testTag = "Options",
            )
        } else {
            BitwardenStandardIconButton(
                vectorIconRes = BitwardenDrawable.ic_copy,
                contentDescription = stringResource(id = BitwardenString.copy),
                onClick = onItemClick,
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
            authCode = "1234567890".chunked(3).joinToString(" "),
            primaryLabel = "Issuer, AKA Name",
            secondaryLabel = "username@bitwarden.com",
            periodSeconds = 30,
            timeLeftSeconds = 15,
            alertThresholdSeconds = 7,
            startIcon = IconData.Local(BitwardenDrawable.ic_login_item),
            onItemClick = {},
            onDropdownMenuClick = {},
            showOverflow = true,
            modifier = Modifier.padding(horizontal = 16.dp),
            showMoveToBitwarden = true,
            cardStyle = CardStyle.Full,
        )
    }
}
