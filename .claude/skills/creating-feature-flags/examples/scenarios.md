# Feature Flag Creation Scenarios

Common use cases and complete examples for creating feature flags.

## Scenario 1: Simple Boolean Feature Flag

**Use Case:** Adding a flag to control rollout of a new Password Manager feature.

**Example: Enable Password History Export**

**User Inputs:**
- Flag Name: "PM-12345 Enable Password History Export"
- Type: Boolean
- Application: Password Manager
- Display Label: "Enable Password History Export"
- Default: false (confirmed)

**Generated Names:**
- keyName: `pm-12345-enable-password-history-export`
- DataClassName: `EnablePasswordHistoryExport`
- string_resource_key: `enable_password_history_export`

**Files Modified:**
1. FlagKey.kt - Added data object and registered in activePasswordManagerFlags
2. FlagKeyTest.kt - Added assertions for keyName and defaultValue
3. FeatureFlagListItems.kt - Added to Boolean when expressions
4. strings_non_localized.xml - Added display string

**Verification Commands:**
```bash
./gradlew :core:testDebug --tests "com.bitwarden.core.data.manager.model.FlagKeyTest"
./gradlew :core:compileDebugKotlin :ui:compileDebugKotlin :app:compileStandardDebugKotlin
```

**Usage in Code:**
```kotlin
featureFlagManager.getFeatureFlagFlow(FlagKey.EnablePasswordHistoryExport)
    .onEach { isEnabled ->
        if (isEnabled) {
            // Show password history export option
        }
    }
    .launchIn(viewModelScope)
```

---

## Scenario 2: Authenticator-Only Flag

**Use Case:** Feature specific to the Authenticator application.

**Example: Enable Biometric Unlock**

**User Inputs:**
- Flag Name: "Enable Biometric Unlock"
- Type: Boolean
- Application: Authenticator (only)
- Display Label: "Enable Biometric Unlock"
- Default: false

**Generated Names:**
- keyName: `enable-biometric-unlock`
- DataClassName: `EnableBiometricUnlock`
- string_resource_key: `enable_biometric_unlock`

**Key Difference:**
- Added to `activeAuthenticatorFlags` instead of `activePasswordManagerFlags`
- Only appears in Authenticator app's debug menu

**Usage in Authenticator:**
```kotlin
featureFlagManager.getFeatureFlagFlow(FlagKey.EnableBiometricUnlock)
    .onEach { isEnabled ->
        if (isEnabled) {
            // Enable biometric authentication option
        }
    }
    .launchIn(viewModelScope)
```

---

## Scenario 3: Multi-Application Flag

**Use Case:** Feature that applies to both Password Manager and Authenticator.

**Example: Enhanced Logging**

**User Inputs:**
- Flag Name: "Enhanced Logging"
- Type: Boolean
- Application: Password Manager AND Authenticator (both selected)
- Display Label: "Enhanced Logging"
- Default: false

**Generated Names:**
- keyName: `enhanced-logging`
- DataClassName: `EnhancedLogging`
- string_resource_key: `enhanced_logging`

**Key Difference:**
- Added to BOTH `activePasswordManagerFlags` AND `activeAuthenticatorFlags`
- Appears in debug menu of both applications
- Single flag definition works for both

**Active Flags Registration:**
```kotlin
val activePasswordManagerFlags: List<FlagKey<*>> by lazy {
    listOf(
        // ... other flags ...
        EnhancedLogging,
    )
}

val activeAuthenticatorFlags: List<FlagKey<*>> by lazy {
    listOf(
        // ... other flags ...
        EnhancedLogging,
    )
}
```

---

## Scenario 4: Feature Flag Without JIRA Ticket

**Use Case:** Internal feature or experiment not tracked in JIRA.

**Example: Experimental UI Animation**

**User Inputs:**
- Flag Name: "Experimental UI Animation"
- Type: Boolean
- Application: Password Manager
- Display Label: "Experimental UI Animation"
- Default: false

**Generated Names:**
- keyName: `experimental-ui-animation` (no JIRA prefix)
- DataClassName: `ExperimentalUiAnimation`
- string_resource_key: `experimental_ui_animation`

**Note:** Absence of JIRA ticket is fine for internal flags, experiments, or R&D features.

---

## Scenario 5: Boolean Flag with True Default

**Use Case:** Flag that's enabled by default, can be disabled if issues occur.

**Example: Use Optimized Crypto**

**User Inputs:**
- Flag Name: "Use Optimized Crypto"
- Type: Boolean
- Application: Password Manager
- Display Label: "Use Optimized Crypto"
- Default: true (user explicitly requested true)

**Generated Names:**
- keyName: `use-optimized-crypto`
- DataClassName: `UseOptimizedCrypto`
- string_resource_key: `use_optimized_crypto`

**Test Difference:**
Requires separate test assertion instead of adding to the list:
```kotlin
@Test
fun `UseOptimizedCrypto has correct default value`() {
    assertTrue(FlagKey.UseOptimizedCrypto.defaultValue)
}
```

---

## Scenario 6: Int Flag (Advanced)

**Use Case:** Configurable numeric threshold or count.

**Example: Max Login Attempts**

**User Inputs:**
- Flag Name: "Max Login Attempts"
- Type: Int
- Application: Password Manager
- Display Label: "Max Login Attempts"
- Default: 5

**Generated Names:**
- keyName: `max-login-attempts`
- DataClassName: `MaxLoginAttempts`
- string_resource_key: `max_login_attempts`

**Special Handling:**
- Works fully as a feature flag
- Can be accessed via FeatureFlagManager
- **Does NOT appear in debug menu** (requires custom IntFlagItem UI component)
- Add TODO comment in FeatureFlagListItems.kt

**Usage:**
```kotlin
featureFlagManager.getFeatureFlagFlow(FlagKey.MaxLoginAttempts)
    .onEach { maxAttempts ->
        // Use the configured max attempts value
        if (attemptCount >= maxAttempts) {
            // Lock account
        }
    }
    .launchIn(viewModelScope)
```

**Test Pattern:**
```kotlin
@Test
fun `MaxLoginAttempts has correct default value`() {
    assertEquals(5, FlagKey.MaxLoginAttempts.defaultValue)
}
```

---

## Scenario 7: String Flag (Advanced)

**Use Case:** Configurable string value or enum.

**Example: API Endpoint Override**

**User Inputs:**
- Flag Name: "API Endpoint Override"
- Type: String
- Application: Password Manager
- Display Label: "API Endpoint Override"
- Default: "https://api.bitwarden.com"

**Generated Names:**
- keyName: `api-endpoint-override`
- DataClassName: `ApiEndpointOverride`
- string_resource_key: `api_endpoint_override`

**Special Handling:**
- Works fully as a feature flag
- **Does NOT appear in debug menu** (requires custom StringFlagItem UI component)
- Useful for testing different environments or configurations

**Usage:**
```kotlin
featureFlagManager.getFeatureFlagFlow(FlagKey.ApiEndpointOverride)
    .onEach { apiEndpoint ->
        // Use the configured API endpoint
        retrofit.baseUrl(apiEndpoint)
    }
    .launchIn(viewModelScope)
```

---

## Scenario 8: Reusing Existing Flag Pattern

**Use Case:** Creating a flag similar to an existing one.

**Example: New CXP Feature (following CXP Import/Export pattern)**

**Reference Existing Flags:**
- CredentialExchangeProtocolImport: `cxp-import-mobile`
- CredentialExchangeProtocolExport: `cxp-export-mobile`

**New Flag:**
- Flag Name: "CXP Sync Mobile"
- keyName: `cxp-sync-mobile` (follows existing pattern)
- DataClassName: `CredentialExchangeProtocolSync`

**Benefits:**
- Consistent naming with related features
- Easy to understand relationship
- Groups related flags together in code

---

## Anti-Pattern Examples

### ❌ Anti-Pattern 1: Vague Flag Name

**Bad:**
- Flag Name: "New Feature"
- Result: keyName `new-feature` (what feature?)

**Good:**
- Flag Name: "Enable Password Breach Monitoring"
- Result: keyName `enable-password-breach-monitoring` (clear purpose)

### ❌ Anti-Pattern 2: Overly Specific Flag

**Bad:**
- Flag Name: "Show Blue Button on Settings Screen Third Row"
- Too specific, couples flag to UI implementation

**Good:**
- Flag Name: "Enable Advanced Settings"
- Describes feature, not implementation details

### ❌ Anti-Pattern 3: Wrong Application Target

**Bad:**
- Creating Authenticator-specific flag but targeting Password Manager
- Users won't see the flag in the correct app

**Good:**
- Match application target to where feature will be used
- Use multi-application only when truly shared

### ❌ Anti-Pattern 4: Duplicate Flag

**Bad:**
- Creating `enable-totp-export` when `credential-exchange-protocol-export` already handles TOTP export

**Good:**
- Check existing flags first
- Reuse existing flags when features overlap

---

## Next Steps After Creation

**1. Verify Creation:**
```bash
# Run tests
./gradlew :core:testDebug --tests "FlagKeyTest"

# Verify compilation
./gradlew :app:compileStandardDebugKotlin
```

**2. Test in Debug Menu:**
- Build and install app
- Navigate to Settings → Debug Menu
- Verify flag appears with correct label
- Toggle flag and verify state persists

**3. Implement Feature:**
```kotlin
// In ViewModel or Repository
init {
    featureFlagManager.getFeatureFlagFlow(FlagKey.{YourFlag})
        .onEach { isEnabled ->
            // React to flag state
        }
        .launchIn(viewModelScope)
}
```

**4. Create PR:**
- Include flag in feature PR, or
- Create separate flag-only PR (preferred for large features)
- Reference JIRA ticket in PR description
- Note that flag defaults to false (safe rollout)

**5. Remote Configuration:**
- After merge, configure flag in remote feature flag system
- Test rollout with small percentage
- Monitor for issues
- Gradually increase rollout percentage