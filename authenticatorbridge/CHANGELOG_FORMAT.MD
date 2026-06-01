# CHANGELOG Format

## Contents

* [Template](#template)
* [Sections](#sections)

## Template

Each new version of the SDK should be accompanied by a new `CHANGELOG` entry with the following template:

```
vX.Y.0 (pending)
--------

### API Changes

### Breaking Changes

### Bug Fixes

```

The `(pending)` label, as well as any unused sections, should be removed prior to creating a release.

## Sections

One or more `CHANGELOG` entries should be added for each PR that makes a change that either adds / updates the exposed API of the SDK or changes code internally in a way that is detectable as a behavior change by external consumers. These entries should fall into one of the following sections:

- **API Changes**: Any update to existing code that affects the exposed API or behavior of the SDK in a backwards-compatible way should be included in this section.

- **Breaking Changes**: Any code introduced in a **backwards-incompatible** manner that could in principle affect compilation of existing code should be included in this section. Examples of this include changing return types, changing function / constructor parameter lists, and changing function / class names. Each entry in this list should include detailed instructions where necessary for how a caller should address any related compilation issues.

- **Bug Fixes**: Any update that is performed in order to fix a known bug should be included in this section.

If an PR exhibits multiple kinds of changes then there should be multiple corresponding entries.