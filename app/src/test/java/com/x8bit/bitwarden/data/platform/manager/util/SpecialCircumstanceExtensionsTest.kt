package com.x8bit.bitwarden.data.platform.manager.util

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
            SpecialCircumstance.GeneratorShortcut,
            SpecialCircumstance.VaultShortcut,
        )
            .forEach { specialCircumstance ->
                assertNull(specialCircumstance.toAutofillSelectionDataOrNull())
            }
    }
}
