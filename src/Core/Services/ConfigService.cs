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
        private ConfigResponse _configs;
        private DateTime _lastUpdate;
        private const int UPDATE_INTERVAL_MINS = 60;
        private readonly IApiService _apiService;

        public ConfigService(IApiService apiService)
        {
            _apiService = apiService;
        }

        public async Task<ConfigResponse> GetAllAsync()
        {
            if (_configs == null || _lastUpdate == null || _lastUpdate.AddMinutes(UPDATE_INTERVAL_MINS) < DateTime.Now)
            {
                _configs = await _apiService.GetAllConfigsAsync();
                _lastUpdate = DateTime.Now;
            }

            return _configs;
        }
    }
}

