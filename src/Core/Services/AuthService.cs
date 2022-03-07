using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;
using Bit.Core.Models.Request.IdentityToken;
using System;
using System.Threading.Tasks;
using Bit.Core.Misc.LogInStrategies;

namespace Bit.Core.Services
{
    public class AuthService : IAuthService
    {
        private readonly ICryptoService _cryptoService;
        private readonly IApiService _apiService;
        private readonly ITokenService _tokenService;
        private readonly IAppIdService _appIdService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IMessagingService _messagingService;
        private readonly IKeyConnectorService _keyConnectorService;
        private readonly IStateService _stateService;
        private readonly ITwoFactorService _twoFactorService;
        private readonly II18nService _i18nService;

        public AuthService(
            ICryptoService cryptoService,
            IApiService apiService,
            ITokenService tokenService,
            IAppIdService appIdService,
            IPlatformUtilsService platformUtilsService,
            IMessagingService messagingService,
            IKeyConnectorService keyConnectorService,
            IStateService stateService,
            ITwoFactorService twoFactorService,
            II18nService i18nService)
        {
            _cryptoService = cryptoService;
            _apiService = apiService;
            _tokenService = tokenService;
            _appIdService = appIdService;
            _platformUtilsService = platformUtilsService;
            _messagingService = messagingService;
            _keyConnectorService = keyConnectorService;
            _stateService = stateService;
            _twoFactorService = twoFactorService;
            _i18nService = i18nService;
        }

        public string Email
        {
            get
            {
                return (_logInStrategy as PasswordLogInStrategy)?.Email;
            }
        }

        public string MasterPasswordHash
        {
            get
            {
                return (_logInStrategy as PasswordLogInStrategy)?.MasterPasswordHash;
            }
        }

        private LogInStrategy _logInStrategy;

        public async Task<AuthResult> LogInAsync(LogInCredentials credentials)
        {
            ClearState();

            LogInStrategy strategy = null;
            
            if (credentials is PasswordLogInCredentials)
            {
                strategy = new PasswordLogInStrategy(
                    _cryptoService, _apiService, _tokenService, _appIdService, _platformUtilsService, _messagingService, _stateService,
                    _twoFactorService, this);
            }
            else if (credentials is SsoLogInCredentials)
            {
                strategy = new SsoLogInStrategy(
                    _cryptoService, _apiService, _tokenService, _appIdService, _platformUtilsService, _messagingService, _stateService,
                    _twoFactorService, _keyConnectorService);
            }

            var result = await strategy.LogInAsync(credentials);

            if (result.RequiresTwoFactor)
            {
                SaveState(strategy);
            }
            return result;
        }

        public async Task<AuthResult> LogInTwoFactorAsync(TokenRequestTwoFactor twoFactor, string captchaResponse)
        {
            if (_logInStrategy == null)
            {
                throw new Exception(_i18nService.T("sessionTimeout"));
            }

            try
            {
                var result = await _logInStrategy.LogInTwoFactorAsync(twoFactor, captchaResponse);

                // Only clear state if 2FA token has been accepted, otherwise we need to be able to try again
                if (!result.RequiresTwoFactor && !result.RequiresCaptcha)
                {
                    ClearState();
                }
                return result;
            }
            catch (Exception ex)
            {
                // API exceptions are okay, but if there are any unhandled client-side errors then clear state to be safe
                if (!(ex is ApiException))
                {
                    ClearState();
                }
                throw ex;
            }
        }

        public void LogOut(Action callback)
        {
            callback.Invoke();
            _messagingService.Send("loggedOut");
        }

        public bool AuthingWithSso()
        {
            return _logInStrategy is SsoLogInStrategy;
        }

        public bool AuthingWithPassword()
        {
            return _logInStrategy is PasswordLogInStrategy;
        }

        public async Task<SymmetricCryptoKey> MakePreloginKeyAsync(string masterPassword, string email)
        {
            email = email.Trim().ToLower();
            KdfType? kdf = null;
            int? kdfIterations = null;
            try
            {
                var preloginResponse = await _apiService.PostPreloginAsync(new PreloginRequest { Email = email });
                if (preloginResponse != null)
                {
                    kdf = preloginResponse.Kdf;
                    kdfIterations = preloginResponse.KdfIterations;
                }
            }
            catch (ApiException e)
            {
                if (e.Error == null || e.Error.StatusCode != System.Net.HttpStatusCode.NotFound)
                {
                    throw;
                }
            }
            return await _cryptoService.MakeKeyAsync(masterPassword, email, kdf, kdfIterations);
        }

        private void ClearState()
        {
            _logInStrategy = null;
        }

        private void SaveState(LogInStrategy strategy)
        {
            _logInStrategy = strategy;
        }
    }
}
