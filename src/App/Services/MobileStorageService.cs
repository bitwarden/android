using Bit.Core;
using Bit.Core.Abstractions;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Bit.App.Services
{
    public class MobileStorageService : IStorageService, IDisposable
    {
        private readonly IStorageService _preferencesStorageService;
        private readonly IStorageService _liteDbStorageService;

        private readonly HashSet<string> _preferenceStorageKeys = new HashSet<string>
        {
            Constants.VaultTimeoutKey,
            Constants.VaultTimeoutActionKey,
            Constants.ThemeKey,
            Constants.DefaultUriMatch,
            Constants.DisableAutoTotpCopyKey,
            Constants.DisableFaviconKey,
            Constants.ClearClipboardKey,
            Constants.AutofillDisableSavePromptKey,
            Constants.LastActiveTimeKey,
            Constants.PushInitialPromptShownKey,
            Constants.LastFileCacheClearKey,
            Constants.PushLastRegistrationDateKey,
            Constants.PushRegisteredTokenKey,
            Constants.PushCurrentTokenKey,
            Constants.LastBuildKey,
            Constants.MigratedFromV1,
            Constants.MigratedFromV1AutofillPromptShown,
            Constants.TriedV1Resync,
            Constants.ClearCiphersCacheKey,
            Constants.BiometricIntegrityKey,
            Constants.iOSAutoFillClearCiphersCacheKey,
            Constants.iOSAutoFillBiometricIntegrityKey,
            Constants.iOSExtensionClearCiphersCacheKey,
            Constants.iOSExtensionBiometricIntegrityKey,
            Constants.EnvironmentUrlsKey,
            Constants.InlineAutofillEnabledKey,
            Constants.InvalidUnlockAttempts,
        };

        private readonly HashSet<string> _migrateToPreferences = new HashSet<string>
        {
            Constants.EnvironmentUrlsKey,
        };
        private readonly HashSet<string> _haveMigratedToPreferences = new HashSet<string>();

        public MobileStorageService(
            IStorageService preferenceStorageService,
            IStorageService liteDbStorageService)
        {
            _preferencesStorageService = preferenceStorageService;
            _liteDbStorageService = liteDbStorageService;
        }

        public async Task<T> GetAsync<T>(string key)
        {
            if (_preferenceStorageKeys.Contains(key))
            {
                var prefValue = await _preferencesStorageService.GetAsync<T>(key);
                if (prefValue != null || !_migrateToPreferences.Contains(key) ||
                    _haveMigratedToPreferences.Contains(key))
                {
                    return prefValue;
                }
            }
            var liteDbValue = await _liteDbStorageService.GetAsync<T>(key);
            if (_migrateToPreferences.Contains(key))
            {
                if (liteDbValue != null)
                {
                    await _preferencesStorageService.SaveAsync(key, liteDbValue);
                    await _liteDbStorageService.RemoveAsync(key);
                }
                _haveMigratedToPreferences.Add(key);
            }
            return liteDbValue;
        }

        public Task SaveAsync<T>(string key, T obj)
        {
            if (_preferenceStorageKeys.Contains(key))
            {
                return _preferencesStorageService.SaveAsync(key, obj);
            }
            return _liteDbStorageService.SaveAsync(key, obj);
        }

        public Task RemoveAsync(string key)
        {
            if (_preferenceStorageKeys.Contains(key))
            {
                return _preferencesStorageService.RemoveAsync(key);
            }
            return _liteDbStorageService.RemoveAsync(key);
        }

        public void Dispose()
        {
            if (_liteDbStorageService is IDisposable disposableLiteDbService)
            {
                disposableLiteDbService.Dispose();
            }
            if (_preferencesStorageService is IDisposable disposablePrefService)
            {
                disposablePrefService.Dispose();
            }
        }
    }
}
