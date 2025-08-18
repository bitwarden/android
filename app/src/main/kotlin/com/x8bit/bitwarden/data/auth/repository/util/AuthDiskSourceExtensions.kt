package com.x8bit.bitwarden.data.auth.repository.util

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.model.UserAccountTokens
import com.x8bit.bitwarden.data.auth.repository.model.UserKeyConnectorState
import com.x8bit.bitwarden.data.auth.repository.model.UserOrganizations
import com.x8bit.bitwarden.data.auth.repository.model.UserSwitchingData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Returns the current list of [UserOrganizations].
 */
val AuthDiskSource.userOrganizationsList: List<UserOrganizations>
    get() = this
        .userState
        ?.accounts
        .orEmpty()
        .map { (userId, _) ->
            UserOrganizations(
                userId = userId,
                organizations = this
                    .getOrganizations(userId = userId)
                    .orEmpty()
                    .toOrganizations(),
            )
        }

/**
 * Returns a [Flow] that emits distinct updates to [UserOrganizations].
 */
@OptIn(ExperimentalCoroutinesApi::class)
val AuthDiskSource.userOrganizationsListFlow: Flow<List<UserOrganizations>>
    get() =
        this
            .userStateFlow
            .flatMapLatest { userStateJson ->
                combine(
                    userStateJson
                        ?.accounts
                        .orEmpty()
                        .map { (userId, _) ->
                            this
                                .getOrganizationsFlow(userId = userId)
                                .map {
                                    UserOrganizations(
                                        userId = userId,
                                        organizations = it.orEmpty().toOrganizations(),
                                    )
                                }
                        },
                ) { values -> values.toList() }
            }
            .distinctUntilChanged()

/**
 * Returns the current list of [UserAccountTokens].
 */
val AuthDiskSource.userAccountTokens: List<UserAccountTokens>
    get() = this
        .userState
        ?.accounts
        .orEmpty()
        .map { (userId, _) ->
            val accountTokens = this.getAccountTokens(userId = userId)
            UserAccountTokens(
                userId = userId,
                accessToken = accountTokens?.accessToken,
                refreshToken = accountTokens?.refreshToken,
            )
        }

/**
 * Returns a [Flow] that emits distinct updates to [UserAccountTokens].
 */
@OptIn(ExperimentalCoroutinesApi::class)
val AuthDiskSource.userAccountTokensFlow: Flow<List<UserAccountTokens>>
    get() = this
        .userStateFlow
        .flatMapLatest { userStateJson ->
            combine(
                userStateJson
                    ?.accounts
                    .orEmpty()
                    .map { (userId, _) ->
                        this
                            .getAccountTokensFlow(userId = userId)
                            .map {
                                UserAccountTokens(
                                    userId = userId,
                                    accessToken = it?.accessToken,
                                    refreshToken = it?.refreshToken,
                                )
                            }
                    },
            ) { it.toList() }
        }
        .distinctUntilChanged()

/**
 * Returns the current list of [UserKeyConnectorState].
 */
val AuthDiskSource.userKeyConnectorStateList: List<UserKeyConnectorState>
    get() = this
        .userState
        ?.accounts
        .orEmpty()
        .map { (userId, _) ->
            UserKeyConnectorState(
                userId = userId,
                isUsingKeyConnector = this.getShouldUseKeyConnector(userId = userId),
            )
        }

/**
 * Returns a [Flow] that emits distinct updates to [UserKeyConnectorState].
 */
@OptIn(ExperimentalCoroutinesApi::class)
val AuthDiskSource.userKeyConnectorStateFlow: Flow<List<UserKeyConnectorState>>
    get() = this
        .userStateFlow
        .flatMapLatest { userStateJson ->
            combine(
                userStateJson
                    ?.accounts
                    .orEmpty()
                    .map { (userId, _) ->
                        this
                            .getShouldUseKeyConnectorFlow(userId = userId)
                            .map {
                                UserKeyConnectorState(
                                    userId = userId,
                                    isUsingKeyConnector = it,
                                )
                            }
                    },
            ) { it.toList() }
        }
        .distinctUntilChanged()

/**
 * Returns a [Flow] that emits every time the active user is changed.
 */
val AuthDiskSource.userSwitchingChangesFlow: Flow<UserSwitchingData>
    get() {
        var lastActiveUserId: String? = null
        return activeUserIdChangesFlow
            .map { activeUserId ->
                val previousActiveUserId = lastActiveUserId
                lastActiveUserId = activeUserId
                UserSwitchingData(
                    previousActiveUserId = previousActiveUserId,
                    currentActiveUserId = activeUserId,
                )
            }
    }

/**
 * Returns a [Flow] that emits every time the active user ID is changed.
 */
val AuthDiskSource.activeUserIdChangesFlow: Flow<String?>
    get() = this
        .userStateFlow
        .map { it?.activeUserId }
        .distinctUntilChanged()

/**
 * Returns a [Flow] that emits every time the active user's onboarding status is changed
 */
@OptIn(ExperimentalCoroutinesApi::class)
val AuthDiskSource.onboardingStatusChangesFlow: Flow<OnboardingStatus?>
    get() = activeUserIdChangesFlow
        .flatMapLatest { activeUserId ->
            activeUserId
                ?.let { this.getOnboardingStatusFlow(userId = it) }
                ?: flowOf(null)
        }
        .distinctUntilChanged()

/**
 * Returns the current [OnboardingStatus] of the active user.
 */
val AuthDiskSource.currentOnboardingStatus: OnboardingStatus?
    get() = this
        .userState
        ?.activeUserId
        ?.let { this.getOnboardingStatus(userId = it) }
