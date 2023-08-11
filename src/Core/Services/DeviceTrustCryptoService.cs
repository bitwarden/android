
using System;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;

namespace Bit.Core.Services
{
    public class DeviceTrustCryptoService : IDeviceTrustCryptoService
    {
        private readonly IApiService _apiService;
        private readonly IAppIdService _appIdService;
        private readonly ICryptoFunctionService _cryptoFunctionService;
        private readonly ICryptoService _cryptoService;
        private readonly IStateService _stateService;

        private const int DEVICE_KEY_SIZE = 64;

        public DeviceTrustCryptoService(
            IApiService apiService,
            IAppIdService appIdService,
            ICryptoFunctionService cryptoFunctionService,
            ICryptoService cryptoService,
            IStateService stateService)
        {
            _apiService = apiService;
            _appIdService = appIdService;
            _cryptoFunctionService = cryptoFunctionService;
            _cryptoService = cryptoService;
            _stateService = stateService;
        }

        public async Task<SymmetricCryptoKey> GetDeviceKeyAsync()
        {
            return await _stateService.GetDeviceKeyAsync();
        }

        private async Task SetDeviceKeyAsync(SymmetricCryptoKey deviceKey)
        {
            await _stateService.SetDeviceKeyAsync(deviceKey);
        }

        public async Task RemoveTrustedDeviceAsync()
        {
            await SetDeviceKeyAsync(null);
        }

        public async Task<DeviceResponse> TrustDeviceAsync()
        {
            // Attempt to get user key
            var userKey = await _cryptoService.GetUserKeyAsync();
            if (userKey == null)
            {
                return null;
            }
            // Generate deviceKey
            var deviceKey = await MakeDeviceKeyAsync();

            // Generate asymmetric RSA key pair: devicePrivateKey, devicePublicKey
            var (devicePublicKey, devicePrivateKey) = await _cryptoFunctionService.RsaGenerateKeyPairAsync(2048);

            // Send encrypted keys to server
            var deviceIdentifier = await _appIdService.GetAppIdAsync();
            var deviceRequest = new TrustedDeviceKeysRequest
            {
                EncryptedUserKey = (await _cryptoService.RsaEncryptAsync(userKey.Key, devicePublicKey)).EncryptedString,
                EncryptedPublicKey = (await _cryptoService.EncryptAsync(devicePublicKey, userKey)).EncryptedString,
                EncryptedPrivateKey = (await _cryptoService.EncryptAsync(devicePrivateKey, deviceKey)).EncryptedString,
            };

            var deviceResponse = await _apiService.UpdateTrustedDeviceKeysAsync(deviceIdentifier, deviceRequest);

            // Store device key if successful
            await SetDeviceKeyAsync(deviceKey);
            return deviceResponse;
        }

        private async Task<SymmetricCryptoKey> MakeDeviceKeyAsync()
        {
            // Create 512-bit device key
            var randomBytes = await _cryptoFunctionService.RandomBytesAsync(DEVICE_KEY_SIZE);
            return new SymmetricCryptoKey(randomBytes);
        }

        public async Task<bool> GetShouldTrustDeviceAsync()
        {
            return await _stateService.GetShouldTrustDeviceAsync();
        }

        public async Task SetShouldTrustDeviceAsync(bool value)
        {
            await _stateService.SetShouldTrustDeviceAsync(value);
        }

        public async Task<DeviceResponse> TrustDeviceIfNeededAsync()
        {
            if (!await GetShouldTrustDeviceAsync())
            {
                return null;
            }

            var response = await TrustDeviceAsync();
            await SetShouldTrustDeviceAsync(false);
            return response;
        }

        public async Task<bool> IsDeviceTrustedAsync()
        {
            var existingDeviceKey = await GetDeviceKeyAsync();
            return existingDeviceKey != null;
        }

        public async Task<UserKey> DecryptUserKeyWithDeviceKeyAsync(string encryptedDevicePrivateKey, string encryptedUserKey)
        {
            var existingDeviceKey = await GetDeviceKeyAsync();
            if (existingDeviceKey == null)
            {
                // User doesn't have a device key anymore so device is untrusted
                return null;
            }

            // Attempt to decrypt encryptedDevicePrivateKey with device key
            var devicePrivateKeyBytes = await _cryptoService.DecryptToBytesAsync(
              new EncString(encryptedDevicePrivateKey),
              existingDeviceKey
            );

            // Attempt to decrypt encryptedUserDataKey with devicePrivateKey
            var userKeyBytes = await _cryptoService.RsaDecryptAsync(encryptedUserKey, devicePrivateKeyBytes);
            return new UserKey(userKeyBytes);
        }
    }
}
