package com.x8bit.bitwarden.data.autofill.processor

import android.app.assist.AssistStructure
import android.content.IntentSender
import android.os.CancellationSignal
import android.service.autofill.FillCallback
import android.service.autofill.FillContext
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import com.x8bit.bitwarden.data.autofill.builder.FillResponseBuilder
import com.x8bit.bitwarden.data.autofill.builder.FilledDataBuilder
import com.x8bit.bitwarden.data.autofill.builder.SaveInfoBuilder
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.FilledData
import com.x8bit.bitwarden.data.autofill.parser.AutofillParser
import com.x8bit.bitwarden.data.autofill.util.createAutofillSavedItemIntentSender
import com.x8bit.bitwarden.data.autofill.util.toAutofillSaveItem
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutofillProcessorTest {
    private lateinit var autofillProcessor: AutofillProcessor

    private val dispatcherManager: DispatcherManager = mockk()
    private val cancellationSignal: CancellationSignal = mockk()
    private val filledDataBuilder: FilledDataBuilder = mockk()
    private val fillResponseBuilder: FillResponseBuilder = mockk()
    private val parser: AutofillParser = mockk()
    private val policyManager: PolicyManager = mockk()
    private val saveInfoBuilder: SaveInfoBuilder = mockk()
    private val settingsRepository: SettingsRepository = mockk()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val appInfo: AutofillAppInfo = AutofillAppInfo(
        context = mockk(),
        packageName = "com.x8bit.bitwarden",
        sdkInt = 42,
    )
    private val fillCallback: FillCallback = mockk()

    @BeforeEach
    fun setup() {
        mockkStatic(::createAutofillSavedItemIntentSender)
        mockkStatic(AutofillRequest.Fillable::toAutofillSaveItem)
        every { dispatcherManager.unconfined } returns testDispatcher

        autofillProcessor = AutofillProcessorImpl(
            dispatcherManager = dispatcherManager,
            filledDataBuilder = filledDataBuilder,
            fillResponseBuilder = fillResponseBuilder,
            parser = parser,
            policyManager = policyManager,
            saveInfoBuilder = saveInfoBuilder,
            settingsRepository = settingsRepository,
        )
    }

    @AfterEach
    fun teardown() {
        verify(exactly = 1) {
            dispatcherManager.unconfined
        }
        unmockkStatic(::createAutofillSavedItemIntentSender)
        unmockkStatic(AutofillRequest.Fillable::toAutofillSaveItem)
    }

    @Test
    fun `processFillRequest should invoke callback with null when parse returns Unfillable`() {
        // Setup
        val autofillRequest = AutofillRequest.Unfillable
        val fillRequest: FillRequest = mockk()
        every { cancellationSignal.setOnCancelListener(any()) } just runs
        every {
            parser.parse(
                autofillAppInfo = appInfo,
                fillRequest = fillRequest,
            )
        } returns autofillRequest
        every { fillCallback.onSuccess(null) } just runs

        // Test
        autofillProcessor.processFillRequest(
            autofillAppInfo = appInfo,
            cancellationSignal = cancellationSignal,
            fillCallback = fillCallback,
            request = fillRequest,
        )

        // Verify
        verify(exactly = 1) {
            cancellationSignal.setOnCancelListener(any())
            parser.parse(
                autofillAppInfo = appInfo,
                fillRequest = fillRequest,
            )
            fillCallback.onSuccess(null)
        }
    }

    @Test
    fun `processFillRequest should invoke callback with filled response when has filledItems`() =
        runTest {
            // Setup
            val fillRequest: FillRequest = mockk()
            val filledData = FilledData(
                filledPartitions = listOf(mockk()),
                ignoreAutofillIds = emptyList(),
                originalPartition = mockk(),
                uri = null,
                vaultItemInlinePresentationSpec = null,
                isVaultLocked = false,
            )
            val fillResponse: FillResponse = mockk()
            val autofillPartition: AutofillPartition = mockk()
            val autofillRequest: AutofillRequest.Fillable = mockk {
                every { packageName } returns PACKAGE_NAME
                every { partition } returns autofillPartition
            }
            val saveInfo: SaveInfo = mockk()
            coEvery {
                filledDataBuilder.build(
                    autofillRequest = autofillRequest,
                )
            } returns filledData
            every { cancellationSignal.setOnCancelListener(any()) } just runs
            every {
                parser.parse(
                    autofillAppInfo = appInfo,
                    fillRequest = fillRequest,
                )
            } returns autofillRequest
            every {
                saveInfoBuilder.build(
                    autofillAppInfo = appInfo,
                    autofillPartition = autofillPartition,
                    fillRequest = fillRequest,
                    packageName = PACKAGE_NAME,
                )
            } returns saveInfo
            every {
                fillResponseBuilder.build(
                    autofillAppInfo = appInfo,
                    filledData = filledData,
                    saveInfo = saveInfo,
                )
            } returns fillResponse
            every { fillCallback.onSuccess(fillResponse) } just runs

            // Test
            autofillProcessor.processFillRequest(
                autofillAppInfo = appInfo,
                cancellationSignal = cancellationSignal,
                fillCallback = fillCallback,
                request = fillRequest,
            )

            // Verify
            coVerify(exactly = 1) {
                filledDataBuilder.build(
                    autofillRequest = autofillRequest,
                )
            }
            verify(exactly = 1) {
                cancellationSignal.setOnCancelListener(any())
                parser.parse(
                    autofillAppInfo = appInfo,
                    fillRequest = fillRequest,
                )
                fillResponseBuilder.build(
                    autofillAppInfo = appInfo,
                    filledData = filledData,
                    saveInfo = saveInfo,
                )
                fillCallback.onSuccess(fillResponse)
            }
        }

    @Test
    fun `processSaveRequest should invoke empty callback when autofill prompt disabled`() {
        // Setup
        val saveCallback: SaveCallback = mockk {
            every { onSuccess() } just runs
        }
        val saveRequest: SaveRequest = mockk()
        every { settingsRepository.isAutofillSavePromptDisabled } returns true

        // Test
        autofillProcessor.processSaveRequest(
            autofillAppInfo = appInfo,
            request = saveRequest,
            saveCallback = saveCallback,
        )

        // Verify
        verify(exactly = 1) {
            settingsRepository.isAutofillSavePromptDisabled
            saveCallback.onSuccess()
        }
    }

    @Test
    fun `processSaveRequest should invoke empty callback when personal ownership applies`() {
        // Setup
        val saveCallback: SaveCallback = mockk {
            every { onSuccess() } just runs
        }
        val saveRequest: SaveRequest = mockk()
        val policies: List<SyncResponseJson.Policy> = listOf(mockk())
        every { settingsRepository.isAutofillSavePromptDisabled } returns false
        every {
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns policies

        // Test
        autofillProcessor.processSaveRequest(
            autofillAppInfo = appInfo,
            request = saveRequest,
            saveCallback = saveCallback,
        )

        // Verify
        verify(exactly = 1) {
            settingsRepository.isAutofillSavePromptDisabled
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
            saveCallback.onSuccess()
        }
    }

    @Test
    fun `processSaveRequest should invoke empty callback when no fill contexts`() {
        // Setup
        val saveCallback: SaveCallback = mockk {
            every { onSuccess() } just runs
        }
        val saveRequest: SaveRequest = mockk {
            every { fillContexts } returns emptyList()
        }
        every { settingsRepository.isAutofillSavePromptDisabled } returns false
        every {
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns emptyList()

        // Test
        autofillProcessor.processSaveRequest(
            autofillAppInfo = appInfo,
            request = saveRequest,
            saveCallback = saveCallback,
        )

        // Verify
        verify(exactly = 1) {
            settingsRepository.isAutofillSavePromptDisabled
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
            saveCallback.onSuccess()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `processSaveRequest should invoke intentSender callback when autofill enabled, has fill contexts, and parser returns Fillable`() {
        // Setup
        val intentSender: IntentSender = mockk()
        val saveCallback: SaveCallback = mockk {
            every { onSuccess(intentSender) } just runs
        }
        val assistStructure: AssistStructure = mockk()
        val fillContext: FillContext = mockk {
            every { structure } returns assistStructure
        }
        val saveRequest: SaveRequest = mockk {
            every { fillContexts } returns listOf(fillContext)
        }
        val autofillPartition: AutofillPartition = mockk()
        val autofillSaveItem: AutofillSaveItem = mockk()
        val autofillRequest: AutofillRequest.Fillable = mockk {
            every { packageName } returns PACKAGE_NAME
            every { partition } returns autofillPartition
            every { toAutofillSaveItem() } returns autofillSaveItem
        }
        every { settingsRepository.isAutofillSavePromptDisabled } returns false
        every {
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns emptyList()
        every {
            parser.parse(
                autofillAppInfo = appInfo,
                assistStructure = assistStructure,
            )
        } returns autofillRequest
        every {
            createAutofillSavedItemIntentSender(
                autofillAppInfo = appInfo,
                autofillSaveItem = autofillSaveItem,
            )
        } returns intentSender

        // Test
        autofillProcessor.processSaveRequest(
            autofillAppInfo = appInfo,
            request = saveRequest,
            saveCallback = saveCallback,
        )

        // Verify
        verify(exactly = 1) {
            settingsRepository.isAutofillSavePromptDisabled
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
            parser.parse(
                autofillAppInfo = appInfo,
                assistStructure = assistStructure,
            )
            createAutofillSavedItemIntentSender(
                autofillAppInfo = appInfo,
                autofillSaveItem = autofillSaveItem,
            )
            saveCallback.onSuccess(intentSender)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `processSaveRequest should invoke empty callback when autofill enabled, has fill contexts, and parser returns Unfillable`() {
        // Setup
        val saveCallback: SaveCallback = mockk {
            every { onSuccess() } just runs
        }
        val assistStructure: AssistStructure = mockk()
        val fillContext: FillContext = mockk {
            every { structure } returns assistStructure
        }
        val saveRequest: SaveRequest = mockk {
            every { fillContexts } returns listOf(fillContext)
        }
        val autofillRequest = AutofillRequest.Unfillable
        every { settingsRepository.isAutofillSavePromptDisabled } returns false
        every {
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
        } returns emptyList()
        every {
            parser.parse(
                autofillAppInfo = appInfo,
                assistStructure = assistStructure,
            )
        } returns autofillRequest

        // Test
        autofillProcessor.processSaveRequest(
            autofillAppInfo = appInfo,
            request = saveRequest,
            saveCallback = saveCallback,
        )

        // Verify
        verify(exactly = 1) {
            settingsRepository.isAutofillSavePromptDisabled
            policyManager.getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
            parser.parse(
                autofillAppInfo = appInfo,
                assistStructure = assistStructure,
            )
            saveCallback.onSuccess()
        }
    }
}

private const val PACKAGE_NAME: String = "com.google"
