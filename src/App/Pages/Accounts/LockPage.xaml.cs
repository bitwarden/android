using Bit.App.Models;
using Bit.App.Resources;
using System;
using System.Threading.Tasks;
using Bit.App.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class LockPage : BaseContentPage
    {
        private readonly AppOptions _appOptions;
        private readonly bool _autoPromptBiometric;
        private readonly LockPageViewModel _vm;

        private bool _promptedAfterResume;
        private bool _appeared;

        public LockPage(AppOptions appOptions = null, bool autoPromptBiometric = true)
        {
            _appOptions = appOptions;
            _autoPromptBiometric = autoPromptBiometric;
            InitializeComponent();
            _vm = BindingContext as LockPageViewModel;
            _vm.Page = this;
            _vm.UnlockedAction = () => Device.BeginInvokeOnMainThread(async () => await UnlockedAsync());
            MasterPasswordEntry = _masterPassword;
            PinEntry = _pin;

            if (Device.RuntimePlatform == Device.iOS)
            {
                ToolbarItems.Add(_moreItem);
            }
            else
            {
                ToolbarItems.Add(_logOut);
            }
        }

        public Entry MasterPasswordEntry { get; set; }
        public Entry PinEntry { get; set; }

        public async Task PromptBiometricAfterResumeAsync()
        {
            if (_vm.BiometricLock)
            {
                await Task.Delay(500);
                if (!_promptedAfterResume)
                {
                    _promptedAfterResume = true;
                    await _vm?.PromptBiometricAsync();
                }
            }
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            if (_appeared)
            {
                return;
            }
            _appeared = true;
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
            if (!_vm.BiometricLock)
            {
                if (_vm.PinLock)
                {
                    RequestFocus(PinEntry);
                }
                else
                {
                    RequestFocus(MasterPasswordEntry);
                }
            }
            else
            {
                if (_vm.UsingKeyConnector && !_vm.PinLock)
                {
                    _passwordGrid.IsVisible = false;
                    _unlockButton.IsVisible = false;
                }
                if (_autoPromptBiometric)
                {
                    var tasks = Task.Run(async () =>
                    {
                        await Task.Delay(500);
                        Device.BeginInvokeOnMainThread(async () => await _vm.PromptBiometricAsync());
                    });
                }
            }
        }

        private void Unlock_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                var tasks = Task.Run(async () =>
                {
                    await Task.Delay(50);
                    Device.BeginInvokeOnMainThread(async () => await _vm.SubmitAsync());
                });
            }
        }

        private async void LogOut_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.LogOutAsync();
            }
        }

        private async void Biometric_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.PromptBiometricAsync();
            }
        }

        private async void More_Clicked(object sender, System.EventArgs e)
        {
            if (!DoOnce())
            {
                return;
            }

            var selection = await DisplayActionSheet(AppResources.Options,
                AppResources.Cancel, null, AppResources.LogOut);

            if (selection == AppResources.LogOut)
            {
                await _vm.LogOutAsync();
            }
        }

        private async Task UnlockedAsync()
        {
            if (AppHelpers.SetAlternateMainPage(_appOptions))
            {
                return;
            }
            var previousPage = await AppHelpers.ClearPreviousPage();

            Application.Current.MainPage = new TabsPage(_appOptions, previousPage);
        }

        private async void AccountSwitch_Clicked(object sender, EventArgs e)
        {
            if (_accountListOverlay.IsVisible)
            {
                await ShowAccountListAsync(false, _accountListContainer, _accountListOverlay);
            }
            else
            {
                await RefreshAccountViewsAsync(_accountListView);
                await ShowAccountListAsync(true, _accountListContainer, _accountListOverlay);
            }
        }

        private async void AccountRow_Selected(object sender, SelectedItemChangedEventArgs e)
        {
            await AccountRowSelectedAsync(sender, e, _accountListContainer, _accountListOverlay, null, true);
        }
    }
}
