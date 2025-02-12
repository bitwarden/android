package com.bitwarden.authenticator.ui.authenticator.feature.search

import androidx.lifecycle.SavedStateHandle
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.manager.util.createMockVerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.authenticator.repository.model.AuthenticatorItem
import com.bitwarden.authenticator.data.authenticator.repository.model.SharedVerificationCodesState
import com.bitwarden.authenticator.data.authenticator.repository.util.itemsOrEmpty
import com.bitwarden.authenticator.data.platform.manager.clipboard.BitwardenClipboardManager
import com.bitwarden.authenticator.data.platform.repository.model.DataState
import com.bitwarden.authenticator.ui.platform.base.BaseViewModelTest
import com.bitwarden.authenticator.ui.platform.components.model.IconData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ItemSearchViewModelTest : BaseViewModelTest() {

    private val mutableAuthCodesStateFlow =
        MutableStateFlow<DataState<List<VerificationCodeItem>>>(DataState.Loading)
    private val mutableSharedCodesFlow = MutableStateFlow(
        SharedVerificationCodesState.Success(SHARED_ITEMS),
    )
    private val mockAuthenticatorRepository = mockk<AuthenticatorRepository> {
        every { getLocalVerificationCodesFlow() } returns mutableAuthCodesStateFlow
        every { sharedCodesStateFlow } returns mutableSharedCodesFlow
    }
    private val mockClipboardManager = mockk<BitwardenClipboardManager>()

    @BeforeEach
    fun setup() {
        mockkStatic(SharedVerificationCodesState::itemsOrEmpty)
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(SharedVerificationCodesState::itemsOrEmpty)
    }

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
                displayItems = SHARED_AND_LOCAL_DISPLAY_ITEMS,
            ),
            viewModel.stateFlow.value.viewState,
        )
    }

    @Test
    fun `state contains only local items when shared items are not available`() {
        val viewModel = createViewModel()
        every { mutableSharedCodesFlow.value.itemsOrEmpty } returns emptyList()
        mutableAuthCodesStateFlow.value = DataState.Loaded(LOCAL_ITEMS)

        viewModel.trySendAction(
            ItemSearchAction.SearchTermChange("I"),
        )

        assertEquals(
            ItemSearchState.ViewState.Content(
                displayItems = listOf(SHARED_AND_LOCAL_DISPLAY_ITEMS[1]),
            ),
            viewModel.stateFlow.value.viewState,
        )
    }

    private fun createItemSearchState(
        viewState: ItemSearchState.ViewState = ItemSearchState.ViewState.Empty(message = null),
    ) = ItemSearchState(
        searchTerm = "",
        viewState = viewState,
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

private val LOCAL_ITEMS = listOf(
    createMockVerificationCodeItem(number = 1),
)

private val SHARED_ITEMS = listOf(
    VerificationCodeItem(
        "123456",
        periodSeconds = 60,
        timeLeftSeconds = 30,
        issueTime = 1,
        issuer = "Issuer",
        label = "accountName",
        id = "123",
        source = AuthenticatorItem.Source.Shared(
            userId = "1",
            nameOfUser = "John Test",
            email = "test@test.com",
            environmentLabel = "1234",
        ),
    ),
)

private val SHARED_AND_LOCAL_DISPLAY_ITEMS = listOf(
    ItemSearchState.DisplayItem(
        id = SHARED_ITEMS[0].id,
        authCode = SHARED_ITEMS[0].code,
        title = SHARED_ITEMS[0].issuer!!,
        periodSeconds = SHARED_ITEMS[0].periodSeconds,
        timeLeftSeconds = SHARED_ITEMS[0].timeLeftSeconds,
        alertThresholdSeconds = 7,
        startIcon = IconData.Local(iconRes = R.drawable.ic_login_item),
        subtitle = SHARED_ITEMS[0].label,
    ),
    ItemSearchState.DisplayItem(
        id = LOCAL_ITEMS[0].id,
        authCode = LOCAL_ITEMS[0].code,
        title = LOCAL_ITEMS[0].issuer!!,
        periodSeconds = LOCAL_ITEMS[0].periodSeconds,
        timeLeftSeconds = LOCAL_ITEMS[0].timeLeftSeconds,
        alertThresholdSeconds = 7,
        startIcon = IconData.Local(iconRes = R.drawable.ic_login_item),
        subtitle = LOCAL_ITEMS[0].label,
    ),
)
