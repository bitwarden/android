package com.x8bit.bitwarden.ui.platform.components.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.components.indicator.BitwardenCircularProgressIndicator
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A Bitwarden-themed, re-usable loading state.
 */
@Composable
fun BitwardenLoadingContent(
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        message?.let {
            Text(
                text = it,
                style = BitwardenTheme.typography.titleMedium,
                // setting color explicitly here as we can't assume what the surface will be.
                color = BitwardenTheme.colorScheme.text.primary,
            )
            Spacer(Modifier.height(16.dp))
        }
        BitwardenCircularProgressIndicator()
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
