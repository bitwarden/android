package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.authenticator.R

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
            painter = painterResource(id = R.drawable.ic_search_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(74.dp)
                .padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        viewState.message?.let {
            Text(
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .semantics { testTag = "NoSearchResultsLabel" }
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                text = it(),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
