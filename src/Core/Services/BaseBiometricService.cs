using System.Threading.Tasks;
using Bit.Core.Abstractions;

namespace Bit.App.Services
{
    public abstract class BaseBiometricService : IBiometricService
    {
        protected readonly IStateService _stateService;
        protected readonly ICryptoService _cryptoService;

        protected BaseBiometricService(IStateService stateService, ICryptoService cryptoService)
        {
            _stateService = stateService;
            _cryptoService = cryptoService;
        }

        public async Task<bool> CanUseBiometricsUnlockAsync()
        {
#pragma warning disable CS0618 // Type or member is obsolete
            return await _cryptoService.GetBiometricUnlockKeyAsync() != null || await _stateService.GetKeyEncryptedAsync() != null;
#pragma warning restore CS0618 // Type or member is obsolete
        }

        public async Task SetCanUnlockWithBiometricsAsync(bool canUnlockWithBiometrics)
        {
            if (canUnlockWithBiometrics)
            {
                await SetupBiometricAsync();
                await _stateService.SetBiometricUnlockAsync(true);
            }
            else
            {
                await _stateService.SetBiometricUnlockAsync(null);
            }
            await _stateService.SetBiometricLockedAsync(false);
            await _cryptoService.RefreshKeysAsync();
        }

        public abstract Task<bool> IsSystemBiometricIntegrityValidAsync(string bioIntegritySrcKey = null);
        public abstract Task<bool> SetupBiometricAsync(string bioIntegritySrcKey = null);
    }
}
