package com.bitwarden.authenticator.ui.authenticator.feature.search

import androidx.lifecycle.SavedStateHandle
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.manager.util.createMockVerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.platform.manager.clipboard.BitwardenClipboardManager
import com.bitwarden.authenticator.data.platform.repository.model.DataState
import com.bitwarden.authenticator.ui.platform.base.BaseViewModelTest
import com.bitwarden.authenticator.ui.platform.components.model.IconData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ItemSearchViewModelTest : BaseViewModelTest() {

    private val mutableAuthCodesStateFlow =
        MutableStateFlow<DataState<List<VerificationCodeItem>>>(DataState.Loading)
    private val mockAuthenticatorRepository = mockk<AuthenticatorRepository> {
        every { getLocalVerificationCodesFlow() } returns mutableAuthCodesStateFlow
    }
    private val mockClipboardManager = mockk<BitwardenClipboardManager>()

    @Test
    fun `initial state is correct`() {
        val viewModel = createViewModel()
        assertEquals(ItemSearchState.ViewState.Loading, viewModel.stateFlow.value.viewState)
    }

    @Test
    fun `state is updated when auth codes are received`() {
        val mockVerificationCodeItem = createMockVerificationCodeItem(number = 1)
        val viewModel = createViewModel()

        mutableAuthCodesStateFlow.value = DataState.Loaded(listOf(mockVerificationCodeItem))

        viewModel.trySendAction(
            ItemSearchAction.SearchTermChange(
                mockVerificationCodeItem.label!!,
            ),
        )

        assertEquals(
            ItemSearchState.ViewState.Content(
                displayItems = listOf(
                    ItemSearchState.DisplayItem(
                        id = mockVerificationCodeItem.id,
                        authCode = mockVerificationCodeItem.code,
                        issuer = mockVerificationCodeItem.issuer,
                        periodSeconds = mockVerificationCodeItem.periodSeconds,
                        timeLeftSeconds = mockVerificationCodeItem.timeLeftSeconds,
                        alertThresholdSeconds = 7,
                        startIcon = IconData.Local(iconRes = R.drawable.ic_login_item),
                        label = mockVerificationCodeItem.label,
                    ),
                ),
            ),
            viewModel.stateFlow.value.viewState,
        )
    }

    private fun createItemSearchState(
        viewState: ItemSearchState.ViewState = ItemSearchState.ViewState.Loading,
        dialogState: ItemSearchState.DialogState? = null,
    ) = ItemSearchState(
        searchTerm = "",
        viewState = viewState,
        dialogState = dialogState,
    )

    private fun createViewModel(
        initialState: ItemSearchState = createItemSearchState(),
    ): ItemSearchViewModel {
        return ItemSearchViewModel(
            SavedStateHandle().apply {
                set("state", initialState)
            },
            mockClipboardManager,
            mockAuthenticatorRepository,
        )
    }
}
