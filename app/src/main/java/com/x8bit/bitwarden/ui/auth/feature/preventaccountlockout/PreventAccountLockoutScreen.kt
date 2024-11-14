package com.x8bit.bitwarden.ui.auth.feature.preventaccountlockout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Top level screen component for the prevent account lockout info screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun PreventAccountLockoutScreen(
    onNavigateBack: () -> Unit,
    viewModel: PreventAccountLockoutViewModel = hiltViewModel(),
) {
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            PreventAccountLockoutEvent.NavigateBack -> onNavigateBack()
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(R.string.prevent_account_lockout),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    {
                        viewModel.trySendAction(PreventAccountLockoutAction.CloseClickAction)
                    }
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .verticalScroll(rememberScrollState()),
        ) {
            NeverLoseAccessContent()
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun NeverLoseAccessContent(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(size = 4.dp))
            .background(BitwardenTheme.colorScheme.background.tertiary),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.never_lose_access_to_your_vault),
            style = BitwardenTheme.typography.titleMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                R.string.the_best_way_to_make_sure_you_can_always_access_your_account,
            ),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        BitwardenHorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        AccountRecoveryTipRow(
            title = stringResource(R.string.create_a_hint),
            description = stringResource(
                R.string.your_hint_will_be_send_to_you_via_email_when_you_request_it,
            ),
            icon = rememberVectorPainter(id = R.drawable.ic_light_bulb),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        BitwardenHorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        AccountRecoveryTipRow(
            title = stringResource(R.string.write_your_password_down),
            description = stringResource(R.string.keep_it_secret_keep_it_safe),
            icon = rememberVectorPainter(id = R.drawable.ic_pencil),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AccountRecoveryTipRow(
    title: String,
    description: String,
    icon: Painter,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = BitwardenTheme.colorScheme.icon.primary,
            modifier = Modifier
                .size(32.dp)
                .clearAndSetSemantics { },
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = BitwardenTheme.typography.titleSmall,
                color = BitwardenTheme.colorScheme.text.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = BitwardenTheme.typography.bodyMedium,
                color = BitwardenTheme.colorScheme.text.primary,
            )
        }
    }
}

@Preview
@Composable
private fun PreventAccountLockoutScreenPreview() {
    BitwardenTheme {
        PreventAccountLockoutScreen(onNavigateBack = {})
    }
}
