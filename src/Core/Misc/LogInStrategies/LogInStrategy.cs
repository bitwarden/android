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
        // TODO: LogService?
        private readonly IStateService _stateService;
        private readonly ITwoFactorService _twoFactorService;

        protected abstract TokenRequest TokenRequest { get; set; }

        public LogInStrategy(
            ICryptoService cryptoService,
            IApiService apiService,
            ITokenService tokenService,
            IAppIdService appIdService,
            IPlatformUtilsService platformUtilsService,
            IMessagingService messagingService,
            IStateService stateService,
            ITwoFactorService twoFactorService)
        {
            _cryptoService = cryptoService;
            _apiService = apiService;
            _tokenService = tokenService;
            _appIdService = appIdService;
            _platformUtilsService = platformUtilsService;
            _messagingService = messagingService;
            _stateService = stateService;
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

            if (response is IdentityTwoFactorResponse)
            {
                return ProcessTwoFactorResponseAsync(response);
            }
            else if (response is IdentityCaptchaResponse)
            {
                return ProcessCaptchaResponseAsync(response);
            }
            else if (response is IdentityTokenResponse)
            {
                return ProcessTokenResponseAsync(response);
            }

            throw new Exception("Invalid response object.");
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
            // TODO: this is a bit different from jslib, do we need to setAccessToken there too?
            // also, this may require updating after account switching finalised
            await _tokenService.SetAccessTokenAsync(tokenResponse.AccessToken, true);
            await _stateService.AddAccountAsync(
                new Account(
                    new Account.AccountProfile()
                    {
                        UserId = _tokenService.GetUserId(),
                        Email = _tokenService.GetEmail(),
                        Name = _tokenService.GetName(),
                        KdfType = tokenResponse.Kdf,
                        KdfIterations = tokenResponse.KdfIterations,
                        HasPremiumPersonally = _tokenService.GetPremium(),
                    },
                    new Account.AccountTokens()
                    {
                        AccessToken = tokenResponse.AccessToken,
                        RefreshToken = tokenResponse.RefreshToken,
                    }
                )
            );
            _messagingService.Send("accountAdded");
        }

        protected async Task<AuthResult> ProcessTokenResponseAsync(IdentityTokenResponse response)
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

            _stateService.BiometricLocked = false;
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
