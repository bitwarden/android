using System.Threading.Tasks;
using Bit.Core.Models.Domain;

namespace Bit.Core.Abstractions
{
    public interface IDeviceTrustCryptoService
    {
        Task<SymmetricCryptoKey> GetDeviceKeyAsync();
        Task<DeviceResponse> TrustDeviceAsync();
        Task<DeviceResponse> TrustDeviceIfNeededAsync();
        Task RemoveTrustedDeviceAsync();
        Task<bool> GetShouldTrustDeviceAsync();
        Task SetShouldTrustDeviceAsync(bool value);
        Task<UserKey> DecryptUserKeyWithDeviceKeyAsync(string encryptedDevicePrivateKey, string encryptedUserKey);
        Task<bool> IsDeviceTrustedAsync();
    }
}
