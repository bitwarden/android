# Credential Manager Test Harness

## Purpose

This standalone application serves as a test client for validating the Bitwarden Android credential provider implementation. It uses the Android CredentialManager APIs to request credential operations and verify that the `:app` module responds correctly as a credential provider.

Future iterations will introduce validation for the Android Autofill Framework.

## Features

- **Password Creation**: Test `CreatePasswordRequest` flow through CredentialManager
- **Password Retrieval**: Test `GetPasswordOption` flow through CredentialManager
- **Passkey Creation**: Test `CreatePublicKeyCredentialRequest` flow through CredentialManager

## Requirements

- Android device or emulator running API 28+
- Bitwarden app (`:app`) installed and configured as a credential provider
- Google Play Services (for API 28-33 compatibility)

## Usage

### Setup

1. Build and install the main Bitwarden app (`:app`)
2. Configure Bitwarden as a credential provider in system settings:
   - Settings → Passwords & accounts → Credential Manager
   - Enable Bitwarden as a provider
3. Build and install the test harness:
   ```bash
   ./gradlew :testharness:installDebug
   ```

### Running Tests

1. Launch "Credential Manager Test Harness" app
2. Select a credential operation type (Password Create, Password Get, or Passkey Create)
3. Fill in required fields:
   - **Password Create**: Username, Password, Origin (optional)
   - **Password Get**: No inputs required
   - **Passkey Create**: Username, Relying Party ID, Origin
   - **Passkey Get**: Relying Party ID, Origin
4. Tap "Execute" button
5. System credential picker should appear with Bitwarden as an option
6. Select Bitwarden and follow the flow
7. Result will be displayed in the app

## Known Limitations

1. **API 28-33 compatibility**: Requires Google Play Services for CredentialManager APIs on older Android versions.
2. Passkey operations must be performed as a Privileged App due to security measures built in `:app`.

## Architecture

This module follows the same architectural patterns as the main Bitwarden app:
- **MVVM + UDF**: `BaseViewModel` with State/Action/Event pattern
- **Hilt DI**: Dependency injection throughout
- **Compose UI**: Modern declarative UI with Material 3
- **Result handling**: No exceptions, sealed result classes

## Testing

Run unit tests:
```bash
./gradlew :testharness:test
```

### Debugging

Check Logcat for detailed error messages:
```bash
adb logcat | grep -E "CredentialManager|Bitwarden|TestHarness"
```

## References

- [Android Credential Manager Documentation](https://developer.android.com/identity/credential-manager)
- [Bitwarden Android Architecture](../docs/ARCHITECTURE.md)
- [Passkey Registration Research](../PASSKEY_REGISTRATION_RESEARCH_REPORT.md)
