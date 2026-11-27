# Autofill

A short overview of how autofill works in the Password Manager app: High level overview and helpful debugging steps. Paths below are relative to `app/src/main/kotlin/com/x8bit/bitwarden/data/autofill/` unless noted.

## Scope

There are two ways the app fills credentials into other apps and websites:

1. **The Android Autofill Framework** (this document's focus). The system asks our `AutofillService` for suggestions when the user focuses a field.
2. **The accessibility fallback.** For browsers and apps that do not work with the framework, the `accessibility/` package reads the screen through an `AccessibilityService`. It has its own parser and processor.

Passkeys and Credential Manager are separate features and are not covered here.

## How a fill request flows

```mermaid
flowchart TD
    A["User focuses a field in another app"] --> B["Android Autofill Framework"]
    B -->|"onFillRequest"| C["BitwardenAutofillService<br/>thin entry point"]
    C --> D["AutofillProcessor<br/>orchestrates one request"]
    D --> E["AutofillParser<br/>AssistStructure to AutofillRequest"]
    E --> F{"Fillable?"}
    F -->|"Unfillable"| G["Respond with no suggestions"]
    F -->|"Fillable"| H["FilledDataBuilder<br/>match vault items, fill views"]
    H --> I["SaveInfoBuilder<br/>can a save prompt be offered?"]
    I --> J["FillResponseBuilder<br/>build the FillResponse"]
    J --> K["Framework shows the suggestion<br/>the user picks one"]
```

Saving follows a shorter path:

```mermaid
flowchart TD
    A["Framework calls onSaveRequest"] --> B{"Save prompt enabled and<br/>no org data ownership policy?"}
    B -->|"No"| C["Finish without saving"]
    B -->|"Yes"| D["AutofillParser<br/>parse the structure again"]
    D --> E{"Fillable with<br/>something to save?"}
    E -->|"No"| C
    E -->|"Yes"| F["Open the add-item screen, prefilled"]
```

## Main components

| Component | Role |
|-----------|------|
| [`BitwardenAutofillService`](../app/src/main/kotlin/com/x8bit/bitwarden/data/autofill/BitwardenAutofillService.kt) | The `AutofillService` the system binds to. Kept light on purpose; it only forwards to the processor. |
| [`AutofillProcessor`](../app/src/main/kotlin/com/x8bit/bitwarden/data/autofill/processor/AutofillProcessorImpl.kt) | Runs a fill or save request end to end and calls back to the framework. |
| [`AutofillParser`](../app/src/main/kotlin/com/x8bit/bitwarden/data/autofill/parser/AutofillParserImpl.kt) | Walks the `AssistStructure`, classifies fields, picks the focused partition and the URI. The most intricate part. |
| [`FilledDataBuilder`](../app/src/main/kotlin/com/x8bit/bitwarden/data/autofill/builder/FilledDataBuilderImpl.kt) | Turns a `Fillable` request plus matching vault items into data ready for the response. |
| [`AutofillCipherProvider`](../app/src/main/kotlin/com/x8bit/bitwarden/data/autofill/provider/AutofillCipherProviderImpl.kt) | Supplies the vault items (logins matched by URI, cards, identities) and reports whether the vault is locked. |
| [`SaveInfoBuilder`](../app/src/main/kotlin/com/x8bit/bitwarden/data/autofill/builder/SaveInfoBuilderImpl.kt) | Builds the framework `SaveInfo`, or returns null when a save should not be offered. |
| [`FillResponseBuilder`](../app/src/main/kotlin/com/x8bit/bitwarden/data/autofill/builder/FillResponseBuilderImpl.kt) | Builds the final `FillResponse`. |
| [`FillAssistManager`](../app/src/main/kotlin/com/x8bit/bitwarden/data/autofill/manager/FillAssistManagerImpl.kt) | Fetches and caches server-provided targeting rules (see below). |

## Key models

- **`AutofillRequest`**: either `Fillable` (has a partition, a URI or package name, and inline presentation data) or `Unfillable` (no suggestions).
- **`AutofillView`**: one classified field, such as `Login.Username`, `Login.Password`, `Card.Number`, `Identity.*`, or `Unused` for inputs we do not fill.
- **`AutofillPartition`**: the group of views being filled together: `Login`, `Card` or `Identity`. The parser picks the partition of the first focused view.
- **`FilledData`**: the result of matching and filling, ready to turn into a response.

Identity autofill is behind the `IdentityAutofill` feature flag. With the flag off, a focused Identity field makes the request `Unfillable`.

## How a page or app is matched to vault items

The parser produces one URI per request:

- For web content, it is built from the web domain the structure reports (`webDomain`), or from a browser's URL bar for the browsers listed in `AutofillParserImpl`.
- Otherwise it falls back to the app: `androidapp://<package name>`.

Logins are matched against that URI by `CipherMatchingManager`, so a missing web domain means we match against the browser app itself and find nothing for the site. Card and identity items do not depend on the URI. If the vault is locked there is nothing to match, and the response reflects that instead.

## Browsers and compatibility mode

Some browsers do not report web content to the framework themselves. The service declares them in [`autofill_service_configuration.xml`](../app/src/main/res/xml/autofill_service_configuration.xml) as `compatibility-package` entries, each with a maximum version code. For those browsers, up to that version, Android builds the structure through accessibility data and sets the compatibility-mode flag on the request. Newer builds are expected to support autofill natively. Keep that list in sync with `ACCESSIBILITY_SUPPORTED_BROWSERS` in `accessibility/util/BrowserUtil.kt`, as the comment in the XML says.

Compatibility mode has limits. For example, a login's password may be masked, so a save is not offered for a login in compat mode.

## Fill assist

Fill assist is a set of server-provided rules, keyed by site, that point at specific fields with selectors. When the feature flag and the user setting are both on, and the rules for a host cover the focused field's category, those rules replace the heuristic classification for that request. If the rules match nothing, the request ends up `Unfillable` rather than falling back to the heuristics. Hosts with no rules use the heuristics as usual.

## Debugging

### Logging the heuristics (view structure)

The parser can log every node it sees. It is off by default because the output can contain user data.

1. In `parser/util/LogUtil.kt`, set `ENABLE_ASSIST_STRUCTURE_LOGS` to `true`. **Do not commit this change.**
2. Run a debug build, then reproduce the problem. Each node is logged at verbose level with the tag `BitwardenAutofill`: the website, class name, id entry, id package and text.
3. Read the output in logcat (`adb logcat -s BitwardenAutofill`). Debug builds print logs to logcat. Release and beta builds only write logs to a file while Flight Recorder is on.

Treat these logs as private. The `text` field and other attributes can include what the user typed, so do not share them.

To see more per node, extend `logViewNode` locally with attributes such as `autofillHints`, `hint`, `inputType`, `webDomain` and `htmlInfo`. That is a temporary local change too.

### Other tools

- **Flight Recorder.** Records all log priorities to a file in the app's private storage, shareable from Recorded Logs.
- **`adb shell dumpsys autofill`.** Shows the current autofill service, the compatibility packages the system loaded, and recent requests. The `req=` value in a session's log is the fill request id, the same number as the `-- <id>` suffix in the app's own log lines. The `@<number>` suffix on each node's `autofillId` is the system **session id**, which is a different number. Useful fields include `mCompatMode`, `mUrlBar` and `mSaveOnAllViewsInvisible`.
- **Compat state.** The `Autofill Compat State` section of that dump shows which browsers the system treats as compat-mode and which URL-bar resource id it reads for each.

## Known limitations

### Firefox 160 (Android 17)

Observed on Firefox 160.0.1; this may change with newer releases.

- On some pages Firefox sends the full web structure: a `webDomain`, an `html` or `form` node and `EditText` children. On other pages it sends only an empty root node: a single focused `View` with a blank `webDomain` and no children. The same site can do both, for example on different steps of a login. Forms inside a shadow DOM do not appear in the structure.
- When the structure is empty there is no field to fill, and with no web address the URI falls back to `androidapp://org.mozilla.firefox`. This is the same after the page has been loaded for a while or the field is tapped again.
- The compat-mode version cap in `autofill_service_configuration.xml` does not change this. Raising it did not change the result.
- Firefox sessions run in compat mode, so a login save is not offered.

### Save prompt for native apps with no finish signal

The save-on-all-views-invisible flag is only set in compat mode. A native app that never signals that the login finished may not trigger the save prompt. Setting the flag for such an app made the prompt appear in local testing.

## Where to go next

Read, in order: `BitwardenAutofillService`, `AutofillProcessorImpl`, then `AutofillParserImpl` (start at `parseInternal`). The parser tests (`AutofillParserTests`) show the input shapes the heuristics are expected to handle. For the accessibility path, start at `accessibility/BitwardenAccessibilityService.kt`.
