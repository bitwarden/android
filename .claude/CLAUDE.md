# Claude Guidelines

Core directives for maintaining code quality and consistency in the Bitwarden Android project.

## Core Directives

**You MUST follow these directives at all times.**

1. **Adhere to Architecture**: All code modifications MUST follow patterns in `docs/ARCHITECTURE.md`
2. **Follow Code Style**: ALWAYS follow `docs/STYLE_AND_BEST_PRACTICES.md`
3. **Error Handling**: Use Result types and sealed classes per architecture guidelines
4. **Best Practices**: Follow Kotlin idioms (immutability, appropriate data structures, coroutines)
5. **Document Everything**: All public APIs require KDoc documentation
6. **Dependency Management**: Use Hilt DI patterns as established in the project
7. **Use Established Patterns**: Leverage existing components before creating new ones
8. **File References**: Use file:line_number format when referencing code

## Code Quality Standards

### Module Organization

**Core Library Modules:**
- **`:core`** - Common utilities and managers shared across multiple modules
- **`:data`** - Data sources, database, data repositories
- **`:network`** - Networking interfaces, API clients, network utilities
- **`:ui`** - Reusable Bitwarden Composables, theming, UI utilities

**Application Modules:**
- **`:app`** - Password Manager application, feature screens, ViewModels, DI setup
- **`:authenticator`** - Authenticator application for 2FA/TOTP code generation

**Specialized Library Modules:**
- **`:authenticatorbridge`** - Communication bridge between :authenticator and :app
- **`:annotation`** - Custom annotations for code generation (Hilt, Room, etc.)
- **`:cxf`** - Android Credential Exchange (CXF/CXP) integration layer

### Patterns Enforcement

- **MVVM + UDF**: ViewModels with StateFlow, Compose UI
- **Hilt DI**: Interface injection, @HiltViewModel, @Inject constructor
- **Testing**: JUnit 5, MockK, Turbine for Flow testing
- **Error Handling**: Sealed Result types, no throws in business logic

## Security Requirements

**Every change must consider:**
- Zero-knowledge architecture preservation
- Proper encryption key handling (Android Keystore)
- Input validation and sanitization
- Secure data storage patterns
- Threat model implications

## Workflow Practices

### Before Implementation

1. Read relevant architecture documentation
2. Search for existing patterns to follow
3. Identify affected modules and dependencies
4. Consider security implications

### During Implementation

1. Follow existing code style in surrounding files
2. Write tests alongside implementation
3. Add KDoc to all public APIs
4. Validate against architecture guidelines

### After Implementation

1. Ensure all tests pass
2. Verify compilation succeeds
3. Review security considerations
4. Update relevant documentation

## Anti-Patterns

**Avoid these:**
- Creating new patterns when established ones exist
- Exception-based error handling in business logic
- Direct dependency access (use DI)
- Mutable state in ViewModels (use StateFlow)
- Missing null safety handling
- Undocumented public APIs
- Tight coupling between modules

## Communication & Decision-Making

Always clarify ambiguous requirements before implementing. Use specific questions:
- "Should this use [Approach A] or [Approach B]?"
- "This affects [X]. Proceed or review first?"
- "Expected behavior for [specific requirement]?"

Defer high-impact decisions to the user:
- Architecture/module changes, public API modifications
- Security mechanisms, database migrations
- Third-party library additions

## Reference Documentation

Critical resources:
- `docs/ARCHITECTURE.md` - Architecture patterns and principles
- `docs/STYLE_AND_BEST_PRACTICES.md` - Code style guidelines

**Do not duplicate information from these files - reference them instead.**