using Bit.App.Abstractions;
using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;
using Bit.Core.Resources.Localization;
using Plugin.Fingerprint;
using FingerprintAvailability = Plugin.Fingerprint.Abstractions.FingerprintAvailability;

namespace Bit.Core.Services
{
    public class UserVerificationMediatorService : IUserVerificationMediatorService
    {
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IPasswordRepromptService _passwordRepromptService;
        private readonly IUserPinService _userPinService;
        private readonly IDeviceActionService _deviceActionService;
        private readonly IUserVerificationService _userVerificationService;

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
        }

        public async Task<bool> VerifyUserForFido2Async(Fido2VerificationOptions options)
        {
            // Master Password Reprompt if enabled and shouldn't be bypassed (because of TDE)
            if (options.ShouldCheckMasterPasswordReprompt && !await _passwordRepromptService.ShouldByPassMasterPasswordRepromptAsync())
            {
                options.OnNeedUI?.Invoke();
                return await _passwordRepromptService.PromptAndCheckPasswordIfNeededAsync(Enums.CipherRepromptType.Password);
            }

            if (options.IsUserVerificationRequired)
            {
                return await new Fido2UserVerificationRequiredServiceStrategy(this, _platformUtilsService).VerifyUserForFido2Async(options);
            }

            return await new Fido2UserVerificationPreferredServiceStrategy(this).VerifyUserForFido2Async(options);
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

    public readonly struct Fido2VerificationOptions
    {
        public Fido2VerificationOptions(bool shouldCheckMasterPasswordReprompt,
            bool isUserVerificationRequired,
            bool hasVaultBeenUnlockedInTransaction,
            string rpId,
            Action onNeedUI = null)
        {
            ShouldCheckMasterPasswordReprompt = shouldCheckMasterPasswordReprompt;
            IsUserVerificationRequired = isUserVerificationRequired;
            HasVaultBeenUnlockedInTransaction = hasVaultBeenUnlockedInTransaction;
            RpId = rpId;
            OnNeedUI = onNeedUI;
        }

        public bool ShouldCheckMasterPasswordReprompt { get; }
        public bool IsUserVerificationRequired { get; }
        public bool HasVaultBeenUnlockedInTransaction { get; }
        public string RpId { get; }
        public Action OnNeedUI { get; }
    }

    public interface IUserVerificationServiceStrategy
    {
        Task<bool> VerifyUserForFido2Async(Fido2VerificationOptions options);
    }

    public class Fido2UserVerificationRequiredServiceStrategy : IUserVerificationServiceStrategy
    {
        private readonly IUserVerificationMediatorService _userVerificationMediatorService;
        private readonly IPlatformUtilsService _platformUtilsService;

        public Fido2UserVerificationRequiredServiceStrategy(IUserVerificationMediatorService userVerificationMediatorService,
            IPlatformUtilsService platformUtilsService)
        {
            _userVerificationMediatorService = userVerificationMediatorService;
            _platformUtilsService = platformUtilsService;
        }

        public async Task<bool> VerifyUserForFido2Async(Fido2VerificationOptions options)
        {
            if (options.HasVaultBeenUnlockedInTransaction)
            {
                return true;
            }

            options.OnNeedUI?.Invoke();

            var (canPerformOSUnlock, isOSUnlocked) = await _userVerificationMediatorService.PerformOSUnlockAsync();
            if (canPerformOSUnlock)
            {
                return isOSUnlocked;
            }

            var (canPerformUnlockWithPin, pinVerified) = await _userVerificationMediatorService.VerifyPinCodeAsync();
            if (canPerformUnlockWithPin)
            {
                return pinVerified;
            }

            var (canPerformUnlockWithMasterPassword, mpVerified) = await _userVerificationMediatorService.VerifyMasterPasswordAsync();
            if (canPerformUnlockWithMasterPassword)
            {
                return mpVerified;
            }

            // TODO: Setup PIN code. For the sake of simplicity, we're not implementing this step now and just telling the user to do it in the main app.

            await _platformUtilsService.ShowDialogAsync(AppResources.VerificationRequiredForThisActionSetUpAnUnlockMethodInBitwardenToContinue,
                string.Format(AppResources.VerificationRequiredByX, options.RpId),
                AppResources.Ok);

            return false;
        }
    }

    public class Fido2UserVerificationPreferredServiceStrategy : IUserVerificationServiceStrategy
    {
        private readonly IUserVerificationMediatorService _userVerificationMediatorService;

        public Fido2UserVerificationPreferredServiceStrategy(IUserVerificationMediatorService userVerificationMediatorService)
        {
            _userVerificationMediatorService = userVerificationMediatorService;
        }

        public async Task<bool> VerifyUserForFido2Async(Fido2VerificationOptions options)
        {
            if (options.HasVaultBeenUnlockedInTransaction)
            {
                return true;
            }

            options.OnNeedUI?.Invoke();

            var (canPerformOSUnlock, isOSUnlocked) = await _userVerificationMediatorService.PerformOSUnlockAsync();
            if (canPerformOSUnlock)
            {
                return isOSUnlocked;
            }

            return false;
        }
    }
}
