package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.authenticator.R

/**
 * Show a snackbar that says "Account synced from Bitwarden app" with a close action.
 *
 * @param state Snackbar state used to show/hide. The message and title from this state are unused.
 */
@Composable
fun FirstTimeSyncSnackbarHost(
    state: SnackbarHostState,
) {
    SnackbarHost(
        hostState = state,
        snackbar = {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.inverseSurface,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .shadow(elevation = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier
                        .padding(16.dp)
                        .weight(1f, fill = true),
                    text = stringResource(R.string.account_synced_from_bitwarden_app),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                )
                IconButton(
                    onClick = { state.currentSnackbarData?.dismiss() },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = stringResource(id = R.string.close),
                        tint = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier
                            .size(24.dp),
                    )
                }
            }
        },
    )
}
