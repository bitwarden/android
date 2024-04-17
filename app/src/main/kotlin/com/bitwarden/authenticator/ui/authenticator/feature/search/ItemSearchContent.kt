package com.bitwarden.authenticator.ui.authenticator.feature.search

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bitwarden.authenticator.ui.authenticator.feature.search.handlers.SearchHandlers

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
        items(viewState.displayItems) {
            VaultVerificationCodeItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        // There is some built-in padding to the menu button that makes up
                        // the visual difference here.
                        end = 12.dp,
                    ),
                authCode = it.authCode,
                issuer = it.issuer,
                periodSeconds = it.periodSeconds,
                timeLeftSeconds = it.timeLeftSeconds,
                alertThresholdSeconds = it.alertThresholdSeconds,
                supportingLabel = it.supportingLabel,
                startIcon = it.startIcon,
                onCopyClick = { searchHandlers.onItemClick(it.id) },
                onItemClick = { searchHandlers.onItemClick(it.id) },
            )
        }

        item {
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
