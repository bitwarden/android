using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Android.Security.Keystore;
using Bit.Core.Abstractions;
using Java.Security;
using Javax.Crypto;

namespace Bit.Droid.Services
{
    class BiometricService : IBiometricService
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

        public bool SetupBiometric()
        {
            CreateKey();

            return true;
        }

        public bool ValidateIntegrity()
        {
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
                // Fallback for updates of Bitwarden application
                CreateKey();
            }

            return true;
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
