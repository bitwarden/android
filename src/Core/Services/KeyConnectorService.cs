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

        public async Task GetAndSetMasterKeyAsync(string url)
        {
            try
            {
                var masterKeyResponse = await _apiService.GetMasterKeyFromKeyConnectorAsync(url);
                var masterKeyArr = Convert.FromBase64String(masterKeyResponse.Key);
                var masterKey = new MasterKey(masterKeyArr);
                await _cryptoService.SetMasterKeyAsync(masterKey);
            }
            catch (Exception e)
            {
                throw new Exception("Unable to reach Key Connector", e);
            }
        }

        public async Task SetUsesKeyConnectorAsync(bool usesKeyConnector)
        {
            await _stateService.SetUsesKeyConnectorAsync(usesKeyConnector);
        }

        public async Task<bool> GetUsesKeyConnectorAsync()
        {
            return await _stateService.GetUsesKeyConnectorAsync();
        }

        public async Task<Organization> GetManagingOrganizationAsync()
        {
            var orgs = await _organizationService.GetAllAsync();
            return orgs.Find(o =>
                o.UsesKeyConnector &&
                !o.IsAdmin);
        }

        public async Task MigrateUserAsync()
        {
            var organization = await GetManagingOrganizationAsync();
            var masterKey = await _cryptoService.GetMasterKeyAsync();

            try
            {
                var keyConnectorRequest = new KeyConnectorUserKeyRequest(masterKey.EncKeyB64);
                await _apiService.PostUserKeyToKeyConnector(organization.KeyConnectorUrl, keyConnectorRequest);
            }
            catch (Exception e)
            {
                throw new Exception("Unable to reach Key Connector", e);
            }

            await _apiService.PostConvertToKeyConnector();
        }

        public async Task<bool> UserNeedsMigrationAsync()
        {
            var loggedInUsingSso = await _tokenService.GetIsExternal();
            var requiredByOrganization = await GetManagingOrganizationAsync() != null;
            var userIsNotUsingKeyConnector = !await GetUsesKeyConnectorAsync();

            return loggedInUsingSso && requiredByOrganization && userIsNotUsingKeyConnector;
        }
    }
}
