using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.Core.Resources.Localization;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Enums;

namespace Bit.App.Services
{
    public class MobilePasswordRepromptService : IPasswordRepromptService
    {
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly ICryptoService _cryptoService;
        private readonly IStateService _stateService;

        public MobilePasswordRepromptService(IPlatformUtilsService platformUtilsService, ICryptoService cryptoService, IStateService stateService)
        {
            _platformUtilsService = platformUtilsService;
            _cryptoService = cryptoService;
            _stateService = stateService;
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

            var masterKey = await _cryptoService.GetOrDeriveMasterKeyAsync(password);
            var passwordValid = await _cryptoService.CompareAndUpdateKeyHashAsync(password, masterKey);
            if (passwordValid)
            {
                await AppHelpers.ResetInvalidUnlockAttemptsAsync();

                await _cryptoService.UpdateMasterKeyAndUserKeyAsync(masterKey);
            }

            return passwordValid;
        }

        public async Task<bool> ShouldByPassMasterPasswordRepromptAsync()
        {
            return await _cryptoService.GetMasterKeyHashAsync() is null;
        }
    }
}
