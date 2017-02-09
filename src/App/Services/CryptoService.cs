using System;
using System.Diagnostics;
using System.Text;
using Bit.App.Abstractions;
using Bit.App.Models;
using PCLCrypto;
using System.Linq;
using Xamarin.Forms;

namespace Bit.App.Services
{
    public class CryptoService : ICryptoService
    {
        private const string KeyKey = "key";
        private const string PreviousKeyKey = "previousKey";
        private const int InitializationVectorSize = 16;

        private readonly ISecureStorageService _secureStorage;
        private readonly IKeyDerivationService _keyDerivationService;
        private byte[] _key;
        private byte[] _previousKey;

        public CryptoService(
            ISecureStorageService secureStorage,
            IKeyDerivationService keyDerivationService)
        {
            _secureStorage = secureStorage;
            _keyDerivationService = keyDerivationService;
        }

        public byte[] Key
        {
            get
            {
                if(_key == null)
                {
                    _key = _secureStorage.Retrieve(KeyKey);
                }

                return _key;
            }
            set
            {
                if(value != null)
                {
                    _secureStorage.Store(KeyKey, value);
                }
                else
                {
                    PreviousKey = _key;
                    _secureStorage.Delete(KeyKey);
                    _key = null;
                }
            }
        }

        public string Base64Key
        {
            get
            {
                if(Key == null)
                {
                    return null;
                }

                return Convert.ToBase64String(Key);
            }
        }

        public byte[] PreviousKey
        {
            get
            {
                if(_previousKey == null)
                {
                    _previousKey = _secureStorage.Retrieve(PreviousKeyKey);
                }

                return _previousKey;
            }
            private set
            {
                if(value != null)
                {
                    _secureStorage.Store(PreviousKeyKey, value);
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
                    throw new InvalidOperationException("Key must be set before asking if it has changed.");
                }

                if(PreviousKey == null)
                {
                    return Key != null;
                }

                return !PreviousKey.SequenceEqual(Key);
            }
        }

        public byte[] EncKey => Key?.Take(16).ToArray();
        public byte[] MacKey => Key?.Skip(16).Take(16).ToArray();

        public CipherString Encrypt(string plaintextValue)
        {
            if(Key == null)
            {
                throw new ArgumentNullException(nameof(Key));
            }

            if(plaintextValue == null)
            {
                throw new ArgumentNullException(nameof(plaintextValue));
            }

            var plaintextBytes = Encoding.UTF8.GetBytes(plaintextValue);

            var provider = WinRTCrypto.SymmetricKeyAlgorithmProvider.OpenAlgorithm(SymmetricAlgorithm.AesCbcPkcs7);
            // TODO: Turn on whenever ready to support encrypt-then-mac
            var cryptoKey = provider.CreateSymmetricKey(true ? EncKey : Key);
            var iv = WinRTCrypto.CryptographicBuffer.GenerateRandom(provider.BlockLength);
            var encryptedBytes = WinRTCrypto.CryptographicEngine.Encrypt(cryptoKey, plaintextBytes, iv);
            // TODO: Turn on whenever ready to support encrypt-then-mac
            var mac = true ? ComputeMac(encryptedBytes, iv) : null;

            return new CipherString(Convert.ToBase64String(iv), Convert.ToBase64String(encryptedBytes), mac);
        }

        public string Decrypt(CipherString encyptedValue)
        {
            try
            {
                if(Key == null)
                {
                    throw new ArgumentNullException(nameof(Key));
                }

                if(encyptedValue == null)
                {
                    throw new ArgumentNullException(nameof(encyptedValue));
                }

                if(encyptedValue.Mac != null)
                {
                    var computedMac = ComputeMac(encyptedValue.CipherTextBytes, encyptedValue.InitializationVectorBytes);
                    if(computedMac != encyptedValue.Mac)
                    {
                        throw new InvalidOperationException("MAC failed.");
                    }
                }

                var provider = WinRTCrypto.SymmetricKeyAlgorithmProvider.OpenAlgorithm(SymmetricAlgorithm.AesCbcPkcs7);
                var cryptoKey = provider.CreateSymmetricKey(encyptedValue.Mac != null ? EncKey : Key);
                var decryptedBytes = WinRTCrypto.CryptographicEngine.Decrypt(cryptoKey, encyptedValue.CipherTextBytes,
                    encyptedValue.InitializationVectorBytes);
                return Encoding.UTF8.GetString(decryptedBytes, 0, decryptedBytes.Length).TrimEnd('\0');
            }
            catch(Exception e)
            {
                Debug.WriteLine("Could not decrypt '{0}'. {1}", encyptedValue, e.Message);
                return "[error: cannot decrypt]";
            }
        }

        private string ComputeMac(byte[] ctBytes, byte[] ivBytes)
        {
            if(MacKey == null)
            {
                throw new ArgumentNullException(nameof(MacKey));
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
            var hasher = algorithm.CreateHash(MacKey);
            hasher.Append(ivBytes.Concat(ctBytes).ToArray());
            var mac = hasher.GetValueAndReset();
            return Convert.ToBase64String(mac);
        }

        public byte[] MakeKeyFromPassword(string password, string salt)
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

            var key = _keyDerivationService.DeriveKey(passwordBytes, saltBytes, 5000);
            return key;
        }

        public string MakeKeyFromPasswordBase64(string password, string salt)
        {
            var key = MakeKeyFromPassword(password, salt);
            return Convert.ToBase64String(key);
        }

        public byte[] HashPassword(byte[] key, string password)
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
            var hash = _keyDerivationService.DeriveKey(key, passwordBytes, 1);
            return hash;
        }

        public string HashPasswordBase64(byte[] key, string password)
        {
            var hash = HashPassword(key, password);
            return Convert.ToBase64String(hash);
        }
    }
}
