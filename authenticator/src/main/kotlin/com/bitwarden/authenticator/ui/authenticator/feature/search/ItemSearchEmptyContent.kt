package com.bitwarden.authenticator.ui.authenticator.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * The empty state for the item search screen.
 */
@Composable
fun ItemSearchEmptyContent(
    viewState: ItemSearchState.ViewState.Empty,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(id = BitwardenDrawable.ic_search_wide),
            contentDescription = null,
            tint = BitwardenTheme.colorScheme.icon.primary,
            modifier = Modifier
                .size(74.dp)
                .standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        viewState.message?.let {
            Text(
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .testTag("NoSearchResultsLabel")
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
                text = it(),
                style = BitwardenTheme.typography.bodyMedium,
            )
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
