using System;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
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
        private bool _isEmailEnabled;
        private bool _canLogin;
        private IPlatformUtilsService _platformUtilsService;
        private ILogger _logger;
        private IEnvironmentService _environmentService;
        private IAccountsManager _accountManager;

        public HomeViewModel()
        {
            _stateService = ServiceContainer.Resolve<IStateService>();
            _messagingService = ServiceContainer.Resolve<IMessagingService>();
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>();
            _logger = ServiceContainer.Resolve<ILogger>();
            _environmentService = ServiceContainer.Resolve<IEnvironmentService>();
            _accountManager = ServiceContainer.Resolve<IAccountsManager>();

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

        public bool CanContinue => !string.IsNullOrEmpty(Email);

        public bool ShouldCheckRememberEmail { get; set; }

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

        public async Task InitAsync()
        {
            Email = await _stateService.GetRememberedEmailAsync();
            RememberEmail = !string.IsNullOrEmpty(Email);
        }

        public void CheckNavigateLoginStep()
        {
            if (ShouldCheckRememberEmail && RememberEmail)
            {
                StartLoginAction();
            }
            ShouldCheckRememberEmail = false;
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
                if (!string.IsNullOrWhiteSpace(userId))
                {
                    var userEnvUrls = await _stateService.GetEnvironmentUrlsAsync(userId);
                    if (userEnvUrls?.Base == _environmentService.BaseUrl)
                    {
                        await _accountManager.PromptToSwitchToExistingAccountAsync(userId);
                        return;
                    }
                }
                StartLoginAction();
            }
            catch (Exception ex)
            {
                _logger.Exception(ex);
                await _platformUtilsService.ShowDialogAsync(AppResources.GenericErrorMessage, AppResources.AnErrorHasOccurred, AppResources.Ok);
            }
        }
    }
}
