package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.core.DateTime
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.data.manager.DispatcherManager
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.autofill.util.login
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.manager.model.VerificationCodeItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
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

private const val ONE_SECOND_MILLISECOND = 1000L

/**
 * Primary implementation of [TotpCodeManager].
 */
class TotpCodeManagerImpl(
    private val vaultSdkSource: VaultSdkSource,
    private val dispatcherManager: DispatcherManager,
    private val clock: Clock,
) : TotpCodeManager {
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    private val mutableVerificationCodeStateFlowMap =
        mutableMapOf<CipherView, StateFlow<DataState<VerificationCodeItem?>>>()

    private val mutableCipherListViewVerificationCodeStateFlowMap =
        mutableMapOf<CipherListView, StateFlow<DataState<VerificationCodeItem?>>>()

    override fun getTotpCodesStateFlow(
        userId: String,
        cipherList: List<CipherView>,
    ): StateFlow<DataState<List<VerificationCodeItem>>> {
        // Generate state flows
        val stateFlows = cipherList.map { cipherView ->
            getTotpCodeStateFlowInternal(userId, cipherView)
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
        userId: String,
        cipher: CipherView,
    ): StateFlow<DataState<VerificationCodeItem?>> =
        getTotpCodeStateFlowInternal(
            userId = userId,
            cipher = cipher,
        )

    override fun getTotpCodesForCipherListViewsStateFlow(
        userId: String,
        cipherListViews: List<CipherListView>,
    ): StateFlow<DataState<List<VerificationCodeItem>>> {
        // Generate state flows
        val stateFlows = cipherListViews.map { cipherListView ->
            getTotpCodeStateFlowInternal(userId, cipherListView)
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
        userId: String,
        cipherListView: CipherListView,
    ): StateFlow<DataState<VerificationCodeItem?>> =
        getTotpCodeStateFlowInternal(
            userId = userId,
            cipherListView = cipherListView,
        )

    @Suppress("LongMethod")
    private fun getTotpCodeStateFlowInternal(
        userId: String,
        cipher: CipherView?,
    ): StateFlow<DataState<VerificationCodeItem?>> {
        val cipherId = cipher?.id ?: return MutableStateFlow(DataState.Loaded(null))

        return mutableVerificationCodeStateFlowMap.getOrPut(cipher) {
            // Define a per-item scope so that we can clear the Flow from the scope when it is
            // no longer needed.
            val itemScope = CoroutineScope(dispatcherManager.unconfined)

            flow<DataState<VerificationCodeItem?>> {
                val totpCode = cipher
                    .login
                    ?.totp
                    ?: run {
                        emit(DataState.Loaded(null))
                        return@flow
                    }

                var item: VerificationCodeItem? = null
                while (currentCoroutineContext().isActive) {
                    val time = (clock.millis() / ONE_SECOND_MILLISECOND).toInt()
                    val dateTime = DateTime.now()
                    if (item == null || item.isExpired(clock = clock)) {
                        vaultSdkSource
                            .generateTotp(
                                totp = totpCode,
                                userId = userId,
                                time = dateTime,
                            )
                            .onSuccess { response ->
                                item = VerificationCodeItem(
                                    code = response.code,
                                    periodSeconds = response.period.toInt(),
                                    timeLeftSeconds = response.period.toInt() -
                                        time % response.period.toInt(),
                                    issueTime = clock.millis(),
                                    uriLoginViewList = cipher.login?.uris,
                                    id = cipherId,
                                    name = cipher.name,
                                    username = cipher.login?.username,
                                    hasPasswordReprompt = when (cipher.reprompt) {
                                        CipherRepromptType.PASSWORD -> true
                                        CipherRepromptType.NONE -> false
                                    },
                                    orgUsesTotp = cipher.organizationUseTotp,
                                )
                            }
                            .onFailure {
                                emit(DataState.Loaded(null))
                                return@flow
                            }
                    } else {
                        item.let {
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
                    mutableVerificationCodeStateFlowMap.remove(cipher)
                    itemScope.cancel()
                }
                .stateIn(
                    scope = itemScope,
                    started = SharingStarted.WhileSubscribed(),
                    initialValue = DataState.Loading,
                )
        }
    }

    @Suppress("LongMethod")
    private fun getTotpCodeStateFlowInternal(
        userId: String,
        cipherListView: CipherListView?,
    ): StateFlow<DataState<VerificationCodeItem?>> {
        val cipherId = cipherListView?.id ?: return MutableStateFlow(DataState.Loaded(null))

        return mutableCipherListViewVerificationCodeStateFlowMap.getOrPut(cipherListView) {
            // Define a per-item scope so that we can clear the Flow from the scope when it is
            // no longer needed.
            val itemScope = CoroutineScope(dispatcherManager.unconfined)

            flow<DataState<VerificationCodeItem?>> {
                // If the item does not have a totpCode simply return null.
                cipherListView
                    .login
                    ?.totp
                    ?: run {
                        emit(DataState.Loaded(null))
                        return@flow
                    }

                var item: VerificationCodeItem? = null
                while (currentCoroutineContext().isActive) {
                    val time = (clock.millis() / ONE_SECOND_MILLISECOND).toInt()
                    val dateTime = clock.instant()
                    if (item == null || item.isExpired(clock = clock)) {
                        vaultSdkSource
                            .generateTotpForCipherListView(
                                cipherListView = cipherListView,
                                userId = userId,
                                time = dateTime,
                            )
                            .onSuccess { response ->
                                item = VerificationCodeItem(
                                    code = response.code,
                                    periodSeconds = response.period.toInt(),
                                    timeLeftSeconds = response.period.toInt() -
                                        time % response.period.toInt(),
                                    issueTime = clock.millis(),
                                    uriLoginViewList = cipherListView.login?.uris,
                                    id = cipherId,
                                    name = cipherListView.name,
                                    username = cipherListView.login?.username,
                                    hasPasswordReprompt = when (cipherListView.reprompt) {
                                        CipherRepromptType.PASSWORD -> true
                                        CipherRepromptType.NONE -> false
                                    },
                                    orgUsesTotp = cipherListView.organizationUseTotp,
                                )
                            }
                            .onFailure {
                                emit(DataState.Loaded(null))
                                return@flow
                            }
                    } else {
                        item.let {
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
                    mutableCipherListViewVerificationCodeStateFlowMap.remove(cipherListView)
                    itemScope.cancel()
                }
                .stateIn(
                    scope = itemScope,
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
