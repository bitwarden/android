using System;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.Domain;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Essentials;

namespace Bit.App.Pages
{
    public class LoginSsoPageViewModel : BaseViewModel
    {
        private const string REDIRECT_URI = "bitwarden://sso-callback";

        private readonly IDeviceActionService _deviceActionService;
        private readonly IAuthService _authService;
        private readonly ISyncService _syncService;
        private readonly IApiService _apiService;
        private readonly IPasswordGenerationService _passwordGenerationService;
        private readonly ICryptoFunctionService _cryptoFunctionService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IStateService _stateService;
        private readonly ILogger _logger;
        private readonly IOrganizationService _organizationService;

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
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _logger = ServiceContainer.Resolve<ILogger>("logger");
            _organizationService = ServiceContainer.Resolve<IOrganizationService>();


            PageTitle = AppResources.Bitwarden;
            LogInCommand = new AsyncCommand(LogInAsync, allowsMultipleExecutions: false);
        }

        public string OrgIdentifier
        {
            get => _orgIdentifier;
            set => SetProperty(ref _orgIdentifier, value);
        }

        public ICommand LogInCommand { get; }
        public Action StartTwoFactorAction { get; set; }
        public Action StartSetPasswordAction { get; set; }
        public Action SsoAuthSuccessAction { get; set; }
        public Action CloseAction { get; set; }
        public Action UpdateTempPasswordAction { get; set; }

        public async Task InitAsync()
        {
            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.Loading);
                if (await TryClaimedDomainLogin())
                {
                    return;
                }

                if (string.IsNullOrWhiteSpace(OrgIdentifier))
                {
                    OrgIdentifier = await _stateService.GetRememberedOrgIdentifierAsync();
                }
            }
            catch (Exception ex)
            {
                _logger.Exception(ex);
            }
            finally
            {
                await _deviceActionService.HideLoadingAsync();
            }
        }

        public async Task LogInAsync()
        {
            try
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

                var response = await _apiService.PreValidateSso(OrgIdentifier);

                if (string.IsNullOrWhiteSpace(response?.Token))
                {
                    _logger.Error(response is null ? "Login SSO Error: response is null" : "Login SSO Error: response.Token is null or whitespace");
                    await _deviceActionService.HideLoadingAsync();
                    await _platformUtilsService.ShowDialogAsync(AppResources.LoginSsoError);
                    return;
                }

                var ssoToken = response.Token;


                var passwordOptions = new PasswordGenerationOptions(true);
                passwordOptions.Length = 64;

                var codeVerifier = await _passwordGenerationService.GeneratePasswordAsync(passwordOptions);
                var codeVerifierHash = await _cryptoFunctionService.HashAsync(codeVerifier, CryptoHashAlgorithm.Sha256);
                var codeChallenge = CoreHelpers.Base64UrlEncode(codeVerifierHash);

                var state = await _passwordGenerationService.GeneratePasswordAsync(passwordOptions);

                var url = _apiService.IdentityBaseUrl + "/connect/authorize?" +
                          "client_id=" + _platformUtilsService.GetClientType().GetString() + "&" +
                          "redirect_uri=" + Uri.EscapeDataString(REDIRECT_URI) + "&" +
                          "response_type=code&scope=api%20offline_access&" +
                          "state=" + state + "&code_challenge=" + codeChallenge + "&" +
                          "code_challenge_method=S256&response_mode=query&" +
                          "domain_hint=" + Uri.EscapeDataString(OrgIdentifier) + "&" +
                          "ssoToken=" + Uri.EscapeDataString(ssoToken);

                WebAuthenticatorResult authResult = null;

                authResult = await WebAuthenticator.AuthenticateAsync(new Uri(url),
                    new Uri(REDIRECT_URI));


                var code = GetResultCode(authResult, state);
                if (!string.IsNullOrEmpty(code))
                {
                    await LogIn(code, codeVerifier, OrgIdentifier);
                }
                else
                {
                    await _deviceActionService.HideLoadingAsync();
                    await _platformUtilsService.ShowDialogAsync(AppResources.LoginSsoError,
                        AppResources.AnErrorHasOccurred);
                }
            }
            catch (ApiException e)
            {
                _logger.Exception(e);
                await _deviceActionService.HideLoadingAsync();
                await _platformUtilsService.ShowDialogAsync(e?.Error?.GetSingleMessage() ?? AppResources.LoginSsoError,
                    AppResources.AnErrorHasOccurred);
            }
            catch (TaskCanceledException)
            {
                // user canceled
                await _deviceActionService.HideLoadingAsync();
            }
            catch (Exception ex)
            {
                _logger.Exception(ex);
                await _deviceActionService.HideLoadingAsync();
                await _platformUtilsService.ShowDialogAsync(AppResources.GenericErrorMessage, AppResources.AnErrorHasOccurred);
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

        private async Task LogIn(string code, string codeVerifier, string orgId)
        {
            try
            {
                var response = await _authService.LogInSsoAsync(code, codeVerifier, REDIRECT_URI, orgId);
                await AppHelpers.ResetInvalidUnlockAttemptsAsync();
                await _stateService.SetRememberedOrgIdentifierAsync(OrgIdentifier);
                await _deviceActionService.HideLoadingAsync();
                if (response.TwoFactor)
                {
                    StartTwoFactorAction?.Invoke();
                }
                else if (response.ResetMasterPassword)
                {
                    StartSetPasswordAction?.Invoke();
                }
                else if (response.ForcePasswordReset)
                {
                    UpdateTempPasswordAction?.Invoke();
                }
                else
                {
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

        private async Task<bool> TryClaimedDomainLogin()
        {
            var userEmail = await _stateService.GetRememberedEmailAsync();
            var claimedDomainOrg = await _organizationService.GetClaimedOrganizationDomainAsync(userEmail);
            if (string.IsNullOrEmpty(claimedDomainOrg))
            {
                return false;
            }

            OrgIdentifier = claimedDomainOrg;
            await LogInAsync();
            return true;
        }
    }
}
