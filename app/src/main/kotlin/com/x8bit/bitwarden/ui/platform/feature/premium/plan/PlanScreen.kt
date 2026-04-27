package com.x8bit.bitwarden.ui.platform.feature.premium.plan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.content.BitwardenContentBlock
import com.bitwarden.ui.platform.components.content.model.ContentBlockData
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.composition.LocalAuthTabLaunchers
import com.x8bit.bitwarden.ui.platform.feature.premium.plan.handlers.PlanHandlers
import com.x8bit.bitwarden.ui.platform.model.AuthTabLaunchers

/**
 * The screen for the plan — shows the upgrade flow for free users.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    onNavigateBack: () -> Unit,
    viewModel: PlanViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
    authTabLaunchers: AuthTabLaunchers = LocalAuthTabLaunchers.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val handlers = remember(viewModel) { PlanHandlers.create(viewModel) }
    val snackbarHostState = rememberBitwardenSnackbarHostState()

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is PlanEvent.LaunchBrowser -> {
                intentManager.startAuthTab(
                    uri = event.url.toUri(),
                    authTabData = event.authTabData,
                    launcher = authTabLaunchers.premiumCheckout,
                )
            }

            PlanEvent.NavigateBack -> onNavigateBack()
            is PlanEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.data)
        }
    }

    FreeDialogs(
        dialogState = state.dialogState,
        handlers = handlers,
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = {
            BitwardenSnackbarHost(bitwardenHostState = snackbarHostState)
        },
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = state.title),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = state.navigationIcon),
                navigationIconContentDescription = stringResource(
                    id = state.navigationIconContentDescription,
                ),
                onNavigationIconClick = handlers.onBackClick,
            )
        },
    ) {
        when (val viewState = state.viewState) {
            is PlanState.ViewState.Free -> {
                FreeContent(
                    viewState = viewState,
                    isDialogShowing = state.dialogState != null,
                    handlers = handlers,
                )
            }

            PlanState.ViewState.Premium -> {
                PremiumContent(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun PremiumContent(
    modifier: Modifier = Modifier,
) {
    // TODO(PM-35455): Render the premium subscription management UI —
    // status badge, next-charge summary, billing / storage / discount /
    // tax line items, and manage plan / cancel actions — once the
    // subscription fetch path is wired up.
    Spacer(modifier = modifier)
}

@Composable
private fun FreeDialogs(
    dialogState: PlanState.DialogState?,
    handlers: PlanHandlers,
) {
    when (dialogState) {
        is PlanState.DialogState.CheckoutError -> {
            BitwardenTwoButtonDialog(
                title = stringResource(id = BitwardenString.secure_checkout_didnt_load),
                message = stringResource(id = BitwardenString.trouble_opening_payment_page),
                confirmButtonText = stringResource(id = BitwardenString.try_again),
                dismissButtonText = stringResource(id = BitwardenString.close),
                onConfirmClick = handlers.onRetryClick,
                onDismissClick = handlers.onDismissError,
                onDismissRequest = handlers.onDismissError,
            )
        }

        is PlanState.DialogState.GetPricingError -> {
            BitwardenTwoButtonDialog(
                title = dialogState.title(),
                message = dialogState.message(),
                confirmButtonText = stringResource(BitwardenString.try_again),
                dismissButtonText = stringResource(BitwardenString.close),
                onConfirmClick = handlers.onRetryPricingClick,
                onDismissClick = handlers.onClosePricingErrorClick,
                onDismissRequest = handlers.onClosePricingErrorClick,
            )
        }

        is PlanState.DialogState.WaitingForPayment -> {
            BitwardenTwoButtonDialog(
                title = stringResource(id = BitwardenString.payment_not_received_yet),
                message = stringResource(id = BitwardenString.return_to_stripe_to_finish),
                confirmButtonText = stringResource(id = BitwardenString.go_back),
                dismissButtonText = stringResource(id = BitwardenString.close),
                onConfirmClick = handlers.onGoBackClick,
                onDismissClick = handlers.onCancelWaiting,
                onDismissRequest = handlers.onCancelWaiting,
            )
        }

        is PlanState.DialogState.PendingUpgrade -> {
            BitwardenTwoButtonDialog(
                title = stringResource(id = BitwardenString.upgrade_pending),
                message = stringResource(
                    id = BitwardenString.upgrade_pending_message,
                ),
                confirmButtonText = stringResource(id = BitwardenString.sync_now),
                dismissButtonText = stringResource(id = BitwardenString.continue_text),
                onConfirmClick = handlers.onSyncClick,
                onDismissClick = handlers.onContinueClick,
                onDismissRequest = handlers.onContinueClick,
            )
        }

        is PlanState.DialogState.Loading -> {
            BitwardenLoadingDialog(text = dialogState.message())
        }

        null -> Unit
    }
}

@Composable
private fun FreeContent(
    viewState: PlanState.ViewState.Free,
    isDialogShowing: Boolean,
    handlers: PlanHandlers,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(12.dp))
        PremiumDetailsCard(
            rate = viewState.rate,
            frequency = stringResource(id = BitwardenString.per_month),
            modifier = Modifier.standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        BitwardenFilledButton(
            label = stringResource(id = BitwardenString.upgrade_now),
            onClick = handlers.onUpgradeNowClick,
            isEnabled = !isDialogShowing,
            icon = rememberVectorPainter(id = BitwardenDrawable.ic_external_link),
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth()
                .testTag("UpgradeNowButton"),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(id = BitwardenString.stripe_checkout_footer),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .testTag("StripeFooterText"),
        )

        Spacer(modifier = Modifier.height(16.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun PremiumDetailsCard(
    rate: String,
    frequency: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .cardStyle(
                cardStyle = CardStyle.Full,
                // Override bottom padding to account for custom
                // `BitwardenContentBlock` vertical padding, below.
                paddingBottom = 0.dp,
            ),
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .standardHorizontalMargin(),
        ) {
            PriceRow(
                rate = rate,
                frequency = frequency,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    id = BitwardenString.unlock_premium_features,
                ),
                style = BitwardenTheme.typography.bodyMedium,
                color = BitwardenTheme.colorScheme.text.secondary,
            )
        }

        BitwardenHorizontalDivider()

        val features = listOf(
            BitwardenString.built_in_authenticator,
            BitwardenString.emergency_access,
            BitwardenString.secure_file_storage,
            BitwardenString.breach_monitoring,
        )
        features.forEachIndexed { index, featureStringRes ->
            BitwardenContentBlock(
                data = ContentBlockData(
                    headerText = stringResource(id = featureStringRes),
                    iconVectorResource = BitwardenDrawable.ic_check_mark,
                ),
                headerTextStyle = BitwardenTheme.typography.titleMedium,
                showDivider = index != features.lastIndex,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun PriceRow(
    rate: String,
    frequency: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        Text(
            text = rate,
            style = BitwardenTheme.typography.headlineMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier.alignByBaseline(),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = frequency,
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.secondary,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

@Preview
@OmitFromCoverage
@Composable
private fun PlanScreenFreeAccount_preview() {
    BitwardenTheme {
        BitwardenScaffold {
            FreeContent(
                viewState = PlanState.ViewState.Free(
                    rate = "$1.67",
                    checkoutUrl = null,
                    isAwaitingPremiumStatus = false,
                ),
                isDialogShowing = false,
                handlers = PlanHandlers(
                    onBackClick = {},
                    onUpgradeNowClick = {},
                    onDismissError = {},
                    onRetryClick = {},
                    onRetryPricingClick = {},
                    onClosePricingErrorClick = {},
                    onCancelWaiting = {},
                    onGoBackClick = {},
                    onSyncClick = {},
                    onContinueClick = {},
                ),
            )
        }
    }
}
