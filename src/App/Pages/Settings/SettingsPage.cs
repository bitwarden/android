using System;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Bit.App.Controls;
using Acr.UserDialogs;
using Plugin.Settings.Abstractions;
using Plugin.Fingerprint.Abstractions;
using PushNotification.Plugin.Abstractions;

namespace Bit.App.Pages
{
    public class SettingsPage : ExtendedContentPage
    {
        private readonly IAuthService _authService;
        private readonly IUserDialogs _userDialogs;
        private readonly ISettings _settings;
        private readonly IFingerprint _fingerprint;
        private readonly IPushNotification _pushNotification;

        // TODO: Model binding context?

        public SettingsPage()
        {
            _authService = Resolver.Resolve<IAuthService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _settings = Resolver.Resolve<ISettings>();
            _fingerprint = Resolver.Resolve<IFingerprint>();
            _pushNotification = Resolver.Resolve<IPushNotification>();

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
                Detail = GetLockOptionsDetailsText(),
                ShowDisclousure = true
            };
            LockOptionsCell.Tapped += LockOptionsCell_Tapped;

            var changeMasterPasswordCell = new ExtendedTextCell
            {
                Text = "Change Master Password",
                ShowDisclousure = true
            };
            changeMasterPasswordCell.Tapped += ChangeMasterPasswordCell_Tapped;

            var changeEmailCell = new ExtendedTextCell
            {
                Text = "Change Email",
                ShowDisclousure = true
            };
            changeEmailCell.Tapped += ChangeEmailCell_Tapped;

            var foldersCell = new ExtendedTextCell
            {
                Text = "Folders",
                ShowDisclousure = true
            };
            foldersCell.Tapped += FoldersCell_Tapped;

            var syncCell = new ExtendedTextCell
            {
                Text = "Sync",
                ShowDisclousure = true
            };
            syncCell.Tapped += SyncCell_Tapped;

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
                HasUnevenRows = true,
                Root = new TableRoot
                {
                    new TableSection("Security")
                    {
                        LockOptionsCell,
                        FingerprintCell,
                        PinCell,
                        changeMasterPasswordCell,
                        changeEmailCell
                    },
                    new TableSection("Manage")
                    {
                        foldersCell,
                        syncCell
                    },
                    new TableSection("Current Session")
                    {
                        lockCell,
                        logOutCell
                    }
                }
            };

            if(Device.OS == TargetPlatform.iOS)
            {
                table.RowHeight = -1;
                table.EstimatedRowHeight = 44;
            }

            Title = AppResources.Settings;
            Content = table;
        }

        private async void LockOptionsCell_Tapped(object sender, EventArgs e)
        {
            var selection = await DisplayActionSheet("Lock Options", AppResources.Cancel, null,
                "Immediately", "1 minute", "15 minutes", "1 hour", "4 hours", "Never");

            if(selection == AppResources.Cancel)
            {
                return;
            }

            if(selection == "Immediately")
            {
                _settings.AddOrUpdateValue(Constants.SettingLockSeconds, 0);
            }
            else if(selection == "1 minute")
            {
                _settings.AddOrUpdateValue(Constants.SettingLockSeconds, 60);
            }
            else if(selection == "5 minutes")
            {
                _settings.AddOrUpdateValue(Constants.SettingLockSeconds, 60 * 5);
            }
            else if(selection == "15 minutes")
            {
                _settings.AddOrUpdateValue(Constants.SettingLockSeconds, 60 * 15);
            }
            else if(selection == "1 hour")
            {
                _settings.AddOrUpdateValue(Constants.SettingLockSeconds, 60 * 60);
            }
            else if(selection == "4 hours")
            {
                _settings.AddOrUpdateValue(Constants.SettingLockSeconds, 60 * 60 * 4);
            }
            else if(selection == "Never")
            {
                _settings.Remove(Constants.SettingLockSeconds);
            }

            LockOptionsCell.Detail = selection;
        }

        private void SyncCell_Tapped(object sender, EventArgs e)
        {
            Navigation.PushAsync(new SettingsSyncPage());
        }

        private void LockCell_Tapped(object sender, EventArgs e)
        {
            _settings.AddOrUpdateValue(Constants.SettingLocked, true);
            MessagingCenter.Send(Application.Current, "Lock", true);
        }

        private async void LogOutCell_Tapped(object sender, EventArgs e)
        {
            if(!await _userDialogs.ConfirmAsync("Are you sure you want to log out?", null, AppResources.Yes, AppResources.Cancel))
            {
                return;
            }

            MessagingCenter.Send(Application.Current, "Logout", (string)null);
        }

        private async void ChangeMasterPasswordCell_Tapped(object sender, EventArgs e)
        {
            if(!await _userDialogs.ConfirmAsync("You can change your master password on the bitwarden.com web vault. Do you want to visit the website now?", null, AppResources.Yes, AppResources.Cancel))
            {
                return;
            }

            Device.OpenUri(new Uri("https://vault.bitwarden.com"));
        }

        private async void ChangeEmailCell_Tapped(object sender, EventArgs e)
        {
            if(!await _userDialogs.ConfirmAsync("You can change your email address on the bitwarden.com web vault. Do you want to visit the website now?", null, AppResources.Yes, AppResources.Cancel))
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

            if(cell.On)
            {
                var pinPage = new SettingsPinPage();
                pinPage.OnPinEntered += PinEntered;
                Navigation.PushAsync(pinPage);
            }
            else
            {
                _settings.AddOrUpdateValue(Constants.SettingPinUnlockOn, false);
            }
        }

        private void PinEntered(object sender, EventArgs args)
        {
            var page = sender as SettingsPinPage;
            page.Navigation.PopAsync();

            _authService.PIN = page.Model.PIN;

            _settings.AddOrUpdateValue(Constants.SettingPinUnlockOn, true);
            _settings.AddOrUpdateValue(Constants.SettingFingerprintUnlockOn, false);
            FingerprintCell.On = false;
        }

        private void FoldersCell_Tapped(object sender, EventArgs e)
        {
            Navigation.PushAsync(new SettingsListFoldersPage());
        }

        private string GetLockOptionsDetailsText()
        {
            var lockSeconds = _settings.GetValueOrDefault<int?>(Constants.SettingLockSeconds);
            if(!lockSeconds.HasValue)
            {
                return "Never";
            }

            if(lockSeconds.Value == 60)
            {
                return "1 minute";
            }
            else if(lockSeconds.Value == 60 * 5)
            {
                return "5 minutes";
            }
            else if(lockSeconds.Value == 60 * 15)
            {
                return "15 minutes";
            }
            else if(lockSeconds.Value == 60 * 60)
            {
                return "1 hour";
            }
            else if(lockSeconds.Value == 60 * 60 * 4)
            {
                return "4 hours";
            }
            else
            {
                return "Immediately";
            }
        }
    }
}
