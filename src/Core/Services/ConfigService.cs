using System;
using System.Collections.Generic;
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

        public ConfigService(IApiService apiService, IStateService stateService)
        {
            _apiService = apiService;
            _stateService = stateService;
        }

        public async Task<ConfigResponse> GetAllAsync()
        {
            try
            {
                _configs = _stateService.GetConfigs();
                if (_configs == null || _configs.ExpiresOn == null || _configs.ExpiresOn <= DateTime.UtcNow)
                {
                    _configs = await _apiService.GetConfigsAsync();
                    _configs.ExpiresOn = DateTime.UtcNow.AddMinutes(UPDATE_INTERVAL_MINS);
                    _stateService.SetConfigs(_configs);
                }
            }
            catch (Exception)
            {
                //ignore and return state value or null
            }

            return _configs;
        }
    }
}

