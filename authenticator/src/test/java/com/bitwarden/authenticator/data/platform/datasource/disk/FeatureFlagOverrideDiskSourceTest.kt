package com.bitwarden.authenticator.data.platform.datasource.disk

import androidx.core.content.edit
import com.bitwarden.authenticator.data.platform.base.FakeSharedPreferences
import com.bitwarden.authenticator.data.platform.manager.model.FlagKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FeatureFlagOverrideDiskSourceTest {
    private val fakeSharedPreferences = FakeSharedPreferences()

    private val featureFlagOverrideDiskSource = FeatureFlagOverrideDiskSourceImpl(
        sharedPreferences = fakeSharedPreferences,
    )

    @Test
    fun `call to save feature flag should update SharedPreferences for booleans`() {
        val key = FlagKey.DummyBoolean
        assertFalse(
            fakeSharedPreferences.getBoolean(
                key.keyName,
                false,
            ),
        )
        val value = true
        featureFlagOverrideDiskSource.saveFeatureFlag(key, value)
        assertTrue(
            fakeSharedPreferences.getBoolean(
                key.keyName,
                false,
            ),
        )
    }

    @Test
    fun `call to get feature flag should return correct value for booleans`() {
        val key = FlagKey.DummyBoolean
        fakeSharedPreferences.edit {
            putBoolean(key.keyName, true)
        }

        val actual = featureFlagOverrideDiskSource.getFeatureFlag(key)
        assertTrue(actual!!)
    }

    @Test
    fun `call to save feature flag should update SharedPreferences for strings`() {
        val key = FlagKey.DummyString
        assertNull(
            fakeSharedPreferences.getString(
                key.keyName,
                null,
            ),
        )
        val expectedValue = "string"
        featureFlagOverrideDiskSource.saveFeatureFlag(key, expectedValue)
        assertEquals(
            fakeSharedPreferences.getString(
                key.keyName,
                null,
            ),
            expectedValue,
        )
    }

    @Test
    fun `call to get feature flag should return correct value for strings`() {
        val key = FlagKey.DummyString
        assertNull(featureFlagOverrideDiskSource.getFeatureFlag(key))
        val expectedValue = "string"
        fakeSharedPreferences.edit {
            putString(key.keyName, expectedValue)
        }

        val actual = featureFlagOverrideDiskSource.getFeatureFlag(key)
        assertEquals(actual, expectedValue)
    }

    @Test
    fun `call to save feature flag should update SharedPreferences for ints`() {
        val key = FlagKey.DummyInt()
        assertEquals(
            fakeSharedPreferences.getInt(
                key.keyName,
                0,
            ),
            0,
        )
        val expectedValue = 1
        featureFlagOverrideDiskSource.saveFeatureFlag(key, expectedValue)
        assertEquals(
            fakeSharedPreferences.getInt(
                key.keyName,
                0,
            ),
            expectedValue,
        )
    }

    @Test
    fun `call to get feature flag should return correct value for ints`() {
        val key = FlagKey.DummyInt()
        assertNull(featureFlagOverrideDiskSource.getFeatureFlag(key))
        val expectedValue = 1
        fakeSharedPreferences.edit {
            putInt(key.keyName, expectedValue)
        }

        val actual = featureFlagOverrideDiskSource.getFeatureFlag(key)
        assertEquals(actual, expectedValue)
    }
}
