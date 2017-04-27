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

namespace Bit.App.Services
{
    public class CryptoService : ICryptoService
    {
        private const string KeyKey = "key";
        private const string PreviousKeyKey = "previousKey";
        private const string PrivateKeyKey = "encPrivateKey";
        private const string OrgKeysKey = "encOrgKeys";
        private const int InitializationVectorSize = 16;

        private readonly ISettings _settings;
        private readonly ISecureStorageService _secureStorage;
        private readonly IKeyDerivationService _keyDerivationService;
        private SymmetricCryptoKey _key;
        private SymmetricCryptoKey _legacyEtmKey;
        private SymmetricCryptoKey _previousKey;
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
                    PreviousKey = _key;
                    _secureStorage.Delete(KeyKey);
                    _key = null;
                    _legacyEtmKey = null;
                }
            }
        }

        public SymmetricCryptoKey PreviousKey
        {
            get
            {
                if(_previousKey == null && _secureStorage.Contains(PreviousKeyKey))
                {
                    var keyBytes = _secureStorage.Retrieve(PreviousKeyKey);
                    if(keyBytes != null)
                    {
                        _previousKey = new SymmetricCryptoKey(keyBytes);
                    }
                }

                return _previousKey;
            }
            private set
            {
                if(value != null)
                {
                    _secureStorage.Store(PreviousKeyKey, value.Key);
                    _previousKey = value;
                }
            }
        }

        public bool KeyChanged
        {
            get
            {
                if(Key == null)
                {
                    return false;
                }

                if(PreviousKey == null)
                {
                    return Key != null;
                }

                return !PreviousKey.Key.SequenceEqual(Key.Key);
            }
        }

        public byte[] PrivateKey
        {
            get
            {
                if(_privateKey == null && _settings.Contains(PrivateKeyKey))
                {
                    var encPrivateKey = _settings.GetValueOrDefault<string>(PrivateKeyKey);
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
                    var orgKeysEncDictJson = _settings.GetValueOrDefault<string>(OrgKeysKey);
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

        public void SetPrivateKey(CipherString privateKeyEnc)
        {
            if(privateKeyEnc != null)
            {
                _settings.AddOrUpdateValue(PrivateKeyKey, privateKeyEnc.EncryptedString);
            }
            else if(_settings.Contains(PrivateKeyKey))
            {
                _settings.Remove(PrivateKeyKey);
                _privateKey = null;
            }
            else
            {
                _privateKey = null;
            }
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
                _orgKeys = null;
            }
            else
            {
                _orgKeys = null;
            }
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
        }

        public CipherString Encrypt(string plaintextValue, SymmetricCryptoKey key = null)
        {
            if(key == null)
            {
                key = Key;
            }

            if(key == null)
            {
                throw new ArgumentNullException(nameof(key));
            }

            if(plaintextValue == null)
            {
                throw new ArgumentNullException(nameof(plaintextValue));
            }

            var plaintextBytes = Encoding.UTF8.GetBytes(plaintextValue);

            var provider = WinRTCrypto.SymmetricKeyAlgorithmProvider.OpenAlgorithm(SymmetricAlgorithm.AesCbcPkcs7);
            var cryptoKey = provider.CreateSymmetricKey(key.EncKey);
            var iv = WinRTCrypto.CryptographicBuffer.GenerateRandom(provider.BlockLength);
            var encryptedBytes = WinRTCrypto.CryptographicEngine.Encrypt(cryptoKey, plaintextBytes, iv);
            var mac = key.MacKey != null ? ComputeMacBase64(encryptedBytes, iv, key.MacKey) : null;

            return new CipherString(key.EncryptionType, Convert.ToBase64String(iv),
                Convert.ToBase64String(encryptedBytes), mac);
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
                key = Key;
            }

            if(key == null)
            {
                throw new ArgumentNullException(nameof(key));
            }

            if(encyptedValue == null)
            {
                throw new ArgumentNullException(nameof(encyptedValue));
            }

            if(encyptedValue.EncryptionType == Enums.EncryptionType.AesCbc128_HmacSha256_B64 &&
                key.EncryptionType == Enums.EncryptionType.AesCbc256_B64)
            {
                // Old encrypt-then-mac scheme, swap out the key
                if(_legacyEtmKey == null)
                {
                    _legacyEtmKey = new SymmetricCryptoKey(key.Key, Enums.EncryptionType.AesCbc128_HmacSha256_B64);
                }

                key = _legacyEtmKey;
            }

            if(encyptedValue.EncryptionType != key.EncryptionType)
            {
                throw new ArgumentException("encType unavailable.");
            }

            if(key.MacKey != null && !string.IsNullOrWhiteSpace(encyptedValue.Mac))
            {
                var computedMacBytes = ComputeMac(encyptedValue.CipherTextBytes,
                    encyptedValue.InitializationVectorBytes, key.MacKey);
                if(!MacsEqual(key.MacKey, computedMacBytes, encyptedValue.MacBytes))
                {
                    throw new InvalidOperationException("MAC failed.");
                }
            }

            var provider = WinRTCrypto.SymmetricKeyAlgorithmProvider.OpenAlgorithm(SymmetricAlgorithm.AesCbcPkcs7);
            var cryptoKey = provider.CreateSymmetricKey(key.EncKey);
            var decryptedBytes = WinRTCrypto.CryptographicEngine.Decrypt(cryptoKey, encyptedValue.CipherTextBytes,
                encyptedValue.InitializationVectorBytes);
            return decryptedBytes;
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

            IAsymmetricKeyAlgorithmProvider provider = null;
            switch(encyptedValue.EncryptionType)
            {
                case EncryptionType.Rsa2048_OaepSha256_B64:
                    provider = WinRTCrypto.AsymmetricKeyAlgorithmProvider.OpenAlgorithm(AsymmetricAlgorithm.RsaOaepSha256);
                    break;
                case EncryptionType.Rsa2048_OaepSha1_B64:
                    provider = WinRTCrypto.AsymmetricKeyAlgorithmProvider.OpenAlgorithm(AsymmetricAlgorithm.RsaOaepSha1);
                    break;
                default:
                    throw new ArgumentException("EncryptionType unavailable.");
            }

            var cryptoKey = provider.ImportKeyPair(privateKey, CryptographicPrivateKeyBlobType.Pkcs8RawPrivateKeyInfo);
            var decryptedBytes = WinRTCrypto.CryptographicEngine.Decrypt(cryptoKey, encyptedValue.CipherTextBytes);
            return decryptedBytes;
        }

        private string ComputeMacBase64(byte[] ctBytes, byte[] ivBytes, byte[] macKey)
        {
            var mac = ComputeMac(ctBytes, ivBytes, macKey);
            return Convert.ToBase64String(mac);
        }

        private byte[] ComputeMac(byte[] ctBytes, byte[] ivBytes, byte[] macKey)
        {
            if(macKey == null)
            {
                throw new ArgumentNullException(nameof(macKey));
            }

            if(ctBytes == null)
            {
                throw new ArgumentNullException(nameof(ctBytes));
            }

            if(ivBytes == null)
            {
                throw new ArgumentNullException(nameof(ivBytes));
            }

            var algorithm = WinRTCrypto.MacAlgorithmProvider.OpenAlgorithm(MacAlgorithm.HmacSha256);
            var hasher = algorithm.CreateHash(macKey);
            hasher.Append(ivBytes.Concat(ctBytes).ToArray());
            var mac = hasher.GetValueAndReset();
            return mac;
        }

        // Safely compare two MACs in a way that protects against timing attacks (Double HMAC Verification).
        // ref: https://www.nccgroup.trust/us/about-us/newsroom-and-events/blog/2011/february/double-hmac-verification/
        private bool MacsEqual(byte[] macKey, byte[] mac1, byte[] mac2)
        {
            var algorithm = WinRTCrypto.MacAlgorithmProvider.OpenAlgorithm(MacAlgorithm.HmacSha256);
            var hasher = algorithm.CreateHash(macKey);

            hasher.Append(mac1);
            mac1 = hasher.GetValueAndReset();

            hasher.Append(mac2);
            mac2 = hasher.GetValueAndReset();

            if(mac1.Length != mac2.Length)
            {
                return false;
            }

            for(int i = 0; i < mac2.Length; i++)
            {
                if(mac1[i] != mac2[i])
                {
                    return false;
                }
            }

            return true;
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
                throw new ArgumentNullException(nameof(Key));
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
    }
}
