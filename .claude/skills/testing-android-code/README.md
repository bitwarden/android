# Testing Android Code Skill

Quick-reference guide for writing and reviewing tests in the Bitwarden Android codebase.

## Purpose

This skill provides tactical testing guidance for Bitwarden-specific patterns. It focuses on base test classes, test utilities, and common gotchas unique to this codebase rather than general testing concepts.

## When This Skill Activates

The skill automatically loads when you ask questions like:

- "How do I test this ViewModel?"
- "Why is my Bitwarden test failing?"
- "Write tests for this repository"

Or when you mention terms like: `BaseViewModelTest`, `BitwardenComposeTest`, `stateEventFlow`, `bufferedMutableSharedFlow`, `FakeDispatcherManager`, `createMockCipher`, `asSuccess`

## What's Included

| File | Purpose |
|------|---------|
| `SKILL.md` | Core testing patterns and base class locations |
| `references/test-base-classes.md` | Detailed base class documentation |
| `references/flow-testing-patterns.md` | Turbine patterns for StateFlow/EventFlow |
| `references/critical-gotchas.md` | Anti-patterns and debugging tips |
| `examples/viewmodel-test-example.md` | Complete ViewModel test example |
| `examples/compose-screen-test-example.md` | Complete Compose screen test |
| `examples/repository-test-example.md` | Complete repository test with mocks |

## Patterns Covered

1. **BaseViewModelTest** - Automatic dispatcher setup with `stateEventFlow()` helper
2. **BitwardenComposeTest** - Pre-configured with all managers and theme
3. **BaseServiceTest** - MockWebServer setup for network testing
4. **Turbine Flow Testing** - StateFlow (replay) vs EventFlow (no replay)
5. **Test Data Builders** - 35+ `createMock*` functions with `number: Int` pattern
6. **Fake Implementations** - FakeDispatcherManager, FakeConfigDiskSource
7. **Result Type Testing** - `.asSuccess()`, `.asFailure()`, `assertCoroutineThrows`

## Quick Start

For comprehensive architecture and testing philosophy, see:
- `docs/ARCHITECTURE.md`
