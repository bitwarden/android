package com.x8bit.bitwarden.data.autofill.builder

import android.view.autofill.AutofillId
import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.autofill.model.FilledData
import com.x8bit.bitwarden.data.autofill.model.FilledItem
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FilledDataBuilderTest {
    private lateinit var filledDataBuilder: FilledDataBuilder

    @BeforeEach
    fun setup() {
        filledDataBuilder = FilledDataBuilderImpl()
    }

    @Test
    fun `build should return FilledData with FilledItems and ignored AutofillIds`() = runTest {
        // Setup
        val autofillId: AutofillId = mockk()
        val autofillView = AutofillView.Identity.PostalCode(
            autofillId = autofillId,
            isFocused = false,
        )
        val autofillPartition = AutofillPartition.Identity(
            views = listOf(autofillView),
        )
        val ignoreAutofillIds: List<AutofillId> = mockk()
        val autofillRequest = AutofillRequest.Fillable(
            ignoreAutofillIds = ignoreAutofillIds,
            partition = autofillPartition,
        )
        val filledItem = FilledItem(
            autofillId = autofillId,
        )
        val expected = FilledData(
            filledItems = listOf(
                filledItem,
            ),
            ignoreAutofillIds = ignoreAutofillIds,
        )

        // Test
        val actual = filledDataBuilder.build(
            autofillRequest = autofillRequest,
        )

        // Verify
        assertEquals(expected, actual)
    }
}
