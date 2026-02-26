package com.x8bit.bitwarden.data.autofill.builder

import android.service.autofill.FillRequest
import android.service.autofill.SaveInfo
import android.view.View
import android.view.autofill.AutofillId
import com.bitwarden.core.data.util.mockBuilder
import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SaveInfoBuilderTest {
    private lateinit var saveInfoBuilder: SaveInfoBuilder

    private val settingsRepository: SettingsRepository = mockk()

    private val fillRequest: FillRequest = mockk {
        every { id } returns 55
    }
    private val autofillIdOptional: AutofillId = mockk()
    private val autofillViewDataOptional = AutofillView.Data(
        autofillId = autofillIdOptional,
        autofillOptions = emptyList(),
        autofillType = View.AUTOFILL_TYPE_TEXT,
        isFocused = true,
        textValue = null,
        hasPasswordTerms = false,
        website = null,
    )
    private val autofillIdValid: AutofillId = mockk()
    private val autofillViewDataValid = AutofillView.Data(
        autofillId = autofillIdValid,
        autofillOptions = emptyList(),
        autofillType = View.AUTOFILL_TYPE_TEXT,
        isFocused = true,
        textValue = null,
        hasPasswordTerms = false,
        website = null,
    )
    private val autofillPartitionCard: AutofillPartition.Card = AutofillPartition.Card(
        views = listOf(
            AutofillView.Card.Number(
                data = autofillViewDataValid,
            ),
            AutofillView.Card.SecurityCode(
                data = autofillViewDataOptional,
            ),
        ),
    )
    private val autofillPartitionLogin: AutofillPartition.Login = AutofillPartition.Login(
        views = listOf(
            AutofillView.Login.Password(
                data = autofillViewDataValid,
            ),
            AutofillView.Login.Username(
                data = autofillViewDataOptional,
            ),
        ),
    )
    private val saveInfo: SaveInfo = mockk()

    @BeforeEach
    fun setup() {
        mockkConstructor(SaveInfo.Builder::class)
        saveInfoBuilder = SaveInfoBuilderImpl(
            settingsRepository = settingsRepository,
        )
        every { anyConstructed<SaveInfo.Builder>().build() } returns saveInfo
    }

    @AfterEach
    fun teardown() {
        unmockkConstructor(SaveInfo.Builder::class)
    }

    @Test
    fun `build should return null if autofill disabled`() {
        // Setup
        every { settingsRepository.isAutofillSavePromptDisabled } returns true

        // Test
        val actual = saveInfoBuilder.build(
            autofillPartition = autofillPartitionCard,
            fillRequest = fillRequest,
            packageName = PACKAGE_NAME,
        )

        // Verify
        assertNull(actual)
    }

    @Test
    fun `build should return null if autofill enabled and can't perform autofill`() {
        // Setup
        every { settingsRepository.isAutofillSavePromptDisabled } returns false

        // Test
        val actual = saveInfoBuilder.build(
            autofillPartition = AUTOFILL_PARTITION_LOGIN_EMPTY,
            fillRequest = fillRequest,
            packageName = PACKAGE_NAME,
        )

        // Verify
        assertNull(actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `build should return null if autofill possible but flags indicate compat mode and is login`() {
        // Setup
        every { settingsRepository.isAutofillSavePromptDisabled } returns false
        every { fillRequest.flags } returns FillRequest.FLAG_COMPATIBILITY_MODE_REQUEST

        // Test
        val actual = saveInfoBuilder.build(
            autofillPartition = autofillPartitionLogin,
            fillRequest = fillRequest,
            packageName = PACKAGE_NAME,
        )

        // Verify
        assertNull(actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `build should return SaveInfo with flag set if autofill possible, flags indicate compat mode, and is card`() {
        // Setup
        every { settingsRepository.isAutofillSavePromptDisabled } returns false
        every { fillRequest.flags } returns FillRequest.FLAG_COMPATIBILITY_MODE_REQUEST
        mockBuilder<SaveInfo.Builder> {
            it.setOptionalIds(arrayOf(autofillIdOptional))
        }
        mockBuilder<SaveInfo.Builder> {
            it.setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
        }

        // Test
        val actual = saveInfoBuilder.build(
            autofillPartition = autofillPartitionCard,
            fillRequest = fillRequest,
            packageName = PACKAGE_NAME,
        )

        // Verify
        assertEquals(saveInfo, actual)
        verify(exactly = 1) {
            anyConstructed<SaveInfo.Builder>().setOptionalIds(arrayOf(autofillIdOptional))
            anyConstructed<SaveInfo.Builder>().setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
        }
    }
}

private const val PACKAGE_NAME: String = "com.google"
private val AUTOFILL_PARTITION_LOGIN_EMPTY: AutofillPartition.Login = AutofillPartition.Login(
    views = listOf(),
)
