using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Utilities;
using System;
using System.Threading.Tasks;
using Bit.App.Utilities;
using Xamarin.Forms;
using Newtonsoft.Json;
using System.Text;
using Xamarin.Essentials;
using System.Text.RegularExpressions;

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
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IStateService _stateService;
        private readonly IEnvironmentService _environmentService;
        private readonly II18nService _i18nService;

        private bool _showPassword;
        private string _email;
        private string _masterPassword;
        private string _captchaToken = null;
        private bool _loading = false;

        public LoginPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _authService = ServiceContainer.Resolve<IAuthService>("authService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _environmentService = ServiceContainer.Resolve<IEnvironmentService>("environmentService");
            _i18nService = ServiceContainer.Resolve<II18nService>("i18nService");

            PageTitle = AppResources.Bitwarden;
            TogglePasswordCommand = new Command(TogglePassword);
            LogInCommand = new Command(async () => await LogInAsync());
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

        public string MasterPassword
        {
            get => _masterPassword;
            set => SetProperty(ref _masterPassword, value);
        }

        public Command LogInCommand { get; }
        public Command TogglePasswordCommand { get; }
        public string ShowPasswordIcon => ShowPassword ? "" : "";
        public bool RememberEmail { get; set; }
        public Action StartTwoFactorAction { get; set; }
        public Action LogInSuccessAction { get; set; }
        public Action CloseAction { get; set; }
                
        public async Task InitAsync()
        {
            if (string.IsNullOrWhiteSpace(Email))
            {
                Email = await _storageService.GetAsync<string>(Keys_RememberedEmail);
            }
            var rememberEmail = await _storageService.GetAsync<bool?>(Keys_RememberEmail);
            RememberEmail = rememberEmail.GetValueOrDefault(true);
        }

        public async Task LogInAsync()
        {
            if (Xamarin.Essentials.Connectivity.NetworkAccess == Xamarin.Essentials.NetworkAccess.None)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.InternetConnectionRequiredMessage,
                    AppResources.InternetConnectionRequiredTitle);
                return;
            }
            if (string.IsNullOrWhiteSpace(Email))
            {
                await _platformUtilsService.ShowDialogAsync(
                    string.Format(AppResources.ValidationFieldRequired, AppResources.EmailAddress),
                    AppResources.AnErrorHasOccurred,
                    AppResources.Ok);
                return;
            }
            if (!Email.Contains("@"))
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.InvalidEmail, AppResources.AnErrorHasOccurred,
                    AppResources.Ok);
                return;
            }
            if (string.IsNullOrWhiteSpace(MasterPassword))
            {
                await _platformUtilsService.ShowDialogAsync(
                    string.Format(AppResources.ValidationFieldRequired, AppResources.MasterPassword),
                    AppResources.AnErrorHasOccurred,
                    AppResources.Ok);
                return;
            }

            ShowPassword = false;
            try
            {
                if (!_loading)
                {
                    await _deviceActionService.ShowLoadingAsync(AppResources.LoggingIn);
                    _loading = true;
                }

                var response = await _authService.LogInAsync(Email, MasterPassword, _captchaToken);
                if (RememberEmail)
                {
                    await _storageService.SaveAsync(Keys_RememberedEmail, Email);
                }
                else
                {
                    await _storageService.RemoveAsync(Keys_RememberedEmail);
                }
                await AppHelpers.ResetInvalidUnlockAttemptsAsync();
                await _deviceActionService.HideLoadingAsync();

                if (response.CaptchaNeeded)
                {
                    var callbackUri = "bitwarden://captcha-callback";
                    var data = EncodeDataParameter(new
                    {
                        siteKey = response.CaptchaSiteKey,
                        locale = _i18nService.Culture.TwoLetterISOLanguageName,
                        callbackUri = callbackUri,
                        captchaRequiredText = AppResources.CaptchaRequired,
                    });

                    var url = _environmentService.WebVaultUrl + "/captcha-mobile-connector.html?" + "data=" + data +
                        "&parent=" + Uri.EscapeDataString(callbackUri) + "&v=1";

                    WebAuthenticatorResult authResult = null;
                    bool cancelled = false;
                    try
                    {
                        authResult = await WebAuthenticator.AuthenticateAsync(new Uri(url),
                            new Uri(callbackUri));
                    }
                    catch (TaskCanceledException taskCanceledException)
                    {
                        await _deviceActionService.HideLoadingAsync();
                        cancelled = true;
                    }
                    catch (Exception e)
                    {
                        // WebAuthenticator throws NSErrorException if iOS flow is cancelled - by setting cancelled to true
                        // here we maintain the appearance of a clean cancellation (we don't want to do this across the board
                        // because we still want to present legitimate errors).  If/when this is fixed, we can remove this
                        // particular catch block (catching taskCanceledException above must remain)
                        // https://github.com/xamarin/Essentials/issues/1240
                        if (Device.RuntimePlatform == Device.iOS)
                        {
                            await _deviceActionService.HideLoadingAsync();
                            cancelled = true;
                        }
                    }

                    if (cancelled == false && authResult != null &&
                        authResult.Properties.TryGetValue("token", out _captchaToken))
                    {
                        await LogInAsync();
                        return;
                    }
                    else
                    {
                        await _platformUtilsService.ShowDialogAsync(AppResources.CaptchaFailed,
                            AppResources.CaptchaRequired);
                        _loading = false;
                        return;
                    }
                }
                MasterPassword = string.Empty;
                _captchaToken = null;

                if (response.TwoFactor)
                {
                    StartTwoFactorAction?.Invoke();
                }
                else
                {
                    var disableFavicon = await _storageService.GetAsync<bool?>(Constants.DisableFaviconKey);
                    await _stateService.SaveAsync(Constants.DisableFaviconKey, disableFavicon.GetValueOrDefault());
                    var task = Task.Run(async () => await _syncService.FullSyncAsync(true));
                    LogInSuccessAction?.Invoke();
                }
            }
            catch (ApiException e)
            {
                _captchaToken = null;
                MasterPassword = string.Empty;
                await _deviceActionService.HideLoadingAsync();
                if (e?.Error != null)
                {
                    await _platformUtilsService.ShowDialogAsync(e.Error.GetSingleMessage(),
                        AppResources.AnErrorHasOccurred);
                }
            }
            _loading = false;
        }

        public void TogglePassword()
        {
            ShowPassword = !ShowPassword;
            (Page as LoginPage).MasterPasswordEntry.Focus();
        }

        private string EncodeDataParameter(object obj)
        {
            string EncodeMultibyte(Match match)
            {
                return Convert.ToChar(Convert.ToUInt32($"0x{match.Groups[1].Value}", 16)).ToString();
            }

            var escaped = Uri.EscapeDataString(JsonConvert.SerializeObject(obj));
            var multiByteEscaped = Regex.Replace(escaped, "%([0-9A-F]{2})", EncodeMultibyte);
            return Convert.ToBase64String(Encoding.UTF8.GetBytes(multiByteEscaped));
        }
    }
}
