package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.list

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.data.manager.BitwardenPackageManager
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.credentials.model.PrivilegedAppAllowListJson
import com.x8bit.bitwarden.data.credentials.repository.PrivilegedAppRepository
import com.x8bit.bitwarden.data.credentials.repository.model.PrivilegedAppData
import com.x8bit.bitwarden.data.credentials.util.createMockPrivilegedAppJson
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.list.model.PrivilegedAppListItem
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PrivilegedAppsListViewModelTest : BaseViewModelTest() {

    private val mutableTrustedAppDataStateFlow =
        MutableStateFlow<DataState<PrivilegedAppData>>(DataState.Loading)
    private val mockPrivilegedAppRepository = mockk<PrivilegedAppRepository> {
        every { trustedAppDataStateFlow } returns mutableTrustedAppDataStateFlow
        coEvery { removeTrustedPrivilegedApp(any(), any()) } just runs
    }
    private val mockBitwardenPackageManager = mockk<BitwardenPackageManager>()

    @Test
    fun `initial state should be correct`() {
        val viewModel = createViewModel()
        assert(
            viewModel.stateFlow.value == PrivilegedAppsListState(
                installedApps = persistentListOf(),
                notInstalledApps = persistentListOf(),
                dialogState = PrivilegedAppsListState.DialogState.Loading,
            ),
        )
    }

    @Test
    fun `UserTrustedAppDeleteClick should display confirm delete dialog`() = runTest {
        val app = PrivilegedAppListItem(
            packageName = "com.example.app",
            signature = "signature",
            trustAuthority = PrivilegedAppListItem.PrivilegedAppTrustAuthority.COMMUNITY,
            appName = "Example App",
        )
        val state = PrivilegedAppsListState(
            installedApps = persistentListOf(app),
            notInstalledApps = persistentListOf(),
            dialogState = null,
        )
        val viewModel = createViewModel(state = state)
        viewModel.trySendAction(PrivilegedAppsListAction.UserTrustedAppDeleteClick(app))
        assertEquals(
            state.copy(
                dialogState = PrivilegedAppsListState.DialogState.ConfirmDeleteTrustedApp(app),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `UserTrustedAppDeleteConfirmClick should show loading dialog then delete app`() = runTest {
        val app = PrivilegedAppListItem(
            packageName = "com.example.app",
            signature = "signature",
            trustAuthority = PrivilegedAppListItem.PrivilegedAppTrustAuthority.COMMUNITY,
            appName = "Example App",
        )
        val state = PrivilegedAppsListState(
            installedApps = persistentListOf(app),
            notInstalledApps = persistentListOf(),
            dialogState = PrivilegedAppsListState.DialogState.ConfirmDeleteTrustedApp(app),
        )
        val viewModel = createViewModel(state = state)
        viewModel.trySendAction(PrivilegedAppsListAction.UserTrustedAppDeleteConfirmClick(app))

        assert(
            viewModel.stateFlow.value == state.copy(
                dialogState = PrivilegedAppsListState.DialogState.Loading,
            ),
        )
        assertEquals(DataState.Loading, mutableTrustedAppDataStateFlow.value)
    }

    @Test
    fun `DismissDialogClick should hide dialog`() = runTest {
        val state = PrivilegedAppsListState(
            installedApps = persistentListOf(),
            notInstalledApps = persistentListOf(),
            dialogState = PrivilegedAppsListState.DialogState.ConfirmDeleteTrustedApp(
                PrivilegedAppListItem(
                    packageName = "com.example.app",
                    signature = "signature",
                    trustAuthority = PrivilegedAppListItem.PrivilegedAppTrustAuthority.COMMUNITY,
                    appName = "Example App",
                ),
            ),
        )
        val viewModel = createViewModel(state = state)
        viewModel.trySendAction(PrivilegedAppsListAction.DismissDialogClick)

        assertEquals(
            state.copy(dialogState = null),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on back click should send navigate back event`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(PrivilegedAppsListAction.BackClick)

            assertEquals(PrivilegedAppsListEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on DataState Loaded should update state with data and hide dialog`() = runTest {
        every {
            mockBitwardenPackageManager.isPackageInstalled(any())
        } returns false
        every {
            mockBitwardenPackageManager.isPackageInstalled("mockPackageName-5")
        } returns true
        every {
            mockBitwardenPackageManager.isPackageInstalled("mockPackageName-6")
        } returns true
        every {
            mockBitwardenPackageManager.getAppLabelForPackageOrNull(any())
        } returns null
        every {
            mockBitwardenPackageManager.getAppLabelForPackageOrNull("mockPackageName-5")
        } returns "App 5"
        every {
            mockBitwardenPackageManager.getAppLabelForPackageOrNull("mockPackageName-6")
        } returns "App 6"

        val viewModel = createViewModel(state = DEFAULT_STATE)

        mutableTrustedAppDataStateFlow.emit(
            DataState.Loaded(
                PrivilegedAppData(
                    googleTrustedApps = PrivilegedAppAllowListJson(
                        apps = listOf(
                            createMockPrivilegedAppJson(number = 1),
                            createMockPrivilegedAppJson(number = 2),
                        ),
                    ),
                    communityTrustedApps = PrivilegedAppAllowListJson(
                        apps = listOf(
                            createMockPrivilegedAppJson(number = 3),
                            createMockPrivilegedAppJson(number = 4),
                        ),
                    ),
                    userTrustedApps = PrivilegedAppAllowListJson(
                        apps = listOf(
                            createMockPrivilegedAppJson(number = 5),
                            createMockPrivilegedAppJson(number = 6),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(
            POPULATED_PRIVILEGED_APPS_LIST_STATE,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on DataState Loading should show loading dialog`() = runTest {
        val viewModel = createViewModel(state = DEFAULT_STATE)
        mutableTrustedAppDataStateFlow.emit(DataState.Loading)
        assert(
            viewModel.stateFlow.value == DEFAULT_STATE.copy(
                dialogState = PrivilegedAppsListState.DialogState.Loading,
            ),
        )
    }

    @Test
    fun `onDataState Pending should show data with loading dialog`() = runTest {
        every {
            mockBitwardenPackageManager.isPackageInstalled(any())
        } returns false
        every {
            mockBitwardenPackageManager.isPackageInstalled("mockPackageName-5")
        } returns true
        every {
            mockBitwardenPackageManager.isPackageInstalled("mockPackageName-6")
        } returns true
        every {
            mockBitwardenPackageManager.getAppLabelForPackageOrNull(any())
        } returns null
        every {
            mockBitwardenPackageManager.getAppLabelForPackageOrNull("mockPackageName-5")
        } returns "App 5"
        every {
            mockBitwardenPackageManager.getAppLabelForPackageOrNull("mockPackageName-6")
        } returns "App 6"

        val viewModel = createViewModel(state = DEFAULT_STATE)
        mutableTrustedAppDataStateFlow.emit(
            DataState.Pending(
                PrivilegedAppData(
                    googleTrustedApps = PrivilegedAppAllowListJson(
                        apps = listOf(
                            createMockPrivilegedAppJson(number = 1),
                            createMockPrivilegedAppJson(number = 2),
                        ),
                    ),
                    communityTrustedApps = PrivilegedAppAllowListJson(
                        apps = listOf(
                            createMockPrivilegedAppJson(number = 3),
                            createMockPrivilegedAppJson(number = 4),
                        ),
                    ),
                    userTrustedApps = PrivilegedAppAllowListJson(
                        apps = listOf(
                            createMockPrivilegedAppJson(number = 5),
                            createMockPrivilegedAppJson(number = 6),
                        ),
                    ),
                ),
            ),
        )
        assertEquals(
            POPULATED_PRIVILEGED_APPS_LIST_STATE
                .copy(dialogState = PrivilegedAppsListState.DialogState.Loading),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on DataState Error should show error dialog`() = runTest {
        val viewModel = createViewModel(state = DEFAULT_STATE)
        mutableTrustedAppDataStateFlow.emit(DataState.Error(Exception()))
        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = PrivilegedAppsListState.DialogState.General(
                    message = BitwardenString.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on DataState NoNetwork should display data`() = runTest {
        val viewModel = createViewModel(state = DEFAULT_STATE)
        mutableTrustedAppDataStateFlow.emit(DataState.NoNetwork())
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    private fun createViewModel(
        state: PrivilegedAppsListState? = null,
    ): PrivilegedAppsListViewModel = PrivilegedAppsListViewModel(
        privilegedAppRepository = mockPrivilegedAppRepository,
        bitwardenPackageManager = mockBitwardenPackageManager,
        savedStateHandle = SavedStateHandle().apply {
            set("state", state)
        },
    )
}

private val POPULATED_PRIVILEGED_APPS_LIST_STATE = PrivilegedAppsListState(
    installedApps = persistentListOf(
        createMockPrivilegedAppListItem(
            number = 5,
            appName = "App 5",
            trustAuthority = PrivilegedAppListItem.PrivilegedAppTrustAuthority.USER,
        ),
        createMockPrivilegedAppListItem(
            number = 6,
            appName = "App 6",
            trustAuthority = PrivilegedAppListItem.PrivilegedAppTrustAuthority.USER,
        ),
    ),
    notInstalledApps = persistentListOf(
        createMockPrivilegedAppListItem(
            number = 1,
            trustAuthority = PrivilegedAppListItem.PrivilegedAppTrustAuthority.GOOGLE,
        ),
        createMockPrivilegedAppListItem(
            number = 2,
            trustAuthority = PrivilegedAppListItem.PrivilegedAppTrustAuthority.GOOGLE,
        ),
        createMockPrivilegedAppListItem(
            number = 3,
            trustAuthority = PrivilegedAppListItem
                .PrivilegedAppTrustAuthority
                .COMMUNITY,
        ),
        createMockPrivilegedAppListItem(
            number = 4,
            trustAuthority = PrivilegedAppListItem
                .PrivilegedAppTrustAuthority
                .COMMUNITY,
        ),
    ),
    dialogState = null,
)

private val DEFAULT_STATE = PrivilegedAppsListState(
    installedApps = persistentListOf(),
    notInstalledApps = persistentListOf(),
    dialogState = null,
)

private fun createMockPrivilegedAppListItem(
    number: Int = 1,
    trustAuthority: PrivilegedAppListItem.PrivilegedAppTrustAuthority =
        PrivilegedAppListItem.PrivilegedAppTrustAuthority.COMMUNITY,
    appName: String? = null,
    packageName: String = "mockPackageName-$number",
    signature: String = "mockSignature-$number",
): PrivilegedAppListItem = PrivilegedAppListItem(
    packageName = packageName,
    signature = signature,
    trustAuthority = trustAuthority,
    appName = appName,
)
