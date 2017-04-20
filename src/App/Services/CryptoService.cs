using System;
using System.Diagnostics;
using System.Text;
using Bit.App.Abstractions;
using Bit.App.Models;
using PCLCrypto;
using System.Linq;
using Bit.App.Enums;
using System.Collections.Generic;

namespace Bit.App.Services
{
    public class CryptoService : ICryptoService
    {
        private const string KeyKey = "key";
        private const string PreviousKeyKey = "previousKey";
        private const string PrivateKeyKey = "privateKey";
        private const string OrgKeyKeyPrefix = "orgKey:";
        private const int InitializationVectorSize = 16;

        private readonly ISecureStorageService _secureStorage;
        private readonly IKeyDerivationService _keyDerivationService;
        private CryptoKey _key;
        private CryptoKey _legacyEtmKey;
        private CryptoKey _previousKey;
        private IDictionary<Guid, CryptoKey> _orgKeys = new Dictionary<Guid, CryptoKey>();
        private byte[] _privateKey;

        public CryptoService(
            ISecureStorageService secureStorage,
            IKeyDerivationService keyDerivationService)
        {
            _secureStorage = secureStorage;
            _keyDerivationService = keyDerivationService;
        }

        public CryptoKey Key
        {
            get
            {
                if(_key == null)
                {
                    _key = new CryptoKey(_secureStorage.Retrieve(KeyKey));
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

        public CryptoKey PreviousKey
        {
            get
            {
                if(_previousKey == null)
                {
                    _previousKey = new CryptoKey(_secureStorage.Retrieve(PreviousKeyKey));
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
                if(_privateKey == null)
                {
                    _privateKey = _secureStorage.Retrieve(PrivateKeyKey);
                }

                return _privateKey;
            }
            private set
            {
                if(value != null)
                {
                    _secureStorage.Store(PrivateKeyKey, value);
                    _privateKey = value;
                }
            }
        }

        public void SetPrivateKey(CipherString privateKeyEnc, CryptoKey key)
        {
            var bytes = DecryptToBytes(privateKeyEnc, key);
            PrivateKey = bytes;
        }

        public CryptoKey GetOrgKey(Guid orgId)
        {
            if(!_orgKeys.ContainsKey(orgId))
            {
                var key = string.Concat(OrgKeyKeyPrefix, orgId);
                if(!_secureStorage.Contains(key))
                {
                    return null;
                }

                _orgKeys[orgId] = new CryptoKey(_secureStorage.Retrieve(key));
            }

            return _orgKeys[orgId];
        }

        public void AddOrgKey(Guid orgId, CipherString encOrgKey, byte[] privateKey)
        {
            try
            {
                var decBytes = RsaDecryptToBytes(encOrgKey, privateKey);
                _orgKeys[orgId] = new CryptoKey(decBytes);
            }
            catch
            {
                Debug.WriteLine("Cannot set org key. Decryption failed.");
            }
        }

        public CipherString Encrypt(string plaintextValue, CryptoKey key = null)
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
            var mac = key.MacKey != null ? ComputeMac(encryptedBytes, iv, key.MacKey) : null;

            return new CipherString(key.EncryptionType, Convert.ToBase64String(iv), Convert.ToBase64String(encryptedBytes), mac);
        }

        public string Decrypt(CipherString encyptedValue, CryptoKey key = null)
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

        public byte[] DecryptToBytes(CipherString encyptedValue, CryptoKey key = null)
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
                    _legacyEtmKey = new CryptoKey(key.Key, Enums.EncryptionType.AesCbc128_HmacSha256_B64);
                }

                key = _legacyEtmKey;
            }

            if(encyptedValue.EncryptionType != key.EncryptionType)
            {
                throw new ArgumentException("encType unavailable.");
            }

            if(key.MacKey != null && !string.IsNullOrWhiteSpace(encyptedValue.Mac))
            {
                var computedMac = ComputeMac(encyptedValue.CipherTextBytes,
                    encyptedValue.InitializationVectorBytes, key.MacKey);
                if(computedMac != encyptedValue.Mac)
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

            if(encyptedValue.EncryptionType != EncryptionType.RsaOaep_Sha256_B64)
            {
                throw new ArgumentException("encType unavailable.");
            }

            var provider = WinRTCrypto.AsymmetricKeyAlgorithmProvider.OpenAlgorithm(AsymmetricAlgorithm.RsaOaepSha256);
            var cryptoKey = provider.ImportKeyPair(privateKey, CryptographicPrivateKeyBlobType.Pkcs8RawPrivateKeyInfo);
            var decryptedBytes = WinRTCrypto.CryptographicEngine.Decrypt(cryptoKey, encyptedValue.CipherTextBytes);
            return decryptedBytes;
        }

        private string ComputeMac(byte[] ctBytes, byte[] ivBytes, byte[] macKey)
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
            return Convert.ToBase64String(mac);
        }

        public CryptoKey MakeKeyFromPassword(string password, string salt)
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
            return new CryptoKey(keyBytes);
        }

        public string MakeKeyFromPasswordBase64(string password, string salt)
        {
            var key = MakeKeyFromPassword(password, salt);
            return Convert.ToBase64String(key.Key);
        }

        public byte[] HashPassword(CryptoKey key, string password)
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

        public string HashPasswordBase64(CryptoKey key, string password)
        {
            var hash = HashPassword(key, password);
            return Convert.ToBase64String(hash);
        }
    }
}
