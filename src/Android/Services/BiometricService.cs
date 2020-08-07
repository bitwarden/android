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

        public async Task<bool> SetupBiometric()
        {
            if (Build.VERSION.SdkInt >= BuildVersionCodes.M)
            {
                CreateKey();
            }

            return true;
        }

        public async Task<bool> ValidateIntegrity()
        {
            if (Build.VERSION.SdkInt < BuildVersionCodes.M)
            {
                return true;
            }

            _keystore.Load(null);
            IKey key = _keystore.GetKey(KeyName, null);
            Cipher cipher = Cipher.GetInstance(Transformation);

            try
            {
                cipher.Init(CipherMode.EncryptMode, key);
            }
            catch (KeyPermanentlyInvalidatedException e)
            {
                // Biometric has changed
                return false;
            }
            catch (UnrecoverableKeyException e)
            {
                // Biometric was disabled and re-enabled
                return false;
            }
            catch (InvalidKeyException e)
            {
                // Fallback for old bitwarden users without a key
                CreateKey();
            }

            return false;
        }

        private void CreateKey()
        {
            KeyGenerator keyGen = KeyGenerator.GetInstance(KeyAlgorithm, KeyStoreName);
            KeyGenParameterSpec keyGenSpec =
                new KeyGenParameterSpec.Builder(KeyName, KeyStorePurpose.Encrypt | KeyStorePurpose.Decrypt)
                    .SetBlockModes(BlockMode)
                    .SetEncryptionPaddings(EncryptionPadding)
                    .SetUserAuthenticationRequired(true)
                    .Build();
            keyGen.Init(keyGenSpec);
            keyGen.GenerateKey();
        }
    }
}
