package com.x8bit.bitwarden.data.credentials.builder

import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Icon
import androidx.core.graphics.drawable.IconCompat
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.PasswordCredentialEntry
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.bitwarden.core.data.util.mockBuilder
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherListViewType
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.credentials.manager.CredentialManagerPendingIntentManager
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFido2CredentialAutofillView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockLoginListView
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CredentialEntryBuilderTest {

    private val mockContext = mockk<Context>()
    private val mockGetPublicKeyCredentialIntent = mockk<PendingIntent>(relaxed = true)
    private val mockGetPasswordCredentialIntent = mockk<PendingIntent>(relaxed = true)
    private val mockPendingIntentManager = mockk<CredentialManagerPendingIntentManager> {
        every {
            createFido2GetCredentialPendingIntent(
                userId = any(),
                cipherId = any(),
                credentialId = any(),
                isUserVerified = any(),
            )
        } returns mockGetPublicKeyCredentialIntent
        every {
            createPasswordGetCredentialPendingIntent(
                userId = any(),
                cipherId = any(),
                isUserVerified = any(),
            )
        } returns mockGetPasswordCredentialIntent
    }
    private val mockAuthRepository = mockk<AuthRepository>()
    private val mockBeginGetPublicKeyOption = mockk<BeginGetPublicKeyCredentialOption>()
    private val mockBeginGetPasswordOption = mockk<BeginGetPasswordOption>()
    private val credentialEntryBuilder = CredentialEntryBuilderImpl(
        context = mockContext,
        pendingIntentManager = mockPendingIntentManager,
        authRepository = mockAuthRepository,
    )
    private val mockPublicKeyCredentialEntry = mockk<PublicKeyCredentialEntry>(relaxed = true)
    private val mockPasswordCredentialEntry = mockk<PasswordCredentialEntry>(relaxed = true)
    private val mockIcon = mockk<Icon>()

    @BeforeEach
    @Test
    fun setUp() {
        mockkConstructor(PublicKeyCredentialEntry.Builder::class)
        mockkConstructor(PasswordCredentialEntry.Builder::class)
        mockkStatic(IconCompat::class)
        mockBuilder<PublicKeyCredentialEntry.Builder> { it.setIcon(any()) }
        mockBuilder<PasswordCredentialEntry.Builder> { it.setIcon(any()) }
        every { IconCompat.createWithResource(any(), any()) } returns mockk {
            every { toIcon(mockContext) } returns mockIcon
        }
        every {
            anyConstructed<PublicKeyCredentialEntry.Builder>().build()
        } returns mockPublicKeyCredentialEntry
        every {
            anyConstructed<PasswordCredentialEntry.Builder>().build()
        } returns mockPasswordCredentialEntry
    }

    @AfterEach
    @Test
    fun tearDown() {
        unmockkStatic(IconCompat::class)
        unmockkStatic(::isBuildVersionAtLeast)
        unmockkConstructor(PublicKeyCredentialEntry.Builder::class)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `buildPublicKeyCredentialEntries should return Success with empty list when options list is empty`() =
        runTest {
            val options = emptyList<BeginGetPublicKeyCredentialOption>()
            val fido2AutofillViews: List<Fido2CredentialAutofillView> = listOf(
                createMockFido2CredentialAutofillView(number = 1),
            )

            val result = credentialEntryBuilder
                .buildPublicKeyCredentialEntries(
                    userId = "userId",
                    isUserVerified = false,
                    fido2CredentialAutofillViews = fido2AutofillViews,
                    beginGetPublicKeyCredentialOptions = options,
                )
            assertTrue(result.isEmpty())
        }

    @Suppress("MaxLineLength")
    @Test
    fun `buildPublicKeyCredentialEntries should return Success with empty list when fido2AutofillViews is empty`() =
        runTest {
            val options = listOf(mockBeginGetPublicKeyOption)
            val fido2AutofillViews = emptyList<Fido2CredentialAutofillView>()
            val result = credentialEntryBuilder
                .buildPublicKeyCredentialEntries(
                    userId = "userId",
                    isUserVerified = false,
                    fido2CredentialAutofillViews = fido2AutofillViews,
                    beginGetPublicKeyCredentialOptions = options,
                )
            assertTrue(result.isEmpty())
        }

    @Suppress("MaxLineLength")
    @Test
    fun `buildPublicKeyCredentialEntries should return Success with list of PublicKeyCredentialEntry`() =
        runTest {
            val options = listOf(mockBeginGetPublicKeyOption)
            val fido2AutofillViews: List<Fido2CredentialAutofillView> = listOf(
                createMockFido2CredentialAutofillView(number = 1),
            )

            every { mockAuthRepository.getOrCreateCipher("userId") } returns null

            val result = credentialEntryBuilder
                .buildPublicKeyCredentialEntries(
                    userId = "userId",
                    isUserVerified = false,
                    fido2CredentialAutofillViews = fido2AutofillViews,
                    beginGetPublicKeyCredentialOptions = options,
                )

            assertTrue(result.isNotEmpty())

            verify {
                mockPendingIntentManager.createFido2GetCredentialPendingIntent(
                    userId = "userId",
                    cipherId = "mockCipherId-1",
                    credentialId = fido2AutofillViews.first().credentialId.toString(),
                    isUserVerified = false,
                )

                anyConstructed<PublicKeyCredentialEntry.Builder>().setIcon(mockIcon)
            }
        }

    @Test
    fun `buildPublicKeyCredentialEntries should set biometric prompt data correctly`() = runTest {
        mockkStatic(::isBuildVersionAtLeast)
        val options = listOf(mockBeginGetPublicKeyOption)
        val fido2AutofillViews: List<Fido2CredentialAutofillView> = listOf(
            createMockFido2CredentialAutofillView(number = 1),
        )

        // Verify biometric prompt data is not set when buildVersion is at least 35
        // and cipher is null.
        every { mockAuthRepository.getOrCreateCipher("userId") } returns null
        every { isBuildVersionAtLeast(any()) } returns true

        credentialEntryBuilder
            .buildPublicKeyCredentialEntries(
                userId = "userId",
                isUserVerified = false,
                fido2CredentialAutofillViews = fido2AutofillViews,
                beginGetPublicKeyCredentialOptions = options,
            )
        verify(exactly = 0) {
            anyConstructed<PublicKeyCredentialEntry.Builder>().setBiometricPromptData(any())
        }

        // Verify biometric prompt data is not set when buildVersion is below 35 and cipher is null.
        credentialEntryBuilder
            .buildPublicKeyCredentialEntries(
                userId = "userId",
                isUserVerified = false,
                fido2CredentialAutofillViews = fido2AutofillViews,
                beginGetPublicKeyCredentialOptions = options,
            )

        verify(exactly = 0) {
            anyConstructed<PublicKeyCredentialEntry.Builder>().setBiometricPromptData(any())
        }

        // Verify biometric prompt data is not set when buildVersion is at least 35
        // and cipher is null
        every { isBuildVersionAtLeast(any()) } returns true
        credentialEntryBuilder
            .buildPublicKeyCredentialEntries(
                userId = "userId",
                isUserVerified = false,
                fido2CredentialAutofillViews = fido2AutofillViews,
                beginGetPublicKeyCredentialOptions = options,
            )
        verify(exactly = 0) {
            anyConstructed<PublicKeyCredentialEntry.Builder>().setBiometricPromptData(any())
        }

        // Verify biometric prompt data is not set when user is verified
        every { mockAuthRepository.getOrCreateCipher(any()) } returns mockk(relaxed = true)
        credentialEntryBuilder
            .buildPublicKeyCredentialEntries(
                userId = "userId",
                isUserVerified = true,
                fido2CredentialAutofillViews = fido2AutofillViews,
                beginGetPublicKeyCredentialOptions = options,
            )
        verify(exactly = 0) {
            anyConstructed<PublicKeyCredentialEntry.Builder>().setBiometricPromptData(any())
        }

        // Verify biometric prompt data is set when buildVersion is >= 35, cipher is
        // not null, and user is not verified
        credentialEntryBuilder
            .buildPublicKeyCredentialEntries(
                userId = "userId",
                isUserVerified = false,
                fido2CredentialAutofillViews = fido2AutofillViews,
                beginGetPublicKeyCredentialOptions = options,
            )
        verify(exactly = 1) {
            anyConstructed<PublicKeyCredentialEntry.Builder>().setBiometricPromptData(any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `buildPasswordCredentialEntries should return Success with empty list when options list is empty`() =
        runTest {
            val options = emptyList<BeginGetPasswordOption>()
            val cipherListViews = listOf(
                createMockCipherListView(
                    number = 1,
                    type = CipherListViewType.Login(
                        createMockLoginListView(
                            number = 1,
                            hasFido2 = true,
                            uris = emptyList(),
                        ),
                    ),
                ),
            )

            val result = credentialEntryBuilder
                .buildPasswordCredentialEntries(
                    userId = "userId",
                    cipherListViews = cipherListViews,
                    beginGetPasswordCredentialOptions = options,
                    isUserVerified = false,
                )
            assertTrue(result.isEmpty())
        }

    @Suppress("MaxLineLength")
    @Test
    fun `buildPasswordCredentialEntries should return Success with empty list when passwordAutofillViews is empty`() =
        runTest {
            val options = listOf(mockBeginGetPasswordOption)
            val cipherListViews = emptyList<CipherListView>()
            val result = credentialEntryBuilder
                .buildPasswordCredentialEntries(
                    userId = "userId",
                    cipherListViews = cipherListViews,
                    beginGetPasswordCredentialOptions = options,
                    isUserVerified = false,
                )
            assertTrue(result.isEmpty())
        }

    @Suppress("MaxLineLength")
    @Test
    fun `buildPasswordCredentialEntries should return Success with list of PasswordCredentialEntry`() =
        runTest {
            val options = listOf(mockBeginGetPasswordOption)
            val cipherListViews = listOf(
                createMockCipherListView(
                    number = 1,
                    type = CipherListViewType.Login(
                        createMockLoginListView(
                            number = 1,
                            hasFido2 = true,
                            uris = emptyList(),
                        ),
                    ),
                ),
            )
            every { mockAuthRepository.getOrCreateCipher("userId") } returns null

            val result = credentialEntryBuilder
                .buildPasswordCredentialEntries(
                    userId = "userId",
                    cipherListViews = cipherListViews,
                    beginGetPasswordCredentialOptions = options,
                    isUserVerified = false,
                )

            assertTrue(result.isNotEmpty())

            verify {
                mockPendingIntentManager.createPasswordGetCredentialPendingIntent(
                    userId = "userId",
                    cipherId = "mockId-1",
                    isUserVerified = false,
                )

                anyConstructed<PasswordCredentialEntry.Builder>().setIcon(mockIcon)
            }
        }

    @Test
    fun `buildPasswordCredentialEntries should set biometric prompt data correctly`() = runTest {
        mockkStatic(::isBuildVersionAtLeast)
        val options = listOf(mockBeginGetPasswordOption)
        val cipherListViews = listOf(
            createMockCipherListView(
                number = 1,
                type = CipherListViewType.Login(
                    createMockLoginListView(
                        number = 1,
                        hasFido2 = true,
                        uris = emptyList(),
                    ),
                ),
            ),
        )

        every { mockAuthRepository.getOrCreateCipher(any()) } returns mockk(relaxed = true)
        every { isBuildVersionAtLeast(any()) } returns true

        // Verify biometric prompt data is set when buildVersion is >= 35, cipher is
        // not null, and user is not verified
        credentialEntryBuilder
            .buildPasswordCredentialEntries(
                userId = "userId",
                cipherListViews = cipherListViews,
                beginGetPasswordCredentialOptions = options,
                isUserVerified = false,
            )
        verify(exactly = 1) {
            anyConstructed<PasswordCredentialEntry.Builder>().setBiometricPromptData(any())
        }
    }
}
