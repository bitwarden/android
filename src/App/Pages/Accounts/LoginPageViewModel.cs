using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Models;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.App.Utilities.AccountManagement;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Models.View;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class LoginPageViewModel : CaptchaProtectedViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly IAuthService _authService;
        private readonly ISyncService _syncService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IStateService _stateService;
        private readonly IEnvironmentService _environmentService;
        private readonly II18nService _i18nService;
        private readonly IMessagingService _messagingService;
        private readonly ILogger _logger;
        private readonly IApiService _apiService;
        private readonly IAppIdService _appIdService;
        private readonly IAccountsManager _accountManager;
        private bool _showPassword;
        private bool _showCancelButton;
        private string _email;
        private string _masterPassword;
        private bool _isEmailEnabled;
        private bool _isKnownDevice;
        private bool _isExecutingLogin;
        private string _environmentHostName;

        public LoginPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _authService = ServiceContainer.Resolve<IAuthService>("authService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _environmentService = ServiceContainer.Resolve<IEnvironmentService>("environmentService");
            _i18nService = ServiceContainer.Resolve<II18nService>("i18nService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _logger = ServiceContainer.Resolve<ILogger>("logger");
            _apiService = ServiceContainer.Resolve<IApiService>();
            _appIdService = ServiceContainer.Resolve<IAppIdService>();
            _accountManager = ServiceContainer.Resolve<IAccountsManager>();

            PageTitle = AppResources.Bitwarden;
            TogglePasswordCommand = new Command(TogglePassword);
            LogInCommand = new Command(async () => await LogInAsync());
            MoreCommand = new AsyncCommand(MoreAsync, onException: _logger.Exception, allowsMultipleExecutions: false);
            LogInWithDeviceCommand = new AsyncCommand(() => Device.InvokeOnMainThreadAsync(LogInWithDeviceAction), onException: _logger.Exception, allowsMultipleExecutions: false);

            AccountSwitchingOverlayViewModel = new AccountSwitchingOverlayViewModel(_stateService, _messagingService, _logger)
            {
                AllowAddAccountRow = true,
                AllowActiveAccountSelection = true
            };
        }

        public bool ShowPassword
        {
            get => _showPassword;
            set => SetProperty(ref _showPassword, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ShowPasswordIcon),
                    nameof(PasswordVisibilityAccessibilityText)
                });
        }

        public bool ShowCancelButton
        {
            get => _showCancelButton;
            set => SetProperty(ref _showCancelButton, value);
        }

        public string Email
        {
            get => _email;
            set => SetProperty(ref _email, value,
                additionalPropertyNames: new string[]
                {
                    nameof(LoggingInAsText)
                });
        }

        public string MasterPassword
        {
            get => _masterPassword;
            set => SetProperty(ref _masterPassword, value);
        }

        public bool IsEmailEnabled
        {
            get => _isEmailEnabled;
            set => SetProperty(ref _isEmailEnabled, value);
        }

        public bool IsKnownDevice
        {
            get => _isKnownDevice;
            set => SetProperty(ref _isKnownDevice, value);
        }

        public string EnvironmentDomainName
        {
            get => _environmentHostName;
            set => SetProperty(ref _environmentHostName, value,
                additionalPropertyNames: new string[]
                {
                    nameof(LoggingInAsText)
                });
        }

        public AccountSwitchingOverlayViewModel AccountSwitchingOverlayViewModel { get; }
        public Command LogInCommand { get; }
        public Command TogglePasswordCommand { get; }
        public ICommand MoreCommand { get; internal set; }
        public ICommand LogInWithDeviceCommand { get; }
        public string ShowPasswordIcon => ShowPassword ? BitwardenIcons.EyeSlash : BitwardenIcons.Eye;
        public string PasswordVisibilityAccessibilityText => ShowPassword ? AppResources.PasswordIsVisibleTapToHide : AppResources.PasswordIsNotVisibleTapToShow;
        public string LoggingInAsText => string.Format(AppResources.LoggingInAsXOnY, Email, EnvironmentDomainName);
        public bool IsIosExtension { get; set; }
        public bool CanRemoveAccount { get; set; }
        public Action StartTwoFactorAction { get; set; }
        public Action LogInSuccessAction { get; set; }
        public Action LogInWithDeviceAction { get; set; }
        public Action UpdateTempPasswordAction { get; set; }
        public Action StartSsoLoginAction { get; set; }
        public Action CloseAction { get; set; }

        public bool EmailIsInSavedAccounts => _stateService.AccountViews != null && _stateService.AccountViews.Any(e => e.Email == Email);

        protected override II18nService i18nService => _i18nService;
        protected override IEnvironmentService environmentService => _environmentService;
        protected override IDeviceActionService deviceActionService => _deviceActionService;
        protected override IPlatformUtilsService platformUtilsService => _platformUtilsService;

        public async Task InitAsync()
        {
            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.Loading);
                await _stateService.SetPreLoginEmailAsync(Email);
                await AccountSwitchingOverlayViewModel.RefreshAccountViewsAsync();
                if (string.IsNullOrWhiteSpace(Email))
                {
                    Email = await _stateService.GetRememberedEmailAsync();
                }
                CanRemoveAccount = await _stateService.GetActiveUserEmailAsync() != Email;
                EnvironmentDomainName = CoreHelpers.GetDomain((await _stateService.GetPreAuthEnvironmentUrlsAsync())?.Base);
                IsKnownDevice = await _apiService.GetKnownDeviceAsync(Email, await _appIdService.GetAppIdAsync());
            }
            catch (ApiException apiEx) when (apiEx.Error.StatusCode == System.Net.HttpStatusCode.Unauthorized)
            {
                _logger.Exception(apiEx);
            }
            catch (Exception ex)
            {
                HandleException(ex);
            }
            finally
            {
                await _deviceActionService.HideLoadingAsync();
            }
        }

        public async Task LogInAsync(bool showLoading = true, bool checkForExistingAccount = false)
        {
            if (Xamarin.Essentials.Connectivity.NetworkAccess == Xamarin.Essentials.NetworkAccess.None)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.InternetConnectionRequiredMessage,
                    AppResources.InternetConnectionRequiredTitle, AppResources.Ok);
                return;
            }
            if (string.IsNullOrWhiteSpace(Email))
            {
                await _platformUtilsService.ShowDialogAsync(
                    string.Format(AppResources.ValidationFieldRequired, AppResources.EmailAddress),
                    AppResources.AnErrorHasOccurred, AppResources.Ok);
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
                    AppResources.AnErrorHasOccurred, AppResources.Ok);
                return;
            }

            ShowPassword = false;
            try
            {
                _isExecutingLogin = true;
                if (checkForExistingAccount)
                {
                    var userId = await _stateService.GetUserIdAsync(Email);
                    if (!string.IsNullOrWhiteSpace(userId))
                    {
                        var userEnvUrls = await _stateService.GetEnvironmentUrlsAsync(userId);
                        if (userEnvUrls?.Base == _environmentService.BaseUrl)
                        {
                            await _accountManager.PromptToSwitchToExistingAccountAsync(userId);
                            return;
                        }
                    }
                }

                if (showLoading)
                {
                    await _deviceActionService.ShowLoadingAsync(AppResources.LoggingIn);
                }

                var response = await _authService.LogInAsync(Email, MasterPassword, _captchaToken);
                await AppHelpers.ResetInvalidUnlockAttemptsAsync();

                if (response.CaptchaNeeded)
                {
                    if (await HandleCaptchaAsync(response.CaptchaSiteKey))
                    {
                        await LogInAsync(false);
                        _captchaToken = null;
                    }
                    return;
                }
                MasterPassword = string.Empty;
                _captchaToken = null;

                await _deviceActionService.HideLoadingAsync();

                if (response.RequiresEncryptionKeyMigration)
                {
                    // Legacy users must migrate on web vault.
                    await _platformUtilsService.ShowDialogAsync(AppResources.EncryptionKeyMigrationRequiredDescriptionLong, AppResources.AnErrorHasOccurred,
                        AppResources.Ok);
                    return;
                }

                if (response.TwoFactor)
                {
                    StartTwoFactorAction?.Invoke();
                }
                else if (response.ForcePasswordReset)
                {
                    UpdateTempPasswordAction?.Invoke();
                }
                else
                {
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
                        AppResources.AnErrorHasOccurred, AppResources.Ok);
                }
            }
            finally
            {
                _isExecutingLogin = false;
            }
        }

        public void ResetPasswordField()
        {
            try
            {
                if (!_isExecutingLogin)
                {
                    MasterPassword = string.Empty;
                    ShowPassword = false;
                }
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }

        private async Task MoreAsync()
        {
            var buttons = IsEmailEnabled || CanRemoveAccount
                ? new[] { AppResources.GetPasswordHint }
                : new[] { AppResources.GetPasswordHint, AppResources.RemoveAccount };
            var selection = await _deviceActionService.DisplayActionSheetAsync(AppResources.Options, AppResources.Cancel, null, buttons);

            if (selection == AppResources.GetPasswordHint)
            {
                await ShowMasterPasswordHintAsync();
            }
            else if (selection == AppResources.RemoveAccount)
            {
                await RemoveAccountAsync();
            }
        }

        public async Task ShowMasterPasswordHintAsync()
        {
            var hintNavigationPage = new NavigationPage(new HintPage(Email));
            if (IsIosExtension)
            {
                ThemeManager.ApplyResourcesTo(hintNavigationPage);
            }
            await Page.Navigation.PushModalAsync(hintNavigationPage);
        }

        public void TogglePassword()
        {
            ShowPassword = !ShowPassword;
            var entry = (Page as LoginPage).MasterPasswordEntry;
            entry.Focus();
            entry.CursorPosition = String.IsNullOrEmpty(MasterPassword) ? 0 : MasterPassword.Length;
        }

        public async Task RemoveAccountAsync()
        {
            try
            {
                var confirmed = await _platformUtilsService.ShowDialogAsync(AppResources.RemoveAccountConfirmation,
                    AppResources.RemoveAccount, AppResources.Yes, AppResources.Cancel);
                if (confirmed)
                {
                    _messagingService.Send("logout");
                }
            }
            catch (Exception e)
            {
                _logger.Exception(e);
            }
        }
    }
}
