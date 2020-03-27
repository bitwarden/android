using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Newtonsoft.Json.Linq;
using System;
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
            _settingsCache?.Clear();
            _settingsCache = null;
        }

        public async Task<List<List<string>>> GetEquivalentDomainsAsync()
        {
            var settings = await GetSettingsAsync();
            if (settings != null && settings.ContainsKey(Keys_EquivalentDomains))
            {
                var jArray = (settings[Keys_EquivalentDomains] as JArray);
                return jArray?.ToObject<List<List<string>>>() ?? new List<List<string>>();
            }
            return new List<List<string>>();
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
            if (_settingsCache == null)
            {
                var userId = await _userService.GetUserIdAsync();
                _settingsCache = await _storageService.GetAsync<Dictionary<string, object>>(
                    string.Format(Keys_SettingsFormat, userId));
            }
            return _settingsCache;
        }

        private async Task SetSettingsKeyAsync<T>(string key, T value)
        {
            var userId = await _userService.GetUserIdAsync();
            var settings = await GetSettingsAsync();
            if (settings == null)
            {
                settings = new Dictionary<string, object>();
            }
            if (settings.ContainsKey(key))
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
