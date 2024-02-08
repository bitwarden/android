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

            return await _preferencesStorageService.GetAsync<T>(key) ?? await TryMigrateLiteDbToPrefsAsync<T>(key);
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

        // Helpers

        private bool IsLiteDbKey(string key)
        {
            return _liteDbStorageKeys.Any(key.StartsWith) ||
                   _liteDbStorageKeys.Contains(key);
        }

        private async Task<T> TryMigrateLiteDbToPrefsAsync<T>(string key)
        {
            // Note: this is added to prevent searching a value in LiteDB when the migration has already run and it's in its latest version.
            // If not, we could get several concurrent calls to the DB asking for values we already know they are not there,
            // possible causing crashes on backgrounded apps.
            if (await _preferencesStorageService.GetAsync<int?>(Constants.StateVersionKey) == Constants.LatestStateVersion)
            {
                return default;
            }

            var currentValue = await _liteDbStorageService.GetAsync<T>(key);
            if (currentValue != null)
            {
                await _preferencesStorageService.SaveAsync(key, currentValue);
                await _liteDbStorageService.RemoveAsync(key);
            }

            return currentValue;
        }
    }
}
