package com.x8bit.bitwarden.data.platform.manager

import java.time.Instant

/**
 * Manager for tracking changes to database scheme(s).
 */
interface DatabaseSchemeManager {

    /**
     * The instant of the last database schema change performed on the database, if any.
     */
    var lastDatabaseSchemeChangeInstant: Instant?
}
