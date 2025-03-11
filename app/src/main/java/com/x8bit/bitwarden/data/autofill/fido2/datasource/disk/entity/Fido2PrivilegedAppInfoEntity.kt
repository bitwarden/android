package com.x8bit.bitwarden.data.autofill.fido2.datasource.disk.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Entity representing a trusted privileged app in the database.
 */
@Entity(
    tableName = "fido2_privileged_apps",
    primaryKeys = ["package_name", "signature"],
)
data class Fido2PrivilegedAppInfoEntity(
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "signature")
    val signature: String,
)
