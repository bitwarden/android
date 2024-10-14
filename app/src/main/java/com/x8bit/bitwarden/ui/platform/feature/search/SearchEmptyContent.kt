package com.x8bit.bitwarden.ui.platform.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * The empty state for the search screen.
 */
@Composable
fun SearchEmptyContent(
    viewState: SearchState.ViewState.Empty,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        viewState.message?.let {
            Text(
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .testTag("NoSearchResultsLabel")
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                text = it(),
                style = BitwardenTheme.typography.bodyMedium,
            )
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
