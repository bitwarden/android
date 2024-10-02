# Authenticator Bridge SDK

## Contents

- [Compatibility](#compatibility)
- [Building](#building)
- [Versioning](#versioning)
- [Dependencies](#dependencies)
 
## Other Documents

- [Changelog](CHANGELOG.md)
- [Changelog Format Guide](CHANGELOG_FORMAT.MD)

## Compatibility

- **Minimum SDK**: 28
- **Target SDK**: 34

## Building

To build an AAR for inclusion in consumer applications, run:

    ```sh
    $ ./gradlew authenticatorbridge:assembleRelease
    ```

## Versioning
This repository conforms to the following versioning convention:

**v[MAJOR].[MINOR].[PATCH]**

```
where [RELEASE]   is incremented to represent major milestones that indicate a significant change in the library.

      [MINOR]     is incremented when any standard changes (breaking or otherwise) are introduced to the library.

      [PATCH]     is incremented when a hot-fix patch is required to an existing minor
                  release due to a bug introduced in that release.
```

Some things to note:

- All updates should have a corresponding `CHANGELOG.md` entry that at a high-level describes what is being newly introduced in it. For more info, see [Changelog Format Guide](CHANGELOG_FORMAT.MD)

- When incrementing a level any lower-levels should always reset to 0.

## Dependencies

### Application Dependencies

The following is a list of all third-party dependencies required by the SDK. 

> [!IMPORTANT]
> The SDK does not come packaged with these dependencies, so consumers of the SDK must provide them.

- **AndroidX Appcompat**
  - https://developer.android.com/jetpack/androidx/releases/appcompat
  - Purpose: Allows access to new APIs on older API versions.
  - License: Apache 2.0

- **AndroidX Lifecycle**
  - https://developer.android.com/jetpack/androidx/releases/lifecycle
  - Purpose: Lifecycle aware components and tooling.
  - License: Apache 2.0

- **kotlinx.coroutines**
  - https://github.com/Kotlin/kotlinx.coroutines
  - Purpose: Kotlin coroutines library for asynchronous and reactive code.
  - License: Apache 2.0

- **kotlinx.serialization**
    - https://github.com/Kotlin/kotlinx.serialization/
    - Purpose: JSON serialization library for Kotlin.
    - License: Apache 2.0

### Development Environment Dependencies

The following is a list of additional third-party dependencies used as part of the local development environment. This includes test-related artifacts as well as tools related to code quality and linting. These are not present in the final packaged SDK.

- **JUnit 5**
  - https://github.com/junit-team/junit5
  - Purpose: Unit Testing framework for testing SDK code.
  - License: Eclipse Public License 2.0

- **MockK**
  - https://github.com/mockk/mockk
  - Purpose: Kotlin-friendly mocking library.
  - License: Apache 2.0

- **Turbine**
  - https://github.com/cashapp/turbine
  - Purpose: A small testing library for kotlinx.coroutine's Flow.
  - License: Apache 2.0