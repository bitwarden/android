package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.handlers

import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.AutoFillAction
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.AutoFillViewModel
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.AutofillStyle

/**
 * Handlers for the AutoFill screen.
 */
@Suppress("LongParameterList")
class AutoFillHandlers(
    val onBackClick: () -> Unit,
    val onAutofillActionCardClick: () -> Unit,
    val onAutofillActionCardDismissClick: () -> Unit,
    val onBrowserAutofillActionCardClick: () -> Unit,
    val onBrowserAutofillActionCardDismissClick: () -> Unit,
    val onAutofillServicesClick: (isEnabled: Boolean) -> Unit,
    val onAutofillStyleChange: (style: AutofillStyle) -> Unit,
    val onBrowserAutofillSelected: (browserPackage: BrowserPackage) -> Unit,
    val onPasskeyManagementClick: () -> Unit,
    val onPrivilegedAppsClick: () -> Unit,
    val onPrivilegedAppsHelpLinkClick: () -> Unit,
    val onUseAccessibilityServiceClick: () -> Unit,
    val onCopyTotpAutomaticallyClick: (isEnabled: Boolean) -> Unit,
    val onAskToAddLoginClick: (isEnabled: Boolean) -> Unit,
    val onDefaultUriMatchTypeSelect: (defaultUriMatchType: UriMatchType) -> Unit,
    val onBlockAutoFillClick: () -> Unit,
    val onLearnMoreClick: () -> Unit,
    val onHelpCardClick: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {

        /**
         * Creates a new instance of [AutoFillHandlers] from the given [AutoFillViewModel].
         */
        @Suppress("LongMethod")
        fun create(viewModel: AutoFillViewModel): AutoFillHandlers = AutoFillHandlers(
            onBackClick = { viewModel.trySendAction(AutoFillAction.BackClick) },
            onAutofillActionCardClick = {
                viewModel.trySendAction(AutoFillAction.AutofillActionCardCtaClick)
            },
            onAutofillActionCardDismissClick = {
                viewModel.trySendAction(AutoFillAction.DismissShowAutofillActionCard)
            },
            onBrowserAutofillActionCardClick = {
                viewModel.trySendAction(AutoFillAction.BrowserAutofillActionCardCtaClick)
            },
            onBrowserAutofillActionCardDismissClick = {
                viewModel.trySendAction(AutoFillAction.DismissShowBrowserAutofillActionCard)
            },
            onAutofillServicesClick = {
                viewModel.trySendAction(AutoFillAction.AutoFillServicesClick(it))
            },
            onAutofillStyleChange = {
                viewModel.trySendAction(AutoFillAction.AutofillStyleSelected(it))
            },
            onBrowserAutofillSelected = {
                viewModel.trySendAction(AutoFillAction.BrowserAutofillSelected(it))
            },
            onPasskeyManagementClick = {
                viewModel.trySendAction(AutoFillAction.PasskeyManagementClick)
            },
            onPrivilegedAppsClick = {
                viewModel.trySendAction(AutoFillAction.PrivilegedAppsClick)
            },
            onPrivilegedAppsHelpLinkClick = {
                viewModel.trySendAction(AutoFillAction.AboutPrivilegedAppsClick)
            },
            onUseAccessibilityServiceClick = {
                viewModel.trySendAction(AutoFillAction.UseAccessibilityAutofillClick)
            },
            onCopyTotpAutomaticallyClick = {
                viewModel.trySendAction(AutoFillAction.CopyTotpAutomaticallyClick(it))
            },
            onAskToAddLoginClick = {
                viewModel.trySendAction(AutoFillAction.AskToAddLoginClick(it))
            },
            onDefaultUriMatchTypeSelect = {
                viewModel.trySendAction(AutoFillAction.DefaultUriMatchTypeSelect(it))
            },
            onBlockAutoFillClick = { viewModel.trySendAction(AutoFillAction.BlockAutoFillClick) },
            onLearnMoreClick = { viewModel.trySendAction(AutoFillAction.LearnMoreClick) },
            onHelpCardClick = { viewModel.trySendAction(AutoFillAction.HelpCardClick) },
        )
    }
}
