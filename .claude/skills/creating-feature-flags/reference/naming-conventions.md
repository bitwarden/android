# Feature Flag Naming Conventions

Comprehensive naming rules and conversion patterns for Bitwarden Android feature flags.

## Overview

Feature flags require three naming formats derived from the user-provided flag name:

1. **keyName** (kebab-case) - Used in FlagKey.kt for the string identifier
2. **DataClassName** (PascalCase) - Used for the Kotlin data object name
3. **string_resource_key** (snake_case) - Used in strings_non_localized.xml

## keyName Generation (kebab-case)

**Input:** User-provided flag name (may include JIRA ticket)

**Rules:**
- Convert to lowercase
- Replace spaces with hyphens
- Remove special characters except hyphens
- Include JIRA ticket prefix if present
- Final format: `{ticket}-{feature-description}` or `{feature-description}`

**Examples:**
```
"Enable TOTP Export"               → "enable-totp-export"
"PM-12345 Enable TOTP Export"      → "pm-12345-enable-totp-export"
"Cipher Key Encryption"            → "cipher-key-encryption"
"PM-18021 Force Update KDF"        → "pm-18021-force-update-kdf"
"PROJ-456 New Feature"             → "proj-456-new-feature"
```

**JIRA Ticket Detection:**
- **Pattern:** `[A-Z]{2,4}-\d+` (2-4 uppercase letters, hyphen, one or more digits)
- Extract ticket and include as lowercase prefix in keyName
- Examples: PM-1234, PS-99, PROJ-5678, AB-1

**Validation:**
- Must be all lowercase
- Must use hyphens (not underscores, camelCase, or spaces)
- Should be descriptive and meaningful
- Typically 2-6 words
- Check uniqueness against existing flags in FlagKey.kt

## DataClassName Generation (PascalCase)

**Input:** Generated keyName

**Rules:**
1. Remove JIRA ticket prefix (matches `[a-z]{2,4}-\d+-`)
2. Split on hyphens
3. Capitalize first letter of each word
4. Join without separators
5. Result is valid Kotlin identifier

**Examples:**
```
"enable-totp-export"                    → "EnableTotpExport"
"pm-12345-enable-totp-export"           → "EnableTotpExport"
"cipher-key-encryption"                 → "CipherKeyEncryption"
"proj-456-new-feature"                  → "NewFeature"
```

**Special Cases:**
- Acronyms: Capitalize each letter if widely recognized
  - `"kdf"` → `"Kdf"` (follow Kotlin naming conventions)
  - `"totp"` → `"Totp"`
  - `"api"` → `"Api"`

**Validation:**
- Must start with uppercase letter
- Must be valid Kotlin identifier
- Check uniqueness against existing FlagKey data objects

## string_resource_key Generation (snake_case)

**Input:** Generated keyName

**Rules:**
1. Remove JIRA ticket prefix (matches `[a-z]{2,4}-\d+-`)
2. Replace hyphens with underscores
3. Keep lowercase
4. Result is valid XML resource name

**Examples:**
```
"enable-totp-export"                  → "enable_totp_export"
"pm-12345-enable-totp-export"         → "enable_totp_export"
"cipher-key-encryption"               → "cipher_key_encryption"
"proj-456-new-feature"                → "new_feature"
```

**Validation:**
- Must be all lowercase
- Must use underscores
- Must be valid XML resource name
- Check uniqueness in strings_non_localized.xml

## Name Generation Algorithm

**Complete conversion process:**

```
User Input: "PM-12345 Enable Password History Export"

Step 1: Generate keyName (kebab-case)
├─ Detect JIRA: "PM-12345" (matches [A-Z]{2,4}-\d+)
├─ Extract feature: "Enable Password History Export"
├─ Convert to lowercase: "enable password history export"
├─ Replace spaces with hyphens: "enable-password-history-export"
├─ Combine: "pm-12345" + "-" + "enable-password-history-export"
└─ Result: "pm-12345-enable-password-history-export"

Step 2: Generate DataClassName (PascalCase)
├─ Remove JIRA prefix: "enable-password-history-export"
├─ Split on hyphens: ["enable", "password", "history", "export"]
├─ Capitalize each: ["Enable", "Password", "History", "Export"]
└─ Result: "EnablePasswordHistoryExport"

Step 3: Generate string_resource_key (snake_case)
├─ Remove JIRA prefix: "enable-password-history-export"
├─ Replace hyphens with underscores: "enable_password_history_export"
└─ Result: "enable_password_history_export"
```

## Reference Examples from Codebase

**Existing feature flags:**

| keyName | DataClassName | string_resource_key |
|---------|---------------|---------------------|
| `cxp-import-mobile` | `CredentialExchangeProtocolImport` | `cxp_import` |
| `cipher-key-encryption` | `CipherKeyEncryption` | `cipher_key_encryption` |
| `bitwarden-authentication-enabled` | `BitwardenAuthenticationEnabled` | `bitwarden_authentication_enabled` |
| `pm-18021-force-update-kdf-settings` | `ForceUpdateKdfSettings` | `force_update_kdf_settings` |
| `pm-23995-no-logout-on-kdf-change` | `NoLogoutOnKdfChange` | `avoid_logout_on_kdf_change` |
