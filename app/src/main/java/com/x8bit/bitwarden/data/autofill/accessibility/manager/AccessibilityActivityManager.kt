package com.x8bit.bitwarden.data.autofill.accessibility.manager

import android.app.Activity

/**
 * A helper for dealing with accessibility configuration that must be scoped to a specific
 * [Activity]. In particular, this should be injected into an [Activity] to ensure that the
 * [AccessibilityEnabledManager] reports correct values.
 */
interface AccessibilityActivityManager
