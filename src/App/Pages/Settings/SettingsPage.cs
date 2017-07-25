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
using Bit.App.Utilities;

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
        private ExtendedTextCell TwoStepCell { get; set; }
        private ExtendedTextCell ChangeMasterPasswordCell { get; set; }
        private ExtendedTextCell ChangeEmailCell { get; set; }
        private ExtendedTextCell FoldersCell { get; set; }
        private ExtendedTextCell SyncCell { get; set; }
        private ExtendedTextCell LockCell { get; set; }
        private ExtendedTextCell LogOutCell { get; set; }
        private ExtendedTextCell AboutCell { get; set; }
        private ExtendedTextCell HelpCell { get; set; }
        private ExtendedTextCell RateCell { get; set; }
        private ExtendedTextCell FeaturesCell { get; set; }
        private LongDetailViewCell RateCellLong { get; set; }
        private ExtendedTableView Table { get; set; }

        private async void Init()
        {
            PinCell = new ExtendedSwitchCell
            {
                Text = AppResources.UnlockWithPIN,
                On = _settings.GetValueOrDefault(Constants.SettingPinUnlockOn, false)
            };

            LockOptionsCell = new ExtendedTextCell
            {
                Text = AppResources.LockOptions,
                Detail = GetLockOptionsDetailsText(),
                ShowDisclousure = true
            };

            TwoStepCell = new ExtendedTextCell
            {
                Text = AppResources.TwoStepLogin,
                ShowDisclousure = true
            };

            LockCell = new ExtendedTextCell
            {
                Text = AppResources.Lock
            };

            var securitySecion = new TableSection(AppResources.Security)
            {
                LockOptionsCell,
                PinCell,
                LockCell,
                TwoStepCell
            };

            if((await _fingerprint.GetAvailabilityAsync()) == FingerprintAvailability.Available)
            {
                var fingerprintName = Helpers.OnPlatform(iOS: AppResources.TouchID, Android: AppResources.Fingerprint,
                    WinPhone: AppResources.Fingerprint);
                FingerprintCell = new ExtendedSwitchCell
                {
                    Text = string.Format(AppResources.UnlockWith, fingerprintName),
                    On = _settings.GetValueOrDefault(Constants.SettingFingerprintUnlockOn, false),
                    IsEnabled = true
                };
                securitySecion.Insert(1, FingerprintCell);
            }

            ChangeMasterPasswordCell = new ExtendedTextCell
            {
                Text = AppResources.ChangeMasterPassword,
                ShowDisclousure = true
            };

            ChangeEmailCell = new ExtendedTextCell
            {
                Text = AppResources.ChangeEmail,
                ShowDisclousure = true
            };

            FoldersCell = new ExtendedTextCell
            {
                Text = AppResources.Folders,
                ShowDisclousure = true
            };

            SyncCell = new ExtendedTextCell
            {
                Text = AppResources.Sync,
                ShowDisclousure = true
            };

            LogOutCell = new ExtendedTextCell
            {
                Text = AppResources.LogOut
            };

            AboutCell = new ExtendedTextCell
            {
                Text = AppResources.About,
                ShowDisclousure = true
            };

            HelpCell = new ExtendedTextCell
            {
                Text = AppResources.HelpAndFeedback,
                ShowDisclousure = true
            };

            FeaturesCell = new ExtendedTextCell
            {
                Text = AppResources.Features,
                ShowDisclousure = true
            };

            var otherSection = new TableSection(AppResources.Other)
            {
                FeaturesCell,
                AboutCell,
                HelpCell
            };

            if(Device.RuntimePlatform == Device.iOS)
            {
                RateCellLong = new LongDetailViewCell(AppResources.RateTheApp, AppResources.RateTheAppDescriptionAppStore);
                otherSection.Add(RateCellLong);
            }
            else
            {
                RateCell = new ExtendedTextCell
                {
                    Text = AppResources.RateTheApp,
                    Detail = AppResources.RateTheAppDescription,
                    ShowDisclousure = true,
                    DetailLineBreakMode = LineBreakMode.WordWrap
                };
                otherSection.Add(RateCell);
            }

            Table = new CustomTable
            {
                Root = new TableRoot
                {
                    securitySecion,
                    new TableSection(AppResources.Account)
                    {
                        ChangeMasterPasswordCell,
                        ChangeEmailCell,
                        LogOutCell
                    },
                    new TableSection(AppResources.Manage)
                    {
                        FoldersCell,
                        SyncCell
                    },
                    otherSection
                }
            };

            Title = AppResources.Settings;
            Content = Table;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();

            PinCell.OnChanged += PinCell_Changed;
            LockOptionsCell.Tapped += LockOptionsCell_Tapped;
            TwoStepCell.Tapped += TwoStepCell_Tapped;
            ChangeMasterPasswordCell.Tapped += ChangeMasterPasswordCell_Tapped;

            if(FingerprintCell != null)
            {
                FingerprintCell.OnChanged += FingerprintCell_Changed;
            }

            ChangeEmailCell.Tapped += ChangeEmailCell_Tapped;
            FoldersCell.Tapped += FoldersCell_Tapped;
            SyncCell.Tapped += SyncCell_Tapped;
            LockCell.Tapped += LockCell_Tapped;
            LogOutCell.Tapped += LogOutCell_Tapped;
            AboutCell.Tapped += AboutCell_Tapped;   
            HelpCell.Tapped += HelpCell_Tapped;
            FeaturesCell.Tapped += FeaturesCell_Tapped;

            if(RateCellLong != null)
            {
                RateCellLong.Tapped += RateCell_Tapped;
            }

            if(RateCell != null)
            {
                RateCell.Tapped += RateCell_Tapped;
            }
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();

            PinCell.OnChanged -= PinCell_Changed;
            LockOptionsCell.Tapped -= LockOptionsCell_Tapped;
            TwoStepCell.Tapped -= TwoStepCell_Tapped;
            ChangeMasterPasswordCell.Tapped -= ChangeMasterPasswordCell_Tapped;

            if(FingerprintCell != null)
            {
                FingerprintCell.OnChanged -= FingerprintCell_Changed;
            }

            ChangeEmailCell.Tapped -= ChangeEmailCell_Tapped;
            FoldersCell.Tapped -= FoldersCell_Tapped;
            SyncCell.Tapped -= SyncCell_Tapped;
            LockCell.Tapped -= LockCell_Tapped;
            LogOutCell.Tapped -= LogOutCell_Tapped;
            AboutCell.Tapped -= AboutCell_Tapped;
            HelpCell.Tapped -= HelpCell_Tapped;
            FeaturesCell.Tapped -= FeaturesCell_Tapped;

            if(RateCellLong != null)
            {
                RateCellLong.Tapped -= RateCell_Tapped;
            }

            if(RateCell != null)
            {
                RateCell.Tapped -= RateCell_Tapped;
            }
        }

        private async void TwoStepCell_Tapped(object sender, EventArgs e)
        {
            if(!await _userDialogs.ConfirmAsync(AppResources.TwoStepLoginConfirmation, null, AppResources.Yes,
                AppResources.Cancel))
            {
                return;
            }

            _googleAnalyticsService.TrackAppEvent("OpenedSetting", "TwoStep");
            Device.OpenUri(new Uri("https://help.bitwarden.com/article/setup-two-step-login/"));
        }

        private async void LockOptionsCell_Tapped(object sender, EventArgs e)
        {
            var selection = await DisplayActionSheet(AppResources.LockOptions, AppResources.Cancel, null,
                AppResources.LockOptionImmediately, AppResources.LockOption1Minute, AppResources.LockOption15Minutes,
                AppResources.LockOption1Hour, AppResources.LockOption4Hours, AppResources.Never);

            if(selection == AppResources.Cancel)
            {
                return;
            }

            if(selection == AppResources.LockOptionImmediately)
            {
                _settings.AddOrUpdateValue(Constants.SettingLockSeconds, 0);
            }
            else if(selection == AppResources.LockOption1Minute)
            {
                _settings.AddOrUpdateValue(Constants.SettingLockSeconds, 60);
            }
            else if(selection == AppResources.LockOption15Minutes)
            {
                _settings.AddOrUpdateValue(Constants.SettingLockSeconds, 60 * 15);
            }
            else if(selection == AppResources.LockOption1Hour)
            {
                _settings.AddOrUpdateValue(Constants.SettingLockSeconds, 60 * 60);
            }
            else if(selection == AppResources.LockOption4Hours)
            {
                _settings.AddOrUpdateValue(Constants.SettingLockSeconds, 60 * 60 * 4);
            }
            else if(selection == AppResources.Never)
            {
                _settings.AddOrUpdateValue(Constants.SettingLockSeconds, -1);
            }

            LockOptionsCell.Detail = selection;
        }

        private void SyncCell_Tapped(object sender, EventArgs e)
        {
            Navigation.PushModalAsync(new ExtendedNavigationPage(new SettingsSyncPage()));
        }

        private void AboutCell_Tapped(object sender, EventArgs e)
        {
            Navigation.PushModalAsync(new ExtendedNavigationPage(new SettingsAboutPage()));
        }

        private void RateCell_Tapped(object sender, EventArgs e)
        {
            _googleAnalyticsService.TrackAppEvent("OpenedSetting", "RateApp");
            if(Device.RuntimePlatform == Device.iOS)
            {
                Device.OpenUri(new Uri($"itms-apps://itunes.apple.com/WebObjects/MZStore.woa/wa/viewContentsUserReviews" +
                    "?id=1137397744&onlyLatestVersion=true&pageNumber=0&sortOrdering=1&type=Purple+Software"));
            }
            else if(Device.RuntimePlatform == Device.Android)
            {
                MessagingCenter.Send(Application.Current, "RateApp");
            }
        }

        private void HelpCell_Tapped(object sender, EventArgs e)
        {
            Navigation.PushModalAsync(new ExtendedNavigationPage(new SettingsHelpPage()));
        }

        private void LockCell_Tapped(object sender, EventArgs e)
        {
            _googleAnalyticsService.TrackAppEvent("Locked");
            MessagingCenter.Send(Application.Current, "Lock", true);
        }

        private async void LogOutCell_Tapped(object sender, EventArgs e)
        {
            if(!await _userDialogs.ConfirmAsync(AppResources.LogoutConfirmation, null, AppResources.Yes, AppResources.Cancel))
            {
                return;
            }

            MessagingCenter.Send(Application.Current, "Logout", (string)null);
        }

        private async void ChangeMasterPasswordCell_Tapped(object sender, EventArgs e)
        {
            if(!await _userDialogs.ConfirmAsync(AppResources.ChangePasswordConfirmation, null, AppResources.Yes,
                AppResources.Cancel))
            {
                return;
            }

            _googleAnalyticsService.TrackAppEvent("OpenedSetting", "ChangePassword");
            Device.OpenUri(new Uri("https://help.bitwarden.com/article/change-your-master-password/"));
        }

        private async void ChangeEmailCell_Tapped(object sender, EventArgs e)
        {
            if(!await _userDialogs.ConfirmAsync(AppResources.ChangeEmailConfirmation, null, AppResources.Yes,
                AppResources.Cancel))
            {
                return;
            }

            _googleAnalyticsService.TrackAppEvent("OpenedSetting", "ChangeEmail");
            Device.OpenUri(new Uri("https://help.bitwarden.com/article/change-your-email/"));
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
                var pinPage = new SettingsPinPage((page) => PinEntered(page));
                Navigation.PushModalAsync(new ExtendedNavigationPage(pinPage));
            }
            else if(!cell.On)
            {
                _settings.AddOrUpdateValue(Constants.SettingPinUnlockOn, false);
            }
        }

        private void PinEntered(SettingsPinPage page)
        {
            page.PinControl.Entry.Unfocus();
            page.Navigation.PopModalAsync();

            _authService.PIN = page.Model.PIN;

            _settings.AddOrUpdateValue(Constants.SettingPinUnlockOn, true);
            _settings.AddOrUpdateValue(Constants.SettingFingerprintUnlockOn, false);
            PinCell.On = true;

            if(FingerprintCell != null)
            {
                FingerprintCell.On = false;
            }
        }

        private void FeaturesCell_Tapped(object sender, EventArgs e)
        {
            Navigation.PushModalAsync(new ExtendedNavigationPage(new SettingsFeaturesPage()));
        }

        private void FoldersCell_Tapped(object sender, EventArgs e)
        {
            Navigation.PushModalAsync(new ExtendedNavigationPage(new SettingsListFoldersPage()));
        }

        private string GetLockOptionsDetailsText()
        {
            var lockSeconds = _settings.GetValueOrDefault(Constants.SettingLockSeconds, 60 * 15);
            if(lockSeconds == -1)
            {
                return AppResources.Never;
            }
            else if(lockSeconds == 60)
            {
                return AppResources.LockOption1Minute;
            }
            else if(lockSeconds == 60 * 15)
            {
                return AppResources.LockOption15Minutes;
            }
            else if(lockSeconds == 60 * 60)
            {
                return AppResources.LockOption1Hour;
            }
            else if(lockSeconds == 60 * 60 * 4)
            {
                return AppResources.LockOption4Hours;
            }
            else
            {
                return AppResources.LockOptionImmediately;
            }
        }

        private class CustomTable : ExtendedTableView
        {
            public CustomTable()
            {
                Intent = TableIntent.Settings;
                HasUnevenRows = true;

                if(Device.RuntimePlatform == Device.iOS)
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
