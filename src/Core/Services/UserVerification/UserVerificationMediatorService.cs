using Bit.App.Abstractions;
using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;
using Bit.Core.Resources.Localization;
using Bit.Core.Utilities.Fido2;
using Plugin.Fingerprint;
using FingerprintAvailability = Plugin.Fingerprint.Abstractions.FingerprintAvailability;

namespace Bit.Core.Services.UserVerification
{
    public class UserVerificationMediatorService : IUserVerificationMediatorService
    {
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IPasswordRepromptService _passwordRepromptService;
        private readonly IUserPinService _userPinService;
        private readonly IDeviceActionService _deviceActionService;
        private readonly IUserVerificationService _userVerificationService;

        private readonly Dictionary<Fido2UserVerificationPreference, IUserVerificationServiceStrategy> _fido2UserVerificationStrategies = new Dictionary<Fido2UserVerificationPreference, IUserVerificationServiceStrategy>();

        public UserVerificationMediatorService(
            IPlatformUtilsService platformUtilsService,
            IPasswordRepromptService passwordRepromptService,
            IUserPinService userPinService,
            IDeviceActionService deviceActionService,
            IUserVerificationService userVerificationService)
        {
            _platformUtilsService = platformUtilsService;
            _passwordRepromptService = passwordRepromptService;
            _userPinService = userPinService;
            _deviceActionService = deviceActionService;
            _userVerificationService = userVerificationService;

            _fido2UserVerificationStrategies.Add(Fido2UserVerificationPreference.Required, new Fido2UserVerificationRequiredServiceStrategy(this, _platformUtilsService));
            _fido2UserVerificationStrategies.Add(Fido2UserVerificationPreference.Preferred, new Fido2UserVerificationPreferredServiceStrategy(this));
        }

        public async Task<bool> VerifyUserForFido2Async(Fido2UserVerificationOptions options)
        {
            if (await ShouldPerformMasterPasswordRepromptAsync(options))
            {
                if (options.OnNeedUITask != null)
                {
                    await options.OnNeedUITask();
                }

                return await _passwordRepromptService.PromptAndCheckPasswordIfNeededAsync(Enums.CipherRepromptType.Password);
            }

            if (!_fido2UserVerificationStrategies.TryGetValue(options.UserVerificationPreference, out var userVerificationServiceStrategy))
            {
                return false;
            }

            return await userVerificationServiceStrategy.VerifyUserForFido2Async(options);
        }

        public async Task<bool> CanPerformUserVerificationPreferredAsync(Fido2UserVerificationOptions options)
        {
            if (await ShouldPerformMasterPasswordRepromptAsync(options))
            {
                return true;
            }

            return options.HasVaultBeenUnlockedInTransaction
                   ||
                   await CrossFingerprint.Current.GetAvailabilityAsync() == FingerprintAvailability.Available
                   ||
                   await CrossFingerprint.Current.GetAvailabilityAsync(true) == FingerprintAvailability.Available;
        }

        public async Task<bool> ShouldPerformMasterPasswordRepromptAsync(Fido2UserVerificationOptions options)
        {
            return options.ShouldCheckMasterPasswordReprompt && !await _passwordRepromptService.ShouldByPassMasterPasswordRepromptAsync();
        }

        public async Task<(bool CanPerfom, bool IsUnlocked)> PerformOSUnlockAsync()
        {
            var availability = await CrossFingerprint.Current.GetAvailabilityAsync();
            if (availability == FingerprintAvailability.Available)
            {
                var isValid = await _platformUtilsService.AuthenticateBiometricAsync(null, DeviceInfo.Platform == DevicePlatform.Android ? "." : null);
                return (true, isValid);
            }

            var alternativeAuthAvailability = await CrossFingerprint.Current.GetAvailabilityAsync(true);
            if (alternativeAuthAvailability == FingerprintAvailability.Available)
            {
                var isNonBioValid = await _platformUtilsService.AuthenticateBiometricAsync(null, DeviceInfo.Platform == DevicePlatform.Android ? "." : null, allowAlternativeAuthentication: true);
                return (true, isNonBioValid);
            }

            return (false, false);
        }

        public async Task<(bool canPerformUnlockWithPin, bool pinVerified)> VerifyPinCodeAsync()
        {
            if (!await _userPinService.IsPinLockEnabledAsync())
            {
                return (false, false);
            }

            var pin = await _deviceActionService.DisplayPromptAync(AppResources.EnterPIN,
                AppResources.VerifyPIN, null, AppResources.Ok, AppResources.Cancel, password: true);
            if (pin is null)
            {
                // cancelled by the user
                return (true, false); 
            }

            try
            {
                var isVerified = await _userPinService.VerifyPinAsync(pin);
                return (true, isVerified);
            }
            catch (SymmetricCryptoKey.ArgumentKeyNullException)
            {
                return (true, false);
            }
            catch (SymmetricCryptoKey.InvalidKeyOperationException)
            {
                return (true, false);
            }
        }

        public async Task<(bool canPerformUnlockWithMasterPassword, bool mpVerified)> VerifyMasterPasswordAsync()
        {
            if (!await _userVerificationService.HasMasterPasswordAsync(true))
            {
                return (false, false);
            }

            var (_, isValid) = await _platformUtilsService.ShowPasswordDialogAndGetItAsync(AppResources.MasterPassword, string.Empty, _userVerificationService.VerifyMasterPasswordAsync);
            return (true, isValid);
        }
    }
}
