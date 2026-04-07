package com.x8bit.bitwarden.data.autofill.builder

import android.content.Context
import android.content.IntentSender
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.service.autofill.SaveInfo
import android.view.View
import android.view.autofill.AutofillId
import com.bitwarden.core.data.util.mockBuilder
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.autofill.model.FilledData
import com.x8bit.bitwarden.data.autofill.model.FilledPartition
import com.x8bit.bitwarden.data.autofill.util.buildDataset
import com.x8bit.bitwarden.data.autofill.util.buildVaultItemDataset
import com.x8bit.bitwarden.data.autofill.util.createAutofillCallbackIntentSender
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FillResponseBuilderTest {
    private lateinit var fillResponseBuilder: FillResponseBuilder

    private val context: Context = mockk()
    private val dataset: Dataset = mockk()
    private val intentSender: IntentSender = mockk()
    private val vaultItemDataSet: Dataset = mockk()
    private val fillResponse: FillResponse = mockk()
    private val appInfo: AutofillAppInfo = AutofillAppInfo(
        context = context,
        packageName = PACKAGE_NAME,
        sdkInt = 17,
    )
    private val autofillCipherValid: AutofillCipher = mockk {
        every { cipherId } returns CIPHER_ID
    }
    private val autofillCipherNoId: AutofillCipher = mockk {
        every { cipherId } returns null
    }
    private val filledPartitionOne: FilledPartition = mockk {
        every { this@mockk.filledItems } returns listOf(mockk())
        every { this@mockk.autofillCipher } returns autofillCipherValid
    }
    private val filledPartitionTwo: FilledPartition = mockk {
        every { this@mockk.filledItems } returns emptyList()
    }
    private val filledPartitionThree: FilledPartition = mockk {
        every { this@mockk.filledItems } returns listOf(mockk())
        every { this@mockk.autofillCipher } returns autofillCipherNoId
    }
    private val saveInfo: SaveInfo = mockk()

    @BeforeEach
    fun setup() {
        mockkConstructor(FillResponse.Builder::class)
        mockkStatic(::createAutofillCallbackIntentSender)
        mockkStatic(FilledData::buildVaultItemDataset)
        mockkStatic(FilledPartition::buildDataset)
        every { anyConstructed<FillResponse.Builder>().build() } returns fillResponse
        every {
            createAutofillCallbackIntentSender(
                cipherId = CIPHER_ID,
                context = context,
            )
        } returns intentSender

        fillResponseBuilder = FillResponseBuilderImpl()
    }

    @AfterEach
    fun teardown() {
        unmockkConstructor(FillResponse.Builder::class)
        unmockkStatic(::createAutofillCallbackIntentSender)
        unmockkStatic(FilledData::buildVaultItemDataset)
        unmockkStatic(FilledPartition::buildDataset)
    }

    @Test
    fun `build should return null when original partition contains no views`() {
        // Test
        val filledPartitions = FilledPartition(
            autofillCipher = mockk(),
            filledItems = emptyList(),
            inlinePresentationSpec = null,
        )
        val filledData = FilledData(
            filledPartitions = listOf(
                filledPartitions,
            ),
            ignoreAutofillIds = emptyList(),
            originalPartition = AutofillPartition.Login(
                views = emptyList(),
            ),
            uri = null,
            vaultItemInlinePresentationSpec = null,
            isVaultLocked = false,
        )
        val actual = fillResponseBuilder.build(
            autofillAppInfo = appInfo,
            filledData = filledData,
            saveInfo = saveInfo,
        )

        // Verify
        assertNull(actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `build should apply FilledPartitions with filledItems, ignore ignoreAutofillIds, and set valid SaveInfo`() {
        // Setup
        val ignoredAutofillIdOne: AutofillId = mockk()
        val ignoredAutofillIdTwo: AutofillId = mockk()
        val ignoreAutofillIds = listOf(
            ignoredAutofillIdOne,
            ignoredAutofillIdTwo,
        )
        val filledPartitions = listOf(
            filledPartitionOne,
            filledPartitionTwo,
            filledPartitionThree,
        )
        val filledData = FilledData(
            filledPartitions = filledPartitions,
            ignoreAutofillIds = ignoreAutofillIds,
            originalPartition = AutofillPartition.Login(
                views = listOf(
                    AutofillView.Login.Username(
                        data = AutofillView.Data(
                            autofillId = mockk(),
                            autofillOptions = emptyList(),
                            autofillType = AUTOFILL_TYPE,
                            isFocused = true,
                            textValue = null,
                            hasPasswordTerms = false,
                            website = null,
                        ),
                    ),
                ),
            ),
            uri = null,
            vaultItemInlinePresentationSpec = null,
            isVaultLocked = false,
        )
        every {
            filledPartitionOne.buildDataset(
                authIntentSender = intentSender,
                autofillAppInfo = appInfo,
            )
        } returns dataset
        every {
            filledPartitionThree.buildDataset(
                authIntentSender = null,
                autofillAppInfo = appInfo,
            )
        } returns dataset
        every {
            filledData.buildVaultItemDataset(
                autofillAppInfo = appInfo,
            )
        } returns vaultItemDataSet
        mockBuilder<FillResponse.Builder> {
            it.addDataset(dataset)
            it.addDataset(vaultItemDataSet)
        }
        mockBuilder<FillResponse.Builder> {
            it.setIgnoredIds(
                ignoredAutofillIdOne,
                ignoredAutofillIdTwo,
            )
        }
        mockBuilder<FillResponse.Builder> {
            it.setSaveInfo(saveInfo)
        }

        // Test
        val actual = fillResponseBuilder.build(
            autofillAppInfo = appInfo,
            filledData = filledData,
            saveInfo = saveInfo,
        )

        // Verify
        assertEquals(fillResponse, actual)

        verify(exactly = 1) {
            filledPartitionOne.buildDataset(
                authIntentSender = intentSender,
                autofillAppInfo = appInfo,
            )
            filledPartitionThree.buildDataset(
                authIntentSender = null,
                autofillAppInfo = appInfo,
            )
            filledData.buildVaultItemDataset(
                autofillAppInfo = appInfo,
            )
            anyConstructed<FillResponse.Builder>().addDataset(vaultItemDataSet)
            anyConstructed<FillResponse.Builder>().setIgnoredIds(
                ignoredAutofillIdOne,
                ignoredAutofillIdTwo,
            )
            anyConstructed<FillResponse.Builder>().setSaveInfo(saveInfo)
        }
        verify(exactly = 2) {
            anyConstructed<FillResponse.Builder>().addDataset(dataset)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `build should apply FilledPartitions with filledItems, ignore ignoreAutofillIds, and skip valid SaveInfo`() {
        // Setup
        val ignoredAutofillIdOne: AutofillId = mockk()
        val ignoredAutofillIdTwo: AutofillId = mockk()
        val ignoreAutofillIds = listOf(
            ignoredAutofillIdOne,
            ignoredAutofillIdTwo,
        )
        val filledPartitions = listOf(
            filledPartitionOne,
            filledPartitionTwo,
            filledPartitionThree,
        )
        val filledData = FilledData(
            filledPartitions = filledPartitions,
            ignoreAutofillIds = ignoreAutofillIds,
            originalPartition = AutofillPartition.Login(
                views = listOf(
                    AutofillView.Login.Username(
                        data = AutofillView.Data(
                            autofillId = mockk(),
                            autofillOptions = emptyList(),
                            autofillType = AUTOFILL_TYPE,
                            isFocused = true,
                            textValue = null,
                            hasPasswordTerms = false,
                            website = null,
                        ),
                    ),
                ),
            ),
            uri = null,
            vaultItemInlinePresentationSpec = null,
            isVaultLocked = false,
        )
        every {
            filledPartitionOne.buildDataset(
                authIntentSender = intentSender,
                autofillAppInfo = appInfo,
            )
        } returns dataset
        every {
            filledPartitionThree.buildDataset(
                authIntentSender = null,
                autofillAppInfo = appInfo,
            )
        } returns dataset
        every {
            filledData.buildVaultItemDataset(
                autofillAppInfo = appInfo,
            )
        } returns vaultItemDataSet
        mockBuilder<FillResponse.Builder> {
            it.addDataset(dataset)
            it.addDataset(vaultItemDataSet)
        }
        mockBuilder<FillResponse.Builder> {
            it.setIgnoredIds(
                ignoredAutofillIdOne,
                ignoredAutofillIdTwo,
            )
        }

        // Test
        val actual = fillResponseBuilder.build(
            autofillAppInfo = appInfo,
            filledData = filledData,
            saveInfo = null,
        )

        // Verify
        assertEquals(fillResponse, actual)

        verify(exactly = 1) {
            filledPartitionOne.buildDataset(
                authIntentSender = intentSender,
                autofillAppInfo = appInfo,
            )
            filledPartitionThree.buildDataset(
                authIntentSender = null,
                autofillAppInfo = appInfo,
            )
            filledData.buildVaultItemDataset(
                autofillAppInfo = appInfo,
            )
            anyConstructed<FillResponse.Builder>().addDataset(vaultItemDataSet)
            anyConstructed<FillResponse.Builder>().setIgnoredIds(
                ignoredAutofillIdOne,
                ignoredAutofillIdTwo,
            )
        }
        verify(exactly = 2) {
            anyConstructed<FillResponse.Builder>().addDataset(dataset)
        }
    }

    companion object {
        private const val AUTOFILL_TYPE: Int = View.AUTOFILL_TYPE_TEXT
        private const val CIPHER_ID: String = "1234567890"
        private const val PACKAGE_NAME: String = "com.x8bit.bitwarden"
    }
}
