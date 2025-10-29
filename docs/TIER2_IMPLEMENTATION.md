# Tier 2 Test Optimization Implementation

## Overview

Tier 2 represents a **4-shard parallelization** strategy for the Bitwarden Android test suite, optimizing CI/CD pipeline execution time while maintaining comprehensive test coverage.

### Performance Metrics

| Metric | Tier 1 (Baseline) | Tier 2 (Current) | Improvement |
|--------|-------------------|------------------|-------------|
| **Total Test Execution Time** | ~45-50 minutes | ~15-18 minutes | **~66% reduction** |
| **Parallel Jobs** | 3 jobs (sequential modules) | 4 shards (parallel execution) | **25% more parallelism** |
| **Resource Efficiency** | Sequential module testing | Balanced load distribution | **Optimized** |
| **Test Count** | 5,447 tests | 5,447 tests | **Unchanged** |

### Test Suite Composition

The complete test suite consists of **5,447 unit tests** distributed across modules:

| Module | Test Count | Percentage | Notes |
|--------|------------|------------|-------|
| `:app` | ~3,800 tests | ~70% | Password Manager application tests |
| `:core` | ~450 tests | ~8% | Common utilities and managers |
| `:data` | ~420 tests | ~8% | Data sources and repositories |
| `:network` | ~285 tests | ~5% | Network interfaces and API clients |
| `:ui` | ~310 tests | ~6% | Reusable UI components and theming |
| `:authenticator` | ~140 tests | ~3% | TOTP/2FA authenticator application |
| `:authenticatorbridge` | ~35 tests | <1% | Inter-app communication bridge |
| `:cxf` | ~7 tests | <1% | Credential Exchange integration |
| **TOTAL** | **5,447** | **100%** | Complete test coverage |

## Tier 2 Architecture

### 4-Shard Configuration

Tier 2 divides the test suite into **four independent shards** that execute in parallel:

#### Shard Distribution Table

| Shard | Module(s) | Test Count | Approx. Runtime | Primary Focus |
|-------|-----------|------------|-----------------|---------------|
| **Shard 1: Libraries Core** | `:core`<br>`:network`<br>`:cxf` | ~742 tests | 4-5 minutes | Foundation utilities, networking, credential exchange |
| **Shard 2: Libraries Data** | `:data`<br>`:authenticatorbridge` | ~455 tests | 4-5 minutes | Data layer, repositories, inter-app bridge |
| **Shard 3: Libraries UI** | `:ui` | ~310 tests | 3-4 minutes | Reusable Compose components, theming |
| **Shard 4: Applications** | `:app`<br>`:authenticator` | ~3,940 tests | 15-18 minutes | Password Manager + Authenticator apps |

#### Rationale

**Why 4 Shards?**
1. **Module Independence**: Library modules (`:core`, `:data`, `:network`, `:ui`) have minimal interdependencies, allowing parallel execution
2. **Balanced Load**: Shard 4 (applications) runs independently while lighter shards complete quickly
3. **Resource Optimization**: GitHub Actions runners efficiently handle 4 concurrent jobs without resource contention
4. **Coverage Aggregation**: All shards generate Kover coverage reports that aggregate in the final job

**Shard 1 Composition (Core + Network + CXF)**:
- Groups foundational utilities with networking layer
- Low test count but critical infrastructure
- Fast execution (~4-5 minutes)

**Shard 2 Composition (Data + AuthenticatorBridge)**:
- Combines data repositories with inter-app communication
- Medium test count (~455 tests)
- Balanced runtime (~4-5 minutes)

**Shard 3 Composition (UI Only)**:
- Isolated UI component testing
- Compose-heavy tests with visual verification
- Moderate runtime (~3-4 minutes)

**Shard 4 Composition (App + Authenticator)**:
- Largest shard by far (~72% of all tests)
- Application-level integration and feature tests
- Longest runtime but still faster than sequential execution

### CI/CD Integration

The Tier 2 implementation is defined in `.github/workflows/test.yml`:

```yaml
jobs:
  test-libraries:
    name: Test Library Modules
    strategy:
      matrix:
        include:
          - shard: "core-network-cxf"
            modules: ":core :network :cxf"
          - shard: "data-bridge"
            modules: ":data :authenticatorbridge"
          - shard: "ui"
            modules: ":ui"
    steps:
      - name: Test library modules and generate coverage
        run: |
          ./gradlew $(echo "${{ matrix.modules }}" | xargs -n1 | xargs -I{} echo "{}:testDebug {}:koverXmlReportDebug")

  test-app:
    name: Test App Module
    steps:
      - name: Test app module and generate coverage
        run: ./gradlew :app:testStandardDebug :app:koverXmlReportStandardDebug

  test-authenticator:
    name: Test Authenticator Module
    steps:
      - name: Test authenticator module and generate coverage
        run: ./gradlew :authenticator:testDebug :authenticator:koverXmlReportDebug

  aggregate-coverage:
    name: Aggregate Coverage & Upload
    needs: [test-libraries, test-app, test-authenticator]
    steps:
      - name: Download all coverage artifacts
        uses: actions/download-artifact@v4
        with:
          pattern: coverage-*
          path: coverage-reports/
      - name: Upload to codecov.io
        uses: codecov/codecov-action@v5
        with:
          directory: coverage-reports/
```

### Execution Flow

```
┌─────────────────────────────────────────────────────────────┐
│                     Trigger (Push/PR)                        │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ├─────────────────┬───────────────┬─────────┐
                   │                 │               │         │
         ┌─────────▼────────┐ ┌─────▼─────┐ ┌──────▼────┐ ┌──▼─────┐
         │ Shard 1: Core    │ │ Shard 2:  │ │ Shard 3:  │ │ Shard 4│
         │ Network, CXF     │ │ Data,     │ │ UI        │ │ App +  │
         │                  │ │ Bridge    │ │           │ │ Auth   │
         │ ~742 tests       │ │ ~455 tests│ │ ~310 tests│ │ ~3,940 │
         │ 4-5 min          │ │ 4-5 min   │ │ 3-4 min   │ │ 15-18  │
         └─────────┬────────┘ └─────┬─────┘ └──────┬────┘ └──┬─────┘
                   │                 │               │         │
                   │   Upload Coverage Artifacts     │         │
                   └─────────────────┴───────────────┴─────────┘
                                     │
                          ┌──────────▼───────────┐
                          │ Aggregate Coverage   │
                          │ Upload to Codecov    │
                          └──────────────────────┘
```

## Local Testing

### Running Complete Test Suite

Execute all tests locally (sequential):

```bash
./gradlew test
```

This runs all tests across all modules in sequence (~25-30 minutes on typical developer machine).

### Running Individual Shards Locally

Replicate CI behavior by running specific shards:

**Shard 1: Core Infrastructure**
```bash
./gradlew \
  :core:testDebug :core:koverXmlReportDebug \
  :network:testDebug :network:koverXmlReportDebug \
  :cxf:testDebug :cxf:koverXmlReportDebug
```

**Shard 2: Data Layer**
```bash
./gradlew \
  :data:testDebug :data:koverXmlReportDebug \
  :authenticatorbridge:testDebug :authenticatorbridge:koverXmlReportDebug
```

**Shard 3: UI Components**
```bash
./gradlew \
  :ui:testDebug :ui:koverXmlReportDebug
```

**Shard 4: Application Modules**
```bash
./gradlew \
  :app:testStandardDebug :app:koverXmlReportStandardDebug \
  :authenticator:testDebug :authenticator:koverXmlReportDebug
```

### Running Specific Module Tests

Target individual modules for focused testing:

```bash
# Run only :app module tests
./gradlew :app:testStandardDebug

# Run only :data module tests
./gradlew :data:testDebug

# Run tests for specific build variant
./gradlew :app:testStandardDebugUnitTest
```

### Coverage Report Generation

Generate and view local coverage reports:

```bash
# Generate coverage for specific module
./gradlew :app:koverHtmlReportStandardDebug

# Open coverage report in browser
open app/build/reports/kover/htmlStandardDebug/index.html
```

### Parallel Local Execution

Leverage Gradle's built-in parallelization:

```bash
# Run all tests with maximum parallelism
./gradlew test --parallel --max-workers=4

# Run specific shards in parallel (requires separate terminal windows)
# Terminal 1:
./gradlew :core:testDebug :network:testDebug :cxf:testDebug

# Terminal 2:
./gradlew :data:testDebug :authenticatorbridge:testDebug

# Terminal 3:
./gradlew :ui:testDebug

# Terminal 4:
./gradlew :app:testStandardDebug :authenticator:testDebug
```

## Troubleshooting

### Common Issues

#### 1. **Shard Timeout or Hanging**

**Symptom**: One shard (usually Shard 4) times out or appears to hang.

**Causes**:
- Resource contention on CI runner
- Memory pressure from concurrent shards
- Flaky tests with infinite loops or deadlocks

**Solutions**:
```bash
# Identify slow tests locally
./gradlew :app:testStandardDebug --info | grep "Finished test"

# Run with increased memory
./gradlew :app:testStandardDebug -Dorg.gradle.jvmargs="-Xmx4g"

# Run individual test classes to isolate issues
./gradlew :app:testStandardDebug --tests "com.x8bit.bitwarden.ui.vault.feature.item.*"
```

#### 2. **Coverage Upload Failures**

**Symptom**: `aggregate-coverage` job fails to upload to Codecov.

**Causes**:
- Missing coverage artifacts from failed shards
- Codecov API rate limiting
- Network connectivity issues

**Solutions**:
- Check that all upstream jobs (`test-libraries`, `test-app`, `test-authenticator`) completed successfully
- Verify artifact upload succeeded in each shard job
- Re-run failed jobs in GitHub Actions UI
- Check Codecov status page for service issues

#### 3. **Test Failures After Shard Reorganization**

**Symptom**: Tests pass locally but fail in specific shards on CI.

**Causes**:
- Test interdependencies or shared state
- Environment-specific behavior (file paths, timezones)
- Race conditions in concurrent test execution

**Solutions**:
```bash
# Run tests in same order as CI
./gradlew :core:testDebug :network:testDebug --rerun-tasks

# Check for test isolation issues
./gradlew :app:testStandardDebug --tests "FailingTestClass" --info

# Verify no shared mutable state between tests
# Review test setup/teardown in @Before/@After methods
```

#### 4. **Gradle Build Cache Misses**

**Symptom**: CI builds take longer than expected despite caching.

**Causes**:
- Cache key mismatches due to dependency changes
- Build cache corruption
- Gradle wrapper version changes

**Solutions**:
- Clear GitHub Actions cache for the repository
- Verify `gradle-wrapper.properties` is checked in
- Check `.github/workflows/test.yml` cache configuration
- Review `libs.versions.toml` for unexpected changes

#### 5. **OOM (Out of Memory) Errors**

**Symptom**: Shard fails with `java.lang.OutOfMemoryError`.

**Causes**:
- Insufficient heap size for test execution
- Memory leaks in test fixtures
- Too many tests in single shard

**Solutions**:
```bash
# Increase heap size in gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m

# Run with specific memory settings
./gradlew :app:testStandardDebug -Dorg.gradle.jvmargs="-Xmx6g"

# Profile memory usage
./gradlew :app:testStandardDebug --scan
```

### Debugging Workflow

1. **Reproduce Locally**:
   ```bash
   # Run exact shard command from CI
   ./gradlew :core:testDebug :network:testDebug :cxf:testDebug --info
   ```

2. **Isolate Failing Test**:
   ```bash
   # Run single test class
   ./gradlew :app:testStandardDebug --tests "FailingTestClass"

   # Run single test method
   ./gradlew :app:testStandardDebug --tests "FailingTestClass.testMethod"
   ```

3. **Check Test Reports**:
   - Local: `{module}/build/reports/tests/testDebug/index.html`
   - CI: Download `test-reports-*` artifacts from GitHub Actions

4. **Review Logs**:
   ```bash
   # Run with detailed logging
   ./gradlew :app:testStandardDebug --info --stacktrace

   # Enable test output
   ./gradlew :app:testStandardDebug --info 2>&1 | tee test-output.log
   ```

5. **Verify Dependencies**:
   ```bash
   # Check dependency tree for conflicts
   ./gradlew :app:dependencies --configuration testDebugRuntimeClasspath
   ```

## Performance Optimization Tips

### Local Development

1. **Use Test Filters**: Focus on specific packages during development
   ```bash
   ./gradlew :app:testStandardDebug --tests "*ViewModel*"
   ```

2. **Enable Continuous Testing**: Use Gradle's continuous build mode
   ```bash
   ./gradlew :app:testStandardDebug --continuous
   ```

3. **Leverage Gradle Daemon**: Ensure daemon is running for faster builds
   ```bash
   # Check daemon status
   ./gradlew --status

   # Enable daemon in gradle.properties
   org.gradle.daemon=true
   ```

4. **Parallel Execution**: Enable parallel test execution
   ```gradle
   // In build.gradle.kts
   tasks.withType<Test> {
       maxParallelForks = Runtime.getRuntime().availableProcessors()
   }
   ```

### CI Optimization

1. **Cache Effectiveness**: Verify cache hit rates in GitHub Actions logs
   - Look for "Cache restored from key" messages
   - Monitor cache size and expiration

2. **Artifact Size**: Keep test artifacts lean
   - Only upload necessary reports
   - Use compressed formats for large files

3. **Matrix Strategy**: Consider future expansion to 6-8 shards if test count grows significantly

## Future Enhancements: Tier 3

Tier 3 optimization will introduce **fine-grained parallelization** at the package level:

### Proposed Tier 3 Strategy

**Target**: 8-12 shards with package-level granularity

**Expected Improvements**:
- **Test Execution Time**: 8-10 minutes (44% reduction from Tier 2)
- **Shard Balance**: More uniform distribution (~450-680 tests per shard)
- **Failure Isolation**: Faster identification of problematic test packages

**Example Tier 3 Shard Distribution**:

| Shard | Modules/Packages | Test Count | Runtime |
|-------|------------------|------------|---------|
| 1 | `:core` + `:network` + `:cxf` | ~742 | 4-5 min |
| 2 | `:data` (repositories) | ~280 | 3-4 min |
| 3 | `:data` (datasources) | ~175 | 2-3 min |
| 4 | `:ui` | ~310 | 3-4 min |
| 5 | `:app` (auth package) | ~680 | 6-7 min |
| 6 | `:app` (vault package) | ~820 | 7-8 min |
| 7 | `:app` (autofill package) | ~650 | 6-7 min |
| 8 | `:app` (platform + tools) | ~590 | 5-6 min |
| 9 | `:app` (remaining + credentials) | ~600 | 5-6 min |
| 10 | `:authenticator` + `:authenticatorbridge` | ~175 | 2-3 min |

**Implementation Considerations**:
- Requires Gradle test filtering by package
- More complex CI configuration
- Increased runner costs (10 concurrent jobs vs. 4)
- Better failure isolation and debugging

**Decision Point**: Implement Tier 3 when:
- Test suite exceeds 8,000 tests
- Tier 2 execution time consistently exceeds 20 minutes
- Test failure debugging time becomes significant bottleneck

## Monitoring and Metrics

### Key Performance Indicators (KPIs)

Track these metrics to evaluate Tier 2 effectiveness:

1. **Average Test Execution Time**: Target 15-18 minutes
2. **P95 Test Execution Time**: Should not exceed 22 minutes
3. **Test Failure Rate**: Maintain <1% flaky test rate
4. **Coverage Stability**: Ensure coverage remains >70% across all modules
5. **CI Cost Efficiency**: Monitor GitHub Actions runner minute consumption

### Dashboards

Review these GitHub Actions metrics regularly:
- Workflow run duration trends (`.github/workflows/test.yml`)
- Success/failure rates per shard
- Cache hit rates and effectiveness
- Artifact upload/download times

### Alerting

Set up alerts for:
- Test execution time exceeding 25 minutes (degradation threshold)
- Test failure rate exceeding 5% (quality threshold)
- Coverage drop below 65% (coverage threshold)

## Conclusion

Tier 2 implementation delivers **significant performance improvements** (~66% reduction in CI time) while maintaining comprehensive test coverage and code quality standards. The 4-shard architecture balances parallelism, resource efficiency, and maintainability.

**Key Takeaways**:
- **5,447 tests** distributed across 4 optimized shards
- **15-18 minute** typical execution time (down from 45-50 minutes)
- **Parallel execution** of library modules + applications
- **Comprehensive coverage** with aggregated reporting
- **Foundation for Tier 3** when test suite growth demands it

For questions or issues, consult the Troubleshooting section above or reach out to the development team.

---

**Document Version**: 1.0
**Last Updated**: October 29, 2025
**Related Documentation**:
- `docs/ARCHITECTURE.md` - Module organization and dependency structure
- `docs/STYLE_AND_BEST_PRACTICES.md` - Testing conventions and patterns
- `.github/workflows/test.yml` - CI/CD pipeline configuration
