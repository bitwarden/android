# Creating Feature Flags Skill

Automates the creation of feature flags for Bitwarden Android, following established patterns and best practices.

## What It Does

This skill guides you through creating a new feature flag by:

1. **Gathering requirements** - Asks questions about flag name, type, target application, and display label
2. **Generating names** - Automatically creates kebab-case, PascalCase, and snake_case variants
3. **Validating uniqueness** - Checks for naming conflicts before making changes
4. **Updating files** - Modifies all required files following established patterns:
   - `FlagKey.kt` - Adds sealed class data object and registers in active flags list
   - `FlagKeyTest.kt` - Adds test coverage for key name and default value
   - `FeatureFlagListItems.kt` - Integrates UI rendering in debug menu
   - `strings_non_localized.xml` - Adds display label string resource
5. **Running verification** - Executes tests and compiles affected modules
6. **Providing summary** - Shows what was changed and next steps

## When to Use

Use this skill when you need to:

- Add a new feature flag to Password Manager or Authenticator
- Implement flag-controlled features
- Update the feature flag system

## How to Use

Simply invoke the skill and answer the questions:

```
@creating-feature-flags
```

The skill will ask you:

1. **Flag Name** - Descriptive name with optional JIRA ticket (e.g., "PM-12345 Enable TOTP Export")
2. **Flag Type** - Boolean (most common), Int, or String
3. **Application Target** - Password Manager, Authenticator, or both
4. **Display Label** - Human-readable text for debug menu (e.g., "Enable TOTP Export")
5. **Default Value** - Usually `false` for Boolean flags

## What You Get

After completion, you'll have:

- ✓ Fully integrated feature flag in all required files
- ✓ Passing tests
- ✓ Compiled modules
- ✓ Debug menu integration (for Boolean flags)
- ✓ Ready-to-commit changes

## Prerequisites

- Working in the Bitwarden Android repository
- Feature flag already created in LaunchDarkly (with the kebab-case key name)
- Clean working directory recommended

## Supported Flag Types

| Type | Use Case | Example |
|------|----------|---------|
| **Boolean** | Enable/disable features | `false` → feature disabled |
| **Int** | Numeric thresholds, counts | `3` → max retry count |
| **String** | Configuration values | `"api.bitwarden.com"` → endpoint URL |

**Note:** Int and String flags require custom UI components for debug menu display. The skill will create the flag structure, but UI integration must be implemented separately.

## Example Session

```
You: @creating-feature-flags

Claude: I'll help you create a new feature flag. Let me gather the necessary information:

1. What is the feature flag name? (Include JIRA ticket if applicable)
2. What type should this feature flag be? (Boolean/Int/String)
3. Which application(s) should this flag target?
4. What should the debug menu display label be?

You: pm-20558-migrate-myvault-to-myitems, Boolean, Password Manager, Migrate My Vault to My Items

Claude: [Proceeds to create the flag, showing progress and results]
```

## After Completion

1. **Review changes**: `git diff`
2. **Test in debug menu**: Build and run the app
3. **Use in code**:
   ```kotlin
   featureFlagManager.getFeatureFlagFlow(FlagKey.YourFlagName)
       .onEach { isEnabled ->
           if (isEnabled) {
               // Your feature implementation
           }
       }
       .launchIn(viewModelScope)
   ```
4. **Commit and create PR** when ready