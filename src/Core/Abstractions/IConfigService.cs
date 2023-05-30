using System;
using System.Threading.Tasks;
using Bit.Core.Models.Response;

namespace Bit.Core.Abstractions
{
    public interface IConfigService
    {
        Task<ConfigResponse> GetAsync(bool forceRefresh = false);
        Task<bool> GetFeatureFlagBoolAsync(string key, bool forceRefresh = false, bool defaultValue = false);
        Task<string> GetFeatureFlagStringAsync(string key, bool forceRefresh = false, string defaultValue = null);
        Task<int> GetFeatureFlagIntAsync(string key, bool forceRefresh = false, int defaultValue = 0);
    }
}

