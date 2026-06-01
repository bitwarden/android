package com.x8bit.bitwarden.data.credentials.datasource.disk.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Entity representing a trusted privileged app in the database.
 */
@Entity(
    tableName = "privileged_apps",
    primaryKeys = ["package_name", "signature"],
)
data class PrivilegedAppEntity(
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "signature")
    val signature: String,
)
