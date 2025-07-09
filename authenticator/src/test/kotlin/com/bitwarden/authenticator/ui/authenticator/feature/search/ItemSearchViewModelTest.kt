package com.bitwarden.authenticator.ui.authenticator.feature.search

import androidx.lifecycle.SavedStateHandle
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.manager.util.createMockSharedAuthenticatorItemSource
import com.bitwarden.authenticator.data.authenticator.manager.util.createMockVerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.authenticator.repository.model.SharedVerificationCodesState
import com.bitwarden.authenticator.data.platform.manager.clipboard.BitwardenClipboardManager
import com.bitwarden.authenticator.ui.authenticator.feature.model.SharedCodesDisplayState
import com.bitwarden.authenticator.ui.authenticator.feature.model.VerificationCodeDisplayItem
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.util.asText
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ItemSearchViewModelTest : BaseViewModelTest() {

    private val mutableAuthCodesStateFlow =
        MutableStateFlow<DataState<List<VerificationCodeItem>>>(DataState.Loading)
    private val mutableSharedCodesFlow = MutableStateFlow<SharedVerificationCodesState>(
        SharedVerificationCodesState.Success(items = SHARED_ITEMS),
    )
    private val mockAuthenticatorRepository = mockk<AuthenticatorRepository> {
        every { getLocalVerificationCodesFlow() } returns mutableAuthCodesStateFlow
        every { sharedCodesStateFlow } returns mutableSharedCodesFlow
    }
    private val mockClipboardManager = mockk<BitwardenClipboardManager>()

    @Test
    fun `initial state is correct`() {
        val viewModel = createViewModel()
        assertEquals(
            ItemSearchState.ViewState.Empty(message = null),
            viewModel.stateFlow.value.viewState,
        )
    }

    @Test
    fun `state contains both shared items and local items when available`() {
        val viewModel = createViewModel()

        mutableAuthCodesStateFlow.value = DataState.Loaded(LOCAL_ITEMS)

        viewModel.trySendAction(
            ItemSearchAction.SearchTermChange("I"),
        )

        assertEquals(
            ItemSearchState.ViewState.Content(
                itemList = LOCAL_DISPLAY_ITEMS,
                sharedItems = SHARED_DISPLAY_ITEMS,
            ),
            viewModel.stateFlow.value.viewState,
        )
    }

    @Test
    fun `state contains only local items when there are no shared items`() {
        val viewModel = createViewModel()
        mutableSharedCodesFlow.value = SharedVerificationCodesState.Success(items = emptyList())
        mutableAuthCodesStateFlow.value = DataState.Loaded(LOCAL_ITEMS)

        viewModel.trySendAction(
            ItemSearchAction.SearchTermChange("I"),
        )

        assertEquals(
            ItemSearchState.ViewState.Content(
                itemList = LOCAL_DISPLAY_ITEMS,
                sharedItems = SharedCodesDisplayState.Codes(sections = emptyList()),
            ),
            viewModel.stateFlow.value.viewState,
        )
    }

    @Test
    fun `state contains only local items when shared items are not available`() {
        val viewModel = createViewModel()
        mutableSharedCodesFlow.value = SharedVerificationCodesState.SyncNotEnabled
        mutableAuthCodesStateFlow.value = DataState.Loaded(data = LOCAL_ITEMS)

        viewModel.trySendAction(ItemSearchAction.SearchTermChange(searchTerm = "I"))

        assertEquals(
            ItemSearchState.ViewState.Content(
                itemList = LOCAL_DISPLAY_ITEMS.map { it.copy(showMoveToBitwarden = false) },
                sharedItems = SharedCodesDisplayState.Codes(sections = emptyList()),
            ),
            viewModel.stateFlow.value.viewState,
        )
    }

    private fun createViewModel(
        initialState: ItemSearchState? = null,
    ): ItemSearchViewModel =
        ItemSearchViewModel(
            savedStateHandle = SavedStateHandle().apply {
                set("state", initialState)
            },
            clipboardManager = mockClipboardManager,
            authenticatorRepository = mockAuthenticatorRepository,
        )
}

private val LOCAL_ITEMS = listOf(
    createMockVerificationCodeItem(number = 1),
)

private val SHARED_ITEMS = listOf(
    createMockVerificationCodeItem(
        number = 2,
        source = createMockSharedAuthenticatorItemSource(number = 2),
    ),
)

private val SHARED_DISPLAY_ITEMS = SharedCodesDisplayState.Codes(
    sections = listOf(
        SharedCodesDisplayState.SharedCodesAccountSection(
            id = "mockUserId-2",
            label = R.string.shared_accounts_header.asText(
                "mockEmail-2",
                "mockkEnvironmentLabel-2",
                1,
            ),
            codes = listOf(
                VerificationCodeDisplayItem(
                    id = "mockId-2",
                    title = "mockIssuer-2",
                    subtitle = "mockLabel-2",
                    timeLeftSeconds = 120,
                    periodSeconds = 30,
                    alertThresholdSeconds = 7,
                    authCode = "mockCode-2",
                    favorite = false,
                    allowLongPressActions = false,
                    showMoveToBitwarden = false,
                ),
            ),
            isExpanded = true,
        ),
    ),
)

private val LOCAL_DISPLAY_ITEMS = listOf(
    VerificationCodeDisplayItem(
        id = LOCAL_ITEMS[0].id,
        authCode = LOCAL_ITEMS[0].code,
        title = LOCAL_ITEMS[0].issuer!!,
        periodSeconds = LOCAL_ITEMS[0].periodSeconds,
        timeLeftSeconds = LOCAL_ITEMS[0].timeLeftSeconds,
        alertThresholdSeconds = 7,
        startIcon = IconData.Local(
            iconRes = BitwardenDrawable.ic_login_item,
            testTag = "BitwardenIcon",
        ),
        subtitle = LOCAL_ITEMS[0].label,
        favorite = false,
        allowLongPressActions = true,
        showMoveToBitwarden = true,
    ),
)
