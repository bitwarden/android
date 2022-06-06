using System;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;

namespace Bit.Core.Services
{
    public class KeyConnectorService : IKeyConnectorService
    {
        private readonly IStateService _stateService;
        private readonly ICryptoService _cryptoService;
        private readonly ITokenService _tokenService;
        private readonly IApiService _apiService;
        private readonly IOrganizationService _organizationService;

        public KeyConnectorService(IStateService stateService, ICryptoService cryptoService,
            ITokenService tokenService, IApiService apiService, OrganizationService organizationService)
        {
            _stateService = stateService;
            _cryptoService = cryptoService;
            _tokenService = tokenService;
            _apiService = apiService;
            _organizationService = organizationService;
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
            await _stateService.SetUsesKeyConnectorAsync(usesKeyConnector);
        }

        public async Task<bool> GetUsesKeyConnector()
        {
            return await _stateService.GetUsesKeyConnectorAsync();
        }

        public async Task<Organization> GetManagingOrganization()
        {
            var orgs = await _organizationService.GetAllAsync();
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
            var loggedInUsingSso = await _tokenService.GetIsExternal();
            var requiredByOrganization = await GetManagingOrganization() != null;
            var userIsNotUsingKeyConnector = !await GetUsesKeyConnector();

            return loggedInUsingSso && requiredByOrganization && userIsNotUsingKeyConnector;
        }
    }
}
