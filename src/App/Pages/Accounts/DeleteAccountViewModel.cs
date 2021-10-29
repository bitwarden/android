using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Utilities;
using Microsoft.AppCenter.Crashes;

namespace Bit.App.Pages
{
    public class DeleteAccountViewModel : BaseViewModel
    {
        readonly IApiService _apiService;
        readonly IPasswordRepromptService _passwordRepromptService;
        readonly IMessagingService _messagingService;
        readonly ICryptoService _cryptoService;
        readonly IPlatformUtilsService _platformUtilsService;
        readonly IDeviceActionService _deviceActionService;

        public DeleteAccountViewModel()
        {
            _apiService = ServiceContainer.Resolve<IApiService>("apiService");
            _passwordRepromptService = ServiceContainer.Resolve<IPasswordRepromptService>("passwordRepromptService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _cryptoService = ServiceContainer.Resolve<ICryptoService>("cryptoService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");

            PageTitle = AppResources.DeleteAccount;
        }

        public async Task DeleteAccountAsync()
        {
            try
            {
                if (Xamarin.Essentials.Connectivity.NetworkAccess == Xamarin.Essentials.NetworkAccess.None)
                {
                    await _platformUtilsService.ShowDialogAsync(AppResources.InternetConnectionRequiredMessage,
                        AppResources.InternetConnectionRequiredTitle, AppResources.Ok);
                    return;
                }

                var (password, valid) = await _passwordRepromptService.ShowPasswordPromptAndGetItAsync();
                if (!valid)
                {
                    return;
                }

                await _deviceActionService.ShowLoadingAsync(AppResources.DeletingYourAccount);

                var masterPasswordHashKey = await _cryptoService.HashPasswordAsync(password, null);
                await _apiService.DeleteAccountAsync(new Core.Models.Request.DeleteAccountRequest
                {
                    MasterPasswordHash = masterPasswordHashKey
                });

                await _deviceActionService.HideLoadingAsync();

                _messagingService.Send("logout");

                await _platformUtilsService.ShowDialogAsync(AppResources.YourAccountHasBeenPermanentlyDeleted);
            }
            catch (ApiException apiEx)
            {
                await _deviceActionService.HideLoadingAsync();

                if (apiEx?.Error != null)
                {
                    await _platformUtilsService.ShowDialogAsync(apiEx.Error.GetSingleMessage(), AppResources.AnErrorHasOccurred);
                }
            }
            catch (System.Exception ex)
            {
                await _deviceActionService.HideLoadingAsync();
#if !FDROID
                Crashes.TrackError(ex);
#endif
                await _platformUtilsService.ShowDialogAsync(AppResources.AnErrorHasOccurred);
            }
        }
    }
}
