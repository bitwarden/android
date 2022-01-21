using Bit.App.Models;
using Bit.App.Resources;
using System;
using System.Threading.Tasks;
using Bit.App.Utilities;
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
                await Navigation.PopModalAsync();
            };
            _vm.Email = email;
            MasterPasswordEntry = _masterPassword;
            if (Device.RuntimePlatform == Device.Android)
            {
                ToolbarItems.RemoveAt(0);
            }

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

            if (!_appOptions?.IosExtension ?? false)
            {
                ToolbarItems.Remove(_closeItem);
            }
        }

        public Entry MasterPasswordEntry { get; set; }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            _mainContent.Content = _mainLayout;
            if (await ShowAccountSwitcherAsync())
            {
                _vm.AvatarImageSource = await GetAvatarImageSourceAsync();
            }
            else
            {
                ToolbarItems.Remove(_accountAvatar);
            }
            await _vm.InitAsync();
            if (!_inputFocused)
            {
                RequestFocus(string.IsNullOrWhiteSpace(_vm.Email) ? _email : _masterPassword);
                _inputFocused = true;
            }
        }

        private async void LogIn_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.LogInAsync();
            }
        }

        private void Hint_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                Navigation.PushModalAsync(new NavigationPage(new HintPage()));
            }
        }

        private void Close_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.CloseAction();
            }
        }

        private async void More_Clicked(object sender, System.EventArgs e)
        {
            if (!DoOnce())
            {
                return;
            }

            var selection = await DisplayActionSheet(AppResources.Options, 
                AppResources.Cancel, null, AppResources.GetPasswordHint);

            if (selection == AppResources.GetPasswordHint)
            {
                await Navigation.PushModalAsync(new NavigationPage(new HintPage()));
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

        private async void AccountSwitch_Clicked(object sender, EventArgs e)
        {
            if (_accountListOverlay.IsVisible)
            {
                await ShowAccountListAsync(false, _accountListContainer, _accountListOverlay);
            }
            else
            {
                await RefreshAccountViewsAsync(_accountListView, false);
                await ShowAccountListAsync(true, _accountListContainer, _accountListOverlay);
            }
        }

        private async void AccountRow_Selected(object sender, SelectedItemChangedEventArgs e)
        {
            await AccountRowSelectedAsync(sender, e, _accountListContainer, _accountListOverlay, null, true);
        }
    }
}
