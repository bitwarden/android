# Changelog

All notable changes to the `perform-android-preflight-checklist` skill will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-02-27

### Added

- Quality gate checklist covering:
  - Test verification with correct flavor variants
  - Code quality checks (lint, detekt, KDoc, TODOs)
  - Security validation (secrets, input validation, encrypted storage, logging)
  - Bitwarden pattern compliance (string resources, navigation, Hilt, ViewModel, async)
  - File hygiene (no IDE files, build outputs, or credentials)
- README.md, CHANGELOG.md, CONTRIBUTING.md for marketplace preparation