using System.IO;
using Java.Security;
using Javax.Crypto;
using Android.OS;
using Bit.App.Abstractions;
using System;
using Android.Security;
using Javax.Security.Auth.X500;
using Java.Math;
using Android.Security.Keystore;
using Android.App;
using Plugin.Settings.Abstractions;
using Javax.Crypto.Spec;
using System.Collections.Generic;
using Java.Util;

namespace Bit.Android.Services
{
    public class KeyStoreBackedStorageService : ISecureStorageService
    {
        private const string AndroidKeyStore = "AndroidKeyStore";
        private const string AndroidOpenSSL = "AndroidOpenSSL";
        private const string KeyAlias = "bitwardenKey";
        private const string SettingsFormat = "ksSecured:{0}";
        private const string RsaMode = "RSA/ECB/PKCS1Padding";
        private const string AesMode = "AES/GCM/NoPadding";
        private const string AesKey = "ksSecured:aesKeyForService";

        private readonly ISettings _settings;
        private readonly KeyStore _keyStore;
        private readonly bool _oldAndroid = Build.VERSION.SdkInt < BuildVersionCodes.M;
        private readonly KeyStoreStorageService _oldKeyStorageService;

        public KeyStoreBackedStorageService(ISettings settings)
        {
            _oldKeyStorageService = new KeyStoreStorageService(new char[] { });
            _settings = settings;

            _keyStore = KeyStore.GetInstance(AndroidKeyStore);
            _keyStore.Load(null);

            GenerateKeys();
            GenerateAesKey();
        }

        public bool Contains(string key)
        {
            return _settings.Contains(string.Format(SettingsFormat, key)) || _oldKeyStorageService.Contains(key);
        }

        public void Delete(string key)
        {
            CleanupOldKeyStore(key);
            _settings.Remove(string.Format(SettingsFormat, key));
        }

        public byte[] Retrieve(string key)
        {
            var formattedKey = string.Format(SettingsFormat, key);
            if(!_settings.Contains(formattedKey))
            {
                return TryGetAndMigrateFromOldKeyStore(key);
            }

            var cipherString = _settings.GetValueOrDefault<string>(formattedKey);
            return AesDecrypt(cipherString);
        }

        public void Store(string key, byte[] dataBytes)
        {
            var formattedKey = string.Format(SettingsFormat, key);
            CleanupOldKeyStore(key);
            if(dataBytes == null)
            {
                _settings.Remove(formattedKey);
                return;
            }

            var cipherString = AesEncrypt(dataBytes);
            _settings.AddOrUpdateValue(formattedKey, cipherString);
        }

        private byte[] RandomBytes(int length)
        {
            var key = new byte[length];
            var secureRandom = new SecureRandom();
            secureRandom.NextBytes(key);
            return key;
        }

        private void GenerateKeys()
        {
            if(_keyStore.ContainsAlias(KeyAlias))
            {
                return;
            }

            if(_oldAndroid)
            {
                var start = Calendar.Instance;
                var end = Calendar.Instance;
                end.Add(CalendarField.Year, 30);

                var gen = KeyPairGenerator.GetInstance(KeyProperties.KeyAlgorithmRsa, AndroidKeyStore);
                var spec = new KeyPairGeneratorSpec.Builder(Application.Context)
                    .SetAlias(KeyAlias)
                    .SetSubject(new X500Principal($"CN={KeyAlias}"))
                    .SetSerialNumber(BigInteger.Ten)
                    .SetStartDate(start.Time)
                    .SetEndDate(end.Time)
                    .Build();

                gen.Initialize(spec);
                gen.GenerateKeyPair();
            }
            else
            {
                var gen = KeyGenerator.GetInstance(KeyProperties.KeyAlgorithmAes, AndroidKeyStore);
                var spec = new KeyGenParameterSpec.Builder(KeyAlias, KeyStorePurpose.Decrypt | KeyStorePurpose.Encrypt)
                    .SetBlockModes(KeyProperties.BlockModeGcm).SetEncryptionPaddings(KeyProperties.EncryptionPaddingNone)
                    .Build();

                gen.Init(spec);
                gen.GenerateKey();
            }
        }

        private void GenerateAesKey()
        {
            if(!_oldAndroid)
            {
                return;
            }

            if(_settings.Contains(AesKey))
            {
                return;
            }

            var key = RandomBytes(16);
            var encKey = RsaEncrypt(key);
            _settings.AddOrUpdateValue(AesKey, Convert.ToBase64String(encKey));
        }

        private IKey GetAesKey()
        {
            if(_oldAndroid)
            {
                var encKey = _settings.GetValueOrDefault<string>(AesKey);
                var encKeyBytes = Convert.FromBase64String(encKey);
                var key = RsaDecrypt(encKeyBytes);
                return new SecretKeySpec(key, "AES");
            }
            else
            {
                var entry = _keyStore.GetEntry(KeyAlias, null) as KeyStore.SecretKeyEntry;
                return entry.SecretKey;
            }
        }

        private string AesEncrypt(byte[] input)
        {
            var cipher = Cipher.GetInstance(AesMode);
            //var ivBytes = RandomBytes(12);
            //var spec = new GCMParameterSpec(128, ivBytes);
            cipher.Init(CipherMode.EncryptMode, GetAesKey());
            var encBytes = cipher.DoFinal(input);
            var ivBytes = cipher.GetIV();
            return $"{Convert.ToBase64String(ivBytes)}|{Convert.ToBase64String(encBytes)}";
        }

        private byte[] AesDecrypt(string cipherString)
        {
            var parts = cipherString.Split('|');
            var ivBytes = Convert.FromBase64String(parts[0]);
            var encBytes = Convert.FromBase64String(parts[1]);

            var cipher = Cipher.GetInstance(AesMode);
            var spec = new GCMParameterSpec(128, ivBytes);
            cipher.Init(CipherMode.DecryptMode, GetAesKey(), spec);
            var decBytes = cipher.DoFinal(encBytes);
            return decBytes;
        }

        private byte[] RsaEncrypt(byte[] input)
        {
            var entry = _keyStore.GetEntry(KeyAlias, null) as KeyStore.PrivateKeyEntry;
            var inputCipher = Cipher.GetInstance(RsaMode, AndroidOpenSSL);
            inputCipher.Init(CipherMode.EncryptMode, entry.Certificate.PublicKey);

            var outputStream = new MemoryStream();
            var cipherStream = new CipherOutputStream(outputStream, inputCipher);
            cipherStream.Write(input);
            cipherStream.Close();

            var vals = outputStream.ToArray();
            outputStream.Close();
            return vals;
        }

        private byte[] RsaDecrypt(byte[] encInput)
        {
            var entry = _keyStore.GetEntry(KeyAlias, null) as KeyStore.PrivateKeyEntry;
            var outputCipher = Cipher.GetInstance(RsaMode, AndroidOpenSSL);
            outputCipher.Init(CipherMode.DecryptMode, entry.PrivateKey);

            var inputStream = new MemoryStream(encInput);
            var cipherStream = new CipherInputStream(inputStream, outputCipher);

            var values = new List<byte>();
            int nextByte;
            while((nextByte = cipherStream.Read()) != -1)
            {
                values.Add((byte)nextByte);
            }

            inputStream.Close();
            cipherStream.Close();

            var bytes = new byte[values.Count];
            for(var i = 0; i < bytes.Length; i++)
            {
                bytes[i] = values[i];
            }

            return bytes;
        }

        private byte[] TryGetAndMigrateFromOldKeyStore(string key)
        {
            if(_oldKeyStorageService.Contains(key))
            {
                var value = _oldKeyStorageService.Retrieve(key);
                Store(key, value);
                _oldKeyStorageService.Delete(key);
                return value;
            }

            return null;
        }

        private void CleanupOldKeyStore(string key)
        {
            if(_oldKeyStorageService.Contains(key))
            {
                _oldKeyStorageService.Delete(key);
            }
        }
    }
}