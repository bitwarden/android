package com.bitwarden.authenticator.data.authenticator.datasource.disk.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Provides methods for inserting, reading, and deleting authentication items from the database
 * using [AuthenticatorItemEntity].
 */
@Dao
interface ItemDao {

    /**
     * Inserts a single authenticator item into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg item: AuthenticatorItemEntity)

    /**
     * Read all authenticator items from the database.
     */
    @Query("SELECT * FROM items")
    fun getAllItems(): Flow<List<AuthenticatorItemEntity>>

    /**
     * Deletes the specified authenticator item with the given [itemId]. This will return the number
     * of rows deleted by this query.
     */
    @Query("DELETE FROM items WHERE id = :itemId")
    suspend fun deleteItem(itemId: String): Int
}
