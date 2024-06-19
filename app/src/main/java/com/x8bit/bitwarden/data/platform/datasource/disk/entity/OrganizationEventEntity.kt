package com.x8bit.bitwarden.data.platform.datasource.disk.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.ZonedDateTime

/**
 * Entity representing an organization event in the database.
 */
@Entity(tableName = "organization_events")
data class OrganizationEventEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "user_id", index = true)
    val userId: String,

    @ColumnInfo(name = "organization_event_type")
    val organizationEventType: String,

    @ColumnInfo(name = "cipher_id")
    val cipherId: String?,

    @ColumnInfo(name = "date")
    val date: ZonedDateTime,
)
