using System;
using System.Threading.Tasks;
using Bit.Core.Abstractions;

namespace Bit.Core.Services
{
    public class AppIdService : IAppIdService
    {
        private readonly IStorageService _storageService;

        public AppIdService(IStorageService storageService)
        {
            _storageService = storageService;
        }

        public Task<string> GetAppIdAsync()
        {
            return MakeAndGetAppIdAsync(Constants.AppIdKey);
        }

        private async Task<string> MakeAndGetAppIdAsync(string key)
        {
            var existingId = await _storageService.GetAsync<string>(key);
            if (existingId != null)
            {
                return existingId;
            }
            var guid = Guid.NewGuid().ToString();
            await _storageService.SaveAsync(key, guid);
            return guid;
        }
    }
}
