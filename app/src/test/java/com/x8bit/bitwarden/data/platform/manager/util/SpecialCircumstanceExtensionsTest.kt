package com.x8bit.bitwarden.data.platform.manager.util

import android.content.pm.SigningInfo
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
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
            SpecialCircumstance.PasswordlessRequest(
                passwordlessRequestData = mockk(),
                shouldFinishWhenComplete = true,
            ),
            SpecialCircumstance.Fido2Save(
                fido2CredentialRequest = mockk(),
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
            SpecialCircumstance.PasswordlessRequest(
                passwordlessRequestData = mockk(),
                shouldFinishWhenComplete = true,
            ),
            SpecialCircumstance.Fido2Save(
                fido2CredentialRequest = mockk(),
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
            SpecialCircumstance.ShareNewSend(
                data = mockk(),
                shouldFinishWhenComplete = true,
            ),
            SpecialCircumstance.PasswordlessRequest(
                passwordlessRequestData = mockk(),
                shouldFinishWhenComplete = true,
            ),
            SpecialCircumstance.GeneratorShortcut,
            SpecialCircumstance.VaultShortcut,
        )
            .forEach { specialCircumstance ->
                assertNull(specialCircumstance.toFido2RequestOrNull())
            }
    }

    @Test
    fun `toFido2RequestOrNull should return a non-null value for Fido2Save`() {
        val fido2CredentialRequest = Fido2CredentialRequest(
            userId = "mockUserId",
            requestJson = "mockRequestJson",
            packageName = "mockPackageName",
            signingInfo = SigningInfo(),
            origin = "mockOrigin",
        )
        assertEquals(
            fido2CredentialRequest,
            SpecialCircumstance
                .Fido2Save(
                    fido2CredentialRequest = fido2CredentialRequest,
                )
                .toFido2RequestOrNull(),
        )
    }
}
