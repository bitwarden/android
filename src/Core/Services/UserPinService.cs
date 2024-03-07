using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;
using Bit.Core.Services;

namespace Bit.App.Services
{
    public class UserPinService : IUserPinService
    {
        private readonly IStateService _stateService;
        private readonly ICryptoService _cryptoService;
        private readonly IVaultTimeoutService _vaultTimeoutService;

        public UserPinService(IStateService stateService, ICryptoService cryptoService, IVaultTimeoutService vaultTimeoutService)
        {
            _stateService = stateService;
            _cryptoService = cryptoService;
            _vaultTimeoutService = vaultTimeoutService;
        }

        public async Task<bool> IsPinLockEnabledAsync()
        {
            var pinLockType = await _vaultTimeoutService.GetPinLockTypeAsync();

            var ephemeralPinSet = await _stateService.GetPinKeyEncryptedUserKeyEphemeralAsync()
                ?? await _stateService.GetPinProtectedKeyAsync();

            return (pinLockType == PinLockType.Transient && ephemeralPinSet != null)
                ||
                pinLockType == PinLockType.Persistent;
        }

        public async Task SetupPinAsync(string pin, bool requireMasterPasswordOnRestart)
        {
            var kdfConfig = await _stateService.GetActiveUserCustomDataAsync(a => new KdfConfig(a?.Profile));
            var email = await _stateService.GetEmailAsync();
            var pinKey = await _cryptoService.MakePinKeyAsync(pin, email, kdfConfig);
            var userKey = await _cryptoService.GetUserKeyAsync();
            var protectedPinKey = await _cryptoService.EncryptAsync(userKey.Key, pinKey);

            var encPin = await _cryptoService.EncryptAsync(pin);
            await _stateService.SetProtectedPinAsync(encPin.EncryptedString);

            if (requireMasterPasswordOnRestart)
            {
                await _stateService.SetPinKeyEncryptedUserKeyEphemeralAsync(protectedPinKey);
            }
            else
            {
                await _stateService.SetPinKeyEncryptedUserKeyAsync(protectedPinKey);
            }
        }

        public async Task<bool> VerifyPinAsync(string inputPin)
        {
            var (email, kdfConfig) = await _stateService.GetActiveUserCustomDataAsync(a => a?.Profile is null ? (null, default) : (a.Profile.Email, new KdfConfig(a.Profile)));
            if (kdfConfig.Type is null)
            {
                return false;
            }

            return await VerifyPinAsync(inputPin, email, kdfConfig, await _vaultTimeoutService.GetPinLockTypeAsync());
        }

        public async Task<bool> VerifyPinAsync(string inputPin, string email, KdfConfig kdfConfig, PinLockType pinLockType)
        {
            EncString userKeyPin = null;
            EncString oldPinProtected = null;
            if (pinLockType == PinLockType.Persistent)
            {
                userKeyPin = await _stateService.GetPinKeyEncryptedUserKeyAsync();
                var oldEncryptedKey = await _stateService.GetPinProtectedAsync();
                oldPinProtected = oldEncryptedKey != null ? new EncString(oldEncryptedKey) : null;
            }
            else if (pinLockType == PinLockType.Transient)
            {
                userKeyPin = await _stateService.GetPinKeyEncryptedUserKeyEphemeralAsync();
                oldPinProtected = await _stateService.GetPinProtectedKeyAsync();
            }

            UserKey userKey;
            if (oldPinProtected != null)
            {
                userKey = await _cryptoService.DecryptAndMigrateOldPinKeyAsync(
                    pinLockType == PinLockType.Transient,
                    inputPin,
                    email,
                    kdfConfig,
                    oldPinProtected
                );
            }
            else
            {
                userKey = await _cryptoService.DecryptUserKeyWithPinAsync(
                    inputPin,
                    email,
                    kdfConfig,
                    userKeyPin
                );
            }

            var protectedPin = await _stateService.GetProtectedPinAsync();
            var decryptedPin = await _cryptoService.DecryptToUtf8Async(new EncString(protectedPin), userKey);

            return decryptedPin == inputPin;
        }
    }
}
