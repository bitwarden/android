package com.bitwarden.authenticator.data.authenticator.manager

import com.bitwarden.authenticator.data.authenticator.datasource.sdk.AuthenticatorSdkSource
import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.repository.model.AuthenticatorItem
import com.bitwarden.core.DateTime
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
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
import java.util.UUID
import javax.inject.Inject

private const val ONE_SECOND_MILLISECOND = 1000L

/**
 * Primary implementation of [TotpCodeManager].
 *
 * This implementation uses per-item [StateFlow] caching to prevent flow recreation on each
 * subscribe, ensuring smooth UI updates when returning from background. The pattern mirrors
 * the Password Manager's [com.x8bit.bitwarden.data.vault.manager.TotpCodeManagerImpl].
 */
class TotpCodeManagerImpl @Inject constructor(
    private val authenticatorSdkSource: AuthenticatorSdkSource,
    private val clock: Clock,
    private val dispatcherManager: DispatcherManager,
) : TotpCodeManager {

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    /**
     * Cache of per-item StateFlows to prevent recreation on each subscribe.
     * Key is the [AuthenticatorItem], value is the cached [StateFlow] for that item.
     */
    private val mutableItemVerificationCodeStateFlowMap =
        mutableMapOf<AuthenticatorItem, StateFlow<VerificationCodeItem?>>()

    override fun getTotpCodesFlow(
        itemList: List<AuthenticatorItem>,
    ): StateFlow<List<VerificationCodeItem>> {
        if (itemList.isEmpty()) {
            return MutableStateFlow(emptyList())
        }

        val stateFlows = itemList.map { getOrCreateItemStateFlow(it) }

        return combine(stateFlows) { results ->
            results.filterNotNull()
        }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = emptyList(),
            )
    }

    /**
     * Gets an existing [StateFlow] for the given [item] or creates a new one if it doesn't exist.
     * Each item gets its own [CoroutineScope] to manage its lifecycle independently.
     */
    private fun getOrCreateItemStateFlow(
        item: AuthenticatorItem,
    ): StateFlow<VerificationCodeItem?> {
        return mutableItemVerificationCodeStateFlowMap.getOrPut(item) {
            // Define a per-item scope so that we can clear the Flow from the map when it is
            // no longer needed.
            val itemScope = CoroutineScope(dispatcherManager.unconfined)

            createVerificationCodeFlow(item)
                .onCompletion {
                    mutableItemVerificationCodeStateFlowMap.remove(item)
                    itemScope.cancel()
                }
                .stateIn(
                    scope = itemScope,
                    started = SharingStarted.Eagerly,
                    initialValue = null,
                )
        }
    }

    /**
     * Creates a flow that emits [VerificationCodeItem] updates every second for the given [item].
     */
    @Suppress("LongMethod")
    private fun createVerificationCodeFlow(
        item: AuthenticatorItem,
    ) = flow<VerificationCodeItem?> {
        val otpUri = item.otpUri
        var verificationCodeItem: VerificationCodeItem? = null

        while (currentCoroutineContext().isActive) {
            val time = (clock.millis() / ONE_SECOND_MILLISECOND).toInt()

            if (verificationCodeItem == null || verificationCodeItem.isExpired(clock)) {
                // If the item is expired or we haven't generated our first item,
                // generate a new code using the SDK:
                verificationCodeItem = authenticatorSdkSource
                    .generateTotp(otpUri, DateTime.now())
                    .getOrNull()
                    ?.let { response ->
                        VerificationCodeItem(
                            code = response.code,
                            periodSeconds = response.period.toInt(),
                            timeLeftSeconds = response.period.toInt() -
                                time % response.period.toInt(),
                            issueTime = clock.millis(),
                            id = when (item.source) {
                                is AuthenticatorItem.Source.Local -> item.source.cipherId
                                is AuthenticatorItem.Source.Shared -> UUID.randomUUID().toString()
                            },
                            issuer = item.issuer,
                            label = item.label,
                            source = item.source,
                        )
                    }
                    ?: run {
                        // We are assuming that our otp URIs can generate a valid code.
                        // If they can't, we'll just silently omit that code from the list.
                        emit(null)
                        return@flow
                    }
            } else {
                // Item is not expired, just update time left:
                verificationCodeItem = verificationCodeItem.copy(
                    timeLeftSeconds = verificationCodeItem.periodSeconds -
                        (time % verificationCodeItem.periodSeconds),
                )
            }

            // Emit item
            emit(verificationCodeItem)

            // Wait one second before heading to the top of the loop:
            delay(ONE_SECOND_MILLISECOND)
        }
    }
}

private fun VerificationCodeItem.isExpired(clock: Clock): Boolean {
    val timeExpired = issueTime + (timeLeftSeconds * ONE_SECOND_MILLISECOND)
    return timeExpired < clock.millis()
}
