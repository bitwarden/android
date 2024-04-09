package com.x8bit.bitwarden.authenticator.data.authenticator.repository

import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.AuthenticatorDiskSource
import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.x8bit.bitwarden.authenticator.data.authenticator.manager.TotpCodeManager
import com.x8bit.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.x8bit.bitwarden.authenticator.data.authenticator.repository.model.AuthenticatorData
import com.x8bit.bitwarden.authenticator.data.authenticator.repository.model.CreateItemResult
import com.x8bit.bitwarden.authenticator.data.authenticator.repository.model.DeleteItemResult
import com.x8bit.bitwarden.authenticator.data.authenticator.repository.model.TotpCodeResult
import com.x8bit.bitwarden.authenticator.data.authenticator.repository.model.UpdateItemRequest
import com.x8bit.bitwarden.authenticator.data.authenticator.repository.model.UpdateItemResult
import com.x8bit.bitwarden.authenticator.data.platform.manager.DispatcherManager
import com.x8bit.bitwarden.authenticator.data.platform.repository.model.DataState
import com.x8bit.bitwarden.authenticator.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.authenticator.data.platform.repository.util.combineDataStates
import com.x8bit.bitwarden.authenticator.data.platform.repository.util.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * A "stop timeout delay" in milliseconds used to let a shared coroutine continue to run for the
 * specified period of time after it no longer has subscribers.
 */
private const val STOP_TIMEOUT_DELAY_MS: Long = 5_000L

class AuthenticatorRepositoryImpl @Inject constructor(
    private val authenticatorDiskSource: AuthenticatorDiskSource,
    private val totpCodeManager: TotpCodeManager,
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
                ),
            )
            UpdateItemResult.Success
        } catch (e: Exception) {
            UpdateItemResult.Error(e.message)
        }
    }
}
