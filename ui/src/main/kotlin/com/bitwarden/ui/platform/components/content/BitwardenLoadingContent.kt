package com.bitwarden.ui.platform.components.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.components.indicator.BitwardenCircularProgressIndicator
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A Bitwarden-themed, re-usable loading state.
 */
@Composable
fun BitwardenLoadingContent(
    modifier: Modifier = Modifier,
    text: String? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        text?.let {
            Text(
                text = it,
                style = BitwardenTheme.typography.titleMedium,
                // setting color explicitly here as we can't assume what the surface will be.
                color = BitwardenTheme.colorScheme.text.primary,
                modifier = Modifier.testTag(tag = "AlertTitleText"),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        BitwardenCircularProgressIndicator(
            modifier = Modifier
                .size(48.dp)
                .testTag(tag = "AlertProgressIndicator"),
        )
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
