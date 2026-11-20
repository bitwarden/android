package com.x8bit.bitwarden.data.vault.datasource.disk.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Entity representing a set of domains in the database.
 */
@Entity(tableName = "domains")
data class DomainsEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "domains_json")
    val domainsJson: String?,
)
