using System;
using System.Threading.Tasks;
using Bit.App.Models;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class HomePage : BaseContentPage
    {
        private bool _checkRememberedEmail;
        private readonly HomeViewModel _vm;
        private readonly AppOptions _appOptions;
        private IBroadcasterService _broadcasterService;

        public HomePage(AppOptions appOptions = null, bool shouldCheckRememberEmail = true)
        {
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _appOptions = appOptions;
            InitializeComponent();
            _vm = BindingContext as HomeViewModel;
            _vm.Page = this;
            _vm.ShouldCheckRememberEmail = shouldCheckRememberEmail;
            _vm.ShowCancelButton = _appOptions?.IosExtension ?? false;
            _vm.StartLoginAction = async () => await StartLoginAsync();
            _vm.StartRegisterAction = () => Device.BeginInvokeOnMainThread(async () => await StartRegisterAsync());
            _vm.StartSsoLoginAction = () => Device.BeginInvokeOnMainThread(async () => await StartSsoLoginAsync());
            _vm.StartEnvironmentAction = () => Device.BeginInvokeOnMainThread(async () => await StartEnvironmentAsync());
            _vm.CloseAction = async () =>
            {
                await _accountListOverlay.HideAsync();
                await Navigation.PopModalAsync();
            };
            UpdateLogo();

            if (!_vm.ShowCancelButton)
            {
                ToolbarItems.Remove(_closeButton);
            }
            if (_appOptions?.HideAccountSwitcher ?? false)
            {
                ToolbarItems.Remove(_accountAvatar);
            }
        }

        public async Task DismissRegisterPageAndLogInAsync(string email)
        {
            await Navigation.PopModalAsync();
            await Navigation.PushModalAsync(new NavigationPage(new LoginPage(email, _appOptions)));
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            _mainContent.Content = _mainLayout;
            _accountAvatar?.OnAppearing();

            if (!_appOptions?.HideAccountSwitcher ?? false)
            {
                _vm.AvatarImageSource = await GetAvatarImageSourceAsync(false);
            }
            _broadcasterService.Subscribe(nameof(HomePage), (message) =>
            {
                if (message.Command == "updatedTheme")
                {
                    Device.BeginInvokeOnMainThread(() =>
                    {
                        UpdateLogo();
                    });
                }
            });

            _vm.CheckNavigateLoginStep();
        }

        protected override bool OnBackButtonPressed()
        {
            if (_accountListOverlay.IsVisible)
            {
                _accountListOverlay.HideAsync().FireAndForget();
                return true;
            }
            return false;
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            _broadcasterService.Unsubscribe(nameof(HomePage));
            _accountAvatar?.OnDisappearing();
        }

        private void UpdateLogo()
        {
            _logo.Source = !ThemeManager.UsingLightTheme ? "logo_white.png" : "logo.png";
        }

        private void Cancel_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.CloseAction();
            }
        }

        private async Task StartLoginAsync()
        {
            var page = new LoginPage(_vm.Email, _appOptions);
            await Navigation.PushModalAsync(new NavigationPage(page));
        }

        private async Task StartRegisterAsync()
        {
            var page = new RegisterPage(this);
            await Navigation.PushModalAsync(new NavigationPage(page));
        }

        private void LogInSso_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.StartSsoLoginAction();
            }
        }

        private async Task StartSsoLoginAsync()
        {
            var page = new LoginSsoPage(_appOptions);
            await Navigation.PushModalAsync(new NavigationPage(page));
        }

        private void Environment_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.StartEnvironmentAction();
            }
        }

        private async Task StartEnvironmentAsync()
        {
            await _accountListOverlay.HideAsync();
            var page = new EnvironmentPage();
            await Navigation.PushModalAsync(new NavigationPage(page));
        }
    }
}
