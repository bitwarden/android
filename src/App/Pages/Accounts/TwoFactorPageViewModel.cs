using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.Request;
using Bit.Core.Utilities;
using System.Linq;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class TwoFactorPageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly IAuthService _authService;
        private readonly ISyncService _syncService;
        private readonly IStorageService _storageService;
        private readonly IApiService _apiService;
        private readonly IPlatformUtilsService _platformUtilsService;

        private bool _u2fSupported = false;
        private TwoFactorProviderType? _selectedProviderType;
        private string _twoFactorEmail;

        public TwoFactorPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _authService = ServiceContainer.Resolve<IAuthService>("authService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            _apiService = ServiceContainer.Resolve<IApiService>("apiService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
        }

        public string TwoFactorEmail
        {
            get => _twoFactorEmail;
            set => SetProperty(ref _twoFactorEmail, value);
        }

        public bool Remember { get; set; }

        public string Token { get; set; }

        public bool DuoMethod => SelectedProviderType == TwoFactorProviderType.Email;

        public bool YubikeyMethod => SelectedProviderType == TwoFactorProviderType.YubiKey;

        public bool AuthenticatorMethod => SelectedProviderType == TwoFactorProviderType.Authenticator;

        public bool EmailMethod => SelectedProviderType == TwoFactorProviderType.Email;

        public TwoFactorProviderType? SelectedProviderType
        {
            get => _selectedProviderType;
            set => SetProperty(ref _selectedProviderType, value, additionalPropertyNames: new string[]
            {
                nameof(EmailMethod),
                nameof(DuoMethod),
                nameof(YubikeyMethod),
                nameof(AuthenticatorMethod)
            });
        }

        public void Init()
        {
            if(string.IsNullOrWhiteSpace(_authService.Email) ||
                string.IsNullOrWhiteSpace(_authService.MasterPasswordHash) ||
                _authService.TwoFactorProvidersData == null)
            {
                // TODO: dismiss modal?
                return;
            }

            // TODO: init U2F
            _u2fSupported = false;

            var selectedProviderType = _authService.GetDefaultTwoFactorProvider(_u2fSupported);
            Load();
        }

        public void Load()
        {
            if(SelectedProviderType == null)
            {
                PageTitle = AppResources.LoginUnavailable;
                return;
            }
            PageTitle = _authService.TwoFactorProviders[SelectedProviderType.Value].Name;
            var providerData = _authService.TwoFactorProvidersData[SelectedProviderType.Value];
            switch(SelectedProviderType.Value)
            {
                case TwoFactorProviderType.U2f:
                    // TODO
                    break;
                case TwoFactorProviderType.Duo:
                case TwoFactorProviderType.OrganizationDuo:
                    // TODO: init duo
                    var host = providerData["Host"] as string;
                    var signature = providerData["Signature"] as string;
                    break;
                case TwoFactorProviderType.Email:
                    TwoFactorEmail = providerData["Email"] as string;
                    if(_authService.TwoFactorProvidersData.Count > 1)
                    {
                        var emailTask = Task.Run(() => SendEmailAsync(false, false));
                    }
                    break;
                default:
                    break;
            }
        }

        public async Task SubmitAsync()
        {
            if(SelectedProviderType == null)
            {
                return;
            }
            if(string.IsNullOrWhiteSpace(Token))
            {
                await _platformUtilsService.ShowDialogAsync(
                    string.Format(AppResources.ValidationFieldRequired, AppResources.VerificationCode),
                    AppResources.AnErrorHasOccurred);
            }
            if(SelectedProviderType == TwoFactorProviderType.Email ||
                SelectedProviderType == TwoFactorProviderType.Authenticator)
            {
                Token = Token.Replace(" ", string.Empty).Trim();
            }

            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.LoggingIn);
                await _authService.LogInTwoFactorAsync(SelectedProviderType.Value, Token, Remember);
                await _deviceActionService.HideLoadingAsync();
                var task = Task.Run(() => _syncService.FullSyncAsync(true));
                Application.Current.MainPage = new TabsPage();
            }
            catch(ApiException e)
            {
                await _deviceActionService.HideLoadingAsync();
                await _platformUtilsService.ShowDialogAsync(e.Error.GetSingleMessage(),
                    AppResources.AnErrorHasOccurred);
            }
        }

        public async Task AnotherMethodAsync()
        {
            var supportedProviders = _authService.GetSupportedTwoFactorProviders();
            var options = supportedProviders.Select(p => p.Name).ToList();
            options.Add(AppResources.RecoveryCodeTitle);
            var method = await Page.DisplayActionSheet(AppResources.TwoStepLoginOptions, AppResources.Cancel,
                null, options.ToArray());
            if(method == AppResources.RecoveryCodeTitle)
            {
                _platformUtilsService.LaunchUri("https://help.bitwarden.com/article/lost-two-step-device/");
            }
            else
            {
                SelectedProviderType = supportedProviders.FirstOrDefault(p => p.Name == method)?.Type;
                Load();
            }
        }

        public async Task<bool> SendEmailAsync(bool showLoading, bool doToast)
        {
            if(SelectedProviderType != TwoFactorProviderType.Email)
            {
                return false;
            }
            try
            {
                if(showLoading)
                {
                    await _deviceActionService.ShowLoadingAsync(AppResources.Submitting);
                }
                var request = new TwoFactorEmailRequest
                {
                    Email = _authService.Email,
                    MasterPasswordHash = _authService.MasterPasswordHash
                };
                await _apiService.PostTwoFactorEmailAsync(request);
                if(showLoading)
                {
                    await _deviceActionService.HideLoadingAsync();
                }
                if(doToast)
                {
                    _platformUtilsService.ShowToast("success", null, AppResources.VerificationEmailSent);
                }
                return true;
            }
            catch(ApiException)
            {
                if(showLoading)
                {
                    await _deviceActionService.HideLoadingAsync();
                }
                await _platformUtilsService.ShowDialogAsync(AppResources.VerificationEmailNotSent);
                return false;
            }
        }
    }
}
