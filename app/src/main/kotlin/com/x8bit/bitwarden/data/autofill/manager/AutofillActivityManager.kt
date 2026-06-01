package com.x8bit.bitwarden.data.autofill.manager

import android.app.Activity

/**
 * A helper for dealing with autofill configuration that must be scoped to a specific [Activity].
 * In particular, this should be injected into an [Activity] to ensure that an
 * [AutofillEnabledManager] reports correct values.
 */
interface AutofillActivityManager
