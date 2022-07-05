using System;
using System.Threading.Tasks;
using Bit.App.Models;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class LoginPage : BaseContentPage
    {
        private readonly LoginPageViewModel _vm;
        private readonly AppOptions _appOptions;

        private bool _inputFocused;

        public LoginPage(string email = null, AppOptions appOptions = null)
        {
            _appOptions = appOptions;
            InitializeComponent();
            _vm = BindingContext as LoginPageViewModel;
            _vm.Page = this;
            _vm.StartTwoFactorAction = () => Device.BeginInvokeOnMainThread(async () => await StartTwoFactorAsync());
            _vm.LogInSuccessAction = () => Device.BeginInvokeOnMainThread(async () => await LogInSuccessAsync());
            _vm.UpdateTempPasswordAction =
                () => Device.BeginInvokeOnMainThread(async () => await UpdateTempPasswordAsync());
            _vm.CloseAction = async () =>
            {
                await _accountListOverlay.HideAsync();
                await Navigation.PopModalAsync();
            };
            if (!string.IsNullOrWhiteSpace(email))
            {
                _email.IsEnabled = false;
            }
            else
            {
                _vm.ShowCancelButton = true;
            }
            _vm.Email = email;
            MasterPasswordEntry = _masterPassword;

            _email.ReturnType = ReturnType.Next;
            _email.ReturnCommand = new Command(() => _masterPassword.Focus());

            if (Device.RuntimePlatform == Device.iOS)
            {
                ToolbarItems.Add(_moreItem);
            }
            else
            {
                ToolbarItems.Add(_getPasswordHint);
            }

            if (Device.RuntimePlatform == Device.Android && !_email.IsEnabled)
            {
                ToolbarItems.Add(_removeAccount);
            }

            if (_appOptions?.IosExtension ?? false)
            {
                _vm.ShowCancelButton = true;
            }

            if (_appOptions?.HideAccountSwitcher ?? false)
            {
                ToolbarItems.Remove(_accountAvatar);
            }
        }

        public Entry MasterPasswordEntry { get; set; }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            _mainContent.Content = _mainLayout;
            _accountAvatar?.OnAppearing();

            if (!_appOptions?.HideAccountSwitcher ?? false)
            {
                _vm.AvatarImageSource = await GetAvatarImageSourceAsync();
            }
            await _vm.InitAsync();
            if (!_inputFocused)
            {
                RequestFocus(string.IsNullOrWhiteSpace(_vm.Email) ? _email : _masterPassword);
                _inputFocused = true;
            }
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

            _accountAvatar?.OnDisappearing();
        }

        private async void LogIn_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.LogInAsync(true, _email.IsEnabled);
            }
        }

        private void Hint_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                Navigation.PushModalAsync(new NavigationPage(new HintPage()));
            }
        }

        private async void RemoveAccount_Clicked(object sender, EventArgs e)
        {
            await _accountListOverlay.HideAsync();
            if (DoOnce())
            {
                await _vm.RemoveAccountAsync();
            }
        }

        private void Cancel_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.CloseAction();
            }
        }

        private async void More_Clicked(object sender, System.EventArgs e)
        {
            await _accountListOverlay.HideAsync();
            if (!DoOnce())
            {
                return;
            }

            var buttons = _email.IsEnabled ? new[] { AppResources.GetPasswordHint }
                : new[] { AppResources.GetPasswordHint, AppResources.RemoveAccount };
            var selection = await DisplayActionSheet(AppResources.Options,
                AppResources.Cancel, null, buttons);

            if (selection == AppResources.GetPasswordHint)
            {
                await Navigation.PushModalAsync(new NavigationPage(new HintPage()));
            }
            else if (selection == AppResources.RemoveAccount)
            {
                await _vm.RemoveAccountAsync();
            }
        }

        private async Task StartTwoFactorAsync()
        {
            var page = new TwoFactorPage(false, _appOptions);
            await Navigation.PushModalAsync(new NavigationPage(page));
        }

        private async Task LogInSuccessAsync()
        {
            if (AppHelpers.SetAlternateMainPage(_appOptions))
            {
                return;
            }
            var previousPage = await AppHelpers.ClearPreviousPage();
            Application.Current.MainPage = new TabsPage(_appOptions, previousPage);
        }

        private async Task UpdateTempPasswordAsync()
        {
            var page = new UpdateTempPasswordPage();
            await Navigation.PushModalAsync(new NavigationPage(page));
        }
    }
}
