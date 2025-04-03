package com.x8bit.bitwarden.data.platform.datasource.network.service

import com.bitwarden.network.api.EventApi
import com.bitwarden.network.model.OrganizationEventJson
import com.bitwarden.network.util.toResult

/**
 * The default implementation of the [EventService].
 */
class EventServiceImpl(
    private val eventApi: EventApi,
) : EventService {
    override suspend fun sendOrganizationEvents(
        events: List<OrganizationEventJson>,
    ): Result<Unit> = eventApi.collectOrganizationEvents(events = events).toResult()
}
