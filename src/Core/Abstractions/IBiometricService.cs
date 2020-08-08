using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public interface IBiometricService
    {
        Task<bool> SetupBiometricAsync();
        Task<bool> ValidateIntegrityAsync();
    }
}
