package com.x8bit.bitwarden.data.platform.datasource.disk

import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.network.model.OrganizationEventJson
import com.bitwarden.network.model.OrganizationEventType
import com.x8bit.bitwarden.data.platform.datasource.disk.dao.OrganizationEventDao
import com.x8bit.bitwarden.data.platform.datasource.disk.entity.OrganizationEventEntity
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * The default implementation of [EventDiskSource].
 */
class EventDiskSourceImpl(
    private val organizationEventDao: OrganizationEventDao,
    private val dispatcherManager: DispatcherManager,
    private val json: Json,
) : EventDiskSource {

    override suspend fun deleteOrganizationEvents(userId: String) {
        organizationEventDao.deleteOrganizationEvents(userId = userId)
    }

    override suspend fun addOrganizationEvent(userId: String, event: OrganizationEventJson) {
        organizationEventDao.insertOrganizationEvent(
            event = OrganizationEventEntity(
                userId = userId,
                organizationEventType = withContext(context = dispatcherManager.default) {
                    json.encodeToString(value = event.type)
                },
                cipherId = event.cipherId,
                date = event.date,
            ),
        )
    }

    override suspend fun getOrganizationEvents(
        userId: String,
    ): List<OrganizationEventJson> =
        organizationEventDao
            .getOrganizationEvents(userId = userId)
            .map {
                OrganizationEventJson(
                    type = withContext(context = dispatcherManager.default) {
                        json.decodeFromString<OrganizationEventType>(
                            string = it.organizationEventType,
                        )
                    },
                    cipherId = it.cipherId,
                    date = it.date,
                )
            }
}
