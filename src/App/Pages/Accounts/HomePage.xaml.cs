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
        private readonly HomeViewModel _vm;
        private readonly AppOptions _appOptions;
        private IBroadcasterService _broadcasterService;

        public HomePage(AppOptions appOptions = null)
        {
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _appOptions = appOptions;
            InitializeComponent();
            _vm = BindingContext as HomeViewModel;
            _vm.Page = this;
            _vm.StartLoginAction = () => Device.BeginInvokeOnMainThread(async () => await StartLoginAsync());
            _vm.StartRegisterAction = () => Device.BeginInvokeOnMainThread(async () => await StartRegisterAsync());
            _vm.StartSsoLoginAction = () => Device.BeginInvokeOnMainThread(async () => await StartSsoLoginAsync());
            _vm.StartEnvironmentAction = () => Device.BeginInvokeOnMainThread(async () => await StartEnvironmentAsync());
            UpdateLogo();

            if (!_appOptions?.IosExtension ?? false)
            {
                ToolbarItems.Remove(_closeItem);
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

            if (await ShowAccountSwitcherAsync())
            {
                _vm.AvatarImageSource = await GetAvatarImageSourceAsync();
            }
            else
            {
                ToolbarItems.Remove(_accountAvatar);
            }
            _broadcasterService.Subscribe(nameof(HomePage), async (message) =>
            {
                if (message.Command == "updatedTheme")
                {
                    Device.BeginInvokeOnMainThread(() =>
                    {
                        UpdateLogo();
                    });
                }
            });
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
        
        private void Close_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.CloseAction();
            }
        }

        private void LogIn_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.StartLoginAction();
            }
        }

        private async Task StartLoginAsync()
        {
            var page = new LoginPage(null, _appOptions);
            await Navigation.PushModalAsync(new NavigationPage(page));
        }

        private void Register_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.StartRegisterAction();
            }
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
