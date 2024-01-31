using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Response;
using Bit.Core.Models.View;

namespace Bit.Core.Services
{
    public class ConfigService : IConfigService
    {
        private const int UPDATE_INTERVAL_MINS = 60;
        private ConfigResponse _configs;
        private readonly IApiService _apiService;
        private readonly IStateService _stateService;
        private readonly ILogger _logger;

        public ConfigService(IApiService apiService, IStateService stateService, ILogger logger)
        {
            _apiService = apiService;
            _stateService = stateService;
            _logger = logger;
        }

        public async Task<ConfigResponse> GetAsync(bool forceRefresh = false)
        {
            try
            {
                _configs = _stateService.GetConfigs();
                if (forceRefresh || _configs?.ExpiresOn is null || _configs.ExpiresOn <= DateTime.UtcNow)
                {
                    _configs = await _apiService.GetConfigsAsync();
                    _configs.ExpiresOn = DateTime.UtcNow.AddMinutes(UPDATE_INTERVAL_MINS);
                    _stateService.SetConfigs(_configs);
                }
            }
            catch (ApiException ex) when (ex.Error?.StatusCode == System.Net.HttpStatusCode.BadGateway)
            {
                // ignore if there is no internet connection and return local configs
            }
            catch (Exception ex)
            {
                _logger.Exception(ex);
            }

            return _configs;
        }

        public async Task<bool> GetFeatureFlagBoolAsync(string key, bool forceRefresh = false, bool defaultValue = false) => await GetFeatureFlagAsync<bool>(key, forceRefresh, defaultValue);

        public async Task<string> GetFeatureFlagStringAsync(string key, bool forceRefresh = false, string defaultValue = null) => await GetFeatureFlagAsync<string>(key, forceRefresh, defaultValue);

        public async Task<int> GetFeatureFlagIntAsync(string key, bool forceRefresh = false, int defaultValue = 0) => await GetFeatureFlagAsync<int>(key, forceRefresh, defaultValue);

        private async Task<T> GetFeatureFlagAsync<T>(string key, bool forceRefresh = false, T defaultValue = default)
        {
            await GetAsync(forceRefresh);
            if (_configs == null || _configs.FeatureStates == null)
            {
                return defaultValue;
            }

            if (_configs.FeatureStates.TryGetValue(key, out var val) == true
                    &&
                    val is T actualValue)
            {
                return actualValue;
            }

            return defaultValue;
        }
    }
}

