using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.Request;
using Bit.Core.Utilities;
using System;
using System.Linq;
using System.Net;
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
        private readonly IEnvironmentService _environmentService;
        private readonly IMessagingService _messagingService;
        private readonly IBroadcasterService _broadcasterService;
        private readonly IStateService _stateService;

        private bool _u2fSupported = false;
        private TwoFactorProviderType? _selectedProviderType;
        private string _totpInstruction;
        private string _webVaultUrl = "https://vault.bitwarden.com";

        public TwoFactorPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _authService = ServiceContainer.Resolve<IAuthService>("authService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            _apiService = ServiceContainer.Resolve<IApiService>("apiService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _environmentService = ServiceContainer.Resolve<IEnvironmentService>("environmentService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");

            PageTitle = AppResources.TwoStepLogin;
            SubmitCommand = new Command(async () => await SubmitAsync());
        }

        public string TotpInstruction
        {
            get => _totpInstruction;
            set => SetProperty(ref _totpInstruction, value);
        }

        public bool Remember { get; set; }

        public string Token { get; set; }

        public bool DuoMethod => SelectedProviderType == TwoFactorProviderType.Duo ||
            SelectedProviderType == TwoFactorProviderType.OrganizationDuo;

        public bool YubikeyMethod => SelectedProviderType == TwoFactorProviderType.YubiKey;

        public bool AuthenticatorMethod => SelectedProviderType == TwoFactorProviderType.Authenticator;

        public bool EmailMethod => SelectedProviderType == TwoFactorProviderType.Email;

        public bool TotpMethod => AuthenticatorMethod || EmailMethod;

        public bool ShowTryAgain => YubikeyMethod && Device.RuntimePlatform == Device.iOS;

        public string YubikeyInstruction => Device.RuntimePlatform == Device.iOS ? AppResources.YubiKeyInstructionIos :
            AppResources.YubiKeyInstruction;

        public TwoFactorProviderType? SelectedProviderType
        {
            get => _selectedProviderType;
            set => SetProperty(ref _selectedProviderType, value, additionalPropertyNames: new string[]
            {
                nameof(EmailMethod),
                nameof(DuoMethod),
                nameof(YubikeyMethod),
                nameof(AuthenticatorMethod),
                nameof(TotpMethod),
                nameof(ShowTryAgain),
            });
        }
        public Command SubmitCommand { get; }
        public Action TwoFactorAction { get; set; }

        public void Init()
        {
            if (string.IsNullOrWhiteSpace(_authService.Email) ||
                string.IsNullOrWhiteSpace(_authService.MasterPasswordHash) ||
                _authService.TwoFactorProvidersData == null)
            {
                // TODO: dismiss modal?
                return;
            }

            if (!string.IsNullOrWhiteSpace(_environmentService.BaseUrl))
            {
                _webVaultUrl = _environmentService.BaseUrl;
            }
            else if (!string.IsNullOrWhiteSpace(_environmentService.WebVaultUrl))
            {
                _webVaultUrl = _environmentService.WebVaultUrl;
            }

            // TODO: init U2F
            _u2fSupported = false;

            SelectedProviderType = _authService.GetDefaultTwoFactorProvider(_u2fSupported);
            Load();
        }

        public void Load()
        {
            if (SelectedProviderType == null)
            {
                PageTitle = AppResources.LoginUnavailable;
                return;
            }
            var page = Page as TwoFactorPage;
            PageTitle = _authService.TwoFactorProviders[SelectedProviderType.Value].Name;
            var providerData = _authService.TwoFactorProvidersData[SelectedProviderType.Value];
            switch (SelectedProviderType.Value)
            {
                case TwoFactorProviderType.U2f:
                    // TODO
                    break;
                case TwoFactorProviderType.YubiKey:
                    _messagingService.Send("listenYubiKeyOTP", true);
                    break;
                case TwoFactorProviderType.Duo:
                case TwoFactorProviderType.OrganizationDuo:
                    var host = WebUtility.UrlEncode(providerData["Host"] as string);
                    var req = WebUtility.UrlEncode(providerData["Signature"] as string);
                    page.DuoWebView.Uri = $"{_webVaultUrl}/duo-connector.html?host={host}&request={req}";
                    page.DuoWebView.RegisterAction(sig =>
                    {
                        Token = sig;
                        App.WaitForResume();
                        Device.BeginInvokeOnMainThread(async () => await SubmitAsync());
                    });
                    break;
                case TwoFactorProviderType.Email:
                    TotpInstruction = string.Format(AppResources.EnterVerificationCodeEmail,
                        providerData["Email"] as string);
                    if (_authService.TwoFactorProvidersData.Count > 1)
                    {
                        var emailTask = Task.Run(() => SendEmailAsync(false, false));
                    }
                    break;
                case TwoFactorProviderType.Authenticator:
                    TotpInstruction = AppResources.EnterVerificationCodeApp;
                    break;
                default:
                    break;
            }

            if (!YubikeyMethod)
            {
                _messagingService.Send("listenYubiKeyOTP", false);
            }
            if (SelectedProviderType == null || DuoMethod)
            {
                page.RemoveContinueButton();
            }
            else
            {
                page.AddContinueButton();
            }
        }

        public async Task SubmitAsync()
        {
            if (SelectedProviderType == null)
            {
                return;
            }
            if (Xamarin.Essentials.Connectivity.NetworkAccess == Xamarin.Essentials.NetworkAccess.None)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.InternetConnectionRequiredMessage,
                    AppResources.InternetConnectionRequiredTitle);
                return;
            }
            if (string.IsNullOrWhiteSpace(Token))
            {
                await _platformUtilsService.ShowDialogAsync(
                    string.Format(AppResources.ValidationFieldRequired, AppResources.VerificationCode),
                    AppResources.AnErrorHasOccurred);
                return;
            }
            if (SelectedProviderType == TwoFactorProviderType.Email ||
                SelectedProviderType == TwoFactorProviderType.Authenticator)
            {
                Token = Token.Replace(" ", string.Empty).Trim();
            }

            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.Validating);
                await _authService.LogInTwoFactorAsync(SelectedProviderType.Value, Token, Remember);
                await _deviceActionService.HideLoadingAsync();
                var task = Task.Run(() => _syncService.FullSyncAsync(true));
                _messagingService.Send("listenYubiKeyOTP", false);
                _broadcasterService.Unsubscribe(nameof(TwoFactorPage));
                var disableFavicon = await _storageService.GetAsync<bool?>(Constants.DisableFaviconKey);
                await _stateService.SaveAsync(Constants.DisableFaviconKey, disableFavicon.GetValueOrDefault());
                TwoFactorAction?.Invoke();
            }
            catch (ApiException e)
            {
                await _deviceActionService.HideLoadingAsync();
                if (e?.Error != null)
                {
                    await _platformUtilsService.ShowDialogAsync(e.Error.GetSingleMessage(),
                        AppResources.AnErrorHasOccurred);
                }
            }
        }

        public async Task AnotherMethodAsync()
        {
            var supportedProviders = _authService.GetSupportedTwoFactorProviders();
            var options = supportedProviders.Select(p => p.Name).ToList();
            options.Add(AppResources.RecoveryCodeTitle);
            var method = await Page.DisplayActionSheet(AppResources.TwoStepLoginOptions, AppResources.Cancel,
                null, options.ToArray());
            if (method == AppResources.RecoveryCodeTitle)
            {
                _platformUtilsService.LaunchUri("https://help.bitwarden.com/article/lost-two-step-device/");
            }
            else if (method != AppResources.Cancel)
            {
                var selected = supportedProviders.FirstOrDefault(p => p.Name == method)?.Type;
                if (selected == SelectedProviderType)
                {
                    // Nothing changed
                    return;
                }
                SelectedProviderType = selected;
                Load();
            }
        }

        public async Task<bool> SendEmailAsync(bool showLoading, bool doToast)
        {
            if (!EmailMethod)
            {
                return false;
            }
            if (Xamarin.Essentials.Connectivity.NetworkAccess == Xamarin.Essentials.NetworkAccess.None)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.InternetConnectionRequiredMessage,
                    AppResources.InternetConnectionRequiredTitle);
                return false;
            }
            try
            {
                if (showLoading)
                {
                    await _deviceActionService.ShowLoadingAsync(AppResources.Submitting);
                }
                var request = new TwoFactorEmailRequest
                {
                    Email = _authService.Email,
                    MasterPasswordHash = _authService.MasterPasswordHash
                };
                await _apiService.PostTwoFactorEmailAsync(request);
                if (showLoading)
                {
                    await _deviceActionService.HideLoadingAsync();
                }
                if (doToast)
                {
                    _platformUtilsService.ShowToast("success", null, AppResources.VerificationEmailSent);
                }
                return true;
            }
            catch (ApiException)
            {
                if (showLoading)
                {
                    await _deviceActionService.HideLoadingAsync();
                }
                await _platformUtilsService.ShowDialogAsync(AppResources.VerificationEmailNotSent);
                return false;
            }
        }
    }
}
