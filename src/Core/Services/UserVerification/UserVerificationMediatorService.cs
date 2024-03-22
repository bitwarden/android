using Bit.App.Abstractions;
using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;
using Bit.Core.Resources.Localization;
using Bit.Core.Utilities;
using Bit.Core.Utilities.Fido2;
using Plugin.Fingerprint;
using static Bit.Core.Abstractions.IUserVerificationMediatorService;
using FingerprintAvailability = Plugin.Fingerprint.Abstractions.FingerprintAvailability;

namespace Bit.Core.Services.UserVerification
{
    public class UserVerificationMediatorService : IUserVerificationMediatorService
    {
        private const byte MAX_ATTEMPTS = 5;

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

        public async Task<CancellableResult<bool>> VerifyUserForFido2Async(Fido2UserVerificationOptions options)
        {
            if (await ShouldPerformMasterPasswordRepromptAsync(options))
            {
                if (options.OnNeedUITask != null)
                {
                    await options.OnNeedUITask();
                }

                var mpVerification = await VerifyMasterPasswordAsync(true);
                return new CancellableResult<bool>(
                    !mpVerification.IsCancelled && mpVerification.Result.CanPerform && mpVerification.Result.IsVerified, 
                    mpVerification.IsCancelled
                );
            }

            if (!_fido2UserVerificationStrategies.TryGetValue(options.UserVerificationPreference, out var userVerificationServiceStrategy))
            {
                return new CancellableResult<bool>(false, false);
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

        public async Task<bool> ShouldEnforceFido2RequiredUserVerificationAsync(Fido2UserVerificationOptions options)
        {
            switch (options.UserVerificationPreference)
            {
                case Fido2UserVerificationPreference.Required:
                    return true;
                case Fido2UserVerificationPreference.Discouraged:
                    return await ShouldPerformMasterPasswordRepromptAsync(options);
                default:
                    return await CanPerformUserVerificationPreferredAsync(options);
            }
        }

        public async Task<CancellableResult<UVResult>> PerformOSUnlockAsync()
        {
            var availability = await CrossFingerprint.Current.GetAvailabilityAsync();
            if (availability == FingerprintAvailability.Available)
            {
                var isValid = await _platformUtilsService.AuthenticateBiometricAsync(null, DeviceInfo.Platform == DevicePlatform.Android ? "." : null);
                if (!isValid.HasValue)
                {
                    return new UVResult(false, false).AsCancellable(true);
                }
                return new UVResult(true, isValid.Value).AsCancellable();
            }

            var alternativeAuthAvailability = await CrossFingerprint.Current.GetAvailabilityAsync(true);
            if (alternativeAuthAvailability == FingerprintAvailability.Available)
            {
                var isNonBioValid = await _platformUtilsService.AuthenticateBiometricAsync(null, DeviceInfo.Platform == DevicePlatform.Android ? "." : null, allowAlternativeAuthentication: true);
                if (!isNonBioValid.HasValue)
                {
                    return new UVResult(false, false).AsCancellable(true);
                }
                return new UVResult(true, isNonBioValid.Value).AsCancellable();
            }

            return new UVResult(false, false).AsCancellable();
        }

        public async Task<CancellableResult<UVResult>> VerifyPinCodeAsync()
        {
            return await VerifyWithAttemptsAsync(async () =>
            {
                if (!await _userPinService.IsPinLockEnabledAsync())
                {
                    return new UVResult(false, false).AsCancellable();
                }

                var pin = await _deviceActionService.DisplayPromptAync(AppResources.EnterPIN,
                    AppResources.VerifyPIN, null, AppResources.Ok, AppResources.Cancel, password: true);
                if (pin is null)
                {
                    // cancelled by the user
                    return new UVResult(true, false).AsCancellable(true);
                }

                try
                {
                    var isVerified = await _userPinService.VerifyPinAsync(pin);
                    return new UVResult(true, isVerified).AsCancellable();
                }
                catch (SymmetricCryptoKey.ArgumentKeyNullException)
                {
                    return new UVResult(true, false).AsCancellable();
                }
                catch (SymmetricCryptoKey.InvalidKeyOperationException)
                {
                    return new UVResult(true, false).AsCancellable();
                }
            });
        }

        public async Task<CancellableResult<UVResult>> VerifyMasterPasswordAsync(bool isMasterPasswordReprompt)
        {
            return await VerifyWithAttemptsAsync(async () =>
            {
                if (!await _userVerificationService.HasMasterPasswordAsync(true))
                {
                    return new UVResult(false, false).AsCancellable();
                }

                var title = isMasterPasswordReprompt ? AppResources.PasswordConfirmation : AppResources.MasterPassword;
                var body = isMasterPasswordReprompt ? AppResources.PasswordConfirmationDesc : string.Empty;

                var (password, isValid) = await _platformUtilsService.ShowPasswordDialogAndGetItAsync(title, body, _userVerificationService.VerifyMasterPasswordAsync);
                if (password is null)
                {
                    return new UVResult(true, false).AsCancellable(true);
                }

                return new UVResult(true, isValid).AsCancellable();
            });
        }

        private async Task<CancellableResult<UVResult>> VerifyWithAttemptsAsync(Func<Task<CancellableResult<UVResult>>> verifyAsync)
        {
            byte attempts = 0;
            do
            {
                var verification = await verifyAsync();
                if (verification.IsCancelled)
                {
                    return new UVResult(false, false).AsCancellable(true);
                }
                if (!verification.Result.CanPerform)
                {
                    return new UVResult(false, false).AsCancellable();
                }
                if (verification.Result.IsVerified)
                {
                    return new UVResult(true, true).AsCancellable();
                }
            } while (++attempts < MAX_ATTEMPTS);

            return new UVResult(true, false).AsCancellable();
        }
    }

    public static class UVResultExtensions
    {
        public static CancellableResult<UVResult> AsCancellable(this UVResult result, bool isCancelled = false)
        {
            return new CancellableResult<UVResult>(result, isCancelled);
        }
    }
}
