using System;
using System.Text;
using Bit.App.Abstractions;
using Bit.App.Models;
using Org.BouncyCastle.Crypto.Digests;
using Org.BouncyCastle.Crypto.Engines;
using Org.BouncyCastle.Crypto.Generators;
using Org.BouncyCastle.Crypto.Modes;
using Org.BouncyCastle.Crypto.Paddings;
using Org.BouncyCastle.Crypto.Parameters;

namespace Bit.App.Services
{
    public class CryptoService : ICryptoService
    {
        private const string KeyKey = "key";
        private const int InitializationVectorSize = 16;
        private const int KeySize = 256;
        private const int Iterations = 5000;

        private readonly PaddedBufferedBlockCipher _cipher;
        private readonly ISecureStorageService _secureStorage;
        private KeyParameter _keyParameter;

        public CryptoService(ISecureStorageService secureStorage)
        {
            var engine = new AesEngine();
            var blockCipher = new CbcBlockCipher(engine);
            _cipher = new PaddedBufferedBlockCipher(blockCipher);

            _secureStorage = secureStorage;
        }

        public byte[] Key
        {
            get
            {
                if(_keyParameter != null)
                {
                    _keyParameter.GetKey();
                }

                var storedKey = _secureStorage.Retrieve(KeyKey);
                if(storedKey == null)
                {
                    return null;
                }

                _keyParameter = new KeyParameter(storedKey);
                return _keyParameter.GetKey();
            }
            set
            {
                _secureStorage.Store(KeyKey, value);
                _keyParameter = new KeyParameter(value);
            }
        }
        public string Base64Key { get { return Convert.ToBase64String(Key); } }

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

            var iv = GenerateRandomInitializationVector();
            var keyParamWithIV = new ParametersWithIV(_keyParameter, iv, 0, InitializationVectorSize);

            _cipher.Init(true, keyParamWithIV);
            var encryptedBytes = new byte[_cipher.GetOutputSize(plaintextBytes.Length)];
            var length = _cipher.ProcessBytes(plaintextBytes, encryptedBytes, 0);
            _cipher.DoFinal(encryptedBytes, length);

            return new CipherString(Convert.ToBase64String(iv), Convert.ToBase64String(encryptedBytes));
        }

        public string Decrypt(CipherString encyptedValue)
        {
            if(Key == null)
            {
                throw new ArgumentNullException(nameof(Key));
            }

            if(encyptedValue == null)
            {
                throw new ArgumentNullException(nameof(encyptedValue));
            }

            var keyParamWithIV = new ParametersWithIV(_keyParameter, encyptedValue.InitializationVectorBytes, 0, InitializationVectorSize);

            _cipher.Init(false, keyParamWithIV);
            byte[] comparisonBytes = new byte[_cipher.GetOutputSize(encyptedValue.CipherTextBytes.Length)];
            var length = _cipher.ProcessBytes(encyptedValue.CipherTextBytes, comparisonBytes, 0);
            _cipher.DoFinal(comparisonBytes, length);

            return Encoding.UTF8.GetString(comparisonBytes, 0, comparisonBytes.Length).TrimEnd('\0');
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

            var generator = new Pkcs5S2ParametersGenerator(new Sha256Digest());
            generator.Init(passwordBytes, saltBytes, Iterations);
            return ((KeyParameter)generator.GenerateDerivedMacParameters(KeySize)).GetKey();
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

            var generator = new Pkcs5S2ParametersGenerator(new Sha256Digest());
            generator.Init(key, passwordBytes, 1);
            return ((KeyParameter)generator.GenerateDerivedMacParameters(KeySize)).GetKey();
        }

        public string HashPasswordBase64(byte[] key, string password)
        {
            var hash = HashPassword(key, password);
            return Convert.ToBase64String(hash);
        }

        private byte[] GenerateRandomInitializationVector()
        {
            var rand = new Random();
            var iv = new byte[InitializationVectorSize];
            rand.NextBytes(iv);
            return iv;
        }
    }
}
