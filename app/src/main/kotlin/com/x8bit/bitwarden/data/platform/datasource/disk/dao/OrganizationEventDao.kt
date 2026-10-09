package com.x8bit.bitwarden.data.platform.datasource.disk.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.x8bit.bitwarden.data.platform.datasource.disk.entity.OrganizationEventEntity

/**
 * Provides methods for inserting, retrieving, and deleting events from the database using the
 * [OrganizationEventEntity].
 */
@Dao
interface OrganizationEventDao {
    /**
     * Deletes all the stored events associated with the given [userId]. This will return the
     * number of rows deleted by this query.
     */
    @Query("DELETE FROM organization_events WHERE user_id = :userId")
    suspend fun deleteOrganizationEvents(userId: String): Int

    /**
     * Retrieves all events from the database for a given [userId].
     */
    @Query("SELECT * FROM organization_events WHERE user_id = :userId")
    suspend fun getOrganizationEvents(userId: String): List<OrganizationEventEntity>

    /**
     * Inserts an event into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrganizationEvent(event: OrganizationEventEntity)
}
