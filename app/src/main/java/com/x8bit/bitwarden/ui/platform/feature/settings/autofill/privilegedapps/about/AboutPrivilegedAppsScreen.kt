package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenTextRow
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Top level composable for the About Privileged Apps screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutPrivilegedAppsScreen(
    onNavigateBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(R.string.about_privileged_applications),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_back),
                navigationIconContentDescription = stringResource(id = R.string.back),
                onNavigationIconClick = remember { onNavigateBack },
            )
        },
    ) {
        AboutPrivilegedAppsContent()
    }
}

@Composable
private fun AboutPrivilegedAppsContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .standardHorizontalMargin(),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.privileged_apps_description),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column {
            BitwardenTextRow(
                text = stringResource(R.string.trusted_by_you),
                cardStyle = CardStyle.Top(),
                onClick = {},
                description = stringResource(R.string.trusted_by_you_learn_more),
            )
            BitwardenTextRow(
                text = stringResource(R.string.trusted_by_the_community),
                cardStyle = CardStyle.Middle(),
                onClick = {},
                description = stringResource(R.string.trusted_by_community_learn_more),
            )
            BitwardenTextRow(
                text = stringResource(R.string.trusted_by_google),
                cardStyle = CardStyle.Bottom,
                onClick = {},
                description = stringResource(R.string.trusted_by_google_learn_more),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutPrivilegedAppsContent_Preview() {
    BitwardenTheme {
        AboutPrivilegedAppsContent()
    }
}
