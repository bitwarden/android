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
            return await _cryptoService.GetBiometricUnlockKeyAsync() != null || await _stateService.GetKeyEncryptedAsync() != null;
        }

        public abstract Task<bool> IsSystemBiometricIntegrityValidAsync(string bioIntegritySrcKey = null);
        public abstract Task<bool> SetupBiometricAsync(string bioIntegritySrcKey = null);
    }
}
