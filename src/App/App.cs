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
        private readonly IAuthService _authService;
        private readonly IFingerprint _fingerprint;
        private readonly ISettings _settings;

        public App(
            IAuthService authService,
            IDatabaseService databaseService,
            IFingerprint fingerprint,
            ISettings settings)
        {
            _databaseService = databaseService;
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

            MainPage.BackgroundColor = Color.FromHex("ecf0f5");

            MessagingCenter.Subscribe<Application, bool>(Current, "Lock", async (sender, args) =>
            {
                await CheckLockAsync(args);
            });
        }

        protected override void OnStart()
        {
            // Handle when your app starts
            CheckLockAsync(false);
            _databaseService.CreateTables();

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
                CheckLockAsync(false);
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
            if(fingerprintUnlock && _fingerprint.IsAvailable)
            {
                if(Current.MainPage.Navigation.ModalStack.LastOrDefault() as LockFingerprintPage == null)
                {
                    await Current.MainPage.Navigation.PushModalAsync(new LockFingerprintPage(!forceLock), false);
                }
            }
            else if(pinUnlock && !string.IsNullOrWhiteSpace(_authService.PIN))
            {
                if(Current.MainPage.Navigation.ModalStack.LastOrDefault() as LockPinPage == null)
                {
                    await Current.MainPage.Navigation.PushModalAsync(new LockPinPage(), false);
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

            Resources = new ResourceDictionary();

            // Labels

            Resources.Add(new Style(typeof(Label))
            {
                Setters = {
                    new Setter { Property = Label.TextColorProperty, Value = gray }
                }
            });

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

            Resources.Add(new Style(typeof(Button))
            {
                Setters = {
                    new Setter { Property = Button.TextColorProperty, Value = primaryColor }
                }
            });

            // Editors

            Resources.Add(new Style(typeof(Editor))
            {
                Setters = {
                    new Setter { Property = Editor.TextColorProperty, Value = gray }
                }
            });

            // Entries

            Resources.Add(new Style(typeof(Entry))
            {
                Setters = {
                    new Setter { Property = Entry.TextColorProperty, Value = gray }
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
