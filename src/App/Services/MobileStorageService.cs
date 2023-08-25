using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.Core;
using Bit.Core.Abstractions;

namespace Bit.App.Services
{
    public class MobileStorageService : IStorageService, IDisposable
    {
        private readonly IStorageService _preferencesStorageService;
        private readonly IStorageService _liteDbStorageService;

        private readonly HashSet<string> _liteDbStorageKeys = new HashSet<string>
        {
            Constants.EventCollectionKey,
            Constants.CiphersKey(""),
            Constants.FoldersKey(""),
            Constants.CollectionsKey(""),
            Constants.CiphersLocalDataKey(""),
            Constants.SendsKey(""),
            Constants.PassGenHistoryKey(""),
            Constants.SettingsKey(""),
        };

        private bool IsLiteDbKey(string key)
        {
            return _liteDbStorageKeys.Any(key.StartsWith) ||
                   _liteDbStorageKeys.Contains(key);
        }

        public MobileStorageService(
            IStorageService preferenceStorageService,
            IStorageService liteDbStorageService)
        {
            _preferencesStorageService = preferenceStorageService;
            _liteDbStorageService = liteDbStorageService;
        }

        public async Task<T> GetAsync<T>(string key)
        {
            if (IsLiteDbKey(key))
            {
                return await _liteDbStorageService.GetAsync<T>(key);
            }

            var result = await _preferencesStorageService.GetAsync<T>(key);
            if (result == null)
            {
                // If result is expected in prefs but found in LiteDB, migrate it to prefs
                result = await _liteDbStorageService.GetAsync<T>(key);
                if (result != null)
                {
                    await _preferencesStorageService.SaveAsync(key, result);
                    await _liteDbStorageService.RemoveAsync(key);
                }
            }

            return result;
        }

        public Task SaveAsync<T>(string key, T obj)
        {
            if (IsLiteDbKey(key))
            {
                return _liteDbStorageService.SaveAsync(key, obj);
            }
            return _preferencesStorageService.SaveAsync(key, obj);
        }

        public Task RemoveAsync(string key)
        {
            if (IsLiteDbKey(key))
            {
                return _liteDbStorageService.RemoveAsync(key);
            }
            return _preferencesStorageService.RemoveAsync(key);
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
