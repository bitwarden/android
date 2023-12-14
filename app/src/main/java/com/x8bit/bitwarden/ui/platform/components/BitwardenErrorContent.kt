package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R

/**
 * A Bitwarden-themed, re-usable error state.
 *
 * Note that when [onTryAgainClick] is absent, there will be no "Try again" button displayed.
 */
@Composable
fun BitwardenErrorContent(
    message: String,
    modifier: Modifier = Modifier,
    onTryAgainClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        onTryAgainClick?.let {
            Spacer(modifier = Modifier.height(16.dp))
            BitwardenTextButton(
                label = stringResource(id = R.string.try_again),
                onClick = it,
            )
        }
    }
}
