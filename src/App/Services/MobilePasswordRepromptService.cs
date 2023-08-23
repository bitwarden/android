using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Resources;
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
        private readonly IStorageService _secureStorageService;

        public MobilePasswordRepromptService(IPlatformUtilsService platformUtilsService, ICryptoService cryptoService, IStateService stateService, IStorageService storageService)
        {
            _platformUtilsService = platformUtilsService;
            _cryptoService = cryptoService;
            _stateService = stateService;
            _secureStorageService = storageService;
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

            var email = await _stateService.GetEmailAsync();
            var kdfConfig = await _stateService.GetActiveUserCustomDataAsync(a => new KdfConfig(a?.Profile));
            var masterKey = await _cryptoService.MakeMasterKeyAsync(password, email, kdfConfig);

            var storedPasswordHash = await _cryptoService.GetMasterKeyHashAsync();
            if (storedPasswordHash == null)
            {
                var oldKey = await _secureStorageService.GetAsync<string>("oldKey");
                if (masterKey.KeyB64 == oldKey)
                {
                    var localPasswordHash = await _cryptoService.HashMasterKeyAsync(password, masterKey, HashPurpose.LocalAuthorization);
                    await _secureStorageService.RemoveAsync("oldKey");
                    await _cryptoService.SetMasterKeyHashAsync(localPasswordHash);
                }
            }

            var passwordValid = await _cryptoService.CompareAndUpdateKeyHashAsync(password, masterKey);
            if (passwordValid)
            {
                await AppHelpers.ResetInvalidUnlockAttemptsAsync();

                var userKey = await _cryptoService.DecryptUserKeyWithMasterKeyAsync(masterKey);
                await _cryptoService.SetMasterKeyAsync(masterKey);
                var hasKey = await _cryptoService.HasUserKeyAsync();
                if (!hasKey)
                {
                    await _cryptoService.SetUserKeyAsync(userKey);
                }
            }

            return passwordValid;
        }

        private async Task<bool> ShouldByPassMasterPasswordRepromptAsync()
        {
            return await _cryptoService.GetMasterKeyHashAsync() is null;
        }
    }
}
