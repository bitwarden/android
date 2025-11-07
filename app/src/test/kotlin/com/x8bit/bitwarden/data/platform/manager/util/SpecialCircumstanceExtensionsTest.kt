package com.x8bit.bitwarden.data.platform.manager.util

import androidx.core.os.bundleOf
import com.bitwarden.cxf.model.ImportCredentialsRequestData
import com.bitwarden.ui.platform.model.TotpData
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.credentials.model.CreateCredentialRequest
import com.x8bit.bitwarden.data.credentials.model.createMockFido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.credentials.model.createMockGetCredentialsRequest
import com.x8bit.bitwarden.data.credentials.model.createMockProviderGetPasswordCredentialRequest
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SpecialCircumstanceExtensionsTest {

    @Test
    fun `toAutofillSaveItemOrNull should return a non-null value for AutofillSave`() {
        val autofillSaveItem: AutofillSaveItem = mockk()
        assertEquals(
            autofillSaveItem,
            SpecialCircumstance
                .AutofillSave(
                    autofillSaveItem = autofillSaveItem,
                )
                .toAutofillSaveItemOrNull(),
        )
    }

    @Test
    fun `toAutofillSaveItemOrNull should return a null value for other types`() {
        listOf(
            SpecialCircumstance.AutofillSelection(
                autofillSelectionData = mockk(),
                shouldFinishWhenComplete = true,
            ),
            SpecialCircumstance.ShareNewSend(
                data = mockk(),
                shouldFinishWhenComplete = true,
            ),
            mockk<SpecialCircumstance.AddTotpLoginItem>(),
            SpecialCircumstance.PasswordlessRequest(
                passwordlessRequestData = mockk(),
                shouldFinishWhenComplete = true,
            ),
            SpecialCircumstance.ProviderCreateCredential(
                createCredentialRequest = mockk(),
            ),
            SpecialCircumstance.Fido2Assertion(
                fido2AssertionRequest = mockk(),
            ),
            SpecialCircumstance.ProviderGetPasswordRequest(
                passwordGetRequest = mockk(),
            ),
            SpecialCircumstance.ProviderGetCredentials(
                getCredentialsRequest = mockk(),
            ),
            SpecialCircumstance.GeneratorShortcut,
            SpecialCircumstance.VaultShortcut,
        )
            .forEach { specialCircumstance ->
                assertNull(specialCircumstance.toAutofillSaveItemOrNull())
            }
    }

    @Test
    fun `toAutofillSelectionDataOrNull should return a non-null value for AutofillSelection`() {
        val autofillSelectionData = AutofillSelectionData(
            type = AutofillSelectionData.Type.LOGIN,
            framework = AutofillSelectionData.Framework.AUTOFILL,
            uri = "uri",
        )
        assertEquals(
            autofillSelectionData,
            SpecialCircumstance
                .AutofillSelection(
                    autofillSelectionData = autofillSelectionData,
                    shouldFinishWhenComplete = true,
                )
                .toAutofillSelectionDataOrNull(),
        )
    }

    @Test
    fun `toAutofillSelectionDataOrNull should return a null value for other types`() {
        listOf(
            SpecialCircumstance.AutofillSave(
                autofillSaveItem = mockk(),
            ),
            SpecialCircumstance.ShareNewSend(
                data = mockk(),
                shouldFinishWhenComplete = true,
            ),
            mockk<SpecialCircumstance.AddTotpLoginItem>(),
            SpecialCircumstance.PasswordlessRequest(
                passwordlessRequestData = mockk(),
                shouldFinishWhenComplete = true,
            ),
            SpecialCircumstance.ProviderCreateCredential(
                createCredentialRequest = mockk(),
            ),
            SpecialCircumstance.Fido2Assertion(
                fido2AssertionRequest = mockk(),
            ),
            SpecialCircumstance.ProviderGetPasswordRequest(
                passwordGetRequest = mockk(),
            ),
            SpecialCircumstance.ProviderGetCredentials(
                getCredentialsRequest = mockk(),
            ),
            SpecialCircumstance.GeneratorShortcut,
            SpecialCircumstance.VaultShortcut,
        )
            .forEach { specialCircumstance ->
                assertNull(specialCircumstance.toAutofillSelectionDataOrNull())
            }
    }

    @Test
    fun `toFido2RequestOrNull should return a null value for other types`() {
        listOf(
            SpecialCircumstance.AutofillSelection(
                autofillSelectionData = mockk(),
                shouldFinishWhenComplete = true,
            ),
            SpecialCircumstance.AutofillSave(
                autofillSaveItem = mockk(),
            ),
            mockk<SpecialCircumstance.AddTotpLoginItem>(),
            SpecialCircumstance.ShareNewSend(
                data = mockk(),
                shouldFinishWhenComplete = true,
            ),
            SpecialCircumstance.PasswordlessRequest(
                passwordlessRequestData = mockk(),
                shouldFinishWhenComplete = true,
            ),
            SpecialCircumstance.Fido2Assertion(
                fido2AssertionRequest = mockk(),
            ),
            SpecialCircumstance.ProviderGetPasswordRequest(
                passwordGetRequest = mockk(),
            ),
            SpecialCircumstance.ProviderGetCredentials(
                getCredentialsRequest = mockk(),
            ),
            SpecialCircumstance.GeneratorShortcut,
            SpecialCircumstance.VaultShortcut,
        )
            .forEach { specialCircumstance ->
                assertNull(specialCircumstance.toCreateCredentialRequestOrNull())
            }
    }

    @Test
    fun `toFido2RequestOrNull should return a non-null value for Fido2Save`() {
        val createCredentialRequest = CreateCredentialRequest(
            userId = "mockUserId",
            isUserPreVerified = false,
            requestData = bundleOf(),
        )
        assertEquals(
            createCredentialRequest,
            SpecialCircumstance
                .ProviderCreateCredential(
                    createCredentialRequest = createCredentialRequest,
                )
                .toCreateCredentialRequestOrNull(),
        )
    }

    @Test
    fun `toFido2AssertionRequestOrNull should return a non-null value for Fido2Assertion`() {
        val fido2CredentialAssertionRequest =
            createMockFido2CredentialAssertionRequest(number = 1)

        assertEquals(
            fido2CredentialAssertionRequest,
            SpecialCircumstance
                .Fido2Assertion(
                    fido2AssertionRequest = fido2CredentialAssertionRequest,
                )
                .toFido2AssertionRequestOrNull(),
        )
    }

    @Test
    fun `toFido2AssertionRequestOrNull should return a null value for other types`() {
        listOf(
            SpecialCircumstance.AutofillSelection(
                autofillSelectionData = mockk(),
                shouldFinishWhenComplete = true,
            ),
            SpecialCircumstance.AutofillSave(
                autofillSaveItem = mockk(),
            ),
            SpecialCircumstance.ShareNewSend(
                data = mockk(),
                shouldFinishWhenComplete = true,
            ),
            mockk<SpecialCircumstance.AddTotpLoginItem>(),
            SpecialCircumstance.PasswordlessRequest(
                passwordlessRequestData = mockk(),
                shouldFinishWhenComplete = true,
            ),
            SpecialCircumstance.ProviderGetPasswordRequest(
                passwordGetRequest = mockk(),
            ),
            SpecialCircumstance.ProviderCreateCredential(
                createCredentialRequest = mockk(),
            ),
            SpecialCircumstance.ProviderGetCredentials(
                getCredentialsRequest = mockk(),
            ),
            SpecialCircumstance.GeneratorShortcut,
            SpecialCircumstance.VaultShortcut,
        )
            .forEach { specialCircumstance ->
                assertNull(specialCircumstance.toFido2AssertionRequestOrNull())
            }
    }

    @Test
    fun `toGetCredentialsRequestOrNull should return a non-null value for GetCredentials`() {
        val getCredentialsRequest = createMockGetCredentialsRequest(number = 1)
        assertEquals(
            getCredentialsRequest,
            SpecialCircumstance
                .ProviderGetCredentials(
                    getCredentialsRequest = getCredentialsRequest,
                )
                .toGetCredentialsRequestOrNull(),
        )
    }

    @Test
    fun `toGetCredentialsRequestOrNull should return a null value for other types`() {
        listOf(
            SpecialCircumstance.AutofillSelection(
                autofillSelectionData = mockk(),
                shouldFinishWhenComplete = true,
            ),
            SpecialCircumstance.AutofillSave(
                autofillSaveItem = mockk(),
            ),
            SpecialCircumstance.ShareNewSend(
                data = mockk(),
                shouldFinishWhenComplete = true,
            ),
            mockk<SpecialCircumstance.AddTotpLoginItem>(),
            SpecialCircumstance.PasswordlessRequest(
                passwordlessRequestData = mockk(),
                shouldFinishWhenComplete = true,
            ),
            SpecialCircumstance.ProviderCreateCredential(
                createCredentialRequest = mockk(),
            ),
            SpecialCircumstance.Fido2Assertion(
                fido2AssertionRequest = mockk(),
            ),
            SpecialCircumstance.ProviderGetPasswordRequest(
                passwordGetRequest = mockk(),
            ),
            SpecialCircumstance.GeneratorShortcut,
            SpecialCircumstance.VaultShortcut,
        )
            .forEach { specialCircumstance ->
                assertNull(specialCircumstance.toGetCredentialsRequestOrNull())
            }
    }

    @Test
    fun `toPasswordGetRequestOrNull should return a non-null value for PasswordGetCredentials`() {
        val passwordGetCredentialsRequest = createMockProviderGetPasswordCredentialRequest(1)
        assertEquals(
            passwordGetCredentialsRequest,
            SpecialCircumstance
                .ProviderGetPasswordRequest(
                    passwordGetRequest = passwordGetCredentialsRequest,
                )
                .toPasswordGetRequestOrNull(),
        )
    }

    @Test
    fun `toPasswordGetRequestOrNull should return a null value for other types`() {
        listOf(
            SpecialCircumstance.AutofillSelection(
                autofillSelectionData = mockk(),
                shouldFinishWhenComplete = true,
            ),
            SpecialCircumstance.AutofillSave(
                autofillSaveItem = mockk(),
            ),
            SpecialCircumstance.ShareNewSend(
                data = mockk(),
                shouldFinishWhenComplete = true,
            ),
            mockk<SpecialCircumstance.AddTotpLoginItem>(),
            SpecialCircumstance.PasswordlessRequest(
                passwordlessRequestData = mockk(),
                shouldFinishWhenComplete = true,
            ),
            SpecialCircumstance.ProviderCreateCredential(
                createCredentialRequest = mockk(),
            ),
            SpecialCircumstance.ProviderGetCredentials(
                getCredentialsRequest = mockk(),
            ),
            SpecialCircumstance.Fido2Assertion(
                fido2AssertionRequest = mockk(),
            ),
            SpecialCircumstance.GeneratorShortcut,
            SpecialCircumstance.VaultShortcut,
        )
            .forEach { specialCircumstance ->
                assertNull(specialCircumstance.toPasswordGetRequestOrNull())
            }
    }

    @Test
    fun `toTotpDataOrNull should return a non-null value for AddTotpLoginItem`() {
        val totpData = mockk<TotpData>()
        assertEquals(
            totpData,
            SpecialCircumstance.AddTotpLoginItem(data = totpData).toTotpDataOrNull(),
        )
    }

    @Test
    fun `toTotpDataOrNull should return a null value for other types`() {
        listOf(
            mockk<SpecialCircumstance.AutofillSelection>(),
            mockk<SpecialCircumstance.AutofillSave>(),
            mockk<SpecialCircumstance.ShareNewSend>(),
            mockk<SpecialCircumstance.PasswordlessRequest>(),
            mockk<SpecialCircumstance.ProviderCreateCredential>(),
            mockk<SpecialCircumstance.Fido2Assertion>(),
            mockk<SpecialCircumstance.ProviderGetPasswordRequest>(),
            mockk<SpecialCircumstance.RegistrationEvent>(),
            SpecialCircumstance.GeneratorShortcut,
            SpecialCircumstance.VaultShortcut,
        )
            .forEach { specialCircumstance ->
                assertNull(specialCircumstance.toTotpDataOrNull())
            }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toImportCredentialsRequestDataOrNull should return a non-null value for ImportCredentials`() {
        val importCredentialsRequestData = ImportCredentialsRequestData(
            uri = mockk(),
            requestJson = "",
        )
        assertEquals(
            importCredentialsRequestData,
            SpecialCircumstance
                .CredentialExchangeExport(
                    data = importCredentialsRequestData,
                )
                .toImportCredentialsRequestDataOrNull(),
        )
    }

    @Test
    fun `toImportCredentialsRequestDataOrNull should return a null value for other types`() {
        listOf(
            SpecialCircumstance.AutofillSelection(
                autofillSelectionData = mockk(),
                shouldFinishWhenComplete = true,
            ),
            SpecialCircumstance.AutofillSave(
                autofillSaveItem = mockk(),
            ),
            SpecialCircumstance.ShareNewSend(
                data = mockk(),
                shouldFinishWhenComplete = true,
            ),
            mockk<SpecialCircumstance.AddTotpLoginItem>(),
            SpecialCircumstance.PasswordlessRequest(
                passwordlessRequestData = mockk(),
                shouldFinishWhenComplete = true,
            ),
            SpecialCircumstance.ProviderCreateCredential(
                createCredentialRequest = mockk(),
            ),
            SpecialCircumstance.ProviderGetCredentials(
                getCredentialsRequest = mockk(),
            ),
            SpecialCircumstance.Fido2Assertion(
                fido2AssertionRequest = mockk(),
            ),
            SpecialCircumstance.GeneratorShortcut,
            SpecialCircumstance.VaultShortcut,
        ).forEach { specialCircumstance ->
            assertNull(specialCircumstance.toImportCredentialsRequestDataOrNull())
        }
    }
}
