using System;
using System.Threading.Tasks;
using Bit.Core.Models.Request;
using Bit.Core.Models.Request.IdentityToken;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Response;
using Bit.Core.Abstractions;
using Bit.Core.Enums;

namespace Bit.Core.Misc.LogInStrategies
{
    public class SsoLogInStrategy : LogInStrategy
    {
        public new SsoTokenRequest TokenRequest { get; set; }
        public string OrgId { get; set; }

        private readonly IKeyConnectorService _keyConnectorService;

        public SsoLogInStrategy(
            ICryptoService cryptoService,
            IApiService apiService,
            ITokenService tokenService,
            IAppIdService appIdService,
            IPlatformUtilsService platformUtilsService,
            IMessagingService messagingService,
            IStateService stateService,
            ITwoFactorService twoFactorService,
            IKeyConnectorService keyConnectorService) : base(cryptoService, apiService, tokenService, appIdService, platformUtilsService, messagingService, stateService, twoFactorService)
        {
            _keyConnectorService = keyConnectorService;
        }

        protected async override Task OnSuccessfulLoginAsync(IdentityTokenResponse response)
        {
            var newSsoUser = response.Key == null;

            if (response.KeyConnectorUrl != null)
            {
                if (!newSsoUser)
                {
                    await _keyConnectorService.GetAndSetKey(response.KeyConnectorUrl);
                }
                else
                {
                    await _keyConnectorService.ConvertNewSsoUserToKeyConnector(response, OrgId);
                }
            }
        }

        public async Task<AuthResult> LogInAsync(SsoLogInCredentials credentials)
        {
            OrgId = credentials.OrgId;

            TokenRequest = new SsoTokenRequest(credentials.Code, credentials.CodeVerifier, credentials.RedirectUri, await BuildTwoFactorAsync(credentials.TwoFactor), await BuildDeviceRequestAsync());

            return await StartLogInAsync();
        }
    }
}
