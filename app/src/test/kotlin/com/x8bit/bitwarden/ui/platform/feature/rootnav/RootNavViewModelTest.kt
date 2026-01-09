package com.x8bit.bitwarden.ui.platform.feature.rootnav

import androidx.core.os.bundleOf
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.provider.ProviderCreateCredentialRequest
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.cxf.model.ImportCredentialsRequestData
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.network.model.JwtTokenDataJson
import com.bitwarden.network.model.OrganizationType
import com.bitwarden.network.util.parseJwtTokenDataOrNull
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.manager.share.model.ShareData
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.credentials.model.CreateCredentialRequest
import com.x8bit.bitwarden.data.credentials.model.createMockFido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.credentials.model.createMockGetCredentialsRequest
import com.x8bit.bitwarden.data.credentials.model.createMockProviderGetPasswordCredentialRequest
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManagerImpl
import com.x8bit.bitwarden.data.platform.manager.model.CompleteRegistrationData
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.vault.manager.VaultMigrationManager
import com.x8bit.bitwarden.data.vault.manager.model.VaultMigrationData
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Suppress("LargeClass")
class RootNavViewModelTest : BaseViewModelTest() {
    private val mutableAuthStateFlow = MutableStateFlow<AuthState>(AuthState.Uninitialized)
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(null)
    private val authRepository = mockk<AuthRepository> {
        every { userStateFlow } returns mutableUserStateFlow
        every { authStateFlow } returns mutableAuthStateFlow
        every { showWelcomeCarousel } returns false
    }

    private val mockAuthRepository = mockk<AuthRepository>(relaxed = true)
    private val specialCircumstanceManager: SpecialCircumstanceManager =
        SpecialCircumstanceManagerImpl(
            authRepository = mockAuthRepository,
            dispatcherManager = FakeDispatcherManager(),
        )

    private val mutableVaultMigrationDataStateFlow =
        MutableStateFlow<VaultMigrationData>(VaultMigrationData.NoMigrationRequired)
    private val vaultMigrationManager = mockk<VaultMigrationManager> {
        every { vaultMigrationDataStateFlow } returns mutableVaultMigrationDataStateFlow
    }

    @BeforeEach
    fun setup() {
        mockkStatic(::parseJwtTokenDataOrNull)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::parseJwtTokenDataOrNull)
        unmockkObject(ProviderCreateCredentialRequest.Companion)
    }

    @Test
    fun `when there are no accounts the nav state should be Auth`() {
        mutableUserStateFlow.tryEmit(null)
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.Auth,
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when there are no accounts and the user has not logged on before the nav state should be Auth with the welcome route`() {
        every { authRepository.showWelcomeCarousel } returns true
        mutableUserStateFlow.tryEmit(null)
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.AuthWithWelcome,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `when the active user is not logged in the nav state should be Auth`() {
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = false,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.Auth,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `when the active user needs a password reset the nav state should be ResetPassword`() {
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = false,
                        isVaultUnlocked = false,
                        needsPasswordReset = true,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(RootNavState.ResetPassword, viewModel.stateFlow.value)
    }

    @Test
    fun `when the active user needs a master password the nav state should be SetPassword`() {
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = false,
                        isVaultUnlocked = false,
                        needsPasswordReset = true,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = true,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.SetPassword,
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an untrusted device without password the nav state should be TrustedDevice`() {
        every { authRepository.tdeLoginComplete } returns null
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = false,
                        isVaultUnlocked = false,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = UserState.TrustedDevice(
                            isDeviceTrusted = false,
                            hasAdminApproval = true,
                            hasLoginApprovingDevice = true,
                            hasResetPasswordPermission = false,
                        ),
                        hasMasterPassword = false,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(RootNavState.TrustedDevice, viewModel.stateFlow.value)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an untrusted device with password the nav state should be TrustedDevice`() {
        every { authRepository.tdeLoginComplete } returns null
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = false,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = UserState.TrustedDevice(
                            isDeviceTrusted = false,
                            hasAdminApproval = true,
                            hasLoginApprovingDevice = true,
                            hasResetPasswordPermission = false,
                        ),
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(RootNavState.TrustedDevice, viewModel.stateFlow.value)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an untrusted device but has completed TDE login the nav state should be Auth`() {
        every { authRepository.tdeLoginComplete } returns true
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = false,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = UserState.TrustedDevice(
                            isDeviceTrusted = false,
                            hasAdminApproval = true,
                            hasLoginApprovingDevice = true,
                            hasResetPasswordPermission = false,
                        ),
                        hasMasterPassword = false,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.Auth,
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user but there are pending account additions the nav state should be Auth`() {
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
                hasPendingAccountAddition = true,
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.Auth,
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault with an external user, a key-connector user account and is not currently using key connector the nav state should be RemovePassword`() {
        val jwtTokenDataJson = mockk<JwtTokenDataJson> {
            every { isExternal } returns true
        }
        every { parseJwtTokenDataOrNull(ACCESS_TOKEN) } returns jwtTokenDataJson
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = listOf(
                            Organization(
                                id = "orgId",
                                name = "orgName",
                                shouldManageResetPassword = false,
                                shouldUseKeyConnector = true,
                                role = OrganizationType.USER,
                                keyConnectorUrl = "bitwarden.com",
                                userIsClaimedByOrganization = false,
                            ),
                        ),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        mutableAuthStateFlow.value = AuthState.Authenticated(accessToken = ACCESS_TOKEN)
        val viewModel = createViewModel()
        assertEquals(RootNavState.RemovePassword, viewModel.stateFlow.value)
    }

    @Test
    fun `when the active user has an unlocked vault the nav state should be VaultUnlocked`() {
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.VaultUnlocked(activeUserId = "activeUserId"),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault but there is an AddTotpLoginItem special circumstance the nav state should be VaultUnlockedForNewTotp`() {
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.AddTotpLoginItem(data = mockk())
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.VaultUnlockedForNewTotp(activeUserId = "activeUserId"),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault but there is an AccountSecurityShortcut special circumstance the nav state should be VaultUnlocked`() {
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.AccountSecurityShortcut
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        firstTimeState = FirstTimeState(false),
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.VaultUnlocked(activeUserId = "activeUserId"),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault but the is a ShareNewSend special circumstance the nav state should be VaultUnlockedForNewSend`() {
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.ShareNewSend(
                data = mockk<ShareData.TextSend>(),
                shouldFinishWhenComplete = true,
            )
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.VaultUnlockedForNewSend(sendType = SendItemType.TEXT),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault but there is an AutofillSave special circumstance the nav state should be VaultUnlockedForAutofillSave`() {
        val autofillSaveItem: AutofillSaveItem = mockk()
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.AutofillSave(
                autofillSaveItem = autofillSaveItem,
            )
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.VaultUnlockedForAutofillSave(
                autofillSaveItem = autofillSaveItem,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault but there is an AutofillSelection special circumstance the nav state should be VaultUnlockedForAutofillSelection`() {
        val autofillSelectionData = AutofillSelectionData(
            type = AutofillSelectionData.Type.LOGIN,
            framework = AutofillSelectionData.Framework.AUTOFILL,
            uri = "uri",
        )
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.AutofillSelection(
                autofillSelectionData = autofillSelectionData,
                shouldFinishWhenComplete = true,
            )
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.VaultUnlockedForAutofillSelection(
                activeUserId = "activeUserId",
                type = AutofillSelectionData.Type.LOGIN,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault but there is a Fido2Save special circumstance the nav state should be VaultUnlockedForFido2Save`() {
        mockkObject(ProviderCreateCredentialRequest.Companion)

        val createCredentialRequest = CreateCredentialRequest(
            userId = "activeUserId",
            isUserPreVerified = false,
            requestData = bundleOf(),
        )

        every { ProviderCreateCredentialRequest.fromBundle(any()) } returns mockk {
            every { callingRequest } returns mockk<CreatePublicKeyCredentialRequest>()
        }

        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.ProviderCreateCredential(createCredentialRequest)
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarHexColor",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.VaultUnlockedForFido2Save(
                activeUserId = "activeUserId",
                createCredentialRequest = createCredentialRequest,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault but there is a ProviderCreateCredential with password request the nav state should be VaultUnlockedForCreatePasswordRequest`() {
        mockkObject(ProviderCreateCredentialRequest.Companion)

        val mockUsername = "testUser"
        val mockPassword = "testPassword123"
        val mockPackageName = "com.example.app"

        val createCredentialRequest = CreateCredentialRequest(
            userId = "activeUserId",
            isUserPreVerified = false,
            requestData = bundleOf(),
        )

        every { ProviderCreateCredentialRequest.fromBundle(any()) } returns mockk {
            every { callingRequest } returns mockk<CreatePasswordRequest> {
                every { id } returns mockUsername
                every { password } returns mockPassword
            }
            every { callingAppInfo } returns mockk {
                every { packageName } returns mockPackageName
            }
        }

        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.ProviderCreateCredential(createCredentialRequest)
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarHexColor",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.VaultUnlockedForCreatePasswordRequest(
                username = mockUsername,
                password = mockPassword,
                uri = "androidapp://$mockPackageName",
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault but there is a Fido2Assertion special circumstance the nav state should be VaultUnlockedForFido2Assertion`() {
        val fido2CredentialAssertionRequest =
            createMockFido2CredentialAssertionRequest(number = 1)
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.Fido2Assertion(fido2CredentialAssertionRequest)
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarHexColor",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.VaultUnlockedForFido2Assertion(
                activeUserId = "activeUserId",
                fido2CredentialAssertionRequest = fido2CredentialAssertionRequest,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault but there is a ProviderGetPasswordRequest special circumstance the nav state should be VaultUnlockedForPasswordGet`() {
        val providerGetPasswordCredentialRequest =
            createMockProviderGetPasswordCredentialRequest(1)
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.ProviderGetPasswordRequest(providerGetPasswordCredentialRequest)
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarHexColor",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.VaultUnlockedForPasswordGet(
                activeUserId = "activeUserId",
                providerGetPasswordCredentialRequest = providerGetPasswordCredentialRequest,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault but there is a ProviderGetCredentials special circumstance the nav state should be VaultUnlockedForProviderGetCredentials`() {
        val fido2GetCredentialsRequest = createMockGetCredentialsRequest(number = 1)
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.ProviderGetCredentials(fido2GetCredentialsRequest)
        mutableUserStateFlow.tryEmit(MOCK_VAULT_UNLOCKED_USER_STATE)
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.VaultUnlockedForProviderGetCredentials(
                activeUserId = "activeUserId",
                getCredentialsRequest = fido2GetCredentialsRequest,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault but there is an SendShortcut special circumstance the nav state should be VaultUnlocked`() {
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.SendShortcut
        mutableUserStateFlow.tryEmit(MOCK_VAULT_UNLOCKED_USER_STATE)
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.VaultUnlocked(activeUserId = "activeUserId"),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault but there is an VerificationCodeShortcut special circumstance the nav state should be VaultUnlocked`() {
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.VerificationCodeShortcut
        mutableUserStateFlow.tryEmit(MOCK_VAULT_UNLOCKED_USER_STATE)
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.VaultUnlocked(activeUserId = "activeUserId"),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault but there is an SearchShortcut special circumstance the nav state should be VaultUnlocked`() {
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.SearchShortcut("")
        mutableUserStateFlow.tryEmit(MOCK_VAULT_UNLOCKED_USER_STATE)
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.VaultUnlocked(activeUserId = "activeUserId"),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when there are no accounts but there is a CompleteRegistration special circumstance the nav state should be CompleteRegistration`() {
        every { authRepository.hasPendingAccountAddition } returns false

        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.RegistrationEvent.CompleteRegistration(
                CompleteRegistrationData(
                    email = "example@email.com",
                    verificationToken = "token",
                    fromEmail = true,
                ),
                FIXED_CLOCK.instant().toEpochMilli(),
            )
        mutableUserStateFlow.tryEmit(null)
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.CompleteOngoingRegistration(
                email = "example@email.com",
                verificationToken = "token",
                fromEmail = true,
                timestamp = FIXED_CLOCK.instant().toEpochMilli(),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault but there is a CompleteRegistration special circumstance the nav state should be CompleteRegistration`() {
        every { authRepository.hasPendingAccountAddition } returns true

        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.RegistrationEvent.CompleteRegistration(
                CompleteRegistrationData(
                    email = "example@email.com",
                    verificationToken = "token",
                    fromEmail = true,
                ),
                FIXED_CLOCK.instant().toEpochMilli(),
            )
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarHexColor",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.CompleteOngoingRegistration(
                email = "example@email.com",
                verificationToken = "token",
                fromEmail = true,
                timestamp = FIXED_CLOCK.instant().toEpochMilli(),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has a locked vault but there is a CompleteRegistration special circumstance the nav state should be CompleteRegistration`() {
        every { authRepository.hasPendingAccountAddition } returns true

        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.RegistrationEvent.CompleteRegistration(
                CompleteRegistrationData(
                    email = "example@email.com",
                    verificationToken = "token",
                    fromEmail = true,
                ),
                FIXED_CLOCK.instant().toEpochMilli(),
            )
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = false,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.CompleteOngoingRegistration(
                email = "example@email.com",
                verificationToken = "token",
                fromEmail = true,
                timestamp = FIXED_CLOCK.instant().toEpochMilli(),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when there are no accounts but there is a ExpiredRegistrationLink special circumstance the nav state should be ExpiredRegistrationLink`() {
        every { authRepository.hasPendingAccountAddition } returns false

        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.RegistrationEvent.ExpiredRegistrationLink
        mutableUserStateFlow.tryEmit(null)
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.ExpiredRegistrationLink,
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault but there is a ExpiredRegistrationLink special circumstance the nav state should be ExpiredRegistrationLink`() {
        every { authRepository.hasPendingAccountAddition } returns true

        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.RegistrationEvent.ExpiredRegistrationLink
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarHexColor",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.ExpiredRegistrationLink,
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has a locked vault but there is a ExpiredRegistrationLink special circumstance the nav state should be ExpiredRegistrationLink`() {
        every { authRepository.hasPendingAccountAddition } returns true

        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.RegistrationEvent.ExpiredRegistrationLink
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = false,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.ExpiredRegistrationLink,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `when the active user has a locked vault the nav state should be VaultLocked`() {
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = false,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(RootNavState.VaultLocked, viewModel.stateFlow.value)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault and they have a OnboardingStatus of NOT_STARTED the nav state should be OnboardingAccountLockSetup`() {
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.NOT_STARTED,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.OnboardingAccountLockSetup,
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault and they have a OnboardingStatus of ACCOUNT_LOCK_SETUP the nav state should be OnboardingAccountLockSetup`() {
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.ACCOUNT_LOCK_SETUP,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.OnboardingAccountLockSetup,
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault and they have a OnboardingStatus of AUTOFILL_SETUP the nav state should be OnboardingAutoFillSetup`() {
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.AUTOFILL_SETUP,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.OnboardingAutoFillSetup,
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault and they have a OnboardingStatus of BROWSER_AUTOFILL_SETUP the nav state should be OnboardingBrowserAutofillSetup`() {
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.BROWSER_AUTOFILL_SETUP,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.OnboardingBrowserAutofillSetup,
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault and they have a OnboardingStatus of FINAL_STEP the nav state should be OnboardingAutoFillSetup`() {
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.FINAL_STEP,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.OnboardingStepsComplete,
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an locked vault and they have a OnboardingStatus of NOT_STARTED the nav state should be VaultLocked`() {
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = false,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.NOT_STARTED,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.VaultLocked,
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault and they have a OnboardingStatus of null the nav state should be VaultUnlocked`() {
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.VaultUnlocked(activeUserId = "activeUserId"),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when the active user has an unlocked vault and they have a OnboardingStatus of COMPLETED the nav state should be VaultUnlocked`() {
        mutableUserStateFlow.tryEmit(
            UserState(
                activeUserId = "activeUserId",
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "name",
                        email = "email",
                        avatarColorHex = "avatarColorHex",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = true,
                        needsPasswordReset = false,
                        isBiometricsEnabled = false,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(
                            showImportLoginsCard = true,
                        ),
                        isExportable = true,
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.VaultUnlocked(activeUserId = "activeUserId"),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when SpecialCircumstance is CredentialExchangeExport the nav state should be CredentialExchangeExport`() {
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.CredentialExchangeExport(
                data = ImportCredentialsRequestData(
                    uri = mockk(),
                    requestJson = "mockRequestJson",
                ),
            )
        mutableUserStateFlow.tryEmit(MOCK_VAULT_UNLOCKED_USER_MULTIPLE_ACCOUNTS_STATE)
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.CredentialExchangeExport,
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when SpecialCircumstance is CredentialExchangeExport and only has 1 account, the nav state should be CredentialExchangeExportSkipAccountSelection`() {
        specialCircumstanceManager.specialCircumstance =
            SpecialCircumstance.CredentialExchangeExport(
                data = ImportCredentialsRequestData(
                    uri = mockk(),
                    requestJson = "mockRequestJson",
                ),
            )
        mutableUserStateFlow.tryEmit(MOCK_VAULT_UNLOCKED_USER_STATE)
        val viewModel = createViewModel()
        assertEquals(
            RootNavState.CredentialExchangeExportSkipAccountSelection(
                userId = "activeUserId",
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when vaultMigrationDataStateFlow emits true the nav state should be MigrateToMyItems`() {
        mutableVaultMigrationDataStateFlow.value = MOCK_VAULT_MIGRATION_DATA
        mutableUserStateFlow.tryEmit(MOCK_VAULT_UNLOCKED_USER_STATE)
        val viewModel = createViewModel()

        assertEquals(
            RootNavState.MigrateToMyItems(
                organizationId = "mockOrganizationId-1",
                organizationName = "organizationName",
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `when vault is locked MigrateToMyItems should not show even if flow emits true`() {
        mutableVaultMigrationDataStateFlow.value = MOCK_VAULT_MIGRATION_DATA

        // Vault is locked
        mutableUserStateFlow.tryEmit(
            MOCK_VAULT_UNLOCKED_USER_STATE.copy(
                accounts = listOf(
                    MOCK_VAULT_UNLOCKED_USER_STATE.activeAccount.copy(isVaultUnlocked = false),
                ),
            ),
        )
        val viewModel = createViewModel()

        assertEquals(
            RootNavState.VaultLocked,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `when vaultMigrationDataStateFlow emits false MigrateToMyItems should not show`() {
        mutableVaultMigrationDataStateFlow.value = VaultMigrationData.NoMigrationRequired
        mutableUserStateFlow.tryEmit(MOCK_VAULT_UNLOCKED_USER_STATE)
        val viewModel = createViewModel()

        assertEquals(
            RootNavState.VaultUnlocked("activeUserId"),
            viewModel.stateFlow.value,
        )
    }

    private fun createViewModel(): RootNavViewModel =
        RootNavViewModel(
            authRepository = authRepository,
            specialCircumstanceManager = specialCircumstanceManager,
            vaultMigrationManager = vaultMigrationManager,
        )
}

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)

private const val ACCESS_TOKEN: String = "access_token"

private val MOCK_VAULT_UNLOCKED_USER_STATE = UserState(
    activeUserId = "activeUserId",
    accounts = listOf(
        UserState.Account(
            userId = "activeUserId",
            name = "name",
            email = "email",
            avatarColorHex = "avatarColorHex",
            environment = Environment.Us,
            isPremium = true,
            isLoggedIn = true,
            isVaultUnlocked = true,
            needsPasswordReset = false,
            isBiometricsEnabled = false,
            organizations = emptyList(),
            needsMasterPassword = false,
            trustedDevice = null,
            hasMasterPassword = true,
            isUsingKeyConnector = false,
            firstTimeState = FirstTimeState(false),
            onboardingStatus = OnboardingStatus.COMPLETE,
            isExportable = true,
        ),
    ),
)

private val MOCK_VAULT_UNLOCKED_USER_MULTIPLE_ACCOUNTS_STATE = UserState(
    activeUserId = "activeUserId",
    accounts = listOf(
        UserState.Account(
            userId = "activeUserId",
            name = "name",
            email = "email",
            avatarColorHex = "avatarColorHex",
            environment = Environment.Us,
            isPremium = true,
            isLoggedIn = true,
            isVaultUnlocked = true,
            needsPasswordReset = false,
            isBiometricsEnabled = false,
            organizations = emptyList(),
            needsMasterPassword = false,
            trustedDevice = null,
            hasMasterPassword = true,
            isUsingKeyConnector = false,
            firstTimeState = FirstTimeState(false),
            onboardingStatus = OnboardingStatus.COMPLETE,
            isExportable = true,
        ),

        UserState.Account(
            userId = "activeUserTwoId",
            name = "name two",
            email = "email two",
            avatarColorHex = "avatarColorHex",
            environment = Environment.Us,
            isPremium = true,
            isLoggedIn = true,
            isVaultUnlocked = true,
            needsPasswordReset = false,
            isBiometricsEnabled = false,
            organizations = emptyList(),
            needsMasterPassword = false,
            trustedDevice = null,
            hasMasterPassword = true,
            isUsingKeyConnector = false,
            firstTimeState = FirstTimeState(false),
            onboardingStatus = OnboardingStatus.COMPLETE,
            isExportable = true,
        ),
    ),
)

private val MOCK_VAULT_MIGRATION_DATA = VaultMigrationData.MigrationRequired(
    organizationName = "organizationName",
    organizationId = "mockOrganizationId-1",
)
