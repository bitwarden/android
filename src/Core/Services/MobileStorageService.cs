using Bit.Core.Abstractions;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class MobileStorageService : IStorageService
    {
        private readonly IStorageService _preferencesStorageService;
        private readonly IStorageService _liteDbStorageService;

        private readonly HashSet<string> _preferenceStorageKeys = new HashSet<string>
        {
            Constants.LockOptionKey
        };

        public MobileStorageService(
            IStorageService preferenceStorageService,
            IStorageService liteDbStorageService)
        {
            _preferencesStorageService = preferenceStorageService;
            _liteDbStorageService = liteDbStorageService;
        }

        public Task<T> GetAsync<T>(string key)
        {
            if(_preferenceStorageKeys.Contains(key))
            {
                return _preferencesStorageService.GetAsync<T>(key);
            }
            return _liteDbStorageService.GetAsync<T>(key);
        }

        public Task SaveAsync<T>(string key, T obj)
        {
            if(_preferenceStorageKeys.Contains(key))
            {
                return _preferencesStorageService.SaveAsync(key, obj);
            }
            return _liteDbStorageService.SaveAsync(key, obj);
        }

        public Task RemoveAsync(string key)
        {
            if(_preferenceStorageKeys.Contains(key))
            {
                return _preferencesStorageService.RemoveAsync(key);
            }
            return _liteDbStorageService.RemoveAsync(key);
        }
    }
}
