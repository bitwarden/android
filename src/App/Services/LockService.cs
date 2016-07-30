using System;
using Bit.App.Abstractions;
using Plugin.Settings.Abstractions;
using Plugin.Fingerprint.Abstractions;
using Bit.App.Enums;

namespace Bit.App.Services
{
    public class LockService : ILockService
    {
        private readonly ISettings _settings;
        private readonly IAuthService _authService;
        private readonly IFingerprint _fingerprint;

        public LockService(
            ISettings settings,
            IAuthService authService,
            IFingerprint fingerprint)
        {
            _settings = settings;
            _authService = authService;
            _fingerprint = fingerprint;
        }

        public LockType GetLockType(bool forceLock)
        {
            // Only lock if they are logged in
            if(!_authService.IsAuthenticated)
            {
                return LockType.None;
            }

            // Are we forcing a lock? (i.e. clicking a button to lock the app manually, immediately)
            if(!forceLock && !_settings.GetValueOrDefault(Constants.SettingLocked, false))
            {
                // Lock seconds tells if they want to lock the app or not
                var lockSeconds = _settings.GetValueOrDefault(Constants.SettingLockSeconds, 60 * 15);
                if(lockSeconds == -1)
                {
                    return LockType.None;
                }

                // Has it been longer than lockSeconds since the last time the app was backgrounded?
                var now = DateTime.Now;
                var lastBackground = _settings.GetValueOrDefault(Constants.SettingLastBackgroundedDate, now.AddYears(-1));
                if((now - lastBackground).TotalSeconds < lockSeconds)
                {
                    return LockType.None;
                }
            }

            // What method are we using to unlock?
            var fingerprintUnlock = _settings.GetValueOrDefault<bool>(Constants.SettingFingerprintUnlockOn);
            var pinUnlock = _settings.GetValueOrDefault<bool>(Constants.SettingPinUnlockOn);
            if(fingerprintUnlock && _fingerprint.IsAvailable)
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
    }
}
