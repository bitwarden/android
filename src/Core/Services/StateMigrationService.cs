using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;
using Bit.Core.Utilities;
using DeviceType = Bit.Core.Enums.DeviceType;
using Region = Bit.Core.Enums.Region;

namespace Bit.Core.Services
{
    public class StateMigrationService : IStateMigrationService
    {
        private readonly DeviceType _deviceType;
        private readonly IStorageService _preferencesStorageService;
        private readonly IStorageService _liteDbStorageService;
        private readonly IStorageService _secureStorageService;
        private readonly SemaphoreSlim _semaphore;

        private enum Storage
        {
            LiteDb,
            Prefs,
            Secure,
        }

        public StateMigrationService(DeviceType deviceType, IStorageService liteDbStorageService,
            IStorageService preferenceStorageService, IStorageService secureStorageService)
        {
            _deviceType = deviceType;
            _liteDbStorageService = liteDbStorageService;
            _preferencesStorageService = preferenceStorageService;
            _secureStorageService = secureStorageService;

            _semaphore = new SemaphoreSlim(1);
        }

        public async Task MigrateIfNeededAsync()
        {
            await _semaphore.WaitAsync();
            try
            {
                if (await IsMigrationNeededAsync())
                {
                    await PerformMigrationAsync();
                }
            }
            finally
            {
                _semaphore.Release();
            }
        }

        private async Task<bool> IsMigrationNeededAsync()
        {
            var lastVersion = await GetLastStateVersionAsync();
            if (lastVersion == 0)
            {
                // fresh install, set current/latest version for availability going forward
                lastVersion = Constants.LatestStateVersion;
                await SetLastStateVersionAsync(lastVersion);
            }
            return lastVersion < Constants.LatestStateVersion;
        }

        private async Task PerformMigrationAsync()
        {
            var lastVersion = await GetLastStateVersionAsync();
            switch (lastVersion)
            {
                case 1:
                    await MigrateFrom1To2Async();
                    goto case 2;
                case 2:
                    await MigrateFrom2To3Async();
                    goto case 3;
                case 3:
                    await MigrateFrom3To4Async();
                    goto case 4;
                case 4:
                    await MigrateFrom4To5Async();
                    goto case 5;
                case 5:
                    await MigrateFrom5To6Async();
                    goto case 6;
                case 6:
                    await MigrateFrom6To7Async();
                    break;
            }
        }

        #region v1 to v2 Migration

        private class V1Keys
        {
            internal const string EnvironmentUrlsKey = "environmentUrls";
        }

        private async Task MigrateFrom1To2Async()
        {
            // move environmentUrls from LiteDB to prefs
            var environmentUrls = await GetValueAsync<EnvironmentUrlData>(Storage.LiteDb, V1Keys.EnvironmentUrlsKey);
            if (environmentUrls == null)
            {
                throw new Exception("'environmentUrls' must be in LiteDB during migration from 1 to 2");
            }
            await SetValueAsync(Storage.Prefs, V2Keys.EnvironmentUrlsKey, environmentUrls);

            // Update stored version
            await SetLastStateVersionAsync(2);

            // Remove old data
            await RemoveValueAsync(Storage.LiteDb, V1Keys.EnvironmentUrlsKey);
        }

        #endregion

        #region v2 to v3 Migration

        private class V2Keys
        {
            internal const string SyncOnRefreshKey = "syncOnRefresh";
            internal const string VaultTimeoutKey = "lockOption";
            internal const string VaultTimeoutActionKey = "vaultTimeoutAction";
            internal const string LastActiveTimeKey = "lastActiveTime";
            internal const string BiometricUnlockKey = "fingerprintUnlock";
            internal const string ProtectedPin = "protectedPin";
            internal const string PinProtectedKey = "pinProtectedKey";
            internal const string DefaultUriMatch = "defaultUriMatch";
            internal const string DisableAutoTotpCopyKey = "disableAutoTotpCopy";
            internal const string EnvironmentUrlsKey = "environmentUrls";
            internal const string AutofillDisableSavePromptKey = "autofillDisableSavePrompt";
            internal const string AutofillBlacklistedUrisKey = "autofillBlacklistedUris";
            internal const string DisableFaviconKey = "disableFavicon";
            internal const string ThemeKey = "theme";
            internal const string ClearClipboardKey = "clearClipboard";
            internal const string PreviousPageKey = "previousPage";
            internal const string InlineAutofillEnabledKey = "inlineAutofillEnabled";
            internal const string InvalidUnlockAttempts = "invalidUnlockAttempts";
            internal const string PasswordRepromptAutofillKey = "passwordRepromptAutofillKey";
            internal const string PasswordVerifiedAutofillKey = "passwordVerifiedAutofillKey";
            internal const string MigratedFromV1 = "migratedFromV1";
            internal const string MigratedFromV1AutofillPromptShown = "migratedV1AutofillPromptShown";
            internal const string TriedV1Resync = "triedV1Resync";
            internal const string Keys_UserId = "userId";
            internal const string Keys_UserEmail = "userEmail";
            internal const string Keys_Stamp = "securityStamp";
            internal const string Keys_Kdf = "kdf";
            internal const string Keys_KdfIterations = "kdfIterations";
            internal const string Keys_EmailVerified = "emailVerified";
            internal const string Keys_ForcePasswordReset = "forcePasswordReset";
            internal const string Keys_AccessToken = "accessToken";
            internal const string Keys_RefreshToken = "refreshToken";
            internal const string Keys_LocalData = "ciphersLocalData";
            internal const string Keys_NeverDomains = "neverDomains";
            internal const string Keys_Key = "key";
            internal const string Keys_EncOrgKeys = "encOrgKeys";
            internal const string Keys_EncPrivateKey = "encPrivateKey";
            internal const string Keys_EncKey = "encKey";
            internal const string Keys_KeyHash = "keyHash";
            internal const string Keys_UsesKeyConnector = "usesKeyConnector";
            internal const string Keys_PassGenOptions = "passwordGenerationOptions";
            internal const string Keys_PassGenHistory = "generatedPasswordHistory";
        }

        private async Task MigrateFrom2To3Async()
        {
            // build account and state
            var userId = await GetValueAsync<string>(Storage.LiteDb, V2Keys.Keys_UserId);
            var email = await GetValueAsync<string>(Storage.LiteDb, V2Keys.Keys_UserEmail);
            string name = null;
            var hasPremiumPersonally = false;
            var accessToken = await GetValueAsync<string>(Storage.LiteDb, V2Keys.Keys_AccessToken);
            if (!string.IsNullOrWhiteSpace(accessToken))
            {
                var tokenService = ServiceContainer.Resolve<ITokenService>("tokenService");
                await tokenService.SetAccessTokenAsync(accessToken, true);

                if (string.IsNullOrWhiteSpace(userId))
                {
                    userId = tokenService.GetUserId();
                }
                if (string.IsNullOrWhiteSpace(email))
                {
                    email = tokenService.GetEmail();
                }
                name = tokenService.GetName();
                hasPremiumPersonally = tokenService.GetPremium();
            }
            if (string.IsNullOrWhiteSpace(userId))
            {
                throw new Exception("'userId' must be in LiteDB during migration from 2 to 3");
            }

            var kdfType = await GetValueAsync<int?>(Storage.LiteDb, V2Keys.Keys_Kdf);
            var kdfIterations = await GetValueAsync<int?>(Storage.LiteDb, V2Keys.Keys_KdfIterations);
            var stamp = await GetValueAsync<string>(Storage.LiteDb, V2Keys.Keys_Stamp);
            var emailVerified = await GetValueAsync<bool?>(Storage.LiteDb, V2Keys.Keys_EmailVerified);
            var refreshToken = await GetValueAsync<string>(Storage.LiteDb, V2Keys.Keys_RefreshToken);
            var account = new Account(
                new Account.AccountProfile()
                {
                    UserId = userId,
                    Email = email,
                    Name = name,
                    Stamp = stamp,
                    KdfType = (KdfType?)kdfType,
                    KdfIterations = kdfIterations,
                    EmailVerified = emailVerified,
                    HasPremiumPersonally = hasPremiumPersonally,
                },
                new Account.AccountTokens()
                {
                    AccessToken = accessToken,
                    RefreshToken = refreshToken,
                }
            );
            var environmentUrls = await GetValueAsync<EnvironmentUrlData>(Storage.Prefs, V2Keys.EnvironmentUrlsKey);
            var vaultTimeout = await GetValueAsync<int?>(Storage.Prefs, V2Keys.VaultTimeoutKey);
            var vaultTimeoutAction = await GetValueAsync<string>(Storage.Prefs, V2Keys.VaultTimeoutActionKey);
            account.Settings = new Account.AccountSettings()
            {
                EnvironmentUrls = environmentUrls,
                VaultTimeout = vaultTimeout,
                VaultTimeoutAction =
                    vaultTimeoutAction == "logout" ? VaultTimeoutAction.Logout : VaultTimeoutAction.Lock
            };
            var state = new State { Accounts = new Dictionary<string, Account> { [userId] = account } };
            state.ActiveUserId = userId;
            await SetValueAsync(Storage.LiteDb, Constants.StateKey, state);

            // migrate user-specific non-state data
            var syncOnRefresh = await GetValueAsync<bool?>(Storage.LiteDb, V2Keys.SyncOnRefreshKey);
            await SetValueAsync(Storage.LiteDb, V3Keys.SyncOnRefreshKey(userId), syncOnRefresh);
            var lastActiveTime = await GetValueAsync<long?>(Storage.Prefs, V2Keys.LastActiveTimeKey);
            await SetValueAsync(Storage.LiteDb, V3Keys.LastActiveTimeKey(userId), lastActiveTime);
            var biometricUnlock = await GetValueAsync<bool?>(Storage.LiteDb, V2Keys.BiometricUnlockKey);
            await SetValueAsync(Storage.LiteDb, V3Keys.BiometricUnlockKey(userId), biometricUnlock);
            var protectedPin = await GetValueAsync<string>(Storage.LiteDb, V2Keys.ProtectedPin);
            await SetValueAsync(Storage.LiteDb, V3Keys.ProtectedPinKey(userId), protectedPin);
            var pinProtectedKey = await GetValueAsync<string>(Storage.LiteDb, V2Keys.PinProtectedKey);
            await SetValueAsync(Storage.LiteDb, V3Keys.PinProtectedKey(userId), pinProtectedKey);
            var defaultUriMatch = await GetValueAsync<int?>(Storage.Prefs, V2Keys.DefaultUriMatch);
            await SetValueAsync(Storage.LiteDb, V3Keys.DefaultUriMatchKey(userId), defaultUriMatch);
            var disableAutoTotpCopy = await GetValueAsync<bool?>(Storage.Prefs, V2Keys.DisableAutoTotpCopyKey);
            await SetValueAsync(Storage.LiteDb, V3Keys.DisableAutoTotpCopyKey(userId), disableAutoTotpCopy);
            var autofillDisableSavePrompt =
                await GetValueAsync<bool?>(Storage.Prefs, V2Keys.AutofillDisableSavePromptKey);
            await SetValueAsync(Storage.LiteDb, V3Keys.AutofillDisableSavePromptKey(userId),
                autofillDisableSavePrompt);
            var autofillBlacklistedUris =
                await GetValueAsync<List<string>>(Storage.LiteDb, V2Keys.AutofillBlacklistedUrisKey);
            await SetValueAsync(Storage.LiteDb, V3Keys.AutofillBlacklistedUrisKey(userId), autofillBlacklistedUris);
            var disableFavicon = await GetValueAsync<bool?>(Storage.Prefs, V2Keys.DisableFaviconKey);
            await SetValueAsync(Storage.LiteDb, V3Keys.DisableFaviconKey(userId), disableFavicon);
            var theme = await GetValueAsync<string>(Storage.Prefs, V2Keys.ThemeKey);
            await SetValueAsync(Storage.LiteDb, V3Keys.ThemeKey(userId), theme);
            var clearClipboard = await GetValueAsync<int?>(Storage.Prefs, V2Keys.ClearClipboardKey);
            await SetValueAsync(Storage.LiteDb, V3Keys.ClearClipboardKey(userId), clearClipboard);
            var previousPage = await GetValueAsync<PreviousPageInfo>(Storage.LiteDb, V2Keys.PreviousPageKey);
            await SetValueAsync(Storage.LiteDb, V3Keys.PreviousPageKey(userId), previousPage);
            var inlineAutofillEnabled = await GetValueAsync<bool?>(Storage.Prefs, V2Keys.InlineAutofillEnabledKey);
            await SetValueAsync(Storage.LiteDb, V3Keys.InlineAutofillEnabledKey(userId), inlineAutofillEnabled);
            var invalidUnlockAttempts = await GetValueAsync<int?>(Storage.Prefs, V2Keys.InvalidUnlockAttempts);
            await SetValueAsync(Storage.LiteDb, V3Keys.InvalidUnlockAttemptsKey(userId), invalidUnlockAttempts);
            var passwordRepromptAutofill =
                await GetValueAsync<bool?>(Storage.LiteDb, V2Keys.PasswordRepromptAutofillKey);
            await SetValueAsync(Storage.LiteDb, V3Keys.PasswordRepromptAutofillKey(userId),
                passwordRepromptAutofill);
            var passwordVerifiedAutofill =
                await GetValueAsync<bool?>(Storage.LiteDb, V2Keys.PasswordVerifiedAutofillKey);
            await SetValueAsync(Storage.LiteDb, V3Keys.PasswordVerifiedAutofillKey(userId),
                passwordVerifiedAutofill);
            var cipherLocalData = await GetValueAsync<Dictionary<string, Dictionary<string, object>>>(Storage.LiteDb,
                V2Keys.Keys_LocalData);
            await SetValueAsync(Storage.LiteDb, V3Keys.LocalDataKey(userId), cipherLocalData);
            var neverDomains = await GetValueAsync<HashSet<string>>(Storage.LiteDb, V2Keys.Keys_NeverDomains);
            await SetValueAsync(Storage.LiteDb, V3Keys.NeverDomainsKey(userId), neverDomains);
            var key = await GetValueAsync<string>(Storage.Secure, V2Keys.Keys_Key);
            await SetValueAsync(Storage.Secure, V3Keys.KeyKey(userId), key);
            var encOrgKeys = await GetValueAsync<Dictionary<string, string>>(Storage.LiteDb, V2Keys.Keys_EncOrgKeys);
            await SetValueAsync(Storage.LiteDb, V3Keys.EncOrgKeysKey(userId), encOrgKeys);
            var encPrivateKey = await GetValueAsync<string>(Storage.LiteDb, V2Keys.Keys_EncPrivateKey);
            await SetValueAsync(Storage.LiteDb, V3Keys.EncPrivateKeyKey(userId), encPrivateKey);
            var encKey = await GetValueAsync<string>(Storage.LiteDb, V2Keys.Keys_EncKey);
            await SetValueAsync(Storage.LiteDb, V3Keys.EncKeyKey(userId), encKey);
            var keyHash = await GetValueAsync<string>(Storage.LiteDb, V2Keys.Keys_KeyHash);
            await SetValueAsync(Storage.LiteDb, V3Keys.KeyHashKey(userId), keyHash);
            var usesKeyConnector = await GetValueAsync<bool?>(Storage.LiteDb, V2Keys.Keys_UsesKeyConnector);
            await SetValueAsync(Storage.LiteDb, V3Keys.UsesKeyConnectorKey(userId), usesKeyConnector);
            var passGenOptions =
                await GetValueAsync<PasswordGenerationOptions>(Storage.LiteDb, V2Keys.Keys_PassGenOptions);
            await SetValueAsync(Storage.LiteDb, V3Keys.PassGenOptionsKey(userId), passGenOptions);
            var passGenHistory =
                await GetValueAsync<List<GeneratedPasswordHistory>>(Storage.LiteDb, V2Keys.Keys_PassGenHistory);
            await SetValueAsync(Storage.LiteDb, V3Keys.PassGenHistoryKey(userId), passGenHistory);

            // migrate global non-state data
            await SetValueAsync(Storage.Prefs, V3Keys.PreAuthEnvironmentUrlsKey, environmentUrls);

            // Update stored version
            await SetLastStateVersionAsync(3);

            // Remove old data
            await RemoveValueAsync(Storage.LiteDb, V2Keys.Keys_UserId);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.Keys_UserEmail);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.Keys_AccessToken);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.Keys_RefreshToken);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.Keys_Kdf);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.Keys_KdfIterations);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.Keys_Stamp);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.Keys_EmailVerified);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.Keys_ForcePasswordReset);
            await RemoveValueAsync(Storage.Prefs, V2Keys.EnvironmentUrlsKey);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.PinProtectedKey);
            await RemoveValueAsync(Storage.Prefs, V2Keys.VaultTimeoutKey);
            await RemoveValueAsync(Storage.Prefs, V2Keys.VaultTimeoutActionKey);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.SyncOnRefreshKey);
            await RemoveValueAsync(Storage.Prefs, V2Keys.LastActiveTimeKey);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.BiometricUnlockKey);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.ProtectedPin);
            await RemoveValueAsync(Storage.Prefs, V2Keys.DefaultUriMatch);
            await RemoveValueAsync(Storage.Prefs, V2Keys.DisableAutoTotpCopyKey);
            await RemoveValueAsync(Storage.Prefs, V2Keys.AutofillDisableSavePromptKey);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.AutofillBlacklistedUrisKey);
            await RemoveValueAsync(Storage.Prefs, V2Keys.DisableFaviconKey);
            await RemoveValueAsync(Storage.Prefs, V2Keys.ThemeKey);
            await RemoveValueAsync(Storage.Prefs, V2Keys.ClearClipboardKey);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.PreviousPageKey);
            await RemoveValueAsync(Storage.Prefs, V2Keys.InlineAutofillEnabledKey);
            await RemoveValueAsync(Storage.Prefs, V2Keys.InvalidUnlockAttempts);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.PasswordRepromptAutofillKey);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.PasswordVerifiedAutofillKey);
            await RemoveValueAsync(Storage.Prefs, V2Keys.MigratedFromV1);
            await RemoveValueAsync(Storage.Prefs, V2Keys.MigratedFromV1AutofillPromptShown);
            await RemoveValueAsync(Storage.Prefs, V2Keys.TriedV1Resync);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.Keys_LocalData);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.Keys_NeverDomains);
            await RemoveValueAsync(Storage.Secure, V2Keys.Keys_Key);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.Keys_EncOrgKeys);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.Keys_EncPrivateKey);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.Keys_EncKey);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.Keys_KeyHash);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.Keys_UsesKeyConnector);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.Keys_PassGenOptions);
            await RemoveValueAsync(Storage.LiteDb, V2Keys.Keys_PassGenHistory);
        }

        #endregion

        #region v3 to v4 Migration

        private class V3Keys
        {
            internal const string PreAuthEnvironmentUrlsKey = "preAuthEnvironmentUrls";
            internal static string LocalDataKey(string userId) => $"ciphersLocalData_{userId}";
            internal static string NeverDomainsKey(string userId) => $"neverDomains_{userId}";
            internal static string KeyKey(string userId) => $"key_{userId}";
            internal static string EncOrgKeysKey(string userId) => $"encOrgKeys_{userId}";
            internal static string EncPrivateKeyKey(string userId) => $"encPrivateKey_{userId}";
            internal static string EncKeyKey(string userId) => $"encKey_{userId}";
            internal static string KeyHashKey(string userId) => $"keyHash_{userId}";
            internal static string PinProtectedKey(string userId) => $"pinProtectedKey_{userId}";
            internal static string PassGenOptionsKey(string userId) => $"passwordGenerationOptions_{userId}";
            internal static string PassGenHistoryKey(string userId) => $"generatedPasswordHistory_{userId}";
            internal static string LastActiveTimeKey(string userId) => $"lastActiveTime_{userId}";
            internal static string InvalidUnlockAttemptsKey(string userId) => $"invalidUnlockAttempts_{userId}";
            internal static string InlineAutofillEnabledKey(string userId) => $"inlineAutofillEnabled_{userId}";
            internal static string AutofillDisableSavePromptKey(string userId) => $"autofillDisableSavePrompt_{userId}";
            internal static string AutofillBlacklistedUrisKey(string userId) => $"autofillBlacklistedUris_{userId}";
            internal static string ClearClipboardKey(string userId) => $"clearClipboard_{userId}";
            internal static string SyncOnRefreshKey(string userId) => $"syncOnRefresh_{userId}";
            internal static string DefaultUriMatchKey(string userId) => $"defaultUriMatch_{userId}";
            internal static string DisableAutoTotpCopyKey(string userId) => $"disableAutoTotpCopy_{userId}";
            internal static string PreviousPageKey(string userId) => $"previousPage_{userId}";

            internal static string PasswordRepromptAutofillKey(string userId) =>
                $"passwordRepromptAutofillKey_{userId}";

            internal static string PasswordVerifiedAutofillKey(string userId) =>
                $"passwordVerifiedAutofillKey_{userId}";

            internal static string UsesKeyConnectorKey(string userId) => $"usesKeyConnector_{userId}";
            internal static string ProtectedPinKey(string userId) => $"protectedPin_{userId}";
            internal static string BiometricUnlockKey(string userId) => $"biometricUnlock_{userId}";
            internal static string ThemeKey(string userId) => $"theme_{userId}";
            internal static string AutoDarkThemeKey(string userId) => $"autoDarkTheme_{userId}";
            internal static string DisableFaviconKey(string userId) => $"disableFavicon_{userId}";
        }

        private async Task MigrateFrom3To4Async()
        {
            var state = await GetValueAsync<State>(Storage.LiteDb, Constants.StateKey);
            if (state?.Accounts is null)
            {
                // Update stored version
                await SetLastStateVersionAsync(4);
                return;
            }

            string firstUserId = null;

            // move values from state to standalone values in LiteDB
            foreach (var account in state.Accounts.Where(a => a.Value?.Profile?.UserId != null))
            {
                var userId = account.Value.Profile.UserId;
                if (firstUserId == null)
                {
                    firstUserId = userId;
                }
                var vaultTimeout = account.Value.Settings?.VaultTimeout;
                await SetValueAsync(Storage.LiteDb, V4Keys.VaultTimeoutKey(userId), vaultTimeout);

                var vaultTimeoutAction = account.Value.Settings?.VaultTimeoutAction;
                await SetValueAsync(Storage.LiteDb, V4Keys.VaultTimeoutActionKey(userId), vaultTimeoutAction);

                var screenCaptureAllowed = account.Value.Settings?.ScreenCaptureAllowed;
                await SetValueAsync(Storage.LiteDb, V4Keys.ScreenCaptureAllowedKey(userId), screenCaptureAllowed);
            }

            // use values from first userId to apply globals
            if (firstUserId != null)
            {
                var theme = await GetValueAsync<string>(Storage.LiteDb, V3Keys.ThemeKey(firstUserId));
                await SetValueAsync(Storage.LiteDb, V4Keys.ThemeKey, theme);

                var autoDarkTheme = await GetValueAsync<string>(Storage.LiteDb, V3Keys.AutoDarkThemeKey(firstUserId));
                await SetValueAsync(Storage.LiteDb, V4Keys.AutoDarkThemeKey, autoDarkTheme);

                var disableFavicon = await GetValueAsync<bool?>(Storage.LiteDb, V3Keys.DisableFaviconKey(firstUserId));
                await SetValueAsync(Storage.LiteDb, V4Keys.DisableFaviconKey, disableFavicon);
            }

            // Update stored version
            await SetLastStateVersionAsync(4);

            // Remove old data
            foreach (var account in state.Accounts)
            {
                var userId = account.Value?.Profile?.UserId;
                if (userId != null)
                {
                    await RemoveValueAsync(Storage.LiteDb, V3Keys.ThemeKey(userId));
                    await RemoveValueAsync(Storage.LiteDb, V3Keys.AutoDarkThemeKey(userId));
                    await RemoveValueAsync(Storage.LiteDb, V3Keys.DisableFaviconKey(userId));
                }
            }

            // Removal of old state data will happen organically as state is rebuilt in app
        }

        #endregion

        #region v4 to v5 Migration

        private class V4Keys
        {
            internal static string VaultTimeoutKey(string userId) => $"vaultTimeout_{userId}";
            internal static string VaultTimeoutActionKey(string userId) => $"vaultTimeoutAction_{userId}";
            internal static string ScreenCaptureAllowedKey(string userId) => $"screenCaptureAllowed_{userId}";
            internal const string ThemeKey = "theme";
            internal const string AutoDarkThemeKey = "autoDarkTheme";
            internal const string DisableFaviconKey = "disableFavicon";
            internal const string BiometricIntegrityKey = "biometricIntegrityState";
            internal const string iOSAutoFillBiometricIntegrityKey = "iOSAutoFillBiometricIntegrityState";
            internal const string iOSExtensionBiometricIntegrityKey = "iOSExtensionBiometricIntegrityState";
            internal const string iOSShareExtensionBiometricIntegrityKey = "iOSShareExtensionBiometricIntegrityState";
        }

        private async Task MigrateFrom4To5Async()
        {
            var bioIntegrityState = await GetValueAsync<string>(Storage.Prefs, V4Keys.BiometricIntegrityKey);
            var iOSAutofillBioIntegrityState =
                await GetValueAsync<string>(Storage.Prefs, V4Keys.iOSAutoFillBiometricIntegrityKey);
            var iOSExtensionBioIntegrityState =
                await GetValueAsync<string>(Storage.Prefs, V4Keys.iOSExtensionBiometricIntegrityKey);
            var iOSShareExtensionBioIntegrityState =
                await GetValueAsync<string>(Storage.Prefs, V4Keys.iOSShareExtensionBiometricIntegrityKey);

            if (_deviceType == DeviceType.Android && string.IsNullOrWhiteSpace(bioIntegrityState))
            {
                bioIntegrityState = Guid.NewGuid().ToString();
            }

            await SetValueAsync(Storage.Prefs, V5Keys.BiometricIntegritySourceKey, bioIntegrityState);

            if (_deviceType == DeviceType.iOS)
            {
                await SetValueAsync(Storage.Prefs, V5Keys.iOSAutoFillBiometricIntegritySourceKey,
                    iOSAutofillBioIntegrityState);
                await SetValueAsync(Storage.Prefs, V5Keys.iOSExtensionBiometricIntegritySourceKey,
                    iOSExtensionBioIntegrityState);
                await SetValueAsync(Storage.Prefs, V5Keys.iOSShareExtensionBiometricIntegritySourceKey,
                    iOSShareExtensionBioIntegrityState);
            }

            var state = await GetValueAsync<State>(Storage.LiteDb, Constants.StateKey);
            if (state?.Accounts is null)
            {
                // No accounts available, update stored version and exit
                await SetLastStateVersionAsync(5);
                return;
            }

            // build integrity keys for existing users
            foreach (var account in state.Accounts.Where(a => a.Value?.Profile?.UserId != null))
            {
                var userId = account.Value.Profile.UserId;

                await SetValueAsync(Storage.LiteDb,
                    V5Keys.AccountBiometricIntegrityValidKey(userId, bioIntegrityState), true);

                if (_deviceType == DeviceType.iOS)
                {
                    await SetValueAsync(Storage.LiteDb,
                        V5Keys.AccountBiometricIntegrityValidKey(userId, iOSAutofillBioIntegrityState), true);
                    await SetValueAsync(Storage.LiteDb,
                        V5Keys.AccountBiometricIntegrityValidKey(userId, iOSExtensionBioIntegrityState), true);
                    await SetValueAsync(Storage.LiteDb,
                        V5Keys.AccountBiometricIntegrityValidKey(userId, iOSShareExtensionBioIntegrityState), true);
                }
            }

            // Update stored version
            await SetLastStateVersionAsync(5);

            // Remove old data
            await RemoveValueAsync(Storage.Prefs, V4Keys.BiometricIntegrityKey);
            await RemoveValueAsync(Storage.Prefs, V4Keys.iOSAutoFillBiometricIntegrityKey);
            await RemoveValueAsync(Storage.Prefs, V4Keys.iOSExtensionBiometricIntegrityKey);
            await RemoveValueAsync(Storage.Prefs, V4Keys.iOSShareExtensionBiometricIntegrityKey);
        }

        private class V5Keys
        {
            internal const string BiometricIntegritySourceKey = "biometricIntegritySource";
            internal const string iOSAutoFillBiometricIntegritySourceKey = "iOSAutoFillBiometricIntegritySource";
            internal const string iOSExtensionBiometricIntegritySourceKey = "iOSExtensionBiometricIntegritySource";

            internal const string iOSShareExtensionBiometricIntegritySourceKey =
                "iOSShareExtensionBiometricIntegritySource";

            internal static string AccountBiometricIntegrityValidKey(string userId, string systemBioIntegrityState) =>
                $"accountBiometricIntegrityValid_{userId}_{systemBioIntegrityState}";
        }

        #endregion

        #region v5 to v6 Migration

        private async Task MigrateFrom5To6Async()
        {
            // global data
            await MoveToPrefsAsync<string>(V6Keys.AppIdKey);
            await MoveToPrefsAsync<bool?>(V6Keys.DisableFaviconKey);
            await MoveToPrefsAsync<string>(V6Keys.ThemeKey);
            await MoveToPrefsAsync<string>(V6Keys.AutoDarkThemeKey);
            await MoveToPrefsAsync<V6Keys.PasswordlessRequestNotification>(V6Keys.PasswordlessLoginNotificationKey);
            await MoveToPrefsAsync<string>(V6Keys.PreLoginEmailKey);
            await MoveToPrefsAsync<bool?>(V6Keys.LastUserShouldConnectToWatchKey);
            await MoveToPrefsAsync<string>(V6Keys.PushInstallationRegistrationErrorKey);
            await MoveToPrefsAsync<bool?>(V6Keys.AutofillNeedsIdentityReplacementKey);

            // account data
            var state = await GetValueAsync<State>(Storage.LiteDb, Constants.StateKey);
            if (state?.Accounts != null)
            {
                await SetValueAsync(Storage.Prefs, Constants.StateKey, state);

                foreach (var account in state.Accounts.Where(a => a.Value?.Profile?.UserId != null))
                {
                    var userId = account.Value.Profile.UserId;

                    await MoveToPrefsAsync<bool?>(V6Keys.BiometricUnlockKey(userId));
                    await MoveToPrefsAsync<string>(V6Keys.ProtectedPinKey(userId));
                    await MoveToPrefsAsync<string>(V6Keys.PinKeyEncryptedUserKeyKey(userId));
                    await MoveToPrefsAsync<string>(V6Keys.KeyHashKey(userId));
                    await MoveToPrefsAsync<string>(V6Keys.MasterKeyEncryptedUserKeyKey(userId));
                    await MoveToPrefsAsync<Dictionary<string, string>>(V6Keys.EncOrgKeysKey(userId));
                    await MoveToPrefsAsync<string>(V6Keys.EncPrivateKeyKey(userId));
                    await MoveToPrefsAsync<List<string>>(V6Keys.AutofillBlacklistedUrisKey(userId));
                    await MoveToPrefsAsync<long?>(V6Keys.LastActiveTimeKey(userId));
                    await MoveToPrefsAsync<int?>(V6Keys.VaultTimeoutKey(userId));
                    await MoveToPrefsAsync<V6Keys.VaultTimeoutAction?>(V6Keys.VaultTimeoutActionKey(userId));
                    await MoveToPrefsAsync<bool?>(V6Keys.ScreenCaptureAllowedKey(userId));
                    await MoveToPrefsAsync<V6Keys.PreviousPageInfo>(V6Keys.PreviousPageKey(userId));
                    await MoveToPrefsAsync<int>(V6Keys.InvalidUnlockAttemptsKey(userId));
                    await MoveToPrefsAsync<bool?>(V6Keys.DisableAutoTotpCopyKey(userId));
                    await MoveToPrefsAsync<bool?>(V6Keys.InlineAutofillEnabledKey(userId));
                    await MoveToPrefsAsync<bool?>(V6Keys.AutofillDisableSavePromptKey(userId));
                    await MoveToPrefsAsync<int?>(V6Keys.DefaultUriMatchKey(userId));
                    await MoveToPrefsAsync<int?>(V6Keys.ClearClipboardKey(userId));
                    await MoveToPrefsAsync<bool?>(V6Keys.PasswordRepromptAutofillKey(userId));
                    await MoveToPrefsAsync<bool?>(V6Keys.PasswordVerifiedAutofillKey(userId));
                    await MoveToPrefsAsync<DateTime?>(V6Keys.LastSyncKey(userId));
                    await MoveToPrefsAsync<bool?>(V6Keys.SyncOnRefreshKey(userId));
                    await MoveToPrefsAsync<DateTime?>(V6Keys.PushLastRegistrationDateKey(userId));
                    await MoveToPrefsAsync<string>(V6Keys.PushCurrentTokenKey(userId));
                    await MoveToPrefsAsync<Dictionary<string, V6Keys.PolicyData>>(V6Keys.PoliciesKey(userId));
                    await MoveToPrefsAsync<bool?>(V6Keys.UsesKeyConnectorKey(userId));
                    await MoveToPrefsAsync<Dictionary<string, V6Keys.OrganizationData>>(
                        V6Keys.OrganizationsKey(userId));
                    await MoveToPrefsAsync<V6Keys.PasswordGenerationOptions>(V6Keys.PassGenOptionsKey(userId));
                    await MoveToPrefsAsync<V6Keys.UsernameGenerationOptions>(V6Keys.UsernameGenOptionsKey(userId));
                    await MoveToPrefsAsync<string>(V6Keys.TwoFactorTokenKey(userId));
                    await MoveToPrefsAsync<bool?>(V6Keys.ApprovePasswordlessLoginsKey(userId));
                    await MoveToPrefsAsync<bool?>(V6Keys.ShouldConnectToWatchKey(userId));
                    await MoveToPrefsAsync<string>(V6Keys.DeviceKeyKey(userId));
                }
            }

            await RemoveValueAsync(Storage.LiteDb, Constants.StateKey);
            await RemoveValueAsync(Storage.LiteDb, V6Keys.LastActiveTimeKey(""));

            // Update stored version
            await SetLastStateVersionAsync(6);
        }

        private class V6Keys
        {
            // global keys

            internal const string AppIdKey = "appId";
            internal const string DisableFaviconKey = "disableFavicon";
            internal const string ThemeKey = "theme";
            internal const string AutoDarkThemeKey = "autoDarkTheme";
            internal const string PasswordlessLoginNotificationKey = "passwordlessLoginNotificationKey";
            internal const string PreLoginEmailKey = "preLoginEmailKey";
            internal const string LastUserShouldConnectToWatchKey = "lastUserShouldConnectToWatch";
            internal const string PushInstallationRegistrationErrorKey = "pushInstallationRegistrationError";
            internal const string AutofillNeedsIdentityReplacementKey = "autofillNeedsIdentityReplacement";

            // account keys

            internal static string BiometricUnlockKey(string userId) => $"biometricUnlock_{userId}";
            internal static string ProtectedPinKey(string userId) => $"protectedPin_{userId}";
            internal static string PinKeyEncryptedUserKeyKey(string userId) => $"pinKeyEncryptedUserKey_{userId}";
            internal static string KeyHashKey(string userId) => $"keyHash_{userId}";
            internal static string MasterKeyEncryptedUserKeyKey(string userId) => $"masterKeyEncryptedUserKey_{userId}";
            internal static string EncOrgKeysKey(string userId) => $"encOrgKeys_{userId}";
            internal static string EncPrivateKeyKey(string userId) => $"encPrivateKey_{userId}";
            internal static string AutofillBlacklistedUrisKey(string userId) => $"autofillBlacklistedUris_{userId}";
            internal static string LastActiveTimeKey(string userId) => $"lastActiveTime_{userId}";
            internal static string VaultTimeoutKey(string userId) => $"vaultTimeout_{userId}";
            internal static string VaultTimeoutActionKey(string userId) => $"vaultTimeoutAction_{userId}";
            internal static string ScreenCaptureAllowedKey(string userId) => $"screenCaptureAllowed_{userId}";
            internal static string PreviousPageKey(string userId) => $"previousPage_{userId}";
            internal static string InvalidUnlockAttemptsKey(string userId) => $"invalidUnlockAttempts_{userId}";
            internal static string DisableAutoTotpCopyKey(string userId) => $"disableAutoTotpCopy_{userId}";
            internal static string InlineAutofillEnabledKey(string userId) => $"inlineAutofillEnabled_{userId}";
            internal static string AutofillDisableSavePromptKey(string userId) => $"autofillDisableSavePrompt_{userId}";
            internal static string DefaultUriMatchKey(string userId) => $"defaultUriMatch_{userId}";
            internal static string ClearClipboardKey(string userId) => $"clearClipboard_{userId}";
            internal static string PasswordRepromptAutofillKey(string userId) => $"passwordRepromptAutofillKey_{userId}";
            internal static string PasswordVerifiedAutofillKey(string userId) => $"passwordVerifiedAutofillKey_{userId}";
            internal static string LastSyncKey(string userId) => $"lastSync_{userId}";
            internal static string SyncOnRefreshKey(string userId) => $"syncOnRefresh_{userId}";
            internal static string PushLastRegistrationDateKey(string userId) => $"pushLastRegistrationDate_{userId}";
            internal static string PushCurrentTokenKey(string userId) => $"pushCurrentToken_{userId}";
            internal static string PoliciesKey(string userId) => $"policies_{userId}";
            internal static string UsesKeyConnectorKey(string userId) => $"usesKeyConnector_{userId}";
            internal static string OrganizationsKey(string userId) => $"organizations_{userId}";
            internal static string PassGenOptionsKey(string userId) => $"passwordGenerationOptions_{userId}";
            internal static string UsernameGenOptionsKey(string userId) => $"usernameGenerationOptions_{userId}";
            internal static string TwoFactorTokenKey(string email) => $"twoFactorToken_{email}";
            internal static string ApprovePasswordlessLoginsKey(string userId) => $"approvePasswordlessLogins_{userId}";
            internal static string ShouldConnectToWatchKey(string userId) => $"shouldConnectToWatch_{userId}";
            internal static string DeviceKeyKey(string userId) => $"deviceKey_{userId}";

            // objects

            internal class PasswordlessRequestNotification
            {
                public string UserId { get; set; }
                public string Id { get; set; }
            }

            internal enum VaultTimeoutAction
            {
                Lock = 0,
                Logout = 1,
            }

            internal class PreviousPageInfo
            {
                public string Page { get; set; }
                public string CipherId { get; set; }
                public string SendId { get; set; }
                public string SearchText { get; set; }
            }

            internal class PolicyData
            {
                public string Id { get; set; }
                public string OrganizationId { get; set; }
                public V6Keys.PolicyType Type { get; set; }
                public Dictionary<string, object> Data { get; set; }
                public bool Enabled { get; set; }
            }

            internal enum PolicyType : byte
            {
                TwoFactorAuthentication = 0,
                MasterPassword = 1,
                PasswordGenerator = 2,
                OnlyOrg = 3,
                RequireSso = 4,
                PersonalOwnership = 5,
                DisableSend = 6,
                SendOptions = 7,
                ResetPassword = 8,
                MaximumVaultTimeout = 9,
                DisablePersonalVaultExport = 10,
            }

            internal class OrganizationData
            {
                public string Id { get; set; }
                public string Name { get; set; }
                public V6Keys.OrganizationUserStatusType Status { get; set; }
                public V6Keys.OrganizationUserType Type { get; set; }
                public bool Enabled { get; set; }
                public bool UseGroups { get; set; }
                public bool UseDirectory { get; set; }
                public bool UseEvents { get; set; }
                public bool UseTotp { get; set; }
                public bool Use2fa { get; set; }
                public bool UseApi { get; set; }
                public bool UsePolicies { get; set; }
                public bool SelfHost { get; set; }
                public bool UsersGetPremium { get; set; }
                public int? Seats { get; set; }
                public short? MaxCollections { get; set; }
                public short? MaxStorageGb { get; set; }
                public V6Keys.Permissions Permissions { get; set; } = new Permissions();
                public string Identifier { get; set; }
                public bool UsesKeyConnector { get; set; }
                public string KeyConnectorUrl { get; set; }
            }

            internal enum OrganizationUserStatusType : byte
            {
                Invited = 0,
                Accepted = 1,
                Confirmed = 2
            }

            internal enum OrganizationUserType : byte
            {
                Owner = 0,
                Admin = 1,
                User = 2,
                Manager = 3,
                Custom = 4,
            }

            internal class Permissions
            {
                public bool AccessBusinessPortal { get; set; }
                public bool AccessEventLogs { get; set; }
                public bool AccessImportExport { get; set; }
                public bool AccessReports { get; set; }
                public bool EditAssignedCollections { get; set; }
                public bool DeleteAssignedCollections { get; set; }
                public bool CreateNewCollections { get; set; }
                public bool EditAnyCollection { get; set; }
                public bool DeleteAnyCollection { get; set; }
                public bool ManageGroups { get; set; }
                public bool ManagePolicies { get; set; }
                public bool ManageSso { get; set; }
                public bool ManageUsers { get; set; }
            }

            internal class PasswordGenerationOptions
            {
                public int? Length { get; set; }
                public bool? AllowAmbiguousChar { get; set; }
                public bool? Number { get; set; }
                public int? MinNumber { get; set; }
                public bool? Uppercase { get; set; }
                public int? MinUppercase { get; set; }
                public bool? Lowercase { get; set; }
                public int? MinLowercase { get; set; }
                public bool? Special { get; set; }
                public int? MinSpecial { get; set; }
                public string Type { get; set; }
                public int? NumWords { get; set; }
                public string WordSeparator { get; set; }
                public bool? Capitalize { get; set; }
                public bool? IncludeNumber { get; set; }
            }

            internal class UsernameGenerationOptions
            {
                public V6Keys.UsernameType Type { get; set; }
                public V6Keys.ForwardedEmailServiceType ServiceType { get; set; }
                public V6Keys.UsernameEmailType PlusAddressedEmailType { get; set; }
                public V6Keys.UsernameEmailType CatchAllEmailType { get; set; }
                public bool CapitalizeRandomWordUsername { get; set; }
                public bool IncludeNumberRandomWordUsername { get; set; }
                public string PlusAddressedEmail { get; set; }
                public string CatchAllEmailDomain { get; set; }
                public string FirefoxRelayApiAccessToken { get; set; }
                public string SimpleLoginApiKey { get; set; }
                public string DuckDuckGoApiKey { get; set; }
                public string FastMailApiKey { get; set; }
                public string AnonAddyApiAccessToken { get; set; }
                public string AnonAddyDomainName { get; set; }
                public string EmailWebsite { get; set; }
            }

            internal enum UsernameType
            {
                PlusAddressedEmail = 0,
                CatchAllEmail = 1,
                ForwardedEmailAlias = 2,
                RandomWord = 3,
            }

            internal enum ForwardedEmailServiceType
            {
                None = -1,
                AnonAddy = 0,
                FirefoxRelay = 1,
                SimpleLogin = 2,
                DuckDuckGo = 3,
                Fastmail = 4,
            }

            internal enum UsernameEmailType
            {
                Random = 0,
                Website = 1,
            }
        }

        #endregion

        #region v6 to v7 Migration

        private class V7Keys
        {
            // global keys
            internal const string StateKey = "state";
            internal const string RegionEnvironmentKey = "regionEnvironment";
            internal const string PreAuthEnvironmentUrlsKey = "preAuthEnvironmentUrls";
        }

        private async Task MigrateFrom6To7Async()
        {
            // account data
            var state = await GetValueAsync<State>(Storage.Prefs, V7Keys.StateKey);
            if (state != null)
            {
                // Migrate environment data to use Regions
                foreach (var account in state.Accounts.Where(a => a.Value?.Profile?.UserId != null && a.Value?.Settings != null))
                {
                    var urls = account.Value.Settings.EnvironmentUrls ?? Region.US.GetUrls();
                    account.Value.Settings.Region = urls.Region;
                    account.Value.Settings.EnvironmentUrls = urls.Region.GetUrls() ?? urls;
                }

                await SetValueAsync(Storage.Prefs, Constants.StateKey, state);
            }

            // Update pre auth urls and region
            var preAuthUrls = await GetValueAsync<EnvironmentUrlData>(Storage.Prefs, V7Keys.PreAuthEnvironmentUrlsKey) ?? Region.US.GetUrls();
            await SetValueAsync(Storage.Prefs, V7Keys.RegionEnvironmentKey, preAuthUrls.Region);
            await SetValueAsync(Storage.Prefs, V7Keys.PreAuthEnvironmentUrlsKey, preAuthUrls.Region.GetUrls() ?? preAuthUrls);


            // Update stored version
            await SetLastStateVersionAsync(7);
        }
        #endregion

        // Helpers

        private async Task<int> GetLastStateVersionAsync()
        {
            var lastVersion = await GetValueAsync<int?>(Storage.Prefs, Constants.StateVersionKey);
            if (lastVersion != null)
            {
                return lastVersion.Value;
            }

            // check for v1 state 
            var v1EnvUrls = await GetValueAsync<EnvironmentUrlData>(Storage.LiteDb, V1Keys.EnvironmentUrlsKey);
            if (v1EnvUrls != null)
            {
                // environmentUrls still in LiteDB (never migrated to prefs), this is v1
                return 1;
            }

            // check for v2 state
            var v2UserId = await GetValueAsync<string>(Storage.LiteDb, V2Keys.Keys_UserId);
            if (v2UserId != null)
            {
                // standalone userId still exists (never moved to Account object), this is v2
                return 2;
            }

            // this is a fresh install
            return 0;
        }

        private async Task SetLastStateVersionAsync(int value)
        {
            await SetValueAsync(Storage.Prefs, Constants.StateVersionKey, value);
        }

        private async Task<T> GetValueAsync<T>(Storage storage, string key)
        {
            var value = await GetStorageService(storage).GetAsync<T>(key);
            return value;
        }

        private async Task SetValueAsync<T>(Storage storage, string key, T value)
        {
            if (value == null)
            {
                await RemoveValueAsync(storage, key);
                return;
            }
            await GetStorageService(storage).SaveAsync(key, value);
        }

        private async Task RemoveValueAsync(Storage storage, string key)
        {
            await GetStorageService(storage).RemoveAsync(key);
        }

        private IStorageService GetStorageService(Storage storage)
        {
            switch (storage)
            {
                case Storage.Secure:
                    return _secureStorageService;
                case Storage.Prefs:
                    return _preferencesStorageService;
                default:
                    return _liteDbStorageService;
            }
        }

        private async Task MoveToPrefsAsync<T>(string key)
        {
            var value = await GetValueAsync<T>(Storage.LiteDb, key);
            if (value == null)
            {
                return;
            }

            if (await GetValueAsync<T>(Storage.Prefs, key) == null)
            {
                await SetValueAsync(Storage.Prefs, key, value);
            }
            await RemoveValueAsync(Storage.LiteDb, key);
        }
    }
}
