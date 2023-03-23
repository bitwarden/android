using System.Threading.Tasks;
using Bit.Core.Abstractions;

namespace Bit.Core.Services
{
    public class StorageMediatorService : IStorageMediatorService
    {
        private readonly IStorageService _storageService;
        private readonly IStorageService _secureStorageService;
        private readonly ISynchronousStorageService _synchronousStorageService;

        public StorageMediatorService(IStorageService storageService,
                                      IStorageService secureStorageService,
                                      ISynchronousStorageService synchronousStorageService)
        {
            _storageService = storageService;
            _secureStorageService = secureStorageService;
            _synchronousStorageService = synchronousStorageService;
        }

        public T Get<T>(string key)
        {
            return _synchronousStorageService.Get<T>(key);
        }

        public void Save<T>(string key, T obj)
        {
            _synchronousStorageService.Save<T>(key, obj);
        }

        public void Remove(string key)
        {
            _synchronousStorageService.Remove(key);
        }

        public Task<T> GetAsync<T>(string key, bool useSecureStorage = false)
        {
            return GetAsyncStorage(useSecureStorage).GetAsync<T>(key);
        }

        public async Task SaveAsync<T>(string key, T obj, bool useSecureStorage = false, bool allowSaveNull = false)
        {
            if (obj is null && !allowSaveNull)
            {
                await GetAsyncStorage(useSecureStorage).RemoveAsync(key);
                return;
            }

            await GetAsyncStorage(useSecureStorage).SaveAsync<T>(key, obj);
        }

        public Task RemoveAsync(string key, bool useSecureStorage = false)
        {
            return GetAsyncStorage(useSecureStorage).RemoveAsync(key);
        }

        IStorageService GetAsyncStorage(bool useSecureStorage)
        {
            return useSecureStorage ? _secureStorageService : _storageService;
        }
    }
}
