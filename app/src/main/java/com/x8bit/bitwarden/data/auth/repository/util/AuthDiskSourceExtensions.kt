package com.x8bit.bitwarden.data.auth.repository.util

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.model.UserOrganizations
import com.x8bit.bitwarden.data.auth.repository.model.UserSwitchingData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
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
