package com.bitwarden.authenticator.ui.authenticator.feature.search

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.authenticator.feature.model.SharedCodesDisplayState
import com.bitwarden.authenticator.ui.authenticator.feature.model.VerificationCodeDisplayItem
import com.bitwarden.authenticator.ui.authenticator.feature.search.handlers.SearchHandlers
import com.bitwarden.authenticator.ui.platform.components.header.BitwardenListHeaderText

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
        if (viewState.hasLocalAndSharedItems) {
            item {
                BitwardenListHeaderText(
                    label = viewState.localListHeader(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        items(viewState.itemList) {
            VaultVerificationCodeItem(
                displayItem = it,
                onCopyClick = searchHandlers.onItemClick,
                modifier = Modifier
                    .fillMaxWidth()
                    // There is some built-in padding to the menu button that
                    // makes up the visual difference here.
                    .padding(start = 16.dp, end = 12.dp),
            )
        }

        if (viewState.hasLocalAndSharedItems) {
            item {
                Spacer(Modifier.height(height = 8.dp))
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
                            .padding(horizontal = 16.dp),
                    )
                }

                items(section.codes) {
                    VaultVerificationCodeItem(
                        displayItem = it,
                        onCopyClick = onCopyClick,
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
                    text = stringResource(R.string.shared_codes_error),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun VaultVerificationCodeItem(
    displayItem: VerificationCodeDisplayItem,
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
        modifier = modifier,
    )
}
