package com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount

import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.bitwarden.cxf.model.ImportCredentialsRequestData
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.ui.vault.feature.exportitems.model.AccountSelectionListItem
import com.x8bit.bitwarden.ui.vault.feature.vault.util.initials
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Manages application state for the select account screen.
 */
@HiltViewModel
class SelectAccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val policyManager: PolicyManager,
    specialCircumstanceManager: SpecialCircumstanceManager,
) : BaseViewModel<SelectAccountState, SelectAccountEvent, SelectAccountAction>(
    initialState = run {
        val importRequest = specialCircumstanceManager.specialCircumstance
            as SpecialCircumstance.CredentialExchangeExport

        SelectAccountState(
            importRequest = importRequest.data,
            viewState = SelectAccountState.ViewState.Loading,
        )
    },
) {

    init {
        sendEvent(
            SelectAccountEvent.ValidateImportRequest(
                importCredentialsRequestData = state.importRequest,
            ),
        )
    }

    override fun handleAction(action: SelectAccountAction) {
        when (action) {
            is SelectAccountAction.ValidateImportRequestResultReceive -> {
                handleValidateImportRequestResultReceive(action)
            }

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

    private fun handleValidateImportRequestResultReceive(
        action: SelectAccountAction.ValidateImportRequestResultReceive,
    ) {
        if (action.isValid) {
            observeSelectionData()
        } else {
            mutableStateFlow.update {
                it.copy(
                    viewState = SelectAccountState.ViewState.Error(
                        message = BitwardenString
                            .the_import_request_could_not_be_processed
                            .asText(),
                    ),
                )
            }
        }
    }

    private fun handleCloseClick() {
        sendEvent(SelectAccountEvent.CancelExport)
    }

    private fun handleAccountClick(action: SelectAccountAction.AccountClick) {
        sendEvent(
            event = SelectAccountEvent.NavigateToPasswordVerification(
                userId = action.userId,
                hasOtherAccounts = true,
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

        val accountSelectionListItems = action.userState
            ?.accounts
            .orEmpty()
            // We only want accounts that do not restrict personal vault ownership
            // or vault export
            .filter { account ->
                account.isExportable
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
            .toImmutableList()

        mutableStateFlow.update {
            it.copy(
                viewState = if (accountSelectionListItems.isEmpty()) {
                    SelectAccountState.ViewState.NoItems
                } else {
                    SelectAccountState.ViewState.Content(
                        accountSelectionListItems = accountSelectionListItems,
                    )
                },
            )
        }
    }

    private fun observeSelectionData() {
        combine(
            authRepository.userStateFlow,
            policyManager.getActivePoliciesFlow(PolicyTypeJson.RESTRICT_ITEM_TYPES),
        ) { userState, itemRestrictedOrgs ->
            SelectAccountAction.Internal.SelectionDataReceive(
                userState = userState,
                itemRestrictedOrgs = itemRestrictedOrgs,
            )
        }
            .onEach(::handleAction)
            .launchIn(viewModelScope)
    }
}

/**
 * Represents the state for the select account screen.
 */
@Parcelize
data class SelectAccountState(
    val importRequest: ImportCredentialsRequestData,
    val viewState: ViewState,
) : Parcelable {

    /**
     * Represents the different states for the select account screen.
     */
    @Parcelize
    sealed class ViewState : Parcelable {
        /**
         * Represents the loading state for the select account screen.
         */
        data object Loading : ViewState()

        /**
         * Represents the content state for the select account screen.
         *
         * @param accountSelectionListItems The list of account summaries to be displayed for
         * selection.
         */
        data class Content(
            val accountSelectionListItems: ImmutableList<AccountSelectionListItem>,
        ) : ViewState()

        /**
         * Represents the no items state for the select account screen.
         */
        data object NoItems : ViewState()

        /**
         * Represents the error state for the select account screen.
         */
        data class Error(val message: Text) : ViewState()
    }
}

/**
 * Represents the actions that can be performed on the select account screen.
 */
sealed class SelectAccountAction {

    /**
     * Indicates the validate import request result was received.
     *
     * @param isValid Whether the import request is valid.
     */
    data class ValidateImportRequestResultReceive(
        val isValid: Boolean,
    ) : SelectAccountAction()

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
        ) : Internal()
    }
}

/**
 * Models events for the select account screen.
 */
sealed class SelectAccountEvent {

    /**
     * Validates the import request.
     */
    data class ValidateImportRequest(
        val importCredentialsRequestData: ImportCredentialsRequestData,
    ) : SelectAccountEvent(), BackgroundEvent

    /**
     * Navigates back to the previous screen.
     */
    data object CancelExport : SelectAccountEvent()

    /**
     * Navigate to the password verification screen.
     */
    data class NavigateToPasswordVerification(
        val userId: String,
        val hasOtherAccounts: Boolean,
    ) : SelectAccountEvent()
}
