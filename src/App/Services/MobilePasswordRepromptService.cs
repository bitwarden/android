using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.App.Abstractions;
using Bit.App.Resources;
using System;

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

        public async Task<bool> ShowPasswordPromptAsync()
        {
            Func<string, Task<bool>> validator = async (string password) =>
            {
                // Assume user has canceled.
                if (string.IsNullOrWhiteSpace(password))
                {
                    return false;
                };

                var keyHash = await _cryptoService.HashPasswordAsync(password, null, Core.Enums.HashPurpose.LocalAuthorization);
                var storedKeyHash = await _cryptoService.GetKeyHashAsync();

                if (storedKeyHash == null || keyHash == null || storedKeyHash != keyHash)
                {
                    return false;
                }

                return true;
            };

            return await _platformUtilsService.ShowPasswordDialogAsync(AppResources.PasswordConfirmation, AppResources.PasswordConfirmationDesc, validator);
        }
    }
}
