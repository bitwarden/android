# Infrastructure Review Checklist

## Multi-Pass Strategy

### First Pass: Understand the Change

<thinking>
Assess infrastructure change:
1. What problem does this solve?
2. Does this affect production builds, CI/CD, or dev workflow?
3. What's the risk if this breaks?
4. Can this be tested before merge?
5. What's the rollback plan?
</thinking>

**1. Identify the goal:**
- What problem does this solve?
- Is this optimization, fix, or new capability?
- What's the expected impact?

**2. Assess risk:**
- Does this affect production builds?
- Could this break CI/CD pipelines?
- Impact on developer workflow?

**3. Performance implications:**
- Will builds be faster or slower?
- CI time impact?
- Resource usage changes?

### Second Pass: Verify Implementation

<thinking>
Verify configuration and impact:
1. Is the configuration syntax valid?
2. Are secrets/credentials handled securely?
3. What's the impact on build times and CI performance?
4. How will this affect the team's workflow?
5. Is there adequate testing/validation?
</thinking>

**4. Configuration correctness:**
- Syntax valid?
- References correct?
- Secrets/credentials handled securely?

**5. Impact analysis:**
- What workflows/builds are affected?
- Rollback plan if this breaks?
- Documentation for team?

**6. Testing strategy:**
- How can this be tested before merge?
- Canary/gradual rollout possible?
- Monitoring for issues post-merge?

## What to CHECK

✅ **Configuration Correctness**
- YAML/Groovy syntax valid
- File references correct
- Version numbers/tags valid
- Conditional logic sound

✅ **Security**
- No hardcoded secrets or credentials
- GitHub secrets used properly
- Permissions appropriately scoped
- No sensitive data in logs

✅ **Performance Impact**
- Build time implications understood
- CI queue time impact assessed
- Resource usage reasonable

✅ **Rollback Plan**
- Can this be reverted easily?
- Dependencies on other changes?
- Gradual rollout possible?

✅ **Documentation**
- Changes documented for team?
- README or CONTRIBUTING updated?
- Breaking changes clearly noted?

## What to SKIP

❌ **Bikeshedding Configuration** - Unless clear performance/maintenance benefit
❌ **Over-Optimization** - Unless current system has proven problems
❌ **Suggesting Major Rewrites** - Unless current approach is fundamentally broken

## Red Flags

🚩 **Hardcoded secrets** - Use GitHub secrets or secure storage
🚩 **No rollback plan** - Critical infrastructure should be revertible
🚩 **Untested changes** - CI changes should be validated
🚩 **Breaking changes without notice** - Team needs advance warning
🚩 **Performance regression** - Builds shouldn't get significantly slower

## Key Questions to Ask

Use `reference/review-psychology.md` for phrasing:

- "What's the rollback plan if this breaks CI?"
- "Can we test this on a feature branch before main?"
- "Will this impact build times? By how much?"
- "Should this be documented in CONTRIBUTING.md?"

## Common Infrastructure Patterns

### GitHub Actions

```yaml
# ✅ GOOD - Secure, clear, tested
name: Build and Test
on:
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 30  # Prevent runaway builds
    steps:
      - uses: actions/checkout@v4
      - name: Run tests
        env:
          API_KEY: ${{ secrets.API_KEY }}  # Secure secret usage
        run: ./gradlew test

# ❌ BAD - Insecure, unclear
name: Build
on: push  # Too broad, runs on all branches
jobs:
  build:
    runs-on: ubuntu-latest
    # No timeout - could run forever
    steps:
      - run: |
          export API_KEY="hardcoded_key_here"  # Hardcoded secret!
          ./gradlew test
```

### Gradle Configuration

```kotlin
// ✅ GOOD - Clear, maintainable
dependencies {
    implementation(libs.androidx.core.ktx)  // Version catalog
    implementation(libs.hilt.android)

    testImplementation(libs.junit5)
    testImplementation(libs.mockk)
}

// ❌ BAD - Hardcoded versions
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")  // Hardcoded version
    implementation("com.google.dagger:hilt-android:2.48")
}
```

### Build Optimization

```kotlin
// ✅ GOOD - Parallel, cached
tasks.register("checkAll") {
    dependsOn("detekt", "ktlintCheck", "testStandardDebug")
    group = "verification"
    description = "Run all checks in parallel"

    // Enable caching for faster builds
    outputs.upToDateWhen { false }
}

// ❌ BAD - Sequential, no caching
tasks.register("checkAll") {
    doLast {
        exec { commandLine("./gradlew", "detekt") }
        exec { commandLine("./gradlew", "ktlintCheck") }  // Sequential
        exec { commandLine("./gradlew", "test") }
    }
}
```

## Prioritizing Findings

Use `reference/priority-framework.md` to classify findings as Critical/Important/Suggested/Optional.

## Output Format

Follow the format guidance from `SKILL.md` Step 5 (concise summary with critical issues only, detailed inline comments with `<details>` tags).

```markdown
**Overall Assessment:** APPROVE / REQUEST CHANGES

**Critical Issues** (if any):
- [One-line summary of each critical blocking issue with file:line reference]

See inline comments for all issue details.
```

## Example Review

```markdown
## Summary
Optimizes CI build by parallelizing test execution and caching dependencies

Impact: Estimated 40% reduction in CI time (12 min → 7 min per build)

## Critical Issues
None

## Suggested Improvements

**.github/workflows/build.yml:23** - Add timeout for safety
```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 30  # Prevent builds from hanging
    steps:
      # ...
```
This prevents runaway builds if something goes wrong.

**.github/workflows/build.yml:45** - Consider matrix strategy for module tests
Can we run module tests in parallel using a matrix strategy?
```yaml
strategy:
  matrix:
    module: [app, data, network, ui]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - run: ./gradlew :${{ matrix.module }}:test
```
This could further reduce CI time.

**build.gradle.kts:12** - Document caching strategy
Can we add a comment explaining the caching configuration?
Future maintainers will appreciate understanding why these specific cache keys are used.

## Rollback Plan
If CI breaks:
- Revert commit: `git revert [commit-hash]`
- Previous workflow available at: `.github/workflows/build.yml@main^`
- Monitor CI times at: https://github.com/[org]/[repo]/actions
```
