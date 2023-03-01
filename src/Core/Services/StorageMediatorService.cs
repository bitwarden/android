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

        public Task<T> GetAsync<T>(string key, StorageMediatorOptions options = default)
        {
            return GetAsyncStorage(options).GetAsync<T>(key);
        }

        public async Task SaveAsync<T>(string key, T obj, StorageMediatorOptions options = default)
        {
            if (obj is null && !options.AllowSaveNull)
            {
                await GetAsyncStorage(options).RemoveAsync(key);
                return;
            }

            await GetAsyncStorage(options).SaveAsync<T>(key, obj);
        }

        public Task RemoveAsync(string key, StorageMediatorOptions options = default)
        {
            return GetAsyncStorage(options).RemoveAsync(key);
        }

        IStorageService GetAsyncStorage(StorageMediatorOptions options)
        {
            return options.UseSecureStorage ? _secureStorageService : _storageService;
        }
    }
}
