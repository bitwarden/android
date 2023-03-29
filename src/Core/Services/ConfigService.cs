using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
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

        public async Task<ConfigResponse> GetAllAsync(bool forceRefresh = false)
        {
            try
            {
                _configs = _stateService.GetConfigs();
                if (_configs?.ExpiresOn is null || _configs.ExpiresOn <= DateTime.UtcNow)
                {
                    _configs = await _apiService.GetConfigsAsync();
                    _configs.ExpiresOn = DateTime.UtcNow.AddMinutes(UPDATE_INTERVAL_MINS);
                    _stateService.SetConfigs(_configs);
                }
            }
            catch (Exception ex)
            {
                _logger.Exception(ex);
            }

            return _configs;
        }

        public async Task<bool> GetFeatureFlagAsync(string key, bool forceRefresh = false)
        {
            await GetAllAsync(forceRefresh);
            if (_configs != null
                && _configs.FeatureStates != null
                && _configs.FeatureStates.Any()
                && _configs.FeatureStates.ContainsKey(key)
                && _configs.FeatureStates[key] is bool)
            {
                return (bool)_configs.FeatureStates[key];
            }

            return false;
        }
    }
}

