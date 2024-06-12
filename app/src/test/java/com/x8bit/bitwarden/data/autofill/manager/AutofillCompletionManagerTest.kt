package com.x8bit.bitwarden.data.autofill.manager

import android.app.Activity
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.service.autofill.Dataset
import android.widget.Toast
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.autofill.builder.FilledDataBuilder
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest
import com.x8bit.bitwarden.data.autofill.model.FilledData
import com.x8bit.bitwarden.data.autofill.model.FilledPartition
import com.x8bit.bitwarden.data.autofill.parser.AutofillParser
import com.x8bit.bitwarden.data.autofill.util.buildDataset
import com.x8bit.bitwarden.data.autofill.util.createAutofillSelectionResultIntent
import com.x8bit.bitwarden.data.autofill.util.getAutofillAssistStructureOrNull
import com.x8bit.bitwarden.data.autofill.util.toAutofillAppInfo
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.GenerateTotpResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AutofillCompletionManagerTest {
    private val context: Context = mockk()
    private val activity: Activity = mockk {
        every { applicationContext } returns context
        every { finish() } just runs
        every { setResult(any()) } just runs
        every { setResult(any(), any()) } just runs
    }
    private val assistStructure: AssistStructure = mockk()
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(null)
    private val authRepository: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
    }
    private val autofillAppInfo: AutofillAppInfo = mockk()
    private val autofillParser: AutofillParser = mockk()
    private val cipherView: CipherView = mockk()
    private val clipboardManager: BitwardenClipboardManager = mockk {
        every { setText(any<String>()) } just runs
    }
    private val dataset: Dataset = mockk()
    private val dispatcherManager = FakeDispatcherManager()
    private val fillableRequest: AutofillRequest.Fillable = mockk()
    private val filledDataBuilder: FilledDataBuilder = mockk()
    private val filledPartition: FilledPartition = mockk()
    private val mockIntent: Intent = mockk()
    private val settingsRepository: SettingsRepository = mockk()
    private val resultIntent: Intent = mockk()
    private val toast: Toast = mockk {
        every { show() } just runs
    }
    private val vaultRepository: VaultRepository = mockk()

    private val autofillCompletionManager: AutofillCompletionManager =
        AutofillCompletionManagerImpl(
            authRepository = authRepository,
            autofillParser = autofillParser,
            clipboardManager = clipboardManager,
            dispatcherManager = dispatcherManager,
            filledDataBuilderProvider = { filledDataBuilder },
            settingsRepository = settingsRepository,
            vaultRepository = vaultRepository,
        )

    @BeforeEach
    fun setUp() {
        dispatcherManager.setMain(dispatcherManager.unconfined)
        mockkStatic(::createAutofillSelectionResultIntent)
        mockkStatic(Activity::toAutofillAppInfo)
        mockkStatic(FilledPartition::buildDataset)
        mockkStatic(Intent::getAutofillAssistStructureOrNull)
        mockkStatic(Toast::class)
        every { activity.toAutofillAppInfo() } returns autofillAppInfo
    }

    @AfterEach
    fun tearDown() {
        dispatcherManager.resetMain()
        unmockkStatic(::createAutofillSelectionResultIntent)
        unmockkStatic(Activity::toAutofillAppInfo)
        unmockkStatic(FilledPartition::buildDataset)
        unmockkStatic(Intent::getAutofillAssistStructureOrNull)
        unmockkStatic(Toast::class)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeAutofill when there is no Intent present should cancel and finish the Activity`() {
        every { activity.intent } returns null

        autofillCompletionManager.completeAutofill(
            activity = activity,
            cipherView = cipherView,
        )

        verify {
            activity.setResult(Activity.RESULT_CANCELED)
            activity.finish()
        }
        verify {
            activity.intent
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeAutofill when there is no AssistStructure present should cancel and finish the Activity`() {
        every { activity.intent } returns mockIntent
        every { mockIntent.getAutofillAssistStructureOrNull() } returns null

        autofillCompletionManager.completeAutofill(
            activity = activity,
            cipherView = cipherView,
        )

        verify {
            activity.setResult(Activity.RESULT_CANCELED)
            activity.finish()
        }
        verify {
            activity.intent
            mockIntent.getAutofillAssistStructureOrNull()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeAutofill when the request is not fillable should cancel and finish the Activity`() {
        every { activity.intent } returns mockIntent
        every { mockIntent.getAutofillAssistStructureOrNull() } returns assistStructure
        every {
            autofillParser.parse(
                autofillAppInfo = autofillAppInfo,
                assistStructure = assistStructure,
            )
        } returns AutofillRequest.Unfillable

        autofillCompletionManager.completeAutofill(
            activity = activity,
            cipherView = cipherView,
        )

        verify {
            activity.setResult(Activity.RESULT_CANCELED)
            activity.finish()
        }
        verify {
            activity.intent
            mockIntent.getAutofillAssistStructureOrNull()
            autofillParser.parse(
                autofillAppInfo = autofillAppInfo,
                assistStructure = assistStructure,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeAutofill when there are no filled partitions should cancel and finish the Activity`() {
        val filledData: FilledData = mockk {
            every { filledPartitions } returns emptyList()
        }
        every { activity.intent } returns mockIntent
        every { mockIntent.getAutofillAssistStructureOrNull() } returns assistStructure
        every {
            autofillParser.parse(
                autofillAppInfo = autofillAppInfo,
                assistStructure = assistStructure,
            )
        } returns fillableRequest
        coEvery {
            filledDataBuilder.build(autofillRequest = fillableRequest)
        } returns filledData

        autofillCompletionManager.completeAutofill(
            activity = activity,
            cipherView = cipherView,
        )

        verify {
            activity.setResult(Activity.RESULT_CANCELED)
            activity.finish()
        }
        verify {
            activity.intent
            mockIntent.getAutofillAssistStructureOrNull()
            autofillParser.parse(
                autofillAppInfo = autofillAppInfo,
                assistStructure = assistStructure,
            )
        }
        coVerify {
            filledDataBuilder.build(autofillRequest = fillableRequest)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeAutofill when filled partition, premium active user, a totp code, and totp generated succesfully should build a dataset, place it in a result Intent, copy totp, and finish the Activity`() {
        val filledData: FilledData = mockk {
            every { filledPartitions } returns listOf(filledPartition)
        }
        val generateTotpResult = GenerateTotpResult.Success(
            code = TOTP_RESULT_VALUE,
            periodSeconds = 100,
        )
        every { activity.intent } returns mockIntent
        every { mockIntent.getAutofillAssistStructureOrNull() } returns assistStructure
        every {
            autofillParser.parse(
                autofillAppInfo = autofillAppInfo,
                assistStructure = assistStructure,
            )
        } returns fillableRequest
        every { cipherView.login?.totp } returns TOTP_CODE
        coEvery {
            filledDataBuilder.build(autofillRequest = fillableRequest)
        } returns filledData
        every {
            filledPartition.buildDataset(
                authIntentSender = null,
                autofillAppInfo = autofillAppInfo,
            )
        } returns dataset
        every { settingsRepository.isAutoCopyTotpDisabled } returns false
        every { createAutofillSelectionResultIntent(dataset = dataset) } returns resultIntent
        coEvery {
            vaultRepository.generateTotp(
                time = any(),
                totpCode = TOTP_CODE,
            )
        } returns generateTotpResult
        every {
            Toast.makeText(
                context,
                R.string.verification_code_totp,
                Toast.LENGTH_LONG,
            )
        } returns toast
        mutableUserStateFlow.value = mockk {
            every { activeAccount.isPremium } returns true
        }

        autofillCompletionManager.completeAutofill(
            activity = activity,
            cipherView = cipherView,
        )

        verify {
            activity.setResult(Activity.RESULT_OK, resultIntent)
            activity.finish()
        }
        verify {
            activity.intent
            clipboardManager.setText(any<String>())
            mockIntent.getAutofillAssistStructureOrNull()
            autofillParser.parse(
                autofillAppInfo = autofillAppInfo,
                assistStructure = assistStructure,
            )
            filledPartition.buildDataset(
                authIntentSender = null,
                autofillAppInfo = autofillAppInfo,
            )
            settingsRepository.isAutoCopyTotpDisabled
            createAutofillSelectionResultIntent(dataset = dataset)
            Toast.makeText(
                context,
                R.string.verification_code_totp,
                Toast.LENGTH_LONG,
            )
            toast.show()
        }
        coVerify {
            filledDataBuilder.build(autofillRequest = fillableRequest)
            vaultRepository.generateTotp(
                time = any(),
                totpCode = TOTP_CODE,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeAutofill when filled partition, premium active user, a totp code, and totp generated unsuccessfully should build a dataset, place it in a result Intent, and finish the Activity`() {
        val filledData: FilledData = mockk {
            every { filledPartitions } returns listOf(filledPartition)
        }
        every { activity.intent } returns mockIntent
        every { mockIntent.getAutofillAssistStructureOrNull() } returns assistStructure
        every {
            autofillParser.parse(
                autofillAppInfo = autofillAppInfo,
                assistStructure = assistStructure,
            )
        } returns fillableRequest
        every { cipherView.login?.totp } returns TOTP_CODE
        coEvery {
            filledDataBuilder.build(autofillRequest = fillableRequest)
        } returns filledData
        every {
            filledPartition.buildDataset(
                authIntentSender = null,
                autofillAppInfo = autofillAppInfo,
            )
        } returns dataset
        every { settingsRepository.isAutoCopyTotpDisabled } returns false
        every { createAutofillSelectionResultIntent(dataset = dataset) } returns resultIntent
        coEvery {
            vaultRepository.generateTotp(
                time = any(),
                totpCode = TOTP_CODE,
            )
        } returns GenerateTotpResult.Error
        mutableUserStateFlow.value = mockk {
            every { activeAccount.isPremium } returns true
        }

        autofillCompletionManager.completeAutofill(
            activity = activity,
            cipherView = cipherView,
        )

        verify {
            activity.setResult(Activity.RESULT_OK, resultIntent)
            activity.finish()
        }
        verify {
            activity.intent
            mockIntent.getAutofillAssistStructureOrNull()
            autofillParser.parse(
                autofillAppInfo = autofillAppInfo,
                assistStructure = assistStructure,
            )
            filledPartition.buildDataset(
                authIntentSender = null,
                autofillAppInfo = autofillAppInfo,
            )
            settingsRepository.isAutoCopyTotpDisabled
            createAutofillSelectionResultIntent(dataset = dataset)
        }
        coVerify {
            filledDataBuilder.build(autofillRequest = fillableRequest)
            vaultRepository.generateTotp(
                time = any(),
                totpCode = TOTP_CODE,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeAutofill when filled partition, premium active user, and no totp code should build a dataset, place it in a result Intent, and finish the Activity`() {
        val filledData: FilledData = mockk {
            every { filledPartitions } returns listOf(filledPartition)
        }
        every { activity.intent } returns mockIntent
        every { mockIntent.getAutofillAssistStructureOrNull() } returns assistStructure
        every {
            autofillParser.parse(
                autofillAppInfo = autofillAppInfo,
                assistStructure = assistStructure,
            )
        } returns fillableRequest
        every { cipherView.login?.totp } returns null
        coEvery {
            filledDataBuilder.build(autofillRequest = fillableRequest)
        } returns filledData
        every {
            filledPartition.buildDataset(
                authIntentSender = null,
                autofillAppInfo = autofillAppInfo,
            )
        } returns dataset
        every { settingsRepository.isAutoCopyTotpDisabled } returns false
        every { createAutofillSelectionResultIntent(dataset = dataset) } returns resultIntent
        mutableUserStateFlow.value = mockk {
            every { activeAccount.isPremium } returns true
        }

        autofillCompletionManager.completeAutofill(
            activity = activity,
            cipherView = cipherView,
        )

        verify {
            activity.setResult(Activity.RESULT_OK, resultIntent)
            activity.finish()
        }
        verify {
            settingsRepository.isAutoCopyTotpDisabled
            activity.intent
            mockIntent.getAutofillAssistStructureOrNull()
            autofillParser.parse(
                autofillAppInfo = autofillAppInfo,
                assistStructure = assistStructure,
            )
            filledPartition.buildDataset(
                authIntentSender = null,
                autofillAppInfo = autofillAppInfo,
            )
            settingsRepository.isAutoCopyTotpDisabled
            createAutofillSelectionResultIntent(dataset = dataset)
        }
        coVerify {
            filledDataBuilder.build(autofillRequest = fillableRequest)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeAutofill when filled partition, no premium active user, and totp code should build a dataset, place it in a result Intent, and finish the Activity`() {
        val filledData: FilledData = mockk {
            every { filledPartitions } returns listOf(filledPartition)
        }
        every { activity.intent } returns mockIntent
        every { mockIntent.getAutofillAssistStructureOrNull() } returns assistStructure
        every {
            autofillParser.parse(
                autofillAppInfo = autofillAppInfo,
                assistStructure = assistStructure,
            )
        } returns fillableRequest
        every { cipherView.login?.totp } returns TOTP_CODE
        coEvery {
            filledDataBuilder.build(autofillRequest = fillableRequest)
        } returns filledData
        every {
            filledPartition.buildDataset(
                authIntentSender = null,
                autofillAppInfo = autofillAppInfo,
            )
        } returns dataset
        every { settingsRepository.isAutoCopyTotpDisabled } returns false
        every { createAutofillSelectionResultIntent(dataset = dataset) } returns resultIntent
        mutableUserStateFlow.value = mockk {
            every { activeAccount.isPremium } returns false
        }

        autofillCompletionManager.completeAutofill(
            activity = activity,
            cipherView = cipherView,
        )

        verify {
            activity.setResult(Activity.RESULT_OK, resultIntent)
            activity.finish()
        }
        verify {
            activity.intent
            mockIntent.getAutofillAssistStructureOrNull()
            autofillParser.parse(
                autofillAppInfo = autofillAppInfo,
                assistStructure = assistStructure,
            )
            filledPartition.buildDataset(
                authIntentSender = null,
                autofillAppInfo = autofillAppInfo,
            )
            settingsRepository.isAutoCopyTotpDisabled
            createAutofillSelectionResultIntent(dataset = dataset)
        }
        coVerify {
            filledDataBuilder.build(autofillRequest = fillableRequest)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `completeAutofill when filled partition and totp copy disabled should build a dataset, place it in a result Intent, and finish the Activity`() {
        val filledData: FilledData = mockk {
            every { filledPartitions } returns listOf(filledPartition)
        }
        every { activity.intent } returns mockIntent
        every { mockIntent.getAutofillAssistStructureOrNull() } returns assistStructure
        every {
            autofillParser.parse(
                autofillAppInfo = autofillAppInfo,
                assistStructure = assistStructure,
            )
        } returns fillableRequest
        every { cipherView.login?.totp } returns TOTP_CODE
        coEvery {
            filledDataBuilder.build(autofillRequest = fillableRequest)
        } returns filledData
        every {
            filledPartition.buildDataset(
                authIntentSender = null,
                autofillAppInfo = autofillAppInfo,
            )
        } returns dataset
        every { settingsRepository.isAutoCopyTotpDisabled } returns true
        every { createAutofillSelectionResultIntent(dataset = dataset) } returns resultIntent
        mutableUserStateFlow.value = mockk {
            every { activeAccount.isPremium } returns true
        }

        autofillCompletionManager.completeAutofill(
            activity = activity,
            cipherView = cipherView,
        )

        verify {
            activity.setResult(Activity.RESULT_OK, resultIntent)
            activity.finish()
        }
        verify {
            activity.intent
            mockIntent.getAutofillAssistStructureOrNull()
            autofillParser.parse(
                autofillAppInfo = autofillAppInfo,
                assistStructure = assistStructure,
            )
            filledPartition.buildDataset(
                authIntentSender = null,
                autofillAppInfo = autofillAppInfo,
            )
            settingsRepository.isAutoCopyTotpDisabled
            createAutofillSelectionResultIntent(dataset = dataset)
        }
        coVerify {
            filledDataBuilder.build(autofillRequest = fillableRequest)
        }
    }
}

private const val TOTP_CODE: String = "TOTP_CODE"
private const val TOTP_RESULT_VALUE: String = "TOTP_RESULT_VALUE"
