using System;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Bit.App.Controls;
using Acr.UserDialogs;

namespace Bit.App.Pages
{
    public class SettingsPage : ContentPage
    {
        private readonly IAuthService _authService;
        private readonly IUserDialogs _userDialogs;

        public SettingsPage()
        {
            _authService = Resolver.Resolve<IAuthService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();

            Init();
        }

        private void Init()
        {
            var touchIdCell = new SwitchCell
            {
                Text = "Use Touch ID"
            };
            touchIdCell.Tapped += TouchIdCell_Tapped;

            var lockOnExitCell = new SwitchCell
            {
                Text = "Lock Immediately On Exit"
            };
            lockOnExitCell.Tapped += LockOnExitCell_Tapped;

            var changeMasterPasswordCell = new ExtendedTextCell
            {
                Text = "Change Master Password"
            };
            changeMasterPasswordCell.Tapped += ChangeMasterPasswordCell_Tapped;

            var foldersCell = new ExtendedTextCell
            {
                Text = "Folders",
                ShowDisclousure = true
            };
            foldersCell.Tapped += FoldersCell_Tapped;

            var lockCell = new ExtendedTextCell
            {
                Text = "Lock"
            };
            lockCell.Tapped += LockCell_Tapped;

            var logOutCell = new ExtendedTextCell
            {
                Text = "Log Out"
            };
            logOutCell.Tapped += LogOutCell_Tapped;

            var table = new ExtendedTableView
            {
                EnableScrolling = true,
                Intent = TableIntent.Menu,
                Root = new TableRoot
                {
                    new TableSection("Security")
                    {
                        touchIdCell,
                        lockOnExitCell,
                        changeMasterPasswordCell
                    },
                    new TableSection("Manage Folders")
                    {
                        foldersCell
                    },
                    new TableSection("Current Session")
                    {
                        lockCell,
                        logOutCell
                    }
                }
            };

            var scrollView = new ScrollView
            {
                Content = table
            };

            Title = AppResources.Settings;
            Content = table;
        }

        private void LockCell_Tapped(object sender, EventArgs e)
        {

        }

        private async void LogOutCell_Tapped(object sender, EventArgs e)
        {
            if(!await _userDialogs.ConfirmAsync("Are you sure you want to log out?", null, AppResources.Yes, AppResources.Cancel))
            {
                return;
            }

            _authService.LogOut();
            Application.Current.MainPage = new LoginNavigationPage();
        }

        private void LockOnExitCell_Tapped(object sender, EventArgs e)
        {

        }

        private async void ChangeMasterPasswordCell_Tapped(object sender, EventArgs e)
        {
            if(!await _userDialogs.ConfirmAsync("You can change your master password on the bitwarden.com web vault. Do you want to visit the website now?", null, AppResources.Yes, AppResources.Cancel))
            {
                return;
            }

            Device.OpenUri(new Uri("https://vault.bitwarden.com"));
        }

        private void TouchIdCell_Tapped(object sender, EventArgs e)
        {
            Navigation.PushAsync(new TestListPage());
        }

        private void FoldersCell_Tapped(object sender, EventArgs e)
        {
            Navigation.PushAsync(new SettingsListFoldersPage());
        }
    }
}
