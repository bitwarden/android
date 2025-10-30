# Dependency Update Review Checklist

## Inline Comment Requirement

Create separate inline comment for EACH specific issue on the exact line (`file:line_number`).
Do NOT create one large summary comment. Do NOT update existing comments.
After inline comments, provide one summary comment.

---

## Multi-Pass Strategy

### First Pass: Identify and Assess

<thinking>
Before diving into details:
1. Which dependencies were updated?
2. What are the version changes? (patch, minor, major)
3. Are any security-sensitive libraries involved? (crypto, auth, networking)
4. Any pre-release versions (alpha, beta, RC)?
5. What's the blast radius if something breaks?
</thinking>

**1. Identify the change:**
- Which library? Old version → New version?
- Major (X.0.0), Minor (0.X.0), or Patch (0.0.X) version change?
- Single dependency or multiple?

**2. Check compilation safety:**
- Any imports in codebase that might break?
- Any deprecated APIs we're currently using?
- Check if this is a breaking change version

### Second Pass: Deep Analysis

<thinking>
For each dependency update:
1. What changes are in this release?
2. Are there breaking changes?
3. Are there security fixes?
4. Do we use the affected APIs?
5. How does this affect our codebase?
</thinking>

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
🚩 **Security/crypto library** - Read `reference/android-patterns.md` and `docs/ARCHITECTURE.md#security`
🚩 **Breaking changes in release notes** - Read relevant code sections carefully
🚩 **Multiple dependency updates at once** - Check for interaction risks
🚩 **Beta/Alpha versions** - Assess stability concerns and rollback plan

If any red flags present, escalate to more comprehensive review using appropriate checklist.

## Prioritizing Findings

Use `reference/priority-framework.md` to classify findings as Critical/Important/Suggested/Optional.

## Output Format

```markdown
## Summary
Updates [library] from [old version] to [new version]

## Analysis
- **Compilation**: [✓ No breaking changes | ⚠️ Deprecations found | ❌ Breaking changes detected]
- **Security**: [✓ No concerns | ⚠️ Review release notes | 🔒 CVEs addressed]
- **Testing**: [✓ No test changes needed | ⚠️ Update test utilities | ❌ Test APIs changed]
- **Changelog**: [Brief summary of notable changes]

## Findings

List each finding with file:line reference and specific recommendation.

## Recommendation

**APPROVE** - Low-risk version bump with no concerns identified

[or]

**REQUEST CHANGES** - [specific concerns that must be addressed]:
1. [Issue 1]
2. [Issue 2]
```

## Example Reviews

### Example 1: Simple Patch Version

```markdown
## Summary
Updates androidx.credentials from 1.5.0 to 1.6.0-beta03

## Analysis
- **Compilation**: ✓ No breaking changes detected in changelog
- **Security**: ✓ No CVEs addressed
- **Testing**: ✓ No test API changes
- **Changelog**: Adds support for additional credential types, internal bug fixes

## Findings
⚠️ **Beta version** - Monitor for stability issues in production environment

## Recommendation
**APPROVE** - Low-risk minor version bump. Beta status is noted but no blocking concerns.
```

### Example 2: Major Version with Breaking Changes

```markdown
## Summary
Updates Retrofit from 2.9.0 to 3.0.0

## Analysis
- **Compilation**: ❌ Breaking changes in API
- **Security**: ✓ No security issues
- **Testing**: ⚠️ Test utilities may need updates
- **Changelog**: Major rewrite, new Kotlin coroutines API, removed deprecated methods

## Findings

**network/api/BitwardenApiService.kt:multiple** - Uses deprecated `Call<T>` return type
Migration required: Replace `Call<T>` with `suspend fun` returning `Response<T>`
Reference: https://github.com/square/retrofit/blob/master/CHANGELOG.md#version-300

**network/api/VaultApiService.kt:multiple** - Same issue as above

## Recommendation
**REQUEST CHANGES** - Major version requires code migration:
1. Update all API service interfaces to use suspend functions
2. Update call sites to use coroutines instead of enqueue/execute
3. Update tests to handle new suspend function APIs
4. Consider creating a separate PR for this migration due to scope
```
