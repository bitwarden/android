package com.x8bit.bitwarden.authenticator.data.authenticator.manager

import com.bitwarden.core.DateTime
import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.sdk.AuthenticatorSdkSource
import com.x8bit.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.x8bit.bitwarden.authenticator.data.platform.manager.DispatcherManager
import com.x8bit.bitwarden.authenticator.data.platform.repository.model.DataState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import java.time.Clock
import javax.inject.Inject

private const val ONE_SECOND_MILLISECOND = 1000L

/**
 * Primary implementation of [TotpCodeManager].
 */
class TotpCodeManagerImpl @Inject constructor(
    private val authenticatorSdkSource: AuthenticatorSdkSource,
    dispatcherManager: DispatcherManager,
    private val clock: Clock,
) : TotpCodeManager {
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    private val mutableVerificationCodeStateFlowMap =
        mutableMapOf<AuthenticatorItemEntity, StateFlow<DataState<VerificationCodeItem?>>>()

    override fun getTotpCodesStateFlow(
        itemList: List<AuthenticatorItemEntity>,
    ): StateFlow<DataState<List<VerificationCodeItem>>> {
        // Generate state flows
        val stateFlows = itemList.map { itemEntity ->
            getTotpCodeStateFlowInternal(itemEntity)
        }
        return combine(stateFlows) { results ->
            when {
                results.any { it is DataState.Loading } -> {
                    DataState.Loading
                }

                else -> {
                    DataState.Loaded(
                        data = results.mapNotNull { (it as DataState.Loaded).data },
                    )
                }
            }
        }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = DataState.Loading,
            )
    }

    override fun getTotpCodeStateFlow(
        item: AuthenticatorItemEntity,
    ): StateFlow<DataState<VerificationCodeItem?>> =
        getTotpCodeStateFlowInternal(itemEntity = item)

    @Suppress("LongMethod")
    private fun getTotpCodeStateFlowInternal(
        itemEntity: AuthenticatorItemEntity?,
    ): StateFlow<DataState<VerificationCodeItem?>> {
        val cipherId = itemEntity?.id ?: return MutableStateFlow(DataState.Loaded(null))

        return mutableVerificationCodeStateFlowMap.getOrPut(itemEntity) {
            flow<DataState<VerificationCodeItem?>> {
                val totpCode = itemEntity.key

                var item: VerificationCodeItem? = null
                while (currentCoroutineContext().isActive) {
                    val time = (clock.millis() / ONE_SECOND_MILLISECOND).toInt()
                    val dateTime = DateTime.now()
                    if (item == null || item.isExpired(clock = clock)) {
                        authenticatorSdkSource
                            .generateTotp(
                                totp = totpCode,
                                time = dateTime,
                            )
                            .onSuccess { response ->
                                item = VerificationCodeItem(
                                    code = response.code,
                                    totpCode = totpCode,
                                    periodSeconds = response.period.toInt(),
                                    timeLeftSeconds = response.period.toInt() -
                                        time % response.period.toInt(),
                                    issueTime = clock.millis(),
                                    id = cipherId,
                                    username = itemEntity.username,
                                    issuer = itemEntity.issuer,
                                )
                            }
                            .onFailure {
                                emit(DataState.Loaded(null))
                                return@flow
                            }
                    } else {
                        item?.let {
                            item = it.copy(
                                timeLeftSeconds = it.periodSeconds -
                                    (time % it.periodSeconds),
                            )
                        }
                    }

                    item?.let {
                        emit(DataState.Loaded(it))
                    }
                    delay(ONE_SECOND_MILLISECOND)
                }
            }
                .onCompletion {
                    mutableVerificationCodeStateFlowMap.remove(itemEntity)
                }
                .stateIn(
                    scope = unconfinedScope,
                    started = SharingStarted.WhileSubscribed(),
                    initialValue = DataState.Loading,
                )
        }
    }
}

private fun VerificationCodeItem.isExpired(clock: Clock): Boolean {
    val timeExpired = issueTime + (timeLeftSeconds * ONE_SECOND_MILLISECOND)
    return timeExpired < clock.millis()
}
