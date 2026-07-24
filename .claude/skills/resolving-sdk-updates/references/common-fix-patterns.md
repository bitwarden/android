# Common Fix Patterns for SDK Updates

Quick-reference for resolving build failures after `com.bitwarden:sdk-android` version bumps.

---

## Error Categories and Fix Strategies

### 1. Exhaustive `when` Expression

**Compiler message**: `'when' expression must be exhaustive, add necessary 'X' branch or 'else' branch instead`

**Fix strategy**:
1. Identify the sealed class/enum that gained a new variant from the SDK diff
2. Search for ALL `when` usages of that type across the codebase:
   ```bash
   grep -rn "is ${TypeName}\." --include="*.kt" app/src/main/kotlin/
   grep -rn "when.*${TypeName}" --include="*.kt" app/src/main/kotlin/
   ```
3. Add the new branch to each `when` expression
4. Determine handling from SDK PR context — does the new variant map to existing behavior or need new logic?

**Template**:
```kotlin
// Before (compiler error)
when (value) {
    is SealedType.VariantA -> handleA()
    is SealedType.VariantB -> handleB()
}

// After (new variant added)
when (value) {
    is SealedType.VariantA -> handleA()
    is SealedType.VariantB -> handleB()
    is SealedType.VariantC -> handleC() // Added in SDK vX.Y.Z
}
```

---

### 2. Removed API

**Compiler message**: `Unresolved reference: '<functionOrPropertyName>'`

**Fix strategy**:
1. Check SDK diff for what replaced the removed API:
   ```bash
   gh pr diff <N> --repo bitwarden/sdk-internal | grep -A5 -B5 "<removedName>"
   ```
2. Look for deprecation annotations in previous SDK version
3. Update call sites to use the replacement API

**Template**:
```kotlin
// Before (removed API)
sdkClient.oldMethodName(params)

// After (replacement from SDK changelog)
sdkClient.newMethodName(updatedParams)
```

---

### 3. Renamed Type

**Compiler message**: `Unresolved reference: '<TypeName>'`

**Fix strategy**:
1. Search SDK diff for the old type name to find the rename:
   ```bash
   gh pr diff <N> --repo bitwarden/sdk-internal | grep "<OldTypeName>"
   ```
2. Update all imports and usages:
   ```bash
   grep -rn "import.*<OldTypeName>" --include="*.kt" app/src/main/kotlin/
   grep -rn "<OldTypeName>" --include="*.kt" app/src/main/kotlin/
   ```
3. Use find-and-replace across affected files

---

### 4. New Required Parameter

**Compiler message**: `No value passed for parameter '<paramName>'`

**Fix strategy**:
1. Check SDK diff for parameter semantics and default behavior:
   ```bash
   gh pr diff <N> --repo bitwarden/sdk-internal | grep -A10 "fun.*<functionName>"
   ```
2. Determine appropriate value from SDK PR description
3. If parameter has a logical default for Android, use it; otherwise surface to user

**Template**:
```kotlin
// Before (missing parameter)
sdkClient.initializeCrypto(
    method = method,
)

// After (new required parameter added)
sdkClient.initializeCrypto(
    method = method,
    newParam = appropriateValue, // Added in SDK vX.Y.Z — see PM-XXXXX
)
```

---

### 5. New Error Type

**Compiler message**: `'when' expression must be exhaustive` on Result/Error sealed class

**Fix strategy**:
1. Identify the new error variant from SDK diff
2. Determine if it maps to an existing error handling path or needs new UX
3. Search for all catch/when sites handling the parent error type:
   ```bash
   grep -rn "is.*Error\." --include="*.kt" app/src/main/kotlin/
   ```
4. Add handling — often maps to existing generic error display

**Template**:
```kotlin
// Error handling with new variant
when (error) {
    is SdkError.Network -> showNetworkError()
    is SdkError.Auth -> showAuthError()
    is SdkError.NewErrorType -> {
        // Map to appropriate existing handler or add new handling
        showGenericError(error.message)
    }
}
```

---

## Useful Commands Reference

### CI Error Extraction
```bash
# Get failing run ID for a branch
gh run list --branch <branch> --status failure --limit 1 --json databaseId -q '.[0].databaseId'

# Extract compiler errors
gh run view <run-id> --log-failed | grep -E "e: |error:" | head -50
```

### SDK Diff Investigation
```bash
# View sdk-internal PR details
gh pr view <N> --repo bitwarden/sdk-internal --json title,body,files

# Read full diff
gh pr diff <N> --repo bitwarden/sdk-internal

# Search for specific changes in diff
gh pr diff <N> --repo bitwarden/sdk-internal | grep -B5 -A10 "<searchTerm>"
```

### Codebase Impact Search
```bash
# Find all usages of a type
grep -rn "<TypeName>" --include="*.kt" app/src/main/kotlin/

# Find all when expressions over a sealed type
grep -rn "is <TypeName>\." --include="*.kt" app/src/main/kotlin/

# Find imports of SDK types
grep -rn "import com.bitwarden.sdk.*<TypeName>" --include="*.kt" app/src/main/kotlin/
```

### Jira Ticket Search (via bitwarden-atlassian-tools MCP)
```
# Search for Android tickets related to an SDK change
project = PM AND text ~ "<keyword>" AND text ~ "Android"

# Find linked issues from an SDK ticket
issue in linkedIssues(PM-XXXXX)

# Check ticket status
project = PM AND key = PM-XXXXX
```

### Build Verification
```bash
# Compile check (fastest feedback)
./gradlew app:compileStandardDebugKotlin

# Run specific test class
./gradlew app:testStandardDebugUnitTest --tests "*.AffectedClassTest"

# Full test suite
./gradlew app:testStandardDebugUnitTest
```