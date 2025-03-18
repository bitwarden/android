package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.handlers

import com.x8bit.bitwarden.data.autofill.model.chrome.ChromeReleaseChannel
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.AutoFillAction
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.AutoFillViewModel

/**
 * Handlers for the AutoFill screen.
 */
@Suppress("LongParameterList")
class AutoFillHandlers(
    val onAutofillActionCardClick: () -> Unit,
    val onAutofillActionCardDismissClick: () -> Unit,
    val onAutofillServicesClick: (isEnabled: Boolean) -> Unit,
    val onUseInlineAutofillClick: (isEnabled: Boolean) -> Unit,
    val onChromeAutofillSelected: (releaseChannel: ChromeReleaseChannel) -> Unit,
    val onPasskeyManagementClick: () -> Unit,
    val onPrivilegedAppsClick: () -> Unit,
    val onPrivilegedAppsHelpLinkClick: () -> Unit,
    val onUseAccessibilityServiceClick: () -> Unit,
    val onCopyTotpAutomaticallyClick: (isEnabled: Boolean) -> Unit,
    val onAskToAddLoginClick: (isEnabled: Boolean) -> Unit,
    val onDefaultUriMatchTypeSelect: (defaultUriMatchType: UriMatchType) -> Unit,
    val onBlockAutoFillClick: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {

        /**
         * Creates a new instance of [AutoFillHandlers] from the given [AutoFillViewModel].
         */
        fun create(viewModel: AutoFillViewModel): AutoFillHandlers = AutoFillHandlers(
            onAutofillActionCardClick = {
                viewModel.trySendAction(AutoFillAction.AutofillActionCardCtaClick)
            },
            onAutofillActionCardDismissClick = {
                viewModel.trySendAction(AutoFillAction.DismissShowAutofillActionCard)
            },
            onAutofillServicesClick = {
                viewModel.trySendAction(
                    AutoFillAction.AutoFillServicesClick(
                        it,
                    ),
                )
            },
            onUseInlineAutofillClick = {
                viewModel.trySendAction(
                    AutoFillAction.UseInlineAutofillClick(
                        it,
                    ),
                )
            },
            onChromeAutofillSelected = {
                viewModel.trySendAction(
                    AutoFillAction.ChromeAutofillSelected(
                        it,
                    ),
                )
            },
            onPasskeyManagementClick = {
                viewModel.trySendAction(AutoFillAction.PasskeyManagementClick)
            },
            onPrivilegedAppsClick = { viewModel.trySendAction(AutoFillAction.TrustedAppsClick) },
            onPrivilegedAppsHelpLinkClick = {
                viewModel.trySendAction(AutoFillAction.TrustedAppsHelpLinkClick)
            },
            onUseAccessibilityServiceClick = {
                viewModel.trySendAction(
                    AutoFillAction.UseAccessibilityAutofillClick,
                )
            },
            onCopyTotpAutomaticallyClick = {
                viewModel.trySendAction(
                    AutoFillAction.CopyTotpAutomaticallyClick(
                        it,
                    ),
                )
            },
            onAskToAddLoginClick = {
                viewModel.trySendAction(AutoFillAction.AskToAddLoginClick(it))
            },
            onDefaultUriMatchTypeSelect = {
                viewModel.trySendAction(
                    AutoFillAction.DefaultUriMatchTypeSelect(
                        it,
                    ),
                )
            },
            onBlockAutoFillClick = { viewModel.trySendAction(AutoFillAction.BlockAutoFillClick) },
        )
    }
}
