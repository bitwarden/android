# Changelog

All notable changes to the `implementing-android-code` skill will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.1] - 2026-02-25

### Fixed

- Added missing `@EncryptedPreferences` and `@UnencryptedPreferences` annotations to `ExampleDiskSourceImpl` code example
- Fixed typographic apostrophe example to use correct right single quotation mark (U+2019)

### Removed

- Removed redundant "Summary" section that duplicated existing content

## [0.1.0] - 2026-02-17

### Added

- Bitwarden Android implementation patterns covering:
  - ViewModel State-Action-Event (SAE) pattern with `BaseViewModel`
  - Type-safe navigation with `@Serializable` routes and `composableWithSlideTransitions`
  - Screen/Compose implementation with `EventsEffect` and stateless composables
  - Data layer patterns: repositories, data sources, `DataState<T>`, error handling
  - UI component library usage and string resource conventions
  - Security patterns: zero-knowledge architecture, encrypted storage, SDK isolation
  - Testing quick reference for ViewModels, repositories, compose, and data sources
  - Clock/time injection patterns for deterministic operations
  - Anti-patterns and common gotchas
- Copy-pasteable code templates (templates.md) for all layer types
- README.md, CHANGELOG.md, CONTRIBUTING.md for marketplace preparation
