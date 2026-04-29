# Dependency Update Review Checklist

## Multi-Pass Strategy

### First Pass: Identify and Assess

**1. Identify the change:**
- Which library? Old version → New version?
- Major (X.0.0), Minor (0.X.0), or Patch (0.0.X) version change?
- Single dependency or multiple?

**2. Check compilation safety:**
- Any imports in codebase that might break?
- Any deprecated APIs we're currently using?
- Check if this is a breaking change version

### Second Pass: Deep Analysis

**3. Review release notes** (if available):
- Breaking changes mentioned?
- Security fixes included?
- New features we should know about?
- Deprecations that affect our usage?

**4. Verify consistency:**
- If updating androidx library, are related libraries updated consistently?
- BOM (Bill of Materials) consistency if applicable?
- Test dependencies updated alongside main dependencies?

## What to CHECK

✅ **Compilation Safety**
- Look for API deprecations in our codebase
- Check if import statements still valid
- Major version bumps require extra scrutiny
- Beta/alpha versions need stability assessment

✅ **Security Implications** (if applicable)
- Security-related libraries (crypto, auth, networking)?
- Check for CVEs addressed in release notes
- Review security advisories for this library

✅ **Testing Implications**
- Does this affect test utilities?
- Are there breaking changes in test APIs?
- Do existing tests still cover the same scenarios?

✅ **Changelog Review**
- Read release notes for breaking changes
- Note any behavioral changes
- Check migration guides if major version

## What to SKIP

❌ **Full Architecture Review** - No code changed, patterns unchanged
❌ **Code Style Review** - No code to review
❌ **New Test Requirements** - Unless API changed significantly
❌ **Security Deep-Dive** - Unless crypto/auth/networking library
❌ **Performance Analysis** - Unless release notes mention performance changes

## Red Flags (Escalate to Full Review)

🚩 **Major version bump** (e.g., 1.x → 2.0) - Read `checklists/feature-addition.md`
🚩 **Security/crypto library** - Read `reference/architectural-patterns.md` and `docs/ARCHITECTURE.md#security`
🚩 **Breaking changes in release notes** - Read relevant code sections carefully
🚩 **Multiple dependency updates at once** - Check for interaction risks
🚩 **Beta/Alpha versions** - Assess stability concerns and rollback plan

If any red flags present, escalate to more comprehensive review using appropriate checklist.

## Prioritizing Findings

Use `reference/priority-framework.md` to classify findings as Critical/Important/Suggested/Optional.

## Output Format

See `examples/review-outputs.md` for the required output format and inline comment structure.

## Example Reviews

### Example 1: Simple Patch Version (No Critical Issues)

```markdown
**Overall Assessment:** APPROVE

See inline comments for all issue details.
```

**Inline comment example:**
```
**libs.versions.toml:45** - SUGGESTED: Beta version in production

<details>
<summary>Details</summary>

androidx.credentials updated from 1.5.0 to 1.6.0-beta03

Monitor for stability issues - beta releases may have unexpected behavior in production.

Changelog: Adds support for additional credential types, internal bug fixes.
</details>
```

### Example 2: Major Version with Breaking Changes (With Critical Issues)

```markdown
**Overall Assessment:** REQUEST CHANGES

**Critical Issues:**
- Breaking API changes in Retrofit 3.0.0 (network/api/BitwardenApiService.kt)
- Breaking API changes in Retrofit 3.0.0 (network/api/VaultApiService.kt)

See inline comments for migration details.
```

**Inline comment example:**
```
**network/api/BitwardenApiService.kt:15** - CRITICAL: Breaking API changes

<details>
<summary>Details and fix</summary>

Retrofit 3.0.0 removes `Call<T>` return type. Migration required:

\```kotlin
// Before
fun getUser(): Call<UserResponse>

// After
suspend fun getUser(): Response<UserResponse>
\```

Update all API service interfaces to use suspend functions, update call sites to use coroutines instead of enqueue/execute, and update tests accordingly.

Consider creating a separate PR for this migration due to scope.

Reference: https://github.com/square/retrofit/blob/master/CHANGELOG.md#version-300
</details>
```
