---
name: evaluating-sdk-internal-updates
description: Evaluates a bitwarden/android "Update SDK to" PR against the sdk-internal commit range for compile-time and runtime breaking changes, maps affected symbols to Android call sites, and applies clear in-scope fixes. Use when reviewing an SDK bump PR, a bitwardenSdk version change, or triaging sdk-internal breaking changes.
allowed-tools: Bash(gh pr diff:*), Bash(gh api repos/bitwarden/sdk-internal/*:*), Bash(git clone:*), Bash(git -C *:*), Bash(grep:*), Bash(./gradlew*compileKotlin*:*), Read, Grep, Glob, Skill(plan-android-work), Skill(work-on-android), Skill(bitwarden-delivery-tools:committing-changes)
---

# Evaluating sdk-internal Updates

**Identify both compile-time and runtime breaks before fixing anything — fixing the first break found is not finishing.** Steps 3-7 always cover the entire commit range before step 8 starts, no matter how obvious or urgent an early compile-time break looks.

## Identify

Binding surface facts specific to this SDK: `#[uniffi::export]` / `derive(uniffi::...)` annotations are scattered across many crates, not just `crates/bitwarden-uniffi`; `crates/bitwarden-ffi` is unrelated ("do not use"). No `.udl` files. Kotlin symbols surface as `com.bitwarden.sdk.*`.

A hunk that only touches a macro invocation (e.g. `state_bridge! { ... }`) doesn't show the binding surface — the expansion lives in the macro's definition, often in a different crate (e.g. `bitwarden-state-bridge-macro`). `#[uniffi::export(with_foreign)]` marks a callback interface: a trait Kotlin must implement, where adding a field/method is never additive-safe for the implementor.

1. Check whether `bitwarden/sdk-internal` is already cloned locally. If not, ask the user: clone it now, or continue using `gh api` calls against the remote instead — cloning is optional, not required.
2. `gh pr diff <PR> -R bitwarden/android | grep bitwardenSdk` → old/new `bitwardenSdk` string. Trailing segment of each is the SHA.
3. Attempt `./gradlew <module>:compileStandardDebugKotlin` at the current checkout before crawling sdk-internal. A failure confirms a compile-time break directly, with a more precise location than any git search — note it and continue to steps 4-7 for the full commit range; do not fix it yet. A clean build only rules out compile-time breaks, not runtime ones.
4. Find candidate binding-surface commits in OLD..NEW. With a local clone: `git log --oneline OLD..NEW -G'uniffi::export|derive\(uniffi|#\[uniffi' -- '*.rs'`. Without one: `gh api repos/bitwarden/sdk-internal/compare/OLD...NEW --jq '.commits[].sha'` for the range, then per commit `gh api repos/bitwarden/sdk-internal/commits/<sha> --jq '.files[] | select(.filename | endswith(".rs")) | .patch'` and grep locally for the same pattern.
5. Classify per hunk, not per commit — a commit with one additive headline change can still have a second, unrelated breaking hunk. If a hunk only touches a macro invocation, read the macro's definition before classifying. With a local clone: `git show <sha> -- '*.rs'`. Without one: reuse the patch already fetched in step 4.
6. For every distinct symbol/type touched (every hunk, not just the commit's headline change), grep `app`, `core`, `network`, `ui` for the symbol name and `import com.bitwarden.sdk.<Symbol>` to find Android call sites.
7. Report: compile-time breaks, runtime breaks, safe/no-call-site — each with commit, symbol, and call sites.

## Resolve

Resolve the findings from Step 7 by deciding on a fix, invoking `/plan-android-work` with the report to plan the implementation, followed by invoking `/work-on-android` with the generated plan to implement required changes.

8. Decide the fix for anything found, compile-time or runtime, whenever the correct behavior is clear and within scope. For a new required method, grep for the underlying concept, not the new method/type name (it won't exist yet) — no existing consumer means stub it: a no-op, null/default return, or `TODO()` that satisfies the compiler, not a behavioral decision. A sibling's structure (naming, placement, style) is a template; its behavior (storage, defaulting, error handling, side effects) is not evidence for yours. If unsure, report it instead of guessing, along with anything needing a product decision.
9. Implement every fix from step 8 by `/plan-android-work` (pass it the step 7 findings) followed by `/work-on-android` (pass it the resulting plan) — never edit the fix in yourself, not even a one-line stub.
10. Verify with the affected module's `compileKotlin` task.
11. Commit using `Skill(bitwarden-delivery-tools:committing-changes)` for the message.
