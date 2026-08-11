---
name: evaluating-sdk-internal-updates
description: Evaluates a bitwarden/android "Update SDK to" PR against the sdk-internal commit range for compile-time and runtime breaking changes, maps affected symbols to Android call sites, and applies clear in-scope fixes. Use when reviewing an SDK bump PR, a bitwardenSdk version change, or triaging sdk-internal breaking changes.
allowed-tools: Bash(gh pr diff:*), Bash(git -C *:*), Bash(grep:*), Bash(./gradlew*:*), Read, Grep, Glob, Skill(plan-android-work), Skill(work-on-android)
---

# Evaluating sdk-internal Updates

**Identify both compile-time and runtime breaks before fixing anything — fixing the first break found is not finishing.** Steps 3-7 always cover the entire commit range before step 8 starts, no matter how obvious or urgent an early compile-time break looks.

## Identify

Binding surface facts specific to this SDK: `#[uniffi::export]` / `derive(uniffi::...)` annotations are scattered across many crates, not just `crates/bitwarden-uniffi`; `crates/bitwarden-ffi` is unrelated ("do not use"). No `.udl` files. UniFFI emits one Kotlin package per crate (`com.bitwarden.core`, `com.bitwarden.vault`, `com.bitwarden.crypto`, etc.) — only the top-level `Client`/`AuthClient`/`GeneratorClients` actually live under `com.bitwarden.sdk`. Both `app` and `authenticator` depend on the SDK; neither is optional to search.

A hunk that only touches a macro invocation (e.g. `state_bridge! { ... }`) doesn't show the binding surface — the expansion lives in the macro's definition, often in a different crate (e.g. `bitwarden-state-bridge-macro`). `#[uniffi::export(with_foreign)]` marks a callback interface: a trait Kotlin must implement, where adding a field/method is never additive-safe for the implementor.

1. Locate the local `bitwarden/sdk-internal` clone (check sibling directories to this repo). If none exists, stop and tell the user it's a required prerequisite for this skill — do not clone it yourself.
2. `gh pr diff <PR> -R bitwarden/android | grep bitwardenSdk` → old/new `bitwardenSdk` string. Everything after the second `-` is the git ref — a commit SHA or a branch name (`.dev` SDK builds use both); resolve a branch name as `origin/<branch>` in the clone.
3. Attempt `./gradlew <module>:compileStandardDebugKotlin` at the current checkout before crawling sdk-internal. A failure confirms a compile-time break directly, with a more precise location than any git search — note it and continue to steps 4-7 for the full commit range; do not fix it yet. A clean build only rules out compile-time breaks, not runtime ones.
4. `git -C <sdk-internal-path> log --oneline OLD..NEW -G'uniffi::export|derive\(uniffi|#\[uniffi' -- '*.rs'` → candidate binding-surface commits.
5. Classify per hunk, not per commit — a commit with one additive headline change can still have a second, unrelated breaking hunk. If a hunk only touches a macro invocation, read the macro's definition before classifying. `git -C <sdk-internal-path> show <sha> -- '*.rs'`.
6. For every distinct symbol/type touched (every hunk, not just the commit's headline change), grep the whole repo for the bare symbol name to find Android call sites — a fixed module list or a `com.bitwarden.sdk.<Symbol>` import-prefix check both miss real consumers.
7. Report: compile-time breaks, runtime breaks, safe/no-call-site — each with commit, symbol, and call sites.

## Resolve

Resolve the findings from Step 7 by deciding on a fix, invoking `/plan-android-work` with the report to plan the implementation, followed by invoking `/work-on-android` with the generated plan to implement required changes.

8. Decide the fix for anything found, compile-time or runtime, whenever the correct behavior is clear and within scope. For a new required method, grep for the underlying concept, not the new method/type name (it won't exist yet) — no existing consumer means stub it: a `// no-op` comment or a null/default return that satisfies the compiler, not a behavioral decision. Never `TODO()` — it throws at runtime, which is a crash, not a stub. A sibling's structure (naming, placement, style) is a template; its behavior (storage, defaulting, error handling, side effects) is not evidence for yours. If unsure, report it instead of guessing, along with anything needing a product decision.
9. Implement every fix from step 8 by `/plan-android-work` (pass it the step 7 findings) followed by `/work-on-android` (pass it the resulting plan) — never edit the fix in yourself, not even a one-line stub.
10. Verify with the same compile task used in step 3.
