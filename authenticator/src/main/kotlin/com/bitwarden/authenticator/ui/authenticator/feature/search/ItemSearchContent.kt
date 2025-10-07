package com.bitwarden.authenticator.ui.authenticator.feature.search

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.authenticator.ui.authenticator.feature.model.SharedCodesDisplayState
import com.bitwarden.authenticator.ui.authenticator.feature.model.VerificationCodeDisplayItem
import com.bitwarden.authenticator.ui.authenticator.feature.search.handlers.SearchHandlers
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * The content state for the item search screen.
 */
@Composable
fun ItemSearchContent(
    viewState: ItemSearchState.ViewState.Content,
    searchHandlers: SearchHandlers,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item {
            Spacer(Modifier.height(height = 12.dp))
        }

        if (viewState.hasLocalAndSharedItems) {
            item {
                BitwardenListHeaderText(
                    label = viewState.localListHeader(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(height = 8.dp))
            }
        }

        itemsIndexed(viewState.itemList) { index, it ->
            VaultVerificationCodeItem(
                displayItem = it,
                onCopyClick = searchHandlers.onItemClick,
                cardStyle = viewState.itemList.toListItemCardStyle(index = index),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
        }

        if (viewState.hasLocalAndSharedItems) {
            item {
                Spacer(Modifier.height(height = 12.dp))
            }
        }

        sharedCodes(
            sharedItems = viewState.sharedItems,
            onCopyClick = searchHandlers.onItemClick,
        )

        item {
            Spacer(modifier = Modifier.height(height = 16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

private fun LazyListScope.sharedCodes(
    sharedItems: SharedCodesDisplayState,
    onCopyClick: (authCode: String) -> Unit,
) {
    when (sharedItems) {
        is SharedCodesDisplayState.Codes -> {
            sharedItems.sections.forEach { section ->
                item {
                    BitwardenListHeaderText(
                        label = section.label(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .standardHorizontalMargin()
                            .padding(horizontal = 16.dp),
                    )
                    Spacer(Modifier.height(height = 12.dp))
                }

                itemsIndexed(section.codes) { index, it ->
                    VaultVerificationCodeItem(
                        displayItem = it,
                        onCopyClick = onCopyClick,
                        cardStyle = section.codes.toListItemCardStyle(index = index),
                        modifier = Modifier
                            .fillMaxWidth()
                            // There is some built-in padding to the menu button that
                            // makes up the visual difference here.
                            .padding(start = 16.dp, end = 12.dp),
                    )
                }
            }
        }

        SharedCodesDisplayState.Error -> {
            item {
                Text(
                    text = stringResource(BitwardenString.shared_codes_error),
                    color = BitwardenTheme.colorScheme.text.secondary,
                    style = BitwardenTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun VaultVerificationCodeItem(
    displayItem: VerificationCodeDisplayItem,
    cardStyle: CardStyle,
    onCopyClick: (authCode: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    VaultVerificationCodeItem(
        authCode = displayItem.authCode,
        issuer = displayItem.title,
        periodSeconds = displayItem.periodSeconds,
        timeLeftSeconds = displayItem.timeLeftSeconds,
        alertThresholdSeconds = displayItem.alertThresholdSeconds,
        supportingLabel = displayItem.subtitle,
        startIcon = displayItem.startIcon,
        onCopyClick = { onCopyClick(displayItem.authCode) },
        onItemClick = { onCopyClick(displayItem.authCode) },
        cardStyle = cardStyle,
        modifier = modifier,
    )
}
