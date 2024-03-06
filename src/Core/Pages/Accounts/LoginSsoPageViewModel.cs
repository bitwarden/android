using System;
using System.Net;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.Core.Resources.Localization;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.Domain;
using Bit.Core.Services;
using Bit.Core.Utilities;

using Microsoft.Maui.Authentication;
using Microsoft.Maui.Networking;
using NetworkAccess = Microsoft.Maui.Networking.NetworkAccess;
using Org.BouncyCastle.Asn1.Ocsp;

#if IOS
using AuthenticationServices;
using Foundation;
using UIKit;
using WebAuthenticator = Bit.Core.Utilities.MAUI.WebAuthenticator;
using WebAuthenticatorResult = Bit.Core.Utilities.MAUI.WebAuthenticatorResult;
using WebAuthenticatorOptions = Bit.Core.Utilities.MAUI.WebAuthenticatorOptions;
#endif

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
        private readonly IDeviceTrustCryptoService _deviceTrustCryptoService;
        private readonly ICryptoService _cryptoService;

        private string _orgIdentifier;
        private bool _useEphemeralWebBrowserSession;

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
            _deviceTrustCryptoService = ServiceContainer.Resolve<IDeviceTrustCryptoService>();
            _cryptoService = ServiceContainer.Resolve<ICryptoService>();

            PageTitle = AppResources.Bitwarden;
            LogInCommand = CreateDefaultAsyncRelayCommand(LogInAsync, allowsMultipleExecutions: false);
        }

        public string OrgIdentifier
        {
            get => _orgIdentifier;
            set => SetProperty(ref _orgIdentifier, value);
        }

        public bool FromIosExtension { get; set; }

        public ICommand LogInCommand { get; }
        public Action StartTwoFactorAction { get; set; }
        public Action StartSetPasswordAction { get; set; }
        public Action SsoAuthSuccessAction { get; set; }
        public Action StartDeviceApprovalOptionsAction { get; set; }
        public Action CloseAction { get; set; }
        public Action UpdateTempPasswordAction { get; set; }

        public async Task InitAsync()
        {
            try
            {
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

                var response = await _apiService.PreValidateSsoAsync(OrgIdentifier);

                if (string.IsNullOrWhiteSpace(response?.Token))
                {
                    _logger.Error(response is null ? "Login SSO Error: response is null" : "Login SSO Error: response.Token is null or whitespace");
                    await _deviceActionService.HideLoadingAsync();
                    await _platformUtilsService.ShowDialogAsync(AppResources.LoginSsoError);
                    return;
                }

                var ssoToken = response.Token;

                var passwordOptions = PasswordGenerationOptions.CreateDefault
                                                               .WithLength(64);

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
                authResult = await WebAuthenticator.AuthenticateAsync(new WebAuthenticatorOptions()
                {
                    CallbackUrl = new Uri(REDIRECT_URI),
                    Url = new Uri(url),
                    PrefersEphemeralWebBrowserSession = _useEphemeralWebBrowserSession,
#if IOS
                    ShouldUseSharedApplicationKeyWindow = FromIosExtension
#endif
                });

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
                // Workaroung for cached expired sso token PM-3551 
                _useEphemeralWebBrowserSession = true;
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
                var decryptOptions = await _stateService.GetAccountDecryptionOptions();
                await AppHelpers.ResetInvalidUnlockAttemptsAsync();
                await _stateService.SetRememberedOrgIdentifierAsync(OrgIdentifier);
                await _deviceActionService.HideLoadingAsync();
                if (response.TwoFactor)
                {
                    StartTwoFactorAction?.Invoke();
                    return;
                }

                // Trusted device option is sent regardless if this is a trusted device or not
                // If it is trusted, it will have the necessary keys
                if (decryptOptions?.TrustedDeviceOption != null)
                {
                    if (await _deviceTrustCryptoService.IsDeviceTrustedAsync())
                    {
                        // If we have a device key but no keys on server, we need to remove the device key
                        if (decryptOptions.TrustedDeviceOption.EncryptedPrivateKey == null && decryptOptions.TrustedDeviceOption.EncryptedUserKey == null)
                        {
                            await _deviceTrustCryptoService.RemoveTrustedDeviceAsync();
                            StartDeviceApprovalOptionsAction?.Invoke();
                            return;
                        }
                        // Update temp password only if the device is trusted and therefore has a decrypted User Key set
                        if (response.ForcePasswordReset)
                        {
                            UpdateTempPasswordAction?.Invoke();
                            return;
                        }
                        // If user doesn't have a MP, but has reset password permission, they must set a MP
                        if (!decryptOptions.HasMasterPassword &&
                            decryptOptions.TrustedDeviceOption.HasManageResetPasswordPermission)
                        {
                            await _stateService.SetForcePasswordResetReasonAsync(ForcePasswordResetReason.TdeUserWithoutPasswordHasPasswordResetPermission);
                        }
                        // Device is trusted and has keys, so we can decrypt
                        _syncService.FullSyncAsync(true).FireAndForget();
                        SsoAuthSuccessAction?.Invoke();
                        return;
                    }

                    // Check for pending Admin Auth requests before navigating to device approval options
                    var pendingRequest = await _stateService.GetPendingAdminAuthRequestAsync();
                    if (pendingRequest != null)
                    {
                        var authRequest = await _authService.GetPasswordlessLoginRequestByIdAsync(pendingRequest.Id);
                        if (authRequest?.RequestApproved == true)
                        {
                            var authResult = await _authService.LogInPasswordlessAsync(true, await _stateService.GetActiveUserEmailAsync(), authRequest.RequestAccessCode, pendingRequest.Id, pendingRequest.PrivateKey, authRequest.Key, authRequest.MasterPasswordHash);
                            if (authResult == null && await _stateService.IsAuthenticatedAsync())
                            {
                                await Microsoft.Maui.ApplicationModel.MainThread.InvokeOnMainThreadAsync(
                                 () => _platformUtilsService.ShowToast("info", null, AppResources.LoginApproved));
                                await _stateService.SetPendingAdminAuthRequestAsync(null);
                                _syncService.FullSyncAsync(true).FireAndForget();
                                SsoAuthSuccessAction?.Invoke();
                            }
                        }
                        else
                        {
                            await _stateService.SetPendingAdminAuthRequestAsync(null);
                            StartDeviceApprovalOptionsAction?.Invoke();
                        }
                    }
                    else
                    {
                        StartDeviceApprovalOptionsAction?.Invoke();
                    }
                    return;
                }

                // In the standard, non TDE case, a user must set password if they don't
                // have one and they aren't using key connector.
                // Note: TDE & Key connector are mutually exclusive org config options.
                if (response.ResetMasterPassword || (decryptOptions?.RequireSetPassword == true))
                {
                    // TODO: We need to look into how to handle this when Org removes TDE
                    // Will we have the User Key by now to set a new password?
                    StartSetPasswordAction?.Invoke();
                    return;
                }

                _syncService.FullSyncAsync(true).FireAndForget();
                SsoAuthSuccessAction?.Invoke();
            }
            catch (Exception)
            {
                await _deviceActionService.HideLoadingAsync();
                await _platformUtilsService.ShowDialogAsync(AppResources.LoginSsoError,
                    AppResources.AnErrorHasOccurred);
            }
        }

        private async Task<bool> TryClaimedDomainLogin()
        {
            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.Loading);
                var userEmail = await _stateService.GetPreLoginEmailAsync();
                var claimedDomainOrgDetails = await _organizationService.GetClaimedOrganizationDomainAsync(userEmail);
                await _deviceActionService.HideLoadingAsync();

                if (claimedDomainOrgDetails == null || !claimedDomainOrgDetails.SsoAvailable)
                {
                    return false;
                }

                if (string.IsNullOrEmpty(claimedDomainOrgDetails.OrganizationIdentifier))
                {
                    await _platformUtilsService.ShowDialogAsync(AppResources.OrganizationSsoIdentifierRequired, AppResources.AnErrorHasOccurred);
                    return false;
                }

                OrgIdentifier = claimedDomainOrgDetails.OrganizationIdentifier;
                await LogInAsync();
                return true;
            }
            catch (Exception ex)
            {
                HandleException(ex);
            }

            return false;
        }
    }
}
