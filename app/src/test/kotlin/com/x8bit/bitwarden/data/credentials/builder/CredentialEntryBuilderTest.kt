package com.x8bit.bitwarden.data.credentials.builder

import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Icon
import androidx.core.graphics.drawable.IconCompat
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.PasswordCredentialEntry
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.util.mockBuilder
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFido2CredentialAutofillView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockPasswordCredentialAutofillCipherLogin
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
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
    private val mockIntentManager = mockk<IntentManager> {
        every {
            createFido2GetCredentialPendingIntent(
                action = any(),
                userId = any(),
                cipherId = any(),
                credentialId = any(),
                requestCode = any(),
                isUserVerified = any(),
            )
        } returns mockGetPublicKeyCredentialIntent
        every {
            createPasswordGetCredentialPendingIntent(
                action = any(),
                userId = any(),
                cipherId = any(),
                requestCode = any(),
                isUserVerified = any(),
            )
        } returns mockGetPasswordCredentialIntent
    }
    private val mockFeatureFlagManager = mockk<FeatureFlagManager>()
    private val mockBiometricsEncryptionManager = mockk<BiometricsEncryptionManager>()
    private val mockBeginGetPublicKeyOption = mockk<BeginGetPublicKeyCredentialOption>()
    private val mockBeginGetPasswordOption = mockk<BeginGetPasswordOption>()
    private val credentialEntryBuilder = CredentialEntryBuilderImpl(
        context = mockContext,
        intentManager = mockIntentManager,
        featureFlagManager = mockFeatureFlagManager,
        biometricsEncryptionManager = mockBiometricsEncryptionManager,
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

            every {
                mockFeatureFlagManager.getFeatureFlag(FlagKey.SingleTapPasskeyAuthentication)
            } returns false
            every {
                mockBiometricsEncryptionManager.getOrCreateCipher("userId")
            } returns null

            val result = credentialEntryBuilder
                .buildPublicKeyCredentialEntries(
                    userId = "userId",
                    isUserVerified = false,
                    fido2CredentialAutofillViews = fido2AutofillViews,
                    beginGetPublicKeyCredentialOptions = options,
                )

            assertTrue(result.isNotEmpty())

            verify {
                mockIntentManager.createFido2GetCredentialPendingIntent(
                    action = "com.x8bit.bitwarden.credentials.ACTION_GET_PASSKEY",
                    userId = "userId",
                    cipherId = "mockCipherId-1",
                    credentialId = fido2AutofillViews.first().credentialId.toString(),
                    requestCode = any(),
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

        // Verify biometric prompt data is not set when flag is false, buildVersion is at least 35,
        // and cipher is null.
        every {
            mockFeatureFlagManager.getFeatureFlag(FlagKey.SingleTapPasskeyAuthentication)
        } returns false
        every {
            mockBiometricsEncryptionManager.getOrCreateCipher("userId")
        } returns null
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

        // Verify biometric prompt data is not set when flag is true, buildVersion is below 35, and
        // cipher is null.
        every {
            mockFeatureFlagManager.getFeatureFlag(FlagKey.SingleTapPasskeyAuthentication)
        } returns true
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

        // Verify biometric prompt data is not set when flag is true, buildVersion is at least 35,
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
        every {
            mockBiometricsEncryptionManager.getOrCreateCipher(any())
        } returns mockk(relaxed = true)
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

        // Verify biometric prompt data is set when flag is true, buildVersion is >= 35, cipher is
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
            val passwordCredentialAutofillViews: List<AutofillCipher.Login> = listOf(
                createMockPasswordCredentialAutofillCipherLogin(),
            )

            val result = credentialEntryBuilder
                .buildPasswordCredentialEntries(
                    userId = "userId",
                    isUserVerified = false,
                    passwordCredentialAutofillViews = passwordCredentialAutofillViews,
                    beginGetPasswordCredentialOptions = options,
                )
            assertTrue(result.isEmpty())
        }

    @Suppress("MaxLineLength")
    @Test
    fun `buildPasswordCredentialEntries should return Success with empty list when passwordAutofillViews is empty`() =
        runTest {
            val options = listOf(mockBeginGetPasswordOption)
            val passwordCredentialAutofillViews = emptyList<AutofillCipher.Login>()
            val result = credentialEntryBuilder
                .buildPasswordCredentialEntries(
                    userId = "userId",
                    isUserVerified = false,
                    passwordCredentialAutofillViews = passwordCredentialAutofillViews,
                    beginGetPasswordCredentialOptions = options,
                )
            assertTrue(result.isEmpty())
        }

    @Suppress("MaxLineLength")
    @Test
    fun `buildPasswordCredentialEntries should return Success with list of PasswordCredentialEntry`() =
        runTest {
            val options = listOf(mockBeginGetPasswordOption)
            val passwordAutofillViews: List<AutofillCipher.Login> = listOf(
                createMockPasswordCredentialAutofillCipherLogin(),
            )
            every {
                mockBiometricsEncryptionManager.getOrCreateCipher("userId")
            } returns null

            val result = credentialEntryBuilder
                .buildPasswordCredentialEntries(
                    userId = "userId",
                    isUserVerified = false,
                    passwordCredentialAutofillViews = passwordAutofillViews,
                    beginGetPasswordCredentialOptions = options,
                )

            assertTrue(result.isNotEmpty())

            verify {
                mockIntentManager.createPasswordGetCredentialPendingIntent(
                    action = "com.x8bit.bitwarden.credentials.ACTION_GET_PASSWORD",
                    userId = "userId",
                    cipherId = "mockCipherId",
                    requestCode = any(),
                    isUserVerified = false,
                )

                anyConstructed<PasswordCredentialEntry.Builder>().setIcon(mockIcon)
            }
        }

    @Test
    fun `buildPasswordCredentialEntries should set biometric prompt data correctly`() = runTest {
        mockkStatic(::isBuildVersionBelow)
        val options = listOf(mockBeginGetPasswordOption)
        val passwordAutofillViews: List<AutofillCipher.Login> = listOf(
            createMockPasswordCredentialAutofillCipherLogin(),
        )

        every {
            mockBiometricsEncryptionManager.getOrCreateCipher(any())
        } returns mockk(relaxed = true)
        every { isBuildVersionBelow(any()) } returns false

        // Verify biometric prompt data is set when buildVersion is >= 35, cipher is
        // not null, and user is not verified
        credentialEntryBuilder
            .buildPasswordCredentialEntries(
                userId = "userId",
                isUserVerified = false,
                passwordCredentialAutofillViews = passwordAutofillViews,
                beginGetPasswordCredentialOptions = options,
            )
        verify(exactly = 1) {
            anyConstructed<PasswordCredentialEntry.Builder>().setBiometricPromptData(any())
        }
    }
}
