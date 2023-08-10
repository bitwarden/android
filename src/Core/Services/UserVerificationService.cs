using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Request;

namespace Bit.Core.Services
{
    public class UserVerificationService : IUserVerificationService
    {
        private readonly IApiService _apiService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly II18nService _i18nService;
        private readonly ICryptoService _cryptoService;
        private readonly IStateService _stateService;
        private readonly IKeyConnectorService _keyConnectorService;

        public UserVerificationService(IApiService apiService, IPlatformUtilsService platformUtilsService,
            II18nService i18nService, ICryptoService cryptoService, IStateService stateService, IKeyConnectorService keyConnectorService)
        {
            _apiService = apiService;
            _platformUtilsService = platformUtilsService;
            _i18nService = i18nService;
            _cryptoService = cryptoService;
            _stateService = stateService;
            _keyConnectorService = keyConnectorService;
        }

        async public Task<bool> VerifyUser(string secret, VerificationType verificationType)
        {
            if (string.IsNullOrEmpty(secret))
            {
                await InvalidSecretErrorAsync(verificationType);
                return false;
            }

            if (verificationType == VerificationType.OTP)
            {
                var request = new VerifyOTPRequest(secret);
                try
                {
                    await _apiService.PostAccountVerifyOTPAsync(request);
                }
                catch
                {
                    await InvalidSecretErrorAsync(verificationType);
                    return false;
                }
            }
            else
            {
                var passwordValid = await _cryptoService.CompareAndUpdateKeyHashAsync(secret, null);
                if (!passwordValid)
                {
                    await InvalidSecretErrorAsync(verificationType);
                    return false;
                }
            }

            return true;
        }

        async private Task InvalidSecretErrorAsync(VerificationType verificationType)
        {
            var errorMessage = verificationType == VerificationType.OTP
                ? _i18nService.T("InvalidVerificationCode")
                : _i18nService.T("InvalidMasterPassword");

            await _platformUtilsService.ShowDialogAsync(errorMessage);
        }

        public async Task<bool> HasMasterPasswordAsync()
        {
            var decryptOptions = await _stateService.GetAccountDecryptionOptions();
            if (decryptOptions != null)
            {
                return decryptOptions.HasMasterPassword;
            }

            return !await _keyConnectorService.GetUsesKeyConnectorAsync();
        }
    }
}
