using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Utilities;
using System.Threading.Tasks;
using System.Windows.Input;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class LoginPageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly IAuthService _authService;
        private readonly ISyncService _syncService;

        public LoginPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _authService = ServiceContainer.Resolve<IAuthService>("authService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");

            PageTitle = AppResources.Bitwarden;
            ShowPasswordCommand = new Command(() =>
                Page.DisplayAlert("Button 1 Command", "Button 1 message", "Cancel"));
        }

        public ICommand ShowPasswordCommand { get; }
        public string Email { get; set; }
        public string MasterPassword { get; set; }

        public async Task LogInAsync()
        {
            if(string.IsNullOrWhiteSpace(Email))
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred,
                    string.Format(AppResources.ValidationFieldRequired, AppResources.EmailAddress),
                    AppResources.Ok);
                return;
            }
            if(!Email.Contains("@"))
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred, AppResources.InvalidEmail, AppResources.Ok);
                return;
            }
            if(string.IsNullOrWhiteSpace(MasterPassword))
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred,
                    string.Format(AppResources.ValidationFieldRequired, AppResources.MasterPassword),
                    AppResources.Ok);
                return;
            }

            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.LoggingIn);
                var response = await _authService.LogInAsync(Email, MasterPassword);
                await _deviceActionService.HideLoadingAsync();
                // TODO: remember email?
                if(response.TwoFactor)
                {
                    // TODO: 2fa page
                }
                else
                {
                    var task = Task.Run(async () => await _syncService.FullSyncAsync(true));
                    Application.Current.MainPage = new TabsPage();
                }
            }
            catch(ApiException e)
            {
                await _deviceActionService.HideLoadingAsync();
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred, e.Error.GetSingleMessage(), AppResources.Ok);
            }
        }
    }
}
