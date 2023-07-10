using System.Threading.Tasks;
using Bit.Core.Models.Domain;

namespace Bit.Core.Abstractions
{
    public interface IDeviceTrustCryptoService
    {
        Task<SymmetricCryptoKey> GetDeviceKeyAsync();
        Task<DeviceResponse> TrustDeviceAsync();
        Task<bool> GetUserTrustDeviceChoiceForDecryptionAsync();
        Task SetUserTrustDeviceChoiceForDecryptionAsync(bool value);
    }
}
