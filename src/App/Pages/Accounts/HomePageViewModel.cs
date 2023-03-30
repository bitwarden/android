using System;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Models.Data;
using Bit.Core.Models.Response;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Essentials;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class HomeViewModel : BaseViewModel
    {
        private readonly IStateService _stateService;
        private readonly IMessagingService _messagingService;

        private bool _showCancelButton;
        private bool _rememberEmail;
        private string _email;
        private string _selectedEnvironmentName;
        private bool _isEmailEnabled;
        private bool _canLogin;
        private bool _displayEuEnvironment;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly ILogger _logger;
        private readonly IEnvironmentService _environmentService;
        private readonly IAccountsManager _accountManager;
        private readonly IConfigService _configService;

        public HomeViewModel()
        {
            _stateService = ServiceContainer.Resolve<IStateService>();
            _messagingService = ServiceContainer.Resolve<IMessagingService>();
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>();
            _logger = ServiceContainer.Resolve<ILogger>();
            _environmentService = ServiceContainer.Resolve<IEnvironmentService>();
            _accountManager = ServiceContainer.Resolve<IAccountsManager>();
            _configService = ServiceContainer.Resolve<IConfigService>();

            PageTitle = AppResources.Bitwarden;

            AccountSwitchingOverlayViewModel = new AccountSwitchingOverlayViewModel(_stateService, _messagingService, _logger)
            {
                AllowActiveAccountSelection = true
            };
            RememberEmailCommand = new Command(() => RememberEmail = !RememberEmail);
            ContinueCommand = new AsyncCommand(ContinueToLoginStepAsync, allowsMultipleExecutions: false);
            CreateAccountCommand = new AsyncCommand(async () => await Device.InvokeOnMainThreadAsync(StartRegisterAction),
                onException: _logger.Exception, allowsMultipleExecutions: false);
            CloseCommand = new AsyncCommand(async () => await Device.InvokeOnMainThreadAsync(CloseAction),
                onException: _logger.Exception, allowsMultipleExecutions: false);
            ShowEnvironmentPickerCommand = new AsyncCommand(ShowEnvironmentPickerAsync,
                onException: _logger.Exception, allowsMultipleExecutions: false);
            InitAsync().FireAndForget();
        }

        public bool ShowCancelButton
        {
            get => _showCancelButton;
            set => SetProperty(ref _showCancelButton, value);
        }

        public bool RememberEmail
        {
            get => _rememberEmail;
            set => SetProperty(ref _rememberEmail, value);
        }

        public string Email
        {
            get => _email;
            set => SetProperty(ref _email, value,
                additionalPropertyNames: new[] { nameof(CanContinue) });
        }

        public string SelectedEnvironmentName
        {
            get => $"{_selectedEnvironmentName} {BitwardenIcons.AngleDown}";
            set => SetProperty(ref _selectedEnvironmentName, value);
        }

        public string RegionText => $"{AppResources.Region}:";
        public bool CanContinue => !string.IsNullOrEmpty(Email);

        public FormattedString CreateAccountText
        {
            get
            {
                var fs = new FormattedString();
                fs.Spans.Add(new Span
                {
                    Text = $"{AppResources.NewAroundHere} "
                });
                fs.Spans.Add(new Span
                {
                    Text = AppResources.CreateAccount,
                    TextColor = ThemeManager.GetResourceColor("PrimaryColor")
                });
                return fs;
            }
        }

        public AccountSwitchingOverlayViewModel AccountSwitchingOverlayViewModel { get; }
        public Action StartLoginAction { get; set; }
        public Action StartRegisterAction { get; set; }
        public Action StartSsoLoginAction { get; set; }
        public Action StartEnvironmentAction { get; set; }
        public Action CloseAction { get; set; }
        public Command RememberEmailCommand { get; set; }
        public AsyncCommand ContinueCommand { get; }
        public AsyncCommand CloseCommand { get; }
        public AsyncCommand CreateAccountCommand { get; }
        public AsyncCommand ShowEnvironmentPickerCommand { get; }

        public async Task InitAsync()
        {
            Email = await _stateService.GetRememberedEmailAsync();
            RememberEmail = !string.IsNullOrEmpty(Email);
            _displayEuEnvironment = await _configService.GetFeatureFlagAsync(ConfigResponse.DisplayEuEnvironmentFlag, forceRefresh: true);
        }

        public async Task ContinueToLoginStepAsync()
        {
            try
            {
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

                await _stateService.SetRememberedEmailAsync(RememberEmail ? Email : null);
                var userId = await _stateService.GetUserIdAsync(Email);

                if (!string.IsNullOrWhiteSpace(userId) &&
                    (await _stateService.GetEnvironmentUrlsAsync(userId))?.Base == _environmentService.BaseUrl &&
                    await _stateService.IsAuthenticatedAsync(userId))
                {
                    await _accountManager.PromptToSwitchToExistingAccountAsync(userId);
                    return;
                }
                StartLoginAction();
            }
            catch (Exception ex)
            {
                _logger.Exception(ex);
                await _platformUtilsService.ShowDialogAsync(AppResources.GenericErrorMessage, AppResources.AnErrorHasOccurred, AppResources.Ok);
            }
        }

        public async Task ShowEnvironmentPickerAsync()
        {
            var options = _displayEuEnvironment
                    ? new string[] { AppResources.US, AppResources.EU, AppResources.SelfHosted }
                    : new string[] { AppResources.US, AppResources.SelfHosted };

            await Device.InvokeOnMainThreadAsync(async () =>
            {
                var result = await Page.DisplayActionSheet(AppResources.DataRegion, AppResources.Cancel, null, options);

                if (result is null || result == AppResources.Cancel)
                {
                    return;
                }

                if (result == AppResources.SelfHosted)
                {
                    StartEnvironmentAction?.Invoke();
                }
                else
                {
                    await _environmentService.SetUrlsAsync(result == AppResources.EU ? EnvironmentUrlData.DefaultEU : EnvironmentUrlData.DefaultUS);
                    SelectedEnvironmentName = result;
                }
            });
        }

        public async Task UpdateEnvironment()
        {
            var environmentsSaved = await _stateService.GetPreAuthEnvironmentUrlsAsync();
            if (environmentsSaved == null || environmentsSaved.Equals(new EnvironmentUrlData()))
            {
                await _environmentService.SetUrlsAsync(EnvironmentUrlData.DefaultUS);
                environmentsSaved = EnvironmentUrlData.DefaultUS;
                return;
            }

            if (environmentsSaved.Base == EnvironmentUrlData.DefaultUS.Base)
            {
                SelectedEnvironmentName = AppResources.US;
            }
            else if (environmentsSaved.Base == EnvironmentUrlData.DefaultEU.Base)
            {
                SelectedEnvironmentName = AppResources.EU;
            }
            else
            {
                SelectedEnvironmentName = AppResources.SelfHosted;
            }
        }
    }
}
