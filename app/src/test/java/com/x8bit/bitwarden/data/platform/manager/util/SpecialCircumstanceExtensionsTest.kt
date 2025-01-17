package com.x8bit.bitwarden.data.platform.manager.util

import android.content.pm.SigningInfo
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CreateCredentialRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.createMockFido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.createMockFido2GetCredentialsRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.ui.vault.model.TotpData
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
            SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = mockk(),
            ),
            SpecialCircumstance.Fido2Assertion(
                fido2AssertionRequest = mockk(),
            ),
            SpecialCircumstance.Fido2GetCredentials(
                fido2GetCredentialsRequest = mockk(),
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
            SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = mockk(),
            ),
            SpecialCircumstance.Fido2Assertion(
                fido2AssertionRequest = mockk(),
            ),
            SpecialCircumstance.Fido2GetCredentials(
                fido2GetCredentialsRequest = mockk(),
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
            SpecialCircumstance.Fido2GetCredentials(
                fido2GetCredentialsRequest = mockk(),
            ),
            SpecialCircumstance.GeneratorShortcut,
            SpecialCircumstance.VaultShortcut,
        )
            .forEach { specialCircumstance ->
                assertNull(specialCircumstance.toFido2CreateRequestOrNull())
            }
    }

    @Test
    fun `toFido2RequestOrNull should return a non-null value for Fido2Save`() {
        val fido2CreateCredentialRequest = Fido2CreateCredentialRequest(
            userId = "mockUserId",
            requestJson = "mockRequestJson",
            packageName = "mockPackageName",
            signingInfo = SigningInfo(),
            origin = "mockOrigin",
        )
        assertEquals(
            fido2CreateCredentialRequest,
            SpecialCircumstance
                .Fido2Save(
                    fido2CreateCredentialRequest = fido2CreateCredentialRequest,
                )
                .toFido2CreateRequestOrNull(),
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
            SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = mockk(),
            ),
            SpecialCircumstance.Fido2GetCredentials(
                fido2GetCredentialsRequest = mockk(),
            ),
            SpecialCircumstance.GeneratorShortcut,
            SpecialCircumstance.VaultShortcut,
        )
            .forEach { specialCircumstance ->
                assertNull(specialCircumstance.toFido2AssertionRequestOrNull())
            }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toFido2GetCredentialsRequestOrNull should return a non-null value for Fido2GetCredentials`() {
        val fido2GetCredentialsRequest = createMockFido2GetCredentialsRequest(number = 1)
        assertEquals(
            fido2GetCredentialsRequest,
            SpecialCircumstance
                .Fido2GetCredentials(
                    fido2GetCredentialsRequest = fido2GetCredentialsRequest,
                )
                .toFido2GetCredentialsRequestOrNull(),
        )
    }

    @Test
    fun `toFido2GetCredentialsRequestOrNull should return a null value for other types`() {
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
            SpecialCircumstance.Fido2Save(
                fido2CreateCredentialRequest = mockk(),
            ),
            SpecialCircumstance.Fido2Assertion(
                fido2AssertionRequest = mockk(),
            ),
            SpecialCircumstance.GeneratorShortcut,
            SpecialCircumstance.VaultShortcut,
        )
            .forEach { specialCircumstance ->
                assertNull(specialCircumstance.toFido2GetCredentialsRequestOrNull())
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
            mockk<SpecialCircumstance.Fido2Save>(),
            mockk<SpecialCircumstance.Fido2Assertion>(),
            mockk<SpecialCircumstance.RegistrationEvent>(),
            SpecialCircumstance.GeneratorShortcut,
            SpecialCircumstance.VaultShortcut,
        )
            .forEach { specialCircumstance ->
                assertNull(specialCircumstance.toTotpDataOrNull())
            }
    }
}
