using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Utilities;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class LoginPageViewModel : BaseViewModel
    {
        private const string Keys_RememberedEmail = "rememberedEmail";
        private const string Keys_RememberEmail = "rememberEmail";

        private readonly IDeviceActionService _deviceActionService;
        private readonly IAuthService _authService;
        private readonly ISyncService _syncService;
        private readonly IStorageService _storageService;

        private bool _showPassword;
        private string _email;

        public LoginPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _authService = ServiceContainer.Resolve<IAuthService>("authService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");

            PageTitle = AppResources.Bitwarden;
            TogglePasswordCommand = new Command(TogglePassword);
        }

        public bool ShowPassword
        {
            get => _showPassword;
            set => SetProperty(ref _showPassword, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ShowPasswordIcon)
                });
        }

        public string Email
        {
            get => _email;
            set => SetProperty(ref _email, value);
        }

        public Command TogglePasswordCommand { get; }
        public string ShowPasswordIcon => ShowPassword ? "" : "";
        public string MasterPassword { get; set; }
        public bool RememberEmail { get; set; }

        public async Task InitAsync()
        {
            if(string.IsNullOrWhiteSpace(Email))
            {
                Email = await _storageService.GetAsync<string>(Keys_RememberedEmail);
            }
            var rememberEmail = await _storageService.GetAsync<bool?>(Keys_RememberEmail);
            RememberEmail = rememberEmail.GetValueOrDefault(true);
        }

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
                if(RememberEmail)
                {
                    await _storageService.SaveAsync(Keys_RememberedEmail, Email);
                }
                else
                {
                    await _storageService.RemoveAsync(Keys_RememberedEmail);
                }
                await _deviceActionService.HideLoadingAsync();
                if(response.TwoFactor)
                {
                    var page = new TwoFactorPage();
                    await Page.Navigation.PushModalAsync(new NavigationPage(page));
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

        public void TogglePassword()
        {
            ShowPassword = !ShowPassword;
            (Page as LoginPage).MasterPasswordEntry.Focus();
        }
    }
}
