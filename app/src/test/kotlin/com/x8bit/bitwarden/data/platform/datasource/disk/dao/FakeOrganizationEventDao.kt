package com.x8bit.bitwarden.data.platform.datasource.disk.dao

import com.x8bit.bitwarden.data.platform.datasource.disk.entity.OrganizationEventEntity

class FakeOrganizationEventDao : OrganizationEventDao {
    val storedEvents = mutableListOf<OrganizationEventEntity>()

    var isDeleteCalled = false
    var isInsertCalled = false

    override suspend fun deleteOrganizationEvents(userId: String): Int {
        val count = storedEvents.count { it.userId == userId }
        storedEvents.removeAll { it.userId == userId }
        isDeleteCalled = true
        return count
    }

    override suspend fun getOrganizationEvents(
        userId: String,
    ): List<OrganizationEventEntity> = storedEvents.filter { it.userId == userId }

    override suspend fun insertOrganizationEvent(event: OrganizationEventEntity) {
        storedEvents.add(event)
        isInsertCalled = true
    }
}
