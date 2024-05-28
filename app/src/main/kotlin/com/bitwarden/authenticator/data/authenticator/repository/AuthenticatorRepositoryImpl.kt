package com.bitwarden.authenticator.data.authenticator.repository

import android.net.Uri
import com.bitwarden.authenticator.data.authenticator.datasource.disk.AuthenticatorDiskSource
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.bitwarden.authenticator.data.authenticator.manager.FileManager
import com.bitwarden.authenticator.data.authenticator.manager.TotpCodeManager
import com.bitwarden.authenticator.data.authenticator.manager.model.ExportJsonData
import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.repository.model.AuthenticatorData
import com.bitwarden.authenticator.data.authenticator.repository.model.CreateItemResult
import com.bitwarden.authenticator.data.authenticator.repository.model.DeleteItemResult
import com.bitwarden.authenticator.data.authenticator.repository.model.ExportDataResult
import com.bitwarden.authenticator.data.authenticator.repository.model.TotpCodeResult
import com.bitwarden.authenticator.data.authenticator.repository.model.UpdateItemRequest
import com.bitwarden.authenticator.data.authenticator.repository.model.UpdateItemResult
import com.bitwarden.authenticator.data.platform.manager.DispatcherManager
import com.bitwarden.authenticator.data.platform.manager.imports.ImportManager
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportDataResult
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportFileFormat
import com.bitwarden.authenticator.data.platform.repository.model.DataState
import com.bitwarden.authenticator.data.platform.repository.util.bufferedMutableSharedFlow
import com.bitwarden.authenticator.data.platform.repository.util.combineDataStates
import com.bitwarden.authenticator.data.platform.repository.util.map
import com.bitwarden.authenticator.data.platform.util.asSuccess
import com.bitwarden.authenticator.data.platform.util.flatMap
import com.bitwarden.authenticator.ui.platform.feature.settings.export.model.ExportFormat
import com.bitwarden.authenticator.ui.platform.manager.intent.IntentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.ByteArrayInputStream
import javax.inject.Inject

/**
 * A "stop timeout delay" in milliseconds used to let a shared coroutine continue to run for the
 * specified period of time after it no longer has subscribers.
 */
private const val STOP_TIMEOUT_DELAY_MS: Long = 5_000L

class AuthenticatorRepositoryImpl @Inject constructor(
    private val authenticatorDiskSource: AuthenticatorDiskSource,
    private val totpCodeManager: TotpCodeManager,
    private val fileManager: FileManager,
    private val importManager: ImportManager,
    dispatcherManager: DispatcherManager,
) : AuthenticatorRepository {

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    private val mutableCiphersStateFlow =
        MutableStateFlow<DataState<List<AuthenticatorItemEntity>>>(DataState.Loading)

    private val mutableTotpCodeResultFlow =
        bufferedMutableSharedFlow<TotpCodeResult>()

    override val totpCodeFlow: Flow<TotpCodeResult>
        get() = mutableTotpCodeResultFlow.asSharedFlow()

    override val authenticatorDataFlow: StateFlow<DataState<AuthenticatorData>> =
        ciphersStateFlow.map { cipherDataState ->
            when (cipherDataState) {
                is DataState.Error -> {
                    DataState.Error(
                        cipherDataState.error,
                        AuthenticatorData(cipherDataState.data.orEmpty()),
                    )
                }

                is DataState.Loaded -> {
                    DataState.Loaded(AuthenticatorData(items = cipherDataState.data))
                }

                DataState.Loading -> {
                    DataState.Loading
                }

                is DataState.NoNetwork -> {
                    DataState.NoNetwork(AuthenticatorData(items = cipherDataState.data.orEmpty()))
                }

                is DataState.Pending -> {
                    DataState.Pending(AuthenticatorData(items = cipherDataState.data))
                }
            }
        }.stateIn(
            scope = unconfinedScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT_DELAY_MS),
            initialValue = DataState.Loading,
        )

    override val ciphersStateFlow: StateFlow<DataState<List<AuthenticatorItemEntity>>>
        get() = mutableCiphersStateFlow.asStateFlow()

    init {
        authenticatorDiskSource
            .getItems()
            .onStart {
                mutableCiphersStateFlow.value = DataState.Loading
            }
            .onEach {
                mutableCiphersStateFlow.value = DataState.Loaded(it)
            }
            .launchIn(unconfinedScope)
    }

    override fun getItemStateFlow(itemId: String): StateFlow<DataState<AuthenticatorItemEntity?>> =
        authenticatorDataFlow
            .map { dataState ->
                dataState.map { authenticatorData ->
                    authenticatorData
                        .items
                        .find { it.id == itemId }
                }
            }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_DELAY_MS),
                initialValue = DataState.Loading,
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAuthCodeFlow(cipherId: String): StateFlow<DataState<VerificationCodeItem?>> {
        return getItemStateFlow(cipherId)
            .flatMapLatest { cipherDataState ->
                val cipher = cipherDataState.data
                    ?: return@flatMapLatest flowOf(DataState.Loaded(null))

                totpCodeManager.getTotpCodeStateFlow(item = cipher)
                    .map { totpCodeDataState ->
                        combineDataStates(
                            totpCodeDataState,
                            cipherDataState,
                        ) { totpCodeData, _ ->
                            // Just return the verification items; we are only combining the
                            // DataStates to know the overall state.
                            totpCodeData
                        }
                    }
            }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_DELAY_MS),
                initialValue = DataState.Loading,
            )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAuthCodesFlow(): StateFlow<DataState<List<VerificationCodeItem>>> {
        return authenticatorDataFlow
            .map { dataState ->
                dataState.map { authenticatorData -> authenticatorData.items }
            }
            .flatMapLatest { cipherDataState ->
                val cipherList = cipherDataState.data ?: emptyList()
                totpCodeManager.getTotpCodesStateFlow(itemList = cipherList)
                    .map { verificationCodeDataStates ->
                        combineDataStates(
                            verificationCodeDataStates,
                            cipherDataState,
                        ) { verificationCodeItems, _ ->
                            // Just return the verification items; we are only combining the
                            // DataStates to know the overall state.
                            verificationCodeItems
                        }
                    }
            }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_DELAY_MS),
                initialValue = DataState.Loading,
            )
    }

    override fun emitTotpCodeResult(totpCodeResult: TotpCodeResult) {
        mutableTotpCodeResultFlow.tryEmit(totpCodeResult)
    }

    override suspend fun createItem(item: AuthenticatorItemEntity): CreateItemResult {
        return try {
            authenticatorDiskSource.saveItem(item)
            CreateItemResult.Success
        } catch (e: Exception) {
            CreateItemResult.Error
        }
    }

    override suspend fun addItems(vararg items: AuthenticatorItemEntity): CreateItemResult {
        return try {
            authenticatorDiskSource.saveItem(*items)
            CreateItemResult.Success
        } catch (e: Exception) {
            CreateItemResult.Error
        }
    }

    override suspend fun hardDeleteItem(itemId: String): DeleteItemResult {
        return try {
            authenticatorDiskSource.deleteItem(itemId)
            DeleteItemResult.Success
        } catch (e: Exception) {
            DeleteItemResult.Error
        }
    }

    override suspend fun updateItem(
        itemId: String,
        updateItemRequest: UpdateItemRequest,
    ): UpdateItemResult {
        return try {
            authenticatorDiskSource.saveItem(
                AuthenticatorItemEntity(
                    id = itemId,
                    key = updateItemRequest.key,
                    accountName = updateItemRequest.accountName,
                    type = updateItemRequest.type,
                    period = updateItemRequest.period,
                    digits = updateItemRequest.digits,
                    issuer = updateItemRequest.issuer,
                    userId = null,
                    favorite = updateItemRequest.favorite
                ),
            )
            UpdateItemResult.Success
        } catch (e: Exception) {
            UpdateItemResult.Error(e.message)
        }
    }

    override suspend fun exportVaultData(format: ExportFormat, fileUri: Uri): ExportDataResult {
        return when (format) {
            ExportFormat.JSON -> encodeVaultDataToJson(fileUri)
            ExportFormat.CSV -> encodeVaultDataToCsv(fileUri)
        }
    }

    override suspend fun importVaultData(
        format: ImportFileFormat,
        fileData: IntentManager.FileData,
    ): ImportDataResult = fileManager.uriToByteArray(fileData.uri)
        .map { importManager.import(importFileFormat = format, byteArray = it) }
        .fold(
            onSuccess = { ImportDataResult.Success },
            onFailure = { ImportDataResult.Error }
        )

    private suspend fun encodeVaultDataToCsv(fileUri: Uri): ExportDataResult {
        val headerLine =
            "folder,favorite,type,name,login_uri,login_totp"
        val dataLines = authenticatorDiskSource
            .getItems()
            .firstOrNull()
            .orEmpty()
            .joinToString("\n") { it.toCsvFormat() }

        val csvString = "$headerLine\n$dataLines"

        return if (fileManager.stringToUri(fileUri = fileUri, dataString = csvString)) {
            ExportDataResult.Success
        } else {
            ExportDataResult.Error
        }
    }

    private fun AuthenticatorItemEntity.toCsvFormat() =
        ",,1,$issuer,,${toOtpAuthUriString()},$issuer,$period,$digits"

    private suspend fun encodeVaultDataToJson(fileUri: Uri): ExportDataResult {
        val dataString: String = Json.encodeToString(
            ExportJsonData(
                encrypted = false,
                items = authenticatorDiskSource
                    .getItems()
                    .firstOrNull()
                    .orEmpty()
                    .map { it.toExportJsonItem() },
            )
        )

        return if (
            fileManager.stringToUri(
                fileUri = fileUri,
                dataString = dataString,
            )
        ) {
            ExportDataResult.Success
        } else {
            ExportDataResult.Error
        }
    }

    private fun AuthenticatorItemEntity.toExportJsonItem() = ExportJsonData.ExportItem(
        id = id,
        folderId = null,
        organizationId = null,
        collectionIds = null,
        name = issuer,
        notes = null,
        type = 1,
        login = ExportJsonData.ExportItem.ItemLoginData(
            totp = toOtpAuthUriString(),
        ),
        favorite = false,
    )

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun decodeVaultDataFromJson(fileUri: IntentManager.FileData): ImportDataResult {
        val importJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
        return try {
            fileManager.uriToByteArray(fileUri.uri)
                .flatMap {
                    importJson
                        .decodeFromStream<ExportJsonData>(ByteArrayInputStream(it))
                        .asSuccess()
                }
                .map { exportData ->
                    exportData
                        .items
                        .toAuthenticatorItemEntities()
                }
                .fold(
                    onSuccess = {
                        authenticatorDiskSource.saveItem(*it.toTypedArray())
                        ImportDataResult.Success
                    },
                    onFailure = {
                        ImportDataResult.Error
                    },
                )
        } catch (e: IllegalArgumentException) {
            ImportDataResult.Error
        }
    }

    private fun List<ExportJsonData.ExportItem>.toAuthenticatorItemEntities() =
        map { it.toAuthenticatorItemEntity() }

    private fun ExportJsonData.ExportItem.toAuthenticatorItemEntity(): AuthenticatorItemEntity {
        val otpString = login.totp
        val otpUri = Uri.parse(otpString)
        val type = if (otpString.startsWith(TotpCodeManager.TOTP_CODE_PREFIX)) {
            AuthenticatorItemType.TOTP
        } else if (otpString.startsWith(TotpCodeManager.STEAM_CODE_PREFIX)) {
            AuthenticatorItemType.STEAM
        } else {
            throw IllegalArgumentException("Unsupported OTP type.")
        }

        val key = when (type) {
            AuthenticatorItemType.TOTP -> {
                requireNotNull(otpUri.getQueryParameter(TotpCodeManager.SECRET_PARAM))
            }

            AuthenticatorItemType.STEAM -> {
                requireNotNull(otpUri.authority)
            }
        }

        val algorithm = otpUri.getQueryParameter(TotpCodeManager.ALGORITHM_PARAM)
            ?: TotpCodeManager.ALGORITHM_DEFAULT.name

        val period = otpUri.getQueryParameter(TotpCodeManager.PERIOD_PARAM)
            ?.toIntOrNull()
            ?: TotpCodeManager.PERIOD_SECONDS_DEFAULT

        val digits = when (type) {
            AuthenticatorItemType.TOTP -> {
                otpUri.getQueryParameter(TotpCodeManager.DIGITS_PARAM)
                    ?.toIntOrNull()
                    ?: TotpCodeManager.TOTP_DIGITS_DEFAULT
            }

            AuthenticatorItemType.STEAM -> {
                TotpCodeManager.STEAM_DIGITS_DEFAULT
            }
        }
        val issuer = otpUri.getQueryParameter(TotpCodeManager.ISSUER_PARAM)
            ?: name

        val label = when (type) {
            AuthenticatorItemType.TOTP -> {
                otpUri.pathSegments
                    .firstOrNull()
                    .orEmpty()
                    .removePrefix("$issuer:")
            }

            AuthenticatorItemType.STEAM -> null
        }

        return AuthenticatorItemEntity(
            id = id,
            key = key,
            type = type,
            algorithm = algorithm.let { AuthenticatorItemAlgorithm.valueOf(it) },
            period = period,
            digits = digits,
            issuer = issuer,
            accountName = label,
            favorite = favorite,
        )
    }
}
