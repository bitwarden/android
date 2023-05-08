using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core;
using Bit.Core.Abstractions;

namespace Bit.App.Services
{
    public class MobileStorageService : IStorageService, IDisposable
    {
        private readonly IStorageService _preferencesStorageService;
        private readonly IStorageService _liteDbStorageService;

        private readonly HashSet<string> _preferenceStorageKeys = new HashSet<string>
        {
            Constants.StateVersionKey,
            Constants.PreAuthEnvironmentUrlsKey,
            Constants.AutofillTileAdded,
            Constants.AddSitePromptShownKey,
            Constants.PushInitialPromptShownKey,
            Constants.LastFileCacheClearKey,
            Constants.PushRegisteredTokenKey,
            Constants.LastBuildKey,
            Constants.ClearCiphersCacheKey,
            Constants.BiometricIntegritySourceKey,
            Constants.iOSExtensionActiveUserIdKey,
            Constants.iOSAutoFillClearCiphersCacheKey,
            Constants.iOSAutoFillBiometricIntegritySourceKey,
            Constants.iOSExtensionClearCiphersCacheKey,
            Constants.iOSExtensionBiometricIntegritySourceKey,
            Constants.iOSShareExtensionClearCiphersCacheKey,
            Constants.iOSShareExtensionBiometricIntegritySourceKey,
            Constants.RememberedEmailKey,
            Constants.RememberedOrgIdentifierKey
        };

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
                return await _preferencesStorageService.GetAsync<T>(key);
            }
            return await _liteDbStorageService.GetAsync<T>(key);
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
