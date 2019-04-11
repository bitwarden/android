using Bit.Core.Abstractions;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class SettingsService : ISettingsService
    {
        private const string Keys_SettingsFormat = "settings_{0}";
        private const string Keys_EquivalentDomains = "equivalentDomains";

        private readonly IUserService _userService;
        private readonly IStorageService _storageService;

        private Dictionary<string, object> _settingsCache;

        public SettingsService(
            IUserService userService,
            IStorageService storageService)
        {
            _userService = userService;
            _storageService = storageService;
        }

        public void ClearCache()
        {
            _settingsCache.Clear();
            _settingsCache = null;
        }

        public Task<List<List<string>>> GetEquivalentDomainsAsync()
        {
            return GetSettingsKeyAsync<List<List<string>>>(Keys_EquivalentDomains);
        }

        public Task SetEquivalentDomainsAsync(List<List<string>> equivalentDomains)
        {
            return SetSettingsKeyAsync(Keys_EquivalentDomains, equivalentDomains);
        }

        public async Task ClearAsync(string userId)
        {
            await _storageService.RemoveAsync(string.Format(Keys_SettingsFormat, userId));
            ClearCache();
        }

        // Helpers

        private async Task<Dictionary<string, object>> GetSettingsAsync()
        {
            if(_settingsCache == null)
            {
                var userId = await _userService.GetUserIdAsync();
                _settingsCache = await _storageService.GetAsync<Dictionary<string, object>>(
                    string.Format(Keys_SettingsFormat, userId));
            }
            return _settingsCache;
        }

        private async Task<T> GetSettingsKeyAsync<T>(string key)
        {
            var settings = await GetSettingsAsync();
            if(settings != null && settings.ContainsKey(key))
            {
                return (T)settings[key];
            }
            return (T)(object)null;
        }

        private async Task SetSettingsKeyAsync<T>(string key, T value)
        {
            var userId = await _userService.GetUserIdAsync();
            var settings = await GetSettingsAsync();
            if(settings == null)
            {
                settings = new Dictionary<string, object>();
            }
            if(settings.ContainsKey(key))
            {
                settings[key] = value;
            }
            else
            {
                settings.Add(key, value);
            }
            await _storageService.SaveAsync(string.Format(Keys_SettingsFormat, userId), settings);
            _settingsCache = settings;
        }
    }
}
