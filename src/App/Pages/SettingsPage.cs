using System;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Bit.App.Controls;
using Acr.UserDialogs;
using Plugin.Settings.Abstractions;
using Plugin.Fingerprint.Abstractions;

namespace Bit.App.Pages
{
    public class SettingsPage : ContentPage
    {
        private readonly IAuthService _authService;
        private readonly IUserDialogs _userDialogs;
        private readonly ISettings _settings;
        private readonly IFingerprint _fingerprint;

        // TODO: Model binding context?

        public SettingsPage()
        {
            _authService = Resolver.Resolve<IAuthService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _settings = Resolver.Resolve<ISettings>();
            _fingerprint = Resolver.Resolve<IFingerprint>();

            Init();
        }

        private ExtendedSwitchCell PinCell { get; set; }
        private ExtendedSwitchCell FingerprintCell { get; set; }
        private ExtendedTextCell LockOptionsCell { get; set; }

        private void Init()
        {
            FingerprintCell = new ExtendedSwitchCell
            {
                Text = "Use Touch ID" + (!_fingerprint.IsAvailable ? " (Unavilable)" : null),
                On = _settings.GetValueOrDefault<bool>(Constants.SettingFingerprintUnlockOn),
                IsEnabled = _fingerprint.IsAvailable
            };
            FingerprintCell.OnChanged += FingerprintCell_Changed;

            PinCell = new ExtendedSwitchCell
            {
                Text = "Use PIN Code",
                On = _settings.GetValueOrDefault<bool>(Constants.SettingPinUnlockOn)
            };
            PinCell.OnChanged += PinCell_Changed;

            LockOptionsCell = new ExtendedTextCell
            {
                Text = "Lock Options",
                // TODO: Set detail based on setting
                Detail = "Immediately",
                ShowDisclousure = true
            };
            LockOptionsCell.Tapped += LockOptionsCell_Tapped;

            var changeMasterPasswordCell = new ExtendedTextCell
            {
                Text = "Change Master Password",
                TextColor = Color.FromHex("333333")
            };
            changeMasterPasswordCell.Tapped += ChangeMasterPasswordCell_Tapped;

            var foldersCell = new ExtendedTextCell
            {
                Text = "Folders",
                ShowDisclousure = true,
                TextColor = Color.FromHex("333333")
            };
            foldersCell.Tapped += FoldersCell_Tapped;

            var lockCell = new ExtendedTextCell
            {
                Text = "Lock",
                TextColor = Color.FromHex("333333")
            };
            lockCell.Tapped += LockCell_Tapped;

            var logOutCell = new ExtendedTextCell
            {
                Text = "Log Out",
                TextColor = Color.FromHex("333333")
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
                        LockOptionsCell,
                        FingerprintCell,
                        PinCell,
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

        private async void LockOptionsCell_Tapped(object sender, EventArgs e)
        {
            var selection = await DisplayActionSheet("Lock Options", AppResources.Cancel, null,
                "Immediately", "1 minute", "3 minutes", "15 minutes", "1 hour", "8 hours", "24 hours", "Never");

            if(selection == "Immediately")
            {
                _settings.AddOrUpdateValue(Constants.SettingLockSeconds, 0);
            }
            else if(selection == "1 minute")
            {
                _settings.AddOrUpdateValue(Constants.SettingLockSeconds, 60);
            }
            // TODO: others
            else
            {
                // Never lock
                _settings.Remove(Constants.SettingLockSeconds);
            }

            LockOptionsCell.Detail = selection;
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

        private async void ChangeMasterPasswordCell_Tapped(object sender, EventArgs e)
        {
            if(!await _userDialogs.ConfirmAsync("You can change your master password on the bitwarden.com web vault. Do you want to visit the website now?", null, AppResources.Yes, AppResources.Cancel))
            {
                return;
            }

            Device.OpenUri(new Uri("https://vault.bitwarden.com"));
        }

        private void FingerprintCell_Changed(object sender, EventArgs e)
        {
            var cell = sender as ExtendedSwitchCell;
            if(cell == null)
            {
                return;
            }

            _settings.AddOrUpdateValue(Constants.SettingFingerprintUnlockOn, cell.On);

            if(cell.On)
            {
                _settings.AddOrUpdateValue(Constants.SettingPinUnlockOn, false);
                PinCell.On = false;
            }
        }

        private void PinCell_Changed(object sender, EventArgs e)
        {
            var cell = sender as ExtendedSwitchCell;
            if(cell == null)
            {
                return;
            }

            _settings.AddOrUpdateValue(Constants.SettingPinUnlockOn, cell.On);
            if(cell.On)
            {
                _settings.AddOrUpdateValue(Constants.SettingFingerprintUnlockOn, false);
                FingerprintCell.On = false;
            }
        }

        private void FoldersCell_Tapped(object sender, EventArgs e)
        {
            Navigation.PushAsync(new SettingsListFoldersPage());
        }
    }
}
