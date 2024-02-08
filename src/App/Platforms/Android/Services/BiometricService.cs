using System;
using System.Threading.Tasks;
using Android.OS;
using Android.Security.Keystore;
using Bit.App.Services;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Java.Security;
using Javax.Crypto;

namespace Bit.Droid.Services
{
    public class BiometricService : BaseBiometricService
    {
        private const string KeyName = "com.8bit.bitwarden.biometric_integrity";

        private const string KeyStoreName = "AndroidKeyStore";

        private const string KeyAlgorithm = KeyProperties.KeyAlgorithmAes;
        private const string BlockMode = KeyProperties.BlockModeCbc;
        private const string EncryptionPadding = KeyProperties.EncryptionPaddingPkcs7;
        private const string Transformation = KeyAlgorithm + "/" + BlockMode + "/" + EncryptionPadding;

        private readonly KeyStore _keystore;

        public BiometricService(IStateService stateService, ICryptoService cryptoService)
            : base(stateService, cryptoService)
        {
            _keystore = KeyStore.GetInstance(KeyStoreName);
            _keystore.Load(null);
        }

        public override async Task<bool> SetupBiometricAsync(string bioIntegritySrcKey = null)
        {
            if (Build.VERSION.SdkInt >= BuildVersionCodes.M)
            {
                await CreateKeyAsync(bioIntegritySrcKey);
            }

            return true;
        }

        public override async Task<bool> IsSystemBiometricIntegrityValidAsync(string bioIntegritySrcKey = null)
        {
            if (Build.VERSION.SdkInt < BuildVersionCodes.M)
            {
                return true;
            }

            try
            {
                _keystore.Load(null);
                var key = _keystore.GetKey(KeyName, null);
                var cipher = Cipher.GetInstance(Transformation);

                if (key == null || cipher == null)
                {
                    return true;
                }

                cipher.Init(CipherMode.EncryptMode, key);
            }
            catch (KeyPermanentlyInvalidatedException e)
            {
                // Biometric has changed
                await ClearStateAsync(bioIntegritySrcKey);
                return false;
            }
            catch (UnrecoverableKeyException e)
            {
                // Biometric was disabled and re-enabled
                await ClearStateAsync(bioIntegritySrcKey);
                return false;
            }
            catch (InvalidKeyException e)
            {
                // Fallback for old bitwarden users without a key
                LoggerHelper.LogEvenIfCantBeResolved(e);
                await CreateKeyAsync(bioIntegritySrcKey);
            }

            return true;
        }

        private async Task CreateKeyAsync(string bioIntegritySrcKey = null)
        {
            bioIntegritySrcKey ??= Core.Constants.BiometricIntegritySourceKey;
            await _stateService.SetSystemBiometricIntegrityState(bioIntegritySrcKey,
                await GetStateAsync(bioIntegritySrcKey));
            await _stateService.SetAccountBiometricIntegrityValidAsync(bioIntegritySrcKey);

            try
            {
                var keyGen = KeyGenerator.GetInstance(KeyAlgorithm, KeyStoreName);
                var keyGenSpec =
                    new KeyGenParameterSpec.Builder(KeyName, KeyStorePurpose.Encrypt | KeyStorePurpose.Decrypt)
                        .SetBlockModes(BlockMode)
                        .SetEncryptionPaddings(EncryptionPadding)
                        .SetUserAuthenticationRequired(true)
                        .Build();
                keyGen.Init(keyGenSpec);
                keyGen.GenerateKey();
            }
            catch (Exception e)
            {
                // Catch silently to allow biometrics to function on devices that are in a state where key generation
                // is not functioning
                LoggerHelper.LogEvenIfCantBeResolved(e);
            }
        }

        private async Task<string> GetStateAsync(string bioIntegritySrcKey)
        {
            return await _stateService.GetSystemBiometricIntegrityState(bioIntegritySrcKey) ??
                   Guid.NewGuid().ToString();
        }

        private async Task ClearStateAsync(string bioIntegritySrcKey)
        {
            await _stateService.SetSystemBiometricIntegrityState(bioIntegritySrcKey, null);
        }
    }
}
