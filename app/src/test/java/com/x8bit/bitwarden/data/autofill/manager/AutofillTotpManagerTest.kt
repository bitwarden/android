package com.x8bit.bitwarden.data.autofill.manager

import android.content.Context
import android.widget.Toast
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.LoginView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.GenerateTotpResult
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class AutofillTotpManagerTest {
    private val context: Context = mockk {
        every { applicationContext } returns this
    }
    private val toast: Toast = mockk {
        every { show() } just runs
    }
    private val loginView: LoginView = mockk()
    private val cipherView: CipherView = mockk {
        every { id } returns "cipherId"
        every { login } returns loginView
    }
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(value = null)
    private val authRepository: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
    }
    private val clipboardManager: BitwardenClipboardManager = mockk {
        every { setText(any<String>()) } just runs
    }
    private val settingsRepository: SettingsRepository = mockk()
    private val vaultRepository: VaultRepository = mockk()

    private val autofillTotpManager: AutofillTotpManager = AutofillTotpManagerImpl(
        context = context,
        clock = FIXED_CLOCK,
        clipboardManager = clipboardManager,
        authRepository = authRepository,
        settingsRepository = settingsRepository,
        vaultRepository = vaultRepository,
    )

    @BeforeEach
    fun setup() {
        mockkStatic(Toast::class)
        every {
            Toast.makeText(context, R.string.verification_code_totp, Toast.LENGTH_LONG)
        } returns toast
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Toast::class)
    }

    @Test
    fun `tryCopyTotpToClipboard when isAutoCopyTotpDisabled is true should do nothing`() = runTest {
        every { settingsRepository.isAutoCopyTotpDisabled } returns true

        autofillTotpManager.tryCopyTotpToClipboard(cipherView = cipherView)

        verify(exactly = 1) {
            settingsRepository.isAutoCopyTotpDisabled
        }
        verify(exactly = 0) {
            clipboardManager.setText(any<String>())
            toast.show()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `tryCopyTotpToClipboard when isAutoCopyTotpDisabled is false, user not premium and the organization does not use totp should do nothing`() =
        runTest {
            every { settingsRepository.isAutoCopyTotpDisabled } returns false
            every { cipherView.organizationUseTotp } returns false
            every { cipherView.organizationId } returns null
            mutableUserStateFlow.value = mockk {
                every { activeAccount.isPremium } returns false
                every { activeAccount.organizations } returns emptyList()
            }

            autofillTotpManager.tryCopyTotpToClipboard(cipherView = cipherView)

            verify(exactly = 0) {
                clipboardManager.setText(any<String>())
                toast.show()
            }
            verify(exactly = 1) {
                settingsRepository.isAutoCopyTotpDisabled
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `tryCopyTotpToClipboard when isAutoCopyTotpDisabled is false, has premium and does not have totp should do nothing`() =
        runTest {
            every { settingsRepository.isAutoCopyTotpDisabled } returns false
            every { cipherView.organizationUseTotp } returns true
            every { cipherView.organizationId } returns null
            mutableUserStateFlow.value = mockk {
                every { activeAccount.isPremium } returns true
                every { activeAccount.organizations } returns emptyList()
            }
            every { loginView.totp } returns null

            autofillTotpManager.tryCopyTotpToClipboard(cipherView = cipherView)

            verify(exactly = 0) {
                clipboardManager.setText(any<String>())
                toast.show()
            }
            verify(exactly = 1) {
                settingsRepository.isAutoCopyTotpDisabled
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `tryCopyTotpToClipboard when isAutoCopyTotpDisabled is false, has premium and has totp should set the clipboard and toast`() =
        runTest {
            val generateTotpResult = GenerateTotpResult.Success(
                code = TOTP_RESULT_VALUE,
                periodSeconds = 100,
            )
            every { settingsRepository.isAutoCopyTotpDisabled } returns false
            every { cipherView.organizationUseTotp } returns true
            every { cipherView.organizationId } returns null
            mutableUserStateFlow.value = mockk {
                every { activeAccount.isPremium } returns true
                every { activeAccount.organizations } returns emptyList()
            }
            every { loginView.totp } returns TOTP_CODE
            coEvery {
                vaultRepository.generateTotp(time = FIXED_CLOCK.instant(), totpCode = TOTP_CODE)
            } returns generateTotpResult

            autofillTotpManager.tryCopyTotpToClipboard(cipherView = cipherView)

            verify(exactly = 1) {
                clipboardManager.setText(text = TOTP_RESULT_VALUE)
                settingsRepository.isAutoCopyTotpDisabled
                toast.show()
            }
            coVerify(exactly = 1) {
                vaultRepository.generateTotp(time = FIXED_CLOCK.instant(), totpCode = TOTP_CODE)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `tryCopyTotpToClipboard when isAutoCopyTotpDisabled is false, user has premium but item belongs to an org that doesn't should do nothing`() =
        runTest {
            val orgId = "orgId"
            val mockOrganization = mockk<Organization>(relaxed = true) {
                every { id } returns orgId
                every { shouldUsersGetPremium } returns false
            }
            every { settingsRepository.isAutoCopyTotpDisabled } returns false
            every { cipherView.organizationUseTotp } returns false
            every { cipherView.organizationId } returns orgId
            mutableUserStateFlow.value = mockk {
                every { activeAccount.isPremium } returns true
                every { activeAccount.organizations } returns listOf(mockOrganization)
            }

            autofillTotpManager.tryCopyTotpToClipboard(cipherView = cipherView)

            verify(exactly = 0) {
                clipboardManager.setText(text = TOTP_RESULT_VALUE)
                toast.show()
            }
            coVerify(exactly = 0) {
                vaultRepository.generateTotp(time = FIXED_CLOCK.instant(), totpCode = TOTP_CODE)
            }
        }
}

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)

private const val TOTP_CODE: String = "TOTP_CODE"
private const val TOTP_RESULT_VALUE: String = "TOTP_RESULT_VALUE"
