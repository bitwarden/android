# End-to-end UI tests

Instrumented tests that drive the real app against a real self-hosted server, with test data
created by the [SeederApi](https://github.com/bitwarden/server/tree/main/util/SeederApi). They
live in `kotlin/com/x8bit/bitwarden/e2e/`:

- `screens/`: one screen object per app screen, with intent-level methods.
- `seeder/`: a minimal SeederApi client and the data it seeds.
- `BaseE2eTest`: launches the app, seeds data, and deletes it after each test.

These are a proof of concept (QA-1407). Test method names carry the Testmo case ID.

## Requirements

- JDK 21 and a `user.properties` with `gitHubToken` (see the root README).
- An emulator or device running the **standard** flavor's debug build. The F-Droid flavor skips
  the accessibility disclosure these tests expect.
- A self-hosted server reachable over https, with a SeederApi writing to the same database.
  Servers on a local network also trigger Android's local network access screen, which the tests
  don't handle.

## Running

> **Warning:** every test starts by clearing `com.x8bit.bitwarden.dev`'s data. Don't run these on a
> device whose debug install you want to keep.

The tests read three instrumentation arguments:

| Argument | Value |
|---|---|
| `BW_TEST_SERVER_URL` | Base URL of the server under test (https; production hosts are refused) |
| `BW_SEEDER_URL` | SeederApi base URL (https) |
| `BW_SEEDER_TOKEN` | SeederApi Basic credential: base64 of `username:password` |

Locally, the simplest way is the run script. Export the three values as environment variables, then:

```shell
./scripts/run-e2e-tests.sh                        # every e2e test
./scripts/run-e2e-tests.sh --test VaultLoginTest  # one class (or Class#method)
```

It reinstalls the app and the tests, then runs each test in its own `am instrument` call after
`pm clear`. `--serial` picks a device when more than one is connected.

To run a single test by hand, use `am instrument` so the token is never stored by Gradle (Gradle
records `-Pandroid.testInstrumentationRunnerArguments.*` values in its execution history):

```shell
./gradlew :app:installStandardDebug :app:installStandardDebugAndroidTest
adb shell pm clear com.x8bit.bitwarden.dev
adb shell am instrument -w \
  -e class com.x8bit.bitwarden.e2e.VaultLoginTest#test_22279_readUsernameAndPassword \
  -e BW_TEST_SERVER_URL <server url> \
  -e BW_SEEDER_URL <seeder url> \
  -e BW_SEEDER_TOKEN <token> \
  com.x8bit.bitwarden.dev.test/androidx.test.runner.AndroidJUnitRunner
```

`./gradlew :app:connectedStandardDebugAndroidTest` with the same arguments also works and uses the
orchestrator, but use it only on ephemeral machines (CI) for the reason above. Plain
`connectedAndroidTest` also runs the F-Droid variant.

## CI

`.github/workflows/test-device.yml` runs the tests with `connectedStandardDebugAndroidTest` on an
emulator, on manual dispatch and on weekdays at 05:00 UTC. Scheduled runs are skipped until the
repository variable `E2E_SCHEDULE_ENABLED` is `true`. The workflow needs the `E2E_TEST_SERVER_URL`
and `E2E_SEEDER_URL` secrets and the `seederapi-sh-ci-username` / `seederapi-sh-ci-password`
entries in the `gh-org-bitwarden` key vault (BRE-2399), so it can't pass until CI can read them.

The debug app logs HTTP bodies, and saved test outputs
(`app/build/outputs/androidTest-results/`) include logcat with the seeded account's tokens. Don't
publish them.
