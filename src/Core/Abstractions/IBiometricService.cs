using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public interface IBiometricService
    {
        Task<bool> SetupBiometricAsync(string bioIntegrityKey = null);
        Task<bool> ValidateIntegrityAsync(string bioIntegrityKey = null);
    }
}
