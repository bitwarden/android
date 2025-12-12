# Feature Flag Creation Troubleshooting

Common issues, error messages, and solutions when creating feature flags.

## Compilation Errors

### "Unresolved reference: {DataClassName}"

**Error Message:**
```
e: file:///.../FeatureFlagListItems.kt:XX:XX Unresolved reference: {DataClassName}
```

**Cause:** Data object not added to FlagKey.kt, or typo in data class name.

**Solution:**
1. Verify data object exists in FlagKey.kt
2. Check spelling matches exactly (case-sensitive)
3. Ensure proper PascalCase formatting
4. Clean and rebuild: `./gradlew clean :ui:compileDebugKotlin`

---

### "Unresolved reference: {string_resource_key}"

**Error Message:**
```
e: file:///.../FeatureFlagListItems.kt:XX:XX Unresolved reference: {string_resource_key}
```

**Cause:** String resource not added to strings_non_localized.xml.

**Solution:**
1. Open `ui/src/main/res/values/strings_non_localized.xml`
2. Verify string resource exists within Debug Menu region
3. Check spelling matches exactly (case-sensitive, snake_case)
4. Clean and rebuild: `./gradlew clean :ui:compileDebugKotlin`

---

### "Duplicate case label"

**Error Message:**
```
e: file:///.../FeatureFlagListItems.kt:XX:XX Duplicate case label
```

**Cause:** Flag added twice to the same when expression branch.

**Solution:**
1. Search for `FlagKey.{DataClassName}` in FeatureFlagListItems.kt
2. Remove duplicate entry
3. Keep only one instance in each when expression

---

## Test Failures

### keyName Test Failure

**Error Message:**
```
Expected: "expected-key-name"
Actual: "actual-key-name"
```

**Cause:** Mismatch between keyName in FlagKey.kt and test assertion.

**Solution:**
1. Open both FlagKey.kt and FlagKeyTest.kt
2. Compare keyName values exactly
3. Correct the mismatch (usually fix the test assertion)
4. Ensure proper kebab-case formatting

---

### Default Value Test Failure

**Error Message:**
```
Expected: false
Actual: true
```

**Cause:** Flag's defaultValue doesn't match test expectation.

**Solution:**
1. Verify defaultValue in FlagKey.kt
2. For Boolean false: ensure flag is in the list in test
3. For Boolean true: needs separate test assertion
4. For Int/String: ensure separate test with correct expected value

---

### "No such file or directory: FlagKeyTest.kt"

**Cause:** File path incorrect or file doesn't exist.

**Solution:**
- Correct path: `core/src/test/kotlin/com/bitwarden/core/data/manager/model/FlagKeyTest.kt`
- If file missing, check core module structure

---

## Runtime Errors

### Flag Doesn't Appear in Debug Menu

**Possible Causes:**
1. Flag not added to active flags list
2. UI not updated in FeatureFlagListItems.kt
3. String resource missing
4. App not recompiled after changes
5. Wrong application target - Password Manager flag won't show in Authenticator app

**Solution:**
1. Verify flag in `activePasswordManagerFlags` or `activeAuthenticatorFlags`
2. Check both when expressions in FeatureFlagListItems.kt are updated
3. Verify string resource exists
4. Clean and rebuild app: `./gradlew clean :app:assembleStandardDebug`
5. Reinstall app on device/emulator
6. Confirm testing the correct app (Password Manager vs Authenticator) matching flag's active list

---

### App Crashes When Accessing Flag

**Error Message:**
```
java.lang.IllegalStateException: Flag not found
```

**Cause:** Flag not registered in active flags list.

**Solution:**
Add flag to appropriate list in FlagKey.kt:
```kotlin
val activePasswordManagerFlags: List<FlagKey<*>> by lazy {
    listOf(
        // ... existing flags ...
        {DataClassName},  // Add this
    )
}
```

---

## Validation Issues

### "Flag with keyName already exists"

**Cause:** Attempting to create flag with duplicate keyName.

**Solution:**
1. Search FlagKey.kt for existing keyName
2. Choose a different, unique keyName
3. Consider if this is actually a duplicate feature request

---

### "Invalid keyName format"

**Cause:** keyName doesn't follow kebab-case convention.

**Common Issues:**
- Uses underscores: `enable_totp_export` ❌ → `enable-totp-export` ✓
- Uses camelCase: `enableTotpExport` ❌ → `enable-totp-export` ✓
- Not lowercase: `Enable-TOTP-Export` ❌ → `enable-totp-export` ✓
- Has spaces: `enable totp export` ❌ → `enable-totp-export` ✓

**Solution:**
Follow kebab-case: lowercase words separated by hyphens.

---

### "DataClassName already exists"

**Cause:** Generated data class name conflicts with existing flag.

**Solution:**
1. Search FlagKey.kt for existing data object names
2. Adjust flag name to generate unique DataClassName
3. Consider if features are actually related/duplicate

---

## Build Issues

### "Execution failed for task ':core:compileDebugKotlin'"

**Possible Causes:**
- Syntax error in FlagKey.kt
- Missing comma in active flags list
- Incorrect type specification
- Missing closing brace

**Solution:**
1. Check syntax around new data object
2. Verify comma after flag in active flags list
3. Ensure proper Kotlin syntax (closing braces, proper override declarations)
4. Review error output for specific line number

---

### "Cannot access class 'FlagKey'"

**Cause:** Import or module dependency issue.

**Solution:**
1. Ensure `:ui` module depends on `:core` module
2. Check imports in FeatureFlagListItems.kt
3. Clean and rebuild all modules

---

## XML Errors

### "String resource name must start with a letter"

**Cause:** string_resource_key doesn't follow XML naming rules.

**Solution:**
String resource keys must:
- Start with a letter
- Contain only `[a-z0-9_]`
- Use snake_case convention

---

### "Duplicate resource name"

**Cause:** String resource with same name already exists.

**Solution:**
1. Search strings_non_localized.xml for existing resource
2. Choose different string resource key
3. Update getDisplayLabel when expression with new key

---

## Alphabetical Ordering Issues

### CI/Review Comments About Ordering

**Issue:** Flags or strings not in alphabetical order.

**Solution:**

**Active Flags List:** Order by data class name
```kotlin
listOf(
    CipherKeyEncryption,        // C
    CredentialExchangeProtocol, // C
    EnableTotpExport,          // E
    ForceUpdateKdfSettings,     // F
)
```

**When Expressions:** Order by data class name
```kotlin
FlagKey.BitwardenAuthenticationEnabled,
FlagKey.CipherKeyEncryption,
FlagKey.EnableTotpExport,
```

**String Resources:** Order by resource name
```xml
<string name="cipher_key_encryption">Cipher Key Encryption</string>
<string name="enable_totp_export">Enable TOTP Export</string>
<string name="force_update_kdf_settings">Force update KDF settings</string>
```

---

## Int/String Flag Issues

### "BooleanFlagItem cannot be used for Int/String flags"

**Cause:** Int/String flags require custom UI components not yet implemented.

**Current Status:** Codebase only implements BooleanFlagItem for debug menu.

**Solution:**
1. For now, skip UI integration (flag will work but not appear in debug menu)
2. Add TODO comment in FeatureFlagListItems.kt
3. Implement IntFlagItem or StringFlagItem composable if needed

**Example TODO:**
```kotlin
// TODO: Add UI support for {DataClassName} ({Type} flag)
// Requires implementing {Type}FlagItem composable similar to BooleanFlagItem
```

---

## Git/Merge Issues

### Merge Conflicts in FlagKey.kt

**Cause:** Multiple branches adding flags simultaneously.

**Solution:**
1. Accept both changes
2. Maintain alphabetical ordering in active flags list
3. Ensure no duplicate data objects
4. Run tests after resolving

---

### Merge Conflicts in FlagKeyTest.kt

**Cause:** Multiple branches adding test assertions simultaneously.

**Solution:**
1. Accept both changes
2. Maintain alphabetical ordering in assertions
3. Add all flags to defaultValue test list
4. Run tests to verify

---

## Prevention Checklist

Avoid common issues by verifying:

- [ ] keyName is unique and follows kebab-case
- [ ] DataClassName is unique and follows PascalCase
- [ ] string_resource_key is unique and follows snake_case
- [ ] Flag added to appropriate active flags list
- [ ] Comma added after flag in list
- [ ] Both when expressions updated in FeatureFlagListItems.kt
- [ ] String resource added to strings_non_localized.xml within Debug Menu region
- [ ] Alphabetical ordering maintained in all locations
- [ ] Tests updated (keyName assertion and defaultValue test)
- [ ] All modules compile successfully
- [ ] Tests pass

Run this verification command before committing:
```bash
./gradlew :core:testDebug :ui:compileDebugKotlin :app:compileStandardDebugKotlin
```