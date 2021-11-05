using System;
using System.Threading.Tasks;
using Bit.Core.Abstractions;

namespace Bit.Core.Services
{
    public class KeyConnectorService : IKeyConnectorService
    {
        private const string Keys_UsesKeyConnector = "usesKeyConnector";

        private readonly IStorageService _storageService;

        private bool? _usesKeyConnector;

        public KeyConnectorService(IStorageService storageService)
        {
            _storageService = storageService;
        }

        public Task GetAndSetKey(string url) => throw new NotImplementedException();
        public Task GetManagingOrganization() => throw new NotImplementedException();

        public async Task SetUsesKeyConnector(bool usesKeyConnector)
        {
            _usesKeyConnector = usesKeyConnector;
            await _storageService.SaveAsync(Keys_UsesKeyConnector, usesKeyConnector);
        }

        public async Task<bool> GetUsesKeyConnector()
        {
            if (!_usesKeyConnector.HasValue)
            {
                _usesKeyConnector = await _storageService.GetAsync<bool>(Keys_UsesKeyConnector);
            }

            return _usesKeyConnector.Value;
        }

        public Task MigrateUser() => throw new NotImplementedException();

        public Task<bool> UserNeedsMigration() => throw new NotImplementedException();
    }
}
