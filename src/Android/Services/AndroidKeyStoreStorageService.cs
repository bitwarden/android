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
using Java.Util;
using Javax.Crypto.Spec;
using Android.Preferences;

namespace Bit.Android.Services
{
    public class AndroidKeyStoreStorageService : ISecureStorageService
    {
        private const string AndroidKeyStore = "AndroidKeyStore";
        private const string AesMode = "AES/GCM/NoPadding";

        private const string KeyAlias = "bitwardenKey2";
        private const string KeyAliasV1 = "bitwardenKey";

        private const string SettingsFormat = "ksSecured2:{0}";
        private const string SettingsFormatV1 = "ksSecured:{0}";

        private const string AesKey = "ksSecured2:aesKeyForService";
        private const string AesKeyV1 = "ksSecured:aesKeyForService";

        private readonly string _rsaMode;
        private readonly bool _oldAndroid;
        private readonly ISettings _settings;
        private readonly KeyStore _keyStore;
        private readonly ISecureStorageService _oldKeyStorageService;

        public AndroidKeyStoreStorageService(ISettings settings)
        {
            _oldAndroid = Build.VERSION.SdkInt < BuildVersionCodes.M;
            _rsaMode = _oldAndroid ? "RSA/ECB/PKCS1Padding" : "RSA/ECB/OAEPWithSHA-1AndMGF1Padding";

            _oldKeyStorageService = new KeyStoreStorageService(new char[] { });
            _settings = settings;

            _keyStore = KeyStore.GetInstance(AndroidKeyStore);
            _keyStore.Load(null);

            GenerateStoreKey();
            GenerateAesKey();
        }

        public bool Contains(string key)
        {
            return _settings.Contains(string.Format(SettingsFormat, key)) ||
                _settings.Contains(string.Format(SettingsFormatV1, key)) ||
                _oldKeyStorageService.Contains(key);
        }

        public void Delete(string key)
        {
            CleanupOld(key);

            var formattedKey = string.Format(SettingsFormat, key);
            if(_settings.Contains(formattedKey))
            {
                _settings.Remove(formattedKey);
            }
        }

        public byte[] Retrieve(string key)
        {
            var formattedKey = string.Format(SettingsFormat, key);
            if(!_settings.Contains(formattedKey))
            {
                return TryGetAndMigrate(key);
            }

            var cs = _settings.GetValueOrDefault(formattedKey, null);
            if(string.IsNullOrWhiteSpace(cs))
            {
                return null;
            }

            var aesKey = GetAesKey();
            if(aesKey == null)
            {
                return null;
            }

            try
            {
                return App.Utilities.Crypto.AesCbcDecrypt(new App.Models.CipherString(cs), aesKey);
            }
            catch(Exception e)
            {
                Console.WriteLine("Failed to decrypt from secure storage.");
                _settings.Remove(formattedKey);
                //Utilities.SendCrashEmail(e);
                //Utilities.SaveCrashFile(e);
                return null;
            }
        }

        public void Store(string key, byte[] dataBytes)
        {
            var formattedKey = string.Format(SettingsFormat, key);
            CleanupOld(key);
            if(dataBytes == null)
            {
                _settings.Remove(formattedKey);
                return;
            }

            var aesKey = GetAesKey();
            if(aesKey == null)
            {
                return;
            }

            try
            {
                var cipherString = App.Utilities.Crypto.AesCbcEncrypt(dataBytes, aesKey);
                _settings.AddOrUpdateValue(formattedKey, cipherString.EncryptedString);
            }
            catch(Exception e)
            {
                Console.WriteLine("Failed to encrypt to secure storage.");
                //Utilities.SendCrashEmail(e);
                //Utilities.SaveCrashFile(e);
            }
        }

        private void GenerateStoreKey()
        {
            if(_keyStore.ContainsAlias(KeyAlias))
            {
                return;
            }

            ClearSettings();

            var end = Calendar.Instance;
            end.Add(CalendarField.Year, 99);

            if(_oldAndroid)
            {
                var subject = new X500Principal($"CN={KeyAlias}");

                var spec = new KeyPairGeneratorSpec.Builder(Application.Context)
                    .SetAlias(KeyAlias)
                    .SetSubject(subject)
                    .SetSerialNumber(BigInteger.Ten)
                    .SetStartDate(new Date(0))
                    .SetEndDate(end.Time)
                    .Build();

                var gen = KeyPairGenerator.GetInstance(KeyProperties.KeyAlgorithmRsa, AndroidKeyStore);
                gen.Initialize(spec);
                gen.GenerateKeyPair();
            }
            else
            {
                var spec = new KeyGenParameterSpec.Builder(KeyAlias, KeyStorePurpose.Decrypt | KeyStorePurpose.Encrypt)
                    .SetBlockModes(KeyProperties.BlockModeGcm)
                    .SetEncryptionPaddings(KeyProperties.EncryptionPaddingNone)
                    .SetKeyValidityStart(new Date(0))
                    .SetKeyValidityEnd(end.Time)
                    .Build();

                var gen = KeyGenerator.GetInstance(KeyProperties.KeyAlgorithmAes, AndroidKeyStore);
                gen.Init(spec);
                gen.GenerateKey();
            }
        }

        private KeyStore.PrivateKeyEntry GetRsaKeyEntry(string alias)
        {
            return _keyStore.GetEntry(alias, null) as KeyStore.PrivateKeyEntry;
        }

        private void GenerateAesKey()
        {
            if(_settings.Contains(AesKey))
            {
                return;
            }

            var key = App.Utilities.Crypto.RandomBytes(512 / 8);
            var encKey = _oldAndroid ? RsaEncrypt(key) : AesEncrypt(key);
            _settings.AddOrUpdateValue(AesKey, encKey);
        }

        private App.Models.SymmetricCryptoKey GetAesKey(bool v1 = false)
        {
            try
            {
                var aesKey = v1 ? AesKeyV1 : AesKey;
                if(!_settings.Contains(aesKey))
                {
                    return null;
                }

                var encKey = _settings.GetValueOrDefault(aesKey, null);
                if(string.IsNullOrWhiteSpace(encKey))
                {
                    return null;
                }

                if(_oldAndroid || v1)
                {
                    var encKeyBytes = Convert.FromBase64String(encKey);
                    var key = RsaDecrypt(encKeyBytes, v1);
                    return new App.Models.SymmetricCryptoKey(key);
                }
                else
                {
                    var parts = encKey.Split('|');
                    if(parts.Length < 2)
                    {
                        return null;
                    }

                    var ivBytes = Convert.FromBase64String(parts[0]);
                    var encKeyBytes = Convert.FromBase64String(parts[1]);
                    var key = AesDecrypt(ivBytes, encKeyBytes);
                    return new App.Models.SymmetricCryptoKey(key);
                }
            }
            catch(Exception e)
            {
                Console.WriteLine("Cannot get AesKey.");
                _keyStore.DeleteEntry(KeyAlias);
                _settings.Remove(AesKey);
                if(!v1)
                {
                    //Utilities.SendCrashEmail(e);
                    //Utilities.SaveCrashFile(e);
                }
                return null;
            }
        }

        private string AesEncrypt(byte[] input)
        {
            using(var entry = _keyStore.GetKey(KeyAlias, null))
            using(var cipher = Cipher.GetInstance(AesMode))
            {
                cipher.Init(CipherMode.EncryptMode, entry);
                var encBytes = cipher.DoFinal(input);
                var ivBytes = cipher.GetIV();
                return $"{Convert.ToBase64String(ivBytes)}|{Convert.ToBase64String(encBytes)}";
            }
        }

        private byte[] AesDecrypt(byte[] iv, byte[] encData)
        {
            using(var entry = _keyStore.GetKey(KeyAlias, null))
            using(var cipher = Cipher.GetInstance(AesMode))
            {
                var spec = new GCMParameterSpec(128, iv);
                cipher.Init(CipherMode.DecryptMode, entry, spec);
                var decBytes = cipher.DoFinal(encData);
                return decBytes;
            }
        }

        private string RsaEncrypt(byte[] data)
        {
            using(var entry = GetRsaKeyEntry(KeyAlias))
            using(var cipher = Cipher.GetInstance(_rsaMode))
            {
                cipher.Init(CipherMode.EncryptMode, entry.Certificate.PublicKey);
                var cipherText = cipher.DoFinal(data);
                return Convert.ToBase64String(cipherText);
            }
        }

        private byte[] RsaDecrypt(byte[] encData, bool v1)
        {
            using(var entry = GetRsaKeyEntry(v1 ? KeyAliasV1 : KeyAlias))
            using(var cipher = Cipher.GetInstance(_rsaMode))
            {
                if(_oldAndroid)
                {
                    cipher.Init(CipherMode.DecryptMode, entry.PrivateKey);
                }
                else
                {
                    cipher.Init(CipherMode.DecryptMode, entry.PrivateKey, OAEPParameterSpec.Default);
                }

                var plainText = cipher.DoFinal(encData);
                return plainText;
            }
        }

        private byte[] TryGetAndMigrate(string key)
        {
            if(_oldKeyStorageService.Contains(key))
            {
                var value = _oldKeyStorageService.Retrieve(key);
                Store(key, value);
                return value;
            }

            var formattedKeyV1 = string.Format(SettingsFormatV1, key);
            if(_settings.Contains(formattedKeyV1))
            {
                var aesKeyV1 = GetAesKey(true);
                if(aesKeyV1 != null)
                {
                    try
                    {
                        var cs = _settings.GetValueOrDefault(formattedKeyV1, null);
                        var value = App.Utilities.Crypto.AesCbcDecrypt(new App.Models.CipherString(cs), aesKeyV1);
                        Store(key, value);
                        return value;
                    }
                    catch
                    {
                        Console.WriteLine("Failed to decrypt v1 from secure storage.");
                    }
                }

                _settings.Remove(formattedKeyV1);
            }

            return null;
        }

        private void CleanupOld(string key)
        {
            if(_oldKeyStorageService.Contains(key))
            {
                _oldKeyStorageService.Delete(key);
            }

            var formattedKeyV1 = string.Format(SettingsFormatV1, key);
            if(_settings.Contains(formattedKeyV1))
            {
                _settings.Remove(formattedKeyV1);
            }
        }

        private void ClearSettings(string format = SettingsFormat)
        {
            var prefix = string.Format(format, string.Empty);

            using(var sharedPreferences = PreferenceManager.GetDefaultSharedPreferences(Application.Context))
            using(var sharedPreferencesEditor = sharedPreferences.Edit())
            {
                var removed = false;
                foreach(var pref in sharedPreferences.All)
                {
                    if(pref.Key.StartsWith(prefix))
                    {
                        removed = true;
                        sharedPreferencesEditor.Remove(pref.Key);
                    }
                }

                if(removed)
                {
                    sharedPreferencesEditor.Commit();
                }
            }
        }
    }
}