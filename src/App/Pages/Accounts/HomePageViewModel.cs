using System;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Resources;
using Bit.App.Styles;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Models.Data;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Forms;
using BwRegion = Bit.Core.Enums.Region;

namespace Bit.App.Pages
{
    public class HomeViewModel : BaseViewModel
    {

        private readonly IStateService _stateService;
        private readonly IMessagingService _messagingService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly ILogger _logger;
        private readonly IEnvironmentService _environmentService;
        private readonly IAccountsManager _accountManager;
        private readonly IConfigService _configService;

        private bool _showCancelButton;
        private bool _rememberEmail;
        private string _email;
        private string _selectedEnvironmentName;
        private bool _displayEuEnvironment;

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

        public string RegionText => $"{AppResources.LoggingInOn}:";
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
            _displayEuEnvironment = await _configService.GetFeatureFlagBoolAsync(Constants.DisplayEuEnvironmentFlag, forceRefresh: true);
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
            _displayEuEnvironment = await _configService.GetFeatureFlagBoolAsync(Constants.DisplayEuEnvironmentFlag);
            var options = _displayEuEnvironment
                    ? new string[] { BwRegion.US.Domain(), BwRegion.EU.Domain(), AppResources.SelfHosted }
                    : new string[] { BwRegion.US.Domain(), AppResources.SelfHosted };

            await Device.InvokeOnMainThreadAsync(async () =>
            {
                var result = await Page.DisplayActionSheet(AppResources.LoggingInOn, AppResources.Cancel, null, options);

                if (result is null || result == AppResources.Cancel)
                {
                    return;
                }

                if (result == AppResources.SelfHosted)
                {
                    StartEnvironmentAction?.Invoke();
                    return;
                }

                await _environmentService.SetRegionAsync(result == BwRegion.EU.Domain() ? BwRegion.EU : BwRegion.US);
                await _configService.GetAsync(true);
                SelectedEnvironmentName = result;
            });
        }

        public async Task UpdateEnvironmentAsync()
        {
            var region = _environmentService.SelectedRegion;
            if (region == BwRegion.SelfHosted)
            {
                SelectedEnvironmentName = AppResources.SelfHosted;
                await _configService.GetAsync(true);
            }
            else
            {
                SelectedEnvironmentName = region.Domain();
            }
        }
    }
}
