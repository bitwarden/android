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

            if(authService.IsAuthenticated)
            {
                MainPage = new MainPage();
            }
            else
            {
                MainPage = new LoginNavigationPage();
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
            else if(pinUnlock)
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
    }
}
