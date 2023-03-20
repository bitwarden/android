using System;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Response;

namespace Bit.Core.Services
{
    public class ConfigService : IConfigService
    {
        private readonly IApiService _apiService;

        public ConfigService(IApiService apiService)
        {
            _apiService = apiService;
        }

        public async Task<ConfigResponse> GetAllAsync()
        {
            return await _apiService.GetAllConfigsAsync();
        }
    }
}

