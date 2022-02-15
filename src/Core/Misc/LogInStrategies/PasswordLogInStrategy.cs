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
    public class PasswordLogInStrategy : LogInStrategy
    {
        public string Email
        {
            get
            {
                return TokenRequest.Email;
            }
        }

        public string MasterPasswordHash
        {
            get
            {
                return TokenRequest.MasterPasswordHash;
            }
        }

        public new PasswordTokenRequest TokenRequest { get; set; }

        private string _localHashedPassword;
        private SymmetricCryptoKey _key;

        private readonly IAuthService _authService;

        public PasswordLogInStrategy(
            ICryptoService cryptoService,
            IApiService apiService,
            ITokenService tokenService,
            IAppIdService appIdService,
            IPlatformUtilsService platformUtilsService,
            IMessagingService messagingService,
            IStateService stateService,
            ITwoFactorService twoFactorService,
            IAuthService authService) : base(cryptoService, apiService, tokenService, appIdService, platformUtilsService, messagingService, stateService, twoFactorService)
        {
            _authService = authService;
        }

        protected async override Task OnSuccessfulLoginAsync(IdentityTokenResponse response)
        {
            await _cryptoService.SetKeyAsync(_key);
            await _cryptoService.SetKeyHashAsync(_localHashedPassword);
        }

        public async Task<AuthResult> LogInAsync(PasswordLogInCredentials credentials)
        {
            _key = await _authService.MakePreloginKeyAsync(credentials.MasterPassword, credentials.Email);

            // Hash the password early (before authentication) so we don't persist it in memory in plaintext
            _localHashedPassword = await _cryptoService.HashPasswordAsync(credentials.MasterPassword, _key, HashPurpose.LocalAuthorization);

            var hashedPassword = await _cryptoService.HashPasswordAsync(credentials.MasterPassword, _key);
            TokenRequest = new PasswordTokenRequest(credentials.Email, hashedPassword, credentials.CaptchaToken, await BuildTwoFactorAsync(credentials.TwoFactor), await BuildDeviceRequestAsync());

            return await StartLogInAsync();
        }
    }
}
