@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.platform.feature.premium.plan

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.annotatedPluralsResource
import com.bitwarden.ui.platform.base.util.annotatedStringResource
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.base.util.spanStyleOf
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.badge.BitwardenStatusBadge
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.bitwarden.ui.platform.components.button.model.BitwardenButtonData
import com.bitwarden.ui.platform.components.card.BitwardenInfoCalloutCard
import com.bitwarden.ui.platform.components.content.BitwardenContentBlock
import com.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.bitwarden.ui.platform.components.content.model.ContentBlockData
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.manager.intent.model.AuthTabData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenPlurals
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.billing.repository.model.PremiumSubscriptionStatus
import com.x8bit.bitwarden.ui.platform.composition.LocalAuthTabLaunchers
import com.x8bit.bitwarden.ui.platform.feature.premium.plan.PlanState.ViewState.Error.Type.PRICING_UNAVAILABLE
import com.x8bit.bitwarden.ui.platform.feature.premium.plan.PlanState.ViewState.Error.Type.SUBSCRIPTION
import com.x8bit.bitwarden.ui.platform.feature.premium.plan.handlers.PlanHandlers
import com.x8bit.bitwarden.ui.platform.feature.premium.plan.util.badgeColors
import com.x8bit.bitwarden.ui.platform.feature.premium.plan.util.labelRes
import com.x8bit.bitwarden.ui.platform.feature.premium.plan.util.showsFeatureList
import com.x8bit.bitwarden.ui.platform.model.AuthTabLaunchers
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private const val PLACEHOLDER_TEXT: String = "--"

/**
 * The screen for the plan — shows the upgrade flow for free users and the
 * subscription-management surface for premium users.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    onNavigateBack: () -> Unit,
    onNavigateToUpgradedToPremium: () -> Unit,
    viewModel: PlanViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
    authTabLaunchers: AuthTabLaunchers = LocalAuthTabLaunchers.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val handlers = remember(viewModel) { PlanHandlers.create(viewModel) }

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is PlanEvent.LaunchBrowser -> {
                intentManager.startAuthTab(
                    uri = event.url.toUri(),
                    authTabData = event.authTabData,
                    launcher = authTabLaunchers.premiumCheckout,
                )
            }

            is PlanEvent.LaunchPortal -> {
                intentManager.startAuthTab(
                    uri = event.url.toUri(),
                    authTabData = AuthTabData.CustomScheme(
                        callbackUrl = PREMIUM_CHECKOUT_CALLBACK_URL,
                    ),
                    launcher = authTabLaunchers.stripePortal,
                )
            }

            is PlanEvent.LaunchUri -> intentManager.launchUri(event.url.toUri())
            PlanEvent.NavigateBack -> onNavigateBack()
            PlanEvent.NavigateToUpgradedToPremium -> onNavigateToUpgradedToPremium()
        }
    }

    PlanDialogs(
        dialogState = state.dialogState,
        handlers = handlers,
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
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
            is PlanState.ViewState.Content.Free.Cloud -> {
                FreeCloudContent(
                    viewState = viewState,
                    handlers = handlers,
                )
            }

            is PlanState.ViewState.Content.Free.SelfHosted -> {
                FreeSelfHostedContent()
            }

            is PlanState.ViewState.Content.Premium -> {
                PremiumContent(
                    viewState = viewState,
                    handlers = handlers,
                )
            }

            is PlanState.ViewState.Error -> {
                BitwardenErrorContent(
                    illustrationData = IconData.Local(iconRes = BitwardenDrawable.ill_file_error),
                    message = viewState.message(),
                    buttonData = BitwardenButtonData(
                        label = BitwardenString.try_again.asText(),
                        onClick = {
                            when (viewState.type) {
                                PRICING_UNAVAILABLE -> handlers.onRetryPricingClick()
                                SUBSCRIPTION -> handlers.onRetrySubscriptionClick()
                            }
                        },
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is PlanState.ViewState.Loading -> {
                BitwardenLoadingContent(
                    text = viewState.message(),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun PlanDialogs(
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

        is PlanState.DialogState.CancelConfirmation -> {
            BitwardenTwoButtonDialog(
                title = stringResource(id = BitwardenString.continue_to_stripe),
                message = stringResource(
                    id = BitwardenString
                        .youll_be_taken_to_stripe_to_manage_your_subscription_cancellation,
                    dialogState.nextRenewalDate,
                ),
                confirmButtonText = stringResource(id = BitwardenString.continue_text),
                dismissButtonText = stringResource(id = BitwardenString.cancel),
                onConfirmClick = handlers.onConfirmCancelClick,
                onDismissClick = handlers.onDismissCancelConfirmation,
                onDismissRequest = handlers.onDismissCancelConfirmation,
            )
        }

        is PlanState.DialogState.PortalError -> {
            BitwardenTwoButtonDialog(
                title = stringResource(id = BitwardenString.portal_error),
                message = stringResource(id = BitwardenString.trouble_loading_portal),
                confirmButtonText = stringResource(id = BitwardenString.try_again),
                dismissButtonText = stringResource(id = BitwardenString.close),
                onConfirmClick = handlers.onRetryPortalClick,
                onDismissClick = handlers.onDismissPortalError,
                onDismissRequest = handlers.onDismissPortalError,
            )
        }

        PlanState.DialogState.LoadingPortal -> {
            BitwardenLoadingDialog(
                text = stringResource(id = BitwardenString.loading_portal),
            )
        }

        is PlanState.DialogState.Loading -> {
            BitwardenLoadingDialog(text = dialogState.message())
        }

        null -> Unit
    }
}

@Composable
private fun FreeCloudContent(
    viewState: PlanState.ViewState.Content.Free.Cloud,
    handlers: PlanHandlers,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        PremiumDetailsCard(
            modifier = Modifier.standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(height = 16.dp))

        // Hide the Upgrade Now CTA (and its Stripe footer copy) while a Stripe upgrade is
        // already in flight for the active user. CTAs reappear once the server flips the
        // user to Premium.
        if (!viewState.isPremiumUpgradePending) {
            UpgradeNowCallToAction(
                viewState = viewState,
                onUpgradeNowClick = handlers.onUpgradeNowClick,
            )
        }
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun UpgradeNowCallToAction(
    viewState: PlanState.ViewState.Content.Free.Cloud,
    onUpgradeNowClick: () -> Unit,
) {
    BitwardenFilledButton(
        label = stringResource(id = BitwardenString.upgrade_to_premium),
        onClick = onUpgradeNowClick,
        icon = rememberVectorPainter(id = BitwardenDrawable.ic_external_link),
        modifier = Modifier
            .standardHorizontalMargin()
            .fillMaxWidth()
            .testTag("UpgradeNowButton"),
    )
    Spacer(modifier = Modifier.height(height = 12.dp))

    PriceRow(
        rate = viewState.rate,
        modifier = Modifier
            .fillMaxWidth()
            .standardHorizontalMargin(),
    )
    Spacer(modifier = Modifier.height(height = 12.dp))

    Text(
        text = stringResource(
            id = BitwardenString.youll_complete_the_purchase_with_stripe_secure_checkout,
        ),
        style = BitwardenTheme.typography.bodyMedium,
        color = BitwardenTheme.colorScheme.text.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .standardHorizontalMargin()
            .testTag("StripeFooterText"),
    )
    Spacer(modifier = Modifier.height(height = 16.dp))
}

@Composable
private fun PriceRow(
    rate: String,
    modifier: Modifier = Modifier,
) {
    val formattedContentDescription = stringResource(
        id = BitwardenString.per_month_cancel_anytime_content_description,
        formatArgs = arrayOf(rate),
    )
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = formattedContentDescription
            },
    ) {
        Text(
            text = rate,
            style = BitwardenTheme.typography.labelMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .semantics { hideFromAccessibility() }
                .alignByBaseline(),
        )
        Spacer(modifier = Modifier.width(width = 4.dp))
        Text(
            text = stringResource(id = BitwardenString.per_month_cancel_anytime),
            style = BitwardenTheme.typography.labelSmall,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .semantics { hideFromAccessibility() }
                .alignByBaseline(),
        )
    }
}

@Suppress("MaxLineLength")
@Composable
private fun FreeSelfHostedContent(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        BitwardenInfoCalloutCard(
            text = stringResource(
                id = BitwardenString
                    .to_manage_your_premium_subscription_youll_need_to_login_to_your_web_vault_on_a_computer,
            ),
            startIcon = IconData.Local(iconRes = BitwardenDrawable.ic_info_circle),
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth()
                .testTag("SelfHostedManageOnWebVaultCallout"),
        )
        Spacer(modifier = Modifier.height(16.dp))
        PremiumFeaturesCard(
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun PremiumFeaturesCard(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .cardStyle(
                cardStyle = CardStyle.Full,
                // Override bottom padding to account for custom
                // `BitwardenContentBlock` vertical padding, below.
                paddingBottom = 0.dp,
            ),
    ) {
        Text(
            text = stringResource(id = BitwardenString.unlock_premium_features),
            style = BitwardenTheme.typography.labelLarge,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .standardHorizontalMargin(),
        )

        PremiumFeatureRows()
    }
}

@Composable
private fun ColumnScope.PremiumFeatureRows() {
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
            modifier = Modifier
                .defaultMinSize(minHeight = 60.dp)
                .padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun PremiumDetailsCard(
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .run {
                if (BitwardenTheme.colorScheme.isDynamicTheme) {
                    cardStyle(cardStyle = CardStyle.Full, paddingVertical = 0.dp)
                } else {
                    paint(
                        painter = painterResource(id = BitwardenDrawable.bg_card_gradient),
                        contentScale = ContentScale.FillBounds,
                    )
                }
            },
    ) {
        Spacer(modifier = Modifier.height(height = 8.dp))
        Image(
            painter = painterResource(id = BitwardenDrawable.img_premium),
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(height = 8.dp))
        Text(
            text = stringResource(id = BitwardenString.unlock_advanced_protection),
            style = BitwardenTheme.typography.headlineSmall,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(height = 12.dp))
        Benefits(modifier = Modifier.standardHorizontalMargin())
        Spacer(modifier = Modifier.height(height = 4.dp))
    }
}

@Composable
private fun Benefits(
    modifier: Modifier = Modifier,
    benefits: ImmutableList<Text> = persistentListOf(
        BitwardenString.breeze_through_2fa_with_built_in_codes.asText(),
        BitwardenString.run_reports_to_find_risky_passwords.asText(),
        BitwardenString.keep_documents_safe_and_encrypted.asText(),
        BitwardenString.add_a_trusted_emergency_contact.asText(),
        BitwardenString.identify_unsecure_websites.asText(),
        BitwardenString.flag_accounts_with_inactive_2fa.asText(),
        BitwardenString.share_files_securely_with_anyone_using_send.asText(),
        BitwardenString.receive_24_7_priority_support.asText(),
    ),
) {
    Column(modifier = Modifier.width(intrinsicSize = IntrinsicSize.Max)) {
        benefits.forEach {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier,
            ) {
                Icon(
                    painter = rememberVectorPainter(id = BitwardenDrawable.ic_checkmark_small),
                    contentDescription = null,
                    tint = BitwardenTheme.colorScheme.icon.secondary,
                )
                Spacer(modifier = Modifier.width(width = 8.dp))
                Text(
                    text = it(),
                    style = BitwardenTheme.typography.bodyMedium,
                    color = BitwardenTheme.colorScheme.text.primary,
                )
            }
            Spacer(modifier = Modifier.height(height = 12.dp))
        }
    }
}

@Composable
private fun PremiumContent(
    viewState: PlanState.ViewState.Content.Premium,
    handlers: PlanHandlers,
    modifier: Modifier = Modifier,
) {
    var shouldShowManagePlanDialog by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        SubscriptionCard(
            viewState = viewState,
            modifier = Modifier.standardHorizontalMargin(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        BitwardenFilledButton(
            label = stringResource(id = BitwardenString.manage_plan),
            onClick = { shouldShowManagePlanDialog = true },
            icon = rememberVectorPainter(id = BitwardenDrawable.ic_external_link),
            isExternalLink = true,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth()
                .testTag("ManagePlanButton"),
        )

        if (viewState.showCancelButton) {
            Spacer(modifier = Modifier.height(12.dp))
            BitwardenOutlinedButton(
                label = stringResource(id = BitwardenString.cancel_premium),
                onClick = handlers.onCancelPremiumClick,
                icon = rememberVectorPainter(id = BitwardenDrawable.ic_external_link),
                isExternalLink = true,
                modifier = Modifier
                    .standardHorizontalMargin()
                    .fillMaxWidth()
                    .testTag("CancelPremiumButton"),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }

    if (shouldShowManagePlanDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = BitwardenString.continue_to_web_app),
            message = stringResource(
                id = BitwardenString.manage_your_subscription_plan_in_the_bitwarden_web_app,
            ),
            confirmButtonText = stringResource(id = BitwardenString.continue_text),
            dismissButtonText = stringResource(id = BitwardenString.cancel),
            onConfirmClick = {
                shouldShowManagePlanDialog = false
                handlers.onManagePlanClick()
            },
            onDismissClick = { shouldShowManagePlanDialog = false },
            onDismissRequest = { shouldShowManagePlanDialog = false },
        )
    }
}

@Composable
private fun SubscriptionCard(
    viewState: PlanState.ViewState.Content.Premium,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .cardStyle(
                cardStyle = CardStyle.Full,
                // Override bottom padding; the final row (line item or feature) owns its
                // own spacing.
                paddingBottom = 0.dp,
            ),
    ) {
        SubscriptionHeader(
            status = viewState.status,
            nextChargeTotalText = viewState.nextChargeTotalText,
            nextChargeDateText = viewState.nextChargeDateText,
            cancelAtDateText = viewState.cancelAtDateText,
            canceledDateText = viewState.canceledDateText,
            suspensionDateText = viewState.suspensionDateText,
            gracePeriodDays = viewState.gracePeriodDays,
            modifier = Modifier.standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(height = 16.dp))

        if (viewState.status?.showsFeatureList() == true) {
            PremiumFeatureRows()
        } else {
            SubscriptionLineItems(viewState = viewState)
        }
    }
}

@Composable
private fun ColumnScope.SubscriptionLineItems(
    viewState: PlanState.ViewState.Content.Premium,
) {
    BitwardenHorizontalDivider()
    SubscriptionLineItem(
        label = stringResource(id = BitwardenString.billing_amount),
        value = viewState.billingAmountText(),
        testTag = "BillingAmountRow",
        modifier = Modifier
            .fillMaxWidth()
            .standardHorizontalMargin(),
    )

    viewState.storageCostText?.let { storageCostText ->
        BitwardenHorizontalDivider(modifier = Modifier.padding(start = 16.dp))
        SubscriptionLineItem(
            label = stringResource(id = BitwardenString.storage_cost),
            value = storageCostText,
            testTag = "StorageCostRow",
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    viewState.discountAmountText?.let { discountAmountText ->
        BitwardenHorizontalDivider(modifier = Modifier.padding(start = 16.dp))
        SubscriptionLineItem(
            label = stringResource(id = BitwardenString.discount),
            value = discountAmountText,
            testTag = "DiscountRow",
            valueColor = BitwardenTheme.colorScheme.statusBadge.success.text,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    BitwardenHorizontalDivider(modifier = Modifier.padding(start = 16.dp))
    SubscriptionLineItem(
        label = stringResource(id = BitwardenString.estimated_tax),
        value = viewState.estimatedTaxText,
        testTag = "EstimatedTaxRow",
        modifier = Modifier
            .fillMaxWidth()
            .standardHorizontalMargin(),
    )

    BitwardenHorizontalDivider(modifier = Modifier.padding(start = 16.dp))
    SubscriptionLineItem(
        label = stringResource(id = BitwardenString.total),
        value = viewState.totalText(),
        testTag = "TotalRow",
        labelStyle = BitwardenTheme.typography.bodyLargeEmphasis,
        modifier = Modifier
            .fillMaxWidth()
            .standardHorizontalMargin(),
    )
}

@Composable
private fun SubscriptionHeader(
    status: PremiumSubscriptionStatus?,
    nextChargeTotalText: String?,
    nextChargeDateText: String?,
    cancelAtDateText: String?,
    canceledDateText: String?,
    suspensionDateText: String?,
    gracePeriodDays: Int?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(id = BitwardenString.premium_plan_name),
                style = BitwardenTheme.typography.titleExtraLarge,
                color = BitwardenTheme.colorScheme.text.primary,
            )
            status?.let {
                Spacer(modifier = Modifier.width(8.dp))
                BitwardenStatusBadge(
                    label = stringResource(id = it.labelRes()),
                    colors = it.badgeColors(),
                )
            }
        }

        val descriptionText = subscriptionDescriptionText(
            status = status,
            nextChargeTotalText = nextChargeTotalText,
            nextChargeDateText = nextChargeDateText,
            cancelAtDateText = cancelAtDateText,
            canceledDateText = canceledDateText,
            suspensionDateText = suspensionDateText,
            gracePeriodDays = gracePeriodDays,
        )

        descriptionText?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it,
                style = BitwardenTheme.typography.labelLargeRegular,
                color = BitwardenTheme.colorScheme.text.secondary,
            )
        }
    }
}

@Suppress("CyclomaticComplexMethod", "LongMethod")
@Composable
private fun subscriptionDescriptionText(
    status: PremiumSubscriptionStatus?,
    nextChargeTotalText: String?,
    nextChargeDateText: String?,
    cancelAtDateText: String?,
    canceledDateText: String?,
    suspensionDateText: String?,
    gracePeriodDays: Int?,
): AnnotatedString? {
    val baseStyle = spanStyleOf(
        color = BitwardenTheme.colorScheme.text.secondary,
        textStyle = BitwardenTheme.typography.labelLargeRegular,
    )
    val emphasisStyle = spanStyleOf(
        color = BitwardenTheme.colorScheme.text.secondary,
        textStyle = BitwardenTheme.typography.labelLargeEmphasis,
    )
    return when (status) {
        PremiumSubscriptionStatus.ACTIVE -> annotatedStringResource(
            id = BitwardenString.premium_next_charge_summary,
            args = arrayOf(
                nextChargeTotalText ?: PLACEHOLDER_TEXT,
                nextChargeDateText ?: PLACEHOLDER_TEXT,
            ),
            style = baseStyle,
            emphasisHighlightStyle = emphasisStyle,
        )

        PremiumSubscriptionStatus.CANCELED -> annotatedStringResource(
            id = BitwardenString.subscription_canceled_description,
            args = arrayOf(canceledDateText ?: suspensionDateText ?: PLACEHOLDER_TEXT),
            style = baseStyle,
            emphasisHighlightStyle = emphasisStyle,
        )

        PremiumSubscriptionStatus.PENDING_CANCELLATION -> annotatedStringResource(
            id = BitwardenString.subscription_pending_cancellation_description,
            args = arrayOf(cancelAtDateText ?: PLACEHOLDER_TEXT),
            style = baseStyle,
            emphasisHighlightStyle = emphasisStyle,
        )

        PremiumSubscriptionStatus.UNPAID -> annotatedStringResource(
            id = BitwardenString.subscription_unpaid_description,
            args = arrayOf(suspensionDateText ?: PLACEHOLDER_TEXT),
            style = baseStyle,
            emphasisHighlightStyle = emphasisStyle,
        )

        PremiumSubscriptionStatus.UPDATE_PAYMENT -> annotatedStringResource(
            id = BitwardenString.subscription_update_payment_description,
            args = arrayOf(suspensionDateText ?: PLACEHOLDER_TEXT),
            style = baseStyle,
            emphasisHighlightStyle = emphasisStyle,
        )

        PremiumSubscriptionStatus.PAST_DUE -> {
            val days = gracePeriodDays ?: 0
            annotatedPluralsResource(
                id = BitwardenPlurals.subscription_past_due_description,
                quantity = days,
                days.toString(),
                suspensionDateText ?: PLACEHOLDER_TEXT,
                style = baseStyle,
                emphasisHighlightStyle = emphasisStyle,
            )
        }

        PremiumSubscriptionStatus.PAUSED -> AnnotatedString(
            stringResource(id = BitwardenString.subscription_paused_description),
        )

        PremiumSubscriptionStatus.EXPIRED -> annotatedStringResource(
            id = BitwardenString.subscription_expired_description,
            args = arrayOf(suspensionDateText ?: PLACEHOLDER_TEXT),
            style = baseStyle,
            emphasisHighlightStyle = emphasisStyle,
        )

        null -> null
    }
}

@Composable
private fun SubscriptionLineItem(
    label: String,
    value: String,
    testTag: String,
    modifier: Modifier = Modifier,
    labelStyle: TextStyle = BitwardenTheme.typography.bodyLarge,
    labelColor: Color = BitwardenTheme.colorScheme.text.secondary,
    valueStyle: TextStyle = BitwardenTheme.typography.bodyMedium,
    valueColor: Color = BitwardenTheme.colorScheme.text.primary,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .defaultMinSize(minHeight = 60.dp)
            .padding(vertical = 16.dp)
            .testTag(tag = testTag),
    ) {
        Text(
            text = label,
            style = labelStyle,
            color = labelColor,
        )
        Text(
            text = value,
            style = valueStyle,
            color = valueColor,
        )
    }
}

@Preview
@OmitFromCoverage
@Composable
private fun PlanScreenFreeCloudAccount_preview() {
    BitwardenTheme {
        BitwardenScaffold {
            FreeCloudContent(
                viewState = PlanState.ViewState.Content.Free.Cloud(
                    rate = "$1.67",
                    checkoutUrl = null,
                    isAwaitingPremiumStatus = false,
                    isPremiumUpgradePending = false,
                ),
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
                    onManagePlanClick = {},
                    onCancelPremiumClick = {},
                    onConfirmCancelClick = {},
                    onDismissCancelConfirmation = {},
                    onDismissPortalError = {},
                    onRetryPortalClick = {},
                    onRetrySubscriptionClick = {},
                ),
            )
        }
    }
}

@Preview
@OmitFromCoverage
@Composable
private fun PlanScreenFreeSelfHostedFreeAccount_preview() {
    BitwardenTheme {
        BitwardenScaffold {
            FreeSelfHostedContent()
        }
    }
}

@Preview
@OmitFromCoverage
@Composable
private fun PlanScreenPremiumAccount_preview() {
    BitwardenTheme {
        BitwardenScaffold {
            PremiumContent(
                viewState = PlanState.ViewState.Content.Premium(
                    status = PremiumSubscriptionStatus.ACTIVE,
                    billingAmountText = BitwardenString.billing_rate_per_year.asText("$19.80"),
                    storageCostText = "$24.00",
                    discountAmountText = "-$2.10",
                    estimatedTaxText = "$3.85",
                    totalText = BitwardenString.billing_rate_per_year.asText("$45.55"),
                    nextChargeTotalText = "$45.55",
                    nextChargeDateText = "April 2, 2026",
                    showCancelButton = true,
                ),
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
                    onManagePlanClick = {},
                    onCancelPremiumClick = {},
                    onConfirmCancelClick = {},
                    onDismissCancelConfirmation = {},
                    onDismissPortalError = {},
                    onRetryPortalClick = {},
                    onRetrySubscriptionClick = {},
                ),
            )
        }
    }
}

@Preview
@OmitFromCoverage
@Composable
private fun PlanScreenPremiumAccountZeroState_preview() {
    BitwardenTheme {
        BitwardenScaffold {
            PremiumContent(
                viewState = PlanState.ViewState.Content.Premium(
                    status = PremiumSubscriptionStatus.ACTIVE,
                    billingAmountText = BitwardenString.billing_rate_per_year.asText("$19.80"),
                    storageCostText = null,
                    discountAmountText = null,
                    estimatedTaxText = "$0.00",
                    totalText = BitwardenString.billing_rate_per_year.asText("$19.80"),
                    nextChargeTotalText = "$19.80",
                    nextChargeDateText = "April 2, 2026",
                    showCancelButton = true,
                ),
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
                    onManagePlanClick = {},
                    onCancelPremiumClick = {},
                    onConfirmCancelClick = {},
                    onDismissCancelConfirmation = {},
                    onDismissPortalError = {},
                    onRetryPortalClick = {},
                    onRetrySubscriptionClick = {},
                ),
            )
        }
    }
}
