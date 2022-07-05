using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Newtonsoft.Json.Linq;

namespace Bit.Core.Services
{
    public class SettingsService : ISettingsService
    {
        private const string Keys_EquivalentDomains = "equivalentDomains";

        private readonly IStateService _stateService;

        private Dictionary<string, object> _settingsCache;

        public SettingsService(
            IStateService stateService)
        {
            _stateService = stateService;
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
            await _stateService.SetSettingsAsync(null, userId);
            ClearCache();
        }

        // Helpers

        private async Task<Dictionary<string, object>> GetSettingsAsync()
        {
            if (_settingsCache == null)
            {
                _settingsCache = await _stateService.GetSettingsAsync();
            }
            return _settingsCache;
        }

        private async Task SetSettingsKeyAsync<T>(string key, T value)
        {
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
            await _stateService.SetSettingsAsync(settings);
            _settingsCache = settings;
        }
    }
}
