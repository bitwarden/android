package com.x8bit.bitwarden.ui.platform.components.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.x8bit.bitwarden.ui.platform.components.indicator.BitwardenCircularProgressIndicator

/**
 * A Bitwarden-themed, re-usable loading state.
 */
@Composable
fun BitwardenLoadingContent(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BitwardenCircularProgressIndicator()
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
