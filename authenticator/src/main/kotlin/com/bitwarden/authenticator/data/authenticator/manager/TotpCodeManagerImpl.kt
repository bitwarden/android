package com.bitwarden.authenticator.data.authenticator.manager

import com.bitwarden.authenticator.data.authenticator.datasource.sdk.AuthenticatorSdkSource
import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.repository.model.AuthenticatorItem
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

private const val ONE_SECOND_MILLISECOND = 1000L

/**
 * Primary implementation of [TotpCodeManager].
 */
class TotpCodeManagerImpl @Inject constructor(
    private val authenticatorSdkSource: AuthenticatorSdkSource,
    private val clock: Clock,
) : TotpCodeManager {

    override fun getTotpCodesFlow(
        itemList: List<AuthenticatorItem>,
    ): Flow<List<VerificationCodeItem>> {
        if (itemList.isEmpty()) {
            return flowOf(emptyList())
        }
        val flows = itemList.map { it.toFlowOfVerificationCodes() }
        return combine(flows) { it.toList() }
    }

    private fun AuthenticatorItem.toFlowOfVerificationCodes(): Flow<VerificationCodeItem> {
        val otpUri = this.otpUri
        return flow {
            var item: VerificationCodeItem? = null
            while (currentCoroutineContext().isActive) {
                val time = (clock.millis() / ONE_SECOND_MILLISECOND).toInt()
                if (item == null || item.isExpired(clock)) {
                    // If the item is expired or we haven't generated our first item,
                    // generate a new code using the SDK:
                    item = authenticatorSdkSource
                        .generateTotp(otpUri, clock.instant())
                        .getOrNull()
                        ?.let { response ->
                            VerificationCodeItem(
                                code = response.code,
                                periodSeconds = response.period.toInt(),
                                timeLeftSeconds = response.period.toInt() -
                                    time % response.period.toInt(),
                                issueTime = clock.millis(),
                                id = when (source) {
                                    is AuthenticatorItem.Source.Local -> source.cipherId
                                    is AuthenticatorItem.Source.Shared -> UUID.randomUUID()
                                        .toString()
                                },
                                issuer = issuer,
                                label = label,
                                source = source,
                            )
                        }
                        ?: run {
                            // We are assuming that our otp URIs can generate a valid code.
                            // If they can't, we'll just silently omit that code from the list.
                            currentCoroutineContext().cancel()
                            return@flow
                        }
                } else {
                    // Item is not expired, just update time left:
                    item = item.copy(
                        timeLeftSeconds = item.periodSeconds - (time % item.periodSeconds),
                    )
                }
                // Emit item
                emit(item)
                // Wait one second before heading to the top of the loop:
                delay(ONE_SECOND_MILLISECOND)
            }
        }
    }
}

private fun VerificationCodeItem.isExpired(clock: Clock): Boolean {
    val timeExpired = issueTime + (timeLeftSeconds * ONE_SECOND_MILLISECOND)
    return timeExpired < clock.millis()
}
