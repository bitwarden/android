using System;
using System.Threading.Tasks;
using Bit.Core.Models.Request;
using Bit.Core.Models.Request.IdentityToken;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Response;
using Bit.Core.Abstractions;
using Bit.Core.Enums;

namespace Bit.Core.Misc.LogInStrategies {

    public abstract class LogInStrategy
    {
        protected readonly ICryptoService _cryptoService;
        private readonly IApiService _apiService;
        private readonly ITokenService _tokenService;
        private readonly IAppIdService _appIdService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IMessagingService _messagingService;
        private readonly IKeyConnectorService _keyConnectorService;
        // TODO: LogService?
        // TODO: StateService
        private readonly ITwoFactorService _twoFactorService;

        protected abstract TokenRequest TokenRequest { get; set; }

        public LogInStrategy(
            ICryptoService cryptoService,
            IApiService apiService,
            ITokenService tokenService,
            IAppIdService appIdService,
            IPlatformUtilsService platformUtilsService,
            IMessagingService messagingService,
            ITwoFactorService twoFactorService)
        {
            _cryptoService = cryptoService;
            _apiService = apiService;
            _tokenService = tokenService;
            _appIdService = appIdService;
            _platformUtilsService = platformUtilsService;
            _messagingService = messagingService;
            _twoFactorService = twoFactorService;
        }

        public abstract Task<AuthResult> LogInAsync(LogInCredentials credentials);

        public Task<AuthResult> LogInTwoFactorAsync(TokenRequestTwoFactor twoFactor)
        {
            TokenRequest.SetTwoFactor(twoFactor);
            return StartLogInAsync();
        }

        protected async Task<AuthResult> StartLogInAsync()
        {
            _twoFactorService.clearSelectedProvider();

            var response = await _apiService.PostIdentityTokenAsync(TokenRequest);

            // TODO
        }

        protected virtual Task OnSuccessfulLoginAsync(IdentityTokenResponse response)
        {
            // Implemented in base class if required
            return null;
        }

        protected async Task<DeviceRequest> BuildDeviceRequestAsync()
        {
            var appId = await _appIdService.GetAppIdAsync();
            return new DeviceRequest(appId, _platformUtilsService);
        }

        protected async Task<TokenRequestTwoFactor> BuildTwoFactorAsync(TokenRequestTwoFactor userProvidedTwoFactor)
        {
            if (userProvidedTwoFactor != null)
            {
                return userProvidedTwoFactor;
            }

            var storedTwoFactorToken = await _tokenService.GetTwoFactorTokenAsync();
            if (storedTwoFactorToken != null)
            {
                return new TokenRequestTwoFactor
                {
                    Token = storedTwoFactorToken,
                    Provider = TwoFactorProviderType.Remember,
                    Remember = false
                };
            }

            return new TokenRequestTwoFactor
            {
                Token = null,
                Provider = null,
                Remember = false
            };
        }

        protected virtual async Task SaveAccountInformationAsync(IdentityTokenResponse tokenResponse)
        {
            var accountInformation = await _tokenService.DecodeToken(tokenResponse.AccessToken);
            // TODO: StateService.AddAccount
        }

        protected async Task<AuthResult> ProcessTokenResponse(IdentityTokenResponse response)
        {
            var result = new AuthResult();
            result.ResetMasterPassword = response.ResetMasterPassword;
            result.ForcePasswordReset = response.ForcePasswordReset;

            await SaveAccountInformationAsync(response);

            if (response.TwoFactorToken != null)
            {
                await _tokenService.SetTwoFactorTokenAsync(response);
            }

            var newSsoUser = response.Key == null;
            if (!newSsoUser)
            {
                await _cryptoService.SetEncKeyAsync(response.Key);
                await _cryptoService.SetEncPrivateKeyAsync(response.PrivateKey ?? (await CreateKeyPairForOldAccount()));
            }

            await OnSuccessfulLoginAsync(response);

            // TODO: StateService.SetBiometricLocked
            _messagingService.Send("loggedIn");

            return result;
        }

        private AuthResult ProcessTwoFactorResponseAsync(IdentityTwoFactorResponse response)
        {
            var result = new AuthResult
            {
                TwoFactorProviders = response.TwoFactorProviders2
            };
            _twoFactorService.SetProviders(response);
            return result;
        }

        private AuthResult ProcessCaptchaResponseAsync(IdentityCaptchaResponse response)
        {
            return new AuthResult
            {
                CaptchaSiteKey = response.SiteKey
            };
        }

        private async Task<string> CreateKeyPairForOldAccount()
        {
            var keyPair = await _cryptoService.MakeKeyPairAsync();
            await _apiService.PostAccountKeysAsync(new KeysRequest
            {
                PublicKey = keyPair.Item1,
                EncryptedPrivateKey = keyPair.Item2.EncryptedString
            });
            return keyPair.Item2.EncryptedString;
        }
    }
}
