using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public interface IBiometricService
    {
        Task<bool> CanUseBiometricsUnlockAsync();
        Task<bool> SetupBiometricAsync(string bioIntegritySrcKey = null);
        Task<bool> IsSystemBiometricIntegrityValidAsync(string bioIntegritySrcKey = null);
        Task SetCanUnlockWithBiometricsAsync(bool canUnlockWithBiometrics);
    }
}
