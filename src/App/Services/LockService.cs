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
using System.Diagnostics;

namespace Bit.App.Services
{
    public class LockService : ILockService
    {
        private readonly ISettings _settings;
        private readonly IAppSettingsService _appSettings;
        private readonly IAuthService _authService;
        private readonly IFingerprint _fingerprint;
        private Stopwatch _stopwatch = null;

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

        public void UpdateLastActivity()
        {
            _stopwatch?.Restart();
        }

        public async Task<LockType> GetLockTypeAsync(bool forceLock, bool onlyIfAlreadyLocked = false)
        {
            // Only lock if they are logged in
            if(!_authService.IsAuthenticated)
            {
                return LockType.None;
            }

            // Are we forcing a lock? (i.e. clicking a button to lock the app manually, immediately)
            if(!forceLock && !_appSettings.Locked)
            {
                // Lock seconds tells if they want to lock the app or not
                var lockSeconds = _settings.GetValueOrDefault(Constants.SettingLockSeconds, 60 * 15);
                var neverLock = lockSeconds == -1;

                // Has it been longer than lockSeconds since the last time the app was used?
                if(neverLock || (_stopwatch != null && _stopwatch.Elapsed.TotalSeconds < lockSeconds))
                {
                    return LockType.None;
                }
            }

            if(onlyIfAlreadyLocked && !_appSettings.Locked)
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

        public async Task CheckLockAsync(bool forceLock, bool onlyIfAlreadyLocked = false)
        {
            if(TopPageIsLock())
            {
                return;
            }

            var lockType = await GetLockTypeAsync(forceLock, onlyIfAlreadyLocked);
            if(lockType == LockType.None)
            {
                return;
            }

            if(_stopwatch == null)
            {
                _stopwatch = Stopwatch.StartNew();
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
    }
}
