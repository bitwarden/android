# Collection Management — Design Document

**Feature**: Add collection CRUD (create, edit, delete) to Settings > Vault > Collections
**Date**: 2026-03-18
**Status**: Ready for Implementation
**Branch**: `android-collections`
**Sources**: User requirements, Bitwarden web client (`../clients/apps/web/`), Android folder feature (pattern reference), Android collection data layer (existing infrastructure)

---

## Requirements Specification

> **Status: DRAFT** — G1 (multi-org display) still pending team input. G2 (SDK encryption) resolved — changes implemented in SDK repo, awaiting publish. G3 (permission model expansion) resolved — confirmed in scope.

### Overview

This specification defines the requirements for adding collection management (create, edit, delete) to the Bitwarden Android Password Manager app. Collections are an organizational concept in Bitwarden that group vault items within an organization. They are available only on paid plans (Families, Teams, Teams Starter, Enterprise); free organizations are limited to a single collection.

The web client already supports full collection CRUD. This feature brings parity to the Android app, accessible via **Settings > Vault > Collections**. The implementation will follow the established folder management pattern (`FolderManager`, `FoldersScreen`, `FolderAddEditScreen`) as the primary architectural reference.

**Scope for V1:**
- Create, edit (rename), and delete collections
- Permission-gated: only users with appropriate org roles can perform these actions
- No user/group access management UI (access is managed via the web admin console)
- No nested collection creation (parent picker) in V1; existing nested collections display correctly

### Functional Requirements

| ID | Requirement | Source | Notes |
|----|------------|--------|-------|
| FR1 | Users can view a list of collections they have access to, grouped or filtered by organization | User, Web client | Default: flat list with org name subtitle (G1 pending) |
| FR2 | Users can create a new collection within an organization they have `createNewCollections` permission for | User, Web client | Requires org key encryption of collection name |
| FR3 | Users can edit (rename) a collection they have `manage` or `editAnyCollection` permission for | User, Web client | Only the name field is editable on mobile |
| FR4 | Users can delete a collection they have `manage` or `deleteAnyCollection` permission for | User, Web client | Confirmation dialog required before deletion |
| FR5 | Collection name is required and must not contain `/` characters | Web client | `/` is the nesting delimiter; creation of nested collections deferred to future version |
| FR6 | Free organizations are limited to `maxCollections` (typically 1) collections; creation is blocked when at limit | Web client | Show dialog explaining limit, suggest upgrading via web |
| FR7 | The FAB for creating a new collection is only visible when the user has permission to create collections in at least one organization | Web client | If user has no orgs or no create permission, FAB is hidden |
| FR8 | The delete option is only visible when the user has permission to delete the specific collection | Web client | Shown in overflow menu, matching folder pattern |
| FR9 | Collection list shows the decrypted display name of each collection | Android codebase | Uses existing `toCollectionDisplayName()` helper for nested names |
| FR10 | After successful create/edit/delete, a snackbar confirmation is shown on the Collections list screen | Folder pattern | Uses `SnackbarRelayManager` relay pattern |
| FR11 | Network errors during CRUD operations show a generic error snackbar | Folder pattern | No optimistic local write; server is source of truth |
| FR12 | The Collections list screen shows Loading, Content (with items), Empty, and Error states | Folder pattern | Empty state shown when user has no collections across any org |
| FR13 | Back navigation from CollectionAddEdit returns to the Collections list without saving | Folder pattern | Standard back-press behavior |
| FR14 | The entry point is a new "Collections" row in the Settings > Vault screen | User | Added between "Folders" and "Export Vault" |

### Technical Requirements

| ID | Requirement | Source | Notes |
|----|------------|--------|-------|
| TR1 | **Module scope**: All new UI code lives in `:app` module under `ui/platform/feature/settings/collections/` | Folder pattern | Matches folder feature module structure |
| TR2 | **Network API**: New `CollectionsApi` interface with endpoints: `POST /organizations/{orgId}/collections`, `PUT /organizations/{orgId}/collections/{id}`, `DELETE /organizations/{orgId}/collections/{id}` | Web client | Lives in `:network` module |
| TR3 | **Network service**: New `CollectionService` / `CollectionServiceImpl` wrapping `CollectionsApi` | Folder pattern | Returns `Result<T>` types |
| TR4 | **Request models**: `CollectionJsonRequest` with encrypted `name` field | Web client | Lives in `:network` module |
| TR5 | **Response model**: Reuse existing `SyncResponseJson.Collection` for create/update responses | Web client | Already defined |
| TR6 | **SDK encryption**: `VaultSdkSource` must expose `encryptCollection` — stubbed with TODO until SDK published | SDK repo changes done | SDK changes implemented, awaiting release |
| TR7 | **CollectionManager**: New `CollectionManager` interface + `CollectionManagerImpl` following `FolderManager` pattern | Folder pattern | Delegated from `VaultRepository` |
| TR8 | **Result types**: New sealed classes `CreateCollectionResult`, `UpdateCollectionResult`, `DeleteCollectionResult` | Folder pattern | In `data/vault/repository/model/` |
| TR9 | **VaultDiskSource**: Add `deleteCollection(userId, collectionId)` method | Gap analysis | `saveCollection` exists; delete does not. DAO method already exists. |
| TR10 | **Permission model expansion**: Add to `SyncResponseJson.Permissions`: `createNewCollections`, `editAnyCollection`, `deleteAnyCollection` | In scope (G3 resolved) | Fields exist in API JSON but are not parsed |
| TR11 | **Organization domain model expansion**: Add `maxCollections: Int?` to the Android `Organization` data class | In scope (G3 resolved) | `maxCollections` exists in `SyncResponseJson` but isn't mapped |
| TR12 | **Navigation**: Type-safe `@Serializable` routes: `CollectionsRoute`, `CollectionAddEditRoute` | Folder pattern | `organizationId` required for create |
| TR13 | **SnackbarRelay**: Add `COLLECTION_CREATED`, `COLLECTION_UPDATED`, `COLLECTION_DELETED` entries | Folder pattern | |
| TR14 | **Process death**: Collection name field persisted via `SavedStateHandle` | Folder pattern | |
| TR15 | **VaultSettingsScreen update**: Add "Collections" row between "Folders" and "Export Vault" | VaultSettingsScreen | New action + event in ViewModel |
| TR16 | **F-Droid**: No Google Play Services dependency | Requirement | Pure network + SDK |

### Security Requirements

| ID | Requirement | Source | Notes |
|----|------------|--------|-------|
| SR1 | Collection names must be encrypted with the organization's key via `ScopedVaultSdkSource` before API transmission | Zero-knowledge architecture | Never transmit plaintext collection names |
| SR2 | Use `ScopedVaultSdkSource` for all encryption/decryption to prevent cross-user crypto context leakage | CLAUDE.md Security Rules | Critical for multi-account safety |
| SR3 | On logout, all collection data is cleared via existing `CollectionsDao` user-scoped cleanup | Existing behavior | Already handled by `UserLogoutManager` |
| SR4 | Validate collection name input (non-empty, no `/` characters) before processing | Web client | Input sanitization at UI boundary |
| SR5 | Permission checks must be enforced client-side before showing create/edit/delete UI affordances | Web client | Do not show actions the user cannot perform |

### UX Requirements

| ID | Requirement | Source | Notes |
|----|------------|--------|-------|
| UX1 | **Collections list screen**: Top app bar with "Collections" title, back arrow, FAB for adding | Folder pattern | FAB hidden if no create permissions |
| UX2 | **Collection list item**: Decrypted name with org name subtitle; tap navigates to edit | Folder pattern | |
| UX3 | **Multi-org display**: Flat list with org name as subtitle on each item (default, G1 pending) | User | |
| UX4 | **Add/edit screen**: "New Collection" / "Edit Collection" title, name field, save button, delete in overflow | Folder pattern | |
| UX5 | **Delete confirmation**: Dialog with warning text and Cancel/Delete buttons | Folder pattern | |
| UX6 | **Loading state**: Full-screen loading spinner | Folder pattern | |
| UX7 | **Empty state**: Centered text "No collections available" | Folder pattern | |
| UX8 | **Error state**: Generic error with retry option | Folder pattern | |
| UX9 | **Snackbar messages**: "Collection created", "Collection updated", "Collection deleted" | Folder pattern | |
| UX10 | **Permission error**: "You don\u2019t have permission to perform this action." | Web client | |
| UX11 | **Collection limit reached**: Dialog explaining limit, suggesting web upgrade | Web client | |
| UX12 | **Org selection for create**: If multiple orgs with create permission, select which org | Functional | |
| UX13 | **String resources**: All strings in `:ui` module `strings.xml` with typographic quotes | CLAUDE.md | |

### Open Questions

| ID | Category | Question | Status / Default |
|----|----------|----------|-----------------|
| **G1** | Functional / UX | Multi-organization display: flat list with org subtitle (Option A) vs. org selector (Option B)? | **Pending** — defaulting to Option A |
| **G2** | Technical | SDK `encryptCollection` availability | **Resolved** — implemented in SDK repo, awaiting publish. Stubbed in Android. |
| **G3** | Technical | Permission model expansion in scope? | **Resolved** — confirmed in scope |
| **G4** | Functional | Nested collection creation in V1? | Default: No, flat only |
| **G5** | Functional / UX | Free org at collection limit UX? | Default: Dialog suggesting web upgrade |
| **G6** | UX | Show externalId in edit view? | Default: No |
| **G7** | UX | Exact string values? | Default: Follow folder conventions |
| **G8** | Cross-cutting | Feature flag? | Default: No |
| **G9** | Functional | Offline behavior? | Default: Error snackbar, match folders |
| **G10** | Cross-cutting | Analytics events? | Default: None in V1 |

---

## Implementation Plan

### Classification

**Type: New Feature** — Entirely new screens, data layer managers, network services, and navigation. Multi-phase, touching `:network` and `:app` modules across data, domain, and UI layers.

### Architecture

```
┌─────────────────────────┐   ┌──────────────────────────────┐
│   CollectionsScreen     │   │   CollectionAddEditScreen     │
│  (list, FAB, org names) │   │  (name field, save, delete)  │
└────────────┬────────────┘   └──────────────┬───────────────┘
             │                               │
┌────────────▼────────────┐   ┌──────────────▼───────────────┐
│   CollectionsViewModel  │   │ CollectionAddEditViewModel    │
│  DataState<List<CV>>    │   │ create/update/delete actions  │
└────────────┬────────────┘   └──────────────┬───────────────┘
             │                               │
             └──────────┬───────────────────┘
                        │
              ┌─────────▼──────────┐
              │  VaultRepository   │  ← delegates to CollectionManager
              │  (by collectionMgr)│
              └─────────┬──────────┘
                        │
              ┌─────────▼──────────┐
              │ CollectionManager  │  ← encrypt → API → disk → decrypt
              │ (CollectionMgrImpl)│
              └──┬────┬────┬──────┘
                 │    │    │
         ┌───────┘    │    └────────┐
         ▼            ▼             ▼
   VaultSdkSource  CollectionSvc  VaultDiskSource
   (encrypt/decrypt) (Retrofit)  (Room via DAO)
                      │
               CollectionsApi
            (POST/PUT/DELETE)
```

### Design Decisions

| Decision | Resolution | Rationale |
|----------|-----------|-----------|
| Multi-org display (G1 open) | Flat list with org name subtitle | Simpler; can refactor to org selector later |
| encryptCollection unavailable | Stub in VaultSdkSource with TODO | SDK not yet published; unblocks all other work |
| Collection nesting in V1 | Flat only, reject `/` in name input | Reduces scope; nesting display already works |
| Push notification sync | Defer to future phase | Not blocking for V1 |
| Collection API base path | `/organizations/{orgId}/collections` | Matches web client |

### File Inventory

#### Files to Create

| File Path | Type | Pattern Reference |
|-----------|------|-------------------|
| `network/.../api/CollectionsApi.kt` | Retrofit API | `FoldersApi.kt` |
| `network/.../service/CollectionService.kt` | Service interface | `FolderService.kt` |
| `network/.../service/CollectionServiceImpl.kt` | Service impl | `FolderServiceImpl.kt` |
| `network/.../model/CollectionJsonRequest.kt` | Request model | `FolderJsonRequest.kt` |
| `network/.../model/UpdateCollectionResponseJson.kt` | Response model | `UpdateFolderResponseJson.kt` |
| `app/.../vault/manager/CollectionManager.kt` | Manager interface | `FolderManager.kt` |
| `app/.../vault/manager/CollectionManagerImpl.kt` | Manager impl | `FolderManagerImpl.kt` |
| `app/.../vault/repository/model/CreateCollectionResult.kt` | Result type | `CreateFolderResult.kt` |
| `app/.../vault/repository/model/UpdateCollectionResult.kt` | Result type | `UpdateFolderResult.kt` |
| `app/.../vault/repository/model/DeleteCollectionResult.kt` | Result type | `DeleteFolderResult.kt` |
| `app/.../settings/collections/CollectionsScreen.kt` | Compose screen | `FoldersScreen.kt` |
| `app/.../settings/collections/CollectionsViewModel.kt` | ViewModel | `FoldersViewModel.kt` |
| `app/.../settings/collections/CollectionsNavigation.kt` | Navigation | `FoldersNavigation.kt` |
| `app/.../settings/collections/addedit/CollectionAddEditScreen.kt` | Compose screen | `FolderAddEditScreen.kt` |
| `app/.../settings/collections/addedit/CollectionAddEditViewModel.kt` | ViewModel | `FolderAddEditViewModel.kt` |
| `app/.../settings/collections/addedit/CollectionAddEditNavigation.kt` | Navigation | `FolderAddEditNavigation.kt` |
| `app/.../settings/collections/model/CollectionDisplayItem.kt` | UI model | `FolderDisplayItem.kt` |
| `app/.../settings/collections/model/CollectionAddEditType.kt` | UI model | `FolderAddEditType.kt` |

#### Files to Modify

| File Path | Change Description | Risk |
|-----------|-------------------|------|
| `network/.../model/SyncResponseJson.kt` | Add fields to `Permissions` | Medium |
| `network/.../BitwardenServiceClient.kt` | Add `collectionService` property | Low |
| `network/.../BitwardenServiceClientImpl.kt` | Add `collectionService` lazy impl | Low |
| `app/.../datasource/disk/VaultDiskSource.kt` | Add `deleteCollection` method | Low |
| `app/.../datasource/disk/VaultDiskSourceImpl.kt` | Implement `deleteCollection` | Low |
| `app/.../datasource/sdk/VaultSdkSource.kt` | Add `encryptCollection` stub | Low |
| `app/.../datasource/sdk/VaultSdkSourceImpl.kt` | Implement `encryptCollection` stub | Low |
| `app/.../auth/repository/model/Organization.kt` | Add `maxCollections` field | Low |
| `app/.../auth/repository/util/...Extensions.kt` | Map new `Organization` fields | Medium |
| `app/.../vault/manager/di/VaultManagerModule.kt` | Provide `CollectionManager` | Low |
| `app/.../vault/repository/VaultRepository.kt` | Extend `CollectionManager` | Medium |
| `app/.../vault/repository/VaultRepositoryImpl.kt` | Delegate `CollectionManager` | Medium |
| `app/.../vault/repository/di/VaultRepositoryModule.kt` | Add `CollectionManager` param | Low |
| `app/.../vault/datasource/network/di/VaultNetworkModule.kt` | Provide `CollectionService` | Low |
| `app/.../ui/platform/model/SnackbarRelay.kt` | Add collection relay entries | Low |
| `app/.../settings/vault/VaultSettingsScreen.kt` | Add Collections row | Low |
| `app/.../settings/vault/VaultSettingsViewModel.kt` | Add Collections action/event | Low |
| `app/.../settings/vault/VaultSettingsNavigation.kt` | Add `onNavigateToCollections` | Low |
| `app/.../vaultunlocked/VaultUnlockedNavigation.kt` | Register collections destinations | Low |
| `ui/.../res/values/strings.xml` | Add collection string resources | Low |

### Implementation Phases

#### Phase 1: Permission Model & Data Foundation

**Goal**: Expand the permission and organization models, add missing disk/SDK operations.

**Files**:
- Modify: `SyncResponseJson.kt`, `Organization.kt`, org mapping extensions, `VaultDiskSource.kt`, `VaultDiskSourceImpl.kt`, `VaultSdkSource.kt`, `VaultSdkSourceImpl.kt`

**Tasks**:
1. Add `createNewCollections`, `editAnyCollection`, `deleteAnyCollection` to `SyncResponseJson.Permissions`
2. Add `maxCollections: Int?` to `Organization` domain model and its mapping from `SyncResponseJson.Profile.Organization`
3. Add `deleteCollection(userId, collectionId)` to `VaultDiskSource`/`VaultDiskSourceImpl` (DAO method already exists)
4. Add `encryptCollection(userId, collectionView)` stub to `VaultSdkSource`/`VaultSdkSourceImpl` with TODO comment

**Verification**: `./gradlew app:compileStandardDebugKotlin`

**Skills**: `implementing-android-code`

#### Phase 2: Network Layer

**Goal**: Create the collections API, service, and request/response models in the `:network` module.

**Files**:
- Create: `CollectionsApi.kt`, `CollectionService.kt`, `CollectionServiceImpl.kt`, `CollectionJsonRequest.kt`, `UpdateCollectionResponseJson.kt`
- Modify: `BitwardenServiceClient.kt`, `BitwardenServiceClientImpl.kt`, `VaultNetworkModule.kt`

**Tasks**:
1. Create `CollectionsApi` Retrofit interface with `POST /organizations/{orgId}/collections`, `PUT /organizations/{orgId}/collections/{id}`, `DELETE /organizations/{orgId}/collections/{id}`
2. Create `CollectionJsonRequest` (name field)
3. Create `UpdateCollectionResponseJson` sealed type (Success/Invalid) following `UpdateFolderResponseJson`
4. Create `CollectionService` interface with `createCollection`, `updateCollection`, `deleteCollection`, `getCollection`
5. Create `CollectionServiceImpl` wrapping `CollectionsApi`
6. Add `collectionService: CollectionService` to `BitwardenServiceClient` and `BitwardenServiceClientImpl`
7. Provide `CollectionService` in `VaultNetworkModule`

**Verification**: `./gradlew network:compileDebugKotlin`

**Skills**: `implementing-android-code`

#### Phase 3: Collection Manager & Repository Wiring

**Goal**: Create the CollectionManager business logic layer and wire it into VaultRepository.

**Files**:
- Create: `CollectionManager.kt`, `CollectionManagerImpl.kt`, `CreateCollectionResult.kt`, `UpdateCollectionResult.kt`, `DeleteCollectionResult.kt`
- Modify: `VaultRepository.kt`, `VaultRepositoryImpl.kt`, `VaultRepositoryModule.kt`, `VaultManagerModule.kt`

**Tasks**:
1. Create sealed result classes for create/update/delete (following folder result patterns)
2. Create `CollectionManager` interface with `createCollection(organizationId, collectionView)`, `updateCollection(collectionId, organizationId, collectionView)`, `deleteCollection(collectionId, organizationId)`
3. Implement `CollectionManagerImpl` following `FolderManagerImpl` pattern: encrypt → API → disk → decrypt
4. Add `CollectionManager` provision to `VaultManagerModule`
5. Extend `VaultRepository` interface to include `CollectionManager`
6. Add `by collectionManager` delegation in `VaultRepositoryImpl`
7. Wire `CollectionManager` param in `VaultRepositoryModule`

**Verification**: `./gradlew app:compileStandardDebugKotlin`

**Skills**: `implementing-android-code`

#### Phase 4: UI Models, Navigation & String Resources

**Goal**: Create the UI models, navigation routes, and string resources needed before building screens.

**Files**:
- Create: `CollectionDisplayItem.kt`, `CollectionAddEditType.kt`, `CollectionsNavigation.kt`, `CollectionAddEditNavigation.kt`
- Modify: `SnackbarRelay.kt`, `strings.xml`

**Tasks**:
1. Add `COLLECTION_CREATED`, `COLLECTION_UPDATED`, `COLLECTION_DELETED` to `SnackbarRelay`
2. Create `CollectionDisplayItem` (id, name, organizationName)
3. Create `CollectionAddEditType` sealed class (AddItem with organizationId, EditItem with collectionId)
4. Create `CollectionsRoute` and navigation extension functions
5. Create `CollectionAddEditRoute` and navigation extension functions
6. Add string resources: "Collections", "New collection", "Edit collection", snackbar messages, delete confirmation, etc.

**Verification**: `./gradlew app:compileStandardDebugKotlin`

**Skills**: `implementing-android-code`

#### Phase 5: Collections List Screen

**Goal**: Build the CollectionsScreen and CollectionsViewModel showing all collections with org names.

**Files**:
- Create: `CollectionsScreen.kt`, `CollectionsViewModel.kt`
- Modify: `VaultSettingsScreen.kt`, `VaultSettingsViewModel.kt`, `VaultSettingsNavigation.kt`

**Tasks**:
1. Create `CollectionsViewModel` consuming `collectionsStateFlow` from VaultRepository, mapping to display items with org names
2. Create `CollectionsScreen` with Loading/Content/Error/Empty states, FAB (permission-gated), org name subtitles
3. Add "Collections" row to `VaultSettingsScreen` between Folders and Export Vault (update card styles)
4. Add `CollectionsButtonClick` action and `NavigateToCollections` event to `VaultSettingsViewModel`
5. Wire `onNavigateToCollections` in `VaultSettingsNavigation`

**Verification**: `./gradlew app:compileStandardDebugKotlin`

**Skills**: `implementing-android-code`

#### Phase 6: Collection Add/Edit Screen & Navigation Wiring

**Goal**: Build the CollectionAddEditScreen and ViewModel, wire full navigation graph.

**Files**:
- Create: `CollectionAddEditScreen.kt`, `CollectionAddEditViewModel.kt`
- Modify: `VaultUnlockedNavigation.kt`

**Tasks**:
1. Create `CollectionAddEditViewModel` with create/update/delete flows, name validation (reject `/`), snackbar relay
2. Create `CollectionAddEditScreen` with name field, save button, delete in overflow menu (edit only), loading/error dialogs
3. Register `collectionsDestination` and `collectionAddEditDestination` in `VaultUnlockedNavigation.kt`
4. Wire full navigation: VaultSettings → Collections → AddEdit

**Verification**: `./gradlew app:compileStandardDebugKotlin` and manual navigation test

**Skills**: `implementing-android-code`

### Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| SDK `encryptCollection` not available | Known | High | Stub with TODO; all other code testable |
| G1 multi-org answer changes approach | Medium | Medium | Flat list with org subtitle is easy to refactor |
| Collection API endpoints differ from web client | Low | High | Verified against web client source |
| Permission model fields not in server response | Low | High | Confirmed in SyncServiceTest JSON fixtures |
| VaultRepository delegation causes conflicts | Low | Medium | Follow exact FolderManager delegation pattern |

### Verification Plan

**Automated**:
- `./gradlew app:testStandardDebugUnitTest` after each phase
- `./gradlew app:assembleStandardDebug` for full build verification
- `./gradlew detekt` for lint checks

**Manual**:
- Navigate Settings → Vault → Collections → verify list loads
- Create a new collection → verify snackbar and list update
- Edit a collection name → verify save and snackbar
- Delete a collection → verify confirmation dialog, snackbar, and list update
- Verify FAB hidden for users without create permission
- Verify delete option hidden for users without delete permission
- Process death: enter name on add screen, rotate/kill process, verify name persists

---

## Executing This Plan

To implement this plan, run:

    /work-on-android Collection Management feature

Reference this design document during implementation for architecture decisions,
file locations, and phase ordering.
