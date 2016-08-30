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
        private readonly IGoogleAnalyticsService _googleAnalyticsService;

        // TODO: Model binding context?

        public SettingsPage()
        {
            _authService = Resolver.Resolve<IAuthService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _settings = Resolver.Resolve<ISettings>();
            _fingerprint = Resolver.Resolve<IFingerprint>();
            _pushNotification = Resolver.Resolve<IPushNotification>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();

            Init();
        }

        private ExtendedSwitchCell PinCell { get; set; }
        private ExtendedSwitchCell FingerprintCell { get; set; }
        private ExtendedTextCell LockOptionsCell { get; set; }
        private ExtendedTableView Table { get; set; }

        private void Init()
        {
            PinCell = new ExtendedSwitchCell
            {
                Text = "Unlock with PIN Code",
                On = _settings.GetValueOrDefault(Constants.SettingPinUnlockOn, false)
            };
            PinCell.OnChanged += PinCell_Changed;

            LockOptionsCell = new ExtendedTextCell
            {
                Text = "Lock Options",
                Detail = GetLockOptionsDetailsText(),
                ShowDisclousure = true
            };
            LockOptionsCell.Tapped += LockOptionsCell_Tapped;

            var twoStepCell = new ExtendedTextCell
            {
                Text = "Two-step Login",
                ShowDisclousure = true
            };
            twoStepCell.Tapped += TwoStepCell_Tapped; ;

            var securitySecion = new TableSection("Security")
            {
                LockOptionsCell,
                PinCell,
                twoStepCell
            };

            if(_fingerprint.IsAvailable)
            {
                var fingerprintName = Device.OnPlatform(iOS: "Touch ID", Android: "Fingerprint", WinPhone: "Fingerprint");
                FingerprintCell = new ExtendedSwitchCell
                {
                    Text = "Unlock with " + fingerprintName,
                    On = _settings.GetValueOrDefault(Constants.SettingFingerprintUnlockOn, false),
                    IsEnabled = _fingerprint.IsAvailable
                };
                FingerprintCell.OnChanged += FingerprintCell_Changed;
                securitySecion.Insert(1, FingerprintCell);
            }

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

            var aboutCell = new ExtendedTextCell
            {
                Text = "About",
                ShowDisclousure = true
            };
            aboutCell.Tapped += AboutCell_Tapped;

            var helpCell = new ExtendedTextCell
            {
                Text = "Help and Feedback",
                ShowDisclousure = true
            };
            helpCell.Tapped += HelpCell_Tapped;

            var otherSection = new TableSection("Other")
            {
                aboutCell,
                helpCell
            };

            if(Device.OS == TargetPlatform.iOS)
            {
                var rateCell = new LongDetailViewCell("Rate the App",
                    "App Store ratings are reset with every new version of bitwarden."
                    + " Please consider helping us out with a good review!");
                rateCell.Tapped += RateCell_Tapped;
                otherSection.Add(rateCell);
            }
            else
            {
                var rateCell = new ExtendedTextCell
                {
                    Text = "Rate the App",
                    Detail = "Please consider helping us out with a good review!",
                    ShowDisclousure = true,
                    DetailLineBreakMode = LineBreakMode.WordWrap
                };
                rateCell.Tapped += RateCell_Tapped;
                otherSection.Add(rateCell);
            }

            Table = new CustomTable
            {
                Root = new TableRoot
                {
                    securitySecion,
                    new TableSection("Account")
                    {
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
                    },
                    otherSection
                }
            };

            Title = AppResources.Settings;
            Content = Table;
        }

        private async void TwoStepCell_Tapped(object sender, EventArgs e)
        {
            if(!await _userDialogs.ConfirmAsync("Two-step login makes your account more secure by requiring you to enter"
                + " a security code from an authenticator app whenever you log in. Two-step login can be enabled on the"
                + " bitwarden.com web vault. Do you want to visit the website now?",
                null, AppResources.Yes, AppResources.Cancel))
            {
                return;
            }

            _googleAnalyticsService.TrackAppEvent("OpenedSetting", "TwoStep");
            Device.OpenUri(new Uri("https://vault.bitwarden.com"));
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
                _settings.AddOrUpdateValue(Constants.SettingLockSeconds, -1);
            }

            LockOptionsCell.Detail = selection;
        }

        private void SyncCell_Tapped(object sender, EventArgs e)
        {
            Navigation.PushAsync(new SettingsSyncPage());
        }

        private void AboutCell_Tapped(object sender, EventArgs e)
        {
            Navigation.PushAsync(new SettingsAboutPage());
        }

        private void RateCell_Tapped(object sender, EventArgs e)
        {
            _googleAnalyticsService.TrackAppEvent("OpenedSetting", "RateApp");
            if(Device.OS == TargetPlatform.iOS)
            {
                Device.OpenUri(new Uri($"itms-apps://itunes.apple.com/WebObjects/MZStore.woa/wa/viewContentsUserReviews" +
                    "?id=1137397744&onlyLatestVersion=true&pageNumber=0&sortOrdering=1&type=Purple+Software"));
            }
            else if(Device.OS == TargetPlatform.Android)
            {
                MessagingCenter.Send(Application.Current, "RateApp");
            }
        }

        private void HelpCell_Tapped(object sender, EventArgs e)
        {
            Navigation.PushAsync(new SettingsHelpPage());
        }

        private void LockCell_Tapped(object sender, EventArgs e)
        {
            _googleAnalyticsService.TrackAppEvent("Locked");
            _settings.AddOrUpdateValue(Constants.Locked, true);
            MessagingCenter.Send(Application.Current, "Lock", true);
        }

        private async void LogOutCell_Tapped(object sender, EventArgs e)
        {
            if(!await _userDialogs.ConfirmAsync(
                "Are you sure you want to log out?", null, AppResources.Yes, AppResources.Cancel))
            {
                return;
            }

            MessagingCenter.Send(Application.Current, "Logout", (string)null);
        }

        private async void ChangeMasterPasswordCell_Tapped(object sender, EventArgs e)
        {
            if(!await _userDialogs.ConfirmAsync("You can change your master password on the bitwarden.com web vault."
                + "Do you want to visit the website now?", null, AppResources.Yes, AppResources.Cancel))
            {
                return;
            }

            _googleAnalyticsService.TrackAppEvent("OpenedSetting", "ChangePassword");
            Device.OpenUri(new Uri("https://vault.bitwarden.com"));
        }

        private async void ChangeEmailCell_Tapped(object sender, EventArgs e)
        {
            if(!await _userDialogs.ConfirmAsync("You can change your email address on the bitwarden.com web vault."
                + " Do you want to visit the website now?", null, AppResources.Yes, AppResources.Cancel))
            {
                return;
            }

            _googleAnalyticsService.TrackAppEvent("OpenedSetting", "ChangeEmail");
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

            if(cell.On && !_settings.GetValueOrDefault(Constants.SettingPinUnlockOn, false))
            {
                cell.On = false;
                var pinPage = new SettingsPinPage();
                pinPage.OnPinEntered += PinEntered;
                Navigation.PushAsync(pinPage);
            }
            else if(!cell.On)
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
            PinCell.On = true;

            if(FingerprintCell != null)
            {
                FingerprintCell.On = false;
            }
        }

        private void FoldersCell_Tapped(object sender, EventArgs e)
        {
            Navigation.PushAsync(new SettingsListFoldersPage());
        }

        private string GetLockOptionsDetailsText()
        {
            var lockSeconds = _settings.GetValueOrDefault(Constants.SettingLockSeconds, 60 * 15);
            if(lockSeconds == -1)
            {
                return "Never";
            }
            else if(lockSeconds == 60)
            {
                return "1 minute";
            }
            else if(lockSeconds == 60 * 15)
            {
                return "15 minutes";
            }
            else if(lockSeconds == 60 * 60)
            {
                return "1 hour";
            }
            else if(lockSeconds == 60 * 60 * 4)
            {
                return "4 hours";
            }
            else
            {
                return "Immediately";
            }
        }

        private class CustomTable : ExtendedTableView
        {
            public CustomTable()
            {
                Intent = TableIntent.Settings;
                HasUnevenRows = true;

                if(Device.OS == TargetPlatform.iOS)
                {
                    RowHeight = -1;
                    EstimatedRowHeight = 44;
                }
            }
        }

        private class LongDetailViewCell : ExtendedViewCell
        {
            public LongDetailViewCell(string labelText, string detailText)
            {
                Label = new Label
                {
                    FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Label)),
                    VerticalOptions = LayoutOptions.CenterAndExpand,
                    LineBreakMode = LineBreakMode.TailTruncation,
                    Text = labelText
                };

                Detail = new Label
                {
                    FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                    LineBreakMode = LineBreakMode.WordWrap,
                    VerticalOptions = LayoutOptions.End,
                    Style = (Style)Application.Current.Resources["text-muted"],
                    Text = detailText
                };

                var labelDetailStackLayout = new StackLayout
                {
                    HorizontalOptions = LayoutOptions.StartAndExpand,
                    VerticalOptions = LayoutOptions.FillAndExpand,
                    Children = { Label, Detail },
                    Padding = new Thickness(15)
                };

                ShowDisclousure = true;
                View = labelDetailStackLayout;
            }

            public Label Label { get; set; }
            public Label Detail { get; set; }
        }
    }
}
