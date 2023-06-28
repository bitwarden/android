
using System;
using System.Threading.Tasks;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;

namespace Bit.Core.Abstractions
{
    public class DeviceCryptoService
    {
        private readonly IApiService _apiService;
        private readonly IAppIdService _appIdService;
        private readonly ICryptoFunctionService _cryptoFunctionService;
        private readonly ICryptoService _cryptoService;
        private readonly IStateService _stateService;

        public DeviceCryptoService(
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


        public async Task<DeviceResponse> TrustDeviceAsync()
        {
            // Attempt to get user key
            var userKey = await _cryptoService.GetEncKeyAsync();
            if (userKey == null)
            {
                return null;
            }
            // Generate deviceKey
            var deviceKey = await this.MakeDeviceKeyAsync();

            // Generate asymmetric RSA key pair: devicePrivateKey, devicePublicKey
            var deviceKeyPair = await _cryptoFunctionService.RsaGenerateKeyPairAsync(2048);

            var encryptUserKeyTask = _cryptoService.RsaEncryptAsync(userKey.EncKey, deviceKeyPair.Item1);
            var encryptPublicKeyTask = _cryptoService.EncryptAsync(deviceKeyPair.Item1, userKey);
            var encryptPrivateKeyTask = _cryptoService.EncryptAsync(deviceKeyPair.Item2, deviceKey);

            // Send encrypted keys to server
            var deviceIdentifier = await _appIdService.GetAppIdAsync();
            var deviceRequest = new TrustedDeviceKeysRequest()
            {
                EncryptedUserKey = (await encryptUserKeyTask).EncryptedString,
                EncryptedPublicKey = (await encryptPublicKeyTask).EncryptedString,
                EncryptedPrivateKey = (await encryptPrivateKeyTask).EncryptedString,
            };
            return await _apiService.UpdateTrustedDeviceKeysAsync(deviceIdentifier, deviceRequest);
        }


        public async Task<SymmetricCryptoKey> GetDeviceKeyAsync()
        {
            // Check if device key already exists
            var deviceKey = await this._stateService.GetDeviceKeyAsync();

            return deviceKey != null
                ? deviceKey
                : await this.MakeDeviceKeyAsync();
        }


        private async Task<SymmetricCryptoKey> MakeDeviceKeyAsync()
        {
            // Create 512-bit device key
            var randomBytes = await _cryptoFunctionService.RandomBytesAsync(64);
            var deviceKey = new SymmetricCryptoKey(randomBytes);

            // Save new device key
            await this._stateService.SetDeviceKeyAsync(deviceKey);
            return deviceKey;

        }
    }
}
