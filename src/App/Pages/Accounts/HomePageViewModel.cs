using System;
using System.Threading.Tasks;
using Bit.App.Controls;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.Essentials;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class HomeViewModel : BaseViewModel
    {
        private readonly IStateService _stateService;
        private readonly IMessagingService _messagingService;

        private bool _showCancelButton;
        private bool _showEmail;
        private bool _rememberEmail;
        private string _email;
        private bool _isEmailEnabled;
        private bool _canLogin;

        public HomeViewModel()
        {
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            var logger = ServiceContainer.Resolve<ILogger>("logger");

            PageTitle = AppResources.Bitwarden;

            AccountSwitchingOverlayViewModel = new AccountSwitchingOverlayViewModel(_stateService, _messagingService, logger)
            {
                AllowActiveAccountSelection = true
            };
        }

        public bool ShowCancelButton
        {
            get => _showCancelButton;
            set => SetProperty(ref _showCancelButton, value);
        }

        public bool ShowEmail
        {
            get => _showEmail;
            set => SetProperty(ref _showEmail, value);
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

        public async Task InitAsync()
        {
            Email = await _stateService.GetRememberedEmailAsync();
            RememberEmail = !string.IsNullOrEmpty(Email);
            ShowEmail = RememberEmail;
        }

        public async Task SetRememberEmailAsync()
        {
            await _stateService.SetRememberedEmailAsync(RememberEmail ? Email : null);
        }
    }
}
