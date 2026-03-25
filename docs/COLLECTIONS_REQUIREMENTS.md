# Collection Management on Android - Requirements Specification

> **Status: DRAFT** - Pending answers to blocking questions (see [Open Questions - Blocking](#blocking-questions))
>
> **Date:** 2026-03-17
>
> **Branch:** `android-collections`

---

## Overview

This specification defines the requirements for adding collection management (create, edit, delete) to the Bitwarden Android Password Manager app. Collections are an organizational concept in Bitwarden that group vault items within an organization. They are available only on paid plans (Families, Teams, Teams Starter, Enterprise); free organizations are limited to a single collection.

The web client already supports full collection CRUD. This feature brings parity to the Android app, accessible via **Settings > Vault > Collections**. The implementation will follow the established folder management pattern (`FolderManager`, `FoldersScreen`, `FolderAddEditScreen`) as the primary architectural reference.

**Scope for V1:**
- Create, edit (rename), and delete collections
- Permission-gated: only users with appropriate org roles can perform these actions
- No user/group access management UI (access is managed via the web admin console)
- No nested collection creation (parent picker) in V1; existing nested collections display correctly

---

## Functional Requirements

| ID | Requirement | Source | Notes |
|----|------------|--------|-------|
| FR1 | Users can view a list of collections they have access to, grouped or filtered by organization | User, Web client | **See [G1]** for multi-org display decision |
| FR2 | Users can create a new collection within an organization they have `createNewCollections` permission for | User, Web client | Requires org key encryption of collection name |
| FR3 | Users can edit (rename) a collection they have `manage` or `editAnyCollection` permission for | User, Web client | Only the name field is editable on mobile |
| FR4 | Users can delete a collection they have `manage` or `deleteAnyCollection` permission for | User, Web client | Confirmation dialog required before deletion |
| FR5 | Collection name is required and must not contain `/` characters | Web client | `/` is the nesting delimiter; creation of nested collections deferred to future version |
| FR6 | Free organizations are limited to `maxCollections` (typically 1) collections; creation is blocked when at limit | Web client | **See [G5]** for UX treatment |
| FR7 | The FAB (floating action button) for creating a new collection is only visible when the user has permission to create collections in at least one organization | Web client | If user has no orgs or no create permission, FAB is hidden |
| FR8 | The delete option is only visible when the user has permission to delete the specific collection | Web client | Shown in overflow menu, matching folder pattern |
| FR9 | Collection list shows the decrypted display name of each collection | Android codebase | Uses existing `toCollectionDisplayName()` helper for nested names |
| FR10 | After successful create/edit/delete, a snackbar confirmation is shown on the Collections list screen | Folder pattern | Uses `SnackbarRelayManager` relay pattern |
| FR11 | Network errors during CRUD operations show a generic error snackbar | Folder pattern | No optimistic local write; server is source of truth |
| FR12 | The Collections list screen shows Loading, Content (with items), Empty, and Error states | Folder pattern | Empty state shown when user has no collections across any org |
| FR13 | Back navigation from CollectionAddEdit returns to the Collections list without saving | Folder pattern | Standard back-press behavior |
| FR14 | The entry point is a new "Collections" row in the Settings > Vault screen | User | Added between "Folders" and "Export Vault" |

---

## Technical Requirements

| ID | Requirement | Source | Notes |
|----|------------|--------|-------|
| TR1 | **Module scope**: All new UI code lives in `:app` module under `ui/platform/feature/settings/collections/` | Folder pattern | Matches folder feature module structure |
| TR2 | **Network API**: New `CollectionsApi` interface with endpoints: `POST /organizations/{orgId}/collections`, `PUT /organizations/{orgId}/collections/{id}`, `DELETE /organizations/{orgId}/collections/{id}` | Web client | Lives in `:network` module |
| TR3 | **Network service**: New `CollectionService` / `CollectionServiceImpl` wrapping `CollectionsApi` | Folder pattern | Returns `Result<T>` types |
| TR4 | **Request models**: `CreateCollectionJsonRequest` and `UpdateCollectionJsonRequest` with encrypted `name` field and `externalId` | Web client | Lives in `:network` module |
| TR5 | **Response model**: Reuse existing `SyncResponseJson.Collection` for create/update responses | Web client | Already defined |
| TR6 | **SDK encryption**: `VaultSdkSource` must expose `encryptCollection` to encrypt collection name with org key before API calls | **BLOCKER [G2]** | Only `decryptCollection`/`decryptCollectionList` exist today |
| TR7 | **CollectionManager**: New `CollectionManager` interface + `CollectionManagerImpl` following `FolderManager` pattern; handles encrypt > API call > save to disk > decrypt flow | Folder pattern | Delegated from `VaultRepository` |
| TR8 | **Result types**: New sealed classes `CreateCollectionResult`, `UpdateCollectionResult`, `DeleteCollectionResult` with `Success`/`Error` variants | Folder pattern | In `data/vault/repository/model/` |
| TR9 | **VaultDiskSource**: Add `deleteCollection(userId, collectionId)` method | Gap analysis | `saveCollection` exists; delete does not |
| TR10 | **Permission model expansion**: Add to `SyncResponseJson.Permissions`: `createNewCollections: Boolean`, `editAnyCollection: Boolean`, `deleteAnyCollection: Boolean` | **BLOCKER [G3]** | Fields exist in API JSON but are not parsed |
| TR11 | **Organization domain model expansion**: Add `maxCollections: Int?` and `limitCollectionCreation: Boolean` to the Android `Organization` data class (and its mapping from `SyncResponseJson.Profile.Organization`) | **BLOCKER [G3]** | `maxCollections` exists in `SyncResponseJson` but isn't mapped to domain model |
| TR12 | **Navigation**: Type-safe `@Serializable` routes: `CollectionsRoute`, `CollectionAddEditRoute(actionType, collectionId?, organizationId)` | Folder pattern | `organizationId` required for both create and edit |
| TR13 | **SnackbarRelay**: Add `COLLECTION_CREATED`, `COLLECTION_UPDATED`, `COLLECTION_DELETED` entries to `SnackbarRelay` | Folder pattern | |
| TR14 | **Process death**: Collection name field persisted via `SavedStateHandle` in `CollectionAddEditViewModel` | Folder pattern | |
| TR15 | **VaultSettingsScreen update**: Add "Collections" `BitwardenTextRow` between "Folders" and "Export Vault" with `CardStyle.Middle()` and update surrounding card styles | VaultSettingsScreen | Requires new `CollectionsButtonClick` action and `NavigateToCollections` event |
| TR16 | **F-Droid**: No Google Play Services dependency | Requirement | Feature is pure network + SDK |

---

## Security Requirements

| ID | Requirement | Source | Notes |
|----|------------|--------|-------|
| SR1 | Collection names must be encrypted with the organization's encryption key (via `ScopedVaultSdkSource`) before transmission to the API | Web client, Zero-knowledge architecture | Never transmit plaintext collection names |
| SR2 | Use `ScopedVaultSdkSource` for all encryption/decryption to prevent cross-user crypto context leakage | CLAUDE.md Security Rules | Critical for multi-account safety |
| SR3 | On logout, all collection data is cleared via existing `CollectionsDao` user-scoped cleanup | Existing behavior | Already handled by `UserLogoutManager` |
| SR4 | Validate collection name input (non-empty, no `/` characters) before processing | Web client | Input sanitization at UI boundary |
| SR5 | Permission checks must be enforced client-side before showing create/edit/delete UI affordances | Web client | Do not show actions the user cannot perform |

---

## UX Requirements

| ID | Requirement | Source | Notes |
|----|------------|--------|-------|
| UX1 | **Collections list screen**: Top app bar with title "Collections", back navigation arrow, FAB with `+` icon for creating new collections | Folder pattern | FAB hidden if user has no create permissions |
| UX2 | **Collection list item**: Shows decrypted collection name; tap navigates to edit screen | Folder pattern | Nested collection names shown using `toCollectionDisplayName()` |
| UX3 | **Multi-org display**: **See [G1]** - collections must indicate which organization they belong to | User | Organization name shown as subtitle or section header |
| UX4 | **Add/edit screen**: Top app bar with "New Collection" or "Edit Collection" title, single text field for name, save button in top bar, delete in overflow menu (edit only) | Folder pattern | |
| UX5 | **Delete confirmation**: Dialog with "Do you really want to delete? This collection will be permanently deleted." and Cancel/Delete buttons | Folder pattern | |
| UX6 | **Loading state**: Full-screen loading spinner | Folder pattern | |
| UX7 | **Empty state**: Centered text indicating no collections are available | Folder pattern | Exact copy TBD |
| UX8 | **Error state**: Generic error with retry option | Folder pattern | |
| UX9 | **Snackbar messages**: "Collection created", "Collection updated", "Collection deleted" | Folder pattern | |
| UX10 | **Permission error**: "You don\u2019t have permission to perform this action." snackbar if server returns 403 | Web client | |
| UX11 | **Collection limit reached**: **See [G5]** - message when free org is at max | Web client | |
| UX12 | **Org selection for create**: If user belongs to multiple orgs with create permission, must select which org to create in | Functional | **See [G1]** for approach |
| UX13 | **String resources**: All user-facing strings added to `:ui` module `strings.xml` with typographic quotes/apostrophes | CLAUDE.md | |

---

## Open Questions

### Blocking Questions

These must be answered before implementation can begin.

| ID | Category | Question | Impact |
|----|----------|----------|--------|
| **G1** | Functional / UX | **Multi-organization display**: When a user belongs to multiple organizations, how should the Collections screen present their collections? **Option A**: Flat list with organization name as a subtitle on each item or as section headers grouping collections by org. **Option B**: Organization selector/filter at the top of the screen (user picks which org to manage). | Affects `CollectionsViewModel` state model, `CollectionAddEditRoute` parameters, and whether an org picker component is needed on the create screen. Option A is simpler but may be noisy for users in many orgs. Option B is more focused but adds a selector component. |
| **G2** | Technical | **SDK `encryptCollection`**: Confirmed that `encryptCollection` does **not** exist anywhere in the Bitwarden Rust SDK. The `Collection` type in `crates/bitwarden/src/vault/collection.rs` only implements `KeyDecryptable`, not `KeyEncryptable`. The UniFFI bindings (`crates/bitwarden-uniffi/src/vault/collections.rs`) only expose `decrypt` and `decrypt_list`. By contrast, the folder equivalent has both `encrypt` and `decrypt` at every layer. **The SDK needs a small, well-scoped addition**: (1) `KeyEncryptable<SymmetricCryptoKey, Collection>` impl for `CollectionView` in `collection.rs`, (2) `encrypt` method in `client_collection.rs`, (3) `encrypt` UniFFI export in `collections.rs`. This requires a **new SDK release** before the Android feature can be fully implemented. | Hard blocker for the data layer. Requires coordination with the SDK team for a new release. The change is small and well-patterned (mirrors the existing folder encryption exactly), but it is a cross-repo dependency. |
| **G3** | Technical | **Permission model expansion**: **RESOLVED — In scope.** The Android `SyncResponseJson.Permissions` model will be expanded to parse `createNewCollections`, `editAnyCollection`, and `deleteAnyCollection` from the API JSON. The `Organization` domain model will be expanded to include `limitCollectionCreation` and map `maxCollections`. This enables correct permission gating for all org roles including custom roles. | Low implementation risk. Fields already exist in the API response; only parsing and domain mapping need to be added. |

### Non-Blocking Questions

These have reasonable defaults and can be resolved during implementation.

| ID | Category | Question | Default Assumption |
|----|----------|----------|--------------------|
| **G4** | Functional | Should users be able to create nested collections (via parent picker) in V1? | **No**. V1 supports flat collection creation only. Name input rejects `/` characters. Existing nested collections display correctly in the list using `toCollectionDisplayName()`. A parent picker can be added in a future iteration. |
| **G5** | Functional / UX | When a free org is at its collection limit, what should happen when the user taps the FAB? | Show a dialog explaining the limit has been reached, suggesting the user upgrade via the web vault. No in-app purchase or deep-link needed in V1. |
| **G6** | UX | Should `externalId` be shown (read-only) in the collection edit view? | **No**. `externalId` is an admin-console-only concept. Showing it on mobile adds noise with no actionable benefit. |
| **G7** | UX | What exact strings should appear for screen titles, labels, and messages? | Follow folder conventions: "Collections" (list title), "New Collection" / "Edit Collection" (add/edit titles), "Name" (field label), "Save" (button), standard delete confirmation and snackbar messages as described in UX requirements. |
| **G8** | Cross-cutting | Should this feature be behind a server-side feature flag for staged rollout? | **No feature flag**. This is a purely additive UI feature using existing stable API endpoints (same endpoints the web client has used for years). If PM wants a rollout gate, a `FlagKey` entry can be added. |
| **G9** | Functional | What should happen if the user attempts CRUD while offline? | Match folder behavior: the API call fails and a generic network error snackbar is shown. No optimistic local writes. |
| **G10** | Cross-cutting | Are there analytics events to emit for collection CRUD? | **No analytics in V1**, consistent with folder management. If `OrganizationEventManager` tracking is desired, it can be scoped separately. |

---

## Existing Infrastructure (What We Can Reuse)

The following components already exist in the Android codebase and will be leveraged:

| Component | Location | What It Provides |
|-----------|----------|------------------|
| `CollectionEntity` | `data/vault/datasource/disk/entity/` | Room entity for collection storage |
| `CollectionsDao` | `data/vault/datasource/disk/dao/` | Room DAO with insert/query operations |
| `VaultDiskSource.saveCollection()` | `data/vault/datasource/disk/` | Save collection to disk (needs `deleteCollection` added) |
| `VaultSdkSource.decryptCollection()` | `data/vault/datasource/sdk/` | Decrypt collection with org key (needs `encryptCollection` added) |
| `VaultSyncManager.collectionsStateFlow` | `data/vault/manager/` | Streaming `DataState<List<CollectionView>>` from sync |
| `CollectionViewExtensions` | `ui/vault/feature/util/` | `toCollectionDisplayName()`, `getFilteredCollections()`, permission helpers |
| `CollectionPermission` enum | `ui/vault/feature/util/model/` | VIEW, VIEW_EXCEPT_PASSWORDS, EDIT, EDIT_EXCEPT_PASSWORD, MANAGE |
| `SyncResponseJson.Collection` | `:network` module | API response model for collections |
| `SyncResponseJson.Organization.maxCollections` | `:network` module | Collection limit field (exists in JSON model) |
| `VaultSdkCollectionExtensions` | `data/vault/repository/util/` | `toEncryptedSdkCollection()` conversion, sorting utilities |

---

## New Components Required

| Component | Module | Pattern Reference |
|-----------|--------|-------------------|
| `CollectionsApi` (Retrofit interface) | `:network` | `FoldersApi` |
| `CollectionService` / `CollectionServiceImpl` | `:network` | `FolderService` / `FolderServiceImpl` |
| `CreateCollectionJsonRequest` | `:network` | `FolderJsonRequest` |
| `UpdateCollectionJsonRequest` | `:network` | `FolderJsonRequest` |
| `CollectionManager` / `CollectionManagerImpl` | `:app` data layer | `FolderManager` / `FolderManagerImpl` |
| `CreateCollectionResult` | `:app` repository model | `CreateFolderResult` |
| `UpdateCollectionResult` | `:app` repository model | `UpdateFolderResult` |
| `DeleteCollectionResult` | `:app` repository model | `DeleteFolderResult` |
| `CollectionsScreen` + `CollectionsViewModel` | `:app` UI | `FoldersScreen` + `FoldersViewModel` |
| `CollectionAddEditScreen` + `CollectionAddEditViewModel` | `:app` UI | `FolderAddEditScreen` + `FolderAddEditViewModel` |
| `CollectionsNavigation` / `CollectionAddEditNavigation` | `:app` UI | `FoldersNavigation` / `FolderAddEditNavigation` |
| `CollectionsRoute` / `CollectionAddEditRoute` | `:app` UI | `FoldersRoute` / `FolderAddEditRoute` |
| `CollectionDisplayItem` | `:app` UI model | `FolderDisplayItem` |
| `CollectionAddEditType` | `:app` UI model | `FolderAddEditType` |
| `SnackbarRelay` entries for collection CRUD | `:app` UI model | Existing `SnackbarRelay` enum |
| Permission fields on `SyncResponseJson.Permissions` | `:network` | Extend existing model |
| Permission/limit fields on `Organization` domain model | `:app` data layer | Extend existing model |

---

## Source Documentation

| Source | Type | Description |
|--------|------|-------------|
| Bitwarden Web Client | Codebase reference | `../clients/apps/web/` - `CollectionDialogComponent`, `CollectionAdminService`, collection models |
| Bitwarden Android - Folder Feature | Codebase reference | `ui/platform/feature/settings/folders/` - full CRUD pattern reference |
| Bitwarden Android - VaultSettings | Codebase reference | `ui/platform/feature/settings/vault/` - entry point screen |
| Bitwarden Android - Collection Data Layer | Codebase reference | `data/vault/datasource/disk/entity/CollectionEntity.kt`, `CollectionsDao`, `VaultSdkCollectionExtensions` |
| User requirements | User-provided | Create/edit/delete collections via Settings > Vault > Collections; paid plans only |
