# Bitwarden Android

## Contents

- [Compatibility](#compatibility)
- [Setup](#setup)
- [Dependencies](#dependencies)

## Compatibility

- **Minimum SDK**: 28
- **Target SDK**: 34
- **Device Types Supported**: Phone and Tablet
- **Orientations Supported**: Portrait and Landscape

## Setup


1. Clone the repository:

    ```sh
    $ git clone https://github.com/bitwarden/android
    ```

2. Setup the code style formatter:

    All code must follow the guidelines described in the [Code Style Guidelines document](docs/STYLE_AND_BEST_PRACTICES.md). To aid in adhering to these rules, all contributors should apply `docs/bitwarden-style.xml` as their code style scheme. In IntelliJ / Android Studio:

    - Navigate to `Preferences > Editor > Code Style`.
    - Hit the `Manage` button next to `Scheme`.
    - Select `Import`.
    - Find the `bitwarden-style.xml` file in the project's `docs/` directory.
    - Import "from" `BitwardenStyle` "to" `BitwardenStyle`.
    - Hit `Apply` and `OK` to save the changes and exit Preferences.

    Note that in some cases you may need to restart Android Studio for the changes to take effect.

    All code should be formatted before submitting a pull request. This can be done manually but it can also be helpful to create a macro with a custom keyboard binding to auto-format when saving. In Android Studio on OS X:

    - Select `Edit > Macros > Start Macro Recording`
    - Select `Code > Optimize Imports`
    - Select `Code > Reformat Code`
    - Select `File > Save All`
    - Select `Edit > Macros > Stop Macro Recording`

    This can then be mapped to a set of keys by navigating to `Android Studio > Preferences` and editing the macro under `Keymap` (ex : shift + command + s).

    Please avoid mixing formatting and logical changes in the same commit/PR. When possible, fix any large formatting issues in a separate PR before opening one to make logical changes to the same code. This helps others focus on the meaningful code changes when reviewing the code.

## Dependencies

### Application Dependencies

The following is a list of all third-party dependencies included as part of the application beyond the standard Android SDK.

- **Accompanist**
    - https://github.com/google/accompanist
    - Purpose: Supplementary Android Compose features.
    - License: Apache 2.0

- **Dagger Hilt**
    - https://github.com/google/dagger
    - Purpose: Dependency injection framework.
    - License: Apache 2.0

- **Firebase Cloud Messaging**
    - https://github.com/firebase/firebase-android-sdk
    - Purpose: Allows for push notification support. (**NOTE:** This dependency is not included in builds distributed via F-Droid.)
    - License: Apache 2.0

- **Firebase Crashlytics**
    - https://github.com/firebase/firebase-android-sdk
    - Purpose: SDK for crash and non-fatal error reporting. (**NOTE:** This dependency is not included in builds distributed via F-Droid.)
    - License: Apache 2.0

- **Glide**
    - https://github.com/bumptech/glide)
    - Purpose: Image loading and caching.
    - License: BSD, part MIT and Apache 2.0

- **Jetpack Compose**
    - https://developer.android.com/jetpack/androidx/releases/compose
    - Purpose: A Kotlin-based declarative UI framework.
    - License: Apache 2.0

- **kotlinx.coroutines**
    - https://github.com/Kotlin/kotlinx.coroutines)
    - Purpose: Kotlin coroutines library for asynchronous and reactive code.
    - License: Apache 2.0

- **kotlinx.serialization**
    - https://github.com/Kotlin/kotlinx.serialization/
    - Purpose: JSON serialization library for Kotlin.
    - License: Apache 2.0

- **kotlinx.serialization converter**
    - https://github.com/JakeWharton/retrofit2-kotlinx-serialization-converter
    - Purpose: Converter for Retrofit 2 and kotlinx.serialization.
    - License: Apache 2.0

- **Room**
    - https://developer.android.com/jetpack/androidx/releases/room
    - Purpose: A convenient SQLite-based persistence layer for Android.
    - License: Apache 2.0

- **OkHttp 3**
    - https://github.com/square/okhttp
    - Purpose: An HTTP client used by the library to intercept and log traffic.
    - License: Apache 2.0

- **Retrofit 2**
    - https://github.com/square/retrofit
    - Purpose: A networking layer interface used by the sample app.
    - License: Apache 2.0

- **zxcvbn4j**
    - https://github.com/nulab/zxcvbn4j
    - Purpose: Password strength estimation.
    - License: MIT

- **ZXing**
    - https://github.com/zxing/zxing
    - Purpose: Barcode scanning and generation.
    - License: Apache 2.0

### Development Environment Dependencies

The following is a list of additional third-party dependencies used as part of the local development environment. This includes test-related artifacts as well as tools related to code quality and linting. These are not present in the final packaged application.

- **detekt**
    - https://github.com/detekt/detekt
    - Purpose: A static code analysis tool for the Kotlin programming language.
    - License: Apache 2.0

- **JUnit 5**
    - https://github.com/junit-team/junit5
    - Purpose: Unit Testing framework for testing application code.
    - License: Eclipse Public License 2.0

- **MockK**
    - https://github.com/mockk/mockk
    - Purpose: Kotlin-friendly mocking library.
    - License: Apache 2.0

- **Robolectric**
    - https://github.com/robolectric/robolectric
    - Purpose: A unit testing framework for code directly depending on the Android framework.
    - License: MIT

- **Turbine**
    - https://github.com/cashapp/turbine
    - Purpose: A small testing library for kotlinx.coroutine's Flow.
    - License: Apache 2.0
