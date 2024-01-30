package com.x8bit.bitwarden.data.auth.repository.util

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.model.UserOrganizations
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
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
 * Returns a [Flow] that emits distinct updates to the
 * current user's [SyncResponseJson.Policy] list.
 */
@OptIn(ExperimentalCoroutinesApi::class)
val AuthDiskSource.currentUserPoliciesListFlow: Flow<List<SyncResponseJson.Policy>?>
    get() =
        this
            .userStateFlow
            .flatMapLatest { userStateJson ->
                userStateJson
                    ?.activeUserId
                    ?.let { activeUserId ->
                        this.getPoliciesFlow(activeUserId)
                    }
                    ?: emptyFlow()
            }
            .distinctUntilChanged()
