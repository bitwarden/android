# Feature Flag Code Templates

Complete code templates for all file modifications required when creating feature flags.

## Template Variables

Throughout these templates, replace the following placeholders:

- `{DataClassName}` - PascalCase data object name (e.g., `EnableTotpExport`)
- `{keyName}` - kebab-case key name (e.g., `enable-totp-export`)
- `{Type}` - Flag type: `Boolean`, `Int`, or `String`
- `{defaultValue}` - Default value for the flag
- `{FeatureDescription}` - Human-readable feature description
- `{string_resource_key}` - snake_case resource key (e.g., `enable_totp_export`)
- `{DisplayLabel}` - Display label for debug menu

## FlagKey.kt Templates

**Location:** `core/src/main/kotlin/com/bitwarden/core/data/manager/model/FlagKey.kt`

### Boolean Flag Template

```kotlin
/**
 * Data object holding the feature flag key for the {FeatureDescription} feature.
 */
data object {DataClassName} : FlagKey<Boolean>() {
    override val keyName: String = "{keyName}"
    override val defaultValue: Boolean = false
}
```

**Example:**
```kotlin
/**
 * Data object holding the feature flag key for the Enable TOTP Export feature.
 */
data object EnableTotpExport : FlagKey<Boolean>() {
    override val keyName: String = "enable-totp-export"
    override val defaultValue: Boolean = false
}
```

### Int Flag Template

```kotlin
/**
 * Data object holding the feature flag key for the {FeatureDescription} feature.
 */
data object {DataClassName} : FlagKey<Int>() {
    override val keyName: String = "{keyName}"
    override val defaultValue: Int = {defaultValue}
}
```

**Example:**
```kotlin
/**
 * Data object holding the feature flag key for the Max Retry Count feature.
 */
data object MaxRetryCount : FlagKey<Int>() {
    override val keyName: String = "max-retry-count"
    override val defaultValue: Int = 3
}
```

### String Flag Template

```kotlin
/**
 * Data object holding the feature flag key for the {FeatureDescription} feature.
 */
data object {DataClassName} : FlagKey<String>() {
    override val keyName: String = "{keyName}"
    override val defaultValue: String = "{defaultValue}"
}
```

**Example:**
```kotlin
/**
 * Data object holding the feature flag key for the API Endpoint Override feature.
 */
data object ApiEndpointOverride : FlagKey<String>() {
    override val keyName: String = "api-endpoint-override"
    override val defaultValue: String = "https://api.bitwarden.com"
}
```

### Active Flags List Addition

**For Password Manager flags:**
```kotlin
val activePasswordManagerFlags: List<FlagKey<*>> by lazy {
    listOf(
        // ... existing flags ...
        {DataClassName},  // Add this line
    )
}
```

**For Authenticator flags:**
```kotlin
val activeAuthenticatorFlags: List<FlagKey<*>> by lazy {
    listOf(
        // ... existing flags ...
        {DataClassName},  // Add this line
    )
}
```

## FlagKeyTest.kt Templates

**Location:** `core/src/test/kotlin/com/bitwarden/core/data/manager/model/FlagKeyTest.kt`

### keyName Test Assertion

Add to the test: `fun \`Feature flags have the correct key name set\``

```kotlin
assertEquals(
    FlagKey.{DataClassName}.keyName,
    "{keyName}",
)
```

**Example:**
```kotlin
assertEquals(
    FlagKey.EnableTotpExport.keyName,
    "enable-totp-export",
)
```

### Default Value Tests

**For Boolean flags with false default:**

Add to the list in test: `fun \`All feature flags have the correct default value set\``

```kotlin
assertTrue(
    listOf(
        // ... existing flags ...
        FlagKey.{DataClassName},  // Add this line
    ).all {
        !it.defaultValue
    },
)
```

**For Boolean flags with true default:**

Create new test function:

```kotlin
@Test
fun `{DataClassName} has correct default value`() {
    assertTrue(FlagKey.{DataClassName}.defaultValue)
}
```

**For Int flags:**

```kotlin
@Test
fun `{DataClassName} has correct default value`() {
    assertEquals({defaultValue}, FlagKey.{DataClassName}.defaultValue)
}
```

**For String flags:**

```kotlin
@Test
fun `{DataClassName} has correct default value`() {
    assertEquals("{defaultValue}", FlagKey.{DataClassName}.defaultValue)
}
```

## FeatureFlagListItems.kt Templates

**Location:** `ui/src/main/kotlin/com/bitwarden/ui/platform/components/debug/FeatureFlagListItems.kt`

### ListItemContent When Expression Update

**For Boolean flags:**

Add to existing Boolean block (around line 25-32):

```kotlin
when (val flagKey = this) {
    // ... existing dummy cases ...

    FlagKey.DummyBoolean,
    // ... other existing Boolean flags ...
    FlagKey.{DataClassName},  // Add this line
        -> {
        @Suppress("UNCHECKED_CAST")
        BooleanFlagItem(
            label = flagKey.getDisplayLabel(),
            key = flagKey as FlagKey<Boolean>,
            currentValue = currentValue as Boolean,
            onValueChange = onValueChange as (FlagKey<Boolean>, Boolean) -> Unit,
            cardStyle = cardStyle,
            modifier = modifier,
        )
    }
}
```

**For Int/String flags:**

**NOTE:** Int and String flags require additional UI component implementation. The codebase currently only supports Boolean flags in the debug menu.

### getDisplayLabel When Expression Update

Add to the when expression (around line 73-80):

```kotlin
@Composable
private fun <T : Any> FlagKey<T>.getDisplayLabel(): String = when (this) {
    // ... existing cases ...
    FlagKey.{DataClassName} -> stringResource(BitwardenString.{string_resource_key})
}
```

**Example:**
```kotlin
FlagKey.EnableTotpExport -> stringResource(BitwardenString.enable_totp_export)
```

## strings_non_localized.xml Template

**Location:** `ui/src/main/res/values/strings_non_localized.xml`

Add within the `<!-- region Debug Menu -->` section (around line 20-42):

```xml
<string name="{string_resource_key}">{DisplayLabel}</string>
```

**Example:**
```xml
<string name="enable_totp_export">Enable TOTP Export</string>
```

## Insertion Points

**FlagKey.kt:**
- **Data object:** Insert before `//region Dummy keys for testing` comment
- **Active flags list:** Insert within appropriate list

**FlagKeyTest.kt:**
- **keyName test:** Add assertion within existing test function
- **defaultValue test:** Add to list or create new test based on type/value

**FeatureFlagListItems.kt:**
- **ListItemContent:** Add to appropriate type block (Boolean/Int/String)
- **getDisplayLabel:** Add case to when expression

**strings_non_localized.xml:**
- **String resource:** Add within Debug Menu region

## Complete Example: Boolean Flag

**Flag Details:**
- Flag Name: "PM-12345 Enable TOTP Export"
- Type: Boolean
- Default: false
- Application: Password Manager
- Display Label: "Enable TOTP Export"

**Generated Names:**
- keyName: `pm-12345-enable-totp-export`
- DataClassName: `EnableTotpExport`
- string_resource_key: `enable_totp_export`

### FlagKey.kt Addition

```kotlin
/**
 * Data object holding the feature flag key for the Enable TOTP Export feature.
 */
data object EnableTotpExport : FlagKey<Boolean>() {
    override val keyName: String = "pm-12345-enable-totp-export"
    override val defaultValue: Boolean = false
}
```

Add to activePasswordManagerFlags:
```kotlin
val activePasswordManagerFlags: List<FlagKey<*>> by lazy {
    listOf(
        CipherKeyEncryption,
        CredentialExchangeProtocolExport,
        CredentialExchangeProtocolImport,
        EnableTotpExport,  // New addition
        ForceUpdateKdfSettings,
        NoLogoutOnKdfChange,
    )
}
```

### FlagKeyTest.kt Addition

```kotlin
@Test
fun `Feature flags have the correct key name set`() {
    // ... existing assertions ...
    assertEquals(
        FlagKey.EnableTotpExport.keyName,
        "pm-12345-enable-totp-export",
    )
}

@Test
fun `All feature flags have the correct default value set`() {
    assertTrue(
        listOf(
            // ... existing flags ...
            FlagKey.EnableTotpExport,
        ).all {
            !it.defaultValue
        },
    )
}
```

### FeatureFlagListItems.kt Addition

```kotlin
FlagKey.DummyBoolean,
FlagKey.BitwardenAuthenticationEnabled,
FlagKey.CipherKeyEncryption,
FlagKey.CredentialExchangeProtocolExport,
FlagKey.CredentialExchangeProtocolImport,
FlagKey.EnableTotpExport,  // New addition
FlagKey.ForceUpdateKdfSettings,
FlagKey.NoLogoutOnKdfChange,
    -> {
    // ... existing BooleanFlagItem code ...
}
```

```kotlin
@Composable
private fun <T : Any> FlagKey<T>.getDisplayLabel(): String = when (this) {
    // ... existing cases ...
    FlagKey.EnableTotpExport -> stringResource(BitwardenString.enable_totp_export)
}
```

### strings_non_localized.xml Addition

```xml
<!-- region Debug Menu -->
<string name="avoid_logout_on_kdf_change">Avoid logout on KDF change</string>
<string name="bitwarden_authentication_enabled">Bitwarden authentication enabled</string>
<string name="cipher_key_encryption">Cipher Key Encryption</string>
<string name="enable_totp_export">Enable TOTP Export</string>
<string name="force_update_kdf_settings">Force update KDF settings</string>
<!-- endregion Debug Menu -->
```