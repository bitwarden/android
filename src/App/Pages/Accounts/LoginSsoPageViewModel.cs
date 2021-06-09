using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System;
using System.Threading.Tasks;
using Bit.App.Utilities;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.Domain;
using Xamarin.Essentials;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class LoginSsoPageViewModel : BaseViewModel
    {
        private const string Keys_RememberedOrgIdentifier = "rememberedOrgIdentifier";
        private const string Keys_RememberOrgIdentifier = "rememberOrgIdentifier";

        private readonly IDeviceActionService _deviceActionService;
        private readonly IAuthService _authService;
        private readonly ISyncService _syncService;
        private readonly IApiService _apiService;
        private readonly IPasswordGenerationService _passwordGenerationService;
        private readonly ICryptoFunctionService _cryptoFunctionService;
        private readonly IStorageService _storageService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IStateService _stateService;

        private string _orgIdentifier;

        public LoginSsoPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _authService = ServiceContainer.Resolve<IAuthService>("authService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");
            _apiService = ServiceContainer.Resolve<IApiService>("apiService");
            _passwordGenerationService =
                ServiceContainer.Resolve<IPasswordGenerationService>("passwordGenerationService");
            _cryptoFunctionService = ServiceContainer.Resolve<ICryptoFunctionService>("cryptoFunctionService");
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");

            PageTitle = AppResources.Bitwarden;
            LogInCommand = new Command(async () => await LogInAsync());
        }

        public string OrgIdentifier
        {
            get => _orgIdentifier;
            set => SetProperty(ref _orgIdentifier, value);
        }

        public Command LogInCommand { get; }
        public bool RememberOrgIdentifier { get; set; }
        public Action StartTwoFactorAction { get; set; }
        public Action StartSetPasswordAction { get; set; }
        public Action SsoAuthSuccessAction { get; set; }
        public Action CloseAction { get; set; }

        public async Task InitAsync()
        {
            if (string.IsNullOrWhiteSpace(OrgIdentifier))
            {
                OrgIdentifier = await _storageService.GetAsync<string>(Keys_RememberedOrgIdentifier);
            }
            var rememberOrgIdentifier = await _storageService.GetAsync<bool?>(Keys_RememberOrgIdentifier);
            RememberOrgIdentifier = rememberOrgIdentifier.GetValueOrDefault(true);
        }

        public async Task LogInAsync()
        {
            if (Connectivity.NetworkAccess == NetworkAccess.None)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.InternetConnectionRequiredMessage,
                    AppResources.InternetConnectionRequiredTitle);
                return;
            }
            if (string.IsNullOrWhiteSpace(OrgIdentifier))
            {
                await _platformUtilsService.ShowDialogAsync(
                    string.Format(AppResources.ValidationFieldRequired, AppResources.OrgIdentifier),
                    AppResources.AnErrorHasOccurred,
                    AppResources.Ok);
                return;
            }

            await _deviceActionService.ShowLoadingAsync(AppResources.LoggingIn);

            try
            {
                await _apiService.PreValidateSso(OrgIdentifier);
            }
            catch (ApiException e)
            {
                await _deviceActionService.HideLoadingAsync();
                await _platformUtilsService.ShowDialogAsync(
                    (e?.Error != null ? e.Error.GetSingleMessage() : AppResources.LoginSsoError),
                    AppResources.AnErrorHasOccurred);
                return;
            }

            var passwordOptions = new PasswordGenerationOptions(true);
            passwordOptions.Length = 64;

            var codeVerifier = await _passwordGenerationService.GeneratePasswordAsync(passwordOptions);
            var codeVerifierHash = await _cryptoFunctionService.HashAsync(codeVerifier, CryptoHashAlgorithm.Sha256);
            var codeChallenge = CoreHelpers.Base64UrlEncode(codeVerifierHash);

            var state = await _passwordGenerationService.GeneratePasswordAsync(passwordOptions);

            var redirectUri = "bitwarden://sso-callback";

            var url = _apiService.IdentityBaseUrl + "/connect/authorize?" +
                      "client_id=" + _platformUtilsService.IdentityClientId + "&" +
                      "redirect_uri=" + Uri.EscapeDataString(redirectUri) + "&" +
                      "response_type=code&scope=api%20offline_access&" +
                      "state=" + state + "&code_challenge=" + codeChallenge + "&" +
                      "code_challenge_method=S256&response_mode=query&" +
                      "domain_hint=" + Uri.EscapeDataString(OrgIdentifier);

            WebAuthenticatorResult authResult = null;
            bool cancelled = false;
            try
            {
                authResult = await WebAuthenticator.AuthenticateAsync(new Uri(url),
                    new Uri(redirectUri));
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
            if (!cancelled)
            {
                var code = GetResultCode(authResult, state);
                if (!string.IsNullOrEmpty(code))
                {
                    await LogIn(code, codeVerifier, redirectUri);
                }
                else
                {
                    await _deviceActionService.HideLoadingAsync();
                    await _platformUtilsService.ShowDialogAsync(AppResources.LoginSsoError,
                        AppResources.AnErrorHasOccurred);
                }
            }
        }

        private string GetResultCode(WebAuthenticatorResult authResult, string state)
        {
            string code = null;
            if (authResult != null)
            {
                authResult.Properties.TryGetValue("state", out var resultState);
                if (resultState == state)
                {
                    authResult.Properties.TryGetValue("code", out var resultCode);
                    code = resultCode;
                }
            }
            return code;
        }

        private async Task LogIn(string code, string codeVerifier, string redirectUri)
        {
            try
            {
                var response = await _authService.LogInSsoAsync(code, codeVerifier, redirectUri);
                await AppHelpers.ResetInvalidUnlockAttemptsAsync();
                if (RememberOrgIdentifier)
                {
                    await _storageService.SaveAsync(Keys_RememberedOrgIdentifier, OrgIdentifier);
                }
                else
                {
                    await _storageService.RemoveAsync(Keys_RememberedOrgIdentifier);
                }
                await _deviceActionService.HideLoadingAsync();
                if (response.TwoFactor)
                {
                    StartTwoFactorAction?.Invoke();
                }
                else if (response.ResetMasterPassword)
                {
                    StartSetPasswordAction?.Invoke();
                }
                else
                {
                    var disableFavicon = await _storageService.GetAsync<bool?>(Constants.DisableFaviconKey);
                    await _stateService.SaveAsync(Constants.DisableFaviconKey, disableFavicon.GetValueOrDefault());
                    var task = Task.Run(async () => await _syncService.FullSyncAsync(true));
                    SsoAuthSuccessAction?.Invoke();
                }
            }
            catch (Exception e)
            {
                await _deviceActionService.HideLoadingAsync();
                await _platformUtilsService.ShowDialogAsync(AppResources.LoginSsoError,
                    AppResources.AnErrorHasOccurred);
            }
        }
    }
}
