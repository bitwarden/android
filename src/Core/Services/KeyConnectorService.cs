using System;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;

namespace Bit.Core.Services
{
    public class KeyConnectorService : IKeyConnectorService
    {
        private const string Keys_UsesKeyConnector = "usesKeyConnector";

        private readonly IUserService _userService;
        private readonly ICryptoService _cryptoService;
        private readonly IStorageService _storageService;
        private readonly ITokenService _tokenService;
        private readonly IApiService _apiService;

        private bool? _usesKeyConnector;

        public KeyConnectorService(IUserService userService, ICryptoService cryptoService,
            IStorageService storageService, ITokenService tokenService, IApiService apiService)
        {
            _userService = userService;
            _cryptoService = cryptoService;
            _storageService = storageService;
            _tokenService = tokenService;
            _apiService = apiService;

        }

        public async Task GetAndSetKey(string url)
        {
            try
            {
                var userKeyResponse = await _apiService.GetUserKeyFromKeyConnector(url);
                var keyArr = Convert.FromBase64String(userKeyResponse.Key);
                var k = new SymmetricCryptoKey(keyArr);
                await _cryptoService.SetKeyAsync(k);
            }
            catch (Exception e)
            {
                throw new Exception("Unable to reach Key Connector", e);
            }            
        }

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

        public async Task<Organization> GetManagingOrganization()
        {
            var orgs = await _userService.GetAllOrganizationAsync();
            return orgs.Find(o =>
                o.UsesKeyConnector &&
                !o.IsAdmin);
        }

        public async Task MigrateUser()
        {
            var organization = await GetManagingOrganization();
            var key = await _cryptoService.GetKeyAsync();

            try
            {
                var keyConnectorRequest = new KeyConnectorUserKeyRequest(key.EncKeyB64);
                await _apiService.PostUserKeyToKeyConnector(organization.KeyConnectorUrl, keyConnectorRequest);
            }
            catch (Exception e)
            {
                throw new Exception("Unable to reach Key Connector", e);
            }

            await _apiService.PostConvertToKeyConnector();
        }

        public async Task<bool> UserNeedsMigration()
        {
            var loggedInUsingSso = _tokenService.GetIsExternal();
            var requiredByOrganization = await GetManagingOrganization() != null;
            var userIsNotUsingKeyConnector = !await this.GetUsesKeyConnector();

            return loggedInUsingSso && requiredByOrganization && userIsNotUsingKeyConnector;
        }


    }
}
