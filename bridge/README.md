# Bitwarden Native Bridge SDK

## Contents

- [Compatibility](#compatibility)
- [Versioning](#versioning)
 
## Other Documents

- [Changelog](CHANGELOG.md)
- [Changelog Format Guide](CHANGELOG_FORMAT.MD)

## Compatibility

- **Minimum SDK**: 28
- **Target SDK**: 34

## Versioning
This repository conforms to the following versioning convention:

**v[MAJOR].[MINOR].[PATCH]**

```
where [RELEASE]   is incremented to represent major milestones that indicate a significant change in the library.

      [MINOR]     is incremented when any standard changes (breaking or otherwise) are introduced to the library.

      [PATCH]     is incremented when a hot-fix patch is required to an existing minor
                  release due to a bug introduced in that release.
```

Some things to note:

- All updates should have a corresponding `CHANGELOG.md` entry that at a high-level describes what is being newly introduced in it. For more info, see [Changelog Format Guide](CHANGELOG_FORMAT.MD)

- When incrementing a level any lower-levels should always reset to 0.