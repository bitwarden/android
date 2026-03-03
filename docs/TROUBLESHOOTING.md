# Troubleshooting

## Common Issues

### Build fails with SDK dependency error

**Problem**: Cannot resolve Bitwarden SDK from GitHub Packages

**Solution**:
1. Ensure `GITHUB_TOKEN` is set in `user.properties` or environment
2. Verify token has `read:packages` scope
3. Check network connectivity to `maven.pkg.github.com`

### Tests fail with dispatcher issues

**Problem**: Tests hang or fail with "Module with Main dispatcher had failed to initialize"

**Solution**:
1. Extend `BaseViewModelTest` for ViewModel tests
2. Use `@RegisterExtension val mainDispatcherExtension = MainDispatcherExtension()`
3. Ensure `runTest { }` wraps test body

### Compose preview not rendering

**Problem**: @Preview functions show "Rendering problem"

**Solution**:
1. Check for missing theme wrapper: `BitwardenTheme { YourComposable() }`
2. Verify no ViewModel dependency in preview (use state-based preview)
3. Clean and rebuild project

### ProGuard/R8 stripping required classes

**Problem**: Release build crashes with missing class errors

**Solution**:
1. Add keep rules to `proguard-rules.pro`
2. Check `consumer-rules.pro` in library modules
3. Verify kotlinx.serialization rules are present

### App module test flavor errors

**Problem**: `./gradlew app:testDebugUnitTest` fails or runs no tests

**Solution**: The app module uses build flavors. Use `testStandardDebugUnitTest`:
```bash
./gradlew app:testStandardDebugUnitTest
```

## Debug Tips

- **Timber Logging**: Enabled in debug builds, check Logcat with tag filter
- **Debug Menu**: Available in debug builds via Settings > About > Debug Menu
- **Network Inspector**: Use Android Studio Network Profiler or Charles Proxy
- **SDK Debugging**: Check `BaseSdkSource` for wrapped exceptions
