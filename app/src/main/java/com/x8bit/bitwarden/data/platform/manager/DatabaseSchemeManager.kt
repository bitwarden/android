package com.x8bit.bitwarden.data.platform.manager

import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Manager for tracking changes to database scheme(s).
 */
interface DatabaseSchemeManager {

    /**
     * The instant of the last database schema change performed on the database, if any.
     *
     * There is only a single scheme change instant tracked for all database schemes. It is expected
     * that a scheme change to any database will update this value and trigger a sync.
     */
    var lastDatabaseSchemeChangeInstant: Instant?

    /**
     * A flow of the last database schema change instant.
     */
    val lastDatabaseSchemeChangeInstantFlow: Flow<Instant?>
}
