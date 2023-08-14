using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Enums;

namespace Bit.App.Services
{
    public class MobilePasswordRepromptService : IPasswordRepromptService
    {
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly ICryptoService _cryptoService;

        public MobilePasswordRepromptService(IPlatformUtilsService platformUtilsService, ICryptoService cryptoService)
        {
            _platformUtilsService = platformUtilsService;
            _cryptoService = cryptoService;
        }

        public string[] ProtectedFields { get; } = { "LoginTotp", "LoginPassword", "H_FieldValue", "CardNumber", "CardCode" };

        public async Task<bool> PromptAndCheckPasswordIfNeededAsync(CipherRepromptType repromptType = CipherRepromptType.Password)
        {
            if (repromptType == CipherRepromptType.None || await ShouldByPassMasterPasswordRepromptAsync())
            {
                return true;
            }

            return await _platformUtilsService.ShowPasswordDialogAsync(AppResources.PasswordConfirmation, AppResources.PasswordConfirmationDesc, ValidatePasswordAsync);
        }

        public async Task<(string password, bool valid)> ShowPasswordPromptAndGetItAsync()
        {
            return await _platformUtilsService.ShowPasswordDialogAndGetItAsync(AppResources.PasswordConfirmation, AppResources.PasswordConfirmationDesc, ValidatePasswordAsync);
        }

        private async Task<bool> ValidatePasswordAsync(string password)
        {
            // Assume user has canceled.
            if (string.IsNullOrWhiteSpace(password))
            {
                return false;
            };

            return await _cryptoService.CompareAndUpdateKeyHashAsync(password, null);
        }

        private async Task<bool> ShouldByPassMasterPasswordRepromptAsync()
        {
            return await _cryptoService.GetMasterKeyHashAsync() is null;
        }
    }
}
