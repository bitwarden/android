package com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount

import androidx.lifecycle.viewModelScope
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.ui.vault.feature.exportitems.model.AccountSelectionListItem
import com.x8bit.bitwarden.ui.vault.feature.vault.util.initials
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Manages application state for the select account screen.
 */
@HiltViewModel
class SelectAccountViewModel @Inject constructor(
    authRepository: AuthRepository,
    policyManager: PolicyManager,
) : BaseViewModel<SelectAccountState, SelectAccountEvent, SelectAccountAction>(
    initialState = SelectAccountState(
        accountSelectionListItems = persistentListOf(),
    ),
) {

    init {
        combine(
            authRepository.userStateFlow,
            policyManager.getActivePoliciesFlow(PolicyTypeJson.RESTRICT_ITEM_TYPES),
            policyManager.getActivePoliciesFlow(PolicyTypeJson.PERSONAL_OWNERSHIP),
        ) { userState, itemRestrictedOrgs, personalOwnershipOrgs ->
            SelectAccountAction.Internal.SelectionDataReceive(
                userState,
                itemRestrictedOrgs,
                personalOwnershipOrgs,
            )
        }
            .onEach(::handleAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: SelectAccountAction) {
        when (action) {
            SelectAccountAction.CloseClick -> {
                handleCloseClick()
            }

            is SelectAccountAction.AccountClick -> {
                handleAccountClick(action)
            }

            is SelectAccountAction.Internal -> {
                handleInternalAction(action)
            }
        }
    }

    private fun handleCloseClick() {
        sendEvent(SelectAccountEvent.CancelExport)
    }

    private fun handleAccountClick(action: SelectAccountAction.AccountClick) {
        sendEvent(
            SelectAccountEvent.NavigateToPasswordVerification(
                action.userId,
            ),
        )
    }

    private fun handleInternalAction(action: SelectAccountAction.Internal) {
        when (action) {
            is SelectAccountAction.Internal.SelectionDataReceive -> {
                handleSelectionDataReceive(action)
            }
        }
    }

    private fun handleSelectionDataReceive(
        action: SelectAccountAction.Internal.SelectionDataReceive,
    ) {
        val itemRestrictedOrgIds = action.itemRestrictedOrgs
            .filter { it.isEnabled }
            .map { it.organizationId }
        val personalOwnershipRestrictedOrgIds = action
            .personalOwnershipOrgs
            .filter { it.isEnabled }
            .map { it.organizationId }

        mutableStateFlow.update {
            it.copy(
                accountSelectionListItems = action.userState
                    ?.accounts
                    .orEmpty()
                    // We only want accounts that do not restrict personal vault ownership
                    .filter { account ->
                        account
                            .organizations
                            .none { org -> org.id in personalOwnershipRestrictedOrgIds }
                    }
                    .map { account ->
                        AccountSelectionListItem(
                            userId = account.userId,
                            email = account.email,
                            initials = account.initials,
                            avatarColorHex = account.avatarColorHex,
                            // Indicate which accounts have item restrictions applied.
                            isItemRestricted = account
                                .organizations
                                .any { org -> org.id in itemRestrictedOrgIds },
                        )
                    }
                    .toImmutableList(),
            )
        }
    }
}

/**
 * Represents the state for the select account screen.
 *
 * @param accountSelectionListItems The list of account summaries to be displayed for selection.
 */
data class SelectAccountState(
    val accountSelectionListItems: ImmutableList<AccountSelectionListItem>,
)

/**
 * Represents the actions that can be performed on the select account screen.
 */
sealed class SelectAccountAction {

    /**
     * Indicates the top-bar close button was clicked.
     */
    data object CloseClick : SelectAccountAction()

    /**
     * Indicates the user selected an account summary.
     */
    data class AccountClick(
        val userId: String,
    ) : SelectAccountAction()

    /**
     * Represents internal actions for the select account screen.
     */
    sealed class Internal : SelectAccountAction() {

        /**
         * Indicates the selection data was received.
         */
        data class SelectionDataReceive(
            val userState: UserState?,
            val itemRestrictedOrgs: List<SyncResponseJson.Policy>,
            val personalOwnershipOrgs: List<SyncResponseJson.Policy>,
        ) : Internal()
    }
}

/**
 * Models events for the select account screen.
 */
sealed class SelectAccountEvent {

    /**
     * Navigates back to the previous screen.
     */
    data object CancelExport : SelectAccountEvent()

    /**
     * Navigate to the password verification screen.
     */
    data class NavigateToPasswordVerification(
        val userId: String,
    ) : SelectAccountEvent()
}
