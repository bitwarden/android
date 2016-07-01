using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Bit.App.Abstractions;
using Bit.App.Pages;
using Xamarin.Forms;
using System.Diagnostics;
using Plugin.Fingerprint.Abstractions;
using System.Threading.Tasks;
using Plugin.Settings.Abstractions;
using Bit.App.Controls;

namespace Bit.App
{
    public class App : Application
    {
        private readonly IDatabaseService _databaseService;
        private readonly ISyncService _syncService;
        private readonly IAuthService _authService;
        private readonly IFingerprint _fingerprint;
        private readonly ISettings _settings;

        public App(
            IAuthService authService,
            IDatabaseService databaseService,
            ISyncService syncService,
            IFingerprint fingerprint,
            ISettings settings)
        {
            _databaseService = databaseService;
            _syncService = syncService;
            _authService = authService;
            _fingerprint = fingerprint;
            _settings = settings;

            SetStyles();

            if(authService.IsAuthenticated)
            {
                MainPage = new MainPage();
            }
            else
            {
                MainPage = new HomePage();
            }

            MessagingCenter.Subscribe<Application, bool>(Current, "Resumed", async (sender, args) =>
            {
                var syncTask = _syncService.IncrementalSyncAsync();
                await CheckLockAsync(args);
                await syncTask;
            });

            MessagingCenter.Subscribe<Application, bool>(Current, "Lock", async (sender, args) =>
            {
                await CheckLockAsync(args);
            });
        }

        protected override void OnStart()
        {
            // Handle when your app starts
            var lockTask = CheckLockAsync(false);
            _databaseService.CreateTables();
            var syncTask = _syncService.FullSyncAsync();

            Debug.WriteLine("OnStart");
        }

        protected override void OnSleep()
        {
            // Handle when your app sleeps
            Debug.WriteLine("OnSleep");

            if(Device.OS == TargetPlatform.Android)
            {
                _settings.AddOrUpdateValue(Constants.SettingLastBackgroundedDate, DateTime.UtcNow);
            }
        }

        protected override void OnResume()
        {
            // Handle when your app resumes
            Debug.WriteLine("OnResume");

            if(Device.OS == TargetPlatform.Android)
            {
                var task = CheckLockAsync(false);
            }

            var lockPinPage = Current.MainPage.Navigation.ModalStack.LastOrDefault() as LockPinPage;
            if(lockPinPage != null)
            {
                lockPinPage.PinControl.Entry.Focus();
            }
        }

        private async Task CheckLockAsync(bool forceLock)
        {
            // Only lock if they are logged in
            if(!_authService.IsAuthenticated)
            {
                return;
            }

            // Are we forcing a lock? (i.e. clicking a button to lock the app manually, immediately)
            if(!forceLock)
            {
                // Lock seconds tells if if they want to lock the app or not
                var lockSeconds = _settings.GetValueOrDefault<int?>(Constants.SettingLockSeconds);
                if(!lockSeconds.HasValue)
                {
                    return;
                }

                // Has it been longer than lockSeconds since the last time the app was backgrounded?
                var now = DateTime.UtcNow;
                var lastBackground = _settings.GetValueOrDefault(Constants.SettingLastBackgroundedDate, now.AddYears(-1));
                if((now - lastBackground).TotalSeconds < lockSeconds.Value)
                {
                    return;
                }
            }

            // What method are we using to unlock?
            var fingerprintUnlock = _settings.GetValueOrDefault<bool>(Constants.SettingFingerprintUnlockOn);
            var pinUnlock = _settings.GetValueOrDefault<bool>(Constants.SettingPinUnlockOn);
            var currentPage = Current.MainPage.Navigation.ModalStack.LastOrDefault() as ExtendedNavigationPage;
            if(fingerprintUnlock && _fingerprint.IsAvailable)
            {
                if((currentPage?.CurrentPage as LockFingerprintPage) == null)
                {
                    await Current.MainPage.Navigation.PushModalAsync(new ExtendedNavigationPage(new LockFingerprintPage(!forceLock)), false);
                }
            }
            else if(pinUnlock && !string.IsNullOrWhiteSpace(_authService.PIN))
            {
                var lockPinPage = (currentPage?.CurrentPage as LockPinPage);
                if(lockPinPage == null)
                {
                    lockPinPage = new LockPinPage();
                    await Current.MainPage.Navigation.PushModalAsync(new ExtendedNavigationPage(lockPinPage), false);
                    lockPinPage.PinControl.Entry.Focus();
                }
            }
            else
            {
                // Use master password to unlock if no other methods are set
            }
        }

        private void SetStyles()
        {
            var gray = Color.FromHex("333333");
            var grayLight = Color.FromHex("777777");
            var grayLighter = Color.FromHex("d2d6de");
            var primaryColor = Color.FromHex("3c8dbc");
            var primaryColorAccent = Color.FromHex("286090");

            Resources = new ResourceDictionary();

            // Labels

            Resources.Add("text-muted", new Style(typeof(Label))
            {
                Setters = {
                    new Setter { Property = Label.TextColorProperty, Value = grayLight }
                }
            });

            // Buttons

            Resources.Add("btn-default", new Style(typeof(Button))
            {
                Setters = {
                    new Setter { Property = Button.TextColorProperty, Value = gray }
                }
            });

            Resources.Add("btn-primary", new Style(typeof(Button))
            {
                Setters = {
                    new Setter { Property = Button.TextColorProperty, Value = Color.White },
                    new Setter { Property = Button.BackgroundColorProperty, Value = primaryColor },
                    new Setter { Property = Button.FontAttributesProperty, Value = FontAttributes.Bold },
                    new Setter { Property = Button.BorderRadiusProperty, Value = 0 }
                }
            });

            Resources.Add("btn-primaryAccent", new Style(typeof(Button))
            {
                Setters = {
                    new Setter { Property = Button.TextColorProperty, Value = primaryColorAccent }
                }
            });

            Resources.Add("btn-white", new Style(typeof(Button))
            {
                Setters = {
                    new Setter { Property = Button.BackgroundColorProperty, Value = Color.White },
                    new Setter { Property = Button.TextColorProperty, Value = primaryColor },
                    new Setter { Property = Button.FontAttributesProperty, Value = FontAttributes.Bold },
                    new Setter { Property = Button.BorderRadiusProperty, Value = 0 }
                }
            });

            Resources.Add(new Style(typeof(Button))
            {
                Setters = {
                    new Setter { Property = Button.TextColorProperty, Value = primaryColor }
                }
            });

            // List View

            Resources.Add(new Style(typeof(ListView))
            {
                Setters = {
                    new Setter { Property = ListView.SeparatorColorProperty, Value = grayLighter }
                }
            });
        }
    }
}
