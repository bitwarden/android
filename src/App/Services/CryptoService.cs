using System;
using System.Diagnostics;
using System.Text;
using Bit.App.Abstractions;
using Bit.App.Models;
using PCLCrypto;
using System.Linq;
using Bit.App.Enums;
using System.Collections.Generic;
using Newtonsoft.Json;
using Plugin.Settings.Abstractions;
using Bit.App.Models.Api;
using Bit.App.Utilities;

namespace Bit.App.Services
{
    public class CryptoService : ICryptoService
    {
        private const string KeyKey = "key";
        private const string PrivateKeyKey = "encPrivateKey";
        private const string EncKeyKey = "encKey";
        private const string OrgKeysKey = "encOrgKeys";
        private const int InitializationVectorSize = 16;

        private readonly ISettings _settings;
        private readonly ISecureStorageService _secureStorage;
        private readonly IKeyDerivationService _keyDerivationService;
        private SymmetricCryptoKey _key;
        private SymmetricCryptoKey _encKey;
        private SymmetricCryptoKey _legacyEtmKey;
        private IDictionary<string, SymmetricCryptoKey> _orgKeys;
        private byte[] _privateKey;

        public CryptoService(
            ISettings settings,
            ISecureStorageService secureStorage,
            IKeyDerivationService keyDerivationService)
        {
            _settings = settings;
            _secureStorage = secureStorage;
            _keyDerivationService = keyDerivationService;
        }

        public SymmetricCryptoKey Key
        {
            get
            {
                if(_key == null && _secureStorage.Contains(KeyKey))
                {
                    var keyBytes = _secureStorage.Retrieve(KeyKey);
                    if(keyBytes != null)
                    {
                        _key = new SymmetricCryptoKey(keyBytes);
                    }
                }

                return _key;
            }
            set
            {
                if(value != null)
                {
                    _secureStorage.Store(KeyKey, value.Key);
                }
                else
                {
                    _secureStorage.Delete(KeyKey);
                }

                _key = value;
                _legacyEtmKey = null;
            }
        }

        public SymmetricCryptoKey EncKey
        {
            get
            {
                if(_encKey == null && _settings.Contains(EncKeyKey))
                {
                    var encKey = _settings.GetValueOrDefault(EncKeyKey, null);
                    var encKeyCs = new CipherString(encKey);
                    try
                    {
                        var decBytes = DecryptToBytes(encKeyCs, Key);
                        _encKey = new SymmetricCryptoKey(decBytes);
                    }
                    catch
                    {
                        _encKey = null;
                        Debug.WriteLine($"Cannot set enc key. Decryption failed.");
                    }
                }

                return _encKey;
            }
        }

        public byte[] PrivateKey
        {
            get
            {
                if(_privateKey == null && _settings.Contains(PrivateKeyKey))
                {
                    var encPrivateKey = _settings.GetValueOrDefault(PrivateKeyKey, null);
                    var encPrivateKeyCs = new CipherString(encPrivateKey);
                    try
                    {
                        _privateKey = DecryptToBytes(encPrivateKeyCs);
                    }
                    catch
                    {
                        _privateKey = null;
                        Debug.WriteLine($"Cannot set private key. Decryption failed.");
                    }
                }

                return _privateKey;
            }
        }

        public IDictionary<string, SymmetricCryptoKey> OrgKeys
        {
            get
            {
                if((!_orgKeys?.Any() ?? true) && _settings.Contains(OrgKeysKey))
                {
                    var orgKeysEncDictJson = _settings.GetValueOrDefault(OrgKeysKey, null);
                    if(!string.IsNullOrWhiteSpace(orgKeysEncDictJson))
                    {
                        _orgKeys = new Dictionary<string, SymmetricCryptoKey>();
                        var orgKeysDict = JsonConvert.DeserializeObject<IDictionary<string, string>>(orgKeysEncDictJson);
                        foreach(var item in orgKeysDict)
                        {
                            try
                            {
                                var orgKeyCs = new CipherString(item.Value);
                                var decOrgKeyBytes = RsaDecryptToBytes(orgKeyCs, PrivateKey);
                                _orgKeys.Add(item.Key, new SymmetricCryptoKey(decOrgKeyBytes));
                            }
                            catch
                            {
                                Debug.WriteLine($"Cannot set org key {item.Key}. Decryption failed.");
                            }
                        }
                    }
                }

                return _orgKeys;
            }
        }

        public void SetEncKey(CipherString encKeyEnc)
        {
            if(encKeyEnc != null)
            {
                _settings.AddOrUpdateValue(EncKeyKey, encKeyEnc.EncryptedString);
            }
            else if(_settings.Contains(EncKeyKey))
            {
                _settings.Remove(EncKeyKey);
            }

            _encKey = null;
        }

        public void SetPrivateKey(CipherString privateKeyEnc)
        {
            if(privateKeyEnc != null)
            {
                _settings.AddOrUpdateValue(PrivateKeyKey, privateKeyEnc.EncryptedString);
            }
            else if(_settings.Contains(PrivateKeyKey))
            {
                _settings.Remove(PrivateKeyKey);
            }

            _privateKey = null;
        }

        public void SetOrgKeys(ProfileResponse profile)
        {
            var orgKeysEncDict = new Dictionary<string, string>();

            if(profile?.Organizations?.Any() ?? false)
            {
                foreach(var org in profile.Organizations)
                {
                    orgKeysEncDict.Add(org.Id, org.Key);
                }
            }

            SetOrgKeys(orgKeysEncDict);
        }

        public void SetOrgKeys(Dictionary<string, string> orgKeysEncDict)
        {
            if(orgKeysEncDict?.Any() ?? false)
            {
                var dictJson = JsonConvert.SerializeObject(orgKeysEncDict);
                _settings.AddOrUpdateValue(OrgKeysKey, dictJson);
            }
            else if(_settings.Contains(OrgKeysKey))
            {
                _settings.Remove(OrgKeysKey);
            }

            _orgKeys = null;
        }

        public SymmetricCryptoKey GetOrgKey(string orgId)
        {
            if(OrgKeys == null || !OrgKeys.ContainsKey(orgId))
            {
                return null;
            }

            return OrgKeys[orgId];
        }

        public void ClearKeys()
        {
            SetOrgKeys((Dictionary<string, string>)null);
            Key = null;
            SetPrivateKey(null);
            SetEncKey(null);
        }

        public CipherString Encrypt(string plaintextValue, SymmetricCryptoKey key = null)
        {
            if(plaintextValue == null)
            {
                throw new ArgumentNullException(nameof(plaintextValue));
            }

            var plaintextBytes = Encoding.UTF8.GetBytes(plaintextValue);
            return Encrypt(plaintextBytes, key);
        }

        public CipherString Encrypt(byte[] plainBytes, SymmetricCryptoKey key = null)
        {
            if(key == null)
            {
                key = EncKey ?? Key;
            }

            if(key == null)
            {
                throw new ArgumentNullException(nameof(key));
            }

            if(plainBytes == null)
            {
                throw new ArgumentNullException(nameof(plainBytes));
            }

            return Crypto.AesCbcEncrypt(plainBytes, key);
        }

        public byte[] EncryptToBytes(byte[] plainBytes, SymmetricCryptoKey key = null)
        {
            if(key == null)
            {
                key = EncKey ?? Key;
            }

            if(key == null)
            {
                throw new ArgumentNullException(nameof(key));
            }

            if(plainBytes == null)
            {
                throw new ArgumentNullException(nameof(plainBytes));
            }

            return Crypto.AesCbcEncryptToBytes(plainBytes, key);
        }

        public string Decrypt(CipherString encyptedValue, SymmetricCryptoKey key = null)
        {
            try
            {
                var bytes = DecryptToBytes(encyptedValue, key);
                return Encoding.UTF8.GetString(bytes, 0, bytes.Length).TrimEnd('\0');
            }
            catch(Exception e)
            {
                Debug.WriteLine("Could not decrypt '{0}'. {1}", encyptedValue, e.Message);
                return "[error: cannot decrypt]";
            }
        }

        public byte[] DecryptToBytes(CipherString encyptedValue, SymmetricCryptoKey key = null)
        {
            if(key == null)
            {
                key = EncKey ?? Key;
            }

            if(key == null)
            {
                throw new ArgumentNullException(nameof(key));
            }

            if(encyptedValue == null)
            {
                throw new ArgumentNullException(nameof(encyptedValue));
            }

            if(encyptedValue.EncryptionType == EncryptionType.AesCbc128_HmacSha256_B64 &&
                key.EncryptionType == EncryptionType.AesCbc256_B64)
            {
                // Old encrypt-then-mac scheme, swap out the key
                if(_legacyEtmKey == null)
                {
                    _legacyEtmKey = new SymmetricCryptoKey(key.Key, EncryptionType.AesCbc128_HmacSha256_B64);
                }

                key = _legacyEtmKey;
            }

            return Crypto.AesCbcDecrypt(encyptedValue, key);
        }

        public byte[] DecryptToBytes(byte[] encyptedValue, SymmetricCryptoKey key = null)
        {
            if(key == null)
            {
                key = EncKey ?? Key;
            }

            if(key == null)
            {
                throw new ArgumentNullException(nameof(key));
            }

            if(encyptedValue == null || encyptedValue.Length == 0)
            {
                throw new ArgumentNullException(nameof(encyptedValue));
            }

            byte[] ct, iv, mac = null;
            var encType = (EncryptionType)encyptedValue[0];
            switch(encType)
            {
                case EncryptionType.AesCbc128_HmacSha256_B64:
                case EncryptionType.AesCbc256_HmacSha256_B64:
                    if(encyptedValue.Length <= 49)
                    {
                        throw new InvalidOperationException("Invalid value length.");
                    }

                    iv = new ArraySegment<byte>(encyptedValue, 1, 16).ToArray();
                    mac = new ArraySegment<byte>(encyptedValue, 17, 32).ToArray();
                    ct = new ArraySegment<byte>(encyptedValue, 49, encyptedValue.Length - 49).ToArray();
                    break;
                case EncryptionType.AesCbc256_B64:
                    if(encyptedValue.Length <= 17)
                    {
                        throw new InvalidOperationException("Invalid value length.");
                    }

                    iv = new ArraySegment<byte>(encyptedValue, 1, 16).ToArray();
                    ct = new ArraySegment<byte>(encyptedValue, 17, encyptedValue.Length - 17).ToArray();
                    break;
                default:
                    throw new InvalidOperationException("Invalid encryption type.");
            }

            return Crypto.AesCbcDecrypt(encType, ct, iv, mac, key);
        }

        public byte[] RsaDecryptToBytes(CipherString encyptedValue, byte[] privateKey)
        {
            if(privateKey == null)
            {
                privateKey = PrivateKey;
            }

            if(privateKey == null)
            {
                throw new ArgumentNullException(nameof(privateKey));
            }

            if(EncKey?.MacKey != null && !string.IsNullOrWhiteSpace(encyptedValue.Mac))
            {
                var computedMacBytes = Crypto.ComputeMac(encyptedValue.CipherTextBytes, EncKey.MacKey);
                if(!Crypto.MacsEqual(computedMacBytes, encyptedValue.MacBytes))
                {
                    throw new InvalidOperationException("MAC failed.");
                }
            }

            IAsymmetricKeyAlgorithmProvider provider = null;
            switch(encyptedValue.EncryptionType)
            {
                case EncryptionType.Rsa2048_OaepSha256_B64:
                case EncryptionType.Rsa2048_OaepSha256_HmacSha256_B64:
                    provider = WinRTCrypto.AsymmetricKeyAlgorithmProvider.OpenAlgorithm(AsymmetricAlgorithm.RsaOaepSha256);
                    break;
                case EncryptionType.Rsa2048_OaepSha1_B64:
                case EncryptionType.Rsa2048_OaepSha1_HmacSha256_B64:
                    provider = WinRTCrypto.AsymmetricKeyAlgorithmProvider.OpenAlgorithm(AsymmetricAlgorithm.RsaOaepSha1);
                    break;
                default:
                    throw new ArgumentException("EncryptionType unavailable.");
            }

            var cryptoKey = provider.ImportKeyPair(privateKey, CryptographicPrivateKeyBlobType.Pkcs8RawPrivateKeyInfo);
            var decryptedBytes = WinRTCrypto.CryptographicEngine.Decrypt(cryptoKey, encyptedValue.CipherTextBytes);
            return decryptedBytes;
        }

        public SymmetricCryptoKey MakeKeyFromPassword(string password, string salt)
        {
            if(password == null)
            {
                throw new ArgumentNullException(nameof(password));
            }

            if(salt == null)
            {
                throw new ArgumentNullException(nameof(salt));
            }

            var passwordBytes = Encoding.UTF8.GetBytes(password);
            var saltBytes = Encoding.UTF8.GetBytes(salt);

            var keyBytes = _keyDerivationService.DeriveKey(passwordBytes, saltBytes, 5000);
            return new SymmetricCryptoKey(keyBytes);
        }

        public string MakeKeyFromPasswordBase64(string password, string salt)
        {
            var key = MakeKeyFromPassword(password, salt);
            return Convert.ToBase64String(key.Key);
        }

        public byte[] HashPassword(SymmetricCryptoKey key, string password)
        {
            if(key == null)
            {
                throw new ArgumentNullException(nameof(key));
            }

            if(password == null)
            {
                throw new ArgumentNullException(nameof(password));
            }

            var passwordBytes = Encoding.UTF8.GetBytes(password);
            var hash = _keyDerivationService.DeriveKey(key.Key, passwordBytes, 1);
            return hash;
        }

        public string HashPasswordBase64(SymmetricCryptoKey key, string password)
        {
            var hash = HashPassword(key, password);
            return Convert.ToBase64String(hash);
        }

        public CipherString MakeEncKey(SymmetricCryptoKey key)
        {
            var bytes = Crypto.RandomBytes(512 / 8);
            return Encrypt(bytes, key);
        }
    }
}
