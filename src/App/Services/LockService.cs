using System;
using Bit.App.Abstractions;
using Plugin.Settings.Abstractions;
using Plugin.Fingerprint.Abstractions;
using Bit.App.Enums;
using System.Threading.Tasks;
using Bit.App.Controls;
using Bit.App.Pages;
using Xamarin.Forms;
using System.Linq;

namespace Bit.App.Services
{
    public class LockService : ILockService
    {
        private readonly ISettings _settings;
        private readonly IAppSettingsService _appSettings;
        private readonly IAuthService _authService;
        private readonly IFingerprint _fingerprint;
        private string _timerId = null;

        public LockService(
            ISettings settings,
            IAppSettingsService appSettings,
            IAuthService authService,
            IFingerprint fingerprint)
        {
            _settings = settings;
            _appSettings = appSettings;
            _authService = authService;
            _fingerprint = fingerprint;
        }

        public double CurrentLockTime { get; set; }

        public void UpdateLastActivity()
        {
            if(_appSettings.Locked)
            {
                return;
            }

            _appSettings.LastActivityLockTime = CurrentLockTime;
        }

        public async Task<LockType> GetLockTypeAsync(bool forceLock)
        {
            var returnNone = false;

            // Only lock if they are logged in
            if(!_authService.IsAuthenticated)
            {
                returnNone = true;
            }
            // Are we forcing a lock? (i.e. clicking a button to lock the app manually, immediately)
            else if(!forceLock && !_appSettings.Locked)
            {
                // Lock seconds tells if they want to lock the app or not
                var lockSeconds = _settings.GetValueOrDefault(Constants.SettingLockSeconds, 60 * 15);
                if(lockSeconds == -1)
                {
                    returnNone = true;
                }
                // Validate timer instance
                else if(_appSettings.LockTimerId != null && _timerId == _appSettings.LockTimerId)
                {
                    // Has it been longer than lockSeconds since the last time the app was used?
                    var now = CurrentLockTime;
                    var elapsedSeconds = (now - _appSettings.LastActivityLockTime) / 1000;
                    if(now >= _appSettings.LastActivityLockTime && elapsedSeconds < lockSeconds)
                    {
                        returnNone = true;
                    }
                }
            }

            // Set the new lock timer id
            if(_timerId != null)
            {
                _appSettings.LockTimerId = _timerId;
            }

            if(returnNone)
            {
                return LockType.None;
            }

            // What method are we using to unlock?
            var fingerprintUnlock = _settings.GetValueOrDefault(Constants.SettingFingerprintUnlockOn, false);
            var pinUnlock = _settings.GetValueOrDefault(Constants.SettingPinUnlockOn, false);
            var fingerprintAvailability = await _fingerprint.GetAvailabilityAsync();
            if(fingerprintUnlock && fingerprintAvailability == FingerprintAvailability.Available)
            {
                return LockType.Fingerprint;
            }
            else if(pinUnlock && !string.IsNullOrWhiteSpace(_authService.PIN))
            {
                return LockType.PIN;
            }
            else
            {
                return LockType.Password;
            }
        }

        public async Task CheckLockAsync(bool forceLock)
        {
            if(TopPageIsLock())
            {
                return;
            }

            var lockType = await GetLockTypeAsync(forceLock);
            if(lockType == LockType.None)
            {
                return;
            }

            _appSettings.Locked = true;
            switch(lockType)
            {
                case LockType.Fingerprint:
                    await Application.Current.MainPage.Navigation.PushModalAsync(
                        new ExtendedNavigationPage(new LockFingerprintPage(!forceLock)), false);
                    break;
                case LockType.PIN:
                    await Application.Current.MainPage.Navigation.PushModalAsync(
                        new ExtendedNavigationPage(new LockPinPage()), false);
                    break;
                case LockType.Password:
                    await Application.Current.MainPage.Navigation.PushModalAsync(
                        new ExtendedNavigationPage(new LockPasswordPage()), false);
                    break;
                default:
                    break;
            }
        }

        public bool TopPageIsLock()
        {
            var currentPage = Application.Current.MainPage.Navigation.ModalStack.LastOrDefault() as ExtendedNavigationPage;
            if((currentPage?.CurrentPage as LockFingerprintPage) != null)
            {
                return true;
            }
            if((currentPage?.CurrentPage as LockPinPage) != null)
            {
                return true;
            }
            if((currentPage?.CurrentPage as LockPasswordPage) != null)
            {
                return true;
            }

            return false;
        }

        public void StartLockTimer()
        {
            if(_timerId != null)
            {
                return;
            }

            _timerId = Guid.NewGuid().ToString();
            var interval = TimeSpan.FromSeconds(10);
            Device.StartTimer(interval, () =>
            {
                CurrentLockTime += interval.TotalMilliseconds;
                return true;
            });
        }
    }
}
