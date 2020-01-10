using Bit.Core;
using Bit.Core.Abstractions;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Bit.App.Services
{
    public class MobileStorageService : IStorageService
    {
        private readonly IStorageService _preferencesStorageService;
        private readonly IStorageService _liteDbStorageService;

        private readonly HashSet<string> _preferenceStorageKeys = new HashSet<string>
        {
            Constants.LockOptionKey,
            Constants.ThemeKey,
            Constants.DefaultUriMatch,
            Constants.DisableAutoTotpCopyKey,
            Constants.DisableFaviconKey,
            Constants.ClearClipboardKey,
            Constants.AutofillDisableSavePromptKey,
            Constants.LastActiveKey,
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
        };

        public MobileStorageService(
            IStorageService preferenceStorageService,
            IStorageService liteDbStorageService)
        {
            _preferencesStorageService = preferenceStorageService;
            _liteDbStorageService = liteDbStorageService;
        }

        public Task<T> GetAsync<T>(string key)
        {
            if(_preferenceStorageKeys.Contains(key))
            {
                return _preferencesStorageService.GetAsync<T>(key);
            }
            return _liteDbStorageService.GetAsync<T>(key);
        }

        public Task SaveAsync<T>(string key, T obj)
        {
            if(_preferenceStorageKeys.Contains(key))
            {
                return _preferencesStorageService.SaveAsync(key, obj);
            }
            return _liteDbStorageService.SaveAsync(key, obj);
        }

        public Task RemoveAsync(string key)
        {
            if(_preferenceStorageKeys.Contains(key))
            {
                return _preferencesStorageService.RemoveAsync(key);
            }
            return _liteDbStorageService.RemoveAsync(key);
        }
    }
}
