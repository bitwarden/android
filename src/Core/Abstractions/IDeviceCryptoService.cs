using System.Threading.Tasks;
using Bit.Core.Models.Domain;

namespace Bit.Core.Abstractions
{
    public interface IDeviceCryptoService
    {
        Task<DeviceResponse> TrustDeviceAsync();
        Task<SymmetricCryptoKey> GetDeviceKeyAsync();
    }
}
