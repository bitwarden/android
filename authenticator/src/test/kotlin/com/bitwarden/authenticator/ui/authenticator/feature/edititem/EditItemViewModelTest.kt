package com.bitwarden.authenticator.ui.authenticator.feature.edititem

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.authenticator.repository.model.CreateItemResult
import com.bitwarden.authenticator.ui.authenticator.feature.edititem.model.EditItemData
import com.bitwarden.authenticator.ui.platform.model.SnackbarRelay
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EditItemViewModelTest : BaseViewModelTest() {
    private val mutableItemStateFlow =
        MutableStateFlow<DataState<AuthenticatorItemEntity?>>(DataState.Loading)

    private val authenticatorRepository: AuthenticatorRepository = mockk {
        every { getItemStateFlow(itemId = DEFAULT_ITEM_ID) } returns mutableItemStateFlow
    }
    private val snackbarRelayManager = mockk<SnackbarRelayManager<SnackbarRelay>> {
        every { sendSnackbarData(data = any(), relay = SnackbarRelay.ITEM_SAVED) } just runs
    }

    @BeforeEach
    fun setup() {
        mockkStatic(
            SavedStateHandle::toEditItemArgs,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            SavedStateHandle::toEditItemArgs,
        )
    }

    @Test
    fun `initial state should be correct`() {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `on DismissDialog should clear the dialog state`() = runTest {
        val state = DEFAULT_STATE.copy(
            dialog = EditItemState.DialogState.Loading(message = "loading".asText()),
        )
        val viewModel = createViewModel(state = state)
        viewModel.stateFlow.test {
            assertEquals(state, awaitItem())
            viewModel.trySendAction(EditItemAction.DismissDialog)
            assertEquals(state.copy(dialog = null), awaitItem())
        }
    }

    @Test
    fun `on AlgorithmOptionClick should update the algorithm state`() {
        mutableItemStateFlow.tryEmit(DataState.Loaded(DEFAULT_AUTHENTICATOR_ENTITY))
        val state = DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT)
        val viewModel = createViewModel(state = state)
        val algorithm = AuthenticatorItemAlgorithm.SHA256
        viewModel.trySendAction(EditItemAction.AlgorithmOptionClick(algorithm))
        assertEquals(
            state.copy(
                viewState = DEFAULT_CONTENT.copy(
                    itemData = DEFAULT_ITEM_DATA.copy(algorithm = algorithm),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on CancelClick should send NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(EditItemAction.CancelClick)
            assertEquals(EditItemEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on TypeOptionClick should update the type state`() {
        mutableItemStateFlow.tryEmit(DataState.Loaded(DEFAULT_AUTHENTICATOR_ENTITY))
        val state = DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT)
        val viewModel = createViewModel(state = state)
        val type = AuthenticatorItemType.STEAM
        viewModel.trySendAction(EditItemAction.TypeOptionClick(type))
        assertEquals(
            state.copy(
                viewState = DEFAULT_CONTENT.copy(
                    itemData = DEFAULT_ITEM_DATA.copy(type = type),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on IssuerNameTextChange should update the issuer state`() {
        mutableItemStateFlow.tryEmit(DataState.Loaded(DEFAULT_AUTHENTICATOR_ENTITY))
        val state = DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT)
        val viewModel = createViewModel(state = state)
        val issuer = "newIssuer"
        viewModel.trySendAction(EditItemAction.IssuerNameTextChange(issuer))
        assertEquals(
            state.copy(
                viewState = DEFAULT_CONTENT.copy(
                    itemData = DEFAULT_ITEM_DATA.copy(issuer = issuer),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on UsernameTextChange should update the username state`() {
        mutableItemStateFlow.tryEmit(DataState.Loaded(DEFAULT_AUTHENTICATOR_ENTITY))
        val state = DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT)
        val viewModel = createViewModel(state = state)
        val username = "newUsername"
        viewModel.trySendAction(EditItemAction.UsernameTextChange(username))
        assertEquals(
            state.copy(
                viewState = DEFAULT_CONTENT.copy(
                    itemData = DEFAULT_ITEM_DATA.copy(username = username),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on FavoriteToggleClick should update the favorite state`() {
        mutableItemStateFlow.tryEmit(DataState.Loaded(DEFAULT_AUTHENTICATOR_ENTITY))
        val state = DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT)
        val viewModel = createViewModel(state = state)
        val isFavorite = true
        viewModel.trySendAction(EditItemAction.FavoriteToggleClick(isFavorite))
        assertEquals(
            state.copy(
                viewState = DEFAULT_CONTENT.copy(
                    itemData = DEFAULT_ITEM_DATA.copy(favorite = isFavorite),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on RefreshPeriodOptionClick should update the refresh period state`() {
        mutableItemStateFlow.tryEmit(DataState.Loaded(DEFAULT_AUTHENTICATOR_ENTITY))
        val state = DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT)
        val viewModel = createViewModel(state = state)
        val period = AuthenticatorRefreshPeriodOption.NINETY
        viewModel.trySendAction(EditItemAction.RefreshPeriodOptionClick(period))
        assertEquals(
            state.copy(
                viewState = DEFAULT_CONTENT.copy(
                    itemData = DEFAULT_ITEM_DATA.copy(refreshPeriod = period),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on TotpCodeTextChange should update the totp code state`() {
        mutableItemStateFlow.tryEmit(DataState.Loaded(DEFAULT_AUTHENTICATOR_ENTITY))
        val state = DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT)
        val viewModel = createViewModel(state = state)
        val totpCode = "newTotpCode"
        viewModel.trySendAction(EditItemAction.TotpCodeTextChange(totpCode))
        assertEquals(
            state.copy(
                viewState = DEFAULT_CONTENT.copy(
                    itemData = DEFAULT_ITEM_DATA.copy(totpCode = totpCode),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on NumberOfDigitsOptionClick should update the number of digits state`() {
        mutableItemStateFlow.tryEmit(DataState.Loaded(DEFAULT_AUTHENTICATOR_ENTITY))
        val state = DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT)
        val viewModel = createViewModel(state = state)
        val digits = 8
        viewModel.trySendAction(EditItemAction.NumberOfDigitsOptionClick(digits))
        assertEquals(
            state.copy(
                viewState = DEFAULT_CONTENT.copy(
                    itemData = DEFAULT_ITEM_DATA.copy(digits = digits),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on SaveClick with blank issuer should display an error dialog`() {
        mutableItemStateFlow.tryEmit(
            DataState.Loaded(DEFAULT_AUTHENTICATOR_ENTITY.copy(issuer = "")),
        )
        val state = DEFAULT_STATE.copy(
            viewState = DEFAULT_CONTENT.copy(
                itemData = DEFAULT_ITEM_DATA.copy(issuer = ""),
            ),
        )
        val viewModel = createViewModel(state = state)
        viewModel.trySendAction(EditItemAction.SaveClick)
        assertEquals(
            state.copy(
                dialog = EditItemState.DialogState.Generic(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.validation_field_required
                        .asText(BitwardenString.name.asText()),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on SaveClick with blank totp code should display an error dialog`() {
        mutableItemStateFlow.tryEmit(
            DataState.Loaded(DEFAULT_AUTHENTICATOR_ENTITY.copy(key = "")),
        )
        val state = DEFAULT_STATE.copy(
            viewState = DEFAULT_CONTENT.copy(
                itemData = DEFAULT_ITEM_DATA.copy(totpCode = ""),
            ),
        )
        val viewModel = createViewModel(state = state)
        viewModel.trySendAction(EditItemAction.SaveClick)
        assertEquals(
            state.copy(
                dialog = EditItemState.DialogState.Generic(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.validation_field_required
                        .asText(BitwardenString.key.asText()),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on SaveClick with non-base32 totp code should display an error dialog`() {
        mutableItemStateFlow.tryEmit(
            DataState.Loaded(DEFAULT_AUTHENTICATOR_ENTITY.copy(key = "111%")),
        )
        val state = DEFAULT_STATE.copy(
            viewState = DEFAULT_CONTENT.copy(
                itemData = DEFAULT_ITEM_DATA.copy(totpCode = "111%"),
            ),
        )
        val viewModel = createViewModel(state = state)
        viewModel.trySendAction(EditItemAction.SaveClick)
        assertEquals(
            state.copy(
                dialog = EditItemState.DialogState.Generic(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.key_is_invalid.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on SaveClick with valid data and createItem error should display error dialog`() =
        runTest {
            mutableItemStateFlow.tryEmit(
                DataState.Loaded(DEFAULT_AUTHENTICATOR_ENTITY),
            )
            val state = DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT)
            coEvery {
                authenticatorRepository.createItem(item = any())
            } returns CreateItemResult.Error
            val viewModel = createViewModel(state = state)
            viewModel.stateFlow.test {
                assertEquals(state, awaitItem())
                viewModel.trySendAction(EditItemAction.SaveClick)
                assertEquals(
                    state.copy(
                        dialog = EditItemState.DialogState.Loading(
                            message = BitwardenString.saving.asText(),
                        ),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    state.copy(
                        dialog = EditItemState.DialogState.Generic(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
            coVerify(exactly = 1) {
                authenticatorRepository.createItem(
                    item = AuthenticatorItemEntity(
                        id = DEFAULT_ITEM_ID,
                        key = "ABCD",
                        accountName = "mockAccountName",
                        type = AuthenticatorItemType.TOTP,
                        algorithm = AuthenticatorItemAlgorithm.SHA1,
                        period = 30,
                        digits = 6,
                        issuer = "mockIssuer",
                        favorite = false,
                    ),
                )
            }
        }

    @Test
    fun `on SaveClick with valid data and createItem success navigate back`() = runTest {
        mutableItemStateFlow.tryEmit(
            DataState.Loaded(DEFAULT_AUTHENTICATOR_ENTITY),
        )
        val state = DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT)
        coEvery {
            authenticatorRepository.createItem(item = any())
        } returns CreateItemResult.Success
        val viewModel = createViewModel(state = state)
        viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
            assertEquals(state, stateFlow.awaitItem())
            viewModel.trySendAction(EditItemAction.SaveClick)
            assertEquals(
                state.copy(
                    dialog = EditItemState.DialogState.Loading(
                        message = BitwardenString.saving.asText(),
                    ),
                ),
                stateFlow.awaitItem(),
            )
            assertEquals(EditItemEvent.NavigateBack, eventFlow.awaitItem())
        }
        verify(exactly = 1) {
            snackbarRelayManager.sendSnackbarData(
                data = BitwardenSnackbarData(message = BitwardenString.item_saved.asText()),
                relay = SnackbarRelay.ITEM_SAVED,
            )
        }
        coVerify(exactly = 1) {
            authenticatorRepository.createItem(
                item = AuthenticatorItemEntity(
                    id = DEFAULT_ITEM_ID,
                    key = "ABCD",
                    accountName = "mockAccountName",
                    type = AuthenticatorItemType.TOTP,
                    algorithm = AuthenticatorItemAlgorithm.SHA1,
                    period = 30,
                    digits = 6,
                    issuer = "mockIssuer",
                    favorite = false,
                ),
            )
        }
    }

    @Test
    fun `on ExpandAdvancedOptionsClick should update the isAdvancedOptionsExpanded state`() {
        mutableItemStateFlow.tryEmit(DataState.Loaded(DEFAULT_AUTHENTICATOR_ENTITY))
        val state = DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT)
        val viewModel = createViewModel(state = state)
        viewModel.trySendAction(EditItemAction.ExpandAdvancedOptionsClick)
        assertEquals(
            state.copy(viewState = DEFAULT_CONTENT.copy(isAdvancedOptionsExpanded = true)),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on EditItemDataReceive should update the view state`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(
                EditItemAction.Internal.EditItemDataReceive(DataState.Error(Throwable())),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = EditItemState.ViewState.Error(
                        message = BitwardenString.generic_error_message.asText(),
                    ),
                ),
                awaitItem(),
            )
            viewModel.trySendAction(
                EditItemAction.Internal.EditItemDataReceive(
                    itemDataState = DataState.Loaded(DEFAULT_AUTHENTICATOR_ENTITY),
                ),
            )
            assertEquals(
                DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT),
                awaitItem(),
            )
            viewModel.trySendAction(EditItemAction.Internal.EditItemDataReceive(DataState.Loading))
            assertEquals(
                DEFAULT_STATE.copy(viewState = EditItemState.ViewState.Loading),
                awaitItem(),
            )
            viewModel.trySendAction(
                EditItemAction.Internal.EditItemDataReceive(DataState.NoNetwork(null)),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = EditItemState.ViewState.Error(
                        message = BitwardenString.internet_connection_required_title
                            .asText()
                            .concat(BitwardenString.internet_connection_required_message.asText()),
                    ),
                ),
                awaitItem(),
            )
            viewModel.trySendAction(
                EditItemAction.Internal.EditItemDataReceive(DataState.Pending(null)),
            )
            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = EditItemState.ViewState.Error(
                        message = BitwardenString.generic_error_message.asText(),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    private fun createViewModel(
        state: EditItemState? = null,
    ): EditItemViewModel = EditItemViewModel(
        snackbarRelayManager = snackbarRelayManager,
        authenticatorRepository = authenticatorRepository,
        savedStateHandle = SavedStateHandle().apply {
            set(key = "state", value = state)
            every { toEditItemArgs() } returns EditItemArgs(
                itemId = state?.itemId ?: DEFAULT_ITEM_ID,
            )
        },
    )
}

private const val DEFAULT_ITEM_ID: String = "item_id"
private val DEFAULT_STATE: EditItemState =
    EditItemState(
        itemId = DEFAULT_ITEM_ID,
        viewState = EditItemState.ViewState.Loading,
        dialog = null,
    )

private val DEFAULT_ITEM_DATA: EditItemData =
    EditItemData(
        refreshPeriod = AuthenticatorRefreshPeriodOption.THIRTY,
        totpCode = "ABCD",
        type = AuthenticatorItemType.TOTP,
        username = "mockAccountName",
        issuer = "mockIssuer",
        algorithm = AuthenticatorItemAlgorithm.SHA1,
        digits = 6,
        favorite = false,
    )

private val DEFAULT_CONTENT: EditItemState.ViewState.Content =
    EditItemState.ViewState.Content(
        isAdvancedOptionsExpanded = false,
        minDigitsAllowed = 5,
        maxDigitsAllowed = 10,
        itemData = DEFAULT_ITEM_DATA,
    )

private val DEFAULT_AUTHENTICATOR_ENTITY: AuthenticatorItemEntity =
    AuthenticatorItemEntity(
        id = DEFAULT_ITEM_ID,
        key = "ABCD",
        issuer = "mockIssuer",
        accountName = "mockAccountName",
        userId = null,
        favorite = false,
        type = AuthenticatorItemType.TOTP,
    )
