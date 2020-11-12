using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Android.OS;
using Android.Security.Keystore;
using Bit.Core.Abstractions;
using Java.Security;
using Javax.Crypto;

namespace Bit.Droid.Services
{
    public class BiometricService : IBiometricService
    {
        private const string KeyName = "com.8bit.bitwarden.biometric_integrity";

        private const string KeyStoreName = "AndroidKeyStore";

        private const string KeyAlgorithm = KeyProperties.KeyAlgorithmAes;
        private const string BlockMode = KeyProperties.BlockModeCbc;
        private const string EncryptionPadding = KeyProperties.EncryptionPaddingPkcs7;
        private const string Transformation = KeyAlgorithm + "/" + BlockMode + "/" + EncryptionPadding;

        private readonly KeyStore _keystore;

        public BiometricService()
        {
            _keystore = KeyStore.GetInstance(KeyStoreName);
            _keystore.Load(null);
        }

        public Task<bool> SetupBiometricAsync(string bioIntegrityKey = null)
        {
            // bioIntegrityKey used in iOS only
            if (Build.VERSION.SdkInt >= BuildVersionCodes.M)
            {
                CreateKey();
            }

            return Task.FromResult(true);
        }

        public Task<bool> ValidateIntegrityAsync(string bioIntegrityKey = null)
        {
            // bioIntegrityKey used in iOS only
            if (Build.VERSION.SdkInt < BuildVersionCodes.M)
            {
                return Task.FromResult(true);
            }

            _keystore.Load(null);
            IKey key = _keystore.GetKey(KeyName, null);
            Cipher cipher = Cipher.GetInstance(Transformation);

            if (key == null || cipher == null)
            {
                return Task.FromResult(true);
            }

            try
            {
                cipher.Init(CipherMode.EncryptMode, key);
            }
            catch (KeyPermanentlyInvalidatedException e)
            {
                // Biometric has changed
                return Task.FromResult(false);
            }
            catch (UnrecoverableKeyException e)
            {
                // Biometric was disabled and re-enabled
                return Task.FromResult(false);
            }
            catch (InvalidKeyException e)
            {
                // Fallback for old bitwarden users without a key
                CreateKey();
            }

            return Task.FromResult(true);
        }

        private void CreateKey()
        {
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
            catch
            {
                // Catch silently to allow biometrics to function on devices that are in a state where key generation
                // is not functioning
            }
        }
    }
}
