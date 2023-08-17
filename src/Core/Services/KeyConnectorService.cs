using System;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;
using Bit.Core.Models.Response;

namespace Bit.Core.Services
{
    public class KeyConnectorService : IKeyConnectorService
    {
        private readonly IStateService _stateService;
        private readonly ICryptoService _cryptoService;
        private readonly ITokenService _tokenService;
        private readonly IApiService _apiService;
        private readonly ICryptoFunctionService _cryptoFunctionService;
        private readonly IOrganizationService _organizationService;

        public KeyConnectorService(IStateService stateService, ICryptoService cryptoService,
            ITokenService tokenService, IApiService apiService, ICryptoFunctionService cryptoFunctionService, OrganizationService organizationService)
        {
            _stateService = stateService;
            _cryptoService = cryptoService;
            _tokenService = tokenService;
            _apiService = apiService;
            _cryptoFunctionService = cryptoFunctionService;
            _organizationService = organizationService;
        }

        public async Task SetMasterKeyFromUrlAsync(string url)
        {
            try
            {
                var masterKeyResponse = await _apiService.GetMasterKeyFromKeyConnectorAsync(url);
                var masterKeyBytes = Convert.FromBase64String(masterKeyResponse.Key);
                var masterKey = new MasterKey(masterKeyBytes);
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
                await _apiService.PostMasterKeyToKeyConnectorAsync(organization.KeyConnectorUrl, keyConnectorRequest);
            }
            catch (Exception e)
            {
                throw new Exception("Unable to reach Key Connector", e);
            }

            await _apiService.PostConvertToKeyConnectorAsync();
        }

        public async Task<bool> UserNeedsMigrationAsync()
        {
            var loggedInUsingSso = await _tokenService.GetIsExternal();
            var requiredByOrganization = await GetManagingOrganizationAsync() != null;
            var userIsNotUsingKeyConnector = !await GetUsesKeyConnectorAsync();

            return loggedInUsingSso && requiredByOrganization && userIsNotUsingKeyConnector;
        }

        public async Task ConvertNewUserToKeyConnectorAsync(string orgId, IdentityTokenResponse tokenResponse)
        {
            // SSO Key Connector Onboarding
            var password = await _cryptoFunctionService.RandomBytesAsync(64);
            var newMasterKey = await _cryptoService.MakeMasterKeyAsync(Convert.ToBase64String(password), _tokenService.GetEmail(), tokenResponse.KdfConfig);
            var keyConnectorRequest = new KeyConnectorUserKeyRequest(newMasterKey.EncKeyB64);
            await _cryptoService.SetMasterKeyAsync(newMasterKey);

            var (newUserKey, newProtectedUserKey) = await _cryptoService.EncryptUserKeyWithMasterKeyAsync(
                newMasterKey,
                await _cryptoService.MakeUserKeyAsync());

            await _cryptoService.SetUserKeyAsync(newUserKey);

            try
            {
                await _apiService.PostMasterKeyToKeyConnectorAsync(tokenResponse.KeyConnectorUrl, keyConnectorRequest);
            }
            catch (Exception e)
            {
                throw new Exception("Unable to reach Key Connector", e);
            }

            var (newPublicKey, newProtectedPrivateKey) = await _cryptoService.MakeKeyPairAsync();
            var keys = new KeysRequest
            {
                PublicKey = newPublicKey,
                EncryptedPrivateKey = newProtectedPrivateKey.EncryptedString
            };
            var setPasswordRequest = new SetKeyConnectorKeyRequest(
                newProtectedUserKey.EncryptedString, keys, tokenResponse.KdfConfig, orgId
            );
            await _apiService.PostSetKeyConnectorKeyAsync(setPasswordRequest);
        }
    }
}
