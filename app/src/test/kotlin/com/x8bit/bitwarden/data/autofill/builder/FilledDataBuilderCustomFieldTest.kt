package com.x8bit.bitwarden.data.autofill.builder

import com.bitwarden.vault.FieldType

import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.data.autofill.model.AutofillField
import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.autofill.model.FilledData
import com.x8bit.bitwarden.data.autofill.model.FilledItem
import com.x8bit.bitwarden.data.autofill.model.FilledPartition
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProvider
import com.x8bit.bitwarden.data.autofill.util.buildFilledItemOrNull
import com.x8bit.bitwarden.data.autofill.util.buildUri
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FilledDataBuilderCustomFieldTest {
    private lateinit var filledDataBuilder: FilledDataBuilder

    private val autofillCipherProvider: AutofillCipherProvider = mockk {
        coEvery { isVaultLocked() } returns false
    }

    @BeforeEach
    fun setup() {
        mockkStatic(AutofillValue::forText)
        mockkStatic(AutofillView::buildFilledItemOrNull)
        mockkStatic("com.x8bit.bitwarden.data.autofill.util.AutofillViewExtensionsKt")
        filledDataBuilder = FilledDataBuilderImpl(
            autofillCipherProvider = autofillCipherProvider,
        )
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(AutofillValue::forText)
        unmockkStatic(AutofillView::buildFilledItemOrNull)
        unmockkStatic("com.x8bit.bitwarden.data.autofill.util.AutofillViewExtensionsKt")
    }



    @Test
    fun `build should fill custom field when strict match is true and hint matches`() = runTest {
        val customValue = "CustomValue"
        val customFieldName = "My Field"
        val autofillCipher = createAutofillCipher(
            isStrictMatch = true,
            customFields = listOf(AutofillField(customFieldName, customValue, FieldType.TEXT))
        )

        val filledItemCustom: FilledItem = mockk()
        val autofillViewCustom: AutofillView.Login.Custom = mockk {
            every { data } returns mockk {
                every { website } returns URI
                every { hint } returns customFieldName
                every { idEntry } returns null
            }
            every { buildFilledItemOrNull(customValue) } returns filledItemCustom
        }

        val result = buildFilledData(autofillCipher, listOf(autofillViewCustom))

        assertEquals(1, result.filledPartitions.size)
        assertEquals(listOf(filledItemCustom), result.filledPartitions[0].filledItems)
    }

    @Test
    fun `build should fill custom field when strict match is true and idEntry matches`() = runTest {
        val customValue = "CustomValue"
        val customFieldName = "My Field"
        val autofillCipher = createAutofillCipher(
            isStrictMatch = true,
            customFields = listOf(AutofillField(customFieldName, customValue, FieldType.TEXT))
        )

        val filledItemCustom: FilledItem = mockk()
        val autofillViewCustom: AutofillView.Login.Custom = mockk {
            every { data } returns mockk {
                every { website } returns URI
                every { hint } returns null
                every { idEntry } returns "some_prefix_${customFieldName}_suffix"
            }
            every { buildFilledItemOrNull(customValue) } returns filledItemCustom
        }

        val result = buildFilledData(autofillCipher, listOf(autofillViewCustom))

        assertEquals(1, result.filledPartitions.size)
        assertEquals(listOf(filledItemCustom), result.filledPartitions[0].filledItems)
    }

    @Test
    fun `build should NOT fill custom field when strict match is false`() = runTest {
        val customValue = "CustomValue"
        val customFieldName = "My Field"
        val autofillCipher = createAutofillCipher(
            isStrictMatch = false,
            customFields = listOf(AutofillField(customFieldName, customValue, FieldType.TEXT))
        )

        val autofillViewCustom: AutofillView.Login.Custom = mockk {
            every { data } returns mockk {
                every { website } returns URI
                every { hint } returns customFieldName
                every { idEntry } returns null
            }
            // Should not be called with value, or called with null?
            // In implementation: autofillView.buildFilledItemOrNull(value = null) -> returns null
            // buildFilledItemOrNull should not be called
        }

        val result = buildFilledData(autofillCipher, listOf(autofillViewCustom))

        assertEquals(0, result.filledPartitions.size)
    }

    @Test
    fun `build should NOT fill custom field when name does not match`() = runTest {
        val customValue = "CustomValue"
        val customFieldName = "My Field"
        val autofillCipher = createAutofillCipher(
            isStrictMatch = true,
            customFields = listOf(AutofillField(customFieldName, customValue, FieldType.TEXT))
        )

        val autofillViewCustom: AutofillView.Login.Custom = mockk {
            every { data } returns mockk {
                every { website } returns URI
                every { hint } returns "Other Field"
                every { idEntry } returns "other_id"
            }
            // buildFilledItemOrNull should not be called
        }

        val result = buildFilledData(autofillCipher, listOf(autofillViewCustom))

        assertEquals(0, result.filledPartitions.size)
    }

    private fun createAutofillCipher(
        isStrictMatch: Boolean,
        customFields: List<AutofillField>
    ): AutofillCipher.Login {
        return AutofillCipher.Login(
            cipherId = null,
            name = "Cipher One",
            isTotpEnabled = false,
            password = "password",
            username = "username",
            subtitle = "Subtitle",
            website = URI,
            isStrictMatch = isStrictMatch,
            customFields = customFields
        )
    }

    private suspend fun buildFilledData(
        autofillCipher: AutofillCipher.Login,
        views: List<AutofillView.Login>
    ): FilledData {
        val autofillPartition = AutofillPartition.Login(views = views)
        val autofillRequest = AutofillRequest.Fillable(
            ignoreAutofillIds = emptyList(),
            inlinePresentationSpecs = emptyList(),
            maxInlineSuggestionsCount = 0,
            packageName = null,
            partition = autofillPartition,
            uri = URI,
        )

        coEvery {
            autofillCipherProvider.getLoginAutofillCiphers(uri = URI)
        } returns listOf(autofillCipher)

        return filledDataBuilder.build(autofillRequest)
    }

    companion object {
        private const val URI: String = "androidapp://com.x8bit.bitwarden"
    }
}
