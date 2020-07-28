using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Android.Content;
using Android.Security.Keystore;
using Bit.Core.Abstractions;
using Java.Security;
using Javax.Crypto;

namespace Bit.Droid.Services
{
    class BiometricService : IBiometricService
    {
        static readonly string KEY_NAME = "com.8bit.bitwarden.biometric_integrity";

        static readonly string KEYSTORE_NAME = "AndroidKeyStore";

        static readonly string KEY_ALGORITHM = KeyProperties.KeyAlgorithmAes;
        static readonly string BLOCK_MODE = KeyProperties.BlockModeCbc;
        static readonly string ENCRYPTION_PADDING = KeyProperties.EncryptionPaddingPkcs7;
        static readonly string TRANSFORMATION = KEY_ALGORITHM + "/" + BLOCK_MODE + "/" + ENCRYPTION_PADDING;

        readonly KeyStore _keystore;

        Context context = Android.App.Application.Context;

        public BiometricService()
        {
            _keystore = KeyStore.GetInstance(KEYSTORE_NAME);
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
            IKey key = _keystore.GetKey(KEY_NAME, null);
            Cipher cipher = Cipher.GetInstance(TRANSFORMATION);

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

            return true;
        }

        private void CreateKey()
        {
            KeyGenerator keyGen = KeyGenerator.GetInstance(KEY_ALGORITHM, KEYSTORE_NAME);
            KeyGenParameterSpec keyGenSpec =
                new KeyGenParameterSpec.Builder(KEY_NAME, KeyStorePurpose.Encrypt | KeyStorePurpose.Decrypt)
                    .SetBlockModes(BLOCK_MODE)
                    .SetEncryptionPaddings(ENCRYPTION_PADDING)
                    .SetUserAuthenticationRequired(true)
                    .Build();
            keyGen.Init(keyGenSpec);
            keyGen.GenerateKey();
        }
    }
}
