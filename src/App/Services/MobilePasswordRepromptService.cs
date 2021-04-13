using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.App.Abstractions;
using Bit.App.Resources;

namespace Bit.App.Services
{
    public class MobilePasswordRepromptService : IPasswordRepromptService
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly ICryptoService _cryptoService;

        public MobilePasswordRepromptService(
            IDeviceActionService deviceActionService,
            IPlatformUtilsService platformUtilsService,
            ICryptoService cryptoService)
        {
            _deviceActionService = deviceActionService;
            _platformUtilsService = platformUtilsService;
            _cryptoService = cryptoService;
        }

        public string[] ProtectedFields { get; } = { "LoginTotp", "LoginPassword", "H_FieldValue", "CardNumber", "CardCode" };

        public async Task<bool> ShowPasswordPrompt()
        {
            var password = await _deviceActionService.DisplayPromptAync(AppResources.PasswordConfirmation,
                    AppResources.PasswordConfirmationDesc, null, AppResources.Submit, AppResources.Cancel, password: true);

            // Assume user has canceled.
            if (string.IsNullOrWhiteSpace(password))
            {
                return false;
            };

            var keyHash = await _cryptoService.HashPasswordAsync(password, null);
            var storedKeyHash = await _cryptoService.GetKeyHashAsync();

            if (storedKeyHash == null || keyHash == null || storedKeyHash != keyHash)
            {
                var retry = await _platformUtilsService.ShowDialogAsync(
                       AppResources.InvalidMasterPassword, null, AppResources.Ok);

                return false;
            }

            return true;
        }
    }
}
