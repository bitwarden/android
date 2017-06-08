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
using Java.Util;
using Javax.Crypto.Spec;
using Android.Content;

namespace Bit.Android.Services
{
    public class KeyStoreBackedStorageService : ISecureStorageService
    {
        private const string AndroidKeyStore = "AndroidKeyStore";
        private const string KeyAlias = "bitwardenKey";
        private const string SettingsFormat = "ksSecured:{0}";
        private const string AesKey = "ksSecured:aesKeyForService";

        private readonly string _rsaMode;
        private readonly bool _oldAndroid;
        private readonly ISettings _settings;
        private readonly KeyStore _keyStore;
        private readonly ISecureStorageService _oldKeyStorageService;

        public KeyStoreBackedStorageService(ISettings settings)
        {
            try
            {
                _oldAndroid = Build.VERSION.SdkInt < BuildVersionCodes.M;
                _rsaMode = _oldAndroid ? "RSA/ECB/PKCS1Padding" : "RSA/ECB/OAEPWithSHA-1AndMGF1Padding";

                _oldKeyStorageService = new KeyStoreStorageService(new char[] { });
                _settings = settings;

                _keyStore = KeyStore.GetInstance(AndroidKeyStore);
                _keyStore.Load(null);

                GenerateRsaKey();
                GenerateAesKey();
            }
            catch(Exception e)
            {
                SendEmail(e.Message + "\n\n" + e.StackTrace);
                throw;
            }
        }

        private void SendEmail(string text, bool includeSecurityProviders = true)
        {
            var crashMessage = "bitwarden has crashed. Please send this email to our support team so that we can help " +
                "resolve the problem for you. Thank you.";

            text = crashMessage + "\n\n =============================================== \n\n" + text;

            if(includeSecurityProviders)
            {
                text += "\n\n";
                var providers = Security.GetProviders();
                foreach(var provider in providers)
                {
                    text += ("provider: " + provider.Name + "\n");
                    var services = provider.Services;
                    foreach(var service in provider.Services)
                    {
                        text += ("- alg: " + service.Algorithm + "\n");
                    }
                }
            }

            text += "\n\n ==================================================== \n\n" + crashMessage;

            var emailIntent = new Intent(Intent.ActionSend);

            emailIntent.SetType("plain/text");
            emailIntent.PutExtra(Intent.ExtraEmail, new String[] { "hello@bitwarden.com" });
            emailIntent.PutExtra(Intent.ExtraSubject, "bitwarden Crash Report");
            emailIntent.PutExtra(Intent.ExtraText, text);

            Application.Context.StartActivity(Intent.CreateChooser(emailIntent, "Send mail..."));
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

            var cs = _settings.GetValueOrDefault<string>(formattedKey);
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
                SendEmail(e.Message + "\n\n" + e.StackTrace);
                return null;
            }
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
            catch (Exception e)
            {
                Console.WriteLine("Failed to encrypt to secure storage.");
                SendEmail(e.Message + "\n\n" + e.StackTrace);
            }
        }

        private void GenerateRsaKey()
        {
            if(_keyStore.ContainsAlias(KeyAlias))
            {
                return;
            }

            var gen = KeyPairGenerator.GetInstance(KeyProperties.KeyAlgorithmRsa, AndroidKeyStore);
            var start = Calendar.Instance;
            var end = Calendar.Instance;
            end.Add(CalendarField.Year, 30);
            var subject = new X500Principal($"CN={KeyAlias}");

            if(_oldAndroid)
            {
                var spec = new KeyPairGeneratorSpec.Builder(Application.Context)
                    .SetAlias(KeyAlias)
                    .SetSubject(subject)
                    .SetSerialNumber(BigInteger.Ten)
                    .SetStartDate(start.Time)
                    .SetEndDate(end.Time)
                    .Build();

                gen.Initialize(spec);
            }
            else
            {
                var spec = new KeyGenParameterSpec.Builder(KeyAlias, KeyStorePurpose.Decrypt | KeyStorePurpose.Encrypt)
                    .SetCertificateSubject(subject)
                    .SetDigests(KeyProperties.DigestSha1)
                    .SetEncryptionPaddings(KeyProperties.EncryptionPaddingRsaOaep)
                    .Build();

                gen.Initialize(spec);
            }

            gen.GenerateKeyPair();
        }

        private KeyStore.PrivateKeyEntry GetRsaKeyEntry()
        {
            return _keyStore.GetEntry(KeyAlias, null) as KeyStore.PrivateKeyEntry;
        }

        private void GenerateAesKey()
        {
            if(_settings.Contains(AesKey))
            {
                return;
            }

            var key = App.Utilities.Crypto.RandomBytes(512 / 8);
            var encKey = RsaEncrypt(key);
            _settings.AddOrUpdateValue(AesKey, Convert.ToBase64String(encKey));
        }

        private App.Models.SymmetricCryptoKey GetAesKey()
        {
            try
            {
                var encKey = _settings.GetValueOrDefault<string>(AesKey);
                if(encKey == null)
                {
                    return null;
                }

                var encKeyBytes = Convert.FromBase64String(encKey);
                var key = RsaDecrypt(encKeyBytes);
                return new App.Models.SymmetricCryptoKey(key);
            }
            catch (Exception e)
            {
                Console.WriteLine("Cannot get AesKey.");
                _keyStore.DeleteEntry(KeyAlias);
                _settings.Remove(AesKey);
                SendEmail(e.Message + "\n\n" + e.StackTrace);
                return null;
            }
        }

        private byte[] RsaEncrypt(byte[] data)
        {
            using(var entry = GetRsaKeyEntry())
            using(var cipher = Cipher.GetInstance(_rsaMode))
            {
                cipher.Init(CipherMode.EncryptMode, entry.Certificate.PublicKey);
                var cipherText = cipher.DoFinal(data);
                return cipherText;
            }
        }

        private byte[] RsaDecrypt(byte[] encData)
        {
            using(var entry = GetRsaKeyEntry())
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