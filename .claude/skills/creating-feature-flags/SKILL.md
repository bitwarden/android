---
name: creating-feature-flags
version: 1.0.0
description: Automates feature flag creation for Bitwarden Android. **Use when adding new feature flags, implementing flag-controlled features, or updating the flag system.** Creates FlagKey definitions, updates UI components, generates tests, and adds string resources. Validates uniqueness and naming conventions. Supports Boolean, Int, and String flag types for both Password Manager and Authenticator applications.
---

# Creating Feature Flags

Complete automation for adding feature flags to Bitwarden Android, following established patterns
from PR #6235.

## Overview

Feature flags in Bitwarden Android follow a consistent four-file pattern:

1. **FlagKey.kt** - Sealed class definition with key name and default value
2. **FlagKeyTest.kt** - Test coverage for key name and default value
3. **FeatureFlagListItems.kt** - UI rendering in debug menu
4. **strings_non_localized.xml** - Display label string resource

**File Locations:**

```
core/src/main/kotlin/com/bitwarden/core/data/manager/model/FlagKey.kt
core/src/test/kotlin/com/bitwarden/core/data/manager/model/FlagKeyTest.kt
ui/src/main/kotlin/com/bitwarden/ui/platform/components/debug/FeatureFlagListItems.kt
ui/src/main/res/values/strings_non_localized.xml
```

## Instructions

### Step 1: Gather Feature Flag Information

<thinking>
Information needed to create a feature flag:
1. What is the feature flag name? (user-provided, may include JIRA ticket)
2. What type? (Boolean/Int/String)
3. Which application(s)? (Password Manager/Authenticator/Both)
4. What's the display label for debug menu?
5. What's the default value?
</thinking>

**Collect core information from the user:**

Ask the following questions in your response to gather the necessary information:

1. **Flag Name**: "What is the feature flag name? (Include JIRA ticket if applicable, e.g., 'PM-12345 Enable TOTP Export' or 'Cipher Key Encryption'. This will be converted to kebab-case.)"

2. **Flag Type**: "What type should this feature flag be? (Boolean - most common for enable/disable features; Int - for numeric thresholds; String - for configuration values)"

3. **Application Target**: "Which application(s) should this flag target? (Password Manager - activePasswordManagerFlags; Authenticator - activeAuthenticatorFlags; or Both)"

4. **Display Label**: "What should the debug menu display label be? (Short, human-readable label like 'Enable TOTP Export')"

**Determine default value:**

- Boolean: Standard is `false`. Ask if user wants `true` instead.
- Int/String: Ask for specific default value.

### Step 2: Generate and Validate Names

<thinking>
Name generation and validation:
1. Extract JIRA ticket (pattern: [A-Z]{2,4}-\d+)
2. Generate keyName (kebab-case)
3. Generate DataClassName (PascalCase)
4. Generate string_resource_key (snake_case)
5. Validate uniqueness by reading FlagKey.kt
6. Check format compliance
</thinking>

**Read `reference/naming-conventions.md`** for detailed naming rules.

**Generate three name formats:**

1. **keyName** (kebab-case):
    - Lowercase, hyphens between words
    - Include JIRA ticket if present: `{ticket}-{feature-description}`
    - Example: `"PM-12345 Enable TOTP"` → `"pm-12345-enable-totp"`

2. **DataClassName** (PascalCase):
    - Remove JIRA prefix, capitalize each word
    - Example: `"pm-12345-enable-totp"` → `"EnableTotp"`

3. **string_resource_key** (snake_case):
    - Remove JIRA prefix, replace hyphens with underscores
    - Example: `"pm-12345-enable-totp"` → `"enable_totp"`

**Validate uniqueness:**

- Read FlagKey.kt and search for existing keyNames
- If duplicate found, inform user and suggest alternative
- Verify format compliance (kebab-case, PascalCase, snake_case)

**Display generated names for confirmation:**

```
Generated names:
- keyName: {key-name}
- Data Class: {DataClassName}
- String Resource: {string_resource_key}

Proceeding with these names.
```

### Step 3: Modify FlagKey.kt

<thinking>
FlagKey.kt modifications:
1. Where to insert data object? (before //region Dummy keys)
2. Which active flags list? (based on application target)
3. What template to use? (based on type: Boolean/Int/String)
</thinking>

**Read `reference/file-templates.md`** for complete code templates.

**Location:** `core/src/main/kotlin/com/bitwarden/core/data/manager/model/FlagKey.kt`

**Actions:**

1. **Add data object** before `//region Dummy keys for testing` comment
2. **Add to active flags list(s)**

**Use Edit tool** to make both modifications.

### Step 4: Modify FlagKeyTest.kt

<thinking>
Test modifications required:
1. Add keyName assertion to first test
2. Add default value assertion to second test (or create new test)
3. Pattern depends on type and default value
</thinking>

**Read `reference/file-templates.md`** for test templates.

**Location:** `core/src/test/kotlin/com/bitwarden/core/data/manager/model/FlagKeyTest.kt`

**Actions:**

1. **Add keyName assertion** to test: `Feature flags have the correct key name set`
2. **Add default value test:**
    - Boolean false: Add to existing list
    - Boolean true / Int / String: Create separate test assertion

**Use Edit tool** to make modifications.

### Step 5: Modify FeatureFlagListItems.kt

<thinking>
UI modifications:
1. Add to ListItemContent when expression (by type)
2. Add to getDisplayLabel when expression
3. Int/String flags need special handling (UI components don't exist yet)
</thinking>

**Read `reference/file-templates.md`** for UI templates.

**Location:**
`ui/src/main/kotlin/com/bitwarden/ui/platform/components/debug/FeatureFlagListItems.kt`

**Actions:**

1. **Update ListItemContent when expression:**
    - Boolean: Add to existing Boolean block
    - Int/String: Inform user that UI component needs implementation, add TODO comment

2. **Update getDisplayLabel when expression:**
    - Add mapping to string resource

**Use Edit tool** to make modifications.

**Note:** If Int/String flag, inform user:

```
⚠️  Int/String Flag UI Component Required

The codebase only implements BooleanFlagItem. Int/String flags work but won't
appear in debug menu until custom UI component is created.

Options:
a) Skip UI integration (flag functional but not in debug menu)
b) Pause to implement UI component first

Proceeding with option (a) and adding TODO comment.
```

### Step 6: Modify strings_non_localized.xml

<thinking>
String resource addition:
1. Find Debug Menu region
2. Add string resource
</thinking>

**Read `reference/file-templates.md`** for string resource template.

**Location:** `ui/src/main/res/values/strings_non_localized.xml`

**Actions:**

1. **Add string resource** within `<!-- region Debug Menu -->` section

**Use Edit tool** to insert the string resource.

### Step 7: Run Tests and Verify

<thinking>
Verification steps:
1. Run FlagKeyTest to verify test changes
2. Compile all affected modules to catch errors
3. Check for any compilation or test failures
4. If failures, consult troubleshooting guide
</thinking>

**Execute verification commands:**

```bash
# Test the new flag
./gradlew :core:testDebug --tests "com.bitwarden.core.data.manager.model.FlagKeyTest"

# Verify compilation
./gradlew :core:compileDebugKotlin :ui:compileDebugKotlin :app:compileStandardDebugKotlin
```

**If tests or compilation fail:**

- **Read `reference/troubleshooting.md`** for common issues and solutions
- Review error messages carefully
- Check for typos, missing commas, or syntax issues
- Fix issues and re-run verification

**Use Bash tool** to execute commands.

### Step 8: Provide Completion Summary

**Generate comprehensive summary:**

```markdown
## ✓ Feature Flag Creation Complete

**Flag Details:**

- Data Class: {DataClassName}
- Key Name: {keyName}
- Type: {Type}
- Default: {defaultValue}
- Application: {Password Manager / Authenticator / Both}
- Display Label: {DisplayLabel}

**Files Modified:**

1. ✓ FlagKey.kt
2. ✓ FlagKeyTest.kt
3. ✓ FeatureFlagListItems.kt
4. ✓ strings_non_localized.xml

**Verification:**
✓ Tests passed
✓ Modules compiled successfully

**Next Steps:**

1. Review changes: `git diff`
2. Test in debug menu (build and run app)
3. Use in code:
   \```kotlin
   featureFlagManager.getFeatureFlagFlow(FlagKey.{DataClassName})
       .onEach { isEnabled -> /* ... */ }
       .launchIn(viewModelScope)
   \```
4. Commit and create PR

**Reference:** PR #6235 - https://github.com/bitwarden/android/pull/6235
```

If Int/String flag, add note about missing UI component.

## Additional Resources

<thinking>
When to load additional resources:
1. Need naming examples? → reference/naming-conventions.md
2. Need code templates? → reference/file-templates.md
3. Encountering errors? → reference/troubleshooting.md
4. Want to see examples? → examples/scenarios.md
</thinking>

**Load on-demand when needed:**

- **Naming rules and examples** → `reference/naming-conventions.md`
- **Complete code templates** → `reference/file-templates.md`
- **Common issues and solutions** → `reference/troubleshooting.md`
- **Real-world examples** → `examples/scenarios.md`

## Validation Checklist

Before completing, verify:

- [ ] keyName is unique and follows kebab-case
- [ ] DataClassName is unique and follows PascalCase
- [ ] string_resource_key is unique and follows snake_case
- [ ] Data object added to FlagKey.kt before dummy keys region
- [ ] Flag added to appropriate active flags list(s)
- [ ] Test assertions added (keyName + defaultValue)
- [ ] Both when expressions updated in FeatureFlagListItems.kt
- [ ] String resource added in Debug Menu region
- [ ] All tests pass
- [ ] All modules compile successfully

## Anti-Patterns to Avoid

**DO NOT:**

- Skip validation checks for uniqueness
- Use wrong case format (camelCase for keyName, snake_case for DataClassName, etc.)
- Forget to add flag to active flags list
- Skip test modifications
- Forget commas in lists
- Reorder existing entries (adds unnecessary PR noise)
- Skip running tests before completing

## Success Criteria

A successful feature flag creation:

- ✓ Passes all tests
- ✓ Compiles without errors
- ✓ Follows all naming conventions
- ✓ Appears in debug menu (Boolean flags only)
- ✓ Accessible via FeatureFlagManager
- ✓ Matches established patterns

## Reference

**Supporting Files:**

- `reference/naming-conventions.md` - Complete naming rules and examples
- `reference/file-templates.md` - Code templates for all file types
- `reference/troubleshooting.md` - Common issues and solutions
- `examples/scenarios.md` - Real-world usage examples

**Architecture:** Feature flags use sealed classes for type safety, load from remote config at
startup, flow reactively through FeatureFlagManager, and support local override in debug menu.
